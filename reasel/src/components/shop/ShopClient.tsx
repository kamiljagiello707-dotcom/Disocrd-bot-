'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import type { Category, ColorFamily, Product, Size } from '@/lib/types';
import { categories } from '@/lib/catalog';
import { formatPrice, plural, cn } from '@/lib/format';
import { ProductCard } from '@/components/product/ProductCard';
import { CloseIcon } from '@/components/ui/Icons';
import { PriceRange } from './PriceRange';
import { useScrollLock } from '@/hooks/useScrollLock';
import { useFocusTrap } from '@/hooks/useFocusTrap';
import { ScrollTrigger } from '@/lib/gsap';

const SIZES: Size[] = ['XS', 'S', 'M', 'L', 'XL', 'XXL'];

const COLOR_SWATCHES: Record<ColorFamily, string> = {
  czarny: '#141414',
  biały: '#f2f1ee',
  szary: '#8d8f93',
  beżowy: '#cdb796',
  zielony: '#545a3a',
  niebieski: '#2c3e66',
  czerwony: '#b3202a',
  pomarańczowy: '#ff5a1f',
  różowy: '#e7c3c3',
  żółty: '#e0a526',
  brązowy: '#7a4a2a',
  limonkowy: '#c6ff00',
};

const SORTS = [
  { id: 'polecane', label: 'Polecane' },
  { id: 'najnowsze', label: 'Najnowsze' },
  { id: 'cena-rosnaco', label: 'Cena: od najniższej' },
  { id: 'cena-malejaco', label: 'Cena: od najwyższej' },
  { id: 'nazwa', label: 'Nazwa A–Z' },
] as const;

type SortId = (typeof SORTS)[number]['id'];

interface Filters {
  categories: Category[];
  sizes: Size[];
  colors: ColorFamily[];
  price: [number, number];
}

function toggle<T>(list: T[], value: T): T[] {
  return list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
}

