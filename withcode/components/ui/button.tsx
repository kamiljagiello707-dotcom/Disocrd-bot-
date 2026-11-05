"use client";
import { useRef, type ButtonHTMLAttributes, type Ref } from "react";
import { Slot } from "radix-ui";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";
import { useMagnetic } from "@/hooks/use-magnetic";

export const buttonVariants = cva(
  [
    "group/btn relative isolate inline-flex select-none items-center justify-center gap-2 overflow-hidden whitespace-nowrap font-medium",
    "transition-[transform,box-shadow,background-color,color,border-color] duration-300 ease-out-expo",
    "translate-x-[var(--mx,0px)] translate-y-[var(--my,0px)]",
    "disabled:pointer-events-none disabled:opacity-40",
    "focus-visible:outline-2 focus-visible:outline-offset-3 focus-visible:outline-white",
  ],
  {
    variants: {
      variant: {
        primary: [
          "bg-white text-black",
          "hover:-translate-y-[calc(2px-var(--my,0px))] hover:shadow-[var(--shadow-lift)]",
          "after:pointer-events-none after:absolute after:inset-y-0 after:-left-full after:w-full after:-skew-x-12",
          "after:bg-gradient-to-r after:from-transparent after:via-black/15 after:to-transparent",
          "hover:after:left-full after:transition-[left] after:duration-700 after:ease-out-expo",
          "focus-visible:shadow-[0_0_0_2px_#000_inset]",
        ],
        secondary: [
          "border border-white/90 bg-transparent text-white hover:text-black",
          "before:absolute before:inset-0 before:-z-10 before:m-auto before:aspect-square before:w-[150%] before:scale-0 before:rounded-full before:bg-white",
          "before:transition-transform before:duration-500 before:ease-out-expo hover:before:scale-100",
        ],
        ghost: "text-fg-muted hover:bg-white/[0.06] hover:text-white",
        outline:
          "border border-border-strong bg-surface-1 text-white hover:border-white/40 hover:bg-surface-2",
        danger: "border border-danger/40 bg-danger/10 text-[#e8b4b4] hover:bg-danger/20",
      },
      size: {
        sm: "h-9 rounded-full px-4 text-sm",
        md: "h-11 rounded-full px-6 text-sm",
        lg: "h-14 rounded-full px-8 text-base",
        icon: "size-10 rounded-full",
        iconSm: "size-8 rounded-full",
      },
    },
    defaultVariants: { variant: "primary", size: "md" },
  },
);

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean;
    magnetic?: boolean;
    ref?: Ref<HTMLButtonElement>;
  };

export function Button({
  className,
  variant,
  size,
  asChild,
  magnetic,
  ref,
  children,
  ...props
}: ButtonProps) {
  const innerRef = useRef<HTMLButtonElement>(null);
  const isMagnetic =
    magnetic ?? (variant === "primary" || variant === "secondary" || variant === undefined);
  useMagnetic(innerRef, 0.25, isMagnetic);
  const Comp = asChild ? Slot.Root : "button";
  return (
    <Comp
      ref={(node: HTMLButtonElement | null) => {
        innerRef.current = node;
        if (typeof ref === "function") ref(node);
        else if (ref) ref.current = node;
      }}
      data-cursor="hover"
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    >
      {children}
    </Comp>
  );
}
