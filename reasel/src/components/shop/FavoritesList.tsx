'use client';

import { AnimatePresence, motion } from 'framer-motion';
import type { Product } from '@/lib/types';
import { useFavorites } from '@/store/favorites';
import { useHydrated } from '@/hooks/useHydrated';
import { ProductCard } from '@/components/product/ProductCard';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { HeartIcon } from '@/components/ui/Icons';

export function FavoritesList({ products }: { products: Product[] }) {
  const ids = useFavorites((s) => s.ids);
  const hydrated = useHydrated();
  const list = products.filter((p) => ids.includes(p.id));

  if (!hydrated) return <div className="h-[40vh]" aria-busy="true" />;

  if (list.length === 0) {
    return (
      <div className="container-x pb-32">
        <div className="flex flex-col items-center rounded-3xl border border-dashed border-white/15 px-6 py-24 text-center">
          <HeartIcon width={40} height={40} className="text-ink-500" />
          <p className="h-display mt-6 text-3xl">Jeszcze nic tu nie ma</p>
          <p className="mt-3 max-w-sm text-ink-300">Kliknij serduszko przy produkcie, aby zapisać go na później.</p>
          <TransitionLink href="/sklep" className="btn-primary mt-8">
            Przeglądaj sklep
          </TransitionLink>
        </div>
      </div>
    );
  }

  return (
    <>
      <h2 className="sr-only">Zapisane produkty</h2>
      <ul className="container-x grid grid-cols-2 gap-x-4 gap-y-12 pb-32 md:grid-cols-4 md:gap-x-6">
        <AnimatePresence mode="popLayout">
          {list.map((p) => (
            <motion.li key={p.id} layout initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, scale: 0.9 }}>
              <ProductCard product={p} sizes="(min-width: 768px) 25vw, 50vw" />
            </motion.li>
          ))}
        </AnimatePresence>
      </ul>
    </>
  );
}
