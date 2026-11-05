"use client";
import { create } from "zustand";
import { AnimatePresence, motion } from "motion/react";
import { CheckCircle, X } from "./icons";
import { cn } from "@/lib/utils";

type ToastKind = "success" | "error" | "info";
type ToastItem = { id: number; kind: ToastKind; title: string; description?: string };

type ToastState = {
  items: ToastItem[];
  push: (t: Omit<ToastItem, "id">) => void;
  dismiss: (id: number) => void;
};

let counter = 0;
export const useToastStore = create<ToastState>((set, get) => ({
  items: [],
  push: (t) => {
    const id = ++counter;
    set({ items: [...get().items.slice(-3), { ...t, id }] });
    setTimeout(() => get().dismiss(id), t.kind === "error" ? 7000 : 4000);
  },
  dismiss: (id) => set({ items: get().items.filter((i) => i.id !== id) }),
}));

export const toast = {
  success: (title: string, description?: string) =>
    useToastStore
      .getState()
      .push({ kind: "success", title, ...(description ? { description } : {}) }),
  error: (title: string, description?: string) =>
    useToastStore
      .getState()
      .push({ kind: "error", title, ...(description ? { description } : {}) }),
  info: (title: string, description?: string) =>
    useToastStore.getState().push({ kind: "info", title, ...(description ? { description } : {}) }),
};

export function Toaster({ closeLabel = "Close" }: { closeLabel?: string }) {
  const items = useToastStore((s) => s.items);
  const dismiss = useToastStore((s) => s.dismiss);
  return (
    <div
      aria-live="polite"
      aria-atomic="false"
      role="status"
      className="pointer-events-none fixed right-4 bottom-4 z-[var(--z-toast)] flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-2"
    >
      <AnimatePresence initial={false}>
        {items.map((t) => (
          <motion.div
            key={t.id}
            layout
            initial={{ opacity: 0, y: 16, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.96, transition: { duration: 0.15 } }}
            transition={{ type: "spring", stiffness: 420, damping: 32 }}
            className={cn(
              "glass pointer-events-auto flex items-start gap-3 rounded-lg bg-surface-2/90 p-4",
              t.kind === "error" && "border-danger/40",
            )}
          >
            <span
              className={cn(
                "mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full",
                t.kind === "success" && "text-success",
                t.kind === "error" && "text-danger",
                t.kind === "info" && "text-white",
              )}
            >
              {t.kind === "error" ? <X className="size-4" /> : <CheckCircle className="size-5" />}
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-white">{t.title}</p>
              {t.description ? (
                <p className="mt-0.5 text-xs text-fg-muted">{t.description}</p>
              ) : null}
            </div>
            <button
              type="button"
              onClick={() => dismiss(t.id)}
              aria-label={closeLabel}
              className="rounded-full p-1 text-fg-subtle transition-colors hover:text-white"
            >
              <X className="size-3.5" />
            </button>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}
