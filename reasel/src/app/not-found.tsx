import { TransitionLink } from '@/components/ui/TransitionLink';

export default function NotFound() {
  return (
    <div className="container-x flex min-h-[100svh] flex-col items-center justify-center text-center">
      <p className="h-display text-[30vw] leading-none text-acid md:text-[18rem]">404</p>
      <h1 className="h-display mt-4 text-3xl md:text-5xl">Ta strona wyprzedała się</h1>
      <p className="mt-4 max-w-md text-ink-300">Nie znaleźliśmy tego, czego szukasz. Może znajdziesz coś lepszego w sklepie?</p>
      <div className="mt-10 flex flex-wrap justify-center gap-3">
        <TransitionLink href="/sklep" className="btn-primary">
          Przejdź do sklepu
        </TransitionLink>
        <TransitionLink href="/" className="btn-ghost">
          Strona główna
        </TransitionLink>
      </div>
    </div>
  );
}
