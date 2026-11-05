"use client";
import { Suspense, lazy, useEffect, useMemo, useState, type ComponentType } from "react";
import { Canvas, useThree } from "@react-three/fiber";
import { PerformanceMonitor, Environment, Lightformer } from "@react-three/drei";
import {
  EffectComposer,
  Bloom,
  ChromaticAberration,
  Noise,
  Vignette,
} from "@react-three/postprocessing";
import { BlendFunction } from "postprocessing";
import { Vector2 } from "three";
import { useThreeStore, type Quality, type SceneKey, type Slot } from "./store";
import { useScrollStore } from "@/lib/stores/scroll";
import HeroScene from "./scenes/hero";
import FieldScene from "./scenes/field";

const StoryScene = lazy(() => import("./scenes/story"));
const GlobeScene = lazy(() => import("./scenes/globe"));
const NotFoundScene = lazy(() => import("./scenes/not-found"));

export type SceneComponentProps = { slot: Slot };

const registry: Record<SceneKey, ComponentType<SceneComponentProps>> = {
  hero: HeroScene,
  field: FieldScene,
  story: StoryScene,
  globe: GlobeScene,
  notfound: NotFoundScene,
};

const order: Quality[] = ["low", "medium", "high"];

function initialQuality(): Quality {
  const nav = navigator as Navigator & { deviceMemory?: number };
  const cores = nav.hardwareConcurrency ?? 4;
  const mem = nav.deviceMemory ?? 8;
  const coarse = window.matchMedia("(pointer: coarse)").matches;
  const small = Math.min(window.innerWidth, window.innerHeight) < 700;
  if (coarse || small || cores <= 4 || mem <= 4) return coarse && cores <= 4 ? "low" : "medium";
  return "high";
}

/** Odświeża klatkę w trybie „demand” (reduced-motion) przy scrollu i zmianach slotów. */
function DemandInvalidator({ enabled }: { enabled: boolean }) {
  const invalidate = useThree((s) => s.invalidate);
  useEffect(() => {
    if (!enabled) return;
    const a = useScrollStore.subscribe(() => invalidate());
    const b = useThreeStore.subscribe(() => invalidate());
    const onResize = () => invalidate();
    window.addEventListener("scroll", onResize, { passive: true });
    return () => {
      a();
      b();
      window.removeEventListener("scroll", onResize);
    };
  }, [enabled, invalidate]);
  return null;
}

function Effects({ quality }: { quality: Quality }) {
  const offset = useMemo(() => new Vector2(0.0007, 0.0004), []);
  if (quality === "low") return null;
  return (
    <EffectComposer multisampling={0} enableNormalPass={false}>
      <Bloom
        mipmapBlur
        intensity={quality === "high" ? 0.9 : 0.6}
        luminanceThreshold={0.62}
        luminanceSmoothing={0.2}
      />
      <ChromaticAberration
        offset={offset}
        radialModulation
        modulationOffset={0.35}
        blendFunction={quality === "high" ? BlendFunction.NORMAL : BlendFunction.SKIP}
      />
      <Noise premultiply opacity={0.035} />
      <Vignette offset={0.32} darkness={0.72} />
    </EffectComposer>
  );
}

export default function CanvasRoot() {
  const slots = useThreeStore((s) => s.slots);
  const quality = useThreeStore((s) => s.quality);
  const setQuality = useThreeStore((s) => s.setQuality);
  const [reduced] = useState(() => window.matchMedia("(prefers-reduced-motion: reduce)").matches);
  const [hidden, setHidden] = useState(false);

  useEffect(() => {
    setQuality(initialQuality());
    const onVis = () => setHidden(document.visibilityState === "hidden");
    document.addEventListener("visibilitychange", onVis);
    return () => document.removeEventListener("visibilitychange", onVis);
  }, [setQuality]);

  const list = Object.values(slots);
  const anyVisible = list.some((s) => s.visible);
  const frameloop = hidden || !anyVisible ? "never" : reduced ? "demand" : "always";
  const dpr: [number, number] =
    quality === "high" ? [1, 2] : quality === "medium" ? [1, 1.5] : [1, 1];

  return (
    <div
      aria-hidden
      className="pointer-events-none fixed inset-0 z-[var(--z-canvas)] transition-opacity duration-700"
      style={{ opacity: list.length > 0 ? 1 : 0 }}
    >
      <Canvas
        frameloop={frameloop}
        dpr={dpr}
        camera={{ position: [0, 0, 6], fov: 35, near: 0.1, far: 60 }}
        gl={{ antialias: false, alpha: false, powerPreference: "high-performance", stencil: false }}
        eventSource={typeof document !== "undefined" ? document.body : undefined}
        eventPrefix="client"
        onCreated={({ gl }) => {
          gl.setClearColor("#000000", 1);
          useThreeStore.setState({ ready: true });
          gl.domElement.addEventListener("webglcontextlost", (e) => {
            e.preventDefault();
            useThreeStore.setState({ webgl: "off" });
            document.documentElement.dataset.webgl = "off";
          });
        }}
      >
        <color attach="background" args={["#000000"]} />
        <PerformanceMonitor
          flipflops={3}
          onDecline={() => {
            const i = order.indexOf(useThreeStore.getState().quality);
            if (i > 0) setQuality(order[i - 1] ?? "low");
          }}
          onIncline={() => {
            const i = order.indexOf(useThreeStore.getState().quality);
            if (i < order.length - 1) setQuality(order[i + 1] ?? "high");
          }}
          onFallback={() => setQuality("low")}
        />
        <Environment resolution={quality === "high" ? 256 : 128} frames={1}>
          <Lightformer form="rect" intensity={4} position={[0, 4, -2]} scale={[10, 2, 1]} />
          <Lightformer
            form="rect"
            intensity={2}
            position={[-5, 0, 2]}
            rotation-y={Math.PI / 2}
            scale={[8, 2, 1]}
          />
          <Lightformer
            form="rect"
            intensity={2}
            position={[5, 1, 0]}
            rotation-y={-Math.PI / 2}
            scale={[8, 1, 1]}
          />
          <Lightformer form="ring" intensity={3} position={[0, 0, 6]} scale={3} />
        </Environment>
        <DemandInvalidator enabled={reduced} />
        <Suspense fallback={null}>
          {list.map((slot) => {
            const Scene = registry[slot.scene];
            return (
              <group key={slot.id} visible={slot.visible}>
                <Scene slot={slot} />
              </group>
            );
          })}
        </Suspense>
        <Effects quality={quality} />
      </Canvas>
    </div>
  );
}
