import { describe, expect, it } from "vitest";
import {
  autoModConfigSchema,
  autoModModules,
  defaultRule,
  parseBannedWordsImport,
} from "@/lib/validators/automod";
import {
  antiRaidSchema,
  greetingSchema,
  punishmentsSchema,
  settingsSchema,
  verificationSchema,
} from "@/lib/validators/guild";
import { contactSchema } from "@/lib/validators/contact";

const validConfig = () => ({
  rules: autoModModules.map((m) => defaultRule(m)),
  bannedWords: ["kup*", "scam"],
  versions: Object.fromEntries(autoModModules.map((m) => [m, 1])),
});

describe("autoModConfigSchema", () => {
  it("accepts default config", () => {
    expect(autoModConfigSchema.safeParse(validConfig()).success).toBe(true);
  });
  it("rejects sensitivity out of range", () => {
    const c = validConfig();
    c.rules[0]!.sensitivity = 101;
    expect(autoModConfigSchema.safeParse(c).success).toBe(false);
  });
  it("requires duration for TIMEOUT action", () => {
    const c = validConfig();
    c.rules[0]!.actions = ["DELETE", "TIMEOUT"];
    c.rules[0]!.muteDuration = null;
    const r = autoModConfigSchema.safeParse(c);
    expect(r.success).toBe(false);
    c.rules[0]!.muteDuration = 600;
    expect(autoModConfigSchema.safeParse(c).success).toBe(true);
  });
  it("rejects timeout longer than 28 days", () => {
    const c = validConfig();
    c.rules[0]!.actions = ["TIMEOUT"];
    c.rules[0]!.muteDuration = 29 * 86400;
    expect(autoModConfigSchema.safeParse(c).success).toBe(false);
  });
  it("rejects duplicated module", () => {
    const c = validConfig();
    c.rules[1] = { ...c.rules[0]! };
    expect(autoModConfigSchema.safeParse(c).success).toBe(false);
  });
  it("rejects invalid exempt channel id", () => {
    const c = validConfig();
    c.rules[0]!.exemptChannels = ["not-a-snowflake"];
    expect(autoModConfigSchema.safeParse(c).success).toBe(false);
  });
  it("rejects empty actions", () => {
    const c = validConfig();
    c.rules[0]!.actions = [];
    expect(autoModConfigSchema.safeParse(c).success).toBe(false);
  });
});

describe("parseBannedWordsImport", () => {
  it("parses txt and csv, dedupes, normalizes case", () => {
    const { words, rejected } = parseBannedWordsImport(
      'Scam\nkup*\n"darmowe nitro", SCAM;<script>\nx',
    );
    expect(words).toEqual(["scam", "kup*", "darmowe nitro"]);
    expect(rejected).toEqual(["<script>", "x"]);
  });
});

describe("guild validators", () => {
  it("anti-raid bounds", () => {
    expect(
      antiRaidSchema.safeParse({
        enabled: true,
        joinThreshold: 10,
        joinWindowSec: 10,
        minAccountAgeDays: 7,
        action: "KICK",
        version: 1,
      }).success,
    ).toBe(true);
    expect(
      antiRaidSchema.safeParse({
        enabled: true,
        joinThreshold: 1,
        joinWindowSec: 10,
        minAccountAgeDays: 7,
        action: "KICK",
        version: 1,
      }).success,
    ).toBe(false);
  });
  it("punishments: unique warn counts and timeout duration", () => {
    expect(
      punishmentsSchema.safeParse({
        thresholds: [
          { warnCount: 3, action: "TIMEOUT", duration: 3600 },
          { warnCount: 5, action: "BAN", duration: null },
        ],
      }).success,
    ).toBe(true);
    expect(
      punishmentsSchema.safeParse({
        thresholds: [
          { warnCount: 3, action: "TIMEOUT", duration: 3600 },
          { warnCount: 3, action: "BAN", duration: null },
        ],
      }).success,
    ).toBe(false);
    expect(
      punishmentsSchema.safeParse({
        thresholds: [{ warnCount: 3, action: "TIMEOUT", duration: null }],
      }).success,
    ).toBe(false);
  });
  it("verification requires channel and role when enabled", () => {
    const base = {
      enabled: true,
      mode: "BUTTON" as const,
      channelId: null,
      roleId: null,
      message: null,
      version: 1,
    };
    expect(verificationSchema.safeParse(base).success).toBe(false);
    expect(verificationSchema.safeParse({ ...base, enabled: false }).success).toBe(true);
    expect(
      verificationSchema.safeParse({
        ...base,
        channelId: "123456789012345678",
        roleId: "123456789012345679",
      }).success,
    ).toBe(true);
  });
  it("greeting: https image only, non-empty when enabled", () => {
    const g = {
      type: "WELCOME" as const,
      enabled: true,
      channelId: "123456789012345678",
      content: "Hi {user}",
      embed: null,
      dm: false,
      version: 1,
    };
    expect(greetingSchema.safeParse(g).success).toBe(true);
    expect(greetingSchema.safeParse({ ...g, content: "  " }).success).toBe(false);
    expect(
      greetingSchema.safeParse({
        ...g,
        embed: { title: "x", description: null, imageUrl: "http://evil.test/a.png" },
      }).success,
    ).toBe(false);
  });
  it("settings prefix", () => {
    expect(
      settingsSchema.safeParse({ prefix: "!", locale: "PL", timezone: "Europe/Warsaw", version: 1 })
        .success,
    ).toBe(true);
    expect(
      settingsSchema.safeParse({
        prefix: "@x",
        locale: "PL",
        timezone: "Europe/Warsaw",
        version: 1,
      }).success,
    ).toBe(false);
    expect(
      settingsSchema.safeParse({
        prefix: "toolong",
        locale: "PL",
        timezone: "Europe/Warsaw",
        version: 1,
      }).success,
    ).toBe(false);
    expect(
      settingsSchema.safeParse({ prefix: "!", locale: "PL", timezone: "Mars/Base", version: 1 })
        .success,
    ).toBe(false);
  });
  it("contact honeypot must be empty", () => {
    const ok = {
      name: "Ola",
      email: "ola@example.com",
      topic: "SUPPORT",
      message: "Dzień dobry, mam pytanie o bota.",
    };
    expect(contactSchema.safeParse(ok).success).toBe(true);
    expect(contactSchema.safeParse({ ...ok, website: "http://spam" }).success).toBe(false);
    expect(contactSchema.safeParse({ ...ok, email: "nope" }).success).toBe(false);
  });
});
