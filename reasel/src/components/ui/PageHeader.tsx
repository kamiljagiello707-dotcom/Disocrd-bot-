import { SplitText } from './SplitText';

/** Nagłówek podstron z animowanym tytułem (split text). */
export function PageHeader({ eyebrow, title, description }: { eyebrow?: string; title: string; description?: string }) {
  return (
    <header className="container-x pb-12 pt-[calc(var(--header-h)+4rem)] md:pb-16 md:pt-[calc(var(--header-h)+6rem)]">
      {eyebrow && <p className="eyebrow mb-5">{eyebrow}</p>}
      <SplitText as="h1" text={title} immediate className="h-display text-display-lg" delay={0.55} />
      {description && <p className="mt-6 max-w-xl text-lg text-ink-200">{description}</p>}
    </header>
  );
}
