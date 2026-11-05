"use client";
import type { RefObject } from "react";
import { create } from "zustand";

export type Quality = "high" | "medium" | "low";

export type SceneKey = "hero" | "field" | "story" | "globe" | "notfound";

export type GlobeRegion = { region: string; lat: number; lon: number; count: number };

export type SceneData = {
  hero: undefined;
  field: { intensity?: number } | undefined;
  story: undefined;
  globe: { regions: GlobeRegion[] };
  notfound: undefined;
};

export type Slot<K extends SceneKey = SceneKey> = {
  id: string;
  scene: K;
  track: RefObject<HTMLElement | null>;
  data: SceneData[K];
  visible: boolean;
};

type ThreeState = {
  slots: Record<string, Slot>;
  quality: Quality;
  webgl: "unknown" | "on" | "off";
  ready: boolean;
  register: (slot: Slot) => void;
  unregister: (id: string) => void;
  setVisible: (id: string, visible: boolean) => void;
  setQuality: (q: Quality) => void;
};

export const useThreeStore = create<ThreeState>((set, get) => ({
  slots: {},
  quality: "high",
  webgl: "unknown",
  ready: false,
  register: (slot) => set({ slots: { ...get().slots, [slot.id]: slot } }),
  unregister: (id) => {
    const next = { ...get().slots };
    delete next[id];
    set({ slots: next });
  },
  setVisible: (id, visible) => {
    const s = get().slots[id];
    if (!s || s.visible === visible) return;
    set({ slots: { ...get().slots, [id]: { ...s, visible } } });
  },
  setQuality: (quality) => set({ quality }),
}));

export const isSlotVisible = (id: string) => useThreeStore.getState().slots[id]?.visible === true;

export const particleBudget = (q: Quality, high: number) =>
  Math.round(high * (q === "high" ? 1 : q === "medium" ? 0.5 : 0.22));
