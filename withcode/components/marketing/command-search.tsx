"use client";
import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { useRouter, usePathname } from "next/navigation";
import { AnimatePresence, motion } from "motion/react";
import { commandCategories, commands, type CommandCategory } from "@/content/commands";
import { createCommandSearch } from "@/lib/search/commands";
import { Badge } from "@/components/ui/badge";
import { Kbd } from "@/components/ui/kbd";
import { Copy, Search, Check } from "@/components/ui/icons";
import { cn, l } from "@/lib/utils";

export function CommandSearch({ locale }: { locale: string }) {
  const t = useTranslations("commands");
  const router = useRouter();
  const pathname = usePathname();
  // Strona jest statyczna: pełna lista renderuje się na serwerze (bez CLS), a filtry z URL
  // nakładamy po hydratacji.
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<CommandCategory | "all">("all");
  const [hydrated, setHydrated] = useState(false);
  useEffect(() => {
    const sp = new URLSearchParams(window.location.search);
    const cat = sp.get("category");
    /* eslint-disable react-hooks/set-state-in-effect -- jednorazowy odczyt URL po hydratacji */
    setQuery(sp.get("q") ?? "");
    if (commandCategories.some((c) => c.id === cat)) setCategory(cat as CommandCategory);
    setHydrated(true);
    /* eslint-enable react-hooks/set-state-in-effect */
  }, []);
  const [copied, setCopied] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const search = useMemo(() => createCommandSearch(commands, locale), [locale]);
  const results = useMemo(() => search(query, category), [search, query, category]);

  useEffect(() => {
    if (!hydrated) return;
    const id = setTimeout(() => {
      const sp = new URLSearchParams();
      if (query) sp.set("q", query);
      if (category !== "all") sp.set("category", category);
      const qs = sp.toString();
      router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
    }, 250);
    return () => clearTimeout(id);
  }, [query, category, pathname, router, hydrated]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (
        e.key === "/" &&
        document.activeElement?.tagName !== "INPUT" &&
        document.activeElement?.tagName !== "TEXTAREA"
      ) {
        e.preventDefault();
        inputRef.current?.focus();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  const copy = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(text);
      setTimeout(() => setCopied(null), 1500);
    } catch {
      /* brak uprawnień do schowka */
    }
  };

  return (
    <div>
      <div className="sticky top-16 z-20 -mx-[var(--gutter)] border-b border-border bg-black/80 px-[var(--gutter)] py-4 backdrop-blur-xl md:top-20">
        <div className="relative">
          <Search className="pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2 text-fg-subtle" />
          <label htmlFor="cmd-search" className="sr-only">
            {t("searchPlaceholder")}
          </label>
          <input
            ref={inputRef}
            id="cmd-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t("searchPlaceholder")}
            autoComplete="off"
            className="h-12 w-full rounded-full border border-border-strong bg-surface-1 pr-14 pl-11 text-sm text-white placeholder:text-fg-faint focus-visible:border-white focus-visible:outline-none"
          />
          <span className="absolute top-1/2 right-4 hidden -translate-y-1/2 sm:block">
            <Kbd>/</Kbd>
          </span>
        </div>
        <div
          role="group"
          aria-label={t("eyebrow")}
          className="mt-3 flex [scrollbar-width:none] gap-2 overflow-x-auto pb-1"
        >
          {[
            { id: "all" as const, label: { pl: t("all"), en: t("all") } },
            ...commandCategories,
          ].map((c) => (
            <button
              key={c.id}
              type="button"
              aria-pressed={category === c.id}
              onClick={() => setCategory(c.id)}
              className={cn(
                "shrink-0 rounded-full border px-3.5 py-1.5 text-xs transition-colors",
                category === c.id
                  ? "border-white bg-white text-black"
                  : "border-border text-fg-muted hover:border-white/40 hover:text-white",
              )}
            >
              {l(c.label, locale)}
            </button>
          ))}
        </div>
      </div>
      <p className="mt-6 text-sm text-fg-subtle" aria-live="polite">
        {t("results", { count: results.length })}
      </p>
      <ul className="mt-4 grid gap-3 lg:grid-cols-2">
        <AnimatePresence mode="popLayout" initial={false}>
          {results.map((c) => (
            <motion.li
              key={c.name}
              layout
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.98 }}
              transition={{ duration: 0.25 }}
              className="rounded-lg border border-border bg-surface-1 p-5"
            >
              <div className="flex items-start justify-between gap-3">
                <h3 className="font-mono text-base text-white">/{c.name}</h3>
                <div className="flex shrink-0 gap-1.5">
                  {c.premium ? <Badge variant="solid">{t("premium")}</Badge> : null}
                  <Badge>
                    {l(
                      commandCategories.find((x) => x.id === c.category)?.label ?? {
                        pl: "",
                        en: "",
                      },
                      locale,
                    )}
                  </Badge>
                </div>
              </div>
              <p className="mt-2 text-sm text-fg-muted">{c.descriptionText}</p>
              <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2 [&>div:first-child]:sm:col-span-2 [&>div:nth-child(4)]:sm:col-span-2">
                <div>
                  <dt className="eyebrow mb-1.5">{t("syntax")}</dt>
                  <dd className="flex items-center justify-between gap-2 rounded-md border border-border bg-black px-3 py-2 font-mono text-xs text-white">
                    <span className="overflow-x-auto whitespace-nowrap">{c.syntax}</span>
                    <button
                      type="button"
                      onClick={() => copy(c.syntax)}
                      aria-label={t("copyCommand")}
                      className="shrink-0 text-fg-subtle hover:text-white"
                    >
                      {copied === c.syntax ? (
                        <Check className="size-3.5" />
                      ) : (
                        <Copy className="size-3.5" />
                      )}
                    </button>
                  </dd>
                </div>
                <div>
                  <dt className="eyebrow mb-1.5">{t("permissions")}</dt>
                  <dd className="text-xs text-fg-muted">
                    {c.permissions.length ? c.permissions.join(", ") : t("none")}
                  </dd>
                </div>
                <div>
                  <dt className="eyebrow mb-1.5">{t("example")}</dt>
                  <dd className="font-mono text-xs text-fg-muted">{c.example}</dd>
                </div>
                {c.aliases?.length ? (
                  <div>
                    <dt className="eyebrow mb-1.5">{t("aliases")}</dt>
                    <dd className="font-mono text-xs text-fg-muted">
                      {c.aliases.map((a) => `/${a}`).join(", ")}
                    </dd>
                  </div>
                ) : null}
              </dl>
            </motion.li>
          ))}
        </AnimatePresence>
      </ul>
    </div>
  );
}
