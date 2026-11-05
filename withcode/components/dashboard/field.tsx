export function Field({
  label,
  htmlFor,
  hint,
  error,
  children,
}: {
  label: string;
  htmlFor?: string;
  hint?: string;
  error?: string | undefined;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label htmlFor={htmlFor} className="mb-2 block text-sm text-white">
        {label}
      </label>
      {children}
      {hint ? <p className="mt-1.5 text-xs text-fg-subtle">{hint}</p> : null}
      {error ? (
        <p role="alert" className="mt-1.5 text-xs text-danger">
          {error}
        </p>
      ) : null}
    </div>
  );
}

export function ToggleRow({
  id,
  label,
  desc,
  children,
}: {
  id: string;
  label: string;
  desc?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div>
        <p id={id} className="text-base font-medium text-white">
          {label}
        </p>
        {desc ? <p className="mt-1 text-sm text-fg-muted">{desc}</p> : null}
      </div>
      {children}
    </div>
  );
}

export const numberInputClass =
  "h-11 w-full rounded-md border border-border-strong bg-surface-1 px-3 text-sm text-white tabular-nums focus-visible:border-white focus-visible:outline-none aria-[invalid=true]:border-danger/70";
