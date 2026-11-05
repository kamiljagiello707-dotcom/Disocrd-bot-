import Fuse, { type IFuseOptions } from "fuse.js";
import type { Command, CommandCategory } from "@/content/commands";

export type SearchableCommand = Command & { descriptionText: string };

const options: IFuseOptions<SearchableCommand> = {
  keys: [
    { name: "name", weight: 0.5 },
    { name: "aliases", weight: 0.25 },
    { name: "descriptionText", weight: 0.2 },
    { name: "syntax", weight: 0.05 },
  ],
  threshold: 0.38,
  ignoreLocation: true,
  minMatchCharLength: 2,
};

export function createCommandSearch(list: Command[], locale: string) {
  const items: SearchableCommand[] = list.map((c) => ({
    ...c,
    descriptionText: locale === "en" ? c.description.en : c.description.pl,
  }));
  const fuse = new Fuse(items, options);

  return function search(
    query: string,
    category: CommandCategory | "all" = "all",
  ): SearchableCommand[] {
    const q = query.trim().replace(/^\//, "");
    const base = q.length === 0 ? items : fuse.search(q).map((r) => r.item);
    return category === "all" ? base : base.filter((c) => c.category === category);
  };
}
