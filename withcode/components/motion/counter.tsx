"use client";
import { useEffect, useRef, useState } from "react";

/** Licznik animowany przy wejściu w viewport. Tekst końcowy jest w SSR (bez CLS, czytelny bez JS). */
export function Counter({
  value,
  locale,
  compact = false,
  suffix = "",
  decimals = 0,
  duration = 1800,
}: {
  value: number;
  locale: string;
  compact?: boolean;
  suffix?: string;
  decimals?: number;
  duration?: number;
}) {
  const fmt = new Intl.NumberFormat(locale === "en" ? "en-US" : "pl-PL", {
    notation: compact ? "compact" : "standard",
    maximumFractionDigits: compact ? 1 : decimals,
    minimumFractionDigits: decimals,
  });
  const [display, setDisplay] = useState(() => fmt.format(value));
  const ref = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    let raf = 0;
    const io = new IntersectionObserver(
      ([e]) => {
        if (!e?.isIntersecting) return;
        io.disconnect();
        const start = performance.now();
        const step = (now: number) => {
          const t = Math.min(1, (now - start) / duration);
          const eased = 1 - Math.pow(1 - t, 4);
          setDisplay(fmt.format(value * eased));
          if (t < 1) raf = requestAnimationFrame(step);
        };
        setDisplay(fmt.format(0));
        raf = requestAnimationFrame(step);
      },
      { threshold: 0.4 },
    );
    io.observe(el);
    return () => {
      io.disconnect();
      cancelAnimationFrame(raf);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, duration]);

  return (
    <span ref={ref} className="tabular-nums">
      {display}
      {suffix}
    </span>
  );
}
