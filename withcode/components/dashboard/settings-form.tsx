"use client";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { settingsSchema, timezones, type SettingsInput } from "@/lib/validators/guild";
import { saveSettings } from "@/lib/actions/dashboard";
import { Input } from "@/components/ui/input";
import { Panel } from "./page-header";
import { UnsavedBar } from "./unsaved-bar";
import { useSave } from "./use-save";
import { Field } from "./field";

export function SettingsForm({ guildId, initial }: { guildId: string; initial: SettingsInput }) {
  const t = useTranslations("dashboard.settings");
  const form = useForm<SettingsInput>({
    resolver: zodResolver(settingsSchema),
    defaultValues: initial,
    mode: "onChange",
  });
  const { save, saving, reset } = useSave(
    form,
    (x) => saveSettings(guildId, x),
    (d, values) => ({ ...values, version: d.version }),
  );
  const err = form.formState.errors;
  const select =
    "h-11 w-full rounded-md border border-border-strong bg-surface-1 px-3 text-sm text-white focus-visible:border-white focus-visible:outline-none";
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save();
      }}
    >
      <Panel>
        <div className="grid gap-6 md:grid-cols-3">
          <Field
            label={t("prefix")}
            htmlFor="s-prefix"
            hint={t("prefixHint")}
            error={err.prefix?.message}
          >
            <Input
              id="s-prefix"
              maxLength={5}
              aria-invalid={Boolean(err.prefix)}
              className="font-mono"
              {...form.register("prefix")}
            />
          </Field>
          <Field label={t("locale")} htmlFor="s-locale">
            <select id="s-locale" className={select} {...form.register("locale")}>
              <option value="PL">Polski</option>
              <option value="EN">English</option>
            </select>
          </Field>
          <Field label={t("timezone")} htmlFor="s-tz">
            <select id="s-tz" className={select} {...form.register("timezone")}>
              {timezones.map((z) => (
                <option key={z} value={z}>
                  {z}
                </option>
              ))}
            </select>
          </Field>
        </div>
      </Panel>
      <UnsavedBar dirty={form.formState.isDirty} saving={saving} onSave={save} onReset={reset} />
    </form>
  );
}
