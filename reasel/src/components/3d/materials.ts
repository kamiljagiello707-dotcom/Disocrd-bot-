import * as THREE from 'three';

/**
 * Proceduralna mapa normalnych imitująca splot dzianiny.
 * Generowana raz (128×128) – brak dodatkowych zapytań sieciowych.
 */
let fabricNormal: THREE.DataTexture | null = null;

export function getFabricNormalMap(): THREE.DataTexture {
  if (fabricNormal) return fabricNormal;
  const size = 128;
  const height = new Float32Array(size * size);
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      // „Oczka” dzianiny: przesunięte rzędy + delikatny szum
      const row = Math.floor(y / 4);
      const shift = row % 2 === 0 ? 0 : 2;
      const u = ((x + shift) % 4) / 4;
      const v = (y % 4) / 4;
      const knit = Math.sin(u * Math.PI) * Math.sin(v * Math.PI);
      const noise = Math.sin(x * 12.9898 + y * 78.233) * 43758.5453;
      height[y * size + x] = knit * 0.8 + (noise - Math.floor(noise)) * 0.2;
    }
  }
  const data = new Uint8Array(size * size * 4);
  const strength = 2.2;
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const l = height[y * size + ((x - 1 + size) % size)];
      const r = height[y * size + ((x + 1) % size)];
      const d = height[((y - 1 + size) % size) * size + x];
      const u = height[((y + 1) % size) * size + x];
      const n = new THREE.Vector3((l - r) * strength, (d - u) * strength, 1).normalize();
      const i = (y * size + x) * 4;
      data[i] = (n.x * 0.5 + 0.5) * 255;
      data[i + 1] = (n.y * 0.5 + 0.5) * 255;
      data[i + 2] = (n.z * 0.5 + 0.5) * 255;
      data[i + 3] = 255;
    }
  }
  fabricNormal = new THREE.DataTexture(data, size, size, THREE.RGBAFormat);
  fabricNormal.wrapS = fabricNormal.wrapT = THREE.RepeatWrapping;
  // Mipmapy + filtrowanie trójliniowe – bez migotania (moiré) z daleka
  fabricNormal.generateMipmaps = true;
  fabricNormal.minFilter = THREE.LinearMipmapLinearFilter;
  fabricNormal.magFilter = THREE.LinearFilter;
  fabricNormal.anisotropy = 4;
  fabricNormal.repeat.set(6, 6);
  fabricNormal.needsUpdate = true;
  return fabricNormal;
}

/** Materiał tkaniny – MeshPhysicalMaterial z połyskiem „sheen” jak na bawełnie. */
export function createFabricMaterial(color = '#151515') {
  return new THREE.MeshPhysicalMaterial({
    color,
    roughness: 0.82,
    metalness: 0,
    sheen: 1,
    sheenRoughness: 0.7,
    sheenColor: new THREE.Color('#5a5a5a'),
    normalMap: getFabricNormalMap(),
    normalScale: new THREE.Vector2(0.22, 0.22),
    side: THREE.DoubleSide,
  });
}

/** Presety materiałów używane w scroll-story (interpolowane płynnie). */
export const MATERIAL_PRESETS = [
  // 0 – Onyx: czarna, matowa bawełna
  { color: new THREE.Color('#141414'), roughness: 0.85, metalness: 0, sheen: 1, clearcoat: 0, emissive: 0 },
  // 1 – Acid: neonowa limonka z lekką emisją (łapie bloom)
  { color: new THREE.Color('#c6ff00'), roughness: 0.6, metalness: 0, sheen: 0.6, clearcoat: 0, emissive: 0.12 },
  // 2 – Chrome: płynny metal
  { color: new THREE.Color('#d8d8d8'), roughness: 0.16, metalness: 1, sheen: 0, clearcoat: 1, emissive: 0 },
  // 3 – Bone: złamana biel
  { color: new THREE.Color('#ebe6dc'), roughness: 0.9, metalness: 0, sheen: 1, clearcoat: 0, emissive: 0 },
] as const;

const tmpColor = new THREE.Color();

/** Interpoluje materiał pomiędzy presetami dla ciągłej wartości `t` (np. 1.4). */
export function applyPresetBlend(mat: THREE.MeshPhysicalMaterial, t: number, damp = 1) {
  const max = MATERIAL_PRESETS.length - 1;
  const clamped = Math.min(max, Math.max(0, t));
  const i = Math.floor(clamped);
  const j = Math.min(max, i + 1);
  const f = clamped - i;
  const a = MATERIAL_PRESETS[i];
  const b = MATERIAL_PRESETS[j];
  tmpColor.copy(a.color).lerp(b.color, f);
  mat.color.lerp(tmpColor, damp);
  mat.roughness += (a.roughness + (b.roughness - a.roughness) * f - mat.roughness) * damp;
  mat.metalness += (a.metalness + (b.metalness - a.metalness) * f - mat.metalness) * damp;
  mat.sheen += (a.sheen + (b.sheen - a.sheen) * f - mat.sheen) * damp;
  mat.clearcoat += (a.clearcoat + (b.clearcoat - a.clearcoat) * f - mat.clearcoat) * damp;
  mat.emissive.copy(mat.color);
  mat.emissiveIntensity += (a.emissive + (b.emissive - a.emissive) * f - mat.emissiveIntensity) * damp;
}

export const metalMaterial = () =>
  new THREE.MeshStandardMaterial({ color: '#c9c9c9', metalness: 1, roughness: 0.25 });

export const accentMaterial = () =>
  new THREE.MeshStandardMaterial({ color: '#c6ff00', emissive: '#c6ff00', emissiveIntensity: 2.2, toneMapped: false });
