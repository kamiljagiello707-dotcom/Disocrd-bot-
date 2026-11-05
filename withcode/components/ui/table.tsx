import type { HTMLAttributes, TdHTMLAttributes, ThHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export function Table({ className, ...props }: HTMLAttributes<HTMLTableElement>) {
  return (
    <div className="w-full overflow-x-auto rounded-lg border border-border">
      <table className={cn("w-full border-collapse text-left text-sm", className)} {...props} />
    </div>
  );
}
export const THead = (p: HTMLAttributes<HTMLTableSectionElement>) => (
  <thead
    {...p}
    className={cn("bg-surface-1 text-xs tracking-wider text-fg-subtle uppercase", p.className)}
  />
);
export const TBody = (p: HTMLAttributes<HTMLTableSectionElement>) => (
  <tbody {...p} className={cn("divide-y divide-border", p.className)} />
);
export const TR = (p: HTMLAttributes<HTMLTableRowElement>) => (
  <tr {...p} className={cn("transition-colors hover:bg-white/[0.02]", p.className)} />
);
export const TH = (p: ThHTMLAttributes<HTMLTableCellElement>) => (
  <th scope="col" {...p} className={cn("px-4 py-3 font-medium whitespace-nowrap", p.className)} />
);
export const TD = (p: TdHTMLAttributes<HTMLTableCellElement>) => (
  <td {...p} className={cn("px-4 py-3 align-middle text-fg-muted", p.className)} />
);
