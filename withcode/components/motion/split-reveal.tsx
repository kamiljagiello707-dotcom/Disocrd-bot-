"use client";
import { useRef, type ElementType } from "react";
import { gsap, SplitText, useGSAP } from "@/lib/gsap";
import { cn } from "@/lib/utils";

type Props = {
  as?: ElementType;
  children: string;
  className?: string;
  /** chars: znak po znaku, lines: linia po linii, words: słowo po słowie */
  by?: "chars" | "words" | "lines";
  delay?: number;
  /** odpal od razu (hero) zamiast po wejściu w viewport */
  immediate?: boolean;
  stagger?: number;
};

/**
 * Animowany nagłówek. Pełny tekst zostaje w DOM (SSR, SEO, LCP), a SplitText dzieli go
 * po stronie klienta. Czytniki ekranu czytają ukrytą kopię (sr-only), animowana jest aria-hidden.
 */
export function SplitReveal({
  as: Tag = "h2",
  children,
  className,
  by = "lines",
  delay = 0,
  immediate,
  stagger,
}: Props) {
  const ref = useRef<HTMLSpanElement>(null);

  useGSAP(
    () => {
      const el = ref.current;
      if (!el) return;
      if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
      let split: SplitText | null = null;
      const init = () => {
        split = SplitText.create(el, {
          type: by === "chars" ? "lines,chars" : by === "words" ? "lines,words" : "lines",
          mask: "lines",
          linesClass: "split-line",
          aria: "none",
          autoSplit: true,
          onSplit(self) {
            const targets = by === "chars" ? self.chars : by === "words" ? self.words : self.lines;
            return gsap.from(targets, {
              yPercent: 110,
              rotate: by === "chars" ? 4 : 0,
              duration: by === "chars" ? 1 : 1.1,
              ease: "expo.out",
              stagger: stagger ?? (by === "chars" ? 0.022 : by === "words" ? 0.04 : 0.09),
              delay,
              scrollTrigger: immediate ? undefined : { trigger: el, start: "top 85%", once: true },
            });
          },
        });
      };
      if (immediate) {
        init();
        return () => split?.revert();
      }
      // Podział tekstu (kosztowny layout) dopiero, gdy nagłówek zbliża się do ekranu — lżejsza hydratacja.
      const io = new IntersectionObserver(
        ([entry]) => {
          if (!entry?.isIntersecting) return;
          io.disconnect();
          init();
        },
        { rootMargin: "0px 0px 25% 0px" },
      );
      io.observe(el);
      return () => {
        io.disconnect();
        split?.revert();
      };
    },
    { scope: ref, dependencies: [children] },
  );

  return (
    <Tag className={cn(className)}>
      <span className="sr-only">{children}</span>
      <span ref={ref} aria-hidden className="block">
        {children}
      </span>
    </Tag>
  );
}
