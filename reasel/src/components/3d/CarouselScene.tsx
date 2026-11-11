'use client';

import { Suspense, useEffect, useMemo, useRef, useState, type MutableRefObject } from 'react';
import * as THREE from 'three';
import { Canvas, useFrame, useThree } from '@react-three/fiber';
import { Image as ImagePlane, MeshReflectorMaterial } from '@react-three/drei';
import type { Product } from '@/lib/types';

export interface CarouselControl {
  /** Docelowy obrót karuzeli (w radianach) */
  target: number;
  current: number;
  /** Czy użytkownik właśnie przeciąga (blokuje kliknięcia) */
  dragged: boolean;
}

interface Props {
  products: Product[];
  control: MutableRefObject<CarouselControl>;
  onActiveChange: (index: number) => void;
  onSelect: (index: number) => void;
  active: boolean;
  mobile: boolean;
  reduced: boolean;
}

const CARD_W = 1.7;
const CARD_H = 2.15;
const optimized = (src: string) => `/_next/image?url=${encodeURIComponent(src)}&w=640&q=75`;

function Card({
  product,
  index,
  angle,
  radius,
  control,
  onSelect,
  activeIndex,
}: {
  product: Product;
  index: number;
  angle: number;
  radius: number;
  control: MutableRefObject<CarouselControl>;
  onSelect: (i: number) => void;
  activeIndex: number;
}) {
  const ref = useRef<THREE.Mesh>(null);
  const [hovered, setHovered] = useState(false);
  const isActive = index === activeIndex;

  useFrame((_, delta) => {
    const mesh = ref.current;
    if (!mesh) return;
    const mat = mesh.material as THREE.ShaderMaterial & { zoom: number; grayscale: number };
    const d = 1 - Math.exp(-delta * 6);
    const s = hovered ? 1.06 : isActive ? 1 : 0.92;
    mesh.scale.x += (CARD_W * s - mesh.scale.x) * d;
    mesh.scale.y += (CARD_H * s - mesh.scale.y) * d;
    mat.zoom += ((hovered ? 1.12 : 1) - mat.zoom) * d;
    mat.grayscale += ((isActive || hovered ? 0 : 0.85) - mat.grayscale) * d;
    // Aktywna karta lekko wysunięta do przodu
    const targetY = isActive ? 0.12 : 0;
    mesh.position.y += (targetY - mesh.position.y) * d;
  });

  return (
    <group rotation={[0, angle, 0]}>
      <ImagePlane
        ref={ref}
        url={optimized(product.images[0].src)}
        position={[0, 0, radius]}
        scale={[CARD_W, CARD_H]}
        radius={0.08}
        transparent
        onPointerOver={(e) => {
          e.stopPropagation();
          setHovered(true);
        }}
        onPointerOut={() => setHovered(false)}
        onClick={(e) => {
          e.stopPropagation();
          if (!control.current.dragged) onSelect(index);
        }}
      />
    </group>
  );
}

function Rig({
  products,
  control,
  onActiveChange,
  onSelect,
  reduced,
}: Omit<Props, 'active' | 'mobile'>) {
  const group = useRef<THREE.Group>(null);
  const n = products.length;
  const step = (Math.PI * 2) / n;
  const radius = Math.max(3.2, (n * CARD_W * 1.25) / (Math.PI * 2));
  const [activeIndex, setActiveIndex] = useState(0);

  useFrame((state, delta) => {
    const c = control.current;
    const d = reduced ? 1 : 1 - Math.exp(-delta * 4.5);
    c.current += (c.target - c.current) * d;
    if (group.current) group.current.rotation.y = -c.current;
    // Lekka paralaksa kamery za kursorem
    state.camera.position.x += (state.pointer.x * 0.6 - state.camera.position.x) * 0.05;
    state.camera.lookAt(0, 0.1, 0);

    const idx = ((Math.round(c.current / step) % n) + n) % n;
    if (idx !== activeIndex) {
      setActiveIndex(idx);
      onActiveChange(idx);
    }
  });

  const handleSelect = (i: number) => {
    if (i === activeIndex) {
      onSelect(i);
      return;
    }
    // Obrót najkrótszą drogą do klikniętej karty
    const c = control.current;
    let diff = i - activeIndex;
    if (diff > n / 2) diff -= n;
    if (diff < -n / 2) diff += n;
    c.target = Math.round(c.target / step) * step + diff * step;
  };

  return (
    <group ref={group} position={[0, 0, -radius]}>
      {products.map((p, i) => (
        <Card
          key={p.id}
          product={p}
          index={i}
          angle={i * step}
          radius={radius}
          control={control}
          onSelect={handleSelect}
          activeIndex={activeIndex}
        />
      ))}
    </group>
  );
}

/** Po zmianie frameloop na 'always' trzeba „obudzić” pętlę renderowania. */
function Wake({ active }: { active: boolean }) {
  const invalidate = useThree((s) => s.invalidate);
  useEffect(() => {
    if (active) invalidate();
  }, [active, invalidate]);
  return null;
}

/**
 * Karuzela kolekcji w 3D: karty produktów rozmieszczone na okręgu,
 * obracane scrollem i przeciąganiem, z odbiciami na podłodze (MeshReflectorMaterial).
 */
export default function CarouselScene(props: Props) {
  const { mobile, active } = props;
  const fogArgs = useMemo(() => ['#0a0a0a', 5, 14] as [string, number, number], []);

  return (
    <Canvas
      frameloop={active ? 'always' : 'demand'}
      dpr={mobile ? [1, 1.5] : [1, 1.75]}
      camera={{ position: [0, 0.35, 4.2], fov: 42 }}
      gl={{ antialias: true, alpha: false, powerPreference: 'high-performance' }}
    >
      <color attach="background" args={['#0a0a0a']} />
      <fog attach="fog" args={fogArgs} />
      <Wake active={active} />
      <Suspense fallback={null}>
        <Rig {...props} />
      </Suspense>
      <mesh rotation-x={-Math.PI / 2} position={[0, -1.25, 0]}>
        <planeGeometry args={[40, 40]} />
        <MeshReflectorMaterial
          blur={[300, 80]}
          resolution={mobile ? 256 : 1024}
          mixBlur={1}
          mixStrength={mobile ? 20 : 45}
          roughness={1}
          depthScale={1.1}
          minDepthThreshold={0.4}
          maxDepthThreshold={1.4}
          color="#0d0d0d"
          metalness={0.6}
          mirror={0.7}
        />
      </mesh>
      {/* Neonowa linia horyzontu – łapie odbicie w podłodze */}
      <mesh position={[0, -1.2, -9]}>
        <planeGeometry args={[30, 0.02]} />
        <meshBasicMaterial color="#c6ff00" toneMapped={false} />
      </mesh>
    </Canvas>
  );
}
