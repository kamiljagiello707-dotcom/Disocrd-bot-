'use client';

import { useEffect, useRef } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { usePathname } from 'next/navigation';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { mainNav, siteConfig } from '@/config/site';
import { useUI } from '@/store/ui';
import { useScrollLock } from '@/hooks/useScrollLock';
import { useFocusTrap } from '@/hooks/useFocusTrap';

export function MobileMenu() {
  const { menuOpen, setMenuOpen } = useUI();
  const pathname = usePathname();
  const ref = useRef<HTMLDivElement>(null);
  useScrollLock(menuOpen);
  useFocusTrap(ref, menuOpen, () => setMenuOpen(false));

  useEffect(() => setMenuOpen(false), [pathname, setMenuOpen]);

  return (
    <AnimatePresence>
      {menuOpen && (
        <motion.div
          ref={ref}
          id="mobile-menu"
          role="dialog"
          aria-modal="true"
          aria-label="Menu"
          className="fixed inset-0 z-40 flex flex-col justify-between bg-ink px-6 pb-10 pt-28 lg:hidden"
          initial={{ clipPath: 'circle(0% at 100% 0%)' }}
          animate={{ clipPath: 'circle(150% at 100% 0%)' }}
          exit={{ clipPath: 'circle(0% at 100% 0%)' }}
          transition={{ duration: 0.7, ease: [0.76, 0, 0.24, 1] }}
        >
          <nav aria-label="Menu mobilne">
            <ul className="space-y-2">
              {[...mainNav, { href: '/ulubione', label: 'Ulubione' }].map((item, i) => (
                <li key={item.href} className="overflow-hidden">
                  <motion.div
                    initial={{ y: '100%' }}
                    animate={{ y: '0%' }}
                    transition={{ delay: 0.15 + i * 0.06, duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
                  >
                    <TransitionLink href={item.href} className="h-display block text-5xl hover:text-acid" onClick={() => setMenuOpen(false)}>
                      {item.label}
                    </TransitionLink>
                  </motion.div>
                </li>
              ))}
            </ul>
          </nav>
          <div className="space-y-1 text-sm text-ink-300">
            <p>{siteConfig.email}</p>
            <p>{siteConfig.phone}</p>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
