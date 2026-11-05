import type { Metadata } from "next";
import { getFormatter, getTranslations, setRequestLocale } from "next-intl/server";
import { PageHeader } from "@/components/sections/section-heading";
import { TiltCard } from "@/components/motion/tilt-card";
import { Button } from "@/components/ui/button";
import { Link } from "@/i18n/navigation";
import { getPartners } from "@/lib/content";
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
    path: "/partners",
    titleKey: "partners",
    descriptionKey: "partnersDescription",
  });
}

export default async function PartnersPage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("partners");
  const format = await getFormatter();
  const partners = await getPartners();
  return (
    <>
      <PageHeader eyebrow={t("eyebrow")} title={t("title")} subtitle={t("subtitle")} />
      <section className="container-x pb-20">
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {partners.map((p) => (
            <li key={p.id}>
              <TiltCard className="flex h-full flex-col p-6">
                <div className="flex items-center gap-4">
                  <span
                    className="flex size-12 items-center justify-center rounded-lg border border-border-strong bg-surface-3 font-display text-lg text-white"
                    aria-hidden
                  >
                    {p.name.slice(0, 1)}
                  </span>
                  <div>
                    <h2 className="font-medium text-white">{p.name}</h2>
                    {p.memberCount ? (
                      <p className="text-xs text-fg-subtle">
                        {t("members", { count: format.number(p.memberCount) })}
                      </p>
                    ) : null}
                  </div>
                </div>
                <p className="mt-5 flex-1 text-sm text-fg-muted">
                  {locale === "en" ? p.descEn : p.descPl}
                </p>
                {p.inviteUrl ? (
                  <a
                    href={p.inviteUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="mt-6 text-sm text-white underline decoration-fg-faint underline-offset-4 hover:decoration-white"
                  >
                    {t("join")} ↗
                  </a>
                ) : null}
              </TiltCard>
            </li>
          ))}
        </ul>
      </section>
      <section className="container-x pb-28">
        <div className="glass flex flex-col items-start justify-between gap-6 rounded-xl p-8 md:flex-row md:items-center">
          <div>
            <h2 className="heading text-2xl text-white">{t("become")}</h2>
            <p className="mt-2 max-w-xl text-fg-muted">{t("becomeText")}</p>
          </div>
          <Button asChild>
            <Link href="/contact?topic=PARTNERSHIP">{t("become")}</Link>
          </Button>
        </div>
      </section>
    </>
  );
}
