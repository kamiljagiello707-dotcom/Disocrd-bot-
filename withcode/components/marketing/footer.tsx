import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Logo } from "@/components/ui/logo";
import { CookieSettingsButton } from "./cookie-banner";

export async function Footer() {
  const t = await getTranslations();
  const support = process.env.NEXT_PUBLIC_SUPPORT_SERVER_URL ?? "https://discord.gg/withcode";
  const cols = [
    {
      title: t("footer.product"),
      links: [
        { href: "/features", label: t("nav.features") },
        { href: "/commands", label: t("nav.commands") },
        { href: "/premium", label: t("nav.premium") },
        { href: "/demo", label: t("nav.demo") },
        { href: "/dashboard", label: t("common.dashboard") },
      ],
    },
    {
      title: t("footer.resources"),
      links: [
        { href: "/docs", label: t("nav.docs") },
        { href: "/changelog", label: t("nav.changelog") },
        { href: "/status", label: t("nav.status") },
        { href: "/partners", label: t("nav.partners") },
      ],
    },
    {
      title: t("footer.legal"),
      links: [
        { href: "/legal/terms", label: t("meta.terms") },
        { href: "/legal/privacy", label: t("meta.privacy") },
        { href: "/legal/cookies", label: t("meta.cookies") },
        { href: "/contact", label: t("nav.contact") },
      ],
    },
  ];
  return (
    <footer className="relative z-[var(--z-content)] border-t border-border bg-black">
      <div className="container-x grid gap-12 py-16 md:grid-cols-[1.4fr_repeat(3,1fr)] md:py-24">
        <div className="max-w-xs">
          <Logo />
          <p className="mt-5 text-sm leading-relaxed text-fg-muted">{t("footer.tagline")}</p>
          <a
            href={support}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-6 inline-flex text-sm text-white underline decoration-fg-faint underline-offset-4 hover:decoration-white"
          >
            {t("footer.support")} ↗
          </a>
        </div>
        {cols.map((c) => (
          <div key={c.title}>
            <h2 className="eyebrow mb-5">{c.title}</h2>
            <ul className="space-y-3">
              {c.links.map((l) => (
                <li key={l.href}>
                  <Link
                    href={l.href}
                    prefetch={l.href === "/dashboard" ? false : null}
                    className="text-sm text-fg-muted transition-colors hover:text-white"
                  >
                    {l.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <div className="container-x flex flex-col gap-3 border-t border-border py-6 text-xs text-fg-subtle sm:flex-row sm:items-center sm:justify-between">
        <p>{t("footer.rights", { year: new Date().getFullYear() })}</p>
        <div className="flex items-center gap-4">
          <p>{t("footer.notAffiliated")}</p>
          <CookieSettingsButton label={t("footer.cookieSettings")} />
        </div>
      </div>
      <svg
        aria-hidden
        viewBox="0 0 1000 170"
        className="pointer-events-none block w-full select-none"
        preserveAspectRatio="xMidYMax meet"
      >
        <text
          x="500"
          y="200"
          textAnchor="middle"
          fill="rgba(255,255,255,0.035)"
          style={{ font: "700 260px var(--font-display-stack)", letterSpacing: "-0.04em" }}
        >
          WithBot
        </text>
      </svg>
    </footer>
  );
}
