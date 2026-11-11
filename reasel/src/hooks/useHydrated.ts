'use client';

import { useEffect, useState } from 'react';

/** Zwraca true po pierwszym renderze po stronie klienta. */
export function useHydrated() {
  const [hydrated, setHydrated] = useState(false);
  useEffect(() => setHydrated(true), []);
  return hydrated;
}
