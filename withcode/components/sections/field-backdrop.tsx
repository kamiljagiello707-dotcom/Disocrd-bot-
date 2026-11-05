"use client";
import { useRef, useState, type ReactNode } from "react";
import { SceneSlot } from "@/components/three/scene-slot";

/** Pole cząsteczek za całą zawartością strony (jeden slot dla wszystkich sekcji). */
export function FieldBackdrop({
  children,
  intensity,
}: {
  children: ReactNode;
  intensity?: number;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const [data] = useState(() => (intensity === undefined ? undefined : { intensity }));
  return (
    <div ref={ref}>
      <SceneSlot scene="field" track={ref} data={data} rootMargin="0px" />
      {children}
    </div>
  );
}
