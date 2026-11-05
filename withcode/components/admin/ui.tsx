import { adminDelete } from "@/lib/actions/admin";
import { Button } from "@/components/ui/button";
import { Trash } from "@/components/ui/icons";

export const inputCls =
  "h-10 w-full rounded-md border border-border-strong bg-surface-1 px-3 text-sm text-white placeholder:text-fg-faint focus-visible:border-white focus-visible:outline-none";
export const areaCls =
  "min-h-20 w-full rounded-md border border-border-strong bg-surface-1 px-3 py-2 text-sm text-white placeholder:text-fg-faint focus-visible:border-white focus-visible:outline-none";

export function AdminField({
  label,
  name,
  textarea,
  required = true,
  placeholder,
  type = "text",
}: {
  label: string;
  name: string;
  textarea?: boolean;
  required?: boolean;
  placeholder?: string;
  type?: string;
}) {
  return (
    <label className="block text-sm text-white">
      <span className="mb-1.5 block">{label}</span>
      {textarea ? (
        <textarea name={name} required={required} placeholder={placeholder} className={areaCls} />
      ) : (
        <input
          name={name}
          type={type}
          required={required}
          placeholder={placeholder}
          className={inputCls}
        />
      )}
    </label>
  );
}

export function DeleteButton({ entity, id, label }: { entity: string; id: string; label: string }) {
  return (
    <form action={adminDelete}>
      <input type="hidden" name="entity" value={entity} />
      <input type="hidden" name="id" value={id} />
      <Button type="submit" variant="ghost" size="icon" magnetic={false} aria-label={label}>
        <Trash className="size-4" />
      </Button>
    </form>
  );
}

export function AdminCard({ title, children }: { title?: string; children: React.ReactNode }) {
  return (
    <section className="rounded-xl border border-border bg-surface-1 p-5 md:p-6">
      {title ? <h2 className="mb-4 text-base font-medium text-white">{title}</h2> : null}
      {children}
    </section>
  );
}
