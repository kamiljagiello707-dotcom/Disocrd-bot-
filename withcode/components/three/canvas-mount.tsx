"use client";
import dynamic from "next/dynamic";
import { useEffect, useState } from "react";
import { useThreeStore } from "./store";

const CanvasRoot = dynamic(() => import("./canvas-root"), { ssr: false });

/**
 * WebGL tylko z akceleracją sprzętową. Renderer programowy (SwiftShader, llvmpipe — słabe
 * urządzenia, maszyny bez GPU, część botów) dostaje statyczny fallback zamiast zamrożonej strony.
 * `?gl=force` wymusza sceny (podgląd/debug).
 */
function hasWebGL(): boolean {
  try {
    const force = new URLSearchParams(location.search).get("gl") === "force";
    const c = document.createElement("canvas");
    const attrs = { failIfMajorPerformanceCaveat: !force };
    const gl = (c.getContext("webgl2", attrs) ??
      c.getContext("webgl", attrs)) as WebGLRenderingContext | null;
    if (!gl) return false;
    if (force) return true;
    const dbg = gl.getExtension("WEBGL_debug_renderer_info");
    const renderer = String(
      (dbg ? gl.getParameter(dbg.UNMASKED_RENDERER_WEBGL) : gl.getParameter(gl.RENDERER)) ?? "",
    );
    gl.getExtension("WEBGL_lose_context")?.loseContext();
    return !/swiftshader|llvmpipe|software|basic render/i.test(renderer);
  } catch {
    return false;
  }
}

/** Montuje jeden Canvas dopiero, gdy jakaś sekcja zarejestruje scenę i przeglądarka jest bezczynna. */
export function CanvasMount() {
  const hasSlots = useThreeStore((s) => Object.keys(s.slots).length > 0);
  const [mount, setMount] = useState(false);

  useEffect(() => {
    if (!hasSlots || mount) return;
    const ok = hasWebGL();
    useThreeStore.setState({ webgl: ok ? "on" : "off" });
    document.documentElement.dataset.webgl = ok ? "on" : "off";
    if (!ok) return;
    const idle = window.requestIdleCallback ?? ((cb: () => void) => window.setTimeout(cb, 200));
    const cancel = window.cancelIdleCallback ?? window.clearTimeout;
    const id = idle(() => setMount(true), { timeout: 1200 });
    return () => cancel(id);
  }, [hasSlots, mount]);

  return mount ? <CanvasRoot /> : null;
}
