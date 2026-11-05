/**
 * Seed: przykładowe treści (changelog, FAQ, opinie, partnerzy, incydenty) oraz dane
 * demonstracyjne dla serwerów z trybu DISCORD_API_MOCK, żeby panel od razu miał co pokazać.
 * Uruchom: pnpm db:seed
 */
import "dotenv/config";
import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../lib/db/generated/client";
import {
  seedChangelog,
  seedFaq,
  seedIncidents,
  seedPartners,
  seedTestimonials,
} from "../content/seed-data";
import { MOCK_GUILDS } from "../lib/discord/mock";

const db = new PrismaClient({
  adapter: new PrismaPg({ connectionString: process.env.DATABASE_URL }),
});

async function main() {
  await db.changelogItem.deleteMany();
  await db.changelogEntry.deleteMany();
  for (const e of seedChangelog) {
    await db.changelogEntry.create({
      data: {
        version: e.version,
        publishedAt: new Date(e.publishedAt),
        titlePl: e.titlePl,
        titleEn: e.titleEn,
        items: { create: e.items.map((it, position) => ({ ...it, position })) },
      },
    });
  }
  await db.faq.deleteMany();
  await db.faq.createMany({ data: seedFaq.map((f, i) => ({ ...f, position: i })) });
  await db.testimonial.deleteMany();
  await db.testimonial.createMany({
    data: seedTestimonials.map((t, i) => ({ ...t, position: i })),
  });
  await db.partner.deleteMany();
  await db.partner.createMany({ data: seedPartners.map((p, i) => ({ ...p, position: i })) });
  await db.incidentUpdate.deleteMany();
  await db.incident.deleteMany();
  for (const inc of seedIncidents) {
    await db.incident.create({
      data: {
        titlePl: inc.titlePl,
        titleEn: inc.titleEn,
        severity: inc.severity,
        status: inc.status,
        services: [...inc.services],
        startedAt: new Date(inc.startedAt),
        resolvedAt: new Date(inc.resolvedAt),
        updates: { create: inc.updates.map((u) => ({ ...u, createdAt: new Date(u.createdAt) })) },
      },
    });
  }

  // Serwery demo (zgodne z lib/discord/mock.ts). Ostatnie dwa bez bota.
  const regions = ["PL", "PL", "DE", "PL", "GB"];
  for (const [i, g] of MOCK_GUILDS.entries()) {
    await db.guild.upsert({
      where: { id: g.id },
      create: {
        id: g.id,
        name: g.name,
        ownerId: "100000000000000001",
        memberCount: g.approximate_member_count ?? 0,
        region: regions[i] ?? "PL",
        botPresent: i < 3,
      },
      update: { name: g.name, memberCount: g.approximate_member_count ?? 0, botPresent: i < 3 },
    });
  }
  const demo = MOCK_GUILDS[0]!.id;
  await db.guildStatBucket.deleteMany({ where: { guildId: demo } });
  const now = new Date();
  now.setUTCMinutes(0, 0, 0);
  const buckets: {
    guildId: string;
    metric: "AUTOMOD_DELETED" | "PUNISHMENTS" | "MEMBER_JOINS" | "RAIDS";
    bucketStart: Date;
    value: number;
  }[] = [];
  for (let h = 0; h < 24 * 60; h++) {
    const at = new Date(now.getTime() - h * 3600_000);
    const daily = Math.sin((at.getUTCHours() / 24) * Math.PI * 2 - 1.2) * 0.5 + 0.6;
    const r = (n: number) => Math.max(0, Math.round(n * daily + (Math.random() - 0.5) * n * 0.6));
    buckets.push({ guildId: demo, metric: "AUTOMOD_DELETED", bucketStart: at, value: r(28) });
    buckets.push({ guildId: demo, metric: "PUNISHMENTS", bucketStart: at, value: r(3) });
    buckets.push({ guildId: demo, metric: "MEMBER_JOINS", bucketStart: at, value: r(9) });
    if (h % 173 === 0) buckets.push({ guildId: demo, metric: "RAIDS", bucketStart: at, value: 1 });
  }
  await db.guildStatBucket.createMany({ data: buckets, skipDuplicates: true });

  await db.modCase.deleteMany({ where: { guildId: demo } });
  const types = ["WARN", "WARN", "TIMEOUT", "WARN", "KICK", "BAN", "WARN", "TIMEOUT"] as const;
  const reasons = [
    "Spam na #ogólny",
    "Reklama innego serwera",
    "Flood emotkami",
    "Wyzwiska",
    "Raid",
    "Phishing — fałszywe Nitro",
    "CAPS LOCK",
    "Masowe wzmianki",
  ];
  const names = [
    "kuba_2009",
    "spammer_x",
    "ola.gg",
    "nitro_free",
    "tomek",
    "raid_bot_17",
    "ania",
    "ela_pl",
  ];
  await db.modCase.createMany({
    data: Array.from({ length: 64 }, (_, i) => {
      const type = types[i % types.length]!;
      return {
        guildId: demo,
        caseNumber: i + 1,
        type,
        targetId: `30000000000000${String(1000 + (i % 8)).padStart(4, "0")}`,
        targetTag: names[i % names.length],
        moderatorId: i % 3 === 0 ? "automod" : "100000000000000001",
        moderatorTag: i % 3 === 0 ? "AutoMod" : "Admin (dev)",
        reason: reasons[i % reasons.length],
        source: i % 3 === 0 ? ("AUTOMOD" as const) : ("MANUAL" as const),
        automodModule: i % 3 === 0 ? ("ANTI_SPAM" as const) : null,
        duration: type === "TIMEOUT" ? 3600 : null,
        createdAt: new Date(Date.now() - i * 5.3 * 3600_000),
      };
    }),
  });

  // uptime 90 dni + heartbeat
  await db.uptimeDay.deleteMany();
  const days: {
    service: "BOT" | "API" | "DATABASE" | "SHARDS" | "DASHBOARD";
    day: Date;
    uptimePct: number;
  }[] = [];
  for (let d = 0; d < 90; d++) {
    const day = new Date();
    day.setUTCHours(0, 0, 0, 0);
    day.setUTCDate(day.getUTCDate() - d);
    for (const s of ["BOT", "API", "DATABASE", "SHARDS", "DASHBOARD"] as const) {
      const dip =
        (s === "SHARDS" || s === "BOT") && d === 24
          ? 98.4
          : s === "DATABASE" && d === 46
            ? 99.6
            : 100;
      days.push({ service: s, day, uptimePct: dip });
    }
  }
  await db.uptimeDay.createMany({ data: days });
  await db.serviceCheck.create({ data: { service: "BOT", ok: true, latencyMs: 41 } });
  console.log("✔ Seed complete");
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(() => db.$disconnect());
