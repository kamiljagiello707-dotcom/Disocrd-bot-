import { NextResponse, type NextRequest } from "next/server";
import { revalidateTag } from "next/cache";
import { db } from "@/lib/db";
import {
  NONCE_HEADER,
  SIGNATURE_HEADER,
  TIMESTAMP_HEADER,
  verifySignature,
} from "@/lib/security/hmac";
import { rateLimit } from "@/lib/security/rate-limit";
import { botWebhookSchema, regionFromLocale } from "@/lib/validators/webhook";

/**
 * Endpoint, przez który bot informuje stronę o zmianach.
 * Podpis: X-WithBot-Signature = sha256=HMAC(BOT_WEBHOOK_SECRET, `${timestamp}.${body}`),
 * okno czasowe ±5 min, jednorazowy nonce (ochrona przed replay).
 */
export async function POST(req: NextRequest) {
  const secret = process.env.BOT_WEBHOOK_SECRET;
  if (!secret) return NextResponse.json({ error: "not_configured" }, { status: 503 });
  const limit = await rateLimit("webhook:bot", 600, 60);
  if (!limit.ok) return NextResponse.json({ error: "rate_limited" }, { status: 429 });

  const body = await req.text();
  if (body.length > 256_000) return NextResponse.json({ error: "too_large" }, { status: 413 });
  const check = verifySignature(secret, {
    signature: req.headers.get(SIGNATURE_HEADER),
    timestamp: req.headers.get(TIMESTAMP_HEADER),
    body,
  });
  if (!check.ok) return NextResponse.json({ error: check.reason }, { status: 401 });

  const nonce = req.headers.get(NONCE_HEADER);
  if (nonce) {
    try {
      await db.webhookNonce.create({ data: { nonce } });
    } catch {
      return NextResponse.json({ error: "replay" }, { status: 409 });
    }
  }

  let json: unknown;
  try {
    json = JSON.parse(body);
  } catch {
    return NextResponse.json({ error: "invalid_json" }, { status: 400 });
  }
  const parsed = botWebhookSchema.safeParse(json);
  if (!parsed.success)
    return NextResponse.json(
      { error: "invalid_payload", issues: parsed.error.issues.slice(0, 5) },
      { status: 422 },
    );
  const ev = parsed.data;

  switch (ev.type) {
    case "guild.upsert": {
      const { id, name, icon, ownerId, memberCount, locale } = ev.guild;
      await db.guild.upsert({
        where: { id },
        create: {
          id,
          name,
          icon,
          ownerId,
          memberCount,
          region: regionFromLocale(locale),
          botPresent: true,
        },
        update: {
          name,
          icon,
          ownerId,
          memberCount,
          region: regionFromLocale(locale),
          botPresent: true,
          leftAt: null,
        },
      });
      revalidateTag("stats", "max");
      break;
    }
    case "guild.remove":
      await db.guild.updateMany({
        where: { id: ev.guildId },
        data: { botPresent: false, leftAt: new Date() },
      });
      revalidateTag("stats", "max");
      break;
    case "stats.increment": {
      const at = new Date(ev.at ?? Date.now());
      at.setUTCMinutes(0, 0, 0);
      for (const m of ev.metrics) {
        await db.guildStatBucket.upsert({
          where: {
            guildId_metric_bucketStart: { guildId: ev.guildId, metric: m.metric, bucketStart: at },
          },
          create: { guildId: ev.guildId, metric: m.metric, bucketStart: at, value: m.value },
          update: { value: { increment: m.value } },
        });
      }
      break;
    }
    case "case.create": {
      const c = ev.case;
      await db.$transaction(async (tx) => {
        const last = await tx.modCase.findFirst({
          where: { guildId: c.guildId },
          orderBy: { caseNumber: "desc" },
          select: { caseNumber: true },
        });
        await tx.modCase.create({
          data: {
            ...c,
            caseNumber: (last?.caseNumber ?? 0) + 1,
            expiresAt: c.duration ? new Date(Date.now() + c.duration * 1000) : null,
          },
        });
      });
      break;
    }
    case "heartbeat":
      await db.serviceCheck.create({ data: { service: "BOT", ok: true, latencyMs: ev.pingMs } });
      if (ev.shards)
        await db.serviceCheck.create({
          data: {
            service: "SHARDS",
            ok: ev.shards.every((s) => s.online),
            latencyMs: Math.round(
              ev.shards.reduce((a, s) => a + s.pingMs, 0) / Math.max(1, ev.shards.length),
            ),
          },
        });
      revalidateTag("uptime", "max");
      break;
  }
  return NextResponse.json({ ok: true });
}
