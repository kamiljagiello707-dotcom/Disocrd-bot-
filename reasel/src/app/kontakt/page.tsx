import type { Metadata } from 'next';
import { PageHeader } from '@/components/ui/PageHeader';
import { ContactForm } from '@/components/forms/ContactForm';
import { siteConfig } from '@/config/site';

export const metadata: Metadata = {
  title: 'Kontakt',
  description: 'Masz pytanie o produkt, zamówienie lub zwrot? Napisz do nas – odpowiadamy w 24h w dni robocze.',
  alternates: { canonical: '/kontakt' },
};

export default function ContactPage() {
  return (
    <div className="relative z-10 min-h-screen bg-ink">
      <PageHeader eyebrow="Kontakt" title="Napisz do nas" description="Odpowiadamy w ciągu 24 godzin w dni robocze. Najszybciej – przez formularz." />
      <div className="container-x grid gap-16 pb-32 lg:grid-cols-12">
        <div className="lg:col-span-7">
          <ContactForm />
        </div>
        <aside className="space-y-10 lg:col-span-4 lg:col-start-9">
          <div>
            <h2 className="eyebrow mb-3">Obsługa klienta</h2>
            <p>
              <a href={`mailto:${siteConfig.email}`} className="h-display text-2xl hover:text-acid">
                {siteConfig.email}
              </a>
            </p>
            <p className="mt-1">
              <a href={`tel:${siteConfig.phone.replace(/\s/g, '')}`} className="text-lg text-ink-200 hover:text-acid">
                {siteConfig.phone}
              </a>
            </p>
            <p className="mt-2 text-sm text-ink-400">Pon.–pt. 9:00–17:00</p>
          </div>
          <div>
            <h2 className="eyebrow mb-3">Showroom</h2>
            <address className="not-italic text-ink-200">{siteConfig.address}</address>
            <p className="mt-2 text-sm text-ink-400">Czw.–sob. 12:00–20:00</p>
          </div>
          <div>
            <h2 className="eyebrow mb-3">Dane firmy</h2>
            <p className="text-sm leading-relaxed text-ink-300">
              Reasel sp. z o.o.
              <br />
              NIP: 000-000-00-00 · REGON: 000000000
              <br />
              KRS: 0000000000 (dane przykładowe)
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}
