import { cn } from "@/lib/utils";

/** Tymczasowy znak „W” — podmień ścieżkę SVG na finalne logo. */
export function LogoMark({ className, title = "WithBot" }: { className?: string; title?: string }) {
  return (
    <svg viewBox="0 0 32 32" className={cn("size-7", className)} role="img" aria-label={title}>
      <rect
        x="0.5"
        y="0.5"
        width="31"
        height="31"
        rx="9"
        fill="#000"
        stroke="#fff"
        strokeOpacity="0.25"
      />
      <path
        d="M7 10h3.1l2.3 9.2L15 10h2l2.6 9.2L21.9 10H25l-4 12.5h-2.9L16 14.6l-2.1 7.9H11L7 10Z"
        fill="#fff"
      />
    </svg>
  );
}

export function Logo({ className }: { className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <LogoMark />
      <span className="font-display text-[1.05rem] font-semibold tracking-tight text-white">
        WithCode<span className="text-fg-subtle">.pl</span>
      </span>
    </span>
  );
}
