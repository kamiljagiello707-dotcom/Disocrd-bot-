import type { ReactNode } from 'react';
import { PageHeader } from './PageHeader';

/**
 * Szablon stron prawnych. Treść to wzór – przed startem sklepu
 * należy ją zweryfikować z prawnikiem i uzupełnić dane firmy.
 */
export function LegalPage({ title, eyebrow, updated, children }: { title: string; eyebrow: string; updated: string; children: ReactNode }) {
  return (
    <div className="relative z-10 min-h-screen bg-ink">
      <PageHeader eyebrow={eyebrow} title={title} />
      <div className="container-x grid gap-12 pb-32 lg:grid-cols-12">
        <aside className="lg:col-span-3">
          <p className="text-sm text-ink-400 lg:sticky lg:top-28">Ostatnia aktualizacja: {updated}</p>
        </aside>
        <article className="prose-legal max-w-3xl lg:col-span-8">{children}</article>
      </div>
    </div>
  );
}
