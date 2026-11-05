"use client";
import { useRef } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/ui/button";
import { ArrowRight, Discord } from "@/components/ui/icons";
import { SplitReveal } from "@/components/motion/split-reveal";
import { Scramble } from "@/components/motion/scramble";
import { SceneSlot } from "@/components/three/scene-slot";
import { HeroFallback } from "@/components/three/fallback";
import { inviteUrl } from "@/lib/utils";

export function Hero() {
  const t = useTranslations();
  const ref = useRef<HTMLElement>(null);
  return (
    <section
      ref={ref}
      className="relative flex min-h-[100svh] items-end overflow-hidden pt-32 pb-16 md:items-center md:pb-0"
      aria-labelledby="hero-title"
    >
      <SceneSlot scene="hero" track={ref} />
      <HeroFallback />
      <p className="sr-only">{t("home.hero.sceneAlt")}</p>
      <div className="container-x relative">
        <p className="eyebrow mb-6 flex items-center gap-3">
          <span className="inline-block size-1.5 animate-pulse rounded-full bg-white" aria-hidden />
          <Scramble text={t("home.hero.eyebrow")} trigger="mount" />
        </p>
        <h1 id="hero-title" className="display max-w-[12ch] text-white">
          <span className="block">{t("home.hero.titleA")}</span>
          <SplitReveal as="span" by="chars" immediate delay={0.15} className="block text-white/50">
            {t("home.hero.titleB")}
          </SplitReveal>
        </h1>
        <p className="mt-8 max-w-xl text-lg leading-relaxed text-fg-muted max-md:text-base">
          {t("home.hero.subtitle")}
        </p>
        <div className="mt-10 flex flex-wrap items-center gap-3">
          <Button asChild size="lg">
            <a href={inviteUrl()} target="_blank" rel="noopener noreferrer">
              <Discord className="size-5" />
              {t("common.addToDiscord")}
            </a>
          </Button>
          <Button asChild size="lg" variant="secondary">
            <Link href="/dashboard" prefetch={false}>
              <span className="relative inline-flex items-center gap-2">
                {t("common.dashboard")}
                <ArrowRight className="size-4 transition-transform duration-300 group-hover/btn:translate-x-1" />
              </span>
            </Link>
          </Button>
        </div>
      </div>
      <div
        aria-hidden
        className="absolute right-[var(--gutter)] bottom-8 hidden items-center gap-3 md:flex"
      >
        <span className="eyebrow">{t("home.hero.scroll")}</span>
        <span className="relative h-12 w-px overflow-hidden bg-white/15">
          <span className="absolute inset-x-0 top-0 h-1/2 animate-[scroll-hint_1.8s_var(--ease-in-out-quart)_infinite] bg-white" />
        </span>
      </div>
    </section>
  );
}
