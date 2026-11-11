import type { Metadata } from 'next';
import { ThankYou } from '@/components/checkout/ThankYou';

export const metadata: Metadata = {
  title: 'Dziękujemy za zamówienie',
  robots: { index: false, follow: false },
};

export default function ThankYouPage() {
  return (
    <div className="relative z-10 min-h-screen">
      <ThankYou />
    </div>
  );
}
