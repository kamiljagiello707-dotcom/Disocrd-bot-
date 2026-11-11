'use client';

import { memo, useMemo } from 'react';
import * as THREE from 'three';
import { RoundedBox } from '@react-three/drei';
import type { GarmentType } from '@/lib/types';
import { accentMaterial, metalMaterial } from '../materials';

/**
 * Proceduralne modele ubrań zbudowane z prymitywów Three.js.
 * To placeholder do czasu podmiany na własny model .glb (patrz config/models.ts).
 * Każda część jest osobną siatką – dzięki temu działa efekt „eksplozji”.
 */

interface Props {
  type: GarmentType;
  material: THREE.Material;
}

/** Profil tułowia (promień, wysokość) – obracany przez LatheGeometry. */
const TORSO_PROFILE: [number, number][] = [
  [0.001, -1.0],
  [0.74, -1.0],
  [0.8, -0.92],
  [0.8, -0.3],
  [0.82, 0.3],
  [0.8, 0.6],
  [0.66, 0.82],
  [0.38, 0.95],
  [0.16, 0.98],
  [0.001, 0.98],
];

const BODY_DEPTH = 0.58;

function useShared() {
  return useMemo(() => {
    const torso = new THREE.LatheGeometry(
      TORSO_PROFILE.map(([x, y]) => new THREE.Vector2(x, y)),
      56,
    );
    torso.computeVertexNormals();
    return { torso, metal: metalMaterial(), accent: accentMaterial() };
  }, []);
}

function Torso({ material, geometry }: { material: THREE.Material; geometry: THREE.BufferGeometry }) {
  return <mesh name="torso" geometry={geometry} material={material} scale={[1, 1, BODY_DEPTH]} castShadow receiveShadow />;
}

function Sleeve({
  side,
  material,
  length = 1.45,
  angle = 0.38,
  short = false,
}: {
  side: 1 | -1;
  material: THREE.Material;
  length?: number;
  angle?: number;
  short?: boolean;
}) {
  return (
    <group position={[side * 0.7, 0.62, 0]} rotation={[0, 0, side * angle]}>
      <mesh name="shoulder" material={material} scale={[1, 1, 0.9]} castShadow>
        <sphereGeometry args={[short ? 0.3 : 0.27, 24, 16]} />
      </mesh>
      <mesh name="sleeve" position={[0, -length / 2, 0]} material={material} scale={[1, 1, 0.9]} castShadow>
        <cylinderGeometry args={[short ? 0.3 : 0.27, short ? 0.27 : 0.2, length, 28, 1, true]} />
      </mesh>
      {!short && (
        <mesh name="cuff" position={[0, -length - 0.06, 0]} material={material} castShadow>
          <cylinderGeometry args={[0.2, 0.19, 0.14, 28]} />
        </mesh>
      )}
    </group>
  );
}

function Hoodie({ material, torso, metal, accent, zip = false }: SharedProps & { zip?: boolean }) {
  const gap = 0.95;
  return (
    <>
      <Torso material={material} geometry={torso} />
      <mesh name="waistband" position={[0, -1.03, 0]} scale={[1, 1, BODY_DEPTH]} material={material}>
        <cylinderGeometry args={[0.76, 0.75, 0.14, 48]} />
      </mesh>
      <Sleeve side={1} material={material} />
      <Sleeve side={-1} material={material} />
      {/* Kaptur – sfera z wycięciem z przodu */}
      <mesh name="hood" position={[0, 1.02, -0.2]} rotation={[-0.42, 0, 0]} scale={[0.9, 1.02, 1.22]} material={material} castShadow>
        <sphereGeometry args={[0.5, 40, 24, Math.PI / 2 + gap / 2, Math.PI * 2 - gap, 0, Math.PI * 0.78]} />
      </mesh>
      {/* Obszycie otworu kaptura */}
      <mesh name="hood-rim" position={[0, 1.08, 0.2]} rotation={[-0.35, 0, -Math.PI * 0.125]} scale={[0.82, 1.05, 1]} material={material}>
        <torusGeometry args={[0.36, 0.045, 12, 40, Math.PI * 1.25]} />
      </mesh>
      {zip ? (
        <mesh name="zip" position={[0, -0.04, 0.475]} material={metal}>
          <boxGeometry args={[0.035, 1.9, 0.03]} />
        </mesh>
      ) : (
        <RoundedBox name="pocket" args={[0.95, 0.42, 0.05]} radius={0.02} position={[0, -0.55, 0.465]} material={material} />
      )}
      {/* Sznurki z metalowymi końcówkami */}
      {[-1, 1].map((s) => (
        <group key={s} position={[s * 0.12, 0.66, 0.5]}>
          <mesh name="string" position={[0, -0.25, 0]} material={material}>
            <cylinderGeometry args={[0.017, 0.017, 0.5, 8]} />
          </mesh>
          <mesh name="aglet-metal" position={[0, -0.53, 0]} material={metal}>
            <cylinderGeometry args={[0.024, 0.024, 0.08, 10]} />
          </mesh>
        </group>
      ))}
      <RoundedBox name="logo-patch" args={[0.18, 0.07, 0.02]} radius={0.008} position={[0.36, 0.4, 0.43]} rotation={[0, 0.42, 0]} material={accent} />
    </>
  );
}

