"use client";
import { create } from "zustand";

/** Stan współdzielony między DOM (GSAP/Lenis) a sceną R3F — czytany w useFrame bez rerenderów. */
type ScrollState = {
  heroProgress: number; // 0–1: rozpad tarczy
  storyProgress: number; // 0–1: storytelling
  pointer: { x: number; y: number }; // -1..1
  velocity: number;
};

export const useScrollStore = create<ScrollState>(() => ({
  heroProgress: 0,
  storyProgress: 0,
  pointer: { x: 0, y: 0 },
  velocity: 0,
}));
