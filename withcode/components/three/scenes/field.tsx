"use client";
import { useEffect, useMemo } from "react";
import { useFrame, useThree } from "@react-three/fiber";
import {
  AdditiveBlending,
  BufferAttribute,
  BufferGeometry,
  ShaderMaterial,
  Vector2,
  type PerspectiveCamera,
} from "three";
import { simplex3d } from "../shaders/noise";
import { isSlotVisible, useThreeStore } from "../store";
import { useScrollStore } from "@/lib/stores/scroll";
import type { SceneComponentProps } from "../canvas-root";

const vertex = /* glsl */ `
  uniform float uTime; uniform vec2 uMouse; uniform float uPixelRatio; uniform float uScroll; uniform float uIntensity;
  attribute float aSeed;
  varying float vAlpha;
  ${simplex3d}
  void main(){
    vec3 p = position;
    float wave = sin(p.x * 0.35 + uTime * 0.6) * 0.25 + cos(p.y * 0.4 + uTime * 0.45) * 0.2;
    float n = snoise(vec3(p.xy * 0.12, uTime * 0.08)) * 0.6;
    float d = distance(p.xy, uMouse);
    float ripple = exp(-d * 0.55) * sin(d * 2.2 - uTime * 3.5) * 0.9;
    p.z += wave + n + ripple * uIntensity;
    p.y += uScroll * 2.5;
    vec4 mv = modelViewMatrix * vec4(p, 1.0);
    gl_Position = projectionMatrix * mv;
    float depth = clamp((-mv.z - 4.0) / 18.0, 0.0, 1.0);
    gl_PointSize = (1.6 + aSeed * 1.4 + exp(-d * 0.8) * 2.5) * uPixelRatio * (1.0 - depth * 0.6);
    vAlpha = (0.12 + aSeed * 0.25 + exp(-d * 0.7) * 0.5) * (1.0 - depth * 0.7) * uIntensity;
  }
`;
const fragment = /* glsl */ `
  varying float vAlpha;
  void main(){
    float d = length(gl_PointCoord - 0.5);
    if (d > 0.5) discard;
    gl_FragColor = vec4(vec3(1.0), smoothstep(0.5, 0.1, d) * vAlpha);
  }
`;

/** Pole punktów w głębi: siatka 160×100 animowana w GPU, z falą od kursora. */
export default function FieldScene({ slot }: SceneComponentProps) {
  const quality = useThreeStore((s) => s.quality);
  const camera = useThree((s) => s.camera) as PerspectiveCamera;
  const intensity =
    slot.scene === "field" && slot.data && "intensity" in slot.data
      ? (slot.data.intensity ?? 1)
      : 1;

  const { geometry, material } = useMemo(() => {
    const cols = quality === "high" ? 160 : quality === "medium" ? 110 : 64;
    const rows = Math.round(cols * 0.62);
    const w = 44;
    const h = 28;
    const count = cols * rows;
    const pos = new Float32Array(count * 3);
    const seed = new Float32Array(count);
    let i = 0;
    for (let y = 0; y < rows; y++) {
      for (let x = 0; x < cols; x++) {
        pos[i * 3] = (x / (cols - 1) - 0.5) * w;
        pos[i * 3 + 1] = (y / (rows - 1) - 0.5) * h;
        pos[i * 3 + 2] = 0;
        seed[i] = Math.random();
        i++;
      }
    }
    const g = new BufferGeometry();
    g.setAttribute("position", new BufferAttribute(pos, 3));
    g.setAttribute("aSeed", new BufferAttribute(seed, 1));
    const m = new ShaderMaterial({
      vertexShader: vertex,
      fragmentShader: fragment,
      uniforms: {
        uTime: { value: 0 },
        uMouse: { value: new Vector2(999, 999) },
        uPixelRatio: { value: Math.min(window.devicePixelRatio, 2) },
        uScroll: { value: 0 },
        uIntensity: { value: intensity },
      },
      transparent: true,
      depthWrite: false,
      blending: AdditiveBlending,
    });
    return { geometry: g, material: m };
  }, [quality, intensity]);

  useEffect(
    () => () => {
      geometry.dispose();
      material.dispose();
    },
    [geometry, material],
  );

  useFrame((state) => {
    if (!isSlotVisible(slot.id)) return;
    const u = material.uniforms;
    u.uTime!.value = state.clock.elapsedTime;
    const { pointer } = useScrollStore.getState();
    // kursor → płaszczyzna pola (z = -8)
    const dist = camera.position.z + 8;
    const hh = Math.tan((camera.fov * Math.PI) / 360) * dist;
    const target = new Vector2(pointer.x * hh * camera.aspect, pointer.y * hh);
    (u.uMouse!.value as Vector2).lerp(target, 0.08);
    const maxScroll = Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
    u.uScroll!.value = window.scrollY / maxScroll;
  });

  return (
    <points
      geometry={geometry}
      material={material}
      position={[0, 0, -8]}
      rotation={[-0.35, 0, 0]}
      frustumCulled={false}
    />
  );
}
