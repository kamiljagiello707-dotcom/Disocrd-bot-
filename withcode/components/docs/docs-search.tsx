"use client";
import { useEffect, useMemo, useState } from "react";
import Fuse from "fuse.js";
import { useRouter } from "@/i18n/navigation";
import type { DocIndexEntry } from "@/lib/docs";
import { Modal, ModalContent } from "@/components/ui/modal";
import { Kbd } from "@/components/ui/kbd";
import { Search } from "@/components/ui/icons";

export function DocsSearch({
  index,
  placeholder,
  noResults,
}: {
  index: DocIndexEntry[];
  placeholder: string;
  noResults: string;
}) {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const router = useRouter();
  const fuse = useMemo(
    () =>
      new Fuse(index, {
        keys: [
          { name: "title", weight: 0.5 },
          { name: "headings", weight: 0.3 },
          { name: "text", weight: 0.2 },
        ],
        threshold: 0.35,
        ignoreLocation: true,
      }),
    [index],
  );
  const results = q.trim()
    ? fuse
        .search(q)
        .slice(0, 8)
        .map((r) => r.item)
    : index;

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setOpen(true);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="flex h-10 w-full items-center gap-2 rounded-full border border-border bg-surface-1 px-4 text-sm text-fg-subtle transition-colors hover:border-white/30 hover:text-white"
      >
        <Search className="size-4" />
        <span className="flex-1 text-left">{placeholder}</span>
        <Kbd>⌘K</Kbd>
      </button>
      <Modal open={open} onOpenChange={setOpen}>
        <ModalContent title={placeholder}>
          <input
            autoFocus
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder={placeholder}
            aria-label={placeholder}
            className="h-11 w-full rounded-md border border-border-strong bg-surface-1 px-3.5 text-sm text-white focus-visible:border-white focus-visible:outline-none"
          />
          <ul className="mt-3 max-h-80 overflow-y-auto" data-lenis-prevent>
            {results.length === 0 ? (
              <li className="px-3 py-6 text-center text-sm text-fg-subtle">{noResults}</li>
            ) : null}
            {results.map((r) => (
              <li key={r.slug}>
                <button
                  type="button"
                  onClick={() => {
                    setOpen(false);
                    router.push(`/docs/${r.slug}`);
                  }}
                  className="w-full rounded-md px-3 py-2.5 text-left hover:bg-white/[0.06]"
                >
                  <p className="text-sm text-white">{r.title}</p>
                  <p className="mt-0.5 line-clamp-1 text-xs text-fg-subtle">
                    {r.headings.join(" · ")}
                  </p>
                </button>
              </li>
            ))}
          </ul>
        </ModalContent>
      </Modal>
    </>
  );
}