/** Sklep: filtry (kategoria, rozmiar, kolor, cena), sortowanie i animowane przetasowanie siatki. */
export function ShopClient({ products, initialCategory }: { products: Product[]; initialCategory: Category | null }) {
  const priceBounds = useMemo<[number, number]>(() => {
    const prices = products.map((p) => p.price);
    return [Math.floor(Math.min(...prices) / 1000) * 1000, Math.ceil(Math.max(...prices) / 1000) * 1000];
  }, [products]);

  const [filters, setFilters] = useState<Filters>({
    categories: initialCategory ? [initialCategory] : [],
    sizes: [],
    colors: [],
    price: priceBounds,
  });
  const [sort, setSort] = useState<SortId>('polecane');
  const [mobileOpen, setMobileOpen] = useState(false);
  const drawerRef = useRef<HTMLDivElement>(null);

  useScrollLock(mobileOpen);
  useFocusTrap(drawerRef, mobileOpen, () => setMobileOpen(false));

  // Synchronizacja z ?kategoria= (np. kliknięcie „Bluzy” w menu, będąc już w sklepie)
  useEffect(() => {
    setFilters((f) => {
      if (initialCategory) {
        return f.categories.length === 1 && f.categories[0] === initialCategory ? f : { ...f, categories: [initialCategory] };
      }
      // Brak parametru: reset tylko gdy w URL była pojedyncza kategoria (wiele kategorii nie trafia do URL)
      return f.categories.length === 1 ? { ...f, categories: [] } : f;
    });
  }, [initialCategory]);

  // Odzwierciedlenie wybranych kategorii w URL (bez przeładowania i bez wpisu w historii)
  useEffect(() => {
    const url = new URL(window.location.href);
    if (filters.categories.length === 1) url.searchParams.set('kategoria', filters.categories[0]);
    else url.searchParams.delete('kategoria');
    window.history.replaceState(window.history.state, '', url);
  }, [filters.categories]);

  const availableColors = useMemo(() => {
    const set = new Set<ColorFamily>();
    products.forEach((p) => p.colors.forEach((c) => set.add(c.family)));
    return Array.from(set);
  }, [products]);

  const results = useMemo(() => {
    const list = products.filter((p) => {
      if (filters.categories.length && !filters.categories.includes(p.category)) return false;
      if (filters.sizes.length && !p.sizes.some((s) => filters.sizes.includes(s) && !p.soldOutSizes?.includes(s)))
        return false;
      if (filters.colors.length && !p.colors.some((c) => filters.colors.includes(c.family))) return false;
      if (p.price < filters.price[0] || p.price > filters.price[1]) return false;
      return true;
    });
    const sorted = [...list];
    switch (sort) {
      case 'najnowsze':
        sorted.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
        break;
      case 'cena-rosnaco':
        sorted.sort((a, b) => a.price - b.price);
        break;
      case 'cena-malejaco':
        sorted.sort((a, b) => b.price - a.price);
        break;
      case 'nazwa':
        sorted.sort((a, b) => a.name.localeCompare(b.name, 'pl'));
        break;
      default:
        sorted.sort((a, b) => Number(b.tags.includes('bestseller')) - Number(a.tags.includes('bestseller')));
    }
    return sorted;
  }, [products, filters, sort]);

  // Po zmianie wysokości siatki przeliczamy pozycje ScrollTriggerów
  useEffect(() => {
    const t = window.setTimeout(() => ScrollTrigger.refresh(), 700);
    return () => window.clearTimeout(t);
  }, [results.length]);

  const activeCount =
    filters.categories.length +
    filters.sizes.length +
    filters.colors.length +
    (filters.price[0] !== priceBounds[0] || filters.price[1] !== priceBounds[1] ? 1 : 0);

  const reset = () => setFilters({ categories: [], sizes: [], colors: [], price: priceBounds });

  const filterPanel = (
    <div className="space-y-10">
      <fieldset>
        <legend className="eyebrow mb-4">Kategoria</legend>
        <div className="flex flex-wrap gap-2">
          {categories.map((c) => {
            const on = filters.categories.includes(c.slug);
            return (
              <button
                key={c.slug}
                type="button"
                aria-pressed={on}
                onClick={() => setFilters((f) => ({ ...f, categories: toggle(f.categories, c.slug) }))}
                className={cn(
                  'rounded-full border px-4 py-2 text-sm transition-colors',
                  on ? 'border-acid bg-acid text-ink' : 'border-white/15 hover:border-white/50',
                )}
              >
                {c.label}
              </button>
            );
          })}
        </div>
      </fieldset>

      <fieldset>
        <legend className="eyebrow mb-4">Rozmiar</legend>
        <div className="grid grid-cols-6 gap-2">
          {SIZES.map((s) => {
            const on = filters.sizes.includes(s);
            return (
              <button
                key={s}
                type="button"
                aria-pressed={on}
                onClick={() => setFilters((f) => ({ ...f, sizes: toggle(f.sizes, s) }))}
                className={cn(
                  'rounded-lg border py-2.5 text-xs font-semibold transition-colors',
                  on ? 'border-acid bg-acid text-ink' : 'border-white/15 hover:border-white/50',
                )}
              >
                {s}
              </button>
            );
          })}
        </div>
      </fieldset>

      <fieldset>
        <legend className="eyebrow mb-4">Kolor</legend>
        <div className="flex flex-wrap gap-3">
          {availableColors.map((c) => {
            const on = filters.colors.includes(c);
            return (
              <button
                key={c}
                type="button"
                aria-pressed={on}
                aria-label={`Kolor: ${c}`}
                title={c}
                onClick={() => setFilters((f) => ({ ...f, colors: toggle(f.colors, c) }))}
                className={cn(
                  'relative h-9 w-9 rounded-full border-2 transition-transform hover:scale-110',
                  on ? 'border-acid' : 'border-white/20',
                )}
                style={{ background: COLOR_SWATCHES[c] }}
              >
                {on && <span className="absolute inset-0 m-auto h-2 w-2 rounded-full bg-acid ring-2 ring-ink" aria-hidden />}
              </button>
            );
          })}
        </div>
      </fieldset>

      <fieldset>
        <legend className="eyebrow mb-4">Cena</legend>
        <PriceRange
          min={priceBounds[0]}
          max={priceBounds[1]}
          step={1000}
          value={filters.price}
          onChange={(price) => setFilters((f) => ({ ...f, price }))}
        />
        <div className="mt-3 flex justify-between text-sm tabular-nums text-ink-200">
          <span>{formatPrice(filters.price[0])}</span>
          <span>{formatPrice(filters.price[1])}</span>
        </div>
      </fieldset>

      {activeCount > 0 && (
        <button type="button" onClick={reset} className="text-sm uppercase tracking-[0.14em] text-acid underline underline-offset-4">
          Wyczyść filtry ({activeCount})
        </button>
      )}
    </div>
  );

  return (
    <div className="container-x grid gap-10 pb-32 lg:grid-cols-12">
      {/* Filtry – desktop */}
      <aside className="hidden lg:col-span-3 lg:block" aria-label="Filtry produktów">
        <div className="sticky top-28" data-lenis-prevent>
          {filterPanel}
        </div>
      </aside>

      <div className="lg:col-span-9">
        <div className="mb-8 flex flex-wrap items-center justify-between gap-4 border-b border-white/10 pb-5">
          <p className="text-sm text-ink-300" aria-live="polite">
            <strong className="text-white">{results.length}</strong> {plural(results.length, 'produkt', 'produkty', 'produktów')}
          </p>
          <div className="flex items-center gap-3">
            <button type="button" onClick={() => setMobileOpen(true)} className="btn-ghost px-5 py-2.5 text-xs lg:hidden">
              Filtry{activeCount > 0 && ` (${activeCount})`}
            </button>
            <label htmlFor="sort" className="sr-only">
              Sortuj
            </label>
            <select
              id="sort"
              value={sort}
              onChange={(e) => setSort(e.target.value as SortId)}
              className="rounded-full border border-white/15 bg-ink px-4 py-2.5 text-sm focus:border-acid focus:outline-none"
            >
              {SORTS.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <h2 className="sr-only">Lista produktów</h2>
        <motion.ul layout className="grid grid-cols-2 gap-x-4 gap-y-12 md:grid-cols-3 md:gap-x-6">
          <AnimatePresence mode="popLayout">
            {results.map((p, i) => (
              <motion.li
                key={p.id}
                layout
                initial={{ opacity: 0, scale: 0.85, y: 40 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.85, filter: 'blur(8px)' }}
                transition={{ type: 'spring', stiffness: 260, damping: 30, delay: Math.min(i, 8) * 0.03 }}
              >
                <ProductCard product={p} priority={i < 3} sizes="(min-width: 1024px) 25vw, (min-width: 768px) 33vw, 50vw" />
              </motion.li>
            ))}
          </AnimatePresence>
        </motion.ul>

        {results.length === 0 && (
          <div className="py-24 text-center">
            <p className="h-display text-4xl text-ink-400">Brak wyników</p>
            <p className="mt-3 text-ink-300">Zmień kryteria albo wyczyść filtry.</p>
            <button type="button" onClick={reset} className="btn-primary mt-8">
              Wyczyść filtry
            </button>
          </div>
        )}
      </div>

      {/* Filtry – mobile (drawer) */}
      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div className="fixed inset-0 z-[60] bg-black/60" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setMobileOpen(false)} aria-hidden />
            <motion.div
              ref={drawerRef}
              role="dialog"
              aria-modal="true"
              aria-label="Filtry"
              className="fixed inset-x-0 bottom-0 z-[61] max-h-[85svh] overflow-y-auto rounded-t-3xl border-t border-white/10 bg-ink-900 p-6"
              data-lenis-prevent
              initial={{ y: '100%' }}
              animate={{ y: 0 }}
              exit={{ y: '100%' }}
              transition={{ duration: 0.5, ease: [0.76, 0, 0.24, 1] }}
            >
              <div className="mb-6 flex items-center justify-between">
                <h2 className="h-display text-2xl">Filtry</h2>
                <button type="button" onClick={() => setMobileOpen(false)} className="grid h-10 w-10 place-items-center rounded-full hover:bg-white/10" aria-label="Zamknij filtry">
                  <CloseIcon />
                </button>
              </div>
              {filterPanel}
              <button type="button" onClick={() => setMobileOpen(false)} className="btn-primary mt-10 w-full">
                Pokaż {results.length} {plural(results.length, 'produkt', 'produkty', 'produktów')}
              </button>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
