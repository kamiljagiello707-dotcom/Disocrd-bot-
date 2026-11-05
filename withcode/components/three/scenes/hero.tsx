"use client";
import { useEffect, useMemo, useRef } from "react";
import { useFrame, useThree } from "@react-three/fiber";
import {
  AdditiveBlending,
  BufferAttribute,
  BufferGeometry,
  Color,
  ExtrudeGeometry,
  type Group,
  IcosahedronGeometry,
  Mesh,
  MeshPhysicalMaterial,
  Shape,
  ShaderMaterial,
  Vector3,
  type PerspectiveCamera,
  type WebGLProgramParametersWithUniforms,
} from "three";
import { MeshSurfaceSampler } from "three/examples/jsm/math/MeshSurfaceSampler.js";
import { easing } from "maath";
import { simplex3d } from "../shaders/noise";
import { isSlotVisible, particleBudget, useThreeStore } from "../store";
import { trackElement } from "../track";
import { useScrollStore } from "@/lib/stores/scroll";
import type { SceneComponentProps } from "../canvas-root";

/* ───────── geometria tarczy (proceduralna, bez GLB) ───────── */
function shieldShape(scale = 1, inset = 0): Shape {
  const s = new Shape();
  const w = 1 * scale - inset;
  const top = 1.15 * scale - inset;
  const bottom = -1.35 * scale + inset * 1.4;
  s.moveTo(0, top);
  s.bezierCurveTo(w * 0.55, top - 0.02, w * 0.9, top - 0.18 * scale, w, top - 0.32 * scale);
  s.bezierCurveTo(w * 1.02, 0.1 * scale, w * 0.75, -0.75 * scale, 0, bottom);
  s.bezierCurveTo(-w * 0.75, -0.75 * scale, -w * 1.02, 0.1 * scale, -w, top - 0.32 * scale);
  s.bezierCurveTo(-w * 0.9, top - 0.18 * scale, -w * 0.55, top - 0.02, 0, top);
  return s;
}

function createShieldGeometry(): ExtrudeGeometry {
  const outer = shieldShape(1);
  outer.holes.push(shieldShape(1, 0.16));
  const g = new ExtrudeGeometry(outer, {
    depth: 0.18,
    bevelEnabled: true,
    bevelThickness: 0.06,
    bevelSize: 0.05,
    bevelSegments: 6,
    curveSegments: 64,
  });
  g.center();
  g.computeVertexNormals();
  return g;
}

/* ───────── shader: oddychanie (noise displacement) + fresnel + dissolve ───────── */
function patchBreathing(
  material: MeshPhysicalMaterial,
  uniforms: Record<string, { value: number }>,
  displace: boolean,
) {
  material.onBeforeCompile = (shader: WebGLProgramParametersWithUniforms) => {
    Object.assign(shader.uniforms, uniforms);
    shader.vertexShader = shader.vertexShader
      .replace(
        "#include <common>",
        `#include <common>
         uniform float uTime; uniform float uAmp;
         varying vec3 vObjPos;
         ${simplex3d}
         float disp(vec3 p){ return snoise(p * 1.4 + vec3(uTime * 0.25)) * uAmp; }`,
      )
      .replace(
        "#include <beginnormal_vertex>",
        displace
          ? `#include <beginnormal_vertex>
             vec3 _p = position;
             vec3 _t = normalize(cross(normal, vec3(0.0, 1.0, 0.001)));
             vec3 _b = normalize(cross(normal, _t));
             float _e = 0.01;
             vec3 _p0 = _p + normal * disp(_p);
             vec3 _p1 = (_p + _t * _e) + normal * disp(_p + _t * _e);
             vec3 _p2 = (_p + _b * _e) + normal * disp(_p + _b * _e);
             objectNormal = normalize(cross(_p1 - _p0, _p2 - _p0));
             if (dot(objectNormal, normal) < 0.0) objectNormal = -objectNormal;`
          : "#include <beginnormal_vertex>",
      )
      .replace(
        "#include <begin_vertex>",
        `#include <begin_vertex>
         vObjPos = position;
         ${displace ? "transformed += normal * disp(position);" : ""}`,
      );
    shader.fragmentShader = shader.fragmentShader
      .replace(
        "#include <common>",
        `#include <common>
         uniform float uDissolve; uniform float uTime;
         varying vec3 vObjPos;
         ${simplex3d}`,
      )
      .replace(
        "#include <clipping_planes_fragment>",
        `#include <clipping_planes_fragment>
         float _n = snoise(vObjPos * 2.2) * 0.5 + 0.5;
         if (_n < uDissolve) discard;`,
      )
      .replace(
        "#include <dithering_fragment>",
        `#include <dithering_fragment>
         float _edge = smoothstep(uDissolve, uDissolve + 0.04, _n);
         gl_FragColor.rgb += (1.0 - _edge) * step(0.001, uDissolve) * vec3(1.6);
         float _rim = pow(1.0 - abs(dot(normalize(vViewPosition), normal)), 3.0);
         gl_FragColor.rgb += _rim * 0.35;`,
      );
  };
  material.customProgramCacheKey = () => `breath-${displace ? 1 : 0}`;
}

