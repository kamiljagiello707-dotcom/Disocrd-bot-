"use client";
import { useFieldArray, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { punishmentsSchema, type PunishmentsInput } from "@/lib/validators/guild";
import { savePunishments } from "@/lib/actions/dashboard";
import { Button } from "@/components/ui/button";
import { Plus, Trash } from "@/components/ui/icons";
import { Panel } from "./page-header";
import { UnsavedBar } from "./unsaved-bar";
import { useSave } from "./use-save";
import { DurationSelect } from "./action-picker";
import { numberInputClass } from "./field";

export function PunishmentsForm({
  guildId,
  initial,
}: {
  guildId: string;
  initial: PunishmentsInput;
}) {
  const t = useTranslations("dashboard.punishments");
  const ta = useTranslations("dashboard.actions");
  const form = useForm<PunishmentsInput>({
    resolver: zodResolver(punishmentsSchema),
    defaultValues: initial,
  });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: "thresholds" });
  const values = useWatch({ control: form.control, name: "thresholds" });
  const { save, saving, reset } = useSave(form, (v) => savePunishments(guildId, v));
  const rootError =
    form.formState.errors.thresholds?.root?.message ?? form.formState.errors.thresholds?.message;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save();
      }}
    >
      <Panel>
        {fields.length === 0 ? <p className="text-sm text-fg-subtle">{t("empty")}</p> : null}
        <ul className="space-y-3">
          {fields.map((f, i) => (
            <li
              key={f.id}
              className="flex flex-wrap items-end gap-3 rounded-lg border border-border bg-black/40 p-3"
            >
              <div className="w-28">
                <label htmlFor={`p-${i}-c`} className="mb-1.5 block text-xs text-fg-subtle">
                  {t("warnings")}
                </label>
                <input
                  id={`p-${i}-c`}
                  type="number"
                  min={1}
                  max={50}
                  className={numberInputClass}
                  {...form.register(`thresholds.${i}.warnCount`, { valueAsNumber: true })}
                />
              </div>
              <span className="pb-3 text-fg-subtle" aria-hidden>
                →
              </span>
              <div className="w-40">
                <label htmlFor={`p-${i}-a`} className="mb-1.5 block text-xs text-fg-subtle">
                  {t("action")}
                </label>
                <select
                  id={`p-${i}-a`}
                  className="h-11 w-full rounded-md border border-border-strong bg-surface-1 px-3 text-sm text-white"
                  {...form.register(`thresholds.${i}.action`)}
                >
                  {(["TIMEOUT", "KICK", "BAN"] as const).map((a) => (
                    <option key={a} value={a}>
                      {ta(a)}
                    </option>
                  ))}
                </select>
              </div>
              {values[i]?.action === "TIMEOUT" ? (
                <div>
                  <label htmlFor={`p-${i}-d`} className="mb-1.5 block text-xs text-fg-subtle">
                    {t("duration")}
                  </label>
                  <DurationSelect
                    id={`p-${i}-d`}
                    value={values[i]?.duration ?? 3600}
                    onChange={(d) =>
                      form.setValue(`thresholds.${i}.duration`, d, { shouldDirty: true })
                    }
                  />
                </div>
              ) : null}
              <Button
                type="button"
                variant="ghost"
                size="icon"
                magnetic={false}
                aria-label={t("remove")}
                onClick={() => remove(i)}
                className="ml-auto"
              >
                <Trash className="size-4" />
              </Button>
            </li>
          ))}
        </ul>
        {rootError ? (
          <p role="alert" className="mt-3 text-xs text-danger">
            {rootError}
          </p>
        ) : null}
        <Button
          type="button"
          variant="outline"
          size="sm"
          magnetic={false}
          className="mt-4"
          disabled={fields.length >= 20}
          onClick={() => {
            const next = Math.max(0, ...values.map((x) => x.warnCount || 0)) + 2;
            append({ warnCount: Math.min(50, next || 3), action: "TIMEOUT", duration: 3600 });
          }}
        >
          <Plus className="size-4" /> {t("add")}
        </Button>
      </Panel>
      <UnsavedBar dirty={form.formState.isDirty} saving={saving} onSave={save} onReset={reset} />
    </form>
  );
}
