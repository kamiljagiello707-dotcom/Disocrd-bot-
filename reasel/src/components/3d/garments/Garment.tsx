'use client';

import { useLayoutEffect, useMemo, useRef, type MutableRefObject } from 'react';
import * as THREE from 'three';
import { useFrame } from '@react-three/fiber';
import { useGLTF } from '@react-three/drei';
import type { GarmentType } from '@/lib/types';
import { DRACO_PATH, GARMENT_MODELS } from '@/config/models';
import { ProceduralGarment } from './ProceduralGarment';

interface GarmentProps {
  type: GarmentType;
  /** Materiał tkaniny – kontrolowany z zewnątrz (kolor wariantu, presety story) */
  material: THREE.Material;
  /** Siła „eksplozji” części (0 = złożone, 1 = rozłożone). Ref → zero re-renderów. */
  explode?: MutableRefObject<number>;
  /** Maksymalne oddalenie części przy explode = 1 */
  distance?: number;
}

/** Siatki o tych nazwach zachowują własny materiał z pliku .glb */
const KEEP_MATERIAL = /zip|metal|button|logo/i;

/** Model z pliku .glb (kompresja Draco) – z podmianą materiału tkaniny. */
function GLBGarment({ url, material }: { url: string; material: THREE.Material }) {
  const { scene } = useGLTF(url, DRACO_PATH);
  const model = useMemo(() => {
    const clone = scene.clone(true);
    // Normalizacja: wyśrodkowanie i skala do wysokości ~2.4 jednostki (jak model proceduralny)
    const box = new THREE.Box3().setFromObject(clone);
    const size = box.getSize(new THREE.Vector3());
    const center = box.getCenter(new THREE.Vector3());
    const scale = 2.4 / Math.max(size.x, size.y, size.z);
    clone.scale.setScalar(scale);
    clone.position.copy(center.multiplyScalar(-scale));
    clone.traverse((o) => {
      const mesh = o as THREE.Mesh;
      if (mesh.isMesh) {
        mesh.castShadow = true;
        if (!KEEP_MATERIAL.test(mesh.name)) mesh.material = material;
      }
    });
    return clone;
  }, [scene, material]);

  return <primitive object={model} />;
}

interface PartState {
  mesh: THREE.Object3D;
  origin: THREE.Vector3;
  rotation: THREE.Euler;
  offset: THREE.Vector3;
  spin: THREE.Vector3;
}

/**
 * Uniwersalne ubranie 3D: proceduralne albo z .glb.
 * Efekt eksplozji działa dla obu wariantów – kierunek każdej części liczony
 * jest od środka modelu do środka bounding boxa danej siatki.
 */
export function Garment({ type, material, explode, distance = 1.6 }: GarmentProps) {
  const url = GARMENT_MODELS[type];
  const root = useRef<THREE.Group>(null);
  const parts = useRef<PartState[]>([]);

  useLayoutEffect(() => {
    const group = root.current;
    if (!group) return;
    group.updateWorldMatrix(true, true);
    const rootInv = group.matrixWorld.clone().invert();
    const rootBox = new THREE.Box3();
    const meshes: THREE.Mesh[] = [];
    group.traverse((o) => {
      if ((o as THREE.Mesh).isMesh) meshes.push(o as THREE.Mesh);
    });
    meshes.forEach((m) => rootBox.expandByObject(m));
    const rootCenter = rootBox.getCenter(new THREE.Vector3()).applyMatrix4(rootInv);

    let seed = 1;
    const rand = () => {
      seed = (seed * 16807) % 2147483647;
      return seed / 2147483647 - 0.5;
    };

    parts.current = meshes.map((mesh) => {
      const parent = mesh.parent ?? group;
      const center = new THREE.Box3().setFromObject(mesh).getCenter(new THREE.Vector3()).applyMatrix4(rootInv);
      const dir = center.clone().sub(rootCenter);
      if (dir.lengthSq() < 1e-4) dir.set(0, 0, 1);
      dir.normalize();
      dir.z += 0.35; // lekko w stronę kamery – efektowniej
      dir.normalize().multiplyScalar(distance * (0.75 + Math.abs(rand())));

      // Przeliczenie przesunięcia z przestrzeni modelu do przestrzeni rodzica siatki
      const parentToRoot = rootInv.clone().multiply(parent.matrixWorld);
      const rootToParent = parentToRoot.clone().invert();
      const a = center.clone().applyMatrix4(rootToParent);
      const b = center.clone().add(dir).applyMatrix4(rootToParent);

      return {
        mesh,
        origin: mesh.position.clone(),
        rotation: mesh.rotation.clone(),
        offset: b.sub(a),
        spin: new THREE.Vector3(rand(), rand(), rand()).multiplyScalar(1.2),
      };
    });

    return () => {
      // Przywrócenie pozycji przy zmianie modelu
      parts.current.forEach((p) => {
        p.mesh.position.copy(p.origin);
        p.mesh.rotation.copy(p.rotation);
      });
      parts.current = [];
    };
  }, [type, url, distance]);

  useFrame(() => {
    const e = explode?.current ?? 0;
    if (!parts.current.length) return;
    for (const p of parts.current) {
      p.mesh.position.set(p.origin.x + p.offset.x * e, p.origin.y + p.offset.y * e, p.origin.z + p.offset.z * e);
      p.mesh.rotation.set(p.rotation.x + p.spin.x * e, p.rotation.y + p.spin.y * e, p.rotation.z + p.spin.z * e);
    }
  });

  return (
    <group ref={root}>
      {url ? <GLBGarment url={url} material={material} /> : <ProceduralGarment type={type} material={material} />}
    </group>
  );
}

// Wstępne ładowanie zdefiniowanych modeli .glb
Object.values(GARMENT_MODELS).forEach((u) => u && useGLTF.preload(u, DRACO_PATH));
