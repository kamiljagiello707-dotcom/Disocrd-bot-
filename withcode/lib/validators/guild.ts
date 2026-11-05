import { z } from "zod";
import { durationSeconds, modActionSchema, optionalSnowflake, snowflake, version } from "./common";

export const antiRaidSchema = z.object({
  enabled: z.boolean(),
  joinThreshold: z.number().int().min(3).max(100),
  joinWindowSec: z.number().int().min(5).max(300),
  minAccountAgeDays: z.number().int().min(0).max(365),
  action: z.enum(["KICK", "BAN", "TIMEOUT"]),
  version,
});
export type AntiRaidInput = z.infer<typeof antiRaidSchema>;

export const lockdownSchema = z.object({ active: z.boolean() });

export const thresholdSchema = z
  .object({
    warnCount: z.number().int().min(1).max(50),
    action: z.enum(["TIMEOUT", "KICK", "BAN"]),
    duration: durationSeconds.nullable(),
  })
  .refine((t) => t.action !== "TIMEOUT" || t.duration !== null, {
    message: "Timeout requires a duration",
    path: ["duration"],
  });

export const punishmentsSchema = z
  .object({ thresholds: z.array(thresholdSchema).max(20) })
  .refine((v) => new Set(v.thresholds.map((t) => t.warnCount)).size === v.thresholds.length, {
    message: "Duplicate warning count",
    path: ["thresholds"],
  });
export type PunishmentsInput = z.infer<typeof punishmentsSchema>;

export const logEvents = [
  "MESSAGE_DELETE",
  "MESSAGE_EDIT",
  "MEMBER_JOIN",
  "MEMBER_LEAVE",
  "MEMBER_BAN",
  "MEMBER_UNBAN",
  "ROLE_UPDATE",
  "NICK_UPDATE",
  "CHANNEL_UPDATE",
  "VOICE",
  "MOD_ACTION",
  "AUTOMOD",
] as const;
export type LogEventKey = (typeof logEvents)[number];

export const logsSchema = z.object({
  channels: z.record(z.enum(logEvents), optionalSnowflake),
});
export type LogsInput = z.infer<typeof logsSchema>;

export const verificationSchema = z
  .object({
    enabled: z.boolean(),
    mode: z.enum(["BUTTON", "CAPTCHA"]),
    channelId: optionalSnowflake,
    roleId: optionalSnowflake,
    message: z.string().trim().max(2000).nullable(),
    version,
  })
  .refine((v) => !v.enabled || (v.channelId !== null && v.roleId !== null), {
    message: "Channel and role are required when enabled",
    path: ["roleId"],
  });
export type VerificationInput = z.infer<typeof verificationSchema>;

export const embedSchema = z.object({
  title: z.string().trim().max(256).nullable(),
  description: z.string().trim().max(4000).nullable(),
  imageUrl: z
    .string()
    .trim()
    .url()
    .refine((u) => u.startsWith("https://"), "HTTPS only")
    .nullable(),
});

export const greetingSchema = z
  .object({
    type: z.enum(["WELCOME", "GOODBYE"]),
    enabled: z.boolean(),
    channelId: optionalSnowflake,
    content: z.string().max(2000).nullable(),
    embed: embedSchema.nullable(),
    dm: z.boolean(),
    version,
  })
  .refine((g) => !g.enabled || g.channelId !== null || g.dm, {
    message: "Channel is required",
    path: ["channelId"],
  })
  .refine((g) => !g.enabled || (g.content?.trim() ?? "") !== "" || g.embed !== null, {
    message: "Message cannot be empty",
    path: ["content"],
  });
export type GreetingInput = z.infer<typeof greetingSchema>;

export const reactionRoleGroupSchema = z.object({
  id: z.string().optional(),
  channelId: snowflake,
  title: z.string().trim().min(1).max(256),
  mode: z.enum(["NORMAL", "UNIQUE", "VERIFY"]),
  style: z.enum(["REACTIONS", "BUTTONS", "SELECT"]),
  entries: z
    .array(
      z.object({
        emoji: z.string().trim().max(64).nullable(),
        label: z.string().trim().max(80).nullable(),
        roleId: snowflake,
      }),
    )
    .min(1)
    .max(25),
});

export const rolesSchema = z.object({
  groups: z.array(reactionRoleGroupSchema).max(25),
  autoRoles: z
    .array(
      z.object({
        roleId: snowflake,
        delaySec: z.number().int().min(0).max(86400),
        botsOnly: z.boolean(),
      }),
    )
    .max(10),
});
export type RolesInput = z.infer<typeof rolesSchema>;

export const accessSchema = z.object({
  entries: z
    .array(
      z.object({
        subjectType: z.enum(["USER", "ROLE"]),
        subjectId: snowflake,
        level: z.enum(["VIEWER", "EDITOR"]),
      }),
    )
    .max(50),
});
export type AccessInput = z.infer<typeof accessSchema>;

export const timezones = [
  "Europe/Warsaw",
  "Europe/London",
  "Europe/Berlin",
  "Europe/Kyiv",
  "America/New_York",
  "America/Los_Angeles",
  "Asia/Tokyo",
  "UTC",
] as const;

export const settingsSchema = z.object({
  prefix: z
    .string()
    .trim()
    .min(1)
    .max(5)
    .regex(/^[^\s@#`]+$/, "Invalid prefix"),
  locale: z.enum(["PL", "EN"]),
  timezone: z.enum(timezones),
  version,
});
export type SettingsInput = z.infer<typeof settingsSchema>;

export const revokeCaseSchema = z.object({
  caseId: z.string().min(1),
  reason: z.string().trim().max(500).optional(),
});

export const casesQuerySchema = z.object({
  q: z.string().trim().max(100).optional(),
  type: z.enum(["WARN", "TIMEOUT", "KICK", "BAN", "SOFTBAN", "UNBAN", "NOTE"]).optional(),
  status: z.enum(["active", "revoked", "all"]).default("all"),
  page: z.coerce.number().int().min(1).max(10000).default(1),
});
export type CasesQuery = z.infer<typeof casesQuerySchema>;

export { modActionSchema };
