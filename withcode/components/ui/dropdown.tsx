"use client";
import { DropdownMenu as D } from "radix-ui";
import { cn } from "@/lib/utils";

export const Dropdown = D.Root;
export const DropdownTrigger = D.Trigger;

export function DropdownContent({ className, ...props }: React.ComponentProps<typeof D.Content>) {
  return (
    <D.Portal>
      <D.Content
        sideOffset={8}
        align="end"
        className={cn(
          "z-[95] min-w-48 rounded-lg border border-border-strong bg-surface-2/95 p-1.5 shadow-2xl backdrop-blur-xl data-[state=open]:animate-[fade-in_150ms_ease]",
          className,
        )}
        {...props}
      />
    </D.Portal>
  );
}

export function DropdownItem({ className, ...props }: React.ComponentProps<typeof D.Item>) {
  return (
    <D.Item
      className={cn(
        "flex cursor-default items-center gap-2 rounded-md px-3 py-2 text-sm text-fg-muted outline-none select-none data-[highlighted]:bg-white/[0.08] data-[highlighted]:text-white",
        className,
      )}
      {...props}
    />
  );
}

export const DropdownSeparator = () => <D.Separator className="my-1 h-px bg-border" />;
