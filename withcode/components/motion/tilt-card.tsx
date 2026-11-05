"use client";
import { useRef, type ReactNode } from "react";
import { cn } from "@/lib/utils";

/** Karta z efektem tilt 3D (CSS) i odbiciem światła podążającym za kursorem. */
export function TiltCard({
  children,
  className,
  max = 8,
}: {
  children: ReactNode;
  className?: string;
  max?: number;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const raf = useRef(0);

  const onMove = (e: React.PointerEvent<HTMLDivElement>) => {
    const el = ref.current;
    if (!el || e.pointerType !== "mouse") return;
    const r = el.getBoundingClientRect();
    const px = (e.clientX - r.left) / r.width;
    const py = (e.clientY - r.top) / r.height;
    cancelAnimationFrame(raf.current);
    raf.current = requestAnimationFrame(() => {
      el.style.setProperty("--rx", `${((0.5 - py) * max).toFixed(2)}deg`);
      el.style.setProperty("--ry", `${((px - 0.5) * max).toFixed(2)}deg`);
      el.style.setProperty("--gx", `${(px * 100).toFixed(1)}%`);
      el.style.setProperty("--gy", `${(py * 100).toFixed(1)}%`);
      el.style.setProperty("--go", "1");
    });
  };
  const onLeave = () => {
    const el = ref.current;
    if (!el) return;
    el.style.setProperty("--rx", "0deg");
    el.style.setProperty("--ry", "0deg");
    el.style.setProperty("--go", "0");
  };

  return (
    <div className="[perspective:1000px]">
      <div
        ref={ref}
        onPointerMove={onMove}
        onPointerLeave={onLeave}
        className={cn(
          "group/tilt glass relative h-full overflow-hidden rounded-lg transition-transform duration-500 ease-out-expo will-change-transform motion-reduce:transform-none",
          "[transform:rotateX(var(--rx,0deg))_rotateY(var(--ry,0deg))] [transform-style:preserve-3d]",
          className,
        )}
      >
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 opacity-[var(--go,0)] transition-opacity duration-500"
          style={{
            background:
              "radial-gradient(600px circle at var(--gx,50%) var(--gy,50%), rgb(255 255 255 / 0.10), transparent 40%)",
          }}
        />
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 rounded-[inherit] opacity-[var(--go,0)] transition-opacity duration-500"
          style={{
            padding: 1,
            background:
              "radial-gradient(400px circle at var(--gx,50%) var(--gy,50%), rgb(255 255 255 / 0.5), transparent 40%)",
            WebkitMask: "linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0)",
            WebkitMaskComposite: "xor",
            maskComposite: "exclude",
          }}
        />
        <div className="relative [transform:translateZ(24px)]">{children}</div>
      </div>
    </div>
  );
}
