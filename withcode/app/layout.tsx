import type { ReactNode } from "react";
import "@/styles/globals.css";

/** Root layout przekazuje dalej — <html> renderuje app/[locale]/layout.tsx (next-intl). */
export default function RootLayout({ children }: { children: ReactNode }) {
  return children;
}
