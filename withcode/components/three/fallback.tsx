import { cn } from "@/lib/utils";

/** Statyczna grafika zamiast sceny 3D (brak WebGL / utrata kontekstu). */
export function HeroFallback({ className }: { className?: string }) {
  return (
    <div
      aria-hidden
      className={cn("webgl-fallback pointer-events-none absolute inset-0 hidden", className)}
    >
      <div className="absolute inset-0 bg-[radial-gradient(60%_50%_at_70%_45%,rgba(255,255,255,0.10),transparent_70%)]" />
      <svg
        viewBox="0 0 200 220"
        className="absolute top-1/2 right-[8%] hidden w-[28vw] max-w-[420px] -translate-y-1/2 lg:block"
      >
        <defs>
          <linearGradient id="fb-chrome" x1="0" x2="1" y1="0" y2="1">
            <stop offset="0" stopColor="#fff" />
            <stop offset="0.45" stopColor="#6b6b6b" />
            <stop offset="0.6" stopColor="#e6e6e6" />
            <stop offset="1" stopColor="#3a3a3a" />
          </linearGradient>
          <radialGradient id="fb-core" cx="0.4" cy="0.35" r="0.7">
            <stop offset="0" stopColor="#3a3a3a" />
            <stop offset="1" stopColor="#050505" />
          </radialGradient>
        </defs>
        <path
          d="M100 8c30 0 58 10 78 24 2 54-18 132-78 180C40 164 20 86 22 32 42 18 70 8 100 8Z"
          fill="none"
          stroke="url(#fb-chrome)"
          strokeWidth="10"
        />
        <circle cx="100" cy="100" r="44" fill="url(#fb-core)" stroke="rgba(255,255,255,0.35)" />
      </svg>
    </div>
  );
}
