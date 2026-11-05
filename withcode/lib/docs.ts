import "server-only";
import { readFile } from "node:fs/promises";
import path from "node:path";
import GithubSlugger from "github-slugger";
import { docSlugs, docsNav } from "@/content/docs/manifest";

export type TocItem = { id: string; text: string; depth: 2 | 3 };
export type DocIndexEntry = { slug: string; title: string; headings: string[]; text: string };

const lang = (locale: string) => (locale === "en" ? "en" : "pl");

export async function readDocSource(locale: string, slug: string): Promise<string | null> {
  if (!docSlugs.includes(slug)) return null;
  try {
    return await readFile(
      path.join(process.cwd(), "content", "docs", lang(locale), `${slug}.mdx`),
      "utf8",
    );
  } catch {
    return null;
  }
}

export function extractToc(source: string): TocItem[] {
  const slugger = new GithubSlugger();
  const out: TocItem[] = [];
  let inCode = false;
  for (const line of source.split("\n")) {
    if (line.startsWith("```")) inCode = !inCode;
    if (inCode) continue;
    const m = /^(#{1,3})\s+(.+)$/.exec(line);
    if (!m) continue;
    const depth = m[1]!.length;
    const text = m[2]!.trim();
    const id = slugger.slug(text);
    if (depth === 2 || depth === 3) out.push({ id, text, depth });
  }
  return out;
}

function plain(source: string): string {
  return source
    .replace(/```[\s\S]*?```/g, " ")
    .replace(/[#>*`|_[\]()-]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export async function buildDocsIndex(locale: string): Promise<DocIndexEntry[]> {
  const items = docsNav.flatMap((g) => g.items);
  return Promise.all(
    items.map(async (it) => {
      const src = (await readDocSource(locale, it.slug)) ?? "";
      return {
        slug: it.slug,
        title: locale === "en" ? it.title.en : it.title.pl,
        headings: extractToc(src).map((h) => h.text),
        text: plain(src).slice(0, 1200),
      };
    }),
  );
}
