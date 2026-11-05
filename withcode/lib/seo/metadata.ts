import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { routing } from "@/i18n/routing";
import { siteUrl } from "@/lib/utils";

type MetaKey =
  | "features"
  | "commands"
  | "premium"
  | "docs"
  | "status"
  | "changelog"
  | "demo"
  | "partners"
  | "contact"
  | "terms"
  | "privacy"
  | "cookies"
  | "dashboard"
  | "login"
  | "notFound";

export function localizedPath(locale: string, path: string): string {
  const clean = path === "/" ? "" : path;
  return locale === routing.defaultLocale ? clean || "/" : `/${locale}${clean}`;
}

export function alternates(path: string, locale: string): Metadata["alternates"] {
  return {
    canonical: siteUrl(localizedPath(locale, path)),
    languages: {
      pl: siteUrl(localizedPath("pl", path)),
      en: siteUrl(localizedPath("en", path)),
      "x-default": siteUrl(localizedPath("pl", path)),
    },
  };
}

/** Metadane podstrony: tytuł, opis, hreflang, OG/Twitter (obrazek OG generowany dynamicznie). */
export async function pageMetadata({
  locale,
  path,
  titleKey,
  descriptionKey,
  title,
  description,
  noindex,
}: {
  locale: string;
  path: string;
  titleKey?: MetaKey;
  descriptionKey?: `${MetaKey}Description`;
  title?: string;
  description?: string;
  noindex?: boolean;
}): Promise<Metadata> {
  const t = (await getTranslations({ locale, namespace: "meta" })) as unknown as (
    key: string,
  ) => string;
  const finalTitle = title ?? (titleKey ? t(titleKey) : t("defaultTitle"));
  const finalDescription =
    description ?? (descriptionKey ? t(descriptionKey) : t("defaultDescription"));
  const og = `/api/og?title=${encodeURIComponent(finalTitle)}&subtitle=${encodeURIComponent(finalDescription.slice(0, 120))}`;
  return {
    title: finalTitle,
    description: finalDescription,
    alternates: alternates(path, locale),
    openGraph: {
      type: "website",
      siteName: "WithCode.pl",
      locale: locale === "en" ? "en_US" : "pl_PL",
      url: siteUrl(localizedPath(locale, path)),
      title: finalTitle,
      description: finalDescription,
      images: [{ url: og, width: 1200, height: 630, alt: finalTitle }],
    },
    twitter: {
      card: "summary_large_image",
      title: finalTitle,
      description: finalDescription,
      images: [og],
    },
    robots: noindex ? { index: false, follow: false } : undefined,
  };
}
