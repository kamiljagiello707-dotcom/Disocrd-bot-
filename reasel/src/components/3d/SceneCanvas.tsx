'use client';

import { Suspense, useEffect, useState } from 'react';
import { Canvas, useThree } from '@react-three/fiber';
import { AdaptiveEvents, PerformanceMonitor, Preload } from '@react-three/drei';
import { useScene } from '@/store/scene';
import { useIsMobile, useReducedMotion } from '@/hooks/useMediaQuery';
import { StudioLights } from './StudioLights';
import { Particles } from './Particles';
import { StoryExperience } from './StoryExperience';
import { Effects } from './Effects';
import { ProgressReporter, ReadyReporter } from './SceneReporter';

/** Po wznowieniu (frameloop 'never' → 'always') trzeba obudzić pętlę renderowania. */
function Wake({ paused }: { paused: boolean }) {
  const invalidate = useThree((s) => s.invalidate);
  useEffect(() => {
    if (!paused) invalidate();
  }, [paused, invalidate]);
  return null;
}

/**
 * Globalny, stały Canvas sceny 3D – żyje w layoucie, więc przetrwa nawigację
 * między podstronami (ciągłość sceny podczas przejść).
 *
 * Adaptacyjna jakość: PerformanceMonitor obniża DPR i wyłącza postprocessing,
 * gdy FPS spada. Na telefonach: mniej cząsteczek i brak ciężkich efektów.
 */
export default function SceneCanvas({ home, paused }: { home: boolean; paused: boolean }) {
  const quality = useScene((s) => s.quality);
  const setQuality = useScene((s) => s.setQuality);
  const mobile = useIsMobile();
  const reduced = useReducedMotion();
  const [dpr, setDpr] = useState(1.5);
  // Ciężka scena hero montowana dopiero po wejściu na stronę główną – potem zostaje
  // (ciągłość przy powrocie). Na podstronach renderujemy tylko lekkie cząsteczki.
  const [heroMounted, setHeroMounted] = useState(home);
  if (home && !heroMounted) setHeroMounted(true);

  const high = quality === 'high' && !mobile;

  return (
    <Canvas
      // Pauza, gdy kurtyna przejścia zasłania ekran – zwalnia GPU/CPU dla animacji i nawigacji
      frameloop={paused ? 'never' : 'always'}
      dpr={mobile ? Math.min(dpr, 1.5) : dpr}
      gl={{ antialias: !high, alpha: false, powerPreference: 'high-performance', stencil: false }}
      camera={{ position: [0, 0.1, 6.4], fov: 35, near: 0.1, far: 60 }}
      onCreated={({ gl }) => gl.setClearColor('#0a0a0a')}
      aria-hidden
    >
      <color attach="background" args={['#0a0a0a']} />
      <fog attach="fog" args={['#0a0a0a', 7, 20]} />
      <Wake paused={paused} />
      <PerformanceMonitor
        flipflops={3}
        onIncline={() => {
          setDpr(Math.min(2, window.devicePixelRatio));
          setQuality('high');
        }}
        onDecline={() => {
          setDpr(1);
          setQuality('low');
        }}
        onFallback={() => {
          setDpr(1);
          setQuality('low');
        }}
      />
      <AdaptiveEvents />
      <ProgressReporter />

      <Suspense fallback={null}>
        <StudioLights />
        {heroMounted && (
          // Osobny Suspense – doładowanie sceny hero nie ukrywa reszty (cząsteczek)
          <Suspense fallback={null}>
            <StoryExperience quality={high ? 'high' : 'low'} reduced={reduced} mobile={mobile} />
          </Suspense>
        )}
        <Particles count={mobile ? 350 : quality === 'high' ? 1400 : 600} />
        {high && home && <Effects />}
        <Preload all />
        <ReadyReporter />
      </Suspense>
    </Canvas>
  );
}
