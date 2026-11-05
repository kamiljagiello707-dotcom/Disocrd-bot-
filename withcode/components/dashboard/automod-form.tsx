"use client";
import { useRef, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { AnimatePresence, motion } from "motion/react";
import {
  autoModConfigSchema,
  parseBannedWordsImport,
  type AutoModConfigInput,
} from "@/lib/validators/automod";
import { saveAutoMod } from "@/lib/actions/dashboard";
import { Switch } from "@/components/ui/switch";
import { Slider } from "@/components/ui/slider";
import { Button } from "@/components/ui/button";
import { Upload, X } from "@/components/ui/icons";
import { toast } from "@/components/ui/toast";
import { Panel } from "./page-header";
import { UnsavedBar } from "./unsaved-bar";
import { useSave } from "./use-save";
import { ActionPicker, DurationSelect } from "./action-picker";
import { ChannelMultiPicker, RoleMultiPicker } from "./pickers";
import { cn } from "@/lib/utils";

export function AutoModForm({
  guildId,
  initial,
}: {
  guildId: string;
  initial: AutoModConfigInput;
}) {
  const t = useTranslations("dashboard.automod");
  const form = useForm<AutoModConfigInput>({
    resolver: zodResolver(autoModConfigSchema),
    defaultValues: initial,
    mode: "onChange",
  });
  const rules = useWatch({ control: form.control, name: "rules" });
  const words = useWatch({ control: form.control, name: "bannedWords" });
  const { save, saving, reset } = useSave(
    form,
    (v) => saveAutoMod(guildId, v),
    (data, values) => ({ ...values, versions: data.versions }),
  );
  const [word, setWord] = useState("");
  const fileRef = useRef<HTMLInputElement>(null);

  const set = <P extends Parameters<typeof form.setValue>[0]>(
    path: P,
    value: Parameters<typeof form.setValue<P>>[1],
  ) => form.setValue(path, value, { shouldDirty: true, shouldValidate: true });

  const addWords = (list: string[]) => {
    const merged = [...new Set([...words, ...list])].slice(0, 1000);
    set("bannedWords", merged);
  };

  const onImport = async (file: File) => {
    const text = await file.text();
    const { words: parsed, rejected } = parseBannedWordsImport(text);
    addWords(parsed);
    toast.success(
      `+${parsed.length}`,
      rejected.length ? `✗ ${rejected.slice(0, 5).join(", ")}` : undefined,
    );
  };

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save();
      }}
      className="space-y-4"
      data-testid="automod-form"
    >
      {rules.map((rule, i) => {
        const id = `rule-${rule.module}`;
        return (
          <Panel
            key={rule.module}
            className={cn("transition-colors", rule.enabled && "border-border-strong")}
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 id={`${id}-name`} className="text-base font-medium text-white">
                  {t(`modules.${rule.module}.name`)}
                </h2>
                <p className="mt-1 text-sm text-fg-muted">{t(`modules.${rule.module}.desc`)}</p>
              </div>
              <Switch
                aria-labelledby={`${id}-name`}
                data-testid={`toggle-${rule.module}`}
                checked={rule.enabled}
                onCheckedChange={(v) => set(`rules.${i}.enabled`, v)}
              />
            </div>
            <AnimatePresence initial={false}>
              {rule.enabled ? (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
                  className="overflow-hidden"
                >
                  <div className="mt-6 grid gap-6 border-t border-border pt-6 md:grid-cols-2">
                    <div>
                      <div className="mb-3 flex items-center justify-between">
                        <span id={`${id}-sens`} className="text-sm text-white">
                          {t("sensitivity")}
                        </span>
                        <span className="font-mono text-xs text-fg-muted tabular-nums">
                          {rule.sensitivity}
                        </span>
                      </div>
                      <Slider
                        aria-labelledby={`${id}-sens`}
                        aria-label={t("sensitivity")}
                        data-testid={`slider-${rule.module}`}
                        min={0}
                        max={100}
                        step={5}
                        value={[rule.sensitivity]}
                        onValueChange={([v]) => set(`rules.${i}.sensitivity`, v ?? 50)}
                      />
                    </div>
                    <div>
                      <span id={`${id}-actions`} className="mb-3 block text-sm text-white">
                        {t("actions")}
                      </span>
                      <ActionPicker
                        labelledBy={`${id}-actions`}
                        value={rule.actions}
                        onChange={(v) => set(`rules.${i}.actions`, v)}
                      />
                      {rule.actions.includes("TIMEOUT") ? (
                        <div className="mt-3 flex items-center gap-3">
                          <label htmlFor={`${id}-dur`} className="text-xs text-fg-subtle">
                            {t("muteDuration")}
                          </label>
                          <DurationSelect
                            id={`${id}-dur`}
                            value={rule.muteDuration}
                            onChange={(v) => set(`rules.${i}.muteDuration`, v)}
                          />
                        </div>
                      ) : null}
                    </div>
                    <div>
                      <label htmlFor={`${id}-ch`} className="mb-2 block text-sm text-white">
                        {t("exemptChannels")}
                      </label>
                      <ChannelMultiPicker
                        id={`${id}-ch`}
                        value={rule.exemptChannels}
                        onChange={(v) => set(`rules.${i}.exemptChannels`, v)}
                      />
                    </div>
                    <div>
                      <label htmlFor={`${id}-ro`} className="mb-2 block text-sm text-white">
                        {t("exemptRoles")}
                      </label>
                      <RoleMultiPicker
                        id={`${id}-ro`}
                        value={rule.exemptRoles}
                        onChange={(v) => set(`rules.${i}.exemptRoles`, v)}
                      />
                    </div>
                  </div>
                  {rule.module === "WORD_FILTER" ? (
                    <div className="mt-6 border-t border-border pt-6">
                      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
                        <label htmlFor="banned-input" className="text-sm text-white">
                          {t("bannedWords")}{" "}
                          <span className="text-fg-subtle">
                            · {t("wordsCount", { count: words.length })}
                          </span>
                        </label>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          magnetic={false}
                          onClick={() => fileRef.current?.click()}
                        >
                          <Upload className="size-3.5" /> {t("import")}
                        </Button>
                        <input
                          ref={fileRef}
                          type="file"
                          accept=".txt,.csv,text/plain,text/csv"
                          className="hidden"
                          aria-label={t("importHint")}
                          onChange={(e) => {
                            const f = e.target.files?.[0];
                            if (f) void onImport(f);
                            e.target.value = "";
                          }}
                        />
                      </div>
                      <input
                        id="banned-input"
                        value={word}
                        onChange={(e) => setWord(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter" || e.key === ",") {
                            e.preventDefault();
                            const { words: parsed } = parseBannedWordsImport(word);
                            if (parsed.length) addWords(parsed);
                            setWord("");
                          }
                        }}
                        placeholder={t("addWord")}
                        className="h-10 w-full rounded-md border border-border-strong bg-black px-3 text-sm text-white placeholder:text-fg-faint focus-visible:border-white focus-visible:outline-none"
                      />
                      <p className="mt-1.5 text-xs text-fg-subtle">{t("bannedWordsHint")}</p>
                      <ul className="mt-3 flex max-h-48 flex-wrap gap-1.5 overflow-y-auto">
                        {words.map((w) => (
                          <li
                            key={w}
                            className="inline-flex items-center gap-1 rounded-full border border-border bg-surface-2 py-0.5 pr-1 pl-3 font-mono text-xs text-white"
                          >
                            {w}
                            <button
                              type="button"
                              aria-label={`✗ ${w}`}
                              onClick={() =>
                                set(
                                  "bannedWords",
                                  words.filter((x) => x !== w),
                                )
                              }
                              className="rounded-full p-1 text-fg-subtle hover:text-white"
                            >
                              <X className="size-3" />
                            </button>
                          </li>
                        ))}
                      </ul>
                    </div>
                  ) : null}
                </motion.div>
              ) : null}
            </AnimatePresence>
          </Panel>
        );
      })}
      <UnsavedBar dirty={form.formState.isDirty} saving={saving} onSave={save} onReset={reset} />
    </form>
  );
}
