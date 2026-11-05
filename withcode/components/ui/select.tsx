"use client";
import { Select as S } from "radix-ui";
import { Check, ChevronDown } from "./icons";
import { cn } from "@/lib/utils";

export type SelectOption = { value: string; label: string; hint?: string };

type Props = {
  value: string;
  onValueChange: (v: string) => void;
  options: SelectOption[];
  placeholder?: string;
  id?: string;
  className?: string;
  disabled?: boolean;
  "aria-label"?: string;
  "aria-invalid"?: boolean;
};

export function Select({
  value,
  onValueChange,
  options,
  placeholder,
  id,
  className,
  disabled,
  ...aria
}: Props) {
  return (
    <S.Root value={value} onValueChange={onValueChange} disabled={disabled}>
      <S.Trigger
        id={id}
        aria-label={aria["aria-label"]}
        aria-invalid={aria["aria-invalid"]}
        className={cn(
          "flex h-11 w-full items-center justify-between gap-2 rounded-md border border-border-strong bg-surface-1 px-3.5 text-left text-sm text-white transition-colors hover:border-white/30 focus-visible:border-white focus-visible:outline-none aria-[invalid=true]:border-danger/70 data-[placeholder]:text-fg-faint",
          className,
        )}
      >
        <S.Value placeholder={placeholder} />
        <S.Icon>
          <ChevronDown className="size-4 text-fg-subtle" />
        </S.Icon>
      </S.Trigger>
      <S.Portal>
        <S.Content
          position="popper"
          sideOffset={6}
          className="z-[95] max-h-[min(22rem,var(--radix-select-content-available-height))] w-[var(--radix-select-trigger-width)] overflow-hidden rounded-lg border border-border-strong bg-surface-2/95 shadow-2xl backdrop-blur-xl"
        >
          <S.Viewport className="p-1.5">
            {options.map((o) => (
              <S.Item
                key={o.value}
                value={o.value}
                className="relative flex cursor-default items-center rounded-md py-2 pr-3 pl-8 text-sm text-fg-muted outline-none select-none data-[highlighted]:bg-white/[0.08] data-[highlighted]:text-white data-[state=checked]:text-white"
              >
                <S.ItemIndicator className="absolute left-2.5">
                  <Check className="size-3.5" />
                </S.ItemIndicator>
                <S.ItemText>{o.label}</S.ItemText>
                {o.hint ? (
                  <span className="ml-auto pl-3 text-xs text-fg-faint">{o.hint}</span>
                ) : null}
              </S.Item>
            ))}
          </S.Viewport>
        </S.Content>
      </S.Portal>
    </S.Root>
  );
}
