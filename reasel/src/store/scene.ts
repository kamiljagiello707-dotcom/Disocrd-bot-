'use client';

import { create } from 'zustand';

/**
 * Stan sceny 3D.
 *
 * `sceneState` to zwykły, mutowalny obiekt – czytany w każdej klatce w `useFrame`
 * i zapisywany przez ScrollTrigger / zdarzenia myszy. Dzięki temu animacje
 * sterowane scrollem nie powodują re-renderów Reacta (stabilne 60 FPS).
 */
export const sceneState = {
  /** Ciągły postęp historii scrolla: indeks sekcji + postęp w sekcji (np. 3.42) */
  story: 0,
  /** Znormalizowana pozycja kursora (-1..1) */
  pointer: { x: 0, y: 0 },
  /** Czy aktualnie jesteśmy na stronie głównej (pełna scena hero) */
  home: true,
  /** Prędkość scrolla z Lenisa – wykorzystywana np. przez cząsteczki */
  velocity: 0,
};

export type Quality = 'high' | 'low';

interface SceneStore {
  /** Assety sceny 3D załadowane – preloader może zniknąć */
  ready: boolean;
  /** Preloader zakończył animację wyjścia */
  introDone: boolean;
  quality: Quality;
  webgl: boolean;
  /** Realny postęp ładowania assetów 3D (0–100) raportowany przez scenę */
  progress: number;
  setProgress: (p: number) => void;
  setReady: (v: boolean) => void;
  setIntroDone: (v: boolean) => void;
  setQuality: (q: Quality) => void;
  setWebgl: (v: boolean) => void;
}

export const useScene = create<SceneStore>()((set) => ({
  ready: false,
  introDone: false,
  quality: 'high',
  webgl: true,
  progress: 0,
  setProgress: (progress) => set({ progress }),
  setReady: (ready) => set({ ready }),
  setIntroDone: (introDone) => set({ introDone }),
  setQuality: (quality) => set({ quality }),
  setWebgl: (webgl) => set({ webgl }),
}));
