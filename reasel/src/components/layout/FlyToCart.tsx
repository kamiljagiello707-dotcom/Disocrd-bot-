'use client';

import { AnimatePresence, motion } from 'framer-motion';
import { useUI, type Flyer } from '@/store/ui';
import { useCart } from '@/store/cart';

/**
 * Animacja „lotu” produktu do ikony koszyka po dodaniu.
 * Miniatura startuje z pozycji zdjęcia produktu i leci łukiem do #cart-icon.
 */
export function FlyToCart() {
  const flyers = useUI((s) => s.flyers);
  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 z-[95]">
      <AnimatePresence>
        {flyers.map((f) => (
          <FlyerImage key={f.id} flyer={f} />
        ))}
      </AnimatePresence>
    </div>
  );
}

function FlyerImage({ flyer }: { flyer: Flyer }) {
  const removeFlyer = useUI((s) => s.removeFlyer);
  const pulse = useCart((s) => s.pulse);

  const target = document.getElementById('cart-icon')?.getBoundingClientRect();
  const size = Math.min(flyer.from.width, 180);
  const startX = flyer.from.x + flyer.from.width / 2 - size / 2;
  const startY = flyer.from.y + flyer.from.height / 2 - (size * 1.25) / 2;
  const endX = target ? target.left + target.width / 2 - size / 2 : window.innerWidth - 60;
  const endY = target ? target.top + target.height / 2 - (size * 1.25) / 2 : 20;
  // Punkt kontrolny łuku – nad linią lotu
  const midX = (startX + endX) / 2;
  const midY = Math.min(startY, endY) - 160;

  return (
    <motion.img
      src={flyer.src}
      alt=""
      className="absolute left-0 top-0 rounded-xl object-cover shadow-[0_0_40px_rgba(198,255,0,0.5)]"
      style={{ width: size, height: size * 1.25 }}
      initial={{ x: startX, y: startY, scale: 1, opacity: 1, rotate: 0 }}
      animate={{
        x: [startX, midX, endX],
        y: [startY, midY, endY],
        scale: [1, 0.6, 0.08],
        rotate: [0, -8, 0],
        opacity: [1, 1, 0.6],
      }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.95, ease: [0.55, 0, 0.35, 1], times: [0, 0.45, 1] }}
      onAnimationComplete={() => {
        removeFlyer(flyer.id);
        pulse();
      }}
    />
  );
}
