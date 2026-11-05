"use client";
import { useState } from "react";
import { useFieldArray, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { accessSchema, type AccessInput } from "@/lib/validators/guild";
import { saveAccess } from "@/lib/actions/dashboard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Trash, Plus } from "@/components/ui/icons";
import { Panel } from "./page-header";
import { UnsavedBar } from "./unsaved-bar";
import { useSave } from "./use-save";
import { RolePicker } from "./pickers";
import { useGuildResources } from "./resources";

export function AccessForm({
  guildId,
  initial,
  canManage,
}: {
  guildId: string;
  initial: AccessInput;
  canManage: boolean;
}) {
  const t = useTranslations("dashboard.access");
  const { roles } = useGuildResources();
  const form = useForm<AccessInput>({
    resolver: zodResolver(accessSchema),
    defaultValues: initial,
  });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: "entries" });
  const values = useWatch({ control: form.control, name: "entries" });
  const { save, saving, reset } = useSave(form, (x) => saveAccess(guildId, x));
  const [type, setType] = useState<"ROLE" | "USER">("ROLE");
  const [role, setRole] = useState<string | null>(null);
  const [userId, setUserId] = useState("");
  const roleName = (id: string) => roles.find((r) => r.id === id)?.name ?? id;
  const sel = "h-10 rounded-md border border-border-strong bg-surface-1 px-3 text-sm text-white";

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save();
      }}
      className="space-y-4"
    >
      {canManage ? (
        <Panel>
          <div className="flex flex-wrap items-end gap-3">
            <select
              aria-label={t("subject")}
              className={sel}
              value={type}
              onChange={(e) => setType(e.target.value as "ROLE" | "USER")}
            >
              <option value="ROLE">{t("subjectTypes.ROLE")}</option>
              <option value="USER">{t("subjectTypes.USER")}</option>
            </select>
            <div className="min-w-56 flex-1">
              {type === "ROLE" ? (
                <RolePicker value={role} onChange={setRole} />
              ) : (
                <Input
                  aria-label={t("userId")}
                  placeholder={t("userId")}
                  inputMode="numeric"
                  value={userId}
                  onChange={(e) => setUserId(e.target.value.replace(/\D/g, ""))}
                  className="h-10"
                />
              )}
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              magnetic={false}
              onClick={() => {
                const id = type === "ROLE" ? role : userId;
                if (!id || !/^\d{17,20}$/.test(id)) return;
                append({ subjectType: type, subjectId: id, level: "EDITOR" });
                setUserId("");
              }}
            >
              <Plus className="size-4" /> {t("add")}
            </Button>
          </div>
        </Panel>
      ) : null}
      <Panel>
        {fields.length === 0 ? <p className="text-sm text-fg-subtle">{t("empty")}</p> : null}
        <ul className="divide-y divide-border">
          {fields.map((f, i) => (
            <li key={f.id} className="flex flex-wrap items-center gap-3 py-3">
              <span className="rounded bg-white/[0.06] px-2 py-0.5 text-xs text-fg-muted">
                {t(`subjectTypes.${f.subjectType}`)}
              </span>
              <span className="flex-1 font-mono text-sm text-white">
                {f.subjectType === "ROLE" ? `@${roleName(f.subjectId)}` : f.subjectId}
              </span>
              <select
                aria-label={t("level")}
                disabled={!canManage}
                className={sel}
                value={values[i]?.level ?? "EDITOR"}
                onChange={(e) =>
                  form.setValue(`entries.${i}.level`, e.target.value as "VIEWER" | "EDITOR", {
                    shouldDirty: true,
                  })
                }
              >
                <option value="VIEWER">{t("levels.VIEWER")}</option>
                <option value="EDITOR">{t("levels.EDITOR")}</option>
              </select>
              {canManage ? (
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  magnetic={false}
                  aria-label={t("remove")}
                  onClick={() => remove(i)}
                >
                  <Trash className="size-4" />
                </Button>
              ) : null}
            </li>
          ))}
        </ul>
      </Panel>
      <UnsavedBar dirty={form.formState.isDirty} saving={saving} onSave={save} onReset={reset} />
    </form>
  );
}
