"use client";
import { useEffect, useRef, useState } from "react";

const POOL = "!<>-_\\/[]{}—=+*^?#01";

/** Efekt „scramble”: litery losują się, zanim ułożą w docelowe słowo. */
export function Scramble({
  text,
  className,
  trigger = "view",
}: {
  text: string;
  className?: string;
  trigger?: "view" | "hover" | "mount";
}) {
  const [out, setOut] = useState(text);
  const ref = useRef<HTMLSpanElement>(null);
  const running = useRef(false);

  const run = () => {
    if (running.current || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    running.current = true;
    const start = performance.now();
    const duration = 700 + text.length * 22;
    const tick = (now: number) => {
      const progress = Math.min(1, (now - start) / duration);
      setOut(
        [...text]
          .map((c, i) => {
            if (c === " ") return " ";
            if (i / text.length < progress) return c;
            return POOL[Math.floor(Math.random() * POOL.length)] ?? c;
          })
          .join(""),
      );
      if (progress < 1) requestAnimationFrame(tick);
      else {
        setOut(text);
        running.current = false;
      }
    };
    requestAnimationFrame(tick);
  };

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (trigger === "mount") {
      run();
      return;
    }
    if (trigger !== "view") return;
    const io = new IntersectionObserver(
      ([entry]) => {
        if (entry?.isIntersecting) {
          run();
          io.disconnect();
        }
      },
      { threshold: 0.6 },
    );
    io.observe(el);
    return () => io.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text, trigger]);

  return (
    <span ref={ref} className={className} onPointerEnter={trigger === "hover" ? run : undefined}>
      <span className="sr-only">{text}</span>
      <span aria-hidden>{out}</span>
    </span>
  );
}
