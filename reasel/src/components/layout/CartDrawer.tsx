'use client';

import { useRef } from 'react';
import Image from 'next/image';
import { AnimatePresence, motion } from 'framer-motion';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { CloseIcon, MinusIcon, PlusIcon, TruckIcon, ArrowIcon } from '@/components/ui/Icons';
import { freeShippingProgress, MAX_QTY, selectCount, selectSubtotal, useCart } from '@/store/cart';
import { formatPrice, plural } from '@/lib/format';
import { useScrollLock } from '@/hooks/useScrollLock';
import { useFocusTrap } from '@/hooks/useFocusTrap';

/** Wysuwany panel koszyka z paskiem postępu darmowej dostawy. */
export function CartDrawer() {
  const { items, isOpen, close, setQuantity, remove } = useCart();
  const count = useCart(selectCount);
  const subtotal = useCart(selectSubtotal);
  const shipping = freeShippingProgress(subtotal);
  const panelRef = useRef<HTMLDivElement>(null);

  useScrollLock(isOpen);
  useFocusTrap(panelRef, isOpen, close);

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            key="backdrop"
            className="fixed inset-0 z-[60] bg-black/60 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={close}
            aria-hidden
          />
          <motion.aside
            key="panel"
            ref={panelRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="cart-title"
            className="fixed inset-y-0 right-0 z-[61] flex w-full max-w-[460px] flex-col border-l border-white/10 bg-ink-900"
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ duration: 0.6, ease: [0.76, 0, 0.24, 1] }}
          >
            <header className="flex items-center justify-between border-b border-white/10 px-6 py-5">
              <h2 id="cart-title" className="h-display text-2xl">
                Koszyk <span className="text-ink-400">({count})</span>
              </h2>
              <button type="button" onClick={close} className="grid h-10 w-10 place-items-center rounded-full hover:bg-white/10" aria-label="Zamknij koszyk">
                <CloseIcon />
              </button>
            </header>

            {/* Pasek darmowej dostawy */}
            <div className="border-b border-white/10 px-6 py-4" aria-live="polite">
              <p className="mb-3 flex items-center gap-2 text-sm text-ink-200">
                <TruckIcon width={18} height={18} className="text-acid" />
                {shipping.reached ? (
                  <span>
                    Masz <strong className="text-acid">darmową dostawę</strong>!
                  </span>
                ) : (
                  <span>
                    Brakuje Ci <strong className="text-white">{formatPrice(shipping.remaining)}</strong> do darmowej dostawy
                  </span>
                )}
              </p>
              <div
                className="h-1.5 overflow-hidden rounded-full bg-white/10"
                role="progressbar"
                aria-label="Postęp do darmowej dostawy"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.round(shipping.progress * 100)}
              >
                <motion.div
                  className="h-full rounded-full bg-acid shadow-[0_0_16px_rgba(198,255,0,0.6)]"
                  initial={false}
                  animate={{ width: `${shipping.progress * 100}%` }}
                  transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
                />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-6" data-lenis-prevent>
              {items.length === 0 ? (
                <div className="flex h-full flex-col items-center justify-center gap-6 py-16 text-center">
                  <p className="h-display text-4xl text-ink-400">Pusto tu</p>
                  <p className="max-w-xs text-ink-300">Twój koszyk czeka na pierwszy drop. Sprawdź nowości w sklepie.</p>
                  <TransitionLink href="/sklep" onClick={close} className="btn-primary">
                    Przejdź do sklepu
                  </TransitionLink>
                </div>
              ) : (
                <ul className="divide-y divide-white/10">
                  <AnimatePresence initial={false}>
                    {items.map((item) => (
                      <motion.li
                        key={item.key}
                        layout
                        initial={{ opacity: 0, x: 40 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: 80, height: 0, paddingTop: 0, paddingBottom: 0 }}
                        transition={{ duration: 0.45 }}
                        className="flex gap-4 overflow-hidden py-5"
                      >
                        <TransitionLink href={`/produkt/${item.slug}`} onClick={close} className="relative h-28 w-24 shrink-0 overflow-hidden rounded-lg bg-ink-800">
                          <Image src={item.image} alt={item.name} fill sizes="96px" className="object-cover" />
                        </TransitionLink>
                        <div className="flex min-w-0 flex-1 flex-col">
                          <div className="flex items-start justify-between gap-3">
                            <div className="min-w-0">
                              <p className="truncate font-semibold">{item.name}</p>
                              <p className="mt-1 flex items-center gap-2 text-sm text-ink-300">
                                <span className="inline-block h-3 w-3 rounded-full border border-white/30" style={{ background: item.color.hex }} aria-hidden />
                                {item.color.name} · {item.size}
                              </p>
                            </div>
                            <p className="shrink-0 tabular-nums">{formatPrice(item.price * item.quantity)}</p>
                          </div>
                          <div className="mt-auto flex items-center justify-between pt-3">
                            <div className="flex items-center rounded-full border border-white/15" role="group" aria-label={`Ilość: ${item.name}`}>
                              <button
                                type="button"
                                className="grid h-9 w-9 place-items-center rounded-full hover:text-acid"
                                onClick={() => setQuantity(item.key, item.quantity - 1)}
                                aria-label="Zmniejsz ilość"
                              >
                                <MinusIcon width={16} height={16} />
                              </button>
                              <span className="w-7 text-center text-sm tabular-nums" aria-live="polite">
                                {item.quantity}
                              </span>
                              <button
                                type="button"
                                className="grid h-9 w-9 place-items-center rounded-full hover:text-acid disabled:opacity-30"
                                onClick={() => setQuantity(item.key, item.quantity + 1)}
                                disabled={item.quantity >= MAX_QTY}
                                aria-label="Zwiększ ilość"
                              >
                                <PlusIcon width={16} height={16} />
                              </button>
                            </div>
                            <button type="button" onClick={() => remove(item.key)} className="text-xs uppercase tracking-[0.16em] text-ink-300 underline-offset-4 hover:text-white hover:underline">
                              Usuń
                            </button>
                          </div>
                        </div>
                      </motion.li>
                    ))}
                  </AnimatePresence>
                </ul>
              )}
            </div>

            {items.length > 0 && (
              <footer className="space-y-4 border-t border-white/10 px-6 py-6">
                <div className="flex items-center justify-between text-sm text-ink-300">
                  <span>
                    {count} {plural(count, 'produkt', 'produkty', 'produktów')}
                  </span>
                  <span>Dostawa: {shipping.reached ? 'gratis' : 'od 13,99 zł'}</span>
                </div>
                <div className="flex items-end justify-between">
                  <span className="text-sm uppercase tracking-[0.16em] text-ink-200">Suma</span>
                  <span className="h-display text-3xl tabular-nums">{formatPrice(subtotal)}</span>
                </div>
                <TransitionLink href="/kasa" onClick={close} className="btn-primary w-full">
                  Przejdź do kasy <ArrowIcon width={18} height={18} />
                </TransitionLink>
                <p className="text-center text-xs text-ink-400">BLIK · Przelew · Karta · 30 dni na zwrot</p>
              </footer>
            )}
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
}
