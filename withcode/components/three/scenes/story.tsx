"use client";
import { useEffect, useMemo, useRef } from "react";
import { useFrame, useThree } from "@react-three/fiber";
import {
  AdditiveBlending,
  BufferAttribute,
  BufferGeometry,
  CanvasTexture,
  DoubleSide,
  type Group,
  InstancedBufferAttribute,
  InstancedBufferGeometry,
  PlaneGeometry,
  SRGBColorSpace,
  MeshBasicMaterial,
  ShaderMaterial,
  type PerspectiveCamera,
} from "three";
import { trackElement } from "../track";
import { easing } from "maath";
import { isSlotVisible, particleBudget, useThreeStore } from "../store";
import { useScrollStore } from "@/lib/stores/scroll";
import type { SceneComponentProps } from "../canvas-root";

const MESSAGES: { name: string; text: string; spam: boolean }[] = [
  { name: "Ola", text: "ktoś gra dziś w ranked?", spam: false },
  { name: "Kuba", text: "dzięki za pomoc z configiem!", spam: false },
  { name: "Ania", text: "nowy odcinek już jest 🔥", spam: false },
  { name: "Tomek", text: "o której startuje event?", spam: false },
  { name: "Ela", text: "witam wszystkich :)", spam: false },
  { name: "fr33_n1tr0", text: "FREE NITRO discord.gg/xyz", spam: true },
  { name: "bot_4821", text: "@everyone @here @everyone", spam: true },
  { name: "spammer", text: "KUP TANIE KONTA TERAZ!!!", spam: true },
];

const CELL_W = 512;
const CELL_H = 128;

