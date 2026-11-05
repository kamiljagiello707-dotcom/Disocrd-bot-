"use client";
import { Tooltip as T } from "radix-ui";

export const TooltipProvider = T.Provider;

export function Tooltip({
  content,
  children,
  side = "top",
}: {
  content: React.ReactNode;
  children: React.ReactNode;
  side?: "top" | "bottom" | "left" | "right";
}) {
  return (
    <T.Root delayDuration={150}>
      <T.Trigger asChild>{children}</T.Trigger>
      <T.Portal>
        <T.Content
          side={side}
          sideOffset={8}
          className="z-[95] max-w-xs rounded-md border border-border-strong bg-surface-2 px-2.5 py-1.5 text-xs text-white shadow-xl data-[state=delayed-open]:animate-[fade-in_150ms_ease]"
        >
          {content}
          <T.Arrow className="fill-surface-2" />
        </T.Content>
      </T.Portal>
    </T.Root>
  );
}
