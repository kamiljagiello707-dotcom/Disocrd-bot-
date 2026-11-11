'use client';

import { useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AnimatePresence, motion } from 'framer-motion';
import { SplitText } from '@/components/ui/SplitText';
import { Magnetic } from '@/components/ui/Magnetic';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { ArrowIcon, CheckIcon } from '@/components/ui/Icons';
import { useStorySection } from '@/hooks/useStorySection';

const schema = z.object({
  email: z.string().trim().min(1, 'Podaj adres e-mail').email('Podaj poprawny adres e-mail'),
  consent: z.literal(true, { errorMap: () => ({ message: 'Zgoda jest wymagana do zapisu' }) }),
});

type FormValues = z.infer<typeof schema>;

/** Newsletter – zapis do „klubu” z walidacją (mock – bez wysyłki do API). */
export function Newsletter() {
  const ref = useRef<HTMLElement>(null);
  const [done, setDone] = useState(false);
  useStorySection(ref, 5);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async () => {
    // Miejsce na integrację: np. fetch('/api/newsletter') → Mailchimp / Klaviyo / GetResponse
    await new Promise((r) => setTimeout(r, 900));
    setDone(true);
  };

  return (
    <section ref={ref} className="relative flex min-h-[100svh] items-center py-24" aria-labelledby="newsletter-title">
      <div className="container-x grid gap-12 lg:grid-cols-2 lg:items-end">
        <div>
          <p className="eyebrow mb-4">Reasel Club</p>
          <SplitText id="newsletter-title" text="Bądź pierwszy przy dropie" className="h-display text-display-md" />
          <p className="mt-6 max-w-md text-ink-200">
            Dropy wyprzedają się w godziny. Członkowie klubu dostają dostęp 24h wcześniej i −10% na pierwsze zamówienie.
          </p>
        </div>

        <div className="rounded-3xl border border-white/10 bg-ink/60 p-6 backdrop-blur-xl md:p-10">
          <AnimatePresence mode="wait">
            {done ? (
              <motion.div key="ok" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="flex items-center gap-4" role="status">
                <span className="grid h-14 w-14 shrink-0 place-items-center rounded-full bg-acid text-ink">
                  <CheckIcon />
                </span>
                <div>
                  <p className="h-display text-2xl">Jesteś w klubie!</p>
                  <p className="text-ink-300">Sprawdź skrzynkę – czeka tam kod −10%.</p>
                </div>
              </motion.div>
            ) : (
              <motion.form key="form" exit={{ opacity: 0, y: -10 }} onSubmit={handleSubmit(onSubmit)} noValidate>
                <label htmlFor="nl-email" className="label">
                  Adres e-mail
                </label>
                <div className="flex flex-col gap-3 sm:flex-row">
                  <input
                    id="nl-email"
                    type="email"
                    autoComplete="email"
                    placeholder="ty@przyklad.pl"
                    className="input flex-1"
                    aria-invalid={!!errors.email}
                    aria-describedby={errors.email ? 'nl-email-error' : undefined}
                    {...register('email')}
                  />
                  <Magnetic className="block">
                    <button type="submit" className="btn-primary w-full sm:w-auto" disabled={isSubmitting}>
                      {isSubmitting ? 'Zapisuję…' : 'Dołącz'} <ArrowIcon width={18} height={18} />
                    </button>
                  </Magnetic>
                </div>
                {errors.email && (
                  <p id="nl-email-error" className="error-text">
                    {errors.email.message}
                  </p>
                )}
                <label className="mt-5 flex items-start gap-3 text-sm text-ink-300">
                  <input type="checkbox" className="mt-0.5 h-4 w-4 shrink-0 accent-[#c6ff00]" aria-invalid={!!errors.consent} {...register('consent')} />
                  <span>
                    Wyrażam zgodę na otrzymywanie informacji handlowych drogą elektroniczną. Administratorem danych jest
                    Reasel. Szczegóły w{' '}
                    <TransitionLink href="/polityka-prywatnosci" className="text-acid underline underline-offset-4">
                      polityce prywatności
                    </TransitionLink>
                    .
                  </span>
                </label>
                {errors.consent && <p className="error-text">{errors.consent.message}</p>}
              </motion.form>
            )}
          </AnimatePresence>
        </div>
      </div>
    </section>
  );
}
