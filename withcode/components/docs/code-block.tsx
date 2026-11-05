"use client";
import { useRef, useState, type ComponentProps } from "react";
import { Check, Copy } from "@/components/ui/icons";

export function CodeBlock(props: ComponentProps<"pre">) {
  const ref = useRef<HTMLPreElement>(null);
  const [copied, setCopied] = useState(false);
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(ref.current?.innerText ?? "");
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* brak dostępu do schowka */
    }
  };
  return (
    <div className="group relative">
      <pre ref={ref} {...props} />
      <button
        type="button"
        onClick={copy}
        aria-label={copied ? "Copied" : "Copy"}
        className="absolute top-3 right-3 rounded-md border border-border bg-surface-2 p-1.5 text-fg-subtle opacity-0 transition-opacity group-hover:opacity-100 hover:text-white focus-visible:opacity-100"
      >
        {copied ? <Check className="size-3.5" /> : <Copy className="size-3.5" />}
      </button>
    </div>
  );
}
