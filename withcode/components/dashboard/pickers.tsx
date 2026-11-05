"use client";
import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { Popover } from "radix-ui";
import { Check, ChevronDown, X } from "@/components/ui/icons";
import { cn } from "@/lib/utils";
import { useGuildResources, type Option } from "./resources";

function MultiPicker({
  options,
  value,
  onChange,
  placeholder,
  search,
  prefix,
  id,
}: {
  options: Option[];
  value: string[];
  onChange: (v: string[]) => void;
  placeholder: string;
  search: string;
  prefix: string;
  id?: string;
}) {
  const [q, setQ] = useState("");
  const t = useTranslations("dashboard.pickers");
  const filtered = useMemo(
    () => options.filter((o) => o.name.toLowerCase().includes(q.toLowerCase())),
    [options, q],
  );
  const selected = options.filter((o) => value.includes(o.id));
  const toggle = (oid: string) =>
    onChange(value.includes(oid) ? value.filter((v) => v !== oid) : [...value, oid]);
  return (
    <Popover.Root>
      <Popover.Trigger
        id={id}
        className="flex min-h-11 w-full flex-wrap items-center gap-1.5 rounded-md border border-border-strong bg-surface-1 px-2.5 py-1.5 text-left text-sm transition-colors hover:border-white/30"
      >
        {selected.length === 0 ? <span className="px-1 text-fg-faint">{placeholder}</span> : null}
        {selected.slice(0, 6).map((s) => (
          <span
            key={s.id}
            className="inline-flex items-center gap-1 rounded bg-white/[0.08] px-2 py-0.5 text-xs text-white"
          >
            {prefix}
            {s.name}
          </span>
        ))}
        {selected.length > 6 ? (
          <span className="text-xs text-fg-subtle">+{selected.length - 6}</span>
        ) : null}
        <ChevronDown className="ml-auto size-4 text-fg-subtle" />
      </Popover.Trigger>
      <Popover.Portal>
        <Popover.Content
          sideOffset={6}
          align="start"
          className="z-[95] w-[var(--radix-popover-trigger-width)] min-w-64 rounded-lg border border-border-strong bg-surface-2/95 p-2 shadow-2xl backdrop-blur-xl"
        >
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder={search}
            aria-label={search}
            className="mb-2 h-9 w-full rounded-md bg-surface-1 px-3 text-sm text-white placeholder:text-fg-faint focus-visible:ring-1 focus-visible:ring-white/30 focus-visible:outline-none"
          />
          <ul role="listbox" aria-multiselectable className="max-h-64 overflow-y-auto">
            {filtered.map((o) => (
              <li key={o.id} role="option" aria-selected={value.includes(o.id)}>
                <button
                  type="button"
                  onClick={() => toggle(o.id)}
                  className="flex w-full items-center gap-2 rounded-md px-2.5 py-1.5 text-sm text-fg-muted hover:bg-white/[0.06] hover:text-white"
                >
                  <span
                    className={cn(
                      "flex size-4 items-center justify-center rounded border",
                      value.includes(o.id)
                        ? "border-white bg-white text-black"
                        : "border-border-strong",
                    )}
                  >
                    {value.includes(o.id) ? <Check className="size-3" /> : null}
                  </span>
                  {prefix}
                  {o.name}
                </button>
              </li>
            ))}
          </ul>
          {value.length > 0 ? (
            <button
              type="button"
              onClick={() => onChange([])}
              className="mt-2 flex w-full items-center justify-center gap-1 rounded-md py-1.5 text-xs text-fg-subtle hover:text-white"
            >
              <X className="size-3" /> {t("selected", { count: value.length })}
            </button>
          ) : null}
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  );
}

function SinglePicker({
  options,
  value,
  onChange,
  placeholder,
  prefix,
  id,
  allowNone = true,
  invalid,
}: {
  options: Option[];
  value: string | null;
  onChange: (v: string | null) => void;
  placeholder: string;
  prefix: string;
  id?: string;
  allowNone?: boolean;
  invalid?: boolean;
}) {
  const t = useTranslations("dashboard.pickers");
  return (
    <select
      id={id}
      value={value ?? ""}
      aria-invalid={invalid}
      onChange={(e) => onChange(e.target.value === "" ? null : e.target.value)}
      className="h-11 w-full rounded-md border border-border-strong bg-surface-1 px-3 text-sm text-white transition-colors hover:border-white/30 focus-visible:border-white focus-visible:outline-none aria-[invalid=true]:border-danger/70"
    >
      {allowNone ? (
        <option value="">{t("none")}</option>
      ) : (
        <option value="" disabled>
          {placeholder}
        </option>
      )}
      {options.map((o) => (
        <option key={o.id} value={o.id}>
          {prefix}
          {o.name}
        </option>
      ))}
    </select>
  );
}

export function ChannelMultiPicker(props: {
  value: string[];
  onChange: (v: string[]) => void;
  id?: string;
}) {
  const { channels } = useGuildResources();
  const t = useTranslations("dashboard.pickers");
  return (
    <MultiPicker
      {...props}
      options={channels}
      placeholder={t("channel")}
      search={t("searchChannels")}
      prefix="#"
    />
  );
}
export function RoleMultiPicker(props: {
  value: string[];
  onChange: (v: string[]) => void;
  id?: string;
}) {
  const { roles } = useGuildResources();
  const t = useTranslations("dashboard.pickers");
  return (
    <MultiPicker
      {...props}
      options={roles}
      placeholder={t("role")}
      search={t("searchRoles")}
      prefix="@"
    />
  );
}
export function ChannelPicker(props: {
  value: string | null;
  onChange: (v: string | null) => void;
  id?: string;
  invalid?: boolean;
}) {
  const { channels } = useGuildResources();
  const t = useTranslations("dashboard.pickers");
  return <SinglePicker {...props} options={channels} placeholder={t("channel")} prefix="#" />;
}
export function RolePicker(props: {
  value: string | null;
  onChange: (v: string | null) => void;
  id?: string;
  allowNone?: boolean;
  invalid?: boolean;
}) {
  const { roles } = useGuildResources();
  const t = useTranslations("dashboard.pickers");
  return <SinglePicker {...props} options={roles} placeholder={t("role")} prefix="@" />;
}
