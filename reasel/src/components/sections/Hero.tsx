'use client';

import { useRef } from 'react';
import { motion } from 'framer-motion';
import { SplitText } from '@/components/ui/SplitText';
import { Magnetic } from '@/components/ui/Magnetic';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { ArrowIcon } from '@/components/ui/Icons';
import { useScene } from '@/store/scene';
import { useStorySection } from '@/hooks/useStorySection';

/** Pełnoekranowe hero – tło to scena 3D (logo REASEL + bluza) z GlobalScene. */
export function Hero() {
  const ref = useRef<HTMLElement>(null);
  const introDone = useScene((s) => s.introDone);
  const webgl = useScene((s) => s.webgl);
  useStorySection(ref, 0, 'first');

  const fade = (delay: number) => ({
    initial: { opacity: 0, y: 24 },
    animate: introDone ? { opacity: 1, y: 0 } : undefined,
    transition: { delay, duration: 1, ease: [0.16, 1, 0.3, 1] as const },
  });

  return (
    <section ref={ref} className="relative flex h-[100svh] min-h-[640px] flex-col justify-between" aria-labelledby="hero-title">
      <h1 id="hero-title" className="sr-only">
        Reasel — premium streetwear. Nowa kolekcja jesień/zima 2026
      </h1>

      {/* Bez WebGL pokazujemy logo tekstowo */}
      {!webgl && (
        <p aria-hidden className="h-display absolute inset-0 flex items-center justify-center text-display-xl">
          REASEL
        </p>
      )}

      <div className="container-x flex justify-between pt-[calc(var(--header-h)+1.5rem)]">
        <motion.p {...fade(0.2)} className="eyebrow">
          Drop 04 <span className="text-acid">●</span> FW26
        </motion.p>
        <motion.p {...fade(0.3)} className="eyebrow hidden text-right sm:block">
          Projektowane w Warszawie
          <br />
          Szyte w Polsce
        </motion.p>
      </div>

      <div className="container-x grid items-end gap-8 pb-10 md:grid-cols-12 md:pb-14">
        <div className="md:col-span-7">
          {introDone ? (
            <SplitText as="p" immediate text="Noś przyszłość." className="h-display text-display-lg" stagger={0.035} delay={0.15} />
          ) : (
            <p className="h-display text-display-lg opacity-0" aria-hidden>
              Noś przyszłość.
            </p>
          )}
        </div>
        <div className="flex flex-col gap-6 md:col-span-5 md:items-end md:text-right">
          <motion.p {...fade(0.6)} className="max-w-sm text-base leading-relaxed text-ink-200">
            Ciężkie dzianiny, techniczne kroje i limitowane serie. Streetwear premium, który projektujemy z myślą o
            ulicy jutra.
          </motion.p>
          <motion.div {...fade(0.75)} className="flex flex-wrap gap-3 md:justify-end">
            <Magnetic>
              <TransitionLink href="/sklep" className="btn-primary">
                Odkryj kolekcję <ArrowIcon width={18} height={18} />
              </TransitionLink>
            </Magnetic>
            <Magnetic>
              <TransitionLink href="/o-nas" className="btn-ghost">
                Manifest
              </TransitionLink>
            </Magnetic>
          </motion.div>
        </div>
      </div>

    </section>
  );
}
