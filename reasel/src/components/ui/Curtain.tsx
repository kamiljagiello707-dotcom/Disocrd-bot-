'use client';

import { useEffect, useRef } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { usePathname, useRouter } from 'next/navigation';
import { useTransition } from '@/store/transition';
import { scrollToTop } from '@/lib/lenis';
import { ScrollTrigger } from '@/lib/gsap';

const EASE = [0.76, 0, 0.24, 1] as const;

/**
 * Kurtyna przejść między stronami.
 * 1) faza „cover” – panel wjeżdża od dołu z zaokrągloną krawędzią,
 * 2) po zakryciu: router.push + scroll na górę,
 * 3) gdy zmieni się pathname – faza „reveal”, panel odjeżdża w górę.
 * Scena 3D (GlobalScene) żyje w layoucie, więc nie jest przeładowywana – ciągłość zachowana.
 */
export function Curtain() {
  const router = useRouter();
  const pathname = usePathname();
  const { phase, href, setPhase } = useTransition();
  const fromPath = useRef(pathname);

  // Prefetch docelowej strony już w momencie startu animacji
  useEffect(() => {
    if (phase === 'cover' && href) router.prefetch(href);
  }, [phase, href, router]);

  useEffect(() => {
    if (phase === 'cover' && pathname !== fromPath.current) {
      setPhase('reveal');
    }
    if (phase === 'idle') fromPath.current = pathname;
  }, [pathname, phase, setPhase]);

  const onCovered = () => {
    if (phase !== 'cover' || !href) return;
    scrollToTop(true);
    router.push(href, { scroll: true });
    // Zabezpieczenie: gdy nawigacja nie zmienia ścieżki (np. ten sam URL)
    window.setTimeout(() => {
      if (useTransition.getState().phase === 'cover') setPhase('reveal');
    }, 2500);
  };

  const onRevealed = () => {
    if (phase === 'reveal') {
      setPhase('idle');
      requestAnimationFrame(() => ScrollTrigger.refresh());
    }
  };

  return (
    <AnimatePresence>
      {phase !== 'idle' && (
        <motion.div
          key="curtain"
          aria-hidden
          className="pointer-events-auto fixed inset-0 z-[80] flex items-center justify-center bg-acid"
          initial={{ clipPath: 'inset(100% 0% 0% 0% round 40% 40% 0 0)' }}
          animate={
            phase === 'cover'
              ? { clipPath: 'inset(0% 0% 0% 0% round 0% 0% 0 0)' }
              : { clipPath: 'inset(0% 0% 100% 0% round 0 0 40% 40%)' }
          }
          transition={{ duration: phase === 'cover' ? 0.7 : 0.8, ease: EASE, delay: phase === 'reveal' ? 0.15 : 0 }}
          onAnimationComplete={phase === 'cover' ? onCovered : onRevealed}
        >
          <motion.span
            className="h-display text-[18vw] leading-none text-ink md:text-[12vw]"
            initial={{ y: '40%', opacity: 0 }}
            animate={phase === 'cover' ? { y: '0%', opacity: 1 } : { y: '-40%', opacity: 0 }}
            transition={{ duration: 0.6, ease: EASE, delay: phase === 'cover' ? 0.15 : 0 }}
          >
            REASEL
          </motion.span>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
