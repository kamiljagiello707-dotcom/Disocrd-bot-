'use client';

import { create } from 'zustand';

export interface Flyer {
  id: number;
  src: string;
  from: { x: number; y: number; width: number; height: number };
}

interface UIState {
  searchOpen: boolean;
  menuOpen: boolean;
  sizeChartOpen: boolean;
  flyers: Flyer[];
  setSearchOpen: (v: boolean) => void;
  setMenuOpen: (v: boolean) => void;
  setSizeChartOpen: (v: boolean) => void;
  addFlyer: (f: Omit<Flyer, 'id'>) => void;
  removeFlyer: (id: number) => void;
}

let flyerId = 0;

export const useUI = create<UIState>()((set) => ({
  searchOpen: false,
  menuOpen: false,
  sizeChartOpen: false,
  flyers: [],
  setSearchOpen: (v) => set({ searchOpen: v }),
  setMenuOpen: (v) => set({ menuOpen: v }),
  setSizeChartOpen: (v) => set({ sizeChartOpen: v }),
  addFlyer: (f) => set((s) => ({ flyers: [...s.flyers, { ...f, id: ++flyerId }] })),
  removeFlyer: (id) => set((s) => ({ flyers: s.flyers.filter((f) => f.id !== id) })),
}));
