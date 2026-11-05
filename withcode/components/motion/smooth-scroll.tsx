"use client";
import { useEffect } from "react";
import Lenis from "lenis";
import { gsap, ScrollTrigger } from "@/lib/gsap";
import { useScrollStore } from "@/lib/stores/scroll";

/** Lenis + GSAP na jednym zegarze (gsap.ticker). Wyłączony przy prefers-reduced-motion. */
export function SmoothScroll() {
  useEffect(() => {
    const onPointer = (e: PointerEvent) => {
      useScrollStore.setState({
        pointer: {
          x: (e.clientX / window.innerWidth) * 2 - 1,
          y: -((e.clientY / window.innerHeight) * 2 - 1),
        },
      });
    };
    window.addEventListener("pointermove", onPointer, { passive: true });

    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      return () => window.removeEventListener("pointermove", onPointer);
    }

    const lenis = new Lenis({ autoRaf: false, lerp: 0.1, smoothWheel: true, anchors: true });
    lenis.on("scroll", (l: Lenis) => {
      ScrollTrigger.update();
      useScrollStore.setState({ velocity: l.velocity });
    });
    const update = (time: number) => lenis.raf(time * 1000);
    gsap.ticker.add(update);
    gsap.ticker.lagSmoothing(0);
    (window as Window & { __lenis?: Lenis }).__lenis = lenis;

    return () => {
      window.removeEventListener("pointermove", onPointer);
      gsap.ticker.remove(update);
      lenis.destroy();
      delete (window as Window & { __lenis?: Lenis }).__lenis;
    };
  }, []);
  return null;
}
