'use client';

import { useEffect, useRef } from 'react';
import Image from 'next/image';
import { gsap } from '@/lib/gsap';
import { SplitText } from '@/components/ui/SplitText';
import { useStorySection } from '@/hooks/useStorySection';
import { useReducedMotion } from '@/hooks/useMediaQuery';
import { cn } from '@/lib/format';

interface LookbookImage {
  src: string;
  alt: string;
  caption: string;
}

/** Lookbook – galeria z paralaksą: kolumny przesuwają się z różną prędkością. */
export function Lookbook({ images }: { images: LookbookImage[] }) {
  const ref = useRef<HTMLElement>(null);
  const reduced = useReducedMotion();
  useStorySection(ref, 4);

  useEffect(() => {
    const el = ref.current;
    if (!el || reduced) return;
    const ctx = gsap.context(() => {
      gsap.utils.toArray<HTMLElement>('[data-speed]').forEach((col) => {
        const speed = Number(col.dataset.speed);
        gsap.fromTo(
          col,
          { yPercent: speed * 12 },
          { yPercent: -speed * 12, ease: 'none', scrollTrigger: { trigger: el, start: 'top bottom', end: 'bottom top', scrub: true } },
        );
      });
      gsap.utils.toArray<HTMLElement>('[data-zoom]').forEach((img) => {
        gsap.fromTo(
          img,
          { scale: 1.25 },
          { scale: 1, ease: 'none', scrollTrigger: { trigger: img, start: 'top bottom', end: 'bottom top', scrub: true } },
        );
      });
    }, el);
    return () => ctx.revert();
  }, [reduced]);

  const columns = [images.slice(0, 3), images.slice(3, 5), images.slice(5, 8)];
  const speeds = [1, -1.4, 0.8];

  return (
    <section ref={ref} className="relative overflow-hidden py-24 md:py-40" aria-labelledby="lookbook-title">
      <div className="container-x mb-16 grid gap-6 md:grid-cols-2 md:items-end">
        <div>
          <p className="eyebrow mb-4">Kampania FW26 · Warszawa</p>
          <SplitText id="lookbook-title" text="Lookbook" className="h-display text-display-lg" />
        </div>
        <p className="max-w-md text-ink-200 md:justify-self-end md:text-right">
          Fotografowane nocą na dachach i w przejściach podziemnych. Ludzie, miasto i ubrania, które z nim rezonują.
        </p>
      </div>

      <div className="container-x grid grid-cols-2 gap-4 md:grid-cols-3 md:gap-8">
        {columns.map((col, ci) => (
          <div key={ci} data-speed={speeds[ci]} className={cn('flex flex-col gap-4 md:gap-8', ci === 1 && 'mt-24', ci === 2 && 'hidden md:flex')}>
            {col.map((img) => (
              <figure key={img.src} className="group">
                <div className="relative aspect-[3/4] overflow-hidden rounded-2xl bg-ink-800">
                  <div data-zoom className="absolute inset-0">
                    <Image src={img.src} alt={img.alt} fill sizes="(min-width: 768px) 33vw, 50vw" className="object-cover grayscale-[40%] transition-[filter] duration-700 group-hover:grayscale-0" />
                  </div>
                </div>
                <figcaption className="eyebrow mt-3">{img.caption}</figcaption>
              </figure>
            ))}
          </div>
        ))}
      </div>
    </section>
  );
}
