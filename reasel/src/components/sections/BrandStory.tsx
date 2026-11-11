'use client';

import { useEffect, useRef } from 'react';
import { gsap } from '@/lib/gsap';
import { useStorySection } from '@/hooks/useStorySection';
import { useReducedMotion } from '@/hooks/useMediaQuery';

const CHAPTERS = [
  {
    no: '01',
    title: 'Tkanina',
    text: 'Wszystko zaczyna się od włókna. Bawełna organiczna z certyfikatem GOTS, czesana i szczotkowana – 460 gramów na metr kwadratowy.',
  },
  {
    no: '02',
    title: 'Konstrukcja',
    text: 'Rozkładamy każdy model na części pierwsze. 14 elementów kroju, wzmocnione szwy i obniżona linia ramion projektowana w 3D.',
  },
  {
    no: '03',
    title: 'Detal',
    text: 'Metalowe końcówki, haft 3D puff, odblaskowe nadruki. Detale, które widać dopiero z bliska – i które zostają na lata.',
  },
  {
    no: '04',
    title: 'Całość',
    text: 'Z części powstaje forma. Szyjemy w małych seriach w polskich szwalniach, bo jakość nie znosi pośpiechu.',
  },
];

/**
 * Historia marki – wysoka sekcja (400vh) ze sticky kadrem.
 * Scena 3D w tle (GlobalScene) przelatuje kamerą wokół bluzy, rozkłada ją na części
 * i składa z powrotem; tu animujemy tylko rozdziały tekstu (GSAP timeline + scrub).
 */
export function BrandStory() {
  const ref = useRef<HTMLElement>(null);
  const reduced = useReducedMotion();
  useStorySection(ref, 3, 'pinned');

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const ctx = gsap.context(() => {
      const chapters = gsap.utils.toArray<HTMLElement>('[data-chapter]');
      const tl = gsap.timeline({
        scrollTrigger: { trigger: el, start: 'top top', end: 'bottom bottom', scrub: reduced ? true : 0.8 },
        defaults: { ease: 'none' },
      });
      chapters.forEach((ch, i) => {
        const at = i;
        tl.fromTo(ch, { autoAlpha: 0, y: 80, filter: 'blur(12px)' }, { autoAlpha: 1, y: 0, filter: 'blur(0px)', duration: 0.35 }, at);
        if (i < chapters.length - 1) {
          tl.to(ch, { autoAlpha: 0, y: -80, filter: 'blur(12px)', duration: 0.35 }, at + 0.7);
        }
      });
      tl.fromTo('[data-story-progress]', { scaleX: 0 }, { scaleX: 1, duration: chapters.length, ease: 'none' }, 0);
    }, el);
    return () => ctx.revert();
  }, [reduced]);

  return (
    <section ref={ref} className="relative h-[400vh]" aria-labelledby="story-title">
      <div className="sticky top-0 flex h-[100svh] flex-col justify-between overflow-hidden py-24">
        <div className="container-x flex items-center justify-between">
          <h2 id="story-title" className="eyebrow">
            Historia marki · Anatomia bluzy VOID
          </h2>
          <div className="h-px w-32 overflow-hidden bg-white/15 md:w-64" aria-hidden>
            <div data-story-progress className="h-full origin-left bg-acid" />
          </div>
        </div>

        <div className="container-x relative flex-1">
          {CHAPTERS.map((c, i) => (
            <article
              key={c.no}
              data-chapter
              className={`invisible absolute top-1/2 w-[min(34rem,calc(100%-2.5rem))] -translate-y-1/2 ${i % 2 === 0 ? 'left-5 sm:left-8 lg:left-12' : 'right-5 text-right sm:right-8 lg:right-12'}`}
            >
              <p className="h-display text-outline text-[22vw] leading-none md:text-[11rem]" aria-hidden>
                {c.no}
              </p>
              <h3 className="h-display -mt-4 text-[clamp(2rem,4.6vw,3.75rem)] md:-mt-8">
                {c.title}
                <span className="text-acid">.</span>
              </h3>
              <p className="mt-4 text-base leading-relaxed text-ink-100 [text-shadow:0_2px_24px_rgba(0,0,0,0.9)] md:text-lg">{c.text}</p>
            </article>
          ))}
        </div>

        <p className="container-x eyebrow text-center">Przewijaj, aby rozłożyć bluzę na części</p>
      </div>
    </section>
  );
}
