'use client';

import { useEffect, useState } from 'react';
import { motion, useAnimationControls } from 'framer-motion';
import { usePathname } from 'next/navigation';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { Magnetic } from '@/components/ui/Magnetic';
import { BagIcon, HeartIcon, SearchIcon } from '@/components/ui/Icons';
import { mainNav } from '@/config/site';
import { selectCount, useCart } from '@/store/cart';
import { useFavorites } from '@/store/favorites';
import { useUI } from '@/store/ui';
import { cn } from '@/lib/format';

/**
 * Nagłówek: chowa się przy scrollu w dół, wraca przy scrollu w górę.
 * Ikona koszyka (id="cart-icon") jest celem animacji „lotu” produktu.
 */
export function Header() {
  const pathname = usePathname();
  const [hidden, setHidden] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const count = useCart(selectCount);
  const bump = useCart((s) => s.bump);
  const openCart = useCart((s) => s.open);
  const favCount = useFavorites((s) => s.ids.length);
  const { setSearchOpen, menuOpen, setMenuOpen } = useUI();
  const bagControls = useAnimationControls();

  useEffect(() => {
    let last = window.scrollY;
    const onScroll = () => {
      const y = window.scrollY;
      setScrolled(y > 40);
      if (Math.abs(y - last) > 6) {
        setHidden(y > last && y > 200);
        last = y;
      }
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    if (bump > 0) {
      setHidden(false);
      void bagControls.start({ scale: [1, 1.35, 0.9, 1], rotate: [0, -12, 8, 0], transition: { duration: 0.6 } });
    }
  }, [bump, bagControls]);

  // Skrót klawiszowy: Ctrl/Cmd + K lub „/” otwiera wyszukiwarkę
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const typing = ['INPUT', 'TEXTAREA', 'SELECT'].includes((e.target as HTMLElement).tagName);
      if ((e.key === 'k' && (e.metaKey || e.ctrlKey)) || (e.key === '/' && !typing)) {
        e.preventDefault();
        setSearchOpen(true);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [setSearchOpen]);

  return (
    <motion.header
      className={cn(
        'fixed inset-x-0 top-0 z-50 transition-[background-color,backdrop-filter] duration-500',
        scrolled && !menuOpen ? 'bg-ink/70 backdrop-blur-xl' : 'bg-transparent',
      )}
      animate={{ y: hidden && !menuOpen ? '-100%' : '0%' }}
      transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
    >
      <div className="container-x flex h-[var(--header-h)] items-center justify-between gap-6">
        <TransitionLink href="/" className="h-display text-2xl tracking-tight" aria-label="Reasel – strona główna">
          REASEL<span className="text-acid">.</span>
        </TransitionLink>

        <nav aria-label="Główna nawigacja" className="hidden lg:block">
          <ul className="flex items-center gap-9 text-[13px] font-medium uppercase tracking-[0.16em]">
            {mainNav.map((item) => {
              const active = item.href === pathname;
              return (
                <li key={item.href}>
                  <TransitionLink
                    href={item.href}
                    className={cn('link-underline py-2', active ? 'text-acid' : 'text-ink-100 hover:text-white')}
                    aria-current={active ? 'page' : undefined}
                  >
                    {item.label}
                  </TransitionLink>
                </li>
              );
            })}
          </ul>
        </nav>

        <div className="flex items-center gap-1 sm:gap-2">
          <Magnetic>
            <button
              type="button"
              onClick={() => setSearchOpen(true)}
              className="grid h-11 w-11 place-items-center rounded-full hover:text-acid"
              aria-label="Szukaj produktów (Ctrl+K)"
            >
              <SearchIcon />
            </button>
          </Magnetic>
          <Magnetic>
            <TransitionLink
              href="/ulubione"
              className="relative grid h-11 w-11 place-items-center rounded-full hover:text-acid"
              aria-label={`Ulubione (${favCount})`}
            >
              <HeartIcon />
              {favCount > 0 && (
                <span className="absolute right-1 top-1 grid h-4 min-w-4 place-items-center rounded-full bg-white px-1 text-[10px] font-bold text-ink">
                  {favCount}
                </span>
              )}
            </TransitionLink>
          </Magnetic>
          <Magnetic>
            <motion.button
              id="cart-icon"
              type="button"
              onClick={openCart}
              animate={bagControls}
              className="relative grid h-11 w-11 place-items-center rounded-full hover:text-acid"
              aria-label={`Koszyk – liczba produktów: ${count}`}
            >
              <BagIcon />
              {count > 0 && (
                <motion.span
                  key={count}
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  className="absolute right-0.5 top-0.5 grid h-[18px] min-w-[18px] place-items-center rounded-full bg-acid px-1 text-[10px] font-bold text-ink"
                >
                  {count}
                </motion.span>
              )}
            </motion.button>
          </Magnetic>
          <button
            type="button"
            className="ml-1 flex h-11 w-11 flex-col items-center justify-center gap-1.5 lg:hidden"
            aria-label={menuOpen ? 'Zamknij menu' : 'Otwórz menu'}
            aria-expanded={menuOpen}
            aria-controls="mobile-menu"
            onClick={() => setMenuOpen(!menuOpen)}
          >
            <motion.span className="block h-px w-6 bg-current" animate={menuOpen ? { rotate: 45, y: 3.5 } : { rotate: 0, y: 0 }} />
            <motion.span className="block h-px w-6 bg-current" animate={menuOpen ? { rotate: -45, y: -3.5 } : { rotate: 0, y: 0 }} />
          </button>
        </div>
      </div>
    </motion.header>
  );
}
