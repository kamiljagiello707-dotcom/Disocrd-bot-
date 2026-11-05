import type { Metadata } from "next";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { PageHeader } from "@/components/sections/section-heading";
import { ContactForm } from "@/components/marketing/contact-form";
import { Button } from "@/components/ui/button";
import { Discord } from "@/components/ui/icons";
import { pageMetadata } from "@/lib/seo/metadata";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  return pageMetadata({
    locale,
    path: "/contact",
    titleKey: "contact",
    descriptionKey: "contactDescription",
  });
}

export default async function ContactPage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("contact");
  const support = process.env.NEXT_PUBLIC_SUPPORT_SERVER_URL ?? "https://discord.gg/withcode";
  const siteKey = process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY;
  return (
    <>
      <PageHeader eyebrow={t("eyebrow")} title={t("title")} subtitle={t("subtitle")} />
      <section className="container-x grid gap-8 pb-28 lg:grid-cols-[2fr_1fr]">
        <ContactForm {...(siteKey ? { turnstileSiteKey: siteKey } : {})} />
        <aside className="h-fit rounded-xl border border-border bg-surface-1 p-6 md:p-8">
          <Discord className="size-8 text-white" />
          <h2 className="heading mt-6 text-2xl text-white">{t("supportTitle")}</h2>
          <p className="mt-2 text-fg-muted">{t("supportText")}</p>
          <Button asChild variant="secondary" className="mt-8">
            <a href={support} target="_blank" rel="noopener noreferrer">
              <span className="relative">{t("joinSupport")}</span>
            </a>
          </Button>
        </aside>
      </section>
    </>
  );
}
