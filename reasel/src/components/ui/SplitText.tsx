'use client';

import { motion, type Variants } from 'framer-motion';
import { useMemo, type ElementType } from 'react';
import { cn } from '@/lib/format';

const container: Variants = {
  hidden: {},
  visible: ({ stagger, delay }: { stagger: number; delay: number }) => ({
    transition: { staggerChildren: stagger, delayChildren: delay },
  }),
};

const char: Variants = {
  hidden: { y: '115%', rotate: 8, opacity: 0 },
  visible: {
    y: '0%',
    rotate: 0,
    opacity: 1,
    transition: { duration: 0.9, ease: [0.16, 1, 0.3, 1] },
  },
};

interface SplitTextProps {
  text: string;
  as?: ElementType;
  className?: string;
  /** Opóźnienie między kolejnymi literami (s) */
  stagger?: number;
  delay?: number;
  /** Animacja przy wejściu w viewport (domyślnie) lub od razu */
  immediate?: boolean;
  id?: string;
}

/**
 * Nagłówek animowany litera po literze.
 * Czytniki ekranu dostają pełny tekst (aria-label), litery są aria-hidden.
 * Słowa nie łamią się w środku (każde słowo to inline-block z nowrap).
 */
export function SplitText({
  text,
  as: Tag = 'h2',
  className,
  stagger = 0.025,
  delay = 0,
  immediate = false,
  id,
}: SplitTextProps) {
  const words = text.split(' ');
  // Komponent motion tworzymy raz na dany tag (inaczej React remontowałby go przy każdym renderze)
  const MotionTag = useMemo(() => motion.create(Tag), [Tag]);

  return (
    <MotionTag
      id={id}
      aria-label={text}
      className={cn('split-text', className)}
      variants={container}
      custom={{ stagger, delay }}
      initial="hidden"
      {...(immediate ? { animate: 'visible' } : { whileInView: 'visible', viewport: { once: true, margin: '-10% 0px' } })}
    >
      {words.map((word, wi) => (
        <span key={wi} aria-hidden className="inline-block whitespace-nowrap">
          {Array.from(word).map((c, ci) => (
            <span key={ci} className="inline-block overflow-hidden pb-[0.08em] align-bottom">
              <motion.span className="inline-block origin-bottom-left" variants={char}>
                {c}
              </motion.span>
            </span>
          ))}
          {wi < words.length - 1 && <span className="inline-block">&nbsp;</span>}
        </span>
      ))}
    </MotionTag>
  );
}
