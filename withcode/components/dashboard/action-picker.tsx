"use client";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";

const ACTIONS = ["DELETE", "WARN", "TIMEOUT", "KICK", "BAN"] as const;
type A = (typeof ACTIONS)[number];

export function ActionPicker({
  value,
  onChange,
  labelledBy,
}: {
  value: A[];
  onChange: (v: A[]) => void;
  labelledBy?: string;
}) {
  const t = useTranslations("dashboard.actions");
  return (
    <div role="group" aria-labelledby={labelledBy} className="flex flex-wrap gap-1.5">
      {ACTIONS.map((a) => {
        const on = value.includes(a);
        return (
          <button
            key={a}
            type="button"
            aria-pressed={on}
            onClick={() => {
              const next = on ? value.filter((x) => x !== a) : [...value, a];
              onChange(next.length ? next : value);
            }}
            className={cn(
              "rounded-full border px-3 py-1 text-xs transition-colors",
              on
                ? "border-white bg-white text-black"
                : "border-border text-fg-muted hover:border-white/40 hover:text-white",
            )}
          >
            {t(a)}
          </button>
        );
      })}
    </div>
  );
}

export const DURATIONS = [60, 300, 600, 3600, 21600, 86400, 604800] as const;

export function DurationSelect({
  value,
  onChange,
  id,
}: {
  value: number | null;
  onChange: (v: number) => void;
  id?: string;
}) {
  const t = useTranslations("dashboard.durations");
  return (
    <select
      id={id}
      value={value ?? 600}
      onChange={(e) => onChange(Number(e.target.value))}
      className="h-10 rounded-md border border-border-strong bg-surface-1 px-3 text-sm text-white focus-visible:border-white focus-visible:outline-none"
    >
      {DURATIONS.map((d) => (
        <option key={d} value={d}>
          {t(String(d) as `${(typeof DURATIONS)[number]}`)}
        </option>
      ))}
    </select>
  );
}