function Jacket({ material, torso, metal, accent }: SharedProps) {
  return (
    <>
      <Torso material={material} geometry={torso} />
      <mesh name="hem" position={[0, -1.03, 0]} scale={[1, 1, BODY_DEPTH]} material={material}>
        <cylinderGeometry args={[0.76, 0.75, 0.14, 48]} />
      </mesh>
      <Sleeve side={1} material={material} />
      <Sleeve side={-1} material={material} />
      <mesh name="collar" position={[0, 1.02, 0]} scale={[1, 1, 0.72]} material={material}>
        <cylinderGeometry args={[0.32, 0.4, 0.24, 36, 1, true]} />
      </mesh>
      <mesh name="zip" position={[0, -0.02, 0.475]} material={metal}>
        <boxGeometry args={[0.035, 1.95, 0.03]} />
      </mesh>
      {[-1, 1].map((s) => (
        <RoundedBox
          key={s}
          name="pocket-flap"
          args={[0.3, 0.12, 0.03]}
          radius={0.01}
          position={[s * 0.4, 0.36, 0.415]}
          rotation={[0, s * 0.48, 0]}
          material={material}
        />
      ))}
      <RoundedBox name="logo-patch" args={[0.05, 0.16, 0.02]} radius={0.008} position={[-0.8, 0.05, 0.12]} rotation={[0, -1.35, 0]} material={accent} />
    </>
  );
}

function TShirt({ material, torso, accent }: SharedProps) {
  return (
    <>
      <Torso material={material} geometry={torso} />
      <Sleeve side={1} material={material} length={0.55} angle={0.85} short />
      <Sleeve side={-1} material={material} length={0.55} angle={0.85} short />
      <mesh name="collar" position={[0, 0.97, 0.02]} rotation={[Math.PI / 2, 0, 0]} scale={[1, 0.72, 1]} material={material}>
        <torusGeometry args={[0.2, 0.035, 12, 40]} />
      </mesh>
      <RoundedBox name="logo-patch" args={[0.22, 0.06, 0.02]} radius={0.008} position={[0, 0.5, 0.465]} material={accent} />
    </>
  );
}

