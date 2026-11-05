import Link from "next/link";
import { ChevronLeft, ChevronRight } from "./icons";
import { cn } from "@/lib/utils";

export function Pagination({
  page,
  totalPages,
  hrefFor,
  labels,
}: {
  page: number;
  totalPages: number;
  hrefFor: (p: number) => string;
  labels: { previous: string; next: string; page: string };
}) {
  if (totalPages <= 1) return null;
  const btn =
    "inline-flex h-9 items-center gap-1 rounded-full border border-border px-3 text-sm transition-colors";
  return (
    <nav aria-label="Pagination" className="flex items-center justify-between gap-4 pt-4">
      {page > 1 ? (
        <Link
          className={cn(btn, "text-white hover:border-white/40")}
          href={hrefFor(page - 1)}
          rel="prev"
        >
          <ChevronLeft className="size-4" /> {labels.previous}
        </Link>
      ) : (
        <span className={cn(btn, "text-fg-faint")} aria-disabled>
          <ChevronLeft className="size-4" /> {labels.previous}
        </span>
      )}
      <span className="text-sm text-fg-subtle">{labels.page}</span>
      {page < totalPages ? (
        <Link
          className={cn(btn, "text-white hover:border-white/40")}
          href={hrefFor(page + 1)}
          rel="next"
        >
          {labels.next} <ChevronRight className="size-4" />
        </Link>
      ) : (
        <span className={cn(btn, "text-fg-faint")} aria-disabled>
          {labels.next} <ChevronRight className="size-4" />
        </span>
      )}
    </nav>
  );
}
