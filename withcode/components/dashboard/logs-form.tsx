"use client";
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { logEvents, logsSchema, type LogsInput } from "@/lib/validators/guild";
import { saveLogs } from "@/lib/actions/dashboard";
import { Button } from "@/components/ui/button";
import { Panel } from "./page-header";
import { UnsavedBar } from "./unsaved-bar";
import { useSave } from "./use-save";
import { ChannelPicker } from "./pickers";

export function LogsForm({ guildId, initial }: { guildId: string; initial: LogsInput }) {
  const t = useTranslations("dashboard.logs");
  const form = useForm<LogsInput>({ resolver: zodResolver(logsSchema), defaultValues: initial });
  const channels = useWatch({ control: form.control, name: "channels" });
  const { save, saving, reset } = useSave(form, (v) => saveLogs(guildId, v));
  const [all, setAll] = useState<string | null>(null);
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save();
      }}
    >
      <Panel className="mb-4">
        <div className="flex flex-wrap items-end gap-3">
          <div className="min-w-64 flex-1">
            <label htmlFor="log-all" className="mb-2 block text-sm text-white">
              {t("applyAll")}
            </label>
            <ChannelPicker id="log-all" value={all} onChange={setAll} />
          </div>
          <Button
            type="button"
            variant="outline"
            magnetic={false}
            onClick={() =>
              logEvents.forEach((e) => form.setValue(`channels.${e}`, all, { shouldDirty: true }))
            }
          >
            {t("applyAll")}
          </Button>
        </div>
      </Panel>
      <Panel>
        <ul className="divide-y divide-border">
          {logEvents.map((e) => (
            <li key={e} className="grid items-center gap-2 py-3 sm:grid-cols-[1fr_18rem]">
              <label htmlFor={`log-${e}`} className="text-sm text-white">
                {t(`events.${e}`)}
              </label>
              <ChannelPicker
                id={`log-${e}`}
                value={channels[e] ?? null}
                onChange={(v) => form.setValue(`channels.${e}`, v, { shouldDirty: true })}
              />
            </li>
          ))}
        </ul>
      </Panel>
      <UnsavedBar dirty={form.formState.isDirty} saving={saving} onSave={save} onReset={reset} />
    </form>
  );
}
