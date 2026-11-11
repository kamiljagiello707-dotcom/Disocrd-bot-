'use client';

import { useRef, useState } from 'react';
import { motion, useMotionTemplate, useMotionValue, useSpring } from 'framer-motion';
import type { Product } from '@/lib/types';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { FavoriteButton } from '@/components/ui/FavoriteButton';
import { Price } from '@/components/ui/Price';
import { categoryLabel } from '@/lib/catalog';
import { useFinePointer, useReducedMotion } from '@/hooks/useMediaQuery';
import { useScene } from '@/store/scene';
import { DisplacementImage } from './DisplacementImage';
import { cn } from '@/lib/format';

interface Props {
  product: Product;
  priority?: boolean;
  className?: string;
  sizes?: string;
}

/**
 * Karta produktu: tilt 3D za kursorem (sprężyny Framer Motion), odblask,
 * przejście między zdjęciami shaderem displacement, serduszko ulubionych.
 */
export function ProductCard({ product, priority, className, sizes = '(min-width: 1024px) 25vw, (min-width: 640px) 50vw, 100vw' }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const [hovered, setHovered] = useState(false);
  const fine = useFinePointer();
  const reduced = useReducedMotion();
  const quality = useScene((s) => s.quality);
  const tiltEnabled = fine && !reduced;

  const rx = useMotionValue(0);
  const ry = useMotionValue(0);
  const gx = useMotionValue(50);
  const gy = useMotionValue(50);
  const srx = useSpring(rx, { stiffness: 180, damping: 18 });
  const sry = useSpring(ry, { stiffness: 180, damping: 18 });
  const glare = useMotionTemplate`radial-gradient(circle at ${gx}% ${gy}%, rgba(255,255,255,0.22), transparent 55%)`;

  const onMove = (e: React.PointerEvent) => {
    if (!tiltEnabled || !ref.current) return;
    const r = ref.current.getBoundingClientRect();
    const px = (e.clientX - r.left) / r.width;
    const py = (e.clientY - r.top) / r.height;
    ry.set((px - 0.5) * 14);
    rx.set(-(py - 0.5) * 14);
    gx.set(px * 100);
    gy.set(py * 100);
  };
  const onLeave = () => {
    rx.set(0);
    ry.set(0);
    setHovered(false);
  };

  const discount = product.compareAtPrice ? Math.round((1 - product.price / product.compareAtPrice) * 100) : 0;
  const [img1, img2] = product.images;

  return (
    <div className={cn('group relative [perspective:1100px]', className)}>
      <TransitionLink
        href={`/produkt/${product.slug}`}
        data-cursor="view"
        className="block rounded-2xl"
        onPointerEnter={() => setHovered(true)}
        onPointerLeave={onLeave}
        onPointerMove={onMove}
        onFocus={() => setHovered(true)}
        onBlur={() => setHovered(false)}
      >
        <motion.div
          ref={ref}
          data-product-image={product.id}
          className="preserve-3d relative aspect-[4/5] overflow-hidden rounded-2xl bg-ink-800"
          style={tiltEnabled ? { rotateX: srx, rotateY: sry } : undefined}
        >
          <DisplacementImage
            images={[img1, img2 ?? img1]}
            hovered={hovered}
            enableShader={tiltEnabled && quality === 'high'}
            sizes={sizes}
            priority={priority}
          />
          {tiltEnabled && (
            <motion.div
              aria-hidden
              className="pointer-events-none absolute inset-0 opacity-0 mix-blend-overlay transition-opacity duration-500 group-hover:opacity-100"
              style={{ background: glare }}
            />
          )}
          <div className="absolute left-3 top-3 flex flex-col items-start gap-1.5 [transform:translateZ(40px)]">
            {product.tags.includes('nowosc') && (
              <span className="rounded-full bg-acid px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.14em] text-ink">Nowość</span>
            )}
            {discount > 0 && (
              <span className="rounded-full bg-white px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.14em] text-ink">−{discount}%</span>
            )}
            {product.tags.includes('limitowana') && (
              <span className="rounded-full border border-white/40 bg-black/40 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.14em] text-white backdrop-blur">
                Limitowana
              </span>
            )}
          </div>
          <div className="absolute bottom-3 left-3 right-3 flex translate-y-3 items-center gap-1.5 opacity-0 transition-all duration-500 group-hover:translate-y-0 group-hover:opacity-100 [transform:translateZ(30px)]">
            {product.sizes.map((s) => (
              <span
                key={s}
                className={cn(
                  'rounded-md bg-black/60 px-2 py-1 text-[10px] font-medium backdrop-blur',
                  product.soldOutSizes?.includes(s) && 'text-ink-400 line-through',
                )}
              >
                {s}
              </span>
            ))}
          </div>
        </motion.div>
        <div className="mt-4 flex items-start justify-between gap-4 px-1">
          <div className="min-w-0">
            <p className="eyebrow">{categoryLabel(product.category)}</p>
            <h3 className="mt-1.5 truncate text-[15px] font-semibold transition-colors group-hover:text-acid">{product.name}</h3>
            <div className="mt-2 flex gap-1.5" aria-label={`Dostępne kolory: ${product.colors.map((c) => c.name).join(', ')}`}>
              {product.colors.map((c) => (
                <span key={c.name} className="h-3 w-3 rounded-full border border-white/25" style={{ background: c.hex }} />
              ))}
            </div>
          </div>
          <Price price={product.price} compareAt={product.compareAtPrice} className="shrink-0 text-[15px]" />
        </div>
      </TransitionLink>
      <FavoriteButton productId={product.id} name={product.name} className="absolute right-3 top-3 z-10" />
    </div>
  );
}
