"use client";
import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { AnimatePresence, motion } from "motion/react";
import { Link, usePathname } from "@/i18n/navigation";
import { Logo } from "@/components/ui/logo";
import { Button } from "@/components/ui/button";
import { Menu, X, ArrowUpRight } from "@/components/ui/icons";
import { LocaleSwitch } from "./locale-switch";
import { primaryNav, secondaryNav } from "./nav-links";
import { cn, inviteUrl } from "@/lib/utils";

export function Nav() {
  const t = useTranslations();
  const pathname = usePathname();
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const [lastPath, setLastPath] = useState(pathname);
  if (lastPath !== pathname) {
    setLastPath(pathname);
    setOpen(false);
  }

  useEffect(() => {
    const lenis = (window as Window & { __lenis?: { stop: () => void; start: () => void } })
      .__lenis;
    document.documentElement.style.overflow = open ? "hidden" : "";
    if (open) lenis?.stop();
    else lenis?.start();
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

  return (
    <>
      <header
        style={{ viewTransitionName: "site-header" }}
        className={cn(
          "fixed inset-x-0 top-0 z-[var(--z-nav)] transition-[background-color,border-color,backdrop-filter] duration-500",
          scrolled || open
            ? "border-b border-border bg-black/60 backdrop-blur-xl"
            : "border-b border-transparent",
        )}
      >
        <div className="container-x flex h-16 items-center justify-between gap-6 md:h-20">
          <Link href="/" className="relative z-10 rounded-md" aria-label="WithCode.pl — start">
            <Logo />
          </Link>
          <nav aria-label={t("nav.main")} className="hidden lg:block">
            <ul className="flex items-center gap-1">
              {primaryNav.map((item) => (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    aria-current={isActive(item.href) ? "page" : undefined}
                    className={cn(
                      "relative rounded-full px-3.5 py-2 text-sm transition-colors duration-200",
                      isActive(item.href) ? "text-white" : "text-fg-muted hover:text-white",
                    )}
                  >
                    {t(`nav.${item.key}`)}
                    {isActive(item.href) ? (
                      <motion.span
                        layoutId="nav-dot"
                        className="absolute inset-x-3.5 -bottom-0.5 h-px bg-white"
                      />
                    ) : null}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
          <div className="relative z-10 flex items-center gap-2">
            <LocaleSwitch className="hidden sm:inline-flex" />
            <Button asChild variant="secondary" size="sm" className="hidden md:inline-flex">
              <Link href="/dashboard" prefetch={false}>
                <span className="relative">{t("common.dashboard")}</span>
              </Link>
            </Button>
            <Button asChild size="sm" className="hidden sm:inline-flex">
              <a href={inviteUrl()} target="_blank" rel="noopener noreferrer">
                {t("common.addToDiscord")}
              </a>
            </Button>
            <button
              type="button"
              className="inline-flex size-10 items-center justify-center rounded-full border border-border text-white lg:hidden"
              aria-expanded={open}
              aria-controls="mobile-menu"
              aria-label={open ? t("common.closeMenu") : t("common.openMenu")}
              onClick={() => setOpen((v) => !v)}
            >
              {open ? <X className="size-5" /> : <Menu className="size-5" />}
            </button>
          </div>
        </div>
      </header>

      <AnimatePresence>
        {open ? (
          <motion.div
            id="mobile-menu"
            role="dialog"
            aria-modal="true"
            aria-label={t("nav.main")}
            className="fixed inset-0 z-[calc(var(--z-nav)-1)] flex flex-col bg-black pt-24 lg:hidden"
            initial={{ clipPath: "circle(0% at calc(100% - 2.5rem) 2.5rem)" }}
            animate={{ clipPath: "circle(150% at calc(100% - 2.5rem) 2.5rem)" }}
            exit={{ clipPath: "circle(0% at calc(100% - 2.5rem) 2.5rem)" }}
            transition={{ duration: 0.7, ease: [0.76, 0, 0.24, 1] }}
          >
            <nav className="container-x flex-1 overflow-y-auto">
              <ul className="flex flex-col">
                {[...primaryNav, ...secondaryNav].map((item, i) => (
                  <motion.li
                    key={item.href}
                    initial={{ opacity: 0, y: 30 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.15 + i * 0.04, duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
                    className="border-b border-border"
                  >
                    <Link
                      href={item.href}
                      className="flex items-center justify-between py-4 font-display text-3xl font-semibold tracking-tight text-white"
                    >
                      {t(`nav.${item.key}`)}
                      <ArrowUpRight className="size-5 text-fg-subtle" />
                    </Link>
                  </motion.li>
                ))}
              </ul>
            </nav>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1, transition: { delay: 0.5 } }}
              className="container-x flex flex-wrap items-center gap-3 pt-6 pb-[max(1.5rem,env(safe-area-inset-bottom))]"
            >
              <Button asChild className="flex-1">
                <a href={inviteUrl()} target="_blank" rel="noopener noreferrer">
                  {t("common.addToDiscord")}
                </a>
              </Button>
              <Button asChild variant="secondary" className="flex-1">
                <Link href="/dashboard" prefetch={false}>
                  <span className="relative">{t("common.dashboard")}</span>
                </Link>
              </Button>
              <LocaleSwitch />
            </motion.div>
          </motion.div>
        ) : null}
      </AnimatePresence>
    </>
  );
}
