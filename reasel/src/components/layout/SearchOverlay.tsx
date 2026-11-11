'use client';

import { useMemo, useRef, useState, useDeferredValue } from 'react';
import Image from 'next/image';
import { AnimatePresence, motion } from 'framer-motion';
import { useRouter } from 'next/navigation';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { CloseIcon, SearchIcon } from '@/components/ui/Icons';
import { categoryLabel, getSearchIndex } from '@/lib/catalog';
import { formatPrice, normalizeText, plural } from '@/lib/format';
import { useUI } from '@/store/ui';
import { useTransition } from '@/store/transition';
import { useScrollLock } from '@/hooks/useScrollLock';
import { useFocusTrap } from '@/hooks/useFocusTrap';

const index = getSearchIndex().map((e) => ({ ...e, haystack: normalizeText(e.text) }));
const SUGGESTIONS = ['bluza', 'kurtka', 'jeansy', 'czarny', 'cargo', 'czapka'];

/** Pełnoekranowa wyszukiwarka z wynikami na żywo (bez diakrytyków, wiele słów). */
export function SearchOverlay() {
  const { searchOpen, setSearchOpen } = useUI();
  const [query, setQuery] = useState('');
  const deferred = useDeferredValue(query);
  const ref = useRef<HTMLDivElement>(null);
  const router = useRouter();
  const start = useTransition((s) => s.start);
  const close = () => setSearchOpen(false);

  useScrollLock(searchOpen);
  useFocusTrap(ref, searchOpen, close);

  const results = useMemo(() => {
    const terms = normalizeText(deferred.trim()).split(/\s+/).filter(Boolean);
    if (!terms.length) return [];
    return index.filter((e) => terms.every((t) => e.haystack.includes(t))).slice(0, 8);
  }, [deferred]);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (results[0]) {
      close();
      start(`/produkt/${results[0].slug}`);
    } else if (query.trim()) {
      close();
      router.push('/sklep');
    }
  };

  return (
    <AnimatePresence onExitComplete={() => setQuery('')}>
      {searchOpen && (
        <motion.div
          ref={ref}
          role="dialog"
          aria-modal="true"
          aria-label="Wyszukiwarka produktów"
          className="fixed inset-0 z-[70] overflow-y-auto bg-ink/95 backdrop-blur-2xl"
          data-lenis-prevent
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.35 }}
        >
          <div className="container-x pb-20 pt-8">
            <div className="flex justify-end">
              <button type="button" onClick={close} className="grid h-12 w-12 place-items-center rounded-full border border-white/15 hover:border-acid hover:text-acid" aria-label="Zamknij wyszukiwarkę">
                <CloseIcon />
              </button>
            </div>

            <motion.form
              role="search"
              onSubmit={onSubmit}
              className="mt-10 border-b border-white/20 focus-within:border-acid"
              initial={{ y: 40, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.7 }}
            >
              <label htmlFor="search-input" className="eyebrow">
                Czego szukasz?
              </label>
              <div className="flex items-center gap-4">
                <SearchIcon width={36} height={36} className="shrink-0 text-ink-400" />
                <input
                  id="search-input"
                  data-autofocus
                  type="search"
                  autoComplete="off"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Bluza, kurtka, czarny…"
                  className="h-display w-full bg-transparent py-4 text-4xl placeholder:text-ink-400 focus:outline-none md:text-7xl"
                  aria-controls="search-results"
                />
              </div>
            </motion.form>

            {!query && (
              <div className="mt-8 flex flex-wrap items-center gap-3">
                <span className="eyebrow mr-2">Popularne:</span>
                {SUGGESTIONS.map((s) => (
                  <button key={s} type="button" onClick={() => setQuery(s)} className="rounded-full border border-white/15 px-4 py-2 text-sm hover:border-acid hover:text-acid">
                    {s}
                  </button>
                ))}
              </div>
            )}

            <div id="search-results" aria-live="polite" className="mt-10">
              {query && (
                <p className="eyebrow mb-6">
                  {results.length} {plural(results.length, 'wynik', 'wyniki', 'wyników')}
                </p>
              )}
              <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <AnimatePresence mode="popLayout">
                  {results.map((r, i) => (
                    <motion.li
                      key={r.id}
                      layout
                      initial={{ opacity: 0, y: 30 }}
                      animate={{ opacity: 1, y: 0, transition: { delay: i * 0.04 } }}
                      exit={{ opacity: 0, scale: 0.95 }}
                    >
                      <TransitionLink href={`/produkt/${r.slug}`} onClick={close} className="group flex gap-4 rounded-2xl border border-white/10 p-3 hover:border-acid/60" data-cursor="view">
                        <span className="relative h-24 w-20 shrink-0 overflow-hidden rounded-lg bg-ink-800">
                          <Image src={r.image.src} alt={r.image.alt} fill sizes="80px" className="object-cover transition-transform duration-700 group-hover:scale-110" />
                        </span>
                        <span className="flex flex-col justify-center">
                          <span className="eyebrow">{categoryLabel(r.category)}</span>
                          <span className="mt-1 font-semibold group-hover:text-acid">{r.name}</span>
                          <span className="mt-1 text-sm text-ink-300">{formatPrice(r.price)}</span>
                        </span>
                      </TransitionLink>
                    </motion.li>
                  ))}
                </AnimatePresence>
              </ul>
              {query && results.length === 0 && (
                <p className="text-lg text-ink-300">
                  Nic nie znaleźliśmy dla „{query}”. Spróbuj innej frazy lub{' '}
                  <TransitionLink href="/sklep" onClick={close} className="text-acid underline underline-offset-4">
                    przejrzyj cały sklep
                  </TransitionLink>
                  .
                </p>
              )}
            </div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
