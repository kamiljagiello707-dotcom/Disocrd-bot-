"use client";
import { Slider as S } from "radix-ui";
import { cn } from "@/lib/utils";

export function Slider({ className, ...props }: React.ComponentProps<typeof S.Root>) {
  return (
    <S.Root
      className={cn("relative flex h-5 w-full touch-none items-center select-none", className)}
      {...props}
    >
      <S.Track className="relative h-1 grow overflow-hidden rounded-full bg-white/10">
        <S.Range className="absolute h-full bg-white" />
      </S.Track>
      <S.Thumb
        aria-label={props["aria-label"]}
        className="block size-4 rounded-full border-2 border-black bg-white shadow-[0_0_0_4px_rgb(255_255_255/0.08)] transition-transform hover:scale-110 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
      />
    </S.Root>
  );
}
