"use client";
import { useEffect, useRef } from "react";

/** Własny kursor: punkt + pierścień z opóźnieniem. Ukryty na dotyku i przy reduced-motion. */
export function Cursor() {
  const dot = useRef<HTMLDivElement>(null);
  const ring = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const mq = window.matchMedia("(pointer: fine) and (prefers-reduced-motion: no-preference)");
    if (!mq.matches) return;
    const d = dot.current;
    const r = ring.current;
    if (!d || !r) return;
    document.documentElement.classList.add("has-cursor");

    let x = -100;
    let y = -100;
    let rx = -100;
    let ry = -100;
    let scale = 1;
    let target = 1;
    let visible = false;
    let raf = 0;

    const loop = () => {
      rx += (x - rx) * 0.16;
      ry += (y - ry) * 0.16;
      scale += (target - scale) * 0.2;
      d.style.transform = `translate3d(${x}px, ${y}px, 0) translate(-50%, -50%)`;
      r.style.transform = `translate3d(${rx}px, ${ry}px, 0) translate(-50%, -50%) scale(${scale})`;
      raf = requestAnimationFrame(loop);
    };
    raf = requestAnimationFrame(loop);

    const onMove = (e: PointerEvent) => {
      x = e.clientX;
      y = e.clientY;
      if (!visible) {
        visible = true;
        d.style.opacity = "1";
        r.style.opacity = "1";
      }
      const el =
        e.target instanceof Element
          ? e.target.closest(
              "a, button, [role='button'], [data-cursor], input, textarea, select, label",
            )
          : null;
      const kind = el?.getAttribute("data-cursor");
      const isField = el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement;
      target = isField ? 0.6 : el ? (kind === "lg" ? 2.4 : 1.8) : 1;
      r.dataset.state = isField ? "text" : el ? "hover" : "idle";
    };
    const onLeave = () => {
      visible = false;
      d.style.opacity = "0";
      r.style.opacity = "0";
    };
    const onDown = () => (target *= 0.8);
    const onUp = () => (target /= 0.8);

    window.addEventListener("pointermove", onMove, { passive: true });
    document.addEventListener("pointerleave", onLeave);
    window.addEventListener("pointerdown", onDown);
    window.addEventListener("pointerup", onUp);
    return () => {
      cancelAnimationFrame(raf);
      document.documentElement.classList.remove("has-cursor");
      window.removeEventListener("pointermove", onMove);
      document.removeEventListener("pointerleave", onLeave);
      window.removeEventListener("pointerdown", onDown);
      window.removeEventListener("pointerup", onUp);
    };
  }, []);

  return (
    <div
      aria-hidden
      className="pointer-events-none fixed inset-0 z-[var(--z-cursor)] hidden [@media(pointer:fine)]:block"
    >
      <div
        ref={dot}
        className="absolute top-0 left-0 size-1.5 rounded-full bg-white opacity-0 mix-blend-difference transition-opacity duration-300"
      />
      <div
        ref={ring}
        className="absolute top-0 left-0 size-9 rounded-full border border-white/70 opacity-0 mix-blend-difference transition-[opacity,background-color,border-color] duration-300 data-[state=hover]:border-white data-[state=hover]:bg-white/10"
      />
    </div>
  );
}
