'use client';

import { useEffect, type ReactNode } from 'react';
import { MotionConfig } from 'framer-motion';
import { useCart } from '@/store/cart';
import { useFavorites } from '@/store/favorites';
import { SmoothScroll } from './SmoothScroll';

/** Globalne providery: rehydratacja koszyka/ulubionych, smooth scroll, konfiguracja Framer Motion. */
export function Providers({ children }: { children: ReactNode }) {
  useEffect(() => {
    // Odczyt z localStorage dopiero po hydratacji – brak niezgodności SSR
    void useCart.persist.rehydrate();
    void useFavorites.persist.rehydrate();
  }, []);

  return (
    <MotionConfig reducedMotion="user" transition={{ ease: [0.16, 1, 0.3, 1], duration: 0.8 }}>
      <SmoothScroll />
      {children}
    </MotionConfig>
  );
}
