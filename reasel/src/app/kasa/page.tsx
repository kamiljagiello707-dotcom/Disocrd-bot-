import type { Metadata } from 'next';
import { PageHeader } from '@/components/ui/PageHeader';
import { CheckoutForm } from '@/components/checkout/CheckoutForm';

export const metadata: Metadata = {
  title: 'Kasa',
  description: 'Bezpieczne zamówienie w sklepie Reasel.pl – InPost, DPD, BLIK, przelew, karta.',
  robots: { index: false, follow: false },
};

export default function CheckoutPage() {
  return (
    <div className="relative z-10 min-h-screen bg-ink">
      <PageHeader eyebrow="Krok 1 z 2 · Dane i płatność" title="Kasa" />
      <CheckoutForm />
    </div>
  );
}
