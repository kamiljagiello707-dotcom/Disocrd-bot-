import type { Metadata, Viewport } from 'next';
import './globals.css';
import { siteConfig } from '@/config/site';
import { Providers } from '@/components/layout/Providers';
import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { MobileMenu } from '@/components/layout/MobileMenu';
import { CartDrawer } from '@/components/layout/CartDrawer';
import { SearchOverlay } from '@/components/layout/SearchOverlay';
import { CookieBanner } from '@/components/layout/CookieBanner';
import { FlyToCart } from '@/components/layout/FlyToCart';
import { Preloader } from '@/components/layout/Preloader';
import { Curtain } from '@/components/ui/Curtain';
import { Cursor } from '@/components/ui/Cursor';
import { GlobalScene } from '@/components/3d/GlobalScene';

export const metadata: Metadata = {
  metadataBase: new URL(siteConfig.url),
  title: {
    default: 'Reasel.pl — Premium streetwear | Bluzy, kurtki, t-shirty',
    template: '%s | Reasel.pl',
  },
  description: siteConfig.description,
  applicationName: siteConfig.name,
  keywords: ['streetwear', 'bluzy oversize', 'odzież premium', 'kurtki', 't-shirty', 'polska marka odzieżowa', 'Reasel'],
  authors: [{ name: 'Reasel' }],
  alternates: { canonical: '/' },
  openGraph: {
    type: 'website',
    locale: siteConfig.locale,
    url: siteConfig.url,
    siteName: siteConfig.name,
    title: 'Reasel.pl — Premium streetwear',
    description: siteConfig.description,
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Reasel.pl — Premium streetwear',
    description: siteConfig.description,
  },
  robots: { index: true, follow: true },
  formatDetection: { telephone: false },
};

export const viewport: Viewport = {
  themeColor: '#0a0a0a',
  colorScheme: 'dark',
  width: 'device-width',
  initialScale: 1,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pl">
      <body>
        <a
          href="#tresc"
          className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[200] focus:rounded-full focus:bg-acid focus:px-5 focus:py-3 focus:text-ink"
        >
          Przejdź do treści
        </a>
        <Providers>
          <GlobalScene />
          <Header />
          <MobileMenu />
          <main id="tresc" className="relative z-10">
            {children}
          </main>
          <Footer />
          <CartDrawer />
          <SearchOverlay />
          <FlyToCart />
          <CookieBanner />
          <Curtain />
          <Preloader />
          <Cursor />
          <div className="grain" aria-hidden />
        </Providers>
      </body>
    </html>
  );
}
