"use client";
import { useState, useTransition } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useFormatter, useTranslations } from "next-intl";
import { antiRaidSchema, type AntiRaidInput } from "@/lib/validators/guild";
import { saveAntiRaid, setLockdown } from "@/lib/actions/dashboard";
import { Switch } from "@/components/ui/switch";
import { Slider } from "@/components/ui/slider";
import { Button } from "@/components/ui/button";
import { Modal, ModalContent } from "@/components/ui/modal";
import { Lock, Unlock } from "@/components/ui/icons";
import { toast } from "@/components/ui/toast";
import { Panel } from "./page-header";
import { UnsavedBar } from "./unsaved-bar";
import { useSave } from "./use-save";
import { Field, ToggleRow } from "./field";
import { cn } from "@/lib/utils";

export function AntiRaidForm({
  guildId,
  initial,
  lockdown,
}: {
  guildId: string;
  initial: AntiRaidInput;
  lockdown: { active: boolean; at: string | null };
}) {
  const t = useTranslations("dashboard.antiraid");
  const ta = useTranslations("dashboard.actions");
  const tc = useTranslations("common");
  const format = useFormatter();
  const form = useForm<AntiRaidInput>({
    resolver: zodResolver(antiRaidSchema),
    defaultValues: initial,
  });
  const v = useWatch({ control: form.control });
  const { save, saving, reset } = useSave(
    form,
    (x) => saveAntiRaid(guildId, x),
    (d, values) => ({ ...values, version: d.version }),
  );
  const [ld, setLd] = useState(lockdown);
  const [confirm, setConfirm] = useState(false);
  const [pending, start] = useTransition();
  const set = <K extends keyof AntiRaidInput>(k: K, val: AntiRaidInput[K]) =>
    form.setValue(k, val as never, { shouldDirty: true, shouldValidate: true });

  const toggleLockdown = (active: boolean) =>
    start(async () => {
      const prev = ld;
      setLd({ active, at: active ? new Date().toISOString() : null }); // optimistic
      setConfirm(false);
      const res = await setLockdown(guildId, { active });
      if (res.ok) toast.success(active ? t("lockdownEnabled") : t("lockdownDisabled"));
      else {
        setLd(prev);
        toast.error(res.error);
      }
    });

  return (
    <div className="space-y-4">
      <Panel className={cn(ld.active && "border-danger/40 bg-danger/[0.04]")}>
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="flex items-center gap-2 text-base font-medium text-white">
              {ld.active ? <Lock className="size-4 text-danger" /> : <Unlock className="size-4" />}{" "}
              {t("lockdown")}
            </h2>
            <p className="mt-1 text-sm text-fg-muted">
              {ld.active && ld.at
                ? t("lockdownActive", { time: format.relativeTime(new Date(ld.at)) })
                : t("lockdownDesc")}
            </p>
          </div>
          {ld.active ? (
            <Button variant="secondary" onClick={() => toggleLockdown(false)} disabled={pending}>
              <span className="relative">{t("lockdownOff")}</span>
            </Button>
          ) : (
            <Button
              variant="danger"
              onClick={() => setConfirm(true)}
              disabled={pending}
              data-testid="lockdown-on"
            >
              <Lock className="size-4" /> {t("lockdownOn")}
            </Button>
          )}
        </div>
      </Panel>
      <Modal open={confirm} onOpenChange={setConfirm}>
        <ModalContent
          title={t("lockdownOn")}
          description={t("lockdownConfirm")}
          closeLabel={tc("close")}
        >
          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={() => setConfirm(false)}>
              {tc("cancel")}
            </Button>
            <Button variant="danger" onClick={() => toggleLockdown(true)}>
              {tc("confirm")}
            </Button>
          </div>
        </ModalContent>
      </Modal>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          save();
        }}
      >
        <Panel>
          <ToggleRow
            id="ar-enabled"
            label={t("enabled")}
            desc={t("summary", { threshold: v.joinThreshold ?? 10, window: v.joinWindowSec ?? 10 })}
          >
            <Switch
              aria-labelledby="ar-enabled"
              checked={v.enabled ?? false}
              onCheckedChange={(x) => set("enabled", x)}
            />
          </ToggleRow>
          <div className="mt-6 grid gap-6 border-t border-border pt-6 md:grid-cols-2">
            {(
              [
                ["joinThreshold", t("threshold"), 3, 100, 1],
                ["joinWindowSec", t("window"), 5, 300, 5],
                ["minAccountAgeDays", t("accountAge"), 0, 365, 1],
              ] as const
            ).map(([key, label, min, max, step]) => (
              <div key={key}>
                <div className="mb-3 flex justify-between">
                  <span id={`ar-${key}`} className="text-sm text-white">
                    {label}
                  </span>
                  <span className="font-mono text-xs text-fg-muted">{v[key]}</span>
                </div>
                <Slider
                  aria-labelledby={`ar-${key}`}
                  aria-label={label}
                  min={min}
                  max={max}
                  step={step}
                  value={[v[key] ?? min]}
                  onValueChange={([x]) => set(key, x ?? min)}
                />
              </div>
            ))}
            <Field label={t("action")} htmlFor="ar-action">
              <select
                id="ar-action"
                value={v.action}
                onChange={(e) => set("action", e.target.value as AntiRaidInput["action"])}
                className="h-11 w-full rounded-md border border-border-strong bg-surface-1 px-3 text-sm text-white"
              >
                {(["KICK", "BAN", "TIMEOUT"] as const).map((a) => (
                  <option key={a} value={a}>
                    {ta(a)}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </Panel>
        <UnsavedBar dirty={form.formState.isDirty} saving={saving} onSave={save} onReset={reset} />
      </form>
    </div>
  );
}
