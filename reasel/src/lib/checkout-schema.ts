import { z } from 'zod';

/** Algorytm Luhna – walidacja numeru karty. */
export function luhn(num: string): boolean {
  const digits = num.replace(/\D/g, '');
  if (digits.length < 13 || digits.length > 19) return false;
  let sum = 0;
  let double = false;
  for (let i = digits.length - 1; i >= 0; i--) {
    let d = Number(digits[i]);
    if (double) {
      d *= 2;
      if (d > 9) d -= 9;
    }
    sum += d;
    double = !double;
  }
  return sum % 10 === 0;
}

function expiryValid(value: string): boolean {
  const m = /^(\d{2})\s?\/\s?(\d{2})$/.exec(value);
  if (!m) return false;
  const month = Number(m[1]);
  const year = 2000 + Number(m[2]);
  if (month < 1 || month > 12) return false;
  const now = new Date();
  return year > now.getFullYear() || (year === now.getFullYear() && month >= now.getMonth() + 1);
}

const required = (msg: string) => z.string().trim().min(1, msg);

/**
 * Schemat formularza zamówienia. Pola zależne (Paczkomat, BLIK, karta)
 * są walidowane w superRefine w zależności od wybranej dostawy / płatności.
 */
export const checkoutSchema = z
  .object({
    email: required('Podaj adres e-mail').email('Niepoprawny adres e-mail'),
    phone: required('Podaj numer telefonu').regex(/^(\+48\s?)?\d{3}[\s-]?\d{3}[\s-]?\d{3}$/, 'Podaj numer w formacie 500 600 700'),
    firstName: required('Podaj imię').min(2, 'Imię jest za krótkie'),
    lastName: required('Podaj nazwisko').min(2, 'Nazwisko jest za krótkie'),
    street: required('Podaj ulicę i numer').min(3, 'Adres jest za krótki'),
    postalCode: required('Podaj kod pocztowy').regex(/^\d{2}-\d{3}$/, 'Kod pocztowy w formacie 00-000'),
    city: required('Podaj miejscowość'),
    shipping: z.enum(['inpost', 'dpd'], { errorMap: () => ({ message: 'Wybierz sposób dostawy' }) }),
    paczkomat: z.string().trim().optional(),
    payment: z.enum(['blik', 'przelew', 'karta'], { errorMap: () => ({ message: 'Wybierz metodę płatności' }) }),
    blikCode: z.string().trim().optional(),
    cardNumber: z.string().trim().optional(),
    cardExpiry: z.string().trim().optional(),
    cardCvc: z.string().trim().optional(),
    notes: z.string().max(500, 'Maksymalnie 500 znaków').optional(),
    terms: z.literal(true, { errorMap: () => ({ message: 'Musisz zaakceptować regulamin' }) }),
    newsletter: z.boolean().optional(),
  })
  .superRefine((d, ctx) => {
    if (d.shipping === 'inpost' && !/^[A-Z]{3}\d{1,4}[A-Z]{0,3}$/i.test(d.paczkomat ?? '')) {
      ctx.addIssue({ code: 'custom', path: ['paczkomat'], message: 'Podaj kod Paczkomatu, np. WAW01A' });
    }
    if (d.payment === 'blik' && !/^\d{3}\s?\d{3}$/.test(d.blikCode ?? '')) {
      ctx.addIssue({ code: 'custom', path: ['blikCode'], message: 'Kod BLIK ma 6 cyfr' });
    }
    if (d.payment === 'karta') {
      if (!luhn(d.cardNumber ?? '')) ctx.addIssue({ code: 'custom', path: ['cardNumber'], message: 'Niepoprawny numer karty' });
      if (!expiryValid(d.cardExpiry ?? '')) ctx.addIssue({ code: 'custom', path: ['cardExpiry'], message: 'Data w formacie MM/RR' });
      if (!/^\d{3,4}$/.test(d.cardCvc ?? '')) ctx.addIssue({ code: 'custom', path: ['cardCvc'], message: 'CVC ma 3–4 cyfry' });
    }
  });

export type CheckoutValues = z.infer<typeof checkoutSchema>;

export interface PlacedOrder {
  number: string;
  createdAt: string;
  email: string;
  name: string;
  address: string;
  shipping: { id: string; name: string; price: number; paczkomat?: string };
  payment: { id: string; name: string };
  items: { name: string; size: string; color: string; quantity: number; price: number; image: string }[];
  subtotal: number;
  total: number;
}

export const LAST_ORDER_KEY = 'reasel-last-order';
