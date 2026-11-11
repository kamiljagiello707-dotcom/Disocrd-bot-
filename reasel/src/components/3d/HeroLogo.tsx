'use client';

import { forwardRef } from 'react';
import * as THREE from 'three';
import { Center, Text3D } from '@react-three/drei';
import { LOGO_FONT } from '@/config/models';

/**
 * Logo „REASEL” jako tekst 3D z metalicznym materiałem (chrom + clearcoat).
 * Odbija Lightformery ze StudioLights, a bloom łapie neonowe refleksy.
 */
export const HeroLogo = forwardRef<THREE.Group, { quality: 'high' | 'low' }>(function HeroLogo({ quality }, ref) {
  return (
    <group ref={ref}>
      <Center>
        <Text3D
          font={LOGO_FONT}
          size={1.15}
          height={0.32}
          curveSegments={quality === 'high' ? 10 : 4}
          bevelEnabled
          bevelThickness={0.035}
          bevelSize={0.022}
          bevelSegments={quality === 'high' ? 5 : 2}
          letterSpacing={0.02}
        >
          REASEL
          <meshPhysicalMaterial color="#f2f2f2" metalness={0.92} roughness={0.22} clearcoat={1} clearcoatRoughness={0.15} envMapIntensity={2.6} />
        </Text3D>
      </Center>
    </group>
  );
});
