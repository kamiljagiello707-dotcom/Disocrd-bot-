"use client";
import { useRef } from "react";
import { Link } from "@/i18n/navigation";
import { SceneSlot } from "@/components/three/scene-slot";
import { Button } from "@/components/ui/button";

export function NotFoundStage({
  title,
  subtitle,
  home,
  alt,
}: {
  title: string;
  subtitle: string;
  home: string;
  alt: string;
  back: string;
}) {
  const stage = useRef<HTMLDivElement>(null);
  return (
    <section className="container-x flex min-h-[100svh] flex-col items-center justify-center pt-28 pb-16 text-center">
      <div ref={stage} role="img" aria-label={alt} className="relative h-[34vh] w-full max-w-4xl">
        <SceneSlot scene="notfound" track={stage} />
        <p
          aria-hidden
          className="webgl-fallback display absolute inset-0 hidden text-center text-[28vh] leading-[34vh] text-white/80"
        >
          404
        </p>
      </div>
      <h1 className="heading mt-6 text-3xl text-white">{title}</h1>
      <p className="mt-4 max-w-md text-fg-muted">{subtitle}</p>
      <Button asChild className="mt-10">
        <Link href="/">{home}</Link>
      </Button>
    </section>
  );
}
