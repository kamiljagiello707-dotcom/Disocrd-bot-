'use client';

import { useEffect, type RefObject } from 'react';
import { ScrollTrigger } from '@/lib/gsap';
import { sceneState } from '@/store/scene';

/**
 * Łączy sekcję strony z osią scroll-story sceny 3D.
 * Gdy sekcja jest aktywna, `sceneState.story = index + postęp sekcji`.
 *
 * @param mode  'center' – sekcja aktywna, gdy jej obszar przechodzi przez środek ekranu,
 *              'pinned' – od górnej do dolnej krawędzi (wysokie sekcje sticky),
 *              'first'  – pierwsza sekcja strony (od samej góry do środka ekranu).
 */
const RANGES = {
  center: ['top center', 'bottom center'],
  pinned: ['top top', 'bottom bottom'],
  first: ['top top', 'bottom center'],
} as const;

export function useStorySection(ref: RefObject<HTMLElement>, index: number, mode: keyof typeof RANGES = 'center') {
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const update = (self: ScrollTrigger) => {
      sceneState.story = index + self.progress * 0.999;
    };
    const st = ScrollTrigger.create({
      trigger: el,
      start: RANGES[mode][0],
      end: RANGES[mode][1],
      onUpdate: update,
      onToggle: (self) => self.isActive && update(self),
      onRefresh: (self) => self.isActive && update(self),
    });
    return () => st.kill();
  }, [ref, index, mode]);
}
