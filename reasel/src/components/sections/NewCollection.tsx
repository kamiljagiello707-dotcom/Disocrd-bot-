'use client';

import { useEffect, useRef, useState } from 'react';
import dynamic from 'next/dynamic';
import { AnimatePresence, motion } from 'framer-motion';
import type { Product } from '@/lib/types';
import type { CarouselControl } from '@/components/3d/CarouselScene';
import { SplitText } from '@/components/ui/SplitText';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { Price } from '@/components/ui/Price';
import { ArrowIcon } from '@/components/ui/Icons';
import { ProductCard } from '@/components/product/ProductCard';
import { categoryLabel } from '@/lib/catalog';
import { ScrollTrigger } from '@/lib/gsap';
import { useStorySection } from '@/hooks/useStorySection';
import { useIsMobile, useReducedMotion } from '@/hooks/useMediaQuery';
import { useScene } from '@/store/scene';
import { useTransition } from '@/store/transition';

const CarouselScene = dynamic(() => import('@/components/3d/CarouselScene'), { ssr: false });

/** Sekcja „Nowa kolekcja” z karuzelą 3D (scroll + drag + klawiatura). */
export function NewCollection({ products }: { products: Product[] }) {
  const sectionRef = useRef<HTMLElement>(null);
  const stageRef = useRef<HTMLDivElement>(null);
  const control = useRef<CarouselControl>({ target: 0, current: 0, dragged: false });
  const scrollRot = useRef(0);
  const dragRot = useRef(0);
  const [active, setActive] = useState(0);
  const [nearViewport, setNearViewport] = useState(false);
  const [inView, setInView] = useState(false);
  const mobile = useIsMobile();
  const reduced = useReducedMotion();
  const webgl = useScene((s) => s.webgl);
  const startTransition = useTransition((s) => s.start);
  const n = products.length;
  const step = (Math.PI * 2) / n;

  useStorySection(sectionRef, 1);

  const applyTarget = () => {
    control.current.target = scrollRot.current + dragRot.current;
  };

  // Lazy mount canvasu + pauza renderowania poza ekranem
  useEffect(() => {
    const el = stageRef.current;
    if (!el) return;
    const near = new IntersectionObserver(([e]) => e.isIntersecting && setNearViewport(true), { rootMargin: '400px' });
    const visible = new IntersectionObserver(([e]) => setInView(e.isIntersecting), { rootMargin: '50px' });
    near.observe(el);
    visible.observe(el);
    return () => {
      near.disconnect();
      visible.disconnect();
    };
  }, []);

  // Obrót sterowany scrollem: przejście przez sekcję obraca karuzelę o ~połowę okręgu
  useEffect(() => {
    const el = sectionRef.current;
    if (!el) return;
    const st = ScrollTrigger.create({
      trigger: el,
      start: 'top bottom',
      end: 'bottom top',
      onUpdate: (self) => {
        scrollRot.current = self.progress * step * Math.floor(n / 2);
        applyTarget();
      },
    });
    return () => st.kill();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [n, step]);

  // Przeciąganie (mysz + dotyk) z bezwładnością i przyciąganiem do karty
  useEffect(() => {
    const el = stageRef.current;
    if (!el) return;
    let startX = 0;
    let lastX = 0;
    let velocity = 0;
    let down = false;

    const onDown = (e: PointerEvent) => {
      down = true;
      startX = lastX = e.clientX;
      velocity = 0;
      control.current.dragged = false;
    };
    const onMove = (e: PointerEvent) => {
      if (!down) return;
      const dx = e.clientX - lastX;
      lastX = e.clientX;
      velocity = dx;
      if (Math.abs(e.clientX - startX) > 6) control.current.dragged = true;
      dragRot.current -= dx * (mobile ? 0.008 : 0.005);
      applyTarget();
    };
    const onUp = () => {
      if (!down) return;
      down = false;
      dragRot.current -= velocity * 0.03;
      // Przyciąganie do najbliższej karty
      const total = scrollRot.current + dragRot.current;
      dragRot.current += Math.round(total / step) * step - total;
      applyTarget();
      window.setTimeout(() => (control.current.dragged = false), 50);
    };

    el.addEventListener('pointerdown', onDown);
    window.addEventListener('pointermove', onMove, { passive: true });
    window.addEventListener('pointerup', onUp);
    window.addEventListener('pointercancel', onUp);
    return () => {
      el.removeEventListener('pointerdown', onDown);
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
      window.removeEventListener('pointercancel', onUp);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mobile, step]);

  const go = (dir: 1 | -1) => {
    const total = scrollRot.current + dragRot.current;
    dragRot.current += Math.round(total / step) * step - total + dir * step;
    applyTarget();
  };

  const current = products[active];

  return (
    <section ref={sectionRef} className="relative z-10 overflow-hidden bg-ink py-24 md:py-32" aria-labelledby="new-collection-title">
      <div className="container-x mb-10 flex flex-col justify-between gap-6 md:mb-14 md:flex-row md:items-end">
        <div>
          <p className="eyebrow mb-4">Drop 04 — Jesień / Zima 2026</p>
          <SplitText id="new-collection-title" text="Nowa kolekcja" className="h-display text-display-lg" />
        </div>
        <div className="max-w-sm md:text-right">
          <p className="text-ink-200">
            {n} premierowych modeli. Obracaj karuzelę scrollem, przeciągnij ją lub użyj strzałek.
          </p>
          <TransitionLink href="/sklep" className="link-underline mt-4 inline-flex items-center gap-2 text-sm font-semibold uppercase tracking-[0.14em] text-acid">
            Zobacz wszystko <ArrowIcon width={16} height={16} />
          </TransitionLink>
        </div>
      </div>

      {webgl ? (
        <div className="relative">
          <div
            ref={stageRef}
            data-cursor="drag"
            className="relative h-[62vh] min-h-[420px] touch-pan-y select-none md:h-[78vh]"
            role="region"
            aria-roledescription="karuzela"
            aria-label="Karuzela nowej kolekcji"
            onKeyDown={(e) => {
              if (e.key === 'ArrowRight') go(1);
              if (e.key === 'ArrowLeft') go(-1);
            }}
          >
            {nearViewport && (
              <CarouselScene
                products={products}
                control={control}
                onActiveChange={setActive}
                onSelect={(i) => startTransition(`/produkt/${products[i].slug}`)}
                active={inView}
                mobile={mobile}
                reduced={reduced}
              />
            )}
          </div>

          <div className="container-x pointer-events-none absolute inset-x-0 bottom-0 flex items-end justify-between gap-4 pb-4">
            <div className="pointer-events-auto min-h-[96px]" aria-live="polite">
              <AnimatePresence mode="wait">
                <motion.div
                  key={current.id}
                  initial={{ opacity: 0, y: 16 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -16 }}
                  transition={{ duration: 0.4 }}
                >
                  <p className="eyebrow">
                    {String(active + 1).padStart(2, '0')} / {String(n).padStart(2, '0')} · {categoryLabel(current.category)}
                  </p>
                  <p className="h-display mt-2 text-2xl md:text-4xl">{current.name}</p>
                  <div className="mt-2 flex items-center gap-4">
                    <Price price={current.price} compareAt={current.compareAtPrice} />
                    <TransitionLink href={`/produkt/${current.slug}`} className="text-sm font-semibold uppercase tracking-[0.14em] text-acid underline-offset-4 hover:underline">
                      Zobacz produkt
                    </TransitionLink>
                  </div>
                </motion.div>
              </AnimatePresence>
            </div>
            <div className="pointer-events-auto flex gap-2">
              <button type="button" onClick={() => go(-1)} className="grid h-12 w-12 place-items-center rounded-full border border-white/20 hover:border-acid hover:text-acid" aria-label="Poprzedni produkt">
                <ArrowIcon className="rotate-180" />
              </button>
              <button type="button" onClick={() => go(1)} className="grid h-12 w-12 place-items-center rounded-full border border-white/20 hover:border-acid hover:text-acid" aria-label="Następny produkt">
                <ArrowIcon />
              </button>
            </div>
          </div>
        </div>
      ) : (
        // Fallback bez WebGL: przewijana pozioma lista kart
        <div className="container-x flex snap-x gap-5 overflow-x-auto pb-6 no-scrollbar">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} className="w-[72vw] shrink-0 snap-start sm:w-[320px]" sizes="320px" />
          ))}
        </div>
      )}
    </section>
  );
}