/* ───────── cząsteczki: orbita + rozpad ───────── */
const particleVertex = /* glsl */ `
  uniform float uTime; uniform float uProgress; uniform float uPixelRatio; uniform float uSize;
  attribute vec3 aStart; attribute vec3 aDir; attribute vec4 aOrbit; attribute float aSeed;
  varying float vAlpha;
  ${simplex3d}
  void main(){
    // orbita: aOrbit = (promień, prędkość, nachylenie, faza)
    float a = aOrbit.w + uTime * aOrbit.y;
    vec3 orbit = vec3(cos(a) * aOrbit.x, sin(a * 0.7 + aSeed * 6.28) * 0.25, sin(a) * aOrbit.x);
    float ci = cos(aOrbit.z), si = sin(aOrbit.z);
    orbit = vec3(orbit.x, orbit.y * ci - orbit.z * si, orbit.y * si + orbit.z * ci);

    // rozpad: punkty z powierzchni tarczy uciekają po polu curl noise
    float local = smoothstep(aSeed * 0.35, aSeed * 0.35 + 0.65, uProgress);
    vec3 burst = aStart + aDir * local * (2.0 + aSeed * 5.0) + curl(aStart * 0.6 + uTime * 0.05) * local * 1.6;

    float isSurface = length(aStart) > 0.0 ? 1.0 : 0.0;
    vec3 pos = mix(orbit, burst, isSurface);
    vec4 mv = modelViewMatrix * vec4(pos, 1.0);
    gl_Position = projectionMatrix * mv;
    float size = uSize * (0.6 + aSeed * 0.9);
    gl_PointSize = size * uPixelRatio * (8.0 / -mv.z);
    vAlpha = mix(0.55 + aSeed * 0.45, local * (1.0 - smoothstep(0.75, 1.0, uProgress)), isSurface);
  }
`;
const particleFragment = /* glsl */ `
  varying float vAlpha;
  void main(){
    float d = length(gl_PointCoord - 0.5);
    float a = smoothstep(0.5, 0.0, d);
    gl_FragColor = vec4(vec3(1.0), a * vAlpha);
  }
`;

