import type { Metadata } from "next";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { PageHeader } from "@/components/sections/section-heading";
import { ChangelogList } from "@/components/marketing/changelog-list";
import { getChangelog } from "@/lib/content";
import { pageMetadata } from "@/lib/seo/metadata";

export const revalidate = 300;

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  return pageMetadata({
    locale,
    path: "/changelog",
    titleKey: "changelog",
    descriptionKey: "changelogDescription",
  });
}

export default async function ChangelogPage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("changelog");
  const entries = await getChangelog();
  return (
    <>
      <PageHeader eyebrow={t("eyebrow")} title={t("title")} subtitle={t("subtitle")} />
      <section className="container-x pb-28">
        <ChangelogList entries={entries} locale={locale} />
      </section>
    </>
  );
}
