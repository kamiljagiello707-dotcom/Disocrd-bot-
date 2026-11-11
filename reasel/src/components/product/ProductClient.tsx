'use client';

import { useRef, useState } from 'react';
import Image from 'next/image';
import dynamic from 'next/dynamic';
import { AnimatePresence, motion } from 'framer-motion';
import type { Product, Size } from '@/lib/types';
import type { ViewerApi } from '@/components/3d/ProductViewer';
import { categoryLabel } from '@/lib/catalog';
import { cn, formatPrice } from '@/lib/format';
import { siteConfig } from '@/config/site';
import { useCart } from '@/store/cart';
import { useUI } from '@/store/ui';
import { useScene } from '@/store/scene';
import { useReducedMotion } from '@/hooks/useMediaQuery';
import { Price } from '@/components/ui/Price';
import { Modal } from '@/components/ui/Modal';
import { Magnetic } from '@/components/ui/Magnetic';
import { FavoriteButton } from '@/components/ui/FavoriteButton';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { CheckIcon, MinusIcon, PlusIcon, RotateIcon, StarIcon, TruckIcon } from '@/components/ui/Icons';
import { SizeChart } from './SizeChart';

const ProductViewer = dynamic(() => import('@/components/3d/ProductViewer'), {
  ssr: false,
  loading: () => (
    <div className="grid h-full place-items-center">
      <span className="eyebrow animate-pulse">Ładowanie modelu 3D…</span>
    </div>
  ),
});

type Media = { kind: '3d' } | { kind: 'image'; index: number };

