'use client';

import { useMemo, useRef } from 'react';
import * as THREE from 'three';
import { useFrame, useThree } from '@react-three/fiber';
import { Float } from '@react-three/drei';
import { sceneState } from '@/store/scene';
import { Garment } from './garments/Garment';
import { HeroLogo } from './HeroLogo';
import { applyPresetBlend, createFabricMaterial } from './materials';

/**
 * SCROLL-STORY („4D”)
 * --------------------
 * Klatki kluczowe sceny przypisane do ciągłej osi `sceneState.story`
 * (indeks sekcji strony głównej + postęp w sekcji, ustawiany przez ScrollTrigger).
 * W każdej klatce interpolujemy: pozycję kamery, punkt patrzenia, położenie i skalę
 * modelu, siłę eksplozji, preset materiału oraz widoczność logo 3D.
 *
 * 0 – Hero · 1 – Nowa kolekcja · 2 – Bestsellery · 3–4 – Historia marki · 4 – Lookbook · 5 – Newsletter
 */

interface Keyframe {
  t: number;
  cam: [number, number, number];
  look: [number, number, number];
  pos: [number, number, number];
  scale: number;
  explode: number;
  preset: number;
  logo: number;
  spin: number;
}

const KEYFRAMES: Keyframe[] = [
  { t: 0, cam: [0, 0.1, 6.4], look: [0, 0, 0], pos: [0, -0.05, 0.4], scale: 1, explode: 0, preset: 0, logo: 1, spin: 0 },
  { t: 1, cam: [0, 0.3, 6.6], look: [0, 0, 0], pos: [2.6, -0.2, -1], scale: 0.75, explode: 0, preset: 0, logo: 0, spin: 0.6 },
  { t: 2, cam: [0, 0, 7], look: [0, 0, 0], pos: [-4.4, -0.4, -2.2], scale: 0.9, explode: 0, preset: 1, logo: 0, spin: 1.2 },
  // Historia marki – kamera przelatuje wokół, model rozkłada się i składa
  { t: 3, cam: [0, 0.2, 6.2], look: [0, 0, 0], pos: [0, 0, 0], scale: 1, explode: 0, preset: 1, logo: 0, spin: 1.6 },
  { t: 3.3, cam: [3.2, 0.9, 4.4], look: [0, 0.1, 0], pos: [0, 0, 0], scale: 1, explode: 1, preset: 1.5, logo: 0, spin: 2.0 },
  { t: 3.62, cam: [-2.6, -0.4, 3.4], look: [0, 0, 0], pos: [0, 0, 0], scale: 1.05, explode: 1.25, preset: 2, logo: 0, spin: 2.6 },
  { t: 3.9, cam: [0, 0.4, 5.6], look: [0, 0, 0], pos: [0, 0, 0], scale: 1, explode: 0, preset: 2, logo: 0, spin: 3.4 },
  { t: 4, cam: [0, 0, 7.5], look: [0, 0, 0], pos: [0, 0, -2.2], scale: 0.7, explode: 0, preset: 3, logo: 0, spin: 3.6 },
  { t: 5, cam: [0, 0.1, 6.2], look: [0, 0, 0], pos: [0, 0, 0], scale: 0.95, explode: 0, preset: 0, logo: 0, spin: 4.2 },
  { t: 6, cam: [0, 0.1, 6.2], look: [0, 0, 0], pos: [0, 2.8, -1], scale: 0.6, explode: 0, preset: 0, logo: 0, spin: 4.6 },
];

/** Gdy jesteśmy poza stroną główną: model chowa się, zostaje tło z cząsteczkami. */
const AWAY: Keyframe = { t: 0, cam: [0, 0, 7], look: [0, 0, 0], pos: [3.5, -1, -4], scale: 0.0001, explode: 0.6, preset: 0, logo: 0, spin: 0 };

const smooth = (x: number) => x * x * (3 - 2 * x);
const lerp = THREE.MathUtils.lerp;

function sample(t: number): Keyframe {
  const last = KEYFRAMES[KEYFRAMES.length - 1];
  if (t <= KEYFRAMES[0].t) return KEYFRAMES[0];
  if (t >= last.t) return last;
  let i = 0;
  while (i < KEYFRAMES.length - 1 && KEYFRAMES[i + 1].t < t) i++;
  const a = KEYFRAMES[i];
  const b = KEYFRAMES[i + 1];
  const f = smooth((t - a.t) / (b.t - a.t));
  return {
    t,
    cam: [lerp(a.cam[0], b.cam[0], f), lerp(a.cam[1], b.cam[1], f), lerp(a.cam[2], b.cam[2], f)],
    look: [lerp(a.look[0], b.look[0], f), lerp(a.look[1], b.look[1], f), lerp(a.look[2], b.look[2], f)],
    pos: [lerp(a.pos[0], b.pos[0], f), lerp(a.pos[1], b.pos[1], f), lerp(a.pos[2], b.pos[2], f)],
    scale: lerp(a.scale, b.scale, f),
    explode: lerp(a.explode, b.explode, f),
    preset: lerp(a.preset, b.preset, f),
    logo: lerp(a.logo, b.logo, f),
    spin: lerp(a.spin, b.spin, f),
  };
}

