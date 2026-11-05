import type { Metadata } from "next";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { PageHeader } from "@/components/sections/section-heading";
import { DiscordMock } from "@/components/marketing/discord-mock";
import { demoPresets } from "@/lib/presets";
import { autoModModules } from "@/lib/validators/automod";
import { pageMetadata } from "@/lib/seo/metadata";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  return pageMetadata({
    locale,
    path: "/demo",
    titleKey: "demo",
    descriptionKey: "demoDescription",
  });
}

export default async function DemoPage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("demo");
  const tp = await getTranslations("demo.presetList");
  return (
    <>
      <PageHeader eyebrow={t("eyebrow")} title={t("title")} subtitle={t("subtitle")} />
      <section className="container-x pb-24">
        <DiscordMock modules={[...autoModModules]} presets={demoPresets((k) => tp(k), locale)} />
      </section>
    </>
  );
}
