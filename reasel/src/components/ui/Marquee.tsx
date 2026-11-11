'use client';

import { useEffect, useRef } from 'react';
import { gsap } from '@/lib/gsap';
import { sceneState } from '@/store/scene';
import { useReducedMotion } from '@/hooks/useMediaQuery';
import { cn } from '@/lib/format';

interface MarqueeProps {
  items: string[];
  /** Bazowa prędkość w px/s */
  speed?: number;
  /** -1 = w lewo, 1 = w prawo */
  direction?: 1 | -1;
  className?: string;
  outline?: boolean;
}

/**
 * Marquee z nazwą marki, którego prędkość i kierunek zależą od prędkości scrolla.
 * Ruch liczony w tickerze GSAP (ta sama pętla co Lenis), bez re-renderów Reacta.
 */
export function Marquee({ items, speed = 60, direction = -1, className, outline }: MarqueeProps) {
  const trackRef = useRef<HTMLDivElement>(null);
  const reduced = useReducedMotion();

  useEffect(() => {
    const track = trackRef.current;
    if (!track || reduced) return;

    let x = 0;
    let dir = direction;
    let boost = 0;
    let lastScroll = window.scrollY;

    const tick = (_t: number, dt: number) => {
      const half = track.scrollWidth / 2;
      if (!half) return;
      // Prędkość scrolla: z Lenisa lub z różnicy pozycji (natywny scroll)
      const scrollY = window.scrollY;
      const v = sceneState.velocity || scrollY - lastScroll;
      lastScroll = scrollY;
      if (Math.abs(v) > 0.5) dir = (v > 0 ? 1 : -1) * direction as 1 | -1;
      boost += (Math.min(Math.abs(v) * 25, 900) - boost) * 0.08;

      x += ((speed + boost) * dir * dt) / 1000;
      if (x <= -half) x += half;
      if (x > 0) x -= half;
      track.style.transform = `translate3d(${x}px,0,0)`;
    };

    gsap.ticker.add(tick);
    return () => gsap.ticker.remove(tick);
  }, [direction, speed, reduced]);

  const content = (hidden: boolean) => (
    <div className="flex shrink-0 items-center" aria-hidden={hidden || undefined}>
      {items.map((item, i) => (
        <span key={i} className="flex items-center">
          <span className={cn('h-display whitespace-nowrap px-6 md:px-10', outline && i % 2 === 1 && 'text-outline')}>
            {item}
          </span>
          <span className="text-acid" aria-hidden>
            ✦
          </span>
        </span>
      ))}
    </div>
  );

  return (
    <div className={cn('relative flex overflow-hidden', className)}>
      <div ref={trackRef} className="flex will-change-transform">
        {content(false)}
        {content(true)}
      </div>
    </div>
  );
}
