'use client';

import { useEffect } from 'react';
import Lenis from 'lenis';
import { gsap, ScrollTrigger } from '@/lib/gsap';
import { lenisRef } from '@/lib/lenis';
import { sceneState } from '@/store/scene';
import { useReducedMotion } from '@/hooks/useMediaQuery';

/**
 * Smooth scroll (Lenis) zsynchronizowany z GSAP ScrollTrigger.
 * Jeden wspólny ticker GSAP napędza Lenisa – brak podwójnych pętli rAF.
 * Przy `prefers-reduced-motion` Lenis nie jest uruchamiany (natywny scroll).
 */
export function SmoothScroll() {
  const reduced = useReducedMotion();

  useEffect(() => {
    if (reduced) return;

    const lenis = new Lenis({
      duration: 1.15,
      easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
      smoothWheel: true,
      touchMultiplier: 1.4,
    });
    lenisRef.set(lenis);

    lenis.on('scroll', (l: Lenis) => {
      sceneState.velocity = l.velocity;
      ScrollTrigger.update();
    });

    const tick = (time: number) => lenis.raf(time * 1000);
    gsap.ticker.add(tick);
    gsap.ticker.lagSmoothing(0);

    return () => {
      gsap.ticker.remove(tick);
      lenis.destroy();
      lenisRef.set(null);
    };
  }, [reduced]);

  return null;
}