export function StoryExperience({ quality, reduced, mobile }: { quality: 'high' | 'low'; reduced: boolean; mobile: boolean }) {
  const camera = useThree((s) => s.camera);
  const viewport = useThree((s) => s.viewport);
  const material = useMemo(() => createFabricMaterial('#141414'), []);
  const explode = useRef(0);
  const garment = useRef<THREE.Group>(null);
  const tilt = useRef<THREE.Group>(null);
  const logo = useRef<THREE.Group>(null);
  const look = useRef(new THREE.Vector3());
  const tmp = useMemo(() => new THREE.Vector3(), []);
  const state = useRef({ preset: 0, logo: 1, autoSpin: 0, scale: 1 });

  // Dopasowanie do kadru: na wąskich (pionowych) ekranach model i logo skalujemy osobno,
  // a logo przenosimy nad model – inaczej oba elementy byłyby mikroskopijne.
  const portrait = viewport.aspect < 1;
  const fit = Math.min(1, viewport.width / (portrait ? 2.9 : 7.2));
  const logoFit = Math.min(1, viewport.width / (portrait ? 7.4 : 6.4));
  const logoY = portrait ? 1.05 : 0.15;

  useFrame((_, rawDelta) => {
    const delta = Math.min(rawDelta, 0.05);
    const k = sceneState.home ? sample(sceneState.story) : AWAY;
    // Współczynnik tłumienia niezależny od FPS
    const damp = reduced ? 1 : 1 - Math.exp(-delta * 3.2);
    const p = sceneState.pointer;
    const mobileShift = mobile ? 0.45 : 1;

    // Kamera + paralaksa kursora
    camera.position.x = lerp(camera.position.x, k.cam[0] + p.x * 0.35, damp);
    camera.position.y = lerp(camera.position.y, k.cam[1] + p.y * 0.2, damp);
    camera.position.z = lerp(camera.position.z, k.cam[2], damp);
    look.current.lerp(tmp.set(k.look[0], k.look[1], k.look[2]), damp);
    camera.lookAt(look.current);

    if (garment.current) {
      const g = garment.current;
      g.position.x = lerp(g.position.x, k.pos[0] * mobileShift, damp);
      g.position.y = lerp(g.position.y, k.pos[1] - (portrait && sceneState.story < 1 ? 0.25 : 0), damp);
      g.position.z = lerp(g.position.z, k.pos[2], damp);
      state.current.scale = lerp(state.current.scale, k.scale * fit, damp);
      g.scale.setScalar(state.current.scale);
      // Model schowany (podstrony) – pomijamy go w renderowaniu
      g.visible = state.current.scale > 0.01;
      if (!reduced) state.current.autoSpin += delta * 0.22;
      g.rotation.y = lerp(g.rotation.y, k.spin * Math.PI * 0.5 + state.current.autoSpin, damp);
    }

    // Przechylenie od kursora (osobna grupa, by nie mieszać z obrotem fabularnym)
    if (tilt.current) {
      tilt.current.rotation.x = lerp(tilt.current.rotation.x, -p.y * 0.18, damp);
      tilt.current.rotation.z = lerp(tilt.current.rotation.z, -p.x * 0.08, damp);
    }

    explode.current = lerp(explode.current, k.explode, damp);
    state.current.preset = lerp(state.current.preset, k.preset, damp);
    applyPresetBlend(material, state.current.preset, 1);

    if (logo.current) {
      state.current.logo = lerp(state.current.logo, k.logo, damp);
      const l = state.current.logo;
      logo.current.position.set(0, logoY + (1 - l) * 3.2, portrait ? -1.2 : -1.6);
      logo.current.scale.setScalar(Math.max(0.0001, logoFit * (0.85 + l * 0.15)));
      logo.current.visible = l > 0.01;
      logo.current.rotation.y = p.x * 0.12 * l;
      logo.current.rotation.x = -p.y * 0.08 * l;
    }
  });

  return (
    <>
      <HeroLogo ref={logo} quality={quality} />
      <group ref={garment}>
        <Float enabled={!reduced} speed={1.4} rotationIntensity={0.25} floatIntensity={0.6} floatingRange={[-0.08, 0.08]}>
          <group ref={tilt}>
            <Garment type="hoodie" material={material} explode={explode} distance={1.5} />
          </group>
        </Float>
      </group>
    </>
  );
}
