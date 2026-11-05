"use client";
import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { gsap, ScrollTrigger, useGSAP } from "@/lib/gsap";
import { SceneSlot } from "@/components/three/scene-slot";
import { useScrollStore } from "@/lib/stores/scroll";
import { cn } from "@/lib/utils";

const STEPS = ["s1", "s2", "s3", "s4"] as const;

/** Scroll-driven storytelling: sekcja przypięta, scena 3D i tekst zsynchronizowane przez ScrollTrigger. */
export function Story() {
  const t = useTranslations("home.story");
  const section = useRef<HTMLElement>(null);
  const stage = useRef<HTMLDivElement>(null);
  const [active, setActive] = useState(0);

  useGSAP(
    () => {
      const el = section.current;
      if (!el) return;
      const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
      if (reduce) {
        useScrollStore.setState({ storyProgress: 0.9 });
        return;
      }
      const st = ScrollTrigger.create({
        trigger: el,
        start: "top top",
        end: "+=320%",
        pin: stage.current,
        scrub: true,
        anticipatePin: 1,
        onUpdate: (self) => {
          useScrollStore.setState({ storyProgress: self.progress });
          const idx = Math.min(STEPS.length - 1, Math.floor(self.progress * STEPS.length));
          setActive((prev) => (prev === idx ? prev : idx));
        },
      });
      gsap.fromTo(
        ".story-bar",
        { scaleX: 0 },
        {
          scaleX: 1,
          ease: "none",
          scrollTrigger: { trigger: el, start: "top top", end: "+=320%", scrub: true },
        },
      );
      return () => st.kill();
    },
    { scope: section },
  );

  return (
    <section ref={section} className="relative" aria-labelledby="story-title">
      <SceneSlot scene="story" track={section} rootMargin="0px" />
      <div ref={stage} className="relative flex min-h-[100svh] items-center">
        <p className="sr-only">{t("sceneAlt")}</p>
        <div className="container-x grid gap-10 py-24 md:grid-cols-[minmax(0,26rem)_1fr]">
          <div>
            <p className="eyebrow mb-5">{t("eyebrow")}</p>
            <h2 id="story-title" className="heading text-3xl text-white">
              {t("title")}
            </h2>
            <ol className="relative mt-10 grid">
              {STEPS.map((s, i) => (
                <li
                  key={s}
                  aria-current={active === i ? "step" : undefined}
                  className={cn(
                    "col-start-1 row-start-1 transition-[opacity,transform,filter] duration-700 ease-out-expo motion-reduce:static motion-reduce:opacity-100 motion-reduce:blur-none",
                    active === i
                      ? "blur-0 translate-y-0 opacity-100"
                      : "pointer-events-none translate-y-4 opacity-0 blur-sm",
                  )}
                >
                  <p className="font-mono text-xs text-fg-subtle">0{i + 1} / 04</p>
                  <h3 className="heading mt-3 text-2xl text-white">{t(`steps.${s}.title`)}</h3>
                  <p className="mt-3 max-w-md leading-relaxed text-fg-muted">
                    {t(`steps.${s}.body`)}
                  </p>
                </li>
              ))}
            </ol>
            <div className="mt-10 flex items-center gap-4" aria-hidden>
              <div className="h-px flex-1 overflow-hidden bg-white/15">
                <div className="story-bar h-px origin-left bg-white" />
              </div>
              <div className="flex gap-1.5">
                {STEPS.map((s, i) => (
                  <span
                    key={s}
                    className={cn(
                      "size-1.5 rounded-full transition-colors",
                      i <= active ? "bg-white" : "bg-white/20",
                    )}
                  />
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
