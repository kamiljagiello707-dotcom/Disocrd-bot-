'use client';

import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { useScene } from '@/store/scene';
import { useScrollLock } from '@/hooks/useScrollLock';

const LETTERS = 'REASEL'.split('');

/**
 * Ekran ładowania z licznikiem procentowym i animowanym logo.
 * Licznik łączy realny postęp assetów 3D (useProgress w scenie) z płynną
 * animacją czasową; znika dopiero gdy scena zgłosi gotowość (lub po timeoucie).
 */
export function Preloader() {
  const setIntroDone = useScene((s) => s.setIntroDone);
  const [display, setDisplay] = useState(0);
  const [visible, setVisible] = useState(true);
  const forcedRef = useRef(false);
  const counterRef = useRef<HTMLSpanElement>(null);

  useScrollLock(visible);

  // Jedna pętla rAF przez cały czas życia preloadera (stan czytany przez getState)
  useEffect(() => {
    const start = performance.now();
    // Zabezpieczenie – np. brak WebGL lub bardzo wolna sieć
    const timeout = window.setTimeout(() => (forcedRef.current = true), 8000);
    let raf = 0;
    let value = 0;
    const loop = () => {
      const s = useScene.getState();
      const elapsed = (performance.now() - start) / 1000;
      const timeBased = Math.min(88, 100 * (1 - Math.exp(-elapsed * 1.1)));
      const done = s.ready || forcedRef.current;
      const target = done ? 100 : Math.max(timeBased, Math.min(95, s.progress));
      value += (target - value) * (done ? 0.12 : 0.06);
      if (done && value > 99.5) value = 100;
      const rounded = Math.round(value);
      if (counterRef.current) counterRef.current.textContent = String(rounded).padStart(3, '0');
      setDisplay((d) => (d === rounded ? d : rounded));
      if (rounded < 100) raf = requestAnimationFrame(loop);
    };
    raf = requestAnimationFrame(loop);
    return () => {
      cancelAnimationFrame(raf);
      window.clearTimeout(timeout);
    };
  }, []);

  useEffect(() => {
    if (display >= 100 && visible) {
      const t = window.setTimeout(() => setVisible(false), 350);
      return () => window.clearTimeout(t);
    }
  }, [display, visible]);

  return (
    <AnimatePresence onExitComplete={() => setIntroDone(true)}>
      {visible && (
        <motion.div
          key="preloader"
          role="status"
          aria-live="polite"
          aria-label={`Ładowanie strony: ${display}%`}
          className="fixed inset-0 z-[110] flex flex-col justify-between overflow-hidden bg-ink p-6 md:p-12"
          exit={{ clipPath: 'inset(0 0 100% 0)' }}
          initial={{ clipPath: 'inset(0 0 0% 0)' }}
          transition={{ duration: 1, ease: [0.76, 0, 0.24, 1] }}
        >
          <div className="flex justify-between text-xs uppercase tracking-[0.25em] text-ink-300">
            <span>Reasel.pl</span>
            <span>Kolekcja FW26</span>
          </div>

          <div className="flex flex-col items-center">
            <div className="flex overflow-hidden" aria-hidden>
              {LETTERS.map((l, i) => (
                <motion.span
                  key={i}
                  className="h-display text-[18vw] leading-[0.85] md:text-[13vw]"
                  initial={{ y: '110%' }}
                  animate={{ y: '0%' }}
                  exit={{ y: '-110%' }}
                  transition={{ delay: 0.08 * i, duration: 1, ease: [0.16, 1, 0.3, 1] }}
                  style={{ color: i === LETTERS.length - 1 ? '#c6ff00' : undefined }}
                >
                  {l}
                </motion.span>
              ))}
            </div>
            <div className="mt-8 h-px w-full max-w-md overflow-hidden bg-white/10">
              <div className="h-full bg-acid transition-[width] duration-200 ease-linear" style={{ width: `${display}%` }} />
            </div>
          </div>

          <div className="flex items-end justify-between">
            <p className="max-w-[16rem] text-xs leading-relaxed text-ink-400">
              Przygotowujemy scenę 3D. Najlepiej oglądać ze słuchawkami i w ciemnym pokoju.
            </p>
            <span className="h-display text-6xl tabular-nums md:text-8xl" aria-hidden>
              <span ref={counterRef}>000</span>
              <span className="text-acid">%</span>
            </span>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
