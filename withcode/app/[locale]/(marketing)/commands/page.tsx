import type { Metadata } from "next";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { PageHeader } from "@/components/sections/section-heading";
import { CommandSearch } from "@/components/marketing/command-search";
import { commandCategories, commands } from "@/content/commands";
import { pageMetadata } from "@/lib/seo/metadata";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  return pageMetadata({
    locale,
    path: "/commands",
    titleKey: "commands",
    descriptionKey: "commandsDescription",
  });
}

export default async function CommandsPage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("commands");
  return (
    <>
      <PageHeader
        eyebrow={t("eyebrow")}
        title={t("title")}
        subtitle={t("subtitle", { count: commands.length, categories: commandCategories.length })}
      />
      <section className="container-x pb-28">
        <CommandSearch locale={locale} />
      </section>
    </>
  );
}
