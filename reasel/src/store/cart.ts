'use client';

import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import type { CartItem } from '@/lib/types';
import { siteConfig } from '@/config/site';

interface CartState {
  items: CartItem[];
  isOpen: boolean;
  /** Licznik zwiększany po „wlocie” produktu – wyzwala animację ikony koszyka */
  bump: number;
  add: (item: Omit<CartItem, 'key' | 'quantity'>, quantity?: number) => void;
  remove: (key: string) => void;
  setQuantity: (key: string, quantity: number) => void;
  clear: () => void;
  open: () => void;
  close: () => void;
  toggle: () => void;
  pulse: () => void;
}

export const MAX_QTY = 10;

export const useCart = create<CartState>()(
  persist(
    (set) => ({
      items: [],
      isOpen: false,
      bump: 0,
      add: (item, quantity = 1) =>
        set((state) => {
          const key = `${item.productId}__${item.color.name}__${item.size}`;
          const existing = state.items.find((i) => i.key === key);
          if (existing) {
            return {
              items: state.items.map((i) =>
                i.key === key ? { ...i, quantity: Math.min(MAX_QTY, i.quantity + quantity) } : i,
              ),
            };
          }
          return { items: [...state.items, { ...item, key, quantity }] };
        }),
      remove: (key) => set((state) => ({ items: state.items.filter((i) => i.key !== key) })),
      setQuantity: (key, quantity) =>
        set((state) => ({
          items:
            quantity <= 0
              ? state.items.filter((i) => i.key !== key)
              : state.items.map((i) => (i.key === key ? { ...i, quantity: Math.min(MAX_QTY, quantity) } : i)),
        })),
      clear: () => set({ items: [] }),
      open: () => set({ isOpen: true }),
      close: () => set({ isOpen: false }),
      toggle: () => set((s) => ({ isOpen: !s.isOpen })),
      pulse: () => set((s) => ({ bump: s.bump + 1 })),
    }),
    {
      name: 'reasel-cart',
      storage: createJSONStorage(() => localStorage),
      // Zapisujemy tylko produkty – stan UI nie powinien przetrwać odświeżenia
      partialize: (state) => ({ items: state.items }),
      // Ręczna rehydratacja (StoreHydrator) zapobiega błędom hydratacji SSR
      skipHydration: true,
    },
  ),
);

export const selectCount = (s: CartState) => s.items.reduce((sum, i) => sum + i.quantity, 0);
export const selectSubtotal = (s: CartState) => s.items.reduce((sum, i) => sum + i.price * i.quantity, 0);

export function freeShippingProgress(subtotal: number) {
  const threshold = siteConfig.freeShippingThreshold;
  return {
    remaining: Math.max(0, threshold - subtotal),
    progress: Math.min(1, subtotal / threshold),
    reached: subtotal >= threshold,
  };
}
