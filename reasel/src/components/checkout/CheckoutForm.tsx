'use client';

import { useState } from 'react';
import Image from 'next/image';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AnimatePresence, motion } from 'framer-motion';
import { checkoutSchema, LAST_ORDER_KEY, type CheckoutValues, type PlacedOrder } from '@/lib/checkout-schema';
import { paymentMethods, shippingMethods, siteConfig } from '@/config/site';
import { freeShippingProgress, selectSubtotal, useCart } from '@/store/cart';
import { useTransition } from '@/store/transition';
import { useHydrated } from '@/hooks/useHydrated';
import { cn, formatPrice } from '@/lib/format';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { Field } from './Field';

function Section({ no, title, children }: { no: string; title: string; children: React.ReactNode }) {
  return (
    <fieldset className="rounded-3xl border border-white/10 bg-ink-900/60 p-6 md:p-8">
      <legend className="sr-only">{title}</legend>
      <div className="mb-6 flex items-center gap-4" aria-hidden>
        <span className="grid h-9 w-9 place-items-center rounded-full bg-acid text-sm font-bold text-ink">{no}</span>
        <span className="h-display text-xl">{title}</span>
      </div>
      {children}
    </fieldset>
  );
}

/** Formularz zamówienia (mock płatności) z walidacją react-hook-form + zod. */
export function CheckoutForm() {
  const hydrated = useHydrated();
  const items = useCart((s) => s.items);
  const subtotal = useCart(selectSubtotal);
  const clear = useCart((s) => s.clear);
  const startTransition = useTransition((s) => s.start);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CheckoutValues>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: { shipping: 'inpost', payment: 'blik', newsletter: false },
    mode: 'onTouched',
  });

  const shippingId = watch('shipping');
  const paymentId = watch('payment');
  const shipping = shippingMethods.find((s) => s.id === shippingId) ?? shippingMethods[0];
  const free = freeShippingProgress(subtotal).reached;
  const shippingCost = free ? 0 : shipping.price;
  const total = subtotal + shippingCost;

  const onSubmit = async (values: CheckoutValues) => {
    setSubmitError(null);
    try {
      // MOCK: tutaj trafi integracja z bramką płatności (np. Stripe / Przelewy24 / PayU)
      await new Promise((r) => setTimeout(r, 1600));
      const order: PlacedOrder = {
        number: `RSL-${Date.now().toString(36).toUpperCase().slice(-6)}`,
        createdAt: new Date().toISOString(),
        email: values.email,
        name: `${values.firstName} ${values.lastName}`,
        address: `${values.street}, ${values.postalCode} ${values.city}`,
        shipping: { id: shipping.id, name: shipping.name, price: shippingCost, paczkomat: values.paczkomat?.toUpperCase() },
        payment: { id: values.payment, name: paymentMethods.find((p) => p.id === values.payment)?.name ?? values.payment },
        items: items.map((i) => ({ name: i.name, size: i.size, color: i.color.name, quantity: i.quantity, price: i.price, image: i.image })),
        subtotal,
        total,
      };
      sessionStorage.setItem(LAST_ORDER_KEY, JSON.stringify(order));
      startTransition('/kasa/dziekujemy');
      // Koszyk czyścimy po rozpoczęciu przejścia, by podsumowanie nie „mignęło” pustką
      window.setTimeout(clear, 800);
    } catch {
      setSubmitError('Nie udało się złożyć zamówienia. Spróbuj ponownie.');
    }
  };

  if (!hydrated) {
    return <div className="container-x h-[50vh]" aria-busy="true" />;
  }

  if (items.length === 0 && !isSubmitting) {
    return (
      <div className="container-x pb-32 text-center">
        <p className="h-display text-4xl text-ink-400">Koszyk jest pusty</p>
        <p className="mt-4 text-ink-300">Dodaj produkty, aby złożyć zamówienie.</p>
        <TransitionLink href="/sklep" className="btn-primary mt-8">
          Przejdź do sklepu
        </TransitionLink>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="container-x grid gap-10 pb-32 lg:grid-cols-12">
      <div className="space-y-6 lg:col-span-7">
        <Section no="1" title="Kontakt">
          <div className="grid gap-5 sm:grid-cols-2">
            <Field label="E-mail" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
            <Field label="Telefon" type="tel" autoComplete="tel" placeholder="500 600 700" error={errors.phone?.message} {...register('phone')} />
          </div>
        </Section>

        <Section no="2" title="Dane i adres">
          <div className="grid gap-5 sm:grid-cols-2">
            <Field label="Imię" autoComplete="given-name" error={errors.firstName?.message} {...register('firstName')} />
            <Field label="Nazwisko" autoComplete="family-name" error={errors.lastName?.message} {...register('lastName')} />
            <Field label="Ulica i numer" autoComplete="street-address" className="sm:col-span-2" error={errors.street?.message} {...register('street')} />
            <Field label="Kod pocztowy" autoComplete="postal-code" inputMode="numeric" placeholder="00-000" error={errors.postalCode?.message} {...register('postalCode')} />
            <Field label="Miejscowość" autoComplete="address-level2" error={errors.city?.message} {...register('city')} />
          </div>
        </Section>

        <Section no="3" title="Dostawa">
          <div className="grid gap-3" role="radiogroup" aria-label="Sposób dostawy">
            {shippingMethods.map((m) => (
              <label
                key={m.id}
                className={cn(
                  'flex cursor-pointer items-center gap-4 rounded-2xl border p-5 transition-colors',
                  shippingId === m.id ? 'border-acid bg-acid/5' : 'border-white/10 hover:border-white/30',
                )}
              >
                <input type="radio" value={m.id} className="h-4 w-4 accent-[#c6ff00]" {...register('shipping')} />
                <span className="flex-1">
                  <span className="block font-semibold">{m.name}</span>
                  <span className="block text-sm text-ink-300">{m.description}</span>
                </span>
                <span className="tabular-nums">{free ? <span className="text-acid">0,00 zł</span> : formatPrice(m.price)}</span>
              </label>
            ))}
          </div>
          <AnimatePresence initial={false}>
            {shippingId === 'inpost' && (
              <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden">
                <Field
                  className="pt-5"
                  label="Kod Paczkomatu"
                  placeholder="np. WAW01A"
                  hint="Kod znajdziesz na mapie Paczkomatów InPost (integracja z widgetem – do podpięcia)."
                  autoCapitalize="characters"
                  error={errors.paczkomat?.message}
                  {...register('paczkomat')}
                />
              </motion.div>
            )}
          </AnimatePresence>
        </Section>

        <Section no="4" title="Płatność">
          <div className="grid gap-3 sm:grid-cols-3" role="radiogroup" aria-label="Metoda płatności">
            {paymentMethods.map((m) => (
              <label
                key={m.id}
                className={cn(
                  'flex cursor-pointer flex-col gap-1 rounded-2xl border p-5 transition-colors',
                  paymentId === m.id ? 'border-acid bg-acid/5' : 'border-white/10 hover:border-white/30',
                )}
              >
                <span className="flex items-center gap-3">
                  <input type="radio" value={m.id} className="h-4 w-4 accent-[#c6ff00]" {...register('payment')} />
                  <span className="font-semibold">{m.name}</span>
                </span>
                <span className="text-xs text-ink-300">{m.description}</span>
              </label>
            ))}
          </div>

          <AnimatePresence mode="wait" initial={false}>
            <motion.div key={paymentId} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }} className="pt-5">
              {paymentId === 'blik' && (
                <Field
                  label="Kod BLIK"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={7}
                  placeholder="000 000"
                  hint="Wersja demonstracyjna – wpisz dowolne 6 cyfr."
                  error={errors.blikCode?.message}
                  className="max-w-xs"
                  {...register('blikCode')}
                />
              )}
              {paymentId === 'przelew' && (
                <p className="rounded-2xl border border-white/10 p-5 text-sm text-ink-200">
                  Dane do przelewu otrzymasz na stronie podsumowania i w e-mailu. Zamówienie wyślemy po zaksięgowaniu wpłaty (zwykle 1 dzień roboczy).
                </p>
              )}
              {paymentId === 'karta' && (
                <div className="grid gap-5 sm:grid-cols-4">
                  <Field
                    label="Numer karty"
                    inputMode="numeric"
                    autoComplete="cc-number"
                    placeholder="4242 4242 4242 4242"
                    className="sm:col-span-2"
                    hint="Tryb testowy – użyj np. 4242 4242 4242 4242."
                    error={errors.cardNumber?.message}
                    {...register('cardNumber')}
                  />
                  <Field label="Ważność" autoComplete="cc-exp" placeholder="MM/RR" error={errors.cardExpiry?.message} {...register('cardExpiry')} />
                  <Field label="CVC" inputMode="numeric" autoComplete="cc-csc" placeholder="123" maxLength={4} error={errors.cardCvc?.message} {...register('cardCvc')} />
                </div>
              )}
            </motion.div>
          </AnimatePresence>
        </Section>

        <Section no="5" title="Podsumowanie">
          <label htmlFor="notes" className="label">
            Uwagi do zamówienia (opcjonalnie)
          </label>
          <textarea id="notes" rows={3} className="input resize-none" aria-invalid={!!errors.notes} {...register('notes')} />
          {errors.notes && <p className="error-text">{errors.notes.message}</p>}

          <div className="mt-6 space-y-3 text-sm text-ink-200">
            <label className="flex items-start gap-3">
              <input type="checkbox" className="mt-0.5 h-4 w-4 shrink-0 accent-[#c6ff00]" aria-invalid={!!errors.terms} aria-describedby={errors.terms ? 'terms-error' : undefined} {...register('terms')} />
              <span>
                Akceptuję{' '}
                <TransitionLink href="/regulamin" className="text-acid underline underline-offset-4">
                  regulamin
                </TransitionLink>{' '}
                i{' '}
                <TransitionLink href="/polityka-prywatnosci" className="text-acid underline underline-offset-4">
                  politykę prywatności
                </TransitionLink>{' '}
                sklepu. <span className="text-red-300">*</span>
              </span>
            </label>
            {errors.terms && (
              <p id="terms-error" className="error-text" role="alert">
                {errors.terms.message}
              </p>
            )}
            <label className="flex items-start gap-3">
              <input type="checkbox" className="mt-0.5 h-4 w-4 shrink-0 accent-[#c6ff00]" {...register('newsletter')} />
              <span>Chcę dołączyć do Reasel Club i otrzymywać informacje o dropach.</span>
            </label>
          </div>
        </Section>
      </div>

      {/* Podsumowanie koszyka */}
      <aside className="lg:col-span-5" aria-label="Podsumowanie zamówienia">
        <div className="rounded-3xl border border-white/10 bg-ink-900/80 p-6 backdrop-blur md:p-8 lg:sticky lg:top-28">
          <h2 className="h-display mb-6 text-2xl">Twoje zamówienie</h2>
          <ul className="max-h-[40vh] space-y-4 overflow-y-auto pr-1" data-lenis-prevent>
            {items.map((i) => (
              <li key={i.key} className="flex gap-4">
                <span className="relative h-20 w-16 shrink-0 overflow-hidden rounded-lg bg-ink-800">
                  <Image src={i.image} alt={i.name} fill sizes="64px" className="object-cover" />
                  <span className="absolute -right-0 -top-0 grid h-5 min-w-5 place-items-center rounded-bl-lg bg-acid px-1 text-[10px] font-bold text-ink">{i.quantity}</span>
                </span>
                <span className="flex-1 text-sm">
                  <span className="block font-semibold">{i.name}</span>
                  <span className="text-ink-400">
                    {i.color.name} · {i.size}
                  </span>
                </span>
                <span className="text-sm tabular-nums">{formatPrice(i.price * i.quantity)}</span>
              </li>
            ))}
          </ul>
          <dl className="mt-6 space-y-2 border-t border-white/10 pt-6 text-sm">
            <div className="flex justify-between">
              <dt className="text-ink-300">Produkty</dt>
              <dd className="tabular-nums">{formatPrice(subtotal)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-ink-300">Dostawa ({shipping.name})</dt>
              <dd className="tabular-nums">{shippingCost === 0 ? <span className="text-acid">Gratis</span> : formatPrice(shippingCost)}</dd>
            </div>
            {!free && (
              <p className="text-xs text-ink-400">
                Dodaj produkty za {formatPrice(siteConfig.freeShippingThreshold - subtotal)}, aby dostać darmową dostawę.
              </p>
            )}
            <div className="flex items-end justify-between border-t border-white/10 pt-4">
              <dt className="uppercase tracking-[0.14em]">Do zapłaty</dt>
              <dd className="h-display text-3xl tabular-nums">{formatPrice(total)}</dd>
            </div>
            <p className="text-xs text-ink-400">Cena zawiera VAT.</p>
          </dl>

          <button type="submit" className="btn-primary mt-6 w-full" disabled={isSubmitting}>
            {isSubmitting ? (
              <span className="flex items-center gap-3">
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-ink border-t-transparent" aria-hidden />
                Przetwarzanie płatności…
              </span>
            ) : (
              `Zamawiam i płacę · ${formatPrice(total)}`
            )}
          </button>
          {Object.keys(errors).length > 0 && (
            <p className="error-text mt-3 text-center" role="alert">
              Popraw zaznaczone pola formularza.
            </p>
          )}
          {submitError && (
            <p className="error-text mt-3 text-center" role="alert">
              {submitError}
            </p>
          )}
          <p className="mt-4 text-center text-xs text-ink-400">🔒 Wersja demonstracyjna – żadna płatność nie zostanie pobrana.</p>
        </div>
      </aside>
    </form>
  );
}
