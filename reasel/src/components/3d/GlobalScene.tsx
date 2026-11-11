'use client';

import { useEffect, useState } from 'react';
import dynamic from 'next/dynamic';
import { usePathname } from 'next/navigation';
import { motion } from 'framer-motion';
import { sceneState, useScene } from '@/store/scene';
import { useTransition } from '@/store/transition';

// Lazy loading – Three.js trafia do osobnego chunka, ładowanego dopiero na kliencie
const SceneCanvas = dynamic(() => import('./SceneCanvas'), { ssr: false });

function hasWebGL() {
  try {
    const c = document.createElement('canvas');
    return !!(window.WebGLRenderingContext && (c.getContext('webgl2') || c.getContext('webgl')));
  } catch {
    return false;
  }
}

/**
 * Tło 3D całej witryny (fixed, pod treścią). Na stronie głównej pokazuje pełną
 * scenę hero + scroll-story, na podstronach – przygaszone cząsteczki.
 */
export function GlobalScene() {
  const pathname = usePathname();
  const webgl = useScene((s) => s.webgl);
  // Scena pauzowana na czas całego przejścia (kurtyna zasłania ekran); wznawiana po odsłonięciu
  const covered = useTransition((s) => s.phase !== 'idle');
  const [mounted, setMounted] = useState(false);
  const isHome = pathname === '/';

  useEffect(() => {
    if (!hasWebGL()) {
      useScene.getState().setWebgl(false);
      useScene.getState().setReady(true);
      return;
    }
    setMounted(true);
  }, []);

  useEffect(() => {
    sceneState.home = isHome;
    if (!isHome) sceneState.story = 0;
  }, [isHome]);

  if (!mounted || !webgl) {
    return <div aria-hidden className="fixed inset-0 z-0 bg-[radial-gradient(ellipse_at_center,rgba(198,255,0,0.08),transparent_60%)]" />;
  }

  return (
    <motion.div
      aria-hidden
      className="pointer-events-none fixed inset-0 z-0"
      animate={{ opacity: isHome ? 1 : 0.45 }}
      transition={{ duration: 1.2 }}
    >
      <SceneCanvas home={isHome} paused={covered} />
    </motion.div>
  );
}
