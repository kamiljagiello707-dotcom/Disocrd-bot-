'use client';

import { Suspense, useEffect, useMemo, useRef, type MutableRefObject } from 'react';
import * as THREE from 'three';
import { Canvas, useFrame, useThree } from '@react-three/fiber';
import { ContactShadows, OrbitControls, PerformanceMonitor } from '@react-three/drei';
import type { OrbitControls as OrbitControlsImpl } from 'three-stdlib';
import type { GarmentType } from '@/lib/types';
import { Garment } from './garments/Garment';
import { StudioLights } from './StudioLights';
import { createFabricMaterial } from './materials';

export interface ViewerApi {
  zoom: (dir: 1 | -1) => void;
  reset: () => void;
}

interface Props {
  type: GarmentType;
  /** Kolor wariantu – zmiana płynnie przebarwia model */
  color: string;
  /** 0..1 – rozłożenie modelu na części */
  exploded: boolean;
  autoRotate: boolean;
  apiRef: MutableRefObject<ViewerApi | null>;
  onInteract?: () => void;
  reduced: boolean;
}

const MIN_D = 3.2;
const MAX_D = 8;
const HOME = new THREE.Vector3(0, 0.4, 5.6);

function Model({ type, color, exploded, reduced }: Pick<Props, 'type' | 'color' | 'exploded' | 'reduced'>) {
  const material = useMemo(() => createFabricMaterial(color), []); // eslint-disable-line react-hooks/exhaustive-deps
  const target = useMemo(() => new THREE.Color(color), [color]);
  const explode = useRef(0);

  useFrame((_, delta) => {
    const d = reduced ? 1 : 1 - Math.exp(-delta * 5);
    // Płynna zmiana koloru wariantu „na żywo”
    material.color.lerp(target, d);
    // Jasne kolory potrzebują mniej „sheen”, by nie wyglądały na prześwietlone
    const lum = target.r * 0.3 + target.g * 0.59 + target.b * 0.11;
    material.sheen += ((lum > 0.6 ? 0.4 : 1) - material.sheen) * d;
    explode.current += ((exploded ? 1 : 0) - explode.current) * d;
  });

  return <Garment type={type} material={material} explode={explode} distance={1.1} />;
}

/** Udostępnia API zoomu/resetu dla przycisków poza canvasem. */
function ControlsApi({ apiRef, controls }: { apiRef: MutableRefObject<ViewerApi | null>; controls: MutableRefObject<OrbitControlsImpl | null> }) {
  const camera = useThree((s) => s.camera);
  const anim = useRef<THREE.Vector3 | null>(null);

  useEffect(() => {
    apiRef.current = {
      zoom: (dir) => {
        const c = controls.current;
        if (!c) return;
        const offset = camera.position.clone().sub(c.target);
        const dist = THREE.MathUtils.clamp(offset.length() * (dir === 1 ? 0.8 : 1.25), MIN_D, MAX_D);
        anim.current = c.target.clone().add(offset.setLength(dist));
      },
      reset: () => {
        anim.current = HOME.clone();
        controls.current?.target.set(0, 0, 0);
      },
    };
    return () => {
      apiRef.current = null;
    };
  }, [apiRef, camera, controls]);

  useFrame((_, delta) => {
    if (!anim.current) return;
    camera.position.lerp(anim.current, 1 - Math.exp(-delta * 8));
    controls.current?.update();
    if (camera.position.distanceTo(anim.current) < 0.01) anim.current = null;
  });
  return null;
}

/**
 * Podgląd 360° produktu: obracanie (OrbitControls z ograniczeniami kątów),
 * zoom (kółko / pinch / przyciski), live-zmiana koloru i opcjonalny rozkład modelu.
 */
export default function ProductViewer({ type, color, exploded, autoRotate, apiRef, onInteract, reduced }: Props) {
  const controls = useRef<OrbitControlsImpl | null>(null);

  return (
    <Canvas
      dpr={[1, 1.75]}
      camera={{ position: HOME.toArray(), fov: 35 }}
      gl={{ antialias: true, alpha: true, powerPreference: 'high-performance' }}
      shadows={false}
    >
      <PerformanceMonitor />
      <Suspense fallback={null}>
        <StudioLights intensity={1.15} />
        <group position={[0, 0.05, 0]}>
          <Model type={type} color={color} exploded={exploded} reduced={reduced} />
        </group>
        <ContactShadows position={[0, -1.45, 0]} opacity={0.6} scale={8} blur={2.4} far={3} resolution={512} color="#000000" />
      </Suspense>
      <OrbitControls
        ref={controls}
        makeDefault
        enablePan={false}
        enableDamping
        dampingFactor={0.08}
        minDistance={MIN_D}
        maxDistance={MAX_D}
        minPolarAngle={Math.PI * 0.22}
        maxPolarAngle={Math.PI * 0.62}
        autoRotate={autoRotate && !reduced}
        autoRotateSpeed={1.6}
        onStart={onInteract}
      />
      <ControlsApi apiRef={apiRef} controls={controls} />
    </Canvas>
  );
}
