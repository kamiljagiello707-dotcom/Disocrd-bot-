"use client";
import { useState } from "react";
import { useFormatter, useTranslations } from "next-intl";
import type { ChangelogEntryView } from "@/lib/content";
import { Badge } from "@/components/ui/badge";
import { Reveal } from "@/components/motion/reveal";
import { cn } from "@/lib/utils";

const TAGS = ["NEW", "FIX", "CHANGE"] as const;

export function ChangelogList({
  entries,
  locale,
}: {
  entries: ChangelogEntryView[];
  locale: string;
}) {
  const t = useTranslations("changelog");
  const format = useFormatter();
  const [tag, setTag] = useState<(typeof TAGS)[number] | "ALL">("ALL");
  const filtered = entries
    .map((e) => ({ ...e, items: e.items.filter((i) => tag === "ALL" || i.tag === tag) }))
    .filter((e) => e.items.length > 0);
  return (
    <>
      <div className="mb-12 flex flex-wrap gap-2" role="group" aria-label="Filter">
        {(["ALL", ...TAGS] as const).map((x) => (
          <button
            key={x}
            type="button"
            aria-pressed={tag === x}
            onClick={() => setTag(x)}
            className={cn(
              "rounded-full border px-4 py-1.5 text-sm transition-colors",
              tag === x
                ? "border-white bg-white text-black"
                : "border-border text-fg-muted hover:text-white",
            )}
          >
            {x === "ALL" ? t("all") : t(`tags.${x}`)}
          </button>
        ))}
      </div>
      <ol className="relative space-y-14 border-l border-border pl-8 md:pl-12">
        {filtered.map((e) => (
          <li key={e.id} className="relative">
            <span
              className="absolute top-2 -left-[2.3rem] size-2.5 rounded-full border-2 border-black bg-white md:-left-[3.3rem]"
              aria-hidden
            />
            <Reveal>
              <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1">
                <h2 className="heading text-3xl text-white">v{e.version}</h2>
                <time dateTime={e.publishedAt} className="font-mono text-xs text-fg-subtle">
                  {format.dateTime(new Date(e.publishedAt), { dateStyle: "long" })}
                </time>
              </div>
              <p className="mt-2 text-lg text-fg-muted">
                {locale === "en" ? e.titleEn : e.titlePl}
              </p>
              <ul className="mt-6 space-y-3">
                {e.items.map((i) => (
                  <li key={i.id} className="flex items-start gap-3">
                    <Badge
                      variant={i.tag === "NEW" ? "solid" : "default"}
                      className="mt-0.5 w-24 shrink-0 justify-center"
                    >
                      {t(`tags.${i.tag}`)}
                    </Badge>
                    <span className="text-fg-muted">{locale === "en" ? i.textEn : i.textPl}</span>
                  </li>
                ))}
              </ul>
            </Reveal>
          </li>
        ))}
      </ol>
    </>
  );
}
