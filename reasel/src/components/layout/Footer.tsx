import { TransitionLink } from '@/components/ui/TransitionLink';
import { Marquee } from '@/components/ui/Marquee';
import { siteConfig } from '@/config/site';
import { categories } from '@/lib/catalog';

const help = [
  { href: '/kontakt', label: 'Kontakt' },
  { href: '/zwroty', label: 'Zwroty i reklamacje' },
  { href: '/regulamin', label: 'Regulamin' },
  { href: '/polityka-prywatnosci', label: 'Polityka prywatności' },
];

export function Footer() {
  return (
    <footer className="relative z-10 overflow-hidden border-t border-white/10 bg-ink">
      <Marquee
        items={['Reasel', 'Streetwear', 'Made in Poland', 'Limitowane dropy']}
        className="border-b border-white/10 py-6 text-5xl md:text-7xl"
        outline
        speed={40}
      />

      <div className="container-x grid gap-12 py-16 md:grid-cols-12">
        <div className="md:col-span-5">
          <p className="h-display text-3xl leading-tight md:text-4xl">
            Ubrania dla tych,
            <br /> którzy <span className="text-acid">nie czekają.</span>
          </p>
          <address className="mt-6 space-y-1 text-sm not-italic text-ink-300">
            <p>{siteConfig.address}</p>
            <p>
              <a href={`mailto:${siteConfig.email}`} className="link-underline hover:text-white">
                {siteConfig.email}
              </a>
            </p>
            <p>
              <a href={`tel:${siteConfig.phone.replace(/\s/g, '')}`} className="link-underline hover:text-white">
                {siteConfig.phone}
              </a>
            </p>
          </address>
        </div>

        <nav aria-label="Kategorie" className="md:col-span-2">
          <h2 className="eyebrow mb-4">Sklep</h2>
          <ul className="space-y-2.5">
            {categories.map((c) => (
              <li key={c.slug}>
                <TransitionLink href={`/sklep?kategoria=${c.slug}`} className="link-underline text-ink-100 hover:text-white">
                  {c.label}
                </TransitionLink>
              </li>
            ))}
          </ul>
        </nav>

        <nav aria-label="Pomoc" className="md:col-span-2">
          <h2 className="eyebrow mb-4">Pomoc</h2>
          <ul className="space-y-2.5">
            {help.map((l) => (
              <li key={l.href}>
                <TransitionLink href={l.href} className="link-underline text-ink-100 hover:text-white">
                  {l.label}
                </TransitionLink>
              </li>
            ))}
          </ul>
        </nav>

        <div className="md:col-span-3">
          <h2 className="eyebrow mb-4">Social</h2>
          <ul className="space-y-2.5">
            <li>
              <a href={siteConfig.social.instagram} target="_blank" rel="noopener noreferrer" className="link-underline text-ink-100 hover:text-white">
                Instagram ↗
              </a>
            </li>
            <li>
              <a href={siteConfig.social.tiktok} target="_blank" rel="noopener noreferrer" className="link-underline text-ink-100 hover:text-white">
                TikTok ↗
              </a>
            </li>
            <li>
              <TransitionLink href="/o-nas" className="link-underline text-ink-100 hover:text-white">
                O marce
              </TransitionLink>
            </li>
          </ul>
          <p className="mt-8 text-xs text-ink-400">Płatności: BLIK · Przelew · Visa · Mastercard</p>
          <p className="mt-1 text-xs text-ink-400">Dostawa: InPost Paczkomat · Kurier DPD</p>
        </div>
      </div>

      <div className="container-x flex flex-col items-start justify-between gap-2 border-t border-white/10 py-6 text-xs text-ink-400 md:flex-row md:items-center">
        <p>© {new Date().getFullYear()} Reasel.pl — Wszelkie prawa zastrzeżone.</p>
        <p>Projekt i realizacja: Reasel Studio · Ceny zawierają VAT</p>
      </div>

      {/* Dekoracyjny znak wodny jako SVG (nie jest treścią – pomijany w audycie kontrastu) */}
      <svg aria-hidden viewBox="0 0 1000 190" className="pointer-events-none block w-full select-none" preserveAspectRatio="xMidYMax meet">
        <text x="500" y="182" textAnchor="middle" className="h-display" fontSize="240" fill="rgba(255,255,255,0.04)" letterSpacing="-6">
          REASEL
        </text>
      </svg>
    </footer>
  );
}
