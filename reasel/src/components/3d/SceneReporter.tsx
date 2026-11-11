'use client';

import { useEffect } from 'react';
import { useProgress } from '@react-three/drei';
import { useScene } from '@/store/scene';

/** Raportuje realny postęp ładowania assetów do preloadera. */
export function ProgressReporter() {
  const progress = useProgress((s) => s.progress);
  const setProgress = useScene((s) => s.setProgress);
  useEffect(() => setProgress(progress), [progress, setProgress]);
  return null;
}

/** Montowany wewnątrz <Suspense> – efekt uruchamia się dopiero po załadowaniu sceny. */
export function ReadyReporter() {
  const setReady = useScene((s) => s.setReady);
  useEffect(() => {
    // Dwie klatki zapasu, by shadery zdążyły się skompilować przed zdjęciem preloadera
    let raf2 = 0;
    const raf1 = requestAnimationFrame(() => {
      raf2 = requestAnimationFrame(() => setReady(true));
    });
    return () => {
      cancelAnimationFrame(raf1);
      cancelAnimationFrame(raf2);
    };
  }, [setReady]);
  return null;
}
