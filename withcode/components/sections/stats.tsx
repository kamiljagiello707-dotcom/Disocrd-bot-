"use client";
import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { SceneSlot } from "@/components/three/scene-slot";
import type { GlobeRegion } from "@/components/three/store";
import { Counter } from "@/components/motion/counter";
import { SplitReveal } from "@/components/motion/split-reveal";

export function Stats({ locale, regions }: { locale: string; regions: GlobeRegion[] }) {
  const t = useTranslations("home.stats");
  const globe = useRef<HTMLDivElement>(null);
  const [data] = useState(() => ({ regions }));
  return (
    <section className="section-y relative" aria-labelledby="stats-title">
      <div className="container-x grid items-center gap-12 lg:grid-cols-2">
        <div>
          <p className="eyebrow mb-5">{t("eyebrow")}</p>
          <SplitReveal as="h2" by="words" className="heading text-4xl text-white">
            {t("title")}
          </SplitReveal>
          <p className="mt-6 max-w-lg text-lg leading-relaxed text-fg-muted max-md:text-base">
            {t("subtitle")}
          </p>
          <dl className="mt-12 grid grid-cols-1 gap-6 sm:grid-cols-3">
            <div className="border-t border-border pt-5">
              <dd className="heading text-3xl text-white">
                <Counter value={99.98} decimals={2} locale={locale} suffix="%" />
              </dd>
              <dt className="mt-2 text-sm text-fg-muted">{t("uptime")}</dt>
            </div>
            <div className="border-t border-border pt-5">
              <dd className="heading text-3xl text-white">
                <Counter value={12} locale={locale} suffix=" ms" />
              </dd>
              <dt className="mt-2 text-sm text-fg-muted">{t("latency")}</dt>
            </div>
            <div className="border-t border-border pt-5">
              <dd className="heading text-3xl text-white">
                <Counter value={38_000_000} compact locale={locale} />
              </dd>
              <dt className="mt-2 text-sm text-fg-muted">{t("messages")}</dt>
            </div>
          </dl>
          <p className="mt-8 text-xs text-fg-subtle">{t("regionNote")}</p>
        </div>
        <div
          ref={globe}
          role="img"
          aria-label={t("globeAlt")}
          className="relative aspect-square w-full cursor-grab touch-pan-y active:cursor-grabbing"
          data-cursor="lg"
        >
          <SceneSlot scene="globe" track={globe} data={data} />
          <div className="webgl-fallback absolute inset-[12%] hidden rounded-full border border-border bg-[radial-gradient(circle_at_35%_30%,rgba(255,255,255,0.12),transparent_60%)]" />
        </div>
      </div>
    </section>
  );
}
