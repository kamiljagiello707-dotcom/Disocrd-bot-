"use client";
import { useEffect, useState } from "react";
import type { TocItem } from "@/lib/docs";
import { cn } from "@/lib/utils";

export function DocsToc({ items, label }: { items: TocItem[]; label: string }) {
  const [active, setActive] = useState(items[0]?.id ?? "");
  useEffect(() => {
    const els = items
      .map((i) => document.getElementById(i.id))
      .filter((e): e is HTMLElement => e !== null);
    const io = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (visible[0]) setActive(visible[0].target.id);
      },
      { rootMargin: "-80px 0px -65% 0px" },
    );
    els.forEach((e) => io.observe(e));
    return () => io.disconnect();
  }, [items]);
  if (items.length === 0) return null;
  return (
    <nav aria-label={label}>
      <p className="eyebrow mb-4">{label}</p>
      <ul className="space-y-2 border-l border-border text-sm">
        {items.map((i) => (
          <li key={i.id}>
            <a
              href={`#${i.id}`}
              className={cn(
                "-ml-px block border-l py-0.5 transition-colors",
                i.depth === 3 ? "pl-7" : "pl-4",
                active === i.id
                  ? "border-white text-white"
                  : "border-transparent text-fg-subtle hover:text-white",
              )}
            >
              {i.text}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}
