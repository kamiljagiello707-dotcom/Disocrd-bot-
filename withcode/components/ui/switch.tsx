"use client";
import { Switch as S } from "radix-ui";
import { cn } from "@/lib/utils";

export function Switch({ className, ...props }: React.ComponentProps<typeof S.Root>) {
  return (
    <S.Root
      className={cn(
        "relative inline-flex h-6 w-11 shrink-0 items-center rounded-full border border-border-strong bg-surface-3 transition-colors duration-300",
        "disabled:opacity-40 data-[state=checked]:border-white data-[state=checked]:bg-white",
        className,
      )}
      {...props}
    >
      <S.Thumb className="block size-4 translate-x-1 rounded-full bg-fg-muted shadow transition-transform duration-300 ease-out-expo data-[state=checked]:translate-x-6 data-[state=checked]:bg-black" />
    </S.Root>
  );
}
