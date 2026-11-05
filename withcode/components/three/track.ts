"use client";
import type { RefObject } from "react";
import type { PerspectiveCamera } from "three";

/**
 * Przelicza prostokąt elementu DOM na pozycję i skalę w świecie (płaszczyzna z = depth).
 * Dzięki temu obiekty 3D „siedzą” w sekcjach, choć Canvas jest jeden i pełnoekranowy.
 */
export function trackElement(
  ref: RefObject<HTMLElement | null>,
  camera: PerspectiveCamera,
  depth = 0,
): { x: number; y: number; scale: number; progress: number; rect: DOMRect } | null {
  const el = ref.current;
  if (!el) return null;
  const rect = el.getBoundingClientRect();
  const vh = window.innerHeight;
  const vw = window.innerWidth;
  const dist = camera.position.z - depth;
  const worldH = 2 * Math.tan((camera.fov * Math.PI) / 360) * dist;
  const unit = worldH / vh;
  const cx = rect.left + rect.width / 2;
  const cy = rect.top + rect.height / 2;
  return {
    x: (cx - vw / 2) * unit,
    y: -(cy - vh / 2) * unit,
    scale: rect.height * unit,
    progress: Math.min(1, Math.max(0, -rect.top / Math.max(1, rect.height))),
    rect,
  };
}
