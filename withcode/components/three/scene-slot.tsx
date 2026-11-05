"use client";
import { useEffect, useId, type RefObject } from "react";
import { useThreeStore, type SceneData, type SceneKey } from "./store";

/**
 * Rejestruje scenę we współdzielonym Canvasie (tunel DOM → R3F).
 * Sama scena żyje w osobnym chunku ładowanym dynamicznie — strona nie importuje three.js.
 */
export function SceneSlot<K extends SceneKey>({
  scene,
  track,
  data,
  rootMargin = "15% 0px",
}: {
  scene: K;
  track: RefObject<HTMLElement | null>;
  data?: SceneData[K];
  rootMargin?: string;
}) {
  const id = `${scene}-${useId()}`;
  const register = useThreeStore((s) => s.register);
  const unregister = useThreeStore((s) => s.unregister);
  const setVisible = useThreeStore((s) => s.setVisible);

  useEffect(() => {
    register({ id, scene, track, data: data as SceneData[SceneKey], visible: false });
    const el = track.current;
    let io: IntersectionObserver | undefined;
    if (el) {
      io = new IntersectionObserver(([e]) => setVisible(id, Boolean(e?.isIntersecting)), {
        rootMargin,
      });
      io.observe(el);
    }
    return () => {
      io?.disconnect();
      unregister(id);
    };
  }, [id, scene, track, data, rootMargin, register, unregister, setVisible]);

  return null;
}
