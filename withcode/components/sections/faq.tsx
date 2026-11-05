"use client";
import { Accordion } from "radix-ui";
import { Plus } from "@/components/ui/icons";

export function FaqList({ items }: { items: { id: string; q: string; a: string }[] }) {
  return (
    <Accordion.Root
      type="single"
      collapsible
      className="divide-y divide-border border-y border-border"
    >
      {items.map((it) => (
        <Accordion.Item key={it.id} value={it.id}>
          <Accordion.Header>
            <Accordion.Trigger className="group flex w-full items-center justify-between gap-6 py-6 text-left text-lg text-white transition-colors hover:text-fg-muted max-md:text-base">
              {it.q}
              <Plus className="size-5 shrink-0 text-fg-subtle transition-transform duration-500 ease-out-expo group-data-[state=open]:rotate-45" />
            </Accordion.Trigger>
          </Accordion.Header>
          <Accordion.Content className="overflow-hidden data-[state=closed]:animate-[acc-up_300ms_var(--ease-out-expo)] data-[state=open]:animate-[acc-down_400ms_var(--ease-out-expo)]">
            <p className="max-w-3xl pb-6 leading-relaxed text-fg-muted">{it.a}</p>
          </Accordion.Content>
        </Accordion.Item>
      ))}
    </Accordion.Root>
  );
}
