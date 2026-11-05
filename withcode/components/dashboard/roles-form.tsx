"use client";
import {
  useFieldArray,
  useForm,
  useWatch,
  type Control,
  type UseFormRegister,
} from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { rolesSchema, type RolesInput } from "@/lib/validators/guild";
import { saveRoles } from "@/lib/actions/dashboard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Plus, Trash } from "@/components/ui/icons";
import { Panel } from "./page-header";
import { UnsavedBar } from "./unsaved-bar";
import { useSave } from "./use-save";
import { Field, numberInputClass } from "./field";
import { ChannelPicker, RolePicker } from "./pickers";
import { useGuildResources } from "./resources";

function Entries({
  control,
  register,
  gi,
  setValue,
}: {
  control: Control<RolesInput>;
  register: UseFormRegister<RolesInput>;
  gi: number;
  setValue: (path: `groups.${number}.entries.${number}.roleId`, v: string) => void;
}) {
  const t = useTranslations("dashboard.roles");
  const { roles } = useGuildResources();
  const { fields, append, remove } = useFieldArray({ control, name: `groups.${gi}.entries` });
  const values = useWatch({ control, name: `groups.${gi}.entries` });
  return (
    <div className="mt-4 space-y-2">
      {fields.map((f, ei) => (
        <div key={f.id} className="grid grid-cols-[4rem_1fr_1fr_auto] items-center gap-2">
          <Input
            aria-label={t("emoji")}
            placeholder="🎮"
            className="h-10"
            {...register(`groups.${gi}.entries.${ei}.emoji`, {
              setValueAs: (v: string | null) => (v ? v : null),
            })}
          />
          <Input
            aria-label={t("label")}
            placeholder={t("label")}
            className="h-10"
            {...register(`groups.${gi}.entries.${ei}.label`, {
              setValueAs: (v: string | null) => (v ? v : null),
            })}
          />
          <RolePicker
            value={values[ei]?.roleId ?? null}
            allowNone={false}
            onChange={(v) => v && setValue(`groups.${gi}.entries.${ei}.roleId`, v)}
          />
          <Button
            type="button"
            variant="ghost"
            size="iconSm"
            magnetic={false}
            aria-label={t("delete")}
            onClick={() => remove(ei)}
          >
            <Trash className="size-3.5" />
          </Button>
        </div>
      ))}
      <Button
        type="button"
        variant="ghost"
        size="sm"
        magnetic={false}
        disabled={fields.length >= 25 || roles.length === 0}
        onClick={() => append({ emoji: null, label: null, roleId: roles[0]?.id ?? "" })}
      >
        <Plus className="size-3.5" /> {t("addEntry")}
      </Button>
    </div>
  );
}

