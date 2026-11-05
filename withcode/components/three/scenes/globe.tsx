"use client";
import { useEffect, useMemo, useRef } from "react";
import { useFrame, useThree } from "@react-three/fiber";
import {
  AdditiveBlending,
  BackSide,
  BufferAttribute,
  BufferGeometry,
  type Group,
  QuadraticBezierCurve3,
  ShaderMaterial,
  Vector3,
  type PerspectiveCamera,
} from "three";
import { easing } from "maath";
import { isSlotVisible, particleBudget, useThreeStore, type GlobeRegion } from "../store";
import { trackElement } from "../track";
import type { SceneComponentProps } from "../canvas-root";

/** Uproszczone kontury kontynentów [lon, lat] — wystarczające dla globu z punktów. */
const LAND: [number, number][][] = [
  [
    [-168, 65],
    [-140, 70],
    [-110, 72],
    [-85, 70],
    [-65, 60],
    [-55, 50],
    [-70, 43],
    [-76, 35],
    [-81, 25],
    [-97, 26],
    [-97, 18],
    [-88, 15],
    [-80, 8],
    [-92, 15],
    [-105, 20],
    [-112, 30],
    [-118, 34],
    [-124, 40],
    [-124, 48],
    [-135, 58],
    [-150, 60],
    [-165, 60],
  ],
  [
    [-55, 60],
    [-42, 60],
    [-20, 70],
    [-20, 82],
    [-60, 82],
    [-72, 77],
    [-58, 68],
  ],
  [
    [-80, 8],
    [-72, 12],
    [-60, 10],
    [-50, 0],
    [-35, -6],
    [-38, -15],
    [-48, -28],
    [-58, -38],
    [-65, -42],
    [-68, -55],
    [-74, -50],
    [-72, -30],
    [-70, -18],
    [-76, -14],
    [-81, -5],
    [-78, 2],
  ],
  [
    [-10, 36],
    [-9, 43],
    [-2, 44],
    [-5, 48],
    [2, 51],
    [8, 54],
    [10, 58],
    [5, 62],
    [15, 69],
    [28, 71],
    [40, 67],
    [45, 60],
    [40, 50],
    [30, 46],
    [28, 41],
    [22, 38],
    [15, 40],
    [12, 44],
    [8, 44],
    [3, 43],
    [-1, 37],
  ],
  [
    [-17, 21],
    [-12, 28],
    [-6, 35],
    [10, 37],
    [20, 32],
    [32, 31],
    [35, 28],
    [43, 12],
    [51, 12],
    [45, 0],
    [40, -15],
    [35, -25],
    [20, -35],
    [17, -29],
    [12, -17],
    [13, -5],
    [9, 4],
    [-5, 5],
    [-12, 7],
    [-17, 14],
  ],
  [
    [28, 41],
    [40, 42],
    [48, 40],
    [55, 25],
    [60, 22],
    [67, 25],
    [73, 20],
    [77, 8],
    [80, 15],
    [90, 22],
    [98, 16],
    [100, 5],
    [104, 1],
    [107, 10],
    [109, 20],
    [121, 30],
    [122, 40],
    [130, 43],
    [140, 48],
    [142, 58],
    [160, 60],
    [178, 66],
    [170, 70],
    [140, 73],
    [110, 77],
    [80, 73],
    [68, 70],
    [60, 68],
    [45, 60],
    [40, 67],
    [40, 50],
    [30, 46],
  ],
  [
    [95, 5],
    [105, -6],
    [115, -8],
    [125, -9],
    [141, -6],
    [130, -2],
    [120, 2],
    [110, 2],
    [100, 2],
  ],
  [
    [114, -22],
    [122, -18],
    [130, -12],
    [137, -12],
    [142, -11],
    [146, -19],
    [153, -25],
    [150, -37],
    [140, -38],
    [130, -32],
    [115, -34],
  ],
  [
    [130, 31],
    [135, 34],
    [140, 36],
    [142, 40],
    [141, 45],
    [139, 42],
    [135, 35],
  ],
  [
    [-5, 50],
    [1, 51],
    [0, 53],
    [-2, 57],
    [-5, 58],
    [-6, 55],
    [-3, 54],
    [-5, 52],
  ],
  [
    [44, -12],
    [50, -15],
    [49, -25],
    [44, -24],
  ],
];

export function pointInPolygon(lon: number, lat: number, poly: [number, number][]): boolean {
  let inside = false;
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
    const [xi, yi] = poly[i]!;
    const [xj, yj] = poly[j]!;
    if (yi > lat !== yj > lat && lon < ((xj - xi) * (lat - yi)) / (yj - yi) + xi) inside = !inside;
  }
  return inside;
}

export function latLonToVec(lat: number, lon: number, r = 1): Vector3 {
  const phi = (lat * Math.PI) / 180;
  const lam = (lon * Math.PI) / 180;
  return new Vector3(
    Math.cos(phi) * Math.cos(lam) * r,
    Math.sin(phi) * r,
    -Math.cos(phi) * Math.sin(lam) * r,
  );
}

