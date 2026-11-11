'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AnimatePresence, motion } from 'framer-motion';
import { Field } from '@/components/checkout/Field';
import { CheckIcon } from '@/components/ui/Icons';

const TOPICS = ['Pytanie o produkt', 'Status zamówienia', 'Zwrot lub reklamacja', 'Współpraca', 'Inne'] as const;

const schema = z.object({
  name: z.string().trim().min(2, 'Podaj imię'),
  email: z.string().trim().min(1, 'Podaj adres e-mail').email('Niepoprawny adres e-mail'),
  topic: z.enum(TOPICS, { errorMap: () => ({ message: 'Wybierz temat' }) }),
  orderNumber: z.string().trim().max(20).optional(),
  message: z.string().trim().min(10, 'Wiadomość musi mieć co najmniej 10 znaków').max(2000, 'Maksymalnie 2000 znaków'),
  consent: z.literal(true, { errorMap: () => ({ message: 'Zgoda jest wymagana' }) }),
});

type Values = z.infer<typeof schema>;

/** Formularz kontaktowy (mock – w produkcji: route handler + e-mail / helpdesk). */
export function ContactForm() {
  const [sent, setSent] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<Values>({ resolver: zodResolver(schema), mode: 'onTouched' });

  const onSubmit = async () => {
    await new Promise((r) => setTimeout(r, 1000));
    setSent(true);
    reset();
  };

  return (
    <AnimatePresence mode="wait">
      {sent ? (
        <motion.div key="sent" role="status" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="rounded-3xl border border-acid/40 bg-acid/5 p-10">
          <span className="grid h-14 w-14 place-items-center rounded-full bg-acid text-ink">
            <CheckIcon />
          </span>
          <p className="h-display mt-6 text-3xl">Wiadomość wysłana</p>
          <p className="mt-3 text-ink-200">Odpowiemy w ciągu 24 godzin w dni robocze.</p>
          <button type="button" onClick={() => setSent(false)} className="btn-ghost mt-8">
            Napisz kolejną
          </button>
        </motion.div>
      ) : (
        <motion.form key="form" exit={{ opacity: 0 }} onSubmit={handleSubmit(onSubmit)} noValidate className="grid gap-5 sm:grid-cols-2">
          <Field label="Imię" autoComplete="name" error={errors.name?.message} {...register('name')} />
          <Field label="E-mail" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
          <div>
            <label htmlFor="topic" className="label">
              Temat
            </label>
            <select id="topic" className="input" defaultValue="" aria-invalid={!!errors.topic} {...register('topic')}>
              <option value="" disabled>
                Wybierz…
              </option>
              {TOPICS.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
            {errors.topic && <p className="error-text">{errors.topic.message}</p>}
          </div>
          <Field label="Numer zamówienia (opcjonalnie)" placeholder="RSL-XXXXXX" error={errors.orderNumber?.message} {...register('orderNumber')} />
          <div className="sm:col-span-2">
            <label htmlFor="message" className="label">
              Wiadomość
            </label>
            <textarea id="message" rows={6} className="input resize-none" aria-invalid={!!errors.message} aria-describedby={errors.message ? 'message-error' : undefined} {...register('message')} />
            {errors.message && (
              <p id="message-error" className="error-text">
                {errors.message.message}
              </p>
            )}
          </div>
          <label className="flex items-start gap-3 text-sm text-ink-300 sm:col-span-2">
            <input type="checkbox" className="mt-0.5 h-4 w-4 shrink-0 accent-[#c6ff00]" aria-invalid={!!errors.consent} {...register('consent')} />
            <span>Wyrażam zgodę na przetwarzanie moich danych w celu udzielenia odpowiedzi na wiadomość (art. 6 ust. 1 lit. a RODO).</span>
          </label>
          {errors.consent && <p className="error-text sm:col-span-2">{errors.consent.message}</p>}
          <div className="sm:col-span-2">
            <button type="submit" className="btn-primary" disabled={isSubmitting}>
              {isSubmitting ? 'Wysyłanie…' : 'Wyślij wiadomość'}
            </button>
          </div>
        </motion.form>
      )}
    </AnimatePresence>
  );
}