export function RolesForm({ guildId, initial }: { guildId: string; initial: RolesInput }) {
  const t = useTranslations("dashboard.roles");
  const { channels, roles } = useGuildResources();
  const form = useForm<RolesInput>({ resolver: zodResolver(rolesSchema), defaultValues: initial });
  const groups = useFieldArray({ control: form.control, name: "groups" });
  const autos = useFieldArray({ control: form.control, name: "autoRoles" });
  const gv = useWatch({ control: form.control, name: "groups" });
  const av = useWatch({ control: form.control, name: "autoRoles" });
  const { save, saving, reset } = useSave(form, (x) => saveRoles(guildId, x));
  const sel =
    "h-10 w-full rounded-md border border-border-strong bg-surface-1 px-3 text-sm text-white";
  const sv = (p: Parameters<typeof form.setValue>[0], v: unknown) =>
    form.setValue(p, v as never, { shouldDirty: true });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save();
      }}
      className="space-y-8"
    >
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-medium text-white">{t("reaction")}</h2>
          <Button
            type="button"
            variant="outline"
            size="sm"
            magnetic={false}
            disabled={!channels[0] || !roles[0]}
            onClick={() =>
              groups.append({
                channelId: channels[0]?.id ?? "",
                title: t("groupTitle"),
                mode: "NORMAL",
                style: "BUTTONS",
                entries: [{ emoji: null, label: null, roleId: roles[0]?.id ?? "" }],
              })
            }
          >
            <Plus className="size-4" /> {t("addGroup")}
          </Button>
        </div>
        {groups.fields.length === 0 ? (
          <Panel>
            <p className="text-sm text-fg-subtle">{t("empty")}</p>
          </Panel>
        ) : null}
        <div className="space-y-4">
          {groups.fields.map((g, gi) => (
            <Panel key={g.id}>
              <div className="grid gap-4 md:grid-cols-[2fr_1fr_1fr_1fr_auto] md:items-end">
                <Field label={t("groupTitle")} htmlFor={`rg-${gi}-t`}>
                  <Input
                    id={`rg-${gi}-t`}
                    maxLength={256}
                    {...form.register(`groups.${gi}.title`)}
                  />
                </Field>
                <Field label={t("channel")} htmlFor={`rg-${gi}-c`}>
                  <ChannelPicker
                    id={`rg-${gi}-c`}
                    value={gv[gi]?.channelId ?? null}
                    onChange={(v) => v && sv(`groups.${gi}.channelId`, v)}
                  />
                </Field>
                <Field label={t("mode")} htmlFor={`rg-${gi}-m`}>
                  <select id={`rg-${gi}-m`} className={sel} {...form.register(`groups.${gi}.mode`)}>
                    {(["NORMAL", "UNIQUE", "VERIFY"] as const).map((m) => (
                      <option key={m} value={m}>
                        {t(`modes.${m}`)}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label={t("style")} htmlFor={`rg-${gi}-s`}>
                  <select
                    id={`rg-${gi}-s`}
                    className={sel}
                    {...form.register(`groups.${gi}.style`)}
                  >
                    {(["BUTTONS", "REACTIONS", "SELECT"] as const).map((m) => (
                      <option key={m} value={m}>
                        {t(`styles.${m}`)}
                      </option>
                    ))}
                  </select>
                </Field>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  magnetic={false}
                  aria-label={t("delete")}
                  onClick={() => groups.remove(gi)}
                >
                  <Trash className="size-4" />
                </Button>
              </div>
              <Entries
                control={form.control}
                register={form.register}
                gi={gi}
                setValue={(p, v) => sv(p, v)}
              />
            </Panel>
          ))}
        </div>
      </section>
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-medium text-white">{t("auto")}</h2>
          <Button
            type="button"
            variant="outline"
            size="sm"
            magnetic={false}
            disabled={!roles[0] || autos.fields.length >= 10}
            onClick={() =>
              autos.append({ roleId: roles[0]?.id ?? "", delaySec: 0, botsOnly: false })
            }
          >
            <Plus className="size-4" /> {t("addAuto")}
          </Button>
        </div>
        <Panel>
          {autos.fields.length === 0 ? (
            <p className="text-sm text-fg-subtle">{t("empty")}</p>
          ) : null}
          <ul className="space-y-3">
            {autos.fields.map((a, ai) => (
              <li key={a.id} className="grid items-end gap-3 sm:grid-cols-[2fr_1fr_auto_auto]">
                <Field label={t("autoRole")} htmlFor={`ar-${ai}`}>
                  <RolePicker
                    id={`ar-${ai}`}
                    allowNone={false}
                    value={av[ai]?.roleId ?? null}
                    onChange={(v) => v && sv(`autoRoles.${ai}.roleId`, v)}
                  />
                </Field>
                <Field label={t("delay")} htmlFor={`ad-${ai}`}>
                  <input
                    id={`ad-${ai}`}
                    type="number"
                    min={0}
                    max={86400}
                    className={numberInputClass}
                    {...form.register(`autoRoles.${ai}.delaySec`, { valueAsNumber: true })}
                  />
                </Field>
                <label className="flex h-11 items-center gap-2 text-sm text-fg-muted">
                  <Switch
                    checked={av[ai]?.botsOnly ?? false}
                    onCheckedChange={(v) => sv(`autoRoles.${ai}.botsOnly`, v)}
                    aria-label={t("botsOnly")}
                  />
                  {t("botsOnly")}
                </label>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  magnetic={false}
                  aria-label={t("delete")}
                  onClick={() => autos.remove(ai)}
                >
                  <Trash className="size-4" />
                </Button>
              </li>
            ))}
          </ul>
        </Panel>
      </section>
      <UnsavedBar dirty={form.formState.isDirty} saving={saving} onSave={save} onReset={reset} />
    </form>
  );
}
