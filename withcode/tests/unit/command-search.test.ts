import { describe, expect, it } from "vitest";
import { commands, commandCategories } from "@/content/commands";
import { createCommandSearch } from "@/lib/search/commands";

describe("command search", () => {
  const pl = createCommandSearch(commands, "pl");
  const en = createCommandSearch(commands, "en");

  it("returns all commands for empty query", () => {
    expect(pl("")).toHaveLength(commands.length);
  });
  it("finds exact name and strips leading slash", () => {
    expect(pl("/ban")[0]?.name).toBe("ban");
  });
  it("tolerates typos", () => {
    expect(pl("lockdwon").map((c) => c.name)).toContain("lockdown");
    expect(pl("timout").map((c) => c.name)).toContain("timeout");
  });
  it("matches aliases", () => {
    expect(pl("mute")[0]?.name).toBe("timeout");
  });
  it("searches localized descriptions", () => {
    expect(pl("wycisza").map((c) => c.name)).toContain("timeout");
    expect(en("times a user out").map((c) => c.name)).toContain("timeout");
  });
  it("filters by category", () => {
    const res = pl("", "tickets");
    expect(res.length).toBeGreaterThan(0);
    expect(res.every((c) => c.category === "tickets")).toBe(true);
  });
  it("every command has a valid category and unique name", () => {
    const ids = new Set(commandCategories.map((c) => c.id));
    expect(commands.every((c) => ids.has(c.category))).toBe(true);
    expect(new Set(commands.map((c) => c.name)).size).toBe(commands.length);
  });
});
