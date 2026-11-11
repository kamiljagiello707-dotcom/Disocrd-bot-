'use client';

import { useEffect } from 'react';
import { lockScroll, unlockScroll } from '@/lib/lenis';

export function useScrollLock(active: boolean) {
  useEffect(() => {
    if (!active) return;
    lockScroll();
    return () => unlockScroll();
  }, [active]);
}
