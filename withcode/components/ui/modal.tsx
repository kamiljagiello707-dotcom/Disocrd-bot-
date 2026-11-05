"use client";
import { Dialog } from "radix-ui";
import { X } from "./icons";
import { cn } from "@/lib/utils";

export const Modal = Dialog.Root;
export const ModalTrigger = Dialog.Trigger;
export const ModalClose = Dialog.Close;

export function ModalContent({
  className,
  children,
  title,
  description,
  closeLabel = "Close",
  ...props
}: React.ComponentProps<typeof Dialog.Content> & {
  title: string;
  description?: string;
  closeLabel?: string;
}) {
  return (
    <Dialog.Portal>
      <Dialog.Overlay className="fixed inset-0 z-[85] bg-black/70 backdrop-blur-sm data-[state=open]:animate-[fade-in_200ms_ease]" />
      <Dialog.Content
        className={cn(
          "glass fixed top-1/2 left-1/2 z-[86] w-[calc(100vw-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-xl bg-surface-1/90 p-6 shadow-2xl",
          "data-[state=open]:animate-[modal-in_300ms_var(--ease-out-expo)]",
          className,
        )}
        {...props}
      >
        <Dialog.Title className="heading pr-8 text-xl text-white">{title}</Dialog.Title>
        {description ? (
          <Dialog.Description className="mt-2 text-sm text-fg-muted">
            {description}
          </Dialog.Description>
        ) : (
          <Dialog.Description className="sr-only">{title}</Dialog.Description>
        )}
        <div className="mt-5">{children}</div>
        <Dialog.Close
          aria-label={closeLabel}
          className="absolute top-4 right-4 rounded-full p-1.5 text-fg-muted transition-colors hover:bg-white/10 hover:text-white"
        >
          <X className="size-4" />
        </Dialog.Close>
      </Dialog.Content>
    </Dialog.Portal>
  );
}
