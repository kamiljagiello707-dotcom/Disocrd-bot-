import type { ReactNode } from "react";
import type { Metadata } from "next";

export const metadata: Metadata = { robots: { index: false, follow: false } };

/** Panel: lżejszy layout — bez sceny 3D, bez smooth scrolla, bez kursora. */
export default function DashboardGroupLayout({ children }: { children: ReactNode }) {
  return <div className="min-h-dvh bg-bg">{children}</div>;
}
