import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { routing } from "@/i18n/routing";
import { docSlugs, docsNav } from "@/content/docs/manifest";
import { buildDocsIndex, extractToc, readDocSource } from "@/lib/docs";
import { DocsToc } from "@/components/docs/docs-toc";
import { DocsSearch } from "@/components/docs/docs-search";
import { pageMetadata } from "@/lib/seo/metadata";
import { JsonLd } from "@/lib/seo/json-ld";
import { cn, l, siteUrl } from "@/lib/utils";

export const dynamicParams = false;

export function generateStaticParams() {
  return routing.locales.flatMap((locale) => [
    { locale, slug: [] as string[] },
    ...docSlugs.map((s) => ({ locale, slug: [s] })),
  ]);
}

const resolveSlug = (slug?: string[]) =>
  slug && slug.length > 0 ? slug.join("/") : "introduction";
const allItems = docsNav.flatMap((g) => g.items);

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string; slug?: string[] }>;
}): Promise<Metadata> {
  const { locale, slug } = await params;
  const s = resolveSlug(slug);
  const item = allItems.find((i) => i.slug === s);
  if (!item) return {};
  return pageMetadata({
    locale,
    path: `/docs/${s}`,
    title: l(item.title, locale),
    descriptionKey: "docsDescription",
  });
}

export default async function DocsPage({
  params,
}: {
  params: Promise<{ locale: string; slug?: string[] }>;
}) {
  const { locale, slug } = await params;
  setRequestLocale(locale);
  const s = resolveSlug(slug);
  const idx = allItems.findIndex((i) => i.slug === s);
  const item = allItems[idx];
  const source = await readDocSource(locale, s);
  if (!item || !source) notFound();
  const t = await getTranslations("docs");
  const tc = await getTranslations("common");
  const [{ default: Content }, index] = await Promise.all([
    import(`@/content/docs/${locale === "en" ? "en" : "pl"}/${s}.mdx`) as Promise<{
      default: React.ComponentType;
    }>,
    buildDocsIndex(locale),
  ]);
  const toc = extractToc(source);
  const prev = allItems[idx - 1];
  const next = allItems[idx + 1];

  return (
    <div className="container-x grid gap-10 pt-32 pb-28 md:pt-40 lg:grid-cols-[15rem_minmax(0,1fr)_13rem]">
      <JsonLd
        data={{
          "@context": "https://schema.org",
          "@type": "BreadcrumbList",
          itemListElement: [
            {
              "@type": "ListItem",
              position: 1,
              name: t("eyebrow"),
              item: siteUrl(locale === "en" ? "/en/docs" : "/docs"),
            },
            { "@type": "ListItem", position: 2, name: l(item.title, locale) },
          ],
        }}
      />
      <aside
        className="lg:sticky lg:top-28 lg:h-[calc(100vh-8rem)] lg:overflow-y-auto"
        data-lenis-prevent
      >
        <DocsSearch
          index={index}
          placeholder={t("searchPlaceholder")}
          noResults={tc("noResults")}
        />
        <nav aria-label={t("navLabel")} className="mt-8 space-y-8">
          {docsNav.map((g) => (
            <div key={g.group.pl}>
              <p className="eyebrow mb-3">{l(g.group, locale)}</p>
              <ul className="space-y-1">
                {g.items.map((i) => (
                  <li key={i.slug}>
                    <Link
                      href={`/docs/${i.slug}`}
                      aria-current={i.slug === s ? "page" : undefined}
                      className={cn(
                        "block rounded-md px-3 py-1.5 text-sm transition-colors",
                        i.slug === s
                          ? "bg-white/[0.07] text-white"
                          : "text-fg-muted hover:text-white",
                      )}
                    >
                      {l(i.title, locale)}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>
      </aside>
      <article className="prose-wc min-w-0">
        <Content />
        <hr />
        <nav className="not-prose flex justify-between gap-4" aria-label="Docs pagination">
          {prev ? (
            <Link href={`/docs/${prev.slug}`} className="no-underline">
              <span className="eyebrow block">← {t("previous")}</span>
              <span className="text-white">{l(prev.title, locale)}</span>
            </Link>
          ) : (
            <span />
          )}
          {next ? (
            <Link href={`/docs/${next.slug}`} className="text-right no-underline">
              <span className="eyebrow block">{t("next")} →</span>
              <span className="text-white">{l(next.title, locale)}</span>
            </Link>
          ) : null}
        </nav>
        <p className="text-sm text-fg-subtle">{t("editHint")}</p>
      </article>
      <aside className="hidden lg:block">
        <div className="sticky top-28">
          <DocsToc items={toc} label={t("onThisPage")} />
        </div>
      </aside>
    </div>
  );
}