export default function HeroScene({ slot }: SceneComponentProps) {
  const quality = useThreeStore((s) => s.quality);
  const camera = useThree((s) => s.camera) as PerspectiveCamera;
  const root = useRef<Group>(null);
  const tilt = useRef<Group>(null);
  const shieldRef = useRef<Mesh>(null);
  const coreRef = useRef<Mesh>(null);

  const shieldGeo = useMemo(() => createShieldGeometry(), []);
  const coreGeo = useMemo(
    () => new IcosahedronGeometry(0.62, quality === "low" ? 24 : 64),
    [quality],
  );

  const uniforms = useMemo(
    () => ({ uTime: { value: 0 }, uAmp: { value: 0.09 }, uDissolve: { value: 0 } }),
    [],
  );

  const chrome = useMemo(() => {
    const m = new MeshPhysicalMaterial({
      color: new Color("#d9d9d9"),
      metalness: 1,
      roughness: 0.14,
      clearcoat: 1,
      clearcoatRoughness: 0.08,
      envMapIntensity: 1.4,
    });
    patchBreathing(m, uniforms, false);
    return m;
  }, [uniforms]);

  const glass = useMemo(() => {
    const transmissive = quality !== "low";
    const m = new MeshPhysicalMaterial({
      color: new Color("#0b0b0b"),
      metalness: 0,
      roughness: 0.08,
      transmission: transmissive ? 1 : 0,
      thickness: 1.2,
      ior: 1.5,
      attenuationColor: new Color("#111111"),
      attenuationDistance: 0.6,
      clearcoat: 1,
      clearcoatRoughness: 0.05,
      envMapIntensity: transmissive ? 1.2 : 2.2,
      specularIntensity: 1,
    });
    patchBreathing(m, uniforms, true);
    return m;
  }, [quality, uniforms]);

  useEffect(
    () => () => {
      chrome.dispose();
      glass.dispose();
    },
    [chrome, glass],
  );

  /* cząsteczki: część orbitująca + część próbkowana z powierzchni tarczy */
  const { particleGeo, particleMat } = useMemo(() => {
    const orbitCount = particleBudget(quality, 2400);
    const surfaceCount = particleBudget(quality, 5200);
    const total = orbitCount + surfaceCount;
    const aStart = new Float32Array(total * 3);
    const aDir = new Float32Array(total * 3);
    const aOrbit = new Float32Array(total * 4);
    const aSeed = new Float32Array(total);
    const position = new Float32Array(total * 3);

    const tmpMesh = new Mesh(shieldGeo);
    const sampler = new MeshSurfaceSampler(tmpMesh).build();
    const coreMesh = new Mesh(new IcosahedronGeometry(0.62, 8));
    const coreSampler = new MeshSurfaceSampler(coreMesh).build();
    const p = new Vector3();

    for (let i = 0; i < total; i++) {
      aSeed[i] = Math.random();
      if (i < orbitCount) {
        aOrbit[i * 4] = 1.6 + Math.random() * 1.6;
        aOrbit[i * 4 + 1] = (0.05 + Math.random() * 0.18) * (Math.random() > 0.5 ? 1 : -1);
        aOrbit[i * 4 + 2] = (Math.random() - 0.5) * 1.2;
        aOrbit[i * 4 + 3] = Math.random() * Math.PI * 2;
      } else {
        (Math.random() > 0.35 ? sampler : coreSampler).sample(p);
        aStart.set([p.x, p.y, p.z], i * 3);
        const d = p
          .clone()
          .normalize()
          .add(new Vector3(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.2))
          .normalize();
        aDir.set([d.x, d.y, d.z], i * 3);
      }
    }
    const g = new BufferGeometry();
    g.setAttribute("position", new BufferAttribute(position, 3));
    g.setAttribute("aStart", new BufferAttribute(aStart, 3));
    g.setAttribute("aDir", new BufferAttribute(aDir, 3));
    g.setAttribute("aOrbit", new BufferAttribute(aOrbit, 4));
    g.setAttribute("aSeed", new BufferAttribute(aSeed, 1));
    g.boundingSphere = null;
    const m = new ShaderMaterial({
      vertexShader: particleVertex,
      fragmentShader: particleFragment,
      uniforms: {
        uTime: { value: 0 },
        uProgress: { value: 0 },
        uPixelRatio: { value: Math.min(window.devicePixelRatio, 2) },
        uSize: { value: 2.2 },
      },
      transparent: true,
      depthWrite: false,
      blending: AdditiveBlending,
    });
    coreMesh.geometry.dispose();
    return { particleGeo: g, particleMat: m };
  }, [quality, shieldGeo]);

  useEffect(
    () => () => {
      particleGeo.dispose();
      particleMat.dispose();
    },
    [particleGeo, particleMat],
  );

  useFrame((state, delta) => {
    if (!isSlotVisible(slot.id) || !root.current || !tilt.current) return;
    const t = state.clock.elapsedTime;
    const tracked = trackElement(slot.track, camera, 0);
    const progress = tracked ? tracked.progress : useScrollStore.getState().heroProgress;
    const { pointer } = useScrollStore.getState();

    uniforms.uTime.value = t;
    uniforms.uDissolve.value = Math.min(1, Math.max(0, (progress - 0.08) * 1.6));
    particleMat.uniforms.uTime!.value = t;
    particleMat.uniforms.uProgress!.value = progress;

    // pozycja: środek sekcji hero, lekko w prawo na desktopie
    if (tracked) {
      const wide = window.innerWidth >= 1024;
      const unitW = tracked.scale / Math.max(1, tracked.rect.height);
      const offsetX = wide ? tracked.rect.width * 0.2 * unitW : 0;
      // mobile: obiekt w górnej części ekranu, nad nagłówkiem (tekst hero jest wyrównany do dołu)
      const offsetY = wide ? 0 : tracked.scale * 0.27;
      easing.damp3(
        root.current.position,
        [tracked.x + offsetX, tracked.y + offsetY + progress * 1.2, -progress * 2],
        0.12,
        delta,
      );
      const s = Math.min(1, tracked.scale / 5.2) * (wide ? 1 : 0.58);
      root.current.scale.setScalar(s);
    }
    // parallax + delikatna rotacja za kursorem
    easing.dampE(
      tilt.current.rotation,
      [pointer.y * 0.25, pointer.x * 0.45 + t * 0.12, 0],
      0.25,
      delta,
    );
    if (shieldRef.current) {
      shieldRef.current.rotation.y = Math.sin(t * 0.4) * 0.2;
      const breath = 1 + Math.sin(t * 1.3) * 0.015;
      shieldRef.current.scale.setScalar(breath);
    }
    if (coreRef.current) coreRef.current.rotation.set(t * 0.1, t * 0.17, 0);
  });

  return (
    <group ref={root}>
      <group ref={tilt}>
        <mesh ref={shieldRef} geometry={shieldGeo} material={chrome} />
        <mesh ref={coreRef} geometry={coreGeo} material={glass} position={[0, 0.05, 0]} />
        <points geometry={particleGeo} material={particleMat} frustumCulled={false} />
      </group>
      <directionalLight position={[3, 4, 5]} intensity={1.2} />
      <ambientLight intensity={0.15} />
    </group>
  );
}
