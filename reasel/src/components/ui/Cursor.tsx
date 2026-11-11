'use client';

import { useEffect, useRef, useState } from 'react';
import { useFinePointer, useReducedMotion } from '@/hooks/useMediaQuery';
import { sceneState } from '@/store/scene';

type Variant = 'default' | 'hover' | 'view' | 'drag' | 'hidden';

const LABELS: Partial<Record<Variant, string>> = { view: 'Zobacz', drag: 'Przeciągnij' };

/**
 * Własny kursor: kółko podążające za myszą z bezwładnością (lerp w rAF).
 * Nad linkami/przyciskami powiększa się, nad produktami pokazuje „Zobacz”,
 * nad karuzelą 3D – „Przeciągnij”. Aktywny tylko na urządzeniach z myszą.
 * Przy okazji zapisuje znormalizowaną pozycję kursora dla sceny 3D (paralaksa).
 */
export function Cursor() {
  const fine = useFinePointer();
  const reduced = useReducedMotion();
  const ringRef = useRef<HTMLDivElement>(null);
  const dotRef = useRef<HTMLDivElement>(null);
  const [variant, setVariant] = useState<Variant>('hidden');

  // Pozycja kursora dla sceny 3D – także na urządzeniach bez własnego kursora
  useEffect(() => {
    const onMove = (e: PointerEvent) => {
      sceneState.pointer.x = (e.clientX / window.innerWidth) * 2 - 1;
      sceneState.pointer.y = -(e.clientY / window.innerHeight) * 2 + 1;
    };
    window.addEventListener('pointermove', onMove, { passive: true });
    return () => window.removeEventListener('pointermove', onMove);
  }, []);

  useEffect(() => {
    if (!fine) return;
    document.documentElement.classList.add('has-cursor');

    const target = { x: -100, y: -100 };
    const pos = { x: -100, y: -100 };
    let raf = 0;

    const loop = () => {
      const k = reduced ? 1 : 0.18;
      pos.x += (target.x - pos.x) * k;
      pos.y += (target.y - pos.y) * k;
      if (ringRef.current) ringRef.current.style.transform = `translate3d(${pos.x}px, ${pos.y}px, 0)`;
      if (dotRef.current) dotRef.current.style.transform = `translate3d(${target.x}px, ${target.y}px, 0)`;
      raf = requestAnimationFrame(loop);
    };
    raf = requestAnimationFrame(loop);

    const onMove = (e: PointerEvent) => {
      target.x = e.clientX;
      target.y = e.clientY;
      setVariant((v) => (v === 'hidden' ? 'default' : v));
    };
    const onOver = (e: MouseEvent) => {
      const el = (e.target as HTMLElement).closest<HTMLElement>(
        '[data-cursor], a, button, [role="button"], label, select',
      );
      if (!el) return setVariant('default');
      const data = el.dataset.cursor as Variant | undefined;
      setVariant(data ?? 'hover');
    };
    const onLeave = () => setVariant('hidden');
    const onDown = () => ringRef.current?.classList.add('scale-75');
    const onUp = () => ringRef.current?.classList.remove('scale-75');

    window.addEventListener('pointermove', onMove, { passive: true });
    document.addEventListener('mouseover', onOver);
    document.documentElement.addEventListener('mouseleave', onLeave);
    window.addEventListener('pointerdown', onDown);
    window.addEventListener('pointerup', onUp);

    return () => {
      cancelAnimationFrame(raf);
      document.documentElement.classList.remove('has-cursor');
      window.removeEventListener('pointermove', onMove);
      document.removeEventListener('mouseover', onOver);
      document.documentElement.removeEventListener('mouseleave', onLeave);
      window.removeEventListener('pointerdown', onDown);
      window.removeEventListener('pointerup', onUp);
    };
  }, [fine, reduced]);

  if (!fine) return null;

  const size =
    variant === 'view' || variant === 'drag' ? 96 : variant === 'hover' ? 64 : variant === 'hidden' ? 0 : 36;
  const label = LABELS[variant];

  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 z-[100]">
      <div ref={ringRef} className="absolute left-0 top-0 will-change-transform">
        <div
          className="flex -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full border transition-[width,height,background-color,border-color] duration-300 ease-expo"
          style={{
            width: size,
            height: size,
            borderColor: variant === 'default' ? 'rgba(242,241,238,0.6)' : 'transparent',
            backgroundColor:
              variant === 'view' || variant === 'drag'
                ? '#c6ff00'
                : variant === 'hover'
                  ? 'rgba(198,255,0,0.18)'
                  : 'transparent',
          }}
        >
          {label && (
            <span className="text-[10px] font-semibold uppercase tracking-[0.18em] text-ink">{label}</span>
          )}
        </div>
      </div>
      <div ref={dotRef} className="absolute left-0 top-0 will-change-transform">
        <div
          className="h-1.5 w-1.5 -translate-x-1/2 -translate-y-1/2 rounded-full bg-acid transition-opacity"
          style={{ opacity: variant === 'hidden' || label ? 0 : 1 }}
        />
      </div>
    </div>
  );
}
