'use client';

import { formatPrice } from '@/lib/format';

interface Props {
  min: number;
  max: number;
  step: number;
  value: [number, number];
  onChange: (v: [number, number]) => void;
}

/** Suwak zakresu ceny z dwoma uchwytami (dwa natywne input[type=range] – dostępne z klawiatury). */
export function PriceRange({ min, max, step, value, onChange }: Props) {
  const [lo, hi] = value;
  const pct = (v: number) => ((v - min) / (max - min)) * 100;
  const thumb =
    'pointer-events-none absolute inset-0 h-full w-full appearance-none bg-transparent [&::-moz-range-thumb]:pointer-events-auto [&::-moz-range-thumb]:h-5 [&::-moz-range-thumb]:w-5 [&::-moz-range-thumb]:rounded-full [&::-moz-range-thumb]:border-0 [&::-moz-range-thumb]:bg-acid [&::-webkit-slider-thumb]:pointer-events-auto [&::-webkit-slider-thumb]:h-5 [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-acid [&::-webkit-slider-thumb]:shadow-[0_0_0_4px_rgba(198,255,0,0.2)]';

  return (
    <div className="relative h-6">
      <div className="absolute inset-x-0 top-1/2 h-1 -translate-y-1/2 rounded-full bg-white/10" aria-hidden />
      <div
        className="absolute top-1/2 h-1 -translate-y-1/2 rounded-full bg-acid"
        style={{ left: `${pct(lo)}%`, right: `${100 - pct(hi)}%` }}
        aria-hidden
      />
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={lo}
        onChange={(e) => onChange([Math.min(Number(e.target.value), hi - step), hi])}
        className={thumb}
        aria-label="Cena minimalna"
        aria-valuetext={formatPrice(lo)}
      />
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={hi}
        onChange={(e) => onChange([lo, Math.max(Number(e.target.value), lo + step)])}
        className={thumb}
        aria-label="Cena maksymalna"
        aria-valuetext={formatPrice(hi)}
      />
    </div>
  );
}
