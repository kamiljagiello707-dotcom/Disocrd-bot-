import { SplitReveal } from "@/components/motion/split-reveal";
import { cn } from "@/lib/utils";

export function SectionHeading({
  eyebrow,
  title,
  subtitle,
  align = "left",
  as = "h2",
  className,
}: {
  eyebrow?: string;
  title: string;
  subtitle?: string;
  align?: "left" | "center";
  as?: "h1" | "h2";
  className?: string;
}) {
  return (
    <div className={cn("max-w-3xl", align === "center" && "mx-auto text-center", className)}>
      {eyebrow ? <p className="eyebrow mb-5">{eyebrow}</p> : null}
      <SplitReveal
        as={as}
        by="words"
        className="heading text-4xl text-white"
        immediate={as === "h1"}
      >
        {title}
      </SplitReveal>
      {subtitle ? (
        <p
          className="mt-6 max-w-2xl text-lg leading-relaxed [text-wrap:pretty] text-fg-muted max-md:text-base"
          style={align === "center" ? { marginInline: "auto" } : undefined}
        >
          {subtitle}
        </p>
      ) : null}
    </div>
  );
}

export function PageHeader({
  eyebrow,
  title,
  subtitle,
  children,
}: {
  eyebrow: string;
  title: string;
  subtitle?: string;
  children?: React.ReactNode;
}) {
  return (
    <section className="container-x pt-36 pb-12 md:pt-48 md:pb-20">
      <SectionHeading as="h1" eyebrow={eyebrow} title={title} {...(subtitle ? { subtitle } : {})} />
      {children}
    </section>
  );
}
