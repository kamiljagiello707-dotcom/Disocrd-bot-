'use client';

import { useRef } from 'react';
import type { Product } from '@/lib/types';
import { SplitText } from '@/components/ui/SplitText';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { ArrowIcon } from '@/components/ui/Icons';
import { ProductCard } from '@/components/product/ProductCard';
import { useStorySection } from '@/hooks/useStorySection';
import { motion } from 'framer-motion';

/** Bestsellery – lewa kolumna sticky (za nią model 3D zmienia materiał na neonowy). */
export function Bestsellers({ products }: { products: Product[] }) {
  const ref = useRef<HTMLElement>(null);
  useStorySection(ref, 2);

  return (
    <section ref={ref} className="relative py-24 md:py-40" aria-labelledby="bestsellers-title">
      <div className="container-x grid gap-12 lg:grid-cols-12">
        <div className="lg:col-span-4">
          <div className="lg:sticky lg:top-32">
            <p className="eyebrow mb-4">Najczęściej wybierane</p>
            <SplitText id="bestsellers-title" text="Bestsellery" className="h-display text-display-md" />
            <p className="mt-6 max-w-sm text-ink-200">
              Modele, które wracają do nas w zamówieniach co tydzień. Sprawdzone kroje, ciężkie tkaniny i detale, które
              robią różnicę.
            </p>
            <TransitionLink href="/sklep" className="btn-ghost mt-8">
              Cały sklep <ArrowIcon width={18} height={18} />
            </TransitionLink>
          </div>
        </div>
        <ul className="grid gap-x-6 gap-y-14 sm:grid-cols-2 lg:col-span-8">
          {products.map((p, i) => (
            <motion.li
              key={p.id}
              initial={{ opacity: 0, y: 60 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-10%' }}
              transition={{ duration: 1, delay: (i % 2) * 0.12 }}
              className={i % 2 === 1 ? 'sm:mt-24' : undefined}
            >
              <ProductCard product={p} sizes="(min-width: 1024px) 30vw, (min-width: 640px) 50vw, 100vw" />
            </motion.li>
          ))}
        </ul>
      </div>
    </section>
  );
}