function createAtlas(): CanvasTexture {
  const c = document.createElement("canvas");
  c.width = CELL_W * 2;
  c.height = CELL_H * 4;
  const ctx = c.getContext("2d");
  if (ctx) {
    MESSAGES.forEach((m, i) => {
      const x = (i % 2) * CELL_W;
      const y = Math.floor(i / 2) * CELL_H;
      ctx.fillStyle = "#0e0e0e";
      ctx.beginPath();
      ctx.roundRect(x + 4, y + 4, CELL_W - 8, CELL_H - 8, 22);
      ctx.fill();
      ctx.strokeStyle = "rgba(255,255,255,0.14)";
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.fillStyle = m.spam ? "#2a2a2a" : "#3a3a3a";
      ctx.beginPath();
      ctx.arc(x + 58, y + CELL_H / 2, 30, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = "#fff";
      ctx.font = "600 30px system-ui, sans-serif";
      ctx.fillText(m.name.slice(0, 1).toUpperCase(), x + 48, y + CELL_H / 2 + 11);
      ctx.font = "600 26px system-ui, sans-serif";
      ctx.fillText(m.name, x + 106, y + 52);
      ctx.fillStyle = "rgba(255,255,255,0.68)";
      ctx.font = "400 25px system-ui, sans-serif";
      ctx.fillText(m.text, x + 106, y + 90, CELL_W - 130);
    });
  }
  const tex = new CanvasTexture(c);
  tex.colorSpace = SRGBColorSpace;
  tex.anisotropy = 4;
  return tex;
}

const FILTER_Z = -7;
const SPAN_START = -34;
const SPAN_END = 0;

const cardPath = /* glsl */ `
  uniform float uTime; uniform float uClean; uniform float uFilterZ; uniform float uFade;
  float cardZ(float speed, float phase){
    return mix(${SPAN_START.toFixed(1)}, ${SPAN_END.toFixed(1)}, fract(uTime * speed + phase));
  }
  vec3 cardPos(vec2 lane, float speed, float phase, float spam, float idx){
    float z = cardZ(speed, phase);
    float after = smoothstep(uFilterZ, uFilterZ + 3.0, z) * uClean * (1.0 - spam);
    vec2 column = vec2(mod(idx, 2.0) * 1.9 - 0.95, lane.y * 0.25);
    return vec3(mix(lane, column, after), z);
  }
`;

const cardVertex = /* glsl */ `
  attribute vec2 aLane; attribute float aSpeed; attribute float aPhase; attribute float aCell; attribute float aSpam; attribute float aIdx;
  uniform float uReact;
  varying vec2 vUv; varying float vAlpha; varying float vSpam; varying float vScan;
  ${cardPath}
  void main(){
    vec3 c = cardPos(aLane, aSpeed, aPhase, aSpam, aIdx);
    float hide = aSpam * smoothstep(uFilterZ - 0.2, uFilterZ + 0.4, c.z) * uReact;
    float s = 1.0 - hide;
    vec3 p = position * s;
    p.xy *= 1.0;
    vec4 mv = modelViewMatrix * vec4(p + c, 1.0);
    gl_Position = projectionMatrix * mv;
    float col = mod(aCell, 2.0);
    float row = floor(aCell / 2.0);
    vUv = vec2((uv.x + col) / 2.0, (uv.y + (3.0 - row)) / 4.0);
    vAlpha = smoothstep(${SPAN_START.toFixed(1)}, ${SPAN_START.toFixed(1)} + 8.0, c.z) * (1.0 - smoothstep(-3.5, ${SPAN_END.toFixed(1)}, c.z)) * s * uFade;
    vSpam = aSpam;
    vScan = 1.0 - smoothstep(0.0, 2.2, abs(c.z - uFilterZ));
  }
`;
const cardFragment = /* glsl */ `
  uniform sampler2D uAtlas; uniform float uAnalysis;
  varying vec2 vUv; varying float vAlpha; varying float vSpam; varying float vScan;
  void main(){
    vec4 tex = texture2D(uAtlas, vUv);
    vec3 col = tex.rgb;
    // skan filtra: spam podświetla się na biało, czyste karty lekko
    col += vec3(1.0) * vScan * uAnalysis * (vSpam > 0.5 ? 0.35 : 0.08);
    gl_FragColor = vec4(col, tex.a * vAlpha);
  }
`;

const burstVertex = /* glsl */ `
  attribute vec2 aLane; attribute float aSpeed; attribute float aPhase; attribute vec3 aDir; attribute float aSeed; attribute float aIdx;
  uniform float uReact; uniform float uPixelRatio;
  varying float vAlpha;
  ${cardPath}
  void main(){
    float z = cardZ(aSpeed, aPhase);
    float local = clamp((z - uFilterZ) / 5.0, 0.0, 1.0);
    vec3 origin = vec3(aLane, uFilterZ);
    vec3 p = origin + vec3(aDir.x * 0.8, aDir.y * 0.25, 0.0) + aDir * local * (2.5 + aSeed * 3.5);
    p.y -= local * local * 1.5;
    vec4 mv = modelViewMatrix * vec4(p, 1.0);
    gl_Position = projectionMatrix * mv;
    gl_PointSize = (1.5 + aSeed * 2.5) * uPixelRatio * (10.0 / -mv.z);
    vAlpha = step(uFilterZ, z) * (1.0 - local) * uReact * uFade;
  }
`;
const burstFragment = /* glsl */ `
  varying float vAlpha;
  void main(){
    float d = length(gl_PointCoord - 0.5);
    if (d > 0.5) discard;
    gl_FragColor = vec4(vec3(1.0), smoothstep(0.5, 0.0, d) * vAlpha);
  }
`;

const filterVertex = /* glsl */ `
  varying vec2 vUv;
  void main(){ vUv = uv; gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }
`;
const filterFragment = /* glsl */ `
  uniform float uTime; uniform float uAnalysis; uniform float uFade;
  varying vec2 vUv;
  void main(){
    vec2 p = vUv - 0.5;
    float r = length(p) * 2.0;
    float ring = smoothstep(0.02, 0.0, abs(r - 0.92)) + smoothstep(0.01, 0.0, abs(r - 0.78)) * 0.5;
    float a = atan(p.y, p.x);
    float scan = smoothstep(0.9, 1.0, sin(a * 1.0 - uTime * 2.0) * 0.5 + 0.5) * step(r, 0.92);
    float grid = (smoothstep(0.015, 0.0, abs(fract(p.x * 14.0) - 0.5) - 0.48) + smoothstep(0.015, 0.0, abs(fract(p.y * 14.0) - 0.5) - 0.48)) * step(r, 0.78) * 0.025 * (1.0 - r);
    float glow = exp(-abs(r - 0.92) * 9.0) * 0.35;
    float intensity = 0.25 + uAnalysis * 0.75;
    float edge = 1.0 - smoothstep(0.96, 1.0, r);
    float alpha = (ring + glow * 0.6 + scan * 0.18 + grid) * intensity * uFade * edge;
    gl_FragColor = vec4(vec3(1.0), alpha);
  }
`;

export default function StoryScene({ slot }: SceneComponentProps) {
  const quality = useThreeStore((s) => s.quality);
  const group = useRef<Group>(null);
  const camera = useThree((st) => st.camera) as PerspectiveCamera;
  const ringMat = useMemo(
    () => new MeshBasicMaterial({ color: "#ffffff", transparent: true, opacity: 0 }),
    [],
  );
  useEffect(() => () => ringMat.dispose(), [ringMat]);

  const atlas = useMemo(() => createAtlas(), []);
  const cardCount = quality === "low" ? 14 : quality === "medium" ? 24 : 40;

  const { cardGeo, cardMat, burstGeo, burstMat, filterMat, uniforms } = useMemo(() => {
    const shared = {
      uTime: { value: 0 },
      uClean: { value: 0 },
      uReact: { value: 0 },
      uAnalysis: { value: 0 },
      uFilterZ: { value: FILTER_Z },
      uPixelRatio: { value: Math.min(window.devicePixelRatio, 2) },
      uFade: { value: 0 },
    };
    const lane = new Float32Array(cardCount * 2);
    const speed = new Float32Array(cardCount);
    const phase = new Float32Array(cardCount);
    const cell = new Float32Array(cardCount);
    const spam = new Float32Array(cardCount);
    const idx = new Float32Array(cardCount);
    for (let i = 0; i < cardCount; i++) {
      lane[i * 2] = (Math.random() - 0.5) * 5.5;
      lane[i * 2 + 1] = (Math.random() - 0.5) * 3.2;
      speed[i] = 0.035 + Math.random() * 0.02;
      phase[i] = i / cardCount + Math.random() * 0.02;
      const isSpam = i % 3 === 0;
      cell[i] = isSpam ? 5 + (i % 3 === 0 ? (i / 3) % 3 : 0) : i % 5;
      spam[i] = isSpam ? 1 : 0;
      idx[i] = i;
    }
    const plane = new PlaneGeometry(2.4, 0.6);
    const cg = new InstancedBufferGeometry();
    cg.index = plane.index;
    cg.setAttribute("position", plane.getAttribute("position"));
    cg.setAttribute("uv", plane.getAttribute("uv"));
    cg.setAttribute("aLane", new InstancedBufferAttribute(lane, 2));
    cg.setAttribute("aSpeed", new InstancedBufferAttribute(speed, 1));
    cg.setAttribute("aPhase", new InstancedBufferAttribute(phase, 1));
    cg.setAttribute("aCell", new InstancedBufferAttribute(cell, 1));
    cg.setAttribute("aSpam", new InstancedBufferAttribute(spam, 1));
    cg.setAttribute("aIdx", new InstancedBufferAttribute(idx, 1));
    cg.instanceCount = cardCount;

    const cm = new ShaderMaterial({
      vertexShader: cardVertex,
      fragmentShader: cardFragment,
      uniforms: { ...shared, uAtlas: { value: atlas } },
      transparent: true,
      depthWrite: false,
      side: DoubleSide,
    });

    // cząsteczki rozbitego spamu
    const spamIdx = [...spam].map((v, i) => (v > 0 ? i : -1)).filter((i) => i >= 0);
    const perCard = Math.max(
      60,
      Math.round(particleBudget(quality, 4800) / Math.max(1, spamIdx.length)),
    );
    const total = perCard * spamIdx.length;
    const bLane = new Float32Array(total * 2);
    const bSpeed = new Float32Array(total);
    const bPhase = new Float32Array(total);
    const bDir = new Float32Array(total * 3);
    const bSeed = new Float32Array(total);
    const bIdx = new Float32Array(total);
    let k = 0;
    for (const ci of spamIdx) {
      for (let j = 0; j < perCard; j++) {
        bLane[k * 2] = lane[ci * 2] ?? 0;
        bLane[k * 2 + 1] = lane[ci * 2 + 1] ?? 0;
        bSpeed[k] = speed[ci] ?? 0;
        bPhase[k] = phase[ci] ?? 0;
        const th = Math.random() * Math.PI * 2;
        const ph = Math.acos(2 * Math.random() - 1);
        bDir[k * 3] = Math.sin(ph) * Math.cos(th);
        bDir[k * 3 + 1] = Math.sin(ph) * Math.sin(th);
        bDir[k * 3 + 2] = Math.abs(Math.cos(ph)) * 0.6;
        bSeed[k] = Math.random();
        bIdx[k] = ci;
        k++;
      }
    }
    const bg = new BufferGeometry();
    bg.setAttribute("position", new BufferAttribute(new Float32Array(total * 3), 3));
    bg.setAttribute("aLane", new BufferAttribute(bLane, 2));
    bg.setAttribute("aSpeed", new BufferAttribute(bSpeed, 1));
    bg.setAttribute("aPhase", new BufferAttribute(bPhase, 1));
    bg.setAttribute("aDir", new BufferAttribute(bDir, 3));
    bg.setAttribute("aSeed", new BufferAttribute(bSeed, 1));
    bg.setAttribute("aIdx", new BufferAttribute(bIdx, 1));
    const bm = new ShaderMaterial({
      vertexShader: burstVertex,
      fragmentShader: burstFragment,
      uniforms: shared,
      transparent: true,
      depthWrite: false,
      blending: AdditiveBlending,
    });
    const fm = new ShaderMaterial({
      vertexShader: filterVertex,
      fragmentShader: filterFragment,
      uniforms: shared,
      transparent: true,
      depthWrite: false,
      blending: AdditiveBlending,
      side: DoubleSide,
    });
    return {
      cardGeo: cg,
      cardMat: cm,
      burstGeo: bg,
      burstMat: bm,
      filterMat: fm,
      uniforms: shared,
    };
  }, [atlas, cardCount, quality]);

  useEffect(
    () => () => {
      cardGeo.dispose();
      cardMat.dispose();
      burstGeo.dispose();
      burstMat.dispose();
      filterMat.dispose();
    },
    [cardGeo, cardMat, burstGeo, burstMat, filterMat],
  );
  useEffect(() => () => atlas.dispose(), [atlas]);

  useFrame((state, delta) => {
    if (!isSlotVisible(slot.id) || !group.current) return;
    const p = useScrollStore.getState().storyProgress;
    const { pointer } = useScrollStore.getState();
    uniforms.uTime.value = state.clock.elapsedTime;
    easing.damp(uniforms.uAnalysis, "value", smoothstep(0.18, 0.32, p), 0.2, delta);
    easing.damp(uniforms.uReact, "value", smoothstep(0.44, 0.54, p), 0.2, delta);
    easing.damp(uniforms.uClean, "value", smoothstep(0.7, 0.84, p), 0.25, delta);
    // „kamera” leci przez scenę: przesuwamy świat w stronę widza i lekko obracamy po krzywej
    // zanikanie przy wejściu/wyjściu sekcji
    const tr = trackElement(slot.track, camera, 0);
    if (tr) {
      const vh = window.innerHeight;
      const fadeIn = Math.min(1, Math.max(0, 1 - tr.rect.top / vh));
      const fadeOut = Math.min(1, Math.max(0, (tr.rect.bottom - vh * 0.15) / (vh * 0.6)));
      uniforms.uFade.value = Math.min(fadeIn, fadeOut);
      ringMat.opacity = uniforms.uFade.value * (0.5 + uniforms.uAnalysis.value * 0.5);
    }
    const wide = window.innerWidth >= 1024;
    const fly = p * 2.5;
    easing.damp3(
      group.current.position,
      [
        (wide ? 2.6 : 0) + Math.sin(p * Math.PI) * -0.8 + pointer.x * 0.3,
        pointer.y * 0.2 + (wide ? 0 : -0.6),
        fly,
      ],
      0.15,
      delta,
    );
    easing.dampE(
      group.current.rotation,
      [0.08 - p * 0.1, Math.sin(p * Math.PI) * 0.25, 0],
      0.2,
      delta,
    );
  });

  return (
    <group ref={group}>
      <mesh geometry={cardGeo} material={cardMat} frustumCulled={false} />
      <points geometry={burstGeo} material={burstMat} frustumCulled={false} />
      <mesh position={[0, 0, FILTER_Z]} material={filterMat}>
        <planeGeometry args={[7.5, 7.5]} />
      </mesh>
      <mesh position={[0, 0, FILTER_Z]}>
        <torusGeometry args={[3.45, 0.025, 12, 160]} />
        <primitive object={ringMat} attach="material" />
      </mesh>
    </group>
  );
}

function smoothstep(a: number, b: number, x: number): number {
  const t = Math.min(1, Math.max(0, (x - a) / (b - a)));
  return t * t * (3 - 2 * t);
}