const dotsVertex = /* glsl */ `
  uniform float uPixelRatio; uniform float uSize;
  attribute float aSize; attribute float aPulse;
  uniform float uTime;
  varying float vAlpha; varying float vFacing;
  void main(){
    vec4 mv = modelViewMatrix * vec4(position, 1.0);
    vec3 n = normalize(normalMatrix * normalize(position));
    vFacing = smoothstep(-0.15, 0.35, dot(n, normalize(-mv.xyz)));
    float pulse = aPulse > 0.0 ? (0.75 + 0.25 * sin(uTime * 2.5 + aPulse * 6.28)) : 1.0;
    gl_PointSize = uSize * aSize * pulse * uPixelRatio * (6.0 / -mv.z);
    gl_Position = projectionMatrix * mv;
    vAlpha = aPulse > 0.0 ? 1.0 : 0.85;
  }
`;
const dotsFragment = /* glsl */ `
  varying float vAlpha; varying float vFacing;
  void main(){
    float d = length(gl_PointCoord - 0.5);
    if (d > 0.5) discard;
    gl_FragColor = vec4(vec3(1.0), smoothstep(0.5, 0.15, d) * vAlpha * (0.08 + vFacing * 0.92));
  }
`;
const atmoVertex = /* glsl */ `
  varying vec3 vNormal; varying vec3 vView;
  void main(){
    vec4 mv = modelViewMatrix * vec4(position, 1.0);
    vNormal = normalize(normalMatrix * normal);
    vView = normalize(-mv.xyz);
    gl_Position = projectionMatrix * mv;
  }
`;
const atmoFragment = /* glsl */ `
  varying vec3 vNormal; varying vec3 vView;
  void main(){
    float f = pow(1.0 - abs(dot(vNormal, vView)), 6.0);
    gl_FragColor = vec4(vec3(1.0), f * 0.3);
  }
`;
const arcVertex = /* glsl */ `
  attribute float aT; attribute float aArc;
  uniform float uTime;
  varying float vAlpha;
  void main(){
    float head = fract(uTime * 0.18 + aArc * 0.137);
    float d = head - aT;
    vAlpha = smoothstep(0.35, 0.0, d) * step(0.0, d) * 0.9 + 0.06;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
  }
`;
const arcFragment = /* glsl */ `
  varying float vAlpha;
  void main(){ gl_FragColor = vec4(vec3(1.0), vAlpha); }
`;

