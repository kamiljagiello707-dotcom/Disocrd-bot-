"use client";
import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { AnimatePresence, motion } from "motion/react";
import { Button } from "@/components/ui/button";

/** Ostrzeżenie przed opuszczeniem strony z niezapisanymi zmianami (zamknięcie karty i kliknięcia w linki). */
export function useLeaveGuard(dirty: boolean, message: string) {
  useEffect(() => {
    if (!dirty) return;
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
    };
    const onClick = (e: MouseEvent) => {
      const a = e.target instanceof Element ? e.target.closest("a[href]") : null;
      if (!a || a.getAttribute("target") === "_blank" || e.defaultPrevented) return;
      const href = a.getAttribute("href") ?? "";
      if (href.startsWith("#")) return;
      if (!window.confirm(message)) {
        e.preventDefault();
        e.stopPropagation();
      }
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    document.addEventListener("click", onClick, true);
    return () => {
      window.removeEventListener("beforeunload", onBeforeUnload);
      document.removeEventListener("click", onClick, true);
    };
  }, [dirty, message]);
}

export function UnsavedBar({
  dirty,
  saving,
  onSave,
  onReset,
}: {
  dirty: boolean;
  saving: boolean;
  onSave: () => void;
  onReset: () => void;
}) {
  const t = useTranslations("dashboard");
  useLeaveGuard(dirty, t("leaveConfirm"));
  return (
    <AnimatePresence>
      {dirty ? (
        <motion.div
          role="region"
          aria-label={t("unsaved")}
          initial={{ y: 80, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: 80, opacity: 0 }}
          transition={{ type: "spring", stiffness: 380, damping: 32 }}
          className="fixed inset-x-3 bottom-4 z-[60] mx-auto flex max-w-2xl items-center justify-between gap-4 rounded-full border border-border-strong bg-surface-2/95 py-2 pr-2 pl-5 shadow-2xl backdrop-blur-xl lg:left-[calc(16rem+0.75rem)]"
          data-testid="unsaved-bar"
        >
          <p className="text-sm text-white">
            <span
              className="mr-2 inline-block size-1.5 animate-pulse rounded-full bg-warning align-middle"
              aria-hidden
            />
            {t("unsaved")}
          </p>
          <div className="flex gap-2">
            <Button type="button" variant="ghost" size="sm" onClick={onReset} disabled={saving}>
              {t("revert")}
            </Button>
            <Button type="button" size="sm" onClick={onSave} disabled={saving} magnetic={false}>
              {saving ? t("saving") : t("saveChanges")}
            </Button>
          </div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
