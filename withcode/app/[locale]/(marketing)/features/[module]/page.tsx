import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { features, getFeature } from "@/content/features";
import { commands } from "@/content/commands";
import { routing } from "@/i18n/routing";
import { DiscordMock } from "@/components/marketing/discord-mock";
import { SectionHeading } from "@/components/sections/section-heading";
import { Reveal } from "@/components/motion/reveal";
import { Button } from "@/components/ui/button";
import { featureIcons, ArrowRight } from "@/components/ui/icons";
import { demoPresets } from "@/lib/presets";
import { pageMetadata } from "@/lib/seo/metadata";
import { l } from "@/lib/utils";

export function generateStaticParams() {
  return routing.locales.flatMap((locale) => features.map((f) => ({ locale, module: f.slug })));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string; module: string }>;
}): Promise<Metadata> {
  const { locale, module } = await params;
  const f = getFeature(module);
  if (!f) return {};
  return pageMetadata({
    locale,
    path: `/features/${module}`,
    title: l(f.name, locale),
    description: l(f.description, locale),
  });
}

export default async function FeaturePage({
  params,
}: {
  params: Promise<{ locale: string; module: string }>;
}) {
  const { locale, module } = await params;
  setRequestLocale(locale);
  const f = getFeature(module);
  if (!f) notFound();
  const t = await getTranslations("features");
  const tp = await getTranslations("demo.presetList");
  const Icon = featureIcons[f.icon];
  const related = commands.filter((c) => c.category === f.commandCategory).slice(0, 6);
  const presets = [
    ...f.demoMessages.map((m, i) => ({
      id: `f${i}`,
      label: l(m, locale).slice(0, 28) + (l(m, locale).length > 28 ? "…" : ""),
      messages: [l(m, locale)],
    })),
    ...demoPresets((k) => tp(k), locale).filter((p) => p.id === "spam"),
  ];

  return (
    <>
      <section className="container-x pt-36 pb-16 md:pt-48">
        <Link href="/features" className="eyebrow inline-flex items-center gap-2 hover:text-white">
          ← {t("eyebrow")}
        </Link>
        <div className="mt-8 flex items-center gap-4">
          <span className="inline-flex size-14 items-center justify-center rounded-lg border border-border-strong bg-white/[0.03]">
            <Icon className="size-6 text-white" />
          </span>
        </div>
        <SectionHeading
          as="h1"
          title={l(f.name, locale)}
          subtitle={l(f.description, locale)}
          className="mt-8"
        />
      </section>

      <section className="container-x grid gap-12 pb-20 lg:grid-cols-2">
        <Reveal>
          <h2 className="heading text-2xl text-white">{t("howItWorks")}</h2>
          <ul className="mt-6 space-y-4">
            {f.bullets.map((b) => (
              <li key={b.pl} className="flex gap-4 border-b border-border pb-4 text-fg-muted">
                <span className="mt-2.5 h-px w-4 shrink-0 bg-white" aria-hidden />
                {l(b, locale)}
              </li>
            ))}
          </ul>
        </Reveal>
        <Reveal delay={0.1}>
          <h2 className="heading text-2xl text-white">{t("configure")}</h2>
          <ol className="mt-6 space-y-4">
            {f.steps.map((s, i) => (
              <li key={s.pl} className="flex gap-4">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-full border border-border-strong font-mono text-xs text-white">
                  {i + 1}
                </span>
                <p className="pt-1 text-fg-muted">{l(s, locale)}</p>
              </li>
            ))}
          </ol>
          <Button asChild className="mt-8">
            <Link href="/dashboard" prefetch={false}>
              {t("configure")} <ArrowRight className="size-4" />
            </Link>
          </Button>
        </Reveal>
      </section>

      <section className="container-x pb-20" aria-labelledby="demo-title">
        <h2 id="demo-title" className="heading text-2xl text-white">
          {t("demoTitle")}
        </h2>
        <p className="mt-2 mb-6 text-fg-muted">{t("demoHint")}</p>
        <DiscordMock modules={f.demoModules} presets={presets} compact />
      </section>

      {related.length > 0 ? (
        <section className="container-x pb-20">
          <h2 className="heading text-2xl text-white">{t("commandsTitle")}</h2>
          <ul className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {related.map((c) => (
              <li key={c.name} className="rounded-lg border border-border bg-surface-1 p-4">
                <code className="font-mono text-sm text-white">/{c.name}</code>
                <p className="mt-1.5 text-sm text-fg-muted">{l(c.description, locale)}</p>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <section className="container-x pb-28">
        <h2 className="eyebrow mb-5">{t("otherModules")}</h2>
        <ul className="flex flex-wrap gap-2">
          {features
            .filter((x) => x.slug !== f.slug)
            .map((x) => (
              <li key={x.slug}>
                <Link
                  href={`/features/${x.slug}`}
                  className="inline-flex rounded-full border border-border px-4 py-2 text-sm text-fg-muted transition-colors hover:border-white/40 hover:text-white"
                >
                  {l(x.name, locale)}
                </Link>
              </li>
            ))}
        </ul>
      </section>
    </>
  );
}
