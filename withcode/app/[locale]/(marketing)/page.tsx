import type { Metadata } from "next";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { Hero } from "@/components/sections/hero";
import { Trust } from "@/components/sections/trust";
import { Story } from "@/components/sections/story";
import { FeaturesGrid } from "@/components/sections/features-grid";
import { Stats } from "@/components/sections/stats";
import { Comparison } from "@/components/sections/comparison";
import { Testimonials } from "@/components/sections/testimonials";
import { FaqList } from "@/components/sections/faq";
import { FinalCta } from "@/components/sections/final-cta";
import { SectionHeading } from "@/components/sections/section-heading";
import { IntroLoader } from "@/components/three/loader";
import { FieldBackdrop } from "@/components/sections/field-backdrop";
import { Button } from "@/components/ui/button";
import { Link } from "@/i18n/navigation";
import { getFaq, getPublicStats, getTestimonials } from "@/lib/content";
import { pageMetadata } from "@/lib/seo/metadata";
import { JsonLd, faqPage, softwareApplication } from "@/lib/seo/json-ld";

export const revalidate = 300;

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  return pageMetadata({ locale, path: "/" });
}

export default async function HomePage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations({ locale });
  const [stats, faq, testimonials] = await Promise.all([
    getPublicStats(),
    getFaq("HOME"),
    getTestimonials(),
  ]);
  const faqItems = faq.map((f) => ({
    id: f.id,
    q: locale === "en" ? f.questionEn : f.questionPl,
    a: locale === "en" ? f.answerEn : f.answerPl,
  }));

  return (
    <>
      <IntroLoader />
      <JsonLd data={softwareApplication(locale)} />
      <JsonLd data={faqPage(faqItems)} />
      <FieldBackdrop>
        <Hero />
        <Trust
          locale={locale}
          servers={stats.servers}
          users={stats.users}
          threats={stats.threats}
          sample={stats.sample}
        />
        <Story />
        <section className="section-y" aria-labelledby="features-heading">
          <div className="container-x">
            <div className="flex flex-wrap items-end justify-between gap-8">
              <SectionHeading
                eyebrow={t("home.features.eyebrow")}
                title={t("home.features.title")}
                subtitle={t("home.features.subtitle")}
              />
              <Button asChild variant="secondary">
                <Link href="/features">
                  <span className="relative">{t("home.features.all")}</span>
                </Link>
              </Button>
            </div>
            <div className="mt-14">
              <FeaturesGrid locale={locale} />
            </div>
          </div>
        </section>
        <Stats locale={locale} regions={stats.regions} />
        <Comparison locale={locale} />
        <Testimonials locale={locale} items={testimonials} />
        <section className="section-y" aria-labelledby="faq-heading">
          <div className="container-x grid gap-12 lg:grid-cols-[1fr_2fr]">
            <SectionHeading eyebrow={t("home.faq.eyebrow")} title={t("home.faq.title")} />
            <FaqList items={faqItems} />
          </div>
        </section>
        <FinalCta locale={locale} />
      </FieldBackdrop>
    </>
  );
}
