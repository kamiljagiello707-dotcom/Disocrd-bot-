"use client";
import { useEffect, useMemo } from "react";
import { useFrame, useThree } from "@react-three/fiber";
import {
  AdditiveBlending,
  BufferAttribute,
  BufferGeometry,
  ShaderMaterial,
  Vector3,
  type Group,
  type PerspectiveCamera,
} from "three";
import { simplex3d } from "../shaders/noise";
import { isSlotVisible, useThreeStore } from "../store";
import { useScrollStore } from "@/lib/stores/scroll";
import { trackElement } from "../track";
import type { SceneComponentProps } from "../canvas-root";

/** Próbkuje piksele napisu z canvasa 2D i zamienia je w pozycje cząsteczek. */
function sampleText(text: string, step: number): Float32Array {
  const c = document.createElement("canvas");
  c.width = 900;
  c.height = 360;
  const ctx = c.getContext("2d");
  if (!ctx) return new Float32Array();
  ctx.fillStyle = "#fff";
  ctx.font = "800 300px system-ui, sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(text, c.width / 2, c.height / 2 + 10);
  const data = ctx.getImageData(0, 0, c.width, c.height).data;
  const out: number[] = [];
  for (let y = 0; y < c.height; y += step) {
    for (let x = 0; x < c.width; x += step) {
      if ((data[(y * c.width + x) * 4 + 3] ?? 0) > 128) {
        out.push((x / c.width - 0.5) * 5, -(y / c.height - 0.5) * 2, (Math.random() - 0.5) * 0.15);
      }
    }
  }
  return new Float32Array(out);
}

const vertex = /* glsl */ `
  uniform float uTime; uniform vec3 uMouse; uniform float uPixelRatio;
  attribute float aSeed;
  varying float vAlpha;
  ${simplex3d}
  void main(){
    vec3 p = position;
    p += vec3(snoise(p * 1.5 + uTime * 0.2), snoise(p * 1.5 - uTime * 0.2), 0.0) * 0.03;
    vec3 diff = p - uMouse;
    float d = length(diff.xy);
    float force = exp(-d * d * 3.0) * 1.2;
    p.xy += normalize(diff.xy + 1e-4) * force;
    p.z += force * (aSeed - 0.3) * 2.0;
    vec4 mv = modelViewMatrix * vec4(p, 1.0);
    gl_Position = projectionMatrix * mv;
    gl_PointSize = (1.8 + aSeed * 1.8) * uPixelRatio * (6.0 / -mv.z);
    vAlpha = 0.55 + aSeed * 0.45 - force * 0.25;
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

export default function NotFoundScene({ slot }: SceneComponentProps) {
  const quality = useThreeStore((s) => s.quality);
  const camera = useThree((s) => s.camera) as PerspectiveCamera;

  const { geometry, material } = useMemo(() => {
    const pos = sampleText("404", quality === "high" ? 3 : quality === "medium" ? 4 : 6);
    const count = pos.length / 3;
    const seed = new Float32Array(count).map(() => Math.random());
    const g = new BufferGeometry();
    g.setAttribute("position", new BufferAttribute(pos, 3));
    g.setAttribute("aSeed", new BufferAttribute(seed, 1));
    const m = new ShaderMaterial({
      vertexShader: vertex,
      fragmentShader: fragment,
      uniforms: {
        uTime: { value: 0 },
        uMouse: { value: new Vector3(99, 99, 0) },
        uPixelRatio: { value: Math.min(window.devicePixelRatio, 2) },
      },
      transparent: true,
      depthWrite: false,
      blending: AdditiveBlending,
    });
    return { geometry: g, material: m };
  }, [quality]);

  useEffect(
    () => () => {
      geometry.dispose();
      material.dispose();
    },
    [geometry, material],
  );

  useFrame((state) => {
    if (!isSlotVisible(slot.id)) return;
    material.uniforms.uTime!.value = state.clock.elapsedTime;
    const { pointer } = useScrollStore.getState();
    const hh = Math.tan((camera.fov * Math.PI) / 360) * camera.position.z;
    const tracked = trackElement(slot.track, camera, 0);
    const offY = tracked ? tracked.y : 0;
    const target = new Vector3(pointer.x * hh * camera.aspect, pointer.y * hh - offY, 0);
    (material.uniforms.uMouse!.value as Vector3).lerp(target, 0.15);
  });

  return (
    <ScenePosition slot={slot}>
      <points geometry={geometry} material={material} frustumCulled={false} />
    </ScenePosition>
  );
}

function ScenePosition({ slot, children }: SceneComponentProps & { children: React.ReactNode }) {
  const camera = useThree((s) => s.camera) as PerspectiveCamera;
  const ref = useMemo(() => ({ current: null as null | Group }), []);
  useFrame(() => {
    const tr = trackElement(slot.track, camera, 0);
    if (tr && ref.current) {
      ref.current.position.set(tr.x, tr.y, 0);
      const s = Math.min(1.2, (tr.rect.width / window.innerWidth) * (camera.aspect * 1.1));
      ref.current.scale.setScalar(Math.max(0.55, s));
    }
  });
  return (
    <group
      ref={(g) => {
        ref.current = g;
      }}
    >
      {children}
    </group>
  );
}
