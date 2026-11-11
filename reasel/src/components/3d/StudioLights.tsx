'use client';

import { Environment, Lightformer } from '@react-three/drei';

/**
 * Oświetlenie studyjne: lokalne środowisko z Lightformerów (bez pobierania HDRI)
 * + światło kluczowe i dwa kontrowe (rim), w tym jedno w kolorze akcentu.
 */
export function StudioLights({ intensity = 1, accent = true }: { intensity?: number; accent?: boolean }) {
  return (
    <>
      <ambientLight intensity={0.15 * intensity} />
      <directionalLight position={[3, 5, 4]} intensity={1.6 * intensity} color="#ffffff" />
      {/* Kontry – podkreślają sylwetkę ubrania na ciemnym tle */}
      <spotLight position={[-5, 3, -4]} angle={0.5} penumbra={1} intensity={60 * intensity} color="#ffffff" distance={20} />
      {accent && (
        <spotLight position={[5, -1, -3]} angle={0.6} penumbra={1} intensity={50 * intensity} color="#c6ff00" distance={18} />
      )}
      <Environment resolution={256} frames={1}>
        <group rotation={[-Math.PI / 3, 0, 0]}>
          <Lightformer form="rect" intensity={4} position={[0, 5, -9]} scale={[10, 10, 1]} />
          <Lightformer form="rect" intensity={2} position={[-5, 1, -1]} rotation-y={Math.PI / 2} scale={[20, 0.5, 1]} />
          <Lightformer form="rect" intensity={2} position={[5, 1, -1]} rotation-y={-Math.PI / 2} scale={[20, 0.5, 1]} />
          <Lightformer form="ring" color="#c6ff00" intensity={3} position={[2, 2, 6]} scale={2} />
          <Lightformer form="rect" intensity={1} position={[0, -4, 4]} scale={[10, 1, 1]} />
        </group>
      </Environment>
    </>
  );
}
