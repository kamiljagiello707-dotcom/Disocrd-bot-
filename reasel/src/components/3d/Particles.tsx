'use client';

import { useMemo, useRef } from 'react';
import * as THREE from 'three';
import { useFrame, useThree } from '@react-three/fiber';
import { sceneState } from '@/store/scene';

const vertex = /* glsl */ `
  uniform float uTime;
  uniform float uPixelRatio;
  uniform float uSize;
  uniform float uSpeed;
  attribute float aScale;
  attribute float aSeed;
  varying float vAccent;
  varying float vAlpha;
  void main() {
    vec3 p = position;
    // Powolne unoszenie się + falowanie; zawijanie w pionie
    p.y = mod(p.y + uTime * (0.08 + aSeed * 0.12) * uSpeed + 6.0, 12.0) - 6.0;
    p.x += sin(uTime * 0.3 + aSeed * 20.0) * 0.25;
    p.z += cos(uTime * 0.25 + aSeed * 10.0) * 0.25;
    vec4 mv = modelViewMatrix * vec4(p, 1.0);
    gl_Position = projectionMatrix * mv;
    gl_PointSize = uSize * aScale * uPixelRatio * (1.0 / -mv.z);
    vAccent = step(0.82, aSeed);
    vAlpha = smoothstep(14.0, 2.0, -mv.z);
  }
`;

const fragment = /* glsl */ `
  varying float vAccent;
  varying float vAlpha;
  void main() {
    float d = length(gl_PointCoord - 0.5);
    float strength = smoothstep(0.5, 0.0, d);
    vec3 color = mix(vec3(0.95), vec3(0.776, 1.0, 0.0) * 1.6, vAccent);
    gl_FragColor = vec4(color, strength * vAlpha * 0.55);
  }
`;

/** Cząsteczki „kurzu” w świetle reflektorów – shader na GPU, jeden draw call. */
export function Particles({ count = 1200 }: { count?: number }) {
  const material = useRef<THREE.ShaderMaterial>(null);
  const dpr = useThree((s) => s.viewport.dpr);

  const { positions, scales, seeds } = useMemo(() => {
    const positions = new Float32Array(count * 3);
    const scales = new Float32Array(count);
    const seeds = new Float32Array(count);
    for (let i = 0; i < count; i++) {
      positions[i * 3] = (Math.random() - 0.5) * 20;
      positions[i * 3 + 1] = (Math.random() - 0.5) * 12;
      positions[i * 3 + 2] = (Math.random() - 0.5) * 14 - 2;
      scales[i] = 0.4 + Math.random() * 1.6;
      seeds[i] = Math.random();
    }
    return { positions, scales, seeds };
  }, [count]);

  const uniforms = useMemo(
    () => ({ uTime: { value: 0 }, uPixelRatio: { value: dpr }, uSize: { value: 38 }, uSpeed: { value: 1 } }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  useFrame((_, delta) => {
    if (!material.current) return;
    const u = material.current.uniforms;
    // Szybszy ruch przy szybkim scrollu
    const boost = 1 + Math.min(Math.abs(sceneState.velocity) * 0.15, 6);
    u.uSpeed.value += (boost - u.uSpeed.value) * 0.05;
    u.uTime.value += delta * u.uSpeed.value;
    u.uPixelRatio.value = dpr;
  });

  return (
    <points frustumCulled={false}>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
        <bufferAttribute attach="attributes-aScale" args={[scales, 1]} />
        <bufferAttribute attach="attributes-aSeed" args={[seeds, 1]} />
      </bufferGeometry>
      <shaderMaterial
        ref={material}
        vertexShader={vertex}
        fragmentShader={fragment}
        uniforms={uniforms}
        transparent
        depthWrite={false}
        blending={THREE.AdditiveBlending}
      />
    </points>
  );
}
