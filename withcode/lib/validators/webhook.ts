import { z } from "zod";

const snowflake = z.string().regex(/^\d{17,20}$/);

/** Zdarzenia, które bot może wysłać do strony (podpisane HMAC). */
export const botWebhookSchema = z.discriminatedUnion("type", [
  z.object({
    type: z.literal("guild.upsert"),
    guild: z.object({
      id: snowflake,
      name: z.string().max(100),
      icon: z.string().nullable(),
      ownerId: snowflake,
      memberCount: z.number().int().min(0),
      locale: z.string().max(10).optional(),
    }),
  }),
  z.object({ type: z.literal("guild.remove"), guildId: snowflake }),
  z.object({
    type: z.literal("stats.increment"),
    guildId: snowflake,
    metrics: z
      .array(
        z.object({
          metric: z.enum([
            "AUTOMOD_DELETED",
            "PUNISHMENTS",
            "MEMBER_JOINS",
            "MEMBER_LEAVES",
            "RAIDS",
            "MESSAGES",
          ]),
          value: z.number().int().min(1).max(1_000_000),
        }),
      )
      .max(20),
    at: z.number().int().optional(),
  }),
  z.object({
    type: z.literal("case.create"),
    case: z.object({
      guildId: snowflake,
      type: z.enum(["WARN", "TIMEOUT", "KICK", "BAN", "SOFTBAN", "UNBAN", "NOTE"]),
      targetId: snowflake,
      targetTag: z.string().max(64).optional(),
      moderatorId: z.string().max(20),
      moderatorTag: z.string().max(64).optional(),
      reason: z.string().max(1000).optional(),
      source: z.enum(["MANUAL", "AUTOMOD", "AUTO_PUNISHMENT", "ANTI_RAID"]).default("MANUAL"),
      duration: z.number().int().min(0).optional(),
    }),
  }),
  z.object({
    type: z.literal("heartbeat"),
    pingMs: z.number().int().min(0).max(60000),
    shards: z
      .array(z.object({ id: z.number().int(), online: z.boolean(), pingMs: z.number().int() }))
      .max(1024)
      .optional(),
  }),
]);
export type BotWebhookEvent = z.infer<typeof botWebhookSchema>;

/** Przybliżony region (dla globu) z języka serwera — bez danych osobowych. */
export function regionFromLocale(locale?: string): string | null {
  if (!locale) return null;
  const map: Record<string, string> = {
    pl: "PL",
    de: "DE",
    "en-GB": "GB",
    "en-US": "US-E",
    "pt-BR": "BR",
    uk: "UA",
    cs: "CZ",
    fr: "FR",
    "es-ES": "ES",
    ja: "JP",
    tr: "TR",
    nl: "NL",
    "sv-SE": "SE",
    hi: "IN",
  };
  return map[locale] ?? null;
}