function Pants({ material, metal, accent }: SharedProps) {
  return (
    <>
      <mesh name="waistband" position={[0, 1.0, 0]} scale={[1, 1, 0.7]} material={material}>
        <cylinderGeometry args={[0.62, 0.62, 0.16, 40]} />
      </mesh>
      <mesh name="hips" position={[0, 0.7, 0]} scale={[1, 1, 0.7]} material={material} castShadow>
        <cylinderGeometry args={[0.62, 0.66, 0.46, 40]} />
      </mesh>
      {[-1, 1].map((s) => (
        <group key={s}>
          <mesh name="leg" position={[s * 0.31, -0.45, 0]} rotation={[0, 0, s * 0.045]} material={material} castShadow>
            <cylinderGeometry args={[0.34, 0.25, 1.9, 32, 1, true]} />
          </mesh>
          <mesh name="hem" position={[s * 0.35, -1.42, 0]} material={material}>
            <cylinderGeometry args={[0.25, 0.25, 0.08, 32]} />
          </mesh>
          <RoundedBox
            name="cargo-pocket"
            args={[0.06, 0.34, 0.26]}
            radius={0.02}
            position={[s * 0.6, -0.25, 0]}
            rotation={[0, 0, s * 0.045]}
            material={material}
          />
        </group>
      ))}
      <mesh name="button-metal" position={[0, 1.0, 0.44]} rotation={[Math.PI / 2, 0, 0]} material={metal}>
        <cylinderGeometry args={[0.04, 0.04, 0.02, 16]} />
      </mesh>
      <RoundedBox name="logo-patch" args={[0.16, 0.06, 0.02]} radius={0.008} position={[0.36, 0.86, 0.37]} rotation={[0, 0.5, 0]} material={accent} />
    </>
  );
}

function Cap({ material, accent }: SharedProps) {
  return (
    <group scale={1.45} position={[0, -0.3, 0]}>
      <mesh name="crown" scale={[1, 0.78, 1.05]} material={material} castShadow>
        <sphereGeometry args={[0.7, 48, 24, 0, Math.PI * 2, 0, Math.PI / 2]} />
      </mesh>
      <mesh name="band" position={[0, 0.02, 0]} scale={[1, 1, 1.05]} material={material}>
        <cylinderGeometry args={[0.705, 0.705, 0.06, 48, 1, true]} />
      </mesh>
      <mesh name="brim" position={[0, 0.02, 0.5]} rotation={[0.12, 0, 0]} scale={[1, 1, 0.95]} material={material} castShadow>
        <cylinderGeometry args={[0.62, 0.62, 0.035, 40, 1, false, -Math.PI / 2, Math.PI]} />
      </mesh>
      <mesh name="button" position={[0, 0.55, 0]} material={material}>
        <sphereGeometry args={[0.055, 16, 12]} />
      </mesh>
      <RoundedBox name="logo-patch" args={[0.26, 0.11, 0.02]} radius={0.01} position={[0, 0.3, 0.62]} rotation={[-0.55, 0, 0]} material={accent} />
    </group>
  );
}

function Bag({ material, metal, accent }: SharedProps) {
  return (
    <>
      <RoundedBox name="body" args={[1.25, 1.65, 0.52]} radius={0.16} smoothness={4} material={material} castShadow />
      <RoundedBox name="front-pocket" args={[0.95, 0.62, 0.22]} radius={0.08} position={[0, -0.38, 0.3]} material={material} />
      <mesh name="handle" position={[0, 0.88, 0]} material={material}>
        <torusGeometry args={[0.16, 0.04, 12, 32, Math.PI]} />
      </mesh>
      {[-1, 1].map((s) => (
        <RoundedBox key={s} name="strap" args={[0.13, 1.35, 0.05]} radius={0.02} position={[s * 0.33, 0.02, -0.3]} material={material} />
      ))}
      <mesh name="zip-metal" position={[0, 0.55, 0.265]} material={metal}>
        <boxGeometry args={[0.9, 0.025, 0.02]} />
      </mesh>
      <RoundedBox name="logo-patch" args={[0.24, 0.08, 0.02]} radius={0.01} position={[0, -0.2, 0.42]} material={accent} />
    </>
  );
}

type SharedProps = {
  material: THREE.Material;
  torso: THREE.BufferGeometry;
  metal: THREE.Material;
  accent: THREE.Material;
};

export const ProceduralGarment = memo(function ProceduralGarment({ type, material }: Props) {
  const { torso, metal, accent } = useShared();
  const shared = { material, torso, metal, accent };

  switch (type) {
    case 'hoodie':
      return <Hoodie {...shared} />;
    case 'jacket':
      return <Jacket {...shared} />;
    case 'tshirt':
      return <TShirt {...shared} />;
    case 'pants':
      return <Pants {...shared} />;
    case 'cap':
      return <Cap {...shared} />;
    case 'bag':
      return <Bag {...shared} />;
    default:
      return <Hoodie {...shared} />;
  }
});