export default function GlobeScene({ slot }: SceneComponentProps) {
  const quality = useThreeStore((s) => s.quality);
  const camera = useThree((s) => s.camera) as PerspectiveCamera;
  const root = useRef<Group>(null);
  const spin = useRef<Group>(null);
  const drag = useRef({ active: false, lastX: 0, velocity: 0, offset: 0 });
  const regions: GlobeRegion[] = useMemo(
    () => (slot.scene === "globe" && slot.data && "regions" in slot.data ? slot.data.regions : []),
    [slot],
  );

  const { dots, dotsMat, arcs, arcMat, atmoMat } = useMemo(() => {
    const n = particleBudget(quality, 14000);
    const land: number[] = [];
    const sizes: number[] = [];
    const pulses: number[] = [];
    const golden = Math.PI * (3 - Math.sqrt(5));
    for (let i = 0; i < n; i++) {
      const y = 1 - (i / (n - 1)) * 2;
      const r = Math.sqrt(1 - y * y);
      const th = golden * i;
      const lat = (Math.asin(y) * 180) / Math.PI;
      const lon = (Math.atan2(-Math.sin(th) * r, Math.cos(th) * r) * 180) / Math.PI;
      if (LAND.some((poly) => pointInPolygon(lon, lat, poly))) {
        const v = latLonToVec(lat, lon, 1);
        land.push(v.x, v.y, v.z);
        sizes.push(1);
        pulses.push(0);
      }
    }
    const max = Math.max(1, ...regions.map((r) => r.count));
    for (const reg of regions) {
      const v = latLonToVec(reg.lat, reg.lon, 1.01);
      land.push(v.x, v.y, v.z);
      sizes.push(3 + (Math.log(reg.count + 1) / Math.log(max + 1)) * 5);
      pulses.push(Math.random() * 0.99 + 0.01);
    }
    const g = new BufferGeometry();
    g.setAttribute("position", new BufferAttribute(new Float32Array(land), 3));
    g.setAttribute("aSize", new BufferAttribute(new Float32Array(sizes), 1));
    g.setAttribute("aPulse", new BufferAttribute(new Float32Array(pulses), 1));
    const dm = new ShaderMaterial({
      vertexShader: dotsVertex,
      fragmentShader: dotsFragment,
      uniforms: {
        uPixelRatio: { value: Math.min(window.devicePixelRatio, 2) },
        uSize: { value: 2.6 },
        uTime: { value: 0 },
      },
      transparent: true,
      depthWrite: false,
      blending: AdditiveBlending,
    });

    // łuki z Polski do pozostałych regionów
    const hub = regions.find((r) => r.region === "PL") ?? regions[0];
    const arcPos: number[] = [];
    const arcT: number[] = [];
    const arcId: number[] = [];
    if (hub && quality !== "low") {
      const a = latLonToVec(hub.lat, hub.lon, 1.01);
      regions
        .filter((r) => r !== hub)
        .forEach((r, idx) => {
          const b = latLonToVec(r.lat, r.lon, 1.01);
          const mid = a.clone().add(b).multiplyScalar(0.5);
          const lift = 1 + a.distanceTo(b) * 0.45;
          mid.normalize().multiplyScalar(lift);
          const curve = new QuadraticBezierCurve3(a, mid, b);
          const pts = curve.getPoints(48);
          for (let i = 0; i < pts.length - 1; i++) {
            const p0 = pts[i]!;
            const p1 = pts[i + 1]!;
            arcPos.push(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z);
            arcT.push(i / 48, (i + 1) / 48);
            arcId.push(idx, idx);
          }
        });
    }
    const ag = new BufferGeometry();
    ag.setAttribute("position", new BufferAttribute(new Float32Array(arcPos), 3));
    ag.setAttribute("aT", new BufferAttribute(new Float32Array(arcT), 1));
    ag.setAttribute("aArc", new BufferAttribute(new Float32Array(arcId), 1));
    const am = new ShaderMaterial({
      vertexShader: arcVertex,
      fragmentShader: arcFragment,
      uniforms: { uTime: { value: 0 } },
      transparent: true,
      depthWrite: false,
      blending: AdditiveBlending,
    });
    const atm = new ShaderMaterial({
      vertexShader: atmoVertex,
      fragmentShader: atmoFragment,
      transparent: true,
      side: BackSide,
      depthWrite: false,
      blending: AdditiveBlending,
    });
    return { dots: g, dotsMat: dm, arcs: ag, arcMat: am, atmoMat: atm };
  }, [quality, regions]);

  useEffect(
    () => () => {
      dots.dispose();
      dotsMat.dispose();
      arcs.dispose();
      arcMat.dispose();
      atmoMat.dispose();
    },
    [dots, dotsMat, arcs, arcMat, atmoMat],
  );

  // obrót przeciąganiem — nasłuch na elemencie DOM sekcji (Canvas ma pointer-events: none)
  useEffect(() => {
    const el = slot.track.current;
    if (!el) return;
    const st = drag.current;
    const down = (e: PointerEvent) => {
      st.active = true;
      st.lastX = e.clientX;
      el.setPointerCapture(e.pointerId);
    };
    const move = (e: PointerEvent) => {
      if (!st.active) return;
      const dx = e.clientX - st.lastX;
      st.lastX = e.clientX;
      st.offset += dx * 0.006;
      st.velocity = dx * 0.006;
    };
    const up = () => (st.active = false);
    el.addEventListener("pointerdown", down);
    el.addEventListener("pointermove", move);
    el.addEventListener("pointerup", up);
    el.addEventListener("pointercancel", up);
    return () => {
      el.removeEventListener("pointerdown", down);
      el.removeEventListener("pointermove", move);
      el.removeEventListener("pointerup", up);
      el.removeEventListener("pointercancel", up);
    };
  }, [slot.track]);

  useFrame((state, delta) => {
    if (!isSlotVisible(slot.id) || !root.current || !spin.current) return;
    const t = state.clock.elapsedTime;
    dotsMat.uniforms.uTime!.value = t;
    arcMat.uniforms.uTime!.value = t;
    const tracked = trackElement(slot.track, camera, 0);
    if (tracked) {
      root.current.position.set(tracked.x, tracked.y, 0);
      const s =
        Math.min(
          tracked.scale,
          tracked.rect.width * (tracked.scale / Math.max(1, tracked.rect.height)),
        ) * 0.42;
      easing.damp3(root.current.scale, [s, s, s], 0.2, delta);
    }
    const st = drag.current;
    if (!st.active) {
      st.velocity *= 0.95;
      st.offset += st.velocity + delta * 0.08;
    }
    // Polska startowo zwrócona do widza
    spin.current.rotation.y = -((21 * Math.PI) / 180) - Math.PI / 2 + st.offset;
    spin.current.rotation.x = 0.35;
  });

  return (
    <group ref={root}>
      <group ref={spin}>
        <points geometry={dots} material={dotsMat} frustumCulled={false} />
        <lineSegments geometry={arcs} material={arcMat} frustumCulled={false} />
        <mesh>
          <sphereGeometry args={[0.985, 48, 48]} />
          <meshBasicMaterial color="#050505" />
        </mesh>
      </group>
      <mesh scale={1.12} material={atmoMat}>
        <sphereGeometry args={[1, 48, 48]} />
      </mesh>
    </group>
  );
}
