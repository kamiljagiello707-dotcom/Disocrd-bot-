export function DashHeader({
  title,
  subtitle,
  actions,
}: {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
}) {
  return (
    <header className="mb-8 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="heading text-3xl text-white">{title}</h1>
        {subtitle ? <p className="mt-2 max-w-2xl text-sm text-fg-muted">{subtitle}</p> : null}
      </div>
      {actions}
    </header>
  );
}

export function Panel({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <section className={`rounded-xl border border-border bg-surface-1 p-5 md:p-6 ${className}`}>
      {children}
    </section>
  );
}
