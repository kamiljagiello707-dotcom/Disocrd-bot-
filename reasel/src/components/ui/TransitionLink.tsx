'use client';

import Link, { type LinkProps } from 'next/link';
import { usePathname } from 'next/navigation';
import { forwardRef, type AnchorHTMLAttributes, type MouseEvent } from 'react';
import { useTransition } from '@/store/transition';

type Props = LinkProps & Omit<AnchorHTMLAttributes<HTMLAnchorElement>, keyof LinkProps>;

/**
 * Link z animowanym przejściem (kurtyna). Zachowuje wszystkie zalety next/link
 * (prefetch, SEO), a kliknięcie najpierw zasłania ekran, potem nawiguje.
 * Ctrl/Cmd/Shift + klik oraz środkowy przycisk działają natywnie.
 */
export const TransitionLink = forwardRef<HTMLAnchorElement, Props>(function TransitionLink(
  { href, onClick, children, ...rest },
  ref,
) {
  const pathname = usePathname();
  const start = useTransition((s) => s.start);

  const handleClick = (e: MouseEvent<HTMLAnchorElement>) => {
    onClick?.(e);
    if (e.defaultPrevented) return;
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey || e.button !== 0) return;
    if (rest.target && rest.target !== '_self') return;

    const url = typeof href === 'string' ? href : (href.pathname ?? '/');
    const targetPath = url.split('?')[0].split('#')[0];
    // Ta sama ścieżka (np. zmiana filtra) – zwykła nawigacja bez kurtyny
    if (targetPath === pathname) return;

    e.preventDefault();
    start(url);
  };

  return (
    <Link ref={ref} href={href} onClick={handleClick} {...rest}>
      {children}
    </Link>
  );
});
