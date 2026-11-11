'use client';

import type Lenis from 'lenis';

/**
 * Globalna referencja do instancji Lenis (smooth scroll).
 * Ustawiana w `SmoothScroll`, używana do blokowania scrolla (drawer, overlay)
 * oraz do odczytu prędkości (marquee, cząsteczki).
 */
let instance: Lenis | null = null;

export const lenisRef = {
  get: () => instance,
  set: (l: Lenis | null) => {
    instance = l;
  },
};

let lockCount = 0;

/** Blokuje scroll strony (zagnieżdżalne – np. drawer otwarty na overlayu). */
export function lockScroll() {
  lockCount++;
  instance?.stop();
  document.documentElement.classList.add('scroll-locked');
}

export function unlockScroll() {
  lockCount = Math.max(0, lockCount - 1);
  if (lockCount === 0) {
    instance?.start();
    document.documentElement.classList.remove('scroll-locked');
  }
}

export function scrollToTop(immediate = true) {
  if (instance) instance.scrollTo(0, { immediate, force: true });
  else window.scrollTo({ top: 0, behavior: immediate ? 'auto' : 'smooth' });
}
