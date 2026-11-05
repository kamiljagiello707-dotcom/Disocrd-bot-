"use client";
import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { LogoMark } from "@/components/ui/logo";
import { useThreeStore } from "./store";

/**
 * Minimalistyczny loader przy pierwszym wejściu: znak + procent w rogu.
 * Nie zasłania nagłówka (LCP) i znika najpóźniej po 2,5 s.
 */
export function IntroLoader() {
  const t = useTranslations("loader");
  const ready = useThreeStore((s) => s.ready);
  const webgl = useThreeStore((s) => s.webgl);
  const [show, setShow] = useState(false);
  const [pct, setPct] = useState(0);
  const [fonts, setFonts] = useState(false);

  useEffect(() => {
    try {
      if (sessionStorage.getItem("wc_intro")) return;
      sessionStorage.setItem("wc_intro", "1");
    } catch {
      /* prywatny tryb — po prostu pokaż */
    }
    // eslint-disable-next-line react-hooks/set-state-in-effect -- sessionStorage dostępny dopiero po hydratacji
    setShow(true);
    void document.fonts.ready.then(() => setFonts(true));
    const hard = setTimeout(() => setShow(false), 2500);
    return () => clearTimeout(hard);
  }, []);

  useEffect(() => {
    if (!show) return;
    const target =
      (fonts ? 35 : 15) + (webgl !== "unknown" ? 15 : 0) + (ready || webgl === "off" ? 50 : 0);
    const id = setInterval(() => {
      setPct((p) => {
        const next = p + Math.max(1, (target - p) * 0.2);
        return Math.min(target, Math.round(next));
      });
    }, 40);
    return () => clearInterval(id);
  }, [show, fonts, ready, webgl]);

  useEffect(() => {
    if (pct >= 100) {
      const id = setTimeout(() => setShow(false), 350);
      return () => clearTimeout(id);
    }
  }, [pct]);

  if (!show) return null;
  return (
    <div
      role="progressbar"
      aria-label={t("label")}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={pct}
      className="pointer-events-none fixed bottom-6 left-6 z-[var(--z-overlay)] flex items-center gap-3 transition-opacity duration-500"
      style={{ opacity: pct >= 100 ? 0 : 1 }}
    >
      <LogoMark className="size-8 animate-pulse" />
      <div className="w-28">
        <div className="h-px w-full bg-white/15">
          <div
            className="h-px bg-white transition-[width] duration-200"
            style={{ width: `${pct}%` }}
          />
        </div>
        <p className="mt-1.5 font-mono text-[11px] text-fg-muted tabular-nums">
          {String(pct).padStart(3, "0")}%
        </p>
      </div>
    </div>
  );
}
