import type { Metadata } from "next";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { PageHeader, SectionHeading } from "@/components/sections/section-heading";
import { Pricing } from "@/components/marketing/pricing";
import { FaqList } from "@/components/sections/faq";
import { Check, X } from "@/components/ui/icons";
import { planComparison, plans } from "@/content/pricing";
import { getFaq } from "@/lib/content";
import { pageMetadata } from "@/lib/seo/metadata";
import { JsonLd, faqPage, softwareApplication } from "@/lib/seo/json-ld";
import { l } from "@/lib/utils";

export const revalidate = 300;

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  return pageMetadata({
    locale,
    path: "/premium",
    titleKey: "premium",
    descriptionKey: "premiumDescription",
  });
}

export default async function PremiumPage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("premium");
  const faq = (await getFaq("PREMIUM")).map((f) => ({
    id: f.id,
    q: locale === "en" ? f.questionEn : f.questionPl,
    a: locale === "en" ? f.answerEn : f.answerPl,
  }));
  return (
    <>
      <JsonLd
        data={softwareApplication(
          locale,
          plans.map((p) => ({ name: p.name, price: p.monthly })),
        )}
      />
      <JsonLd data={faqPage(faq)} />
      <PageHeader eyebrow={t("eyebrow")} title={t("title")} subtitle={t("subtitle")} />
      <section className="container-x pb-24">
        <Pricing plans={plans} locale={locale} />
      </section>
      <section className="container-x pb-24" aria-labelledby="cmp">
        <h2 id="cmp" className="heading text-3xl text-white">
          {t("compareTitle")}
        </h2>
        <div className="mt-8 overflow-x-auto rounded-xl border border-border">
          <table className="w-full min-w-[640px] text-sm">
            <thead className="bg-surface-1">
              <tr>
                <th scope="col" className="px-5 py-4 text-left font-medium text-fg-subtle">
                  <span className="sr-only">Feature</span>
                </th>
                {plans.map((p) => (
                  <th
                    key={p.id}
                    scope="col"
                    className="px-5 py-4 text-center font-semibold text-white"
                  >
                    {p.name}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {planComparison.map((row) => (
                <tr key={row.label.pl}>
                  <th scope="row" className="px-5 py-3.5 text-left font-normal text-fg-muted">
                    {l(row.label, locale)}
                  </th>
                  {row.values.map((v, i) => (
                    <td key={i} className="px-5 py-3.5 text-center text-white">
                      {typeof v === "boolean" ? (
                        v ? (
                          <Check className="mx-auto size-4" aria-label="✓" />
                        ) : (
                          <X className="mx-auto size-4 text-fg-faint" aria-label="✗" />
                        )
                      ) : (
                        v
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <section className="container-x grid gap-12 pb-28 lg:grid-cols-[1fr_2fr]">
        <SectionHeading title={t("faqTitle")} />
        <FaqList items={faq} />
      </section>
    </>
  );
}
