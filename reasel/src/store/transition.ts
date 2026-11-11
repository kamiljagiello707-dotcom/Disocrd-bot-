'use client';

import { create } from 'zustand';

type Phase = 'idle' | 'cover' | 'reveal';

interface TransitionState {
  phase: Phase;
  href: string | null;
  start: (href: string) => void;
  setPhase: (phase: Phase) => void;
}

/** Stan przejścia między stronami (kurtyna). */
export const useTransition = create<TransitionState>()((set, get) => ({
  phase: 'idle',
  href: null,
  start: (href) => {
    if (get().phase !== 'idle') return;
    set({ phase: 'cover', href });
  },
  setPhase: (phase) => set(phase === 'idle' ? { phase, href: null } : { phase }),
}));
