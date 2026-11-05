import { z } from "zod";
import { durationSeconds, modActionSchema, snowflake, version } from "./common";

export const autoModModules = [
  "ANTI_SPAM",
  "ANTI_LINK",
  "ANTI_INVITE",
  "WORD_FILTER",
  "ANTI_CAPS",
  "ANTI_MENTION",
] as const;
export type AutoModModuleKey = (typeof autoModModules)[number];

export const moduleOptionsSchemas = {
  ANTI_SPAM: z.object({
    maxMessages: z.number().int().min(2).max(20),
    perSeconds: z.number().int().min(1).max(60),
    maxDuplicates: z.number().int().min(1).max(10),
  }),
  ANTI_LINK: z.object({
    allowedDomains: z
      .array(
        z
          .string()
          .trim()
          .toLowerCase()
          .regex(/^[a-z0-9.-]+\.[a-z]{2,}$/),
      )
      .max(100),
  }),
  ANTI_INVITE: z.object({
    allowOwnServer: z.boolean(),
  }),
  WORD_FILTER: z.object({
    normalizeLeetspeak: z.boolean(),
  }),
  ANTI_CAPS: z.object({
    maxPercent: z.number().int().min(30).max(100),
    minLength: z.number().int().min(4).max(200),
  }),
  ANTI_MENTION: z.object({
    maxMentions: z.number().int().min(2).max(50),
    countRoles: z.boolean(),
  }),
} as const satisfies Record<AutoModModuleKey, z.ZodType>;

export const moduleDefaults: {
  [M in AutoModModuleKey]: z.infer<(typeof moduleOptionsSchemas)[M]>;
} = {
  ANTI_SPAM: { maxMessages: 5, perSeconds: 5, maxDuplicates: 3 },
  ANTI_LINK: { allowedDomains: ["youtube.com", "github.com"] },
  ANTI_INVITE: { allowOwnServer: true },
  WORD_FILTER: { normalizeLeetspeak: true },
  ANTI_CAPS: { maxPercent: 70, minLength: 10 },
  ANTI_MENTION: { maxMentions: 5, countRoles: true },
};

export type ModuleOptions<M extends AutoModModuleKey> = z.infer<(typeof moduleOptionsSchemas)[M]>;

const ruleBase = {
  enabled: z.boolean(),
  sensitivity: z.number().int().min(0).max(100),
  actions: z.array(modActionSchema).min(1).max(5),
  muteDuration: durationSeconds.nullable(),
  exemptChannels: z.array(snowflake).max(100),
  exemptRoles: z.array(snowflake).max(100),
};

function ruleFor<M extends AutoModModuleKey>(module: M) {
  return z
    .object({ module: z.literal(module), ...ruleBase, options: moduleOptionsSchemas[module] })
    .refine((r) => !r.actions.includes("TIMEOUT") || r.muteDuration !== null, {
      message: "Timeout requires a duration",
      path: ["muteDuration"],
    })
    .refine((r) => new Set(r.actions).size === r.actions.length, {
      message: "Duplicate actions",
      path: ["actions"],
    });
}

export const autoModRuleSchema = z.discriminatedUnion("module", [
  ruleFor("ANTI_SPAM"),
  ruleFor("ANTI_LINK"),
  ruleFor("ANTI_INVITE"),
  ruleFor("WORD_FILTER"),
  ruleFor("ANTI_CAPS"),
  ruleFor("ANTI_MENTION"),
]);
export type AutoModRuleInput = z.infer<typeof autoModRuleSchema>;

export const bannedWordSchema = z
  .string()
  .trim()
  .toLowerCase()
  .min(2)
  .max(100)
  .regex(/^[\p{L}\p{N}*\s'_-]+$/u, "Invalid characters");

export const autoModConfigSchema = z
  .object({
    rules: z.array(autoModRuleSchema).length(autoModModules.length),
    bannedWords: z.array(bannedWordSchema).max(1000),
    versions: z.record(z.string(), version),
  })
  .refine((c) => new Set(c.rules.map((r) => r.module)).size === autoModModules.length, {
    message: "Each module must appear exactly once",
    path: ["rules"],
  });
export type AutoModConfigInput = z.infer<typeof autoModConfigSchema>;

/** Parsuje import zakazanych słów (txt/csv): jedno słowo w linii lub rozdzielone przecinkami. */
export function parseBannedWordsImport(text: string): { words: string[]; rejected: string[] } {
  const raw = text
    .split(/[\r\n,;]+/)
    .map((s) => s.replace(/^["']|["']$/g, "").trim())
    .filter(Boolean);
  const words = new Set<string>();
  const rejected: string[] = [];
  for (const item of raw) {
    const parsed = bannedWordSchema.safeParse(item);
    if (parsed.success) words.add(parsed.data);
    else rejected.push(item);
  }
  return { words: [...words], rejected };
}

/** Domyślne reguły dla nowego serwera. */
export function defaultRule(module: AutoModModuleKey): AutoModRuleInput {
  const base = {
    enabled: module === "ANTI_SPAM" || module === "ANTI_INVITE",
    sensitivity: 50,
    actions: ["DELETE"] as AutoModRuleInput["actions"],
    muteDuration: null,
    exemptChannels: [],
    exemptRoles: [],
  };
  const options = moduleDefaults[module];
  return { module, ...base, options } as AutoModRuleInput;
}
