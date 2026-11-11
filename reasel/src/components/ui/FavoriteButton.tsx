'use client';

import { motion } from 'framer-motion';
import { useFavorites } from '@/store/favorites';
import { HeartIcon } from './Icons';
import { cn } from '@/lib/format';

export function FavoriteButton({ productId, name, className }: { productId: string; name: string; className?: string }) {
  const active = useFavorites((s) => s.ids.includes(productId));
  const toggle = useFavorites((s) => s.toggle);

  return (
    <motion.button
      type="button"
      onClick={(e) => {
        e.preventDefault();
        e.stopPropagation();
        toggle(productId);
      }}
      whileTap={{ scale: 0.8 }}
      aria-pressed={active}
      aria-label={active ? `Usuń ${name} z ulubionych` : `Dodaj ${name} do ulubionych`}
      className={cn(
        'grid h-10 w-10 place-items-center rounded-full backdrop-blur-md transition-colors',
        active ? 'bg-acid text-ink' : 'bg-black/40 text-white hover:bg-black/70',
        className,
      )}
    >
      <motion.span key={String(active)} initial={{ scale: 0.4 }} animate={{ scale: 1 }} transition={{ type: 'spring', stiffness: 500, damping: 15 }}>
        <HeartIcon filled={active} width={18} height={18} />
      </motion.span>
    </motion.button>
  );
}
