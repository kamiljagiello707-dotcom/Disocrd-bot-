import { ViewTransition, type ReactNode } from "react";
import { setRequestLocale } from "next-intl/server";
import { Nav } from "@/components/marketing/nav";
import { Footer } from "@/components/marketing/footer";
import { CookieBanner, consentBootScript } from "@/components/marketing/cookie-banner";
import { SmoothScroll } from "@/components/motion/smooth-scroll";
import { Cursor } from "@/components/motion/cursor";
import { CanvasMount } from "@/components/three/canvas-mount";

export default async function MarketingLayout({
  children,
  params,
}: {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);
  return (
    <>
      <script dangerouslySetInnerHTML={{ __html: consentBootScript }} />
      <SmoothScroll />
      <CanvasMount />
      <Nav />
      <ViewTransition default="page">
        <main id="main" className="relative z-[var(--z-content)]">
          {children}
        </main>
      </ViewTransition>
      <Footer />
      <CookieBanner />
      <Cursor />
    </>
  );
}
