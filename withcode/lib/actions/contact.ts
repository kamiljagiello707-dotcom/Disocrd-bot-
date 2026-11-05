"use server";
import { db } from "@/lib/db";
import { contactSchema } from "@/lib/validators/contact";
import { rateLimit } from "@/lib/security/rate-limit";
import { clientIp, hashIp } from "@/lib/security/request";
import { verifyTurnstile } from "@/lib/security/turnstile";
import { sanitizeUserText } from "@/lib/security/sanitize";

export type ContactResult =
  | { ok: true }
  | { ok: false; error: "validation" | "rate_limited" | "captcha" | "server"; fields?: string[] };

export async function submitContact(input: unknown): Promise<ContactResult> {
  const parsed = contactSchema.safeParse(input);
  if (!parsed.success) {
    return {
      ok: false,
      error: "validation",
      fields: parsed.error.issues.map((i) => String(i.path[0])),
    };
  }
  // honeypot: boty wypełniają ukryte pole — udajemy sukces, nic nie zapisujemy
  if (parsed.data.website) return { ok: true };

  const ip = await clientIp();
  const ipHash = hashIp(ip);
  const limit = await rateLimit(`contact:${ipHash}`, 5, 3600);
  if (!limit.ok) return { ok: false, error: "rate_limited" };
  if (!(await verifyTurnstile(parsed.data.turnstileToken, ip)))
    return { ok: false, error: "captcha" };

  try {
    await db.contactMessage.create({
      data: {
        name: sanitizeUserText(parsed.data.name),
        email: parsed.data.email.toLowerCase(),
        topic: parsed.data.topic,
        message: sanitizeUserText(parsed.data.message),
        ipHash,
      },
    });
    return { ok: true };
  } catch {
    return { ok: false, error: "server" };
  }
}