function Accordion({ title, children, defaultOpen = false }: { title: string; children: React.ReactNode; defaultOpen?: boolean }) {
  const [open, setOpen] = useState(defaultOpen);
  const id = `acc-${title.replace(/\s+/g, '-').toLowerCase()}`;
  return (
    <div className="border-b border-white/10">
      <h2>
        <button
          type="button"
          className="flex w-full items-center justify-between py-5 text-left text-sm font-semibold uppercase tracking-[0.14em]"
          aria-expanded={open}
          aria-controls={id}
          onClick={() => setOpen((o) => !o)}
        >
          {title}
          <motion.span animate={{ rotate: open ? 45 : 0 }} aria-hidden>
            <PlusIcon width={18} height={18} />
          </motion.span>
        </button>
      </h2>
      <AnimatePresence initial={false}>
        {open && (
          <motion.div
            id={id}
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.4 }}
            className="overflow-hidden"
          >
            <div className="pb-6 text-[15px] leading-relaxed text-ink-200">{children}</div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

/** Strona produktu: galeria + podgląd 3D, warianty, tabela rozmiarów, dodawanie do koszyka z animacją. */
export function ProductClient({ product }: { product: Product }) {
  const webgl = useScene((s) => s.webgl);
  const reduced = useReducedMotion();
  const [media, setMedia] = useState<Media>(webgl ? { kind: '3d' } : { kind: 'image', index: 0 });
  const [colorIdx, setColorIdx] = useState(0);
  const [size, setSize] = useState<Size | null>(product.sizes.length === 1 ? product.sizes[0] : null);
  const [qty, setQty] = useState(1);
  const [sizeError, setSizeError] = useState(false);
  const [added, setAdded] = useState(false);
  const [exploded, setExploded] = useState(false);
  const [autoRotate, setAutoRotate] = useState(true);
  const sizeChartOpen = useUI((s) => s.sizeChartOpen);
  const setSizeChartOpen = useUI((s) => s.setSizeChartOpen);
  const addFlyer = useUI((s) => s.addFlyer);
  const addToCart = useCart((s) => s.add);
  const openCart = useCart((s) => s.open);
  const mediaRef = useRef<HTMLDivElement>(null);
  const viewerApi = useRef<ViewerApi | null>(null);

  const color = product.colors[colorIdx];

  const handleAdd = () => {
    if (!size) {
      setSizeError(true);
      document.getElementById('size-picker')?.focus();
      return;
    }
    addToCart(
      {
        productId: product.id,
        slug: product.slug,
        name: product.name,
        price: product.price,
        image: product.images[0].src,
        color: { name: color.name, hex: color.hex },
        size,
      },
      qty,
    );
    // Animacja „lotu” miniatury do ikony koszyka
    const rect = mediaRef.current?.getBoundingClientRect();
    if (rect) {
      addFlyer({
        src: `/_next/image?url=${encodeURIComponent(product.images[0].src)}&w=384&q=70`,
        from: { x: rect.left, y: rect.top, width: rect.width, height: rect.height },
      });
    }
    setAdded(true);
    window.setTimeout(() => setAdded(false), 2600);
  };

  const shippingFree = product.price * qty >= siteConfig.freeShippingThreshold;

  return (
    <>
      <div className="container-x grid gap-10 pb-24 pt-[calc(var(--header-h)+1.5rem)] lg:grid-cols-12 lg:gap-14">
        {/* ───────── Galeria / 3D ───────── */}
        <div className="lg:col-span-7">
          <nav aria-label="Ścieżka nawigacji" className="mb-5 text-xs uppercase tracking-[0.16em] text-ink-400">
            <ol className="flex flex-wrap items-center gap-2">
              <li>
                <TransitionLink href="/" className="hover:text-white">
                  Start
                </TransitionLink>
              </li>
              <li aria-hidden>/</li>
              <li>
                <TransitionLink href={`/sklep?kategoria=${product.category}`} className="hover:text-white">
                  {categoryLabel(product.category)}
                </TransitionLink>
              </li>
              <li aria-hidden>/</li>
              <li aria-current="page" className="text-ink-200">
                {product.name}
              </li>
            </ol>
          </nav>

          <div ref={mediaRef} className="relative aspect-[4/5] overflow-hidden rounded-3xl bg-[radial-gradient(ellipse_at_50%_40%,#1d1d1c,#0c0c0c_70%)] md:aspect-[5/5]">
            <AnimatePresence mode="wait" initial={false}>
              {media.kind === '3d' ? (
                <motion.div key="3d" className="absolute inset-0" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} data-cursor="drag">
                  <ProductViewer
                    type={product.model}
                    color={color.hex}
                    exploded={exploded}
                    autoRotate={autoRotate}
                    apiRef={viewerApi}
                    onInteract={() => setAutoRotate(false)}
                    reduced={reduced}
                  />
                  <div className="pointer-events-none absolute left-4 top-4 flex items-center gap-2 rounded-full bg-black/50 px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.16em] backdrop-blur">
                    <RotateIcon width={14} height={14} className="text-acid" /> 360° · przeciągnij, aby obrócić
                  </div>
                  <div className="absolute bottom-4 left-1/2 flex -translate-x-1/2 items-center gap-1 rounded-full border border-white/10 bg-black/60 p-1 backdrop-blur">
                    <button type="button" onClick={() => viewerApi.current?.zoom(1)} className="grid h-10 w-10 place-items-center rounded-full hover:bg-white/10" aria-label="Przybliż">
                      <PlusIcon width={18} height={18} />
                    </button>
                    <button type="button" onClick={() => viewerApi.current?.zoom(-1)} className="grid h-10 w-10 place-items-center rounded-full hover:bg-white/10" aria-label="Oddal">
                      <MinusIcon width={18} height={18} />
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        viewerApi.current?.reset();
                        setAutoRotate(true);
                      }}
                      className="grid h-10 w-10 place-items-center rounded-full hover:bg-white/10"
                      aria-label="Resetuj widok"
                    >
                      <RotateIcon width={18} height={18} />
                    </button>
                    <button
                      type="button"
                      onClick={() => setExploded((e) => !e)}
                      aria-pressed={exploded}
                      className={cn(
                        'h-10 rounded-full px-4 text-[11px] font-semibold uppercase tracking-[0.14em] transition-colors',
                        exploded ? 'bg-acid text-ink' : 'hover:bg-white/10',
                      )}
                    >
                      {exploded ? 'Złóż' : 'Rozłóż'}
                    </button>
                  </div>
                </motion.div>
              ) : (
                <motion.div
                  key={`img-${media.index}`}
                  className="absolute inset-0"
                  initial={{ opacity: 0, scale: 1.06 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.6 }}
                >
                  <Image
                    src={product.images[media.index].src}
                    alt={product.images[media.index].alt}
                    fill
                    priority={media.index === 0}
                    sizes="(min-width: 1024px) 55vw, 100vw"
                    className="object-cover"
                  />
                </motion.div>
              )}
            </AnimatePresence>
            <FavoriteButton productId={product.id} name={product.name} className="absolute right-4 top-4 z-10" />
          </div>

          <div className="mt-4 flex gap-3" role="tablist" aria-label="Widoki produktu">
            {webgl && (
              <button
                type="button"
                role="tab"
                aria-selected={media.kind === '3d'}
                onClick={() => setMedia({ kind: '3d' })}
                className={cn(
                  'grid h-24 w-20 place-items-center rounded-xl border text-xs font-bold uppercase tracking-[0.14em] transition-colors',
                  media.kind === '3d' ? 'border-acid text-acid' : 'border-white/15 text-ink-300 hover:border-white/40',
                )}
              >
                3D
                <br />
                360°
              </button>
            )}
            {product.images.map((img, i) => {
              const selected = media.kind === 'image' && media.index === i;
              return (
                <button
                  key={img.src}
                  type="button"
                  role="tab"
                  aria-selected={selected}
                  aria-label={`Zdjęcie ${i + 1}: ${img.alt}`}
                  onClick={() => setMedia({ kind: 'image', index: i })}
                  className={cn('relative h-24 w-20 overflow-hidden rounded-xl border-2 transition-colors', selected ? 'border-acid' : 'border-transparent opacity-70 hover:opacity-100')}
                >
                  <Image src={img.src} alt="" fill sizes="80px" className="object-cover" />
                </button>
              );
            })}
          </div>
        </div>

        {/* ───────── Informacje / zakup ───────── */}
        <div className="lg:col-span-5">
          <div className="lg:sticky lg:top-28">
            <p className="eyebrow">
              {categoryLabel(product.category)} · {product.sku}
            </p>
            <h1 className="h-display mt-3 text-4xl leading-[0.95] md:text-5xl">{product.name}</h1>
            <div className="mt-4 flex items-center gap-4">
              <Price price={product.price} compareAt={product.compareAtPrice} className="text-2xl" />
              <span className="flex items-center gap-1 text-sm text-ink-300">
                <StarIcon className="text-acid" />
                <span>
                  {product.rating.toFixed(1)} <span className="text-ink-400">({product.reviewsCount} opinii)</span>
                </span>
              </span>
            </div>
            <p className="mt-5 text-ink-200">{product.shortDescription}</p>

            {/* Kolor */}
            <fieldset className="mt-8">
              <legend className="mb-3 text-sm">
                <span className="eyebrow">Kolor:</span> <span className="font-semibold">{color.name}</span>
              </legend>
              <div className="flex flex-wrap gap-3">
                {product.colors.map((c, i) => (
                  <button
                    key={c.name}
                    type="button"
                    onClick={() => {
                      setColorIdx(i);
                      if (webgl) setMedia({ kind: '3d' });
                    }}
                    aria-pressed={i === colorIdx}
                    aria-label={`Kolor ${c.name}`}
                    className={cn(
                      'h-11 w-11 rounded-full border-2 p-1 transition-transform hover:scale-110',
                      i === colorIdx ? 'border-acid' : 'border-white/15',
                    )}
                  >
                    <span className="block h-full w-full rounded-full" style={{ background: c.hex }} />
                  </button>
                ))}
              </div>
            </fieldset>

            {/* Rozmiar */}
            <fieldset className="mt-8">
              <div className="mb-3 flex items-center justify-between">
                <legend className="eyebrow">Rozmiar</legend>
                <button type="button" onClick={() => setSizeChartOpen(true)} className="text-sm text-ink-200 underline underline-offset-4 hover:text-acid">
                  Tabela rozmiarów
                </button>
              </div>
              <div
                id="size-picker"
                tabIndex={-1}
                className="grid grid-cols-3 gap-2 sm:grid-cols-6"
                role="radiogroup"
                aria-label="Wybierz rozmiar"
                aria-invalid={sizeError}
                aria-describedby={sizeError ? 'size-error' : undefined}
              >
                {product.sizes.map((s) => {
                  const soldOut = product.soldOutSizes?.includes(s);
                  const selected = size === s;
                  return (
                    <button
                      key={s}
                      type="button"
                      role="radio"
                      aria-checked={selected}
                      disabled={soldOut}
                      onClick={() => {
                        setSize(s);
                        setSizeError(false);
                      }}
                      className={cn(
                        'relative rounded-xl border py-3.5 text-sm font-semibold transition-colors',
                        s === 'ONE SIZE' && 'col-span-3 sm:col-span-6',
                        selected ? 'border-acid bg-acid text-ink' : 'border-white/15 hover:border-white/50',
                        soldOut && 'cursor-not-allowed text-ink-500 line-through hover:border-white/15',
                      )}
                    >
                      {s}
                      {soldOut && <span className="sr-only"> – wyprzedany</span>}
                    </button>
                  );
                })}
              </div>
              <AnimatePresence>
                {sizeError && (
                  <motion.p id="size-error" role="alert" className="error-text" initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                    Wybierz rozmiar, aby dodać produkt do koszyka.
                  </motion.p>
                )}
              </AnimatePresence>
            </fieldset>

            {/* Ilość + dodaj */}
            <div className="mt-8 flex gap-3">
              <div className="flex items-center rounded-full border border-white/15" role="group" aria-label="Ilość">
                <button type="button" className="grid h-14 w-12 place-items-center hover:text-acid disabled:opacity-30" onClick={() => setQty((q) => Math.max(1, q - 1))} disabled={qty <= 1} aria-label="Zmniejsz ilość">
                  <MinusIcon width={16} height={16} />
                </button>
                <span className="w-6 text-center tabular-nums" aria-live="polite">
                  {qty}
                </span>
                <button type="button" className="grid h-14 w-12 place-items-center hover:text-acid disabled:opacity-30" onClick={() => setQty((q) => Math.min(10, q + 1))} disabled={qty >= 10} aria-label="Zwiększ ilość">
                  <PlusIcon width={16} height={16} />
                </button>
              </div>
              <Magnetic className="flex-1" strength={0.15}>
                <button type="button" onClick={handleAdd} className="btn-primary h-14 w-full">
                  <AnimatePresence mode="wait" initial={false}>
                    {added ? (
                      <motion.span key="ok" className="flex items-center gap-2" initial={{ y: 20, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: -20, opacity: 0 }}>
                        <CheckIcon width={18} height={18} /> Dodano
                      </motion.span>
                    ) : (
                      <motion.span key="add" initial={{ y: 20, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: -20, opacity: 0 }}>
                        Dodaj do koszyka · {formatPrice(product.price * qty)}
                      </motion.span>
                    )}
                  </AnimatePresence>
                </button>
              </Magnetic>
            </div>
            <div className="mt-3 min-h-[24px] text-sm" aria-live="polite">
              {added && (
                <button type="button" onClick={openCart} className="text-acid underline underline-offset-4">
                  Produkt w koszyku – zobacz koszyk
                </button>
              )}
            </div>

            <ul className="mt-4 space-y-2 rounded-2xl border border-white/10 p-5 text-sm text-ink-200">
              <li className="flex items-center gap-3">
                <TruckIcon width={18} height={18} className="text-acid" />
                {shippingFree ? 'Darmowa dostawa dla tego zamówienia' : `Darmowa dostawa od ${formatPrice(siteConfig.freeShippingThreshold)}`}
              </li>
              <li className="flex items-center gap-3">
                <CheckIcon width={18} height={18} className="text-acid" /> Wysyłka w 24h · InPost Paczkomat lub kurier DPD
              </li>
              <li className="flex items-center gap-3">
                <RotateIcon width={18} height={18} className="text-acid" /> 30 dni na darmowy zwrot
              </li>
            </ul>

            <div className="mt-8 border-t border-white/10">
              <Accordion title="Opis" defaultOpen>
                <p>{product.description}</p>
              </Accordion>
              <Accordion title="Szczegóły">
                <ul className="list-disc space-y-1.5 pl-5">
                  {product.details.map((d) => (
                    <li key={d}>{d}</li>
                  ))}
                </ul>
              </Accordion>
              <Accordion title="Skład i pielęgnacja">
                <p className="mb-3">
                  <strong className="text-white">Skład:</strong> {product.composition}
                </p>
                <ul className="list-disc space-y-1.5 pl-5">
                  {product.care.map((c) => (
                    <li key={c}>{c}</li>
                  ))}
                </ul>
              </Accordion>
              <Accordion title="Dostawa i zwroty">
                <p>
                  InPost Paczkomat 24/7 – 13,99 zł, kurier DPD – 16,99 zł. Darmowa dostawa od{' '}
                  {formatPrice(siteConfig.freeShippingThreshold)}. Masz 30 dni na zwrot –{' '}
                  <TransitionLink href="/zwroty" className="text-acid underline underline-offset-4">
                    szczegóły zwrotów
                  </TransitionLink>
                  .
                </p>
              </Accordion>
            </div>
          </div>
        </div>
      </div>

      <Modal open={sizeChartOpen} onClose={() => setSizeChartOpen(false)} title="Tabela rozmiarów">
        <SizeChart category={product.category} />
      </Modal>
    </>
  );
}
