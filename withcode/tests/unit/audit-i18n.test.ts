import { describe, expect, it } from "vitest";
import { computeDiff } from "@/lib/audit";
import pl from "@/messages/pl.json";
import en from "@/messages/en.json";

describe("audit diff", () => {
  it("diffs nested objects and keyed arrays", () => {
    const before = {
      prefix: "!",
      rules: [
        { module: "ANTI_SPAM", enabled: false },
        { module: "ANTI_CAPS", enabled: true },
      ],
    };
    const after = {
      prefix: "?",
      rules: [
        { module: "ANTI_SPAM", enabled: true },
        { module: "ANTI_CAPS", enabled: true },
      ],
    };
    expect(computeDiff(before, after)).toEqual([
      { path: "prefix", before: "!", after: "?" },
      { path: "rules[ANTI_SPAM].enabled", before: false, after: true },
    ]);
  });
  it("returns empty diff for equal values", () => {
    expect(computeDiff({ a: [1, 2] }, { a: [1, 2] })).toEqual([]);
  });
});

type Tree = { [k: string]: string | Tree };
const keys = (o: Tree, p = ""): string[] =>
  Object.entries(o).flatMap(([k, v]) =>
    typeof v === "string" ? [`${p}${k}`] : keys(v, `${p}${k}.`),
  );

describe("i18n messages", () => {
  it("pl and en have identical keys", () => {
    expect(keys(en as Tree).sort()).toEqual(keys(pl as Tree).sort());
  });
});
