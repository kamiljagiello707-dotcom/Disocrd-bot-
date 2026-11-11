'use client';

import { useMemo } from 'react';
import * as THREE from 'three';
import { Bloom, ChromaticAberration, EffectComposer, Vignette } from '@react-three/postprocessing';
import { BlendFunction } from 'postprocessing';

/**
 * Postprocessing: bloom na neonowych akcentach, delikatna aberracja chromatyczna
 * przy krawędziach kadru i winieta. Wyłączany automatycznie na słabszych urządzeniach.
 */
export function Effects() {
  const offset = useMemo(() => new THREE.Vector2(0.0009, 0.0007), []);
  return (
    <EffectComposer multisampling={0} enableNormalPass={false}>
      <Bloom mipmapBlur intensity={0.9} luminanceThreshold={0.82} luminanceSmoothing={0.2} radius={0.7} />
      <ChromaticAberration
        offset={offset}
        radialModulation
        modulationOffset={0.35}
        blendFunction={BlendFunction.NORMAL}
      />
      <Vignette eskil={false} offset={0.25} darkness={0.75} />
    </EffectComposer>
  );
}
