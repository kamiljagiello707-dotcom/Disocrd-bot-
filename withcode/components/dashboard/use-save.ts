"use client";
import { useTransition } from "react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import type { FieldValues, UseFormReturn } from "react-hook-form";
import { toast } from "@/components/ui/toast";
import type { ActionResult } from "@/lib/actions/result";

/**
 * Zapis z optimistic update: formularz od razu przyjmuje nowe wartości jako „zapisane”;
 * przy błędzie wracamy do poprzedniego stanu i pokazujemy toast.
 */
export function useSave<T extends FieldValues, R>(
  form: UseFormReturn<T>,
  action: (values: T) => Promise<ActionResult<R>>,
  onSuccess?: (data: R, values: T) => T | void,
) {
  const t = useTranslations("dashboard");
  const router = useRouter();
  const [saving, start] = useTransition();

  const save = form.handleSubmit((values) => {
    const previous = form.formState.defaultValues as T;
    // optimistic: od razu traktujemy nowe wartości jako zapisane (pasek znika)
    form.reset(values);
    start(async () => {
      const res = await action(values);
      if (res.ok) {
        const next = onSuccess?.(res.data, values);
        if (next) form.reset(next);
        toast.success(t("saved"));
        router.refresh();
      } else {
        // przywracamy poprzedni „zapisany” stan, zostawiając edycje użytkownika (pasek wraca)
        form.reset(previous, { keepValues: true });
        const msg =
          res.error === "conflict"
            ? t("conflict")
            : res.error === "forbidden"
              ? t("forbidden")
              : res.issues?.[0]?.message;
        toast.error(t("saveError"), msg);
      }
    });
  });

  return { save: () => void save(), saving, reset: () => form.reset() };
}
