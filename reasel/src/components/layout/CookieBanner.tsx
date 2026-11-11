'use client';

import { useEffect, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { useScene } from '@/store/scene';

const KEY = 'reasel-cookie-consent';

type Consent = { necessary: true; analytics: boolean; marketing: boolean; date: string };

function readConsent(): Consent | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Consent) : null;
  } catch {
    return null;
  }
}

/** Baner zgód na cookies zgodny z RODO: domyślnie tylko niezbędne, wybór kategorii. */
export function CookieBanner() {
  const introDone = useScene((s) => s.introDone);
  const [visible, setVisible] = useState(false);
  const [custom, setCustom] = useState(false);
  const [analytics, setAnalytics] = useState(false);
  const [marketing, setMarketing] = useState(false);

  useEffect(() => {
    if (introDone && !readConsent()) {
      const t = window.setTimeout(() => setVisible(true), 1200);
      return () => window.clearTimeout(t);
    }
  }, [introDone]);

  const save = (c: Omit<Consent, 'necessary' | 'date'>) => {
    try {
      localStorage.setItem(KEY, JSON.stringify({ necessary: true, ...c, date: new Date().toISOString() }));
    } catch {
      /* tryb prywatny – ignorujemy */
    }
    setVisible(false);
    // Tu można zainicjować analitykę (np. GA4 / Meta Pixel) zgodnie z wyrażoną zgodą
  };

  return (
    <AnimatePresence>
      {visible && (
        <motion.section
          role="region"
          aria-label="Zgoda na pliki cookies"
          className="fixed bottom-3 left-3 right-3 z-[75] max-w-md rounded-2xl border border-white/10 bg-ink-900/95 p-5 shadow-2xl backdrop-blur-xl sm:bottom-6 sm:left-6 sm:right-auto sm:p-6"
          initial={{ y: 40, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: 40, opacity: 0 }}
        >
          <h2 className="h-display text-lg">Ciasteczka 🍪</h2>
          <p className="mt-2 text-[13px] leading-relaxed text-ink-200 sm:text-sm">
            Używamy plików cookies niezbędnych do działania sklepu oraz – za Twoją zgodą – analitycznych i
            marketingowych. Szczegóły w{' '}
            <TransitionLink href="/polityka-prywatnosci" className="text-acid underline underline-offset-4">
              polityce prywatności
            </TransitionLink>
            .
          </p>

          {custom && (
            <fieldset className="mt-4 space-y-3 text-sm">
              <legend className="sr-only">Kategorie plików cookies</legend>
              <label className="flex items-center justify-between gap-4 opacity-70">
                <span>Niezbędne (zawsze aktywne)</span>
                <input type="checkbox" checked disabled className="h-4 w-4 accent-[#c6ff00]" />
              </label>
              <label className="flex items-center justify-between gap-4">
                <span>Analityczne</span>
                <input type="checkbox" checked={analytics} onChange={(e) => setAnalytics(e.target.checked)} className="h-4 w-4 accent-[#c6ff00]" />
              </label>
              <label className="flex items-center justify-between gap-4">
                <span>Marketingowe</span>
                <input type="checkbox" checked={marketing} onChange={(e) => setMarketing(e.target.checked)} className="h-4 w-4 accent-[#c6ff00]" />
              </label>
            </fieldset>
          )}

          <div className="mt-4 flex flex-wrap items-center gap-2">
            <button type="button" className="btn-primary px-4 py-2.5 text-[11px]" onClick={() => save({ analytics: true, marketing: true })}>
              Akceptuję wszystkie
            </button>
            {custom ? (
              <button type="button" className="btn-ghost px-4 py-2.5 text-[11px]" onClick={() => save({ analytics, marketing })}>
                Zapisz wybór
              </button>
            ) : (
              <button type="button" className="btn-ghost px-4 py-2.5 text-[11px]" onClick={() => setCustom(true)}>
                Ustawienia
              </button>
            )}
            <button type="button" className="px-2 py-2.5 text-[11px] uppercase tracking-[0.14em] text-ink-300 hover:text-white" onClick={() => save({ analytics: false, marketing: false })}>
              Tylko niezbędne
            </button>
          </div>
        </motion.section>
      )}
    </AnimatePresence>
  );
}
