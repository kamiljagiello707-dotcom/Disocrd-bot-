"use client";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { verificationSchema, type VerificationInput } from "@/lib/validators/guild";
import { saveVerification } from "@/lib/actions/dashboard";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/input";
import { Panel } from "./page-header";
import { UnsavedBar } from "./unsaved-bar";
import { useSave } from "./use-save";
import { Field, ToggleRow } from "./field";
import { ChannelPicker, RolePicker } from "./pickers";
import { cn } from "@/lib/utils";

export function VerificationForm({
  guildId,
  initial,
}: {
  guildId: string;
  initial: VerificationInput;
}) {
  const t = useTranslations("dashboard.verification");
  const form = useForm<VerificationInput>({
    resolver: zodResolver(verificationSchema),
    defaultValues: initial,
  });
  const v = useWatch({ control: form.control });
  const { save, saving, reset } = useSave(
    form,
    (x) => saveVerification(guildId, x),
    (d, values) => ({ ...values, version: d.version }),
  );
  const err = form.formState.errors;
  const set = <K extends keyof VerificationInput>(k: K, val: VerificationInput[K]) =>
    form.setValue(k, val as never, {
      shouldDirty: true,
      shouldValidate: form.formState.isSubmitted,
    });
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save();
      }}
    >
      <Panel>
        <ToggleRow id="v-enabled" label={t("enabled")}>
          <Switch
            aria-labelledby="v-enabled"
            checked={v.enabled ?? false}
            onCheckedChange={(x) => set("enabled", x)}
          />
        </ToggleRow>
        <div className="mt-6 grid gap-6 border-t border-border pt-6 md:grid-cols-2">
          <div className="md:col-span-2">
            <span id="v-mode" className="mb-2 block text-sm text-white">
              {t("mode")}
            </span>
            <div role="radiogroup" aria-labelledby="v-mode" className="grid gap-3 sm:grid-cols-2">
              {(["BUTTON", "CAPTCHA"] as const).map((m) => (
                <button
                  key={m}
                  type="button"
                  role="radio"
                  aria-checked={v.mode === m}
                  onClick={() => set("mode", m)}
                  className={cn(
                    "rounded-lg border p-4 text-left text-sm transition-colors",
                    v.mode === m
                      ? "border-white bg-white/[0.05] text-white"
                      : "border-border text-fg-muted hover:border-border-strong",
                  )}
                >
                  {t(`modes.${m}`)}
                </button>
              ))}
            </div>
          </div>
          <Field label={t("channel")} htmlFor="v-ch" error={err.channelId?.message}>
            <ChannelPicker
              id="v-ch"
              value={v.channelId ?? null}
              onChange={(x) => set("channelId", x)}
              invalid={Boolean(err.channelId)}
            />
          </Field>
          <Field label={t("role")} htmlFor="v-role" error={err.roleId?.message}>
            <RolePicker
              id="v-role"
              value={v.roleId ?? null}
              onChange={(x) => set("roleId", x)}
              invalid={Boolean(err.roleId)}
            />
          </Field>
          <div className="md:col-span-2">
            <Field label={t("message")} htmlFor="v-msg">
              <Textarea
                id="v-msg"
                maxLength={2000}
                value={v.message ?? ""}
                onChange={(e) => set("message", e.target.value || null)}
              />
            </Field>
          </div>
        </div>
      </Panel>
      <UnsavedBar dirty={form.formState.isDirty} saving={saving} onSave={save} onReset={reset} />
    </form>
  );
}
