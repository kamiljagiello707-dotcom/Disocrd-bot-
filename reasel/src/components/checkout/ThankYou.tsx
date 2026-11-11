'use client';

import { useEffect, useState } from 'react';
import Image from 'next/image';
import { motion } from 'framer-motion';
import { LAST_ORDER_KEY, type PlacedOrder } from '@/lib/checkout-schema';
import { formatPrice } from '@/lib/format';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { SplitText } from '@/components/ui/SplitText';

/** Strona podziękowania – czyta ostatnie zamówienie z sessionStorage (mock). */
export function ThankYou() {
  const [order, setOrder] = useState<PlacedOrder | null | undefined>(undefined);

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem(LAST_ORDER_KEY);
      setOrder(raw ? (JSON.parse(raw) as PlacedOrder) : null);
    } catch {
      setOrder(null);
    }
  }, []);

  if (order === undefined) return <div className="h-screen" aria-busy="true" />;

  if (order === null) {
    return (
      <div className="container-x flex min-h-screen flex-col items-center justify-center text-center">
        <p className="h-display text-4xl">Nie znaleziono zamówienia</p>
        <p className="mt-4 text-ink-300">Wygląda na to, że ta sesja nie ma aktywnego zamówienia.</p>
        <TransitionLink href="/sklep" className="btn-primary mt-8">
          Wróć do sklepu
        </TransitionLink>
      </div>
    );
  }

  return (
    <div className="container-x grid gap-12 pb-32 pt-[calc(var(--header-h)+4rem)] lg:grid-cols-12">
      <div className="lg:col-span-7">
        <motion.svg
          width="96"
          height="96"
          viewBox="0 0 96 96"
          aria-hidden
          initial={{ scale: 0.6, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ delay: 0.6, type: 'spring', stiffness: 200, damping: 14 }}
        >
          <circle cx="48" cy="48" r="46" fill="#c6ff00" />
          <motion.path
            d="M28 49 L42 63 L69 34"
            fill="none"
            stroke="#0a0a0a"
            strokeWidth="7"
            strokeLinecap="round"
            strokeLinejoin="round"
            initial={{ pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ delay: 1, duration: 0.6 }}
          />
        </motion.svg>
        <p className="eyebrow mt-8">Zamówienie {order.number}</p>
        <SplitText as="h1" immediate delay={0.6} text="Dziękujemy!" className="h-display mt-3 text-display-md" />
        <p className="mt-6 max-w-lg text-lg text-ink-200">
          Potwierdzenie wysłaliśmy na <strong className="text-white">{order.email}</strong>. Damy znać, gdy paczka wyruszy w
          drogę.
        </p>

        {order.payment.id === 'przelew' && (
          <div className="mt-8 max-w-lg rounded-2xl border border-acid/40 bg-acid/5 p-6 text-sm">
            <p className="font-semibold text-acid">Dane do przelewu (demo)</p>
            <dl className="mt-3 grid grid-cols-[auto,1fr] gap-x-6 gap-y-1.5 text-ink-200">
              <dt>Odbiorca</dt>
              <dd>Reasel sp. z o.o.</dd>
              <dt>Konto</dt>
              <dd className="tabular-nums">00 0000 0000 0000 0000 0000 0000</dd>
              <dt>Tytuł</dt>
              <dd>{order.number}</dd>
              <dt>Kwota</dt>
              <dd>{formatPrice(order.total)}</dd>
            </dl>
          </div>
        )}

        <div className="mt-10 flex flex-wrap gap-3">
          <TransitionLink href="/sklep" className="btn-primary">
            Kontynuuj zakupy
          </TransitionLink>
          <TransitionLink href="/" className="btn-ghost">
            Strona główna
          </TransitionLink>
        </div>
      </div>

      <aside className="rounded-3xl border border-white/10 bg-ink-900/80 p-6 backdrop-blur md:p-8 lg:col-span-5" aria-label="Szczegóły zamówienia">
        <h2 className="h-display mb-6 text-2xl">Szczegóły</h2>
        <ul className="space-y-4">
          {order.items.map((i, idx) => (
            <li key={idx} className="flex gap-4 text-sm">
              <span className="relative h-20 w-16 shrink-0 overflow-hidden rounded-lg bg-ink-800">
                <Image src={i.image} alt={i.name} fill sizes="64px" className="object-cover" />
              </span>
              <span className="flex-1">
                <span className="block font-semibold">{i.name}</span>
                <span className="text-ink-400">
                  {i.color} · {i.size} · {i.quantity} szt.
                </span>
              </span>
              <span className="tabular-nums">{formatPrice(i.price * i.quantity)}</span>
            </li>
          ))}
        </ul>
        <dl className="mt-6 space-y-2 border-t border-white/10 pt-6 text-sm">
          <div className="flex justify-between">
            <dt className="text-ink-300">Dostawa</dt>
            <dd>
              {order.shipping.name}
              {order.shipping.paczkomat && ` (${order.shipping.paczkomat})`}
            </dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-ink-300">Adres</dt>
            <dd className="text-right">
              {order.name}
              <br />
              {order.address}
            </dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-ink-300">Płatność</dt>
            <dd>{order.payment.name}</dd>
          </div>
          <div className="flex items-end justify-between border-t border-white/10 pt-4">
            <dt className="uppercase tracking-[0.14em]">Razem</dt>
            <dd className="h-display text-3xl tabular-nums">{formatPrice(order.total)}</dd>
          </div>
        </dl>
      </aside>
    </div>
  );
}
