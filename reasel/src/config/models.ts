import type { GarmentType } from '@/lib/types';

/**
 * PODMIANA MODELI 3D
 * -------------------
 * Domyślnie każdy typ ubrania jest budowany proceduralnie z geometrii Three.js
 * (patrz `components/3d/garments`). Aby użyć własnego modelu:
 *
 * 1. Skompresuj model:  npx gltf-transform optimize bluza.glb public/models/hoodie.glb --compress draco
 * 2. Odkomentuj / dodaj wpis poniżej, np.  hoodie: '/models/hoodie.glb'
 *
 * Siatki, których nazwa zawiera „zip”, „metal”, „button” lub „logo”, zachowają
 * oryginalny materiał – pozostałe otrzymają materiał tkaniny sterowany wariantem koloru.
 */
export const GARMENT_MODELS: Partial<Record<GarmentType, string>> = {
  // hoodie: '/models/hoodie.glb',
  // jacket: '/models/jacket.glb',
};

/** Ścieżka do lokalnego dekodera Draco (skopiowanego z three/examples). */
export const DRACO_PATH = '/draco/';

/** Font typeface.json dla logo 3D (Text3D). */
export const LOGO_FONT = '/fonts/helvetiker_bold.typeface.json';
