"use client";
import { useRef, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { z } from "zod";
import { greetingSchema, type GreetingInput } from "@/lib/validators/guild";
import { saveGreetings } from "@/lib/actions/dashboard";
import { greetingVariables } from "@/lib/security/sanitize";
import { Switch } from "@/components/ui/switch";
import { Input, Textarea } from "@/components/ui/input";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Panel } from "./page-header";
import { UnsavedBar } from "./unsaved-bar";
import { useSave } from "./use-save";
import { Field, ToggleRow } from "./field";
import { ChannelPicker } from "./pickers";
import { DiscordPreview } from "./discord-preview";
import { useGuildResources } from "./resources";

const schema = z.object({ welcome: greetingSchema, goodbye: greetingSchema });
type Values = z.infer<typeof schema>;

export function WelcomeForm({ guildId, initial }: { guildId: string; initial: Values }) {
  const t = useTranslations("dashboard.welcome");
  const { guildName, memberCount } = useGuildResources();
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: initial });
  const [tab, setTab] = useState<"welcome" | "goodbye">("welcome");
  const v = useWatch({ control: form.control, name: tab }) as GreetingInput;
  const textRef = useRef<HTMLTextAreaElement | null>(null);
  const { save, saving, reset } = useSave(
    form,
    (x) => saveGreetings(guildId, x),
    (d, values) => ({
      welcome: { ...values.welcome, version: d.versions.WELCOME },
      goodbye: { ...values.goodbye, version: d.versions.GOODBYE },
    }),
  );
  const err = form.formState.errors[tab];
  const set = <K extends keyof GreetingInput>(k: K, val: GreetingInput[K]) =>
    form.setValue(`${tab}.${k}` as `welcome.${K}`, val as never, {
      shouldDirty: true,
      shouldValidate: form.formState.isSubmitted,
    });

  const insertVar = (variable: string) => {
    const el = textRef.current;
    const current = v.content ?? "";
    const pos = el?.selectionStart ?? current.length;
    const next = current.slice(0, pos) + variable + current.slice(el?.selectionEnd ?? pos);
    set("content", next);
    requestAnimationFrame(() => {
      el?.focus();
      el?.setSelectionRange(pos + variable.length, pos + variable.length);
    });
  };

  const contentReg = form.register(`${tab}.content`);

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save();
      }}
    >
      <Tabs value={tab} onValueChange={(x) => setTab(x as typeof tab)}>
        <TabsList>
          <TabsTrigger value="welcome">{t("welcome")}</TabsTrigger>
          <TabsTrigger value="goodbye">{t("goodbye")}</TabsTrigger>
        </TabsList>
      </Tabs>
      <div className="mt-6 grid gap-4 xl:grid-cols-2">
        <Panel>
          <ToggleRow id="g-enabled" label={t("enabled")}>
            <Switch
              aria-labelledby="g-enabled"
              checked={v.enabled}
              onCheckedChange={(x) => set("enabled", x)}
            />
          </ToggleRow>
          <div className="mt-6 space-y-5 border-t border-border pt-6">
            <Field label={t("channel")} htmlFor="g-ch" error={err?.channelId?.message}>
              <ChannelPicker id="g-ch" value={v.channelId} onChange={(x) => set("channelId", x)} />
            </Field>
            <Field label={t("content")} htmlFor="g-content" error={err?.content?.message}>
              <Textarea
                id="g-content"
                rows={5}
                maxLength={2000}
                {...contentReg}
                ref={(el) => {
                  contentReg.ref(el);
                  textRef.current = el;
                }}
              />
            </Field>
            <div>
              <p className="mb-2 text-xs text-fg-subtle">{t("variables")}</p>
              <div className="flex flex-wrap gap-1.5">
                {greetingVariables.map((x) => (
                  <button
                    key={x}
                    type="button"
                    onClick={() => insertVar(x)}
                    className="rounded-full border border-border px-2.5 py-1 font-mono text-xs text-fg-muted hover:border-white/40 hover:text-white"
                  >
                    {x}
                  </button>
                ))}
              </div>
            </div>
            <ToggleRow id="g-embed" label={t("useEmbed")}>
              <Switch
                aria-labelledby="g-embed"
                checked={v.embed !== null}
                onCheckedChange={(x) =>
                  set(
                    "embed",
                    x ? { title: "Witaj na {server}!", description: null, imageUrl: null } : null,
                  )
                }
              />
            </ToggleRow>
            {v.embed ? (
              <div className="space-y-4 rounded-lg border border-border p-4">
                <Field label={t("embedTitle")} htmlFor="g-et">
                  <Input
                    id="g-et"
                    maxLength={256}
                    value={v.embed.title ?? ""}
                    onChange={(e) => set("embed", { ...v.embed!, title: e.target.value || null })}
                  />
                </Field>
                <Field label={t("embedDescription")} htmlFor="g-ed">
                  <Textarea
                    id="g-ed"
                    rows={3}
                    maxLength={4000}
                    value={v.embed.description ?? ""}
                    onChange={(e) =>
                      set("embed", { ...v.embed!, description: e.target.value || null })
                    }
                  />
                </Field>
                <Field label={t("embedImage")} htmlFor="g-ei" error={err?.embed?.imageUrl?.message}>
                  <Input
                    id="g-ei"
                    type="url"
                    placeholder="https://"
                    value={v.embed.imageUrl ?? ""}
                    onChange={(e) =>
                      set("embed", { ...v.embed!, imageUrl: e.target.value || null })
                    }
                  />
                </Field>
              </div>
            ) : null}
            <ToggleRow id="g-dm" label={t("dm")}>
              <Switch aria-labelledby="g-dm" checked={v.dm} onCheckedChange={(x) => set("dm", x)} />
            </ToggleRow>
          </div>
        </Panel>
        <div className="xl:sticky xl:top-24 xl:h-fit">
          <p className="eyebrow mb-3">{t("preview")}</p>
          <DiscordPreview
            content={v.content}
            embed={v.embed}
            server={guildName}
            memberCount={memberCount}
          />
        </div>
      </div>
      <UnsavedBar dirty={form.formState.isDirty} saving={saving} onSave={save} onReset={reset} />
    </form>
  );
}
