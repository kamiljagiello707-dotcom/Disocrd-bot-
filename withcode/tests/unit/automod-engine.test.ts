import { describe, expect, it } from "vitest";
import {
  capsRatio,
  decide,
  evaluate,
  normalizeText,
  scale,
  type EngineConfig,
  type EngineMessage,
} from "@/lib/automod/engine";

const cfg = (over: Partial<EngineConfig> = {}): EngineConfig => ({
  enabled: {
    ANTI_SPAM: true,
    ANTI_LINK: true,
    ANTI_INVITE: true,
    WORD_FILTER: true,
    ANTI_CAPS: true,
    ANTI_MENTION: true,
  },
  sensitivity: 50,
  bannedWords: ["idiota", "kup*"],
  allowedDomains: ["youtube.com"],
  ...over,
});
const msg = (content: string, t = 0): EngineMessage => ({ authorId: "u", content, timestamp: t });

describe("automod engine", () => {
  it("allows normal messages", () => {
    expect(evaluate(msg("Cześć, ktoś gra dziś?"), [], cfg())).toEqual([]);
  });
  it("detects invites", () => {
    expect(evaluate(msg("wbijaj discord.gg/abc"), [], cfg()).map((v) => v.module)).toContain(
      "ANTI_INVITE",
    );
  });
  it("blocks links outside allowlist but allows subdomains of allowed", () => {
    expect(evaluate(msg("http://free-skins.ru/x"), [], cfg()).map((v) => v.module)).toContain(
      "ANTI_LINK",
    );
    expect(evaluate(msg("https://www.youtube.com/watch?v=1"), [], cfg())).toEqual([]);
    expect(evaluate(msg("https://m.youtube.com/watch?v=1"), [], cfg())).toEqual([]);
  });
  it("word filter handles leetspeak, diacritics and wildcards", () => {
    expect(evaluate(msg("jesteś 1d10t4"), [], cfg()).map((v) => v.module)).toContain("WORD_FILTER");
    expect(
      evaluate(msg("KUPUJCIE teraz"), [], cfg({ enabled: { WORD_FILTER: true } })).map(
        (v) => v.module,
      ),
    ).toContain("WORD_FILTER");
    expect(
      evaluate(msg("skupienie to podstawa"), [], cfg({ enabled: { WORD_FILTER: true } })),
    ).toEqual([]);
  });
  it("anti-caps respects minimum length", () => {
    expect(evaluate(msg("OK"), [], cfg({ enabled: { ANTI_CAPS: true } }))).toEqual([]);
    expect(
      evaluate(msg("CZEMU NIKT NIE ODPISUJE"), [], cfg({ enabled: { ANTI_CAPS: true } }))[0]
        ?.module,
    ).toBe("ANTI_CAPS");
  });
  it("anti-mention counts mentions", () => {
    expect(
      evaluate(msg("@a1 @b2 @c3 @d4 @e5 @f6 @g7"), [], cfg({ enabled: { ANTI_MENTION: true } }))[0]
        ?.module,
    ).toBe("ANTI_MENTION");
  });
  it("anti-spam detects floods and duplicates within 5 s", () => {
    const history = [msg("a", 0), msg("b", 500), msg("c", 1000), msg("d", 1500), msg("e", 2000)];
    expect(
      evaluate(msg("f", 2500), history, cfg({ enabled: { ANTI_SPAM: true } }))[0]?.module,
    ).toBe("ANTI_SPAM");
    expect(evaluate(msg("f", 20_000), history, cfg({ enabled: { ANTI_SPAM: true } }))).toEqual([]);
    const dupes = [msg("nitro", 0), msg("nitro", 400)];
    expect(
      evaluate(msg("NITRO", 800), dupes, cfg({ enabled: { ANTI_SPAM: true } }))[0]?.reason,
    ).toBe("duplicate");
  });
  it("disabled modules do nothing", () => {
    expect(evaluate(msg("discord.gg/x HTTP://BAD.RU"), [], cfg({ enabled: {} }))).toEqual([]);
  });
  it("helpers", () => {
    expect(normalizeText("Łódź 1d10t")).toBe("lodz idiot");
    expect(scale(0, 3, 7)).toBe(7);
    expect(scale(100, 3, 7)).toBe(3);
    expect(capsRatio("ABc").ratio).toBeCloseTo(2 / 3);
    expect(decide([], 5)).toBe("allow");
    expect(decide([{ module: "ANTI_SPAM", reason: "" }], 0)).toBe("delete");
    expect(decide([{ module: "ANTI_SPAM", reason: "" }], 1)).toBe("warn");
    expect(decide([{ module: "ANTI_SPAM", reason: "" }], 2)).toBe("timeout");
  });
});
