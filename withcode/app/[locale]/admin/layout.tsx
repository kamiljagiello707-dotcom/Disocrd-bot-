import type { ReactNode } from "react";
import type { Metadata } from "next";
import { notFound, redirect } from "next/navigation";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Topbar } from "@/components/dashboard/topbar";
import { AccessError, requireAdmin } from "@/lib/permissions";

export const metadata: Metadata = { robots: { index: false, follow: false } };

const tabs = [
  ["", "overview"],
  ["/guilds", "guilds"],
  ["/changelog", "changelog"],
  ["/incidents", "incidents"],
  ["/faq", "faq"],
  ["/testimonials", "testimonials"],
  ["/partners", "partners"],
  ["/blacklist", "blacklist"],
] as const;

/** Ukryta sekcja właściciela — dla innych zwraca 404 (nie zdradza istnienia). */
export default async function AdminLayout({
  children,
  params,
}: {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  setRequestLocale(locale);
  let user;
  try {
    user = await requireAdmin();
  } catch (e) {
    if (e instanceof AccessError && e.code === "unauthorized")
      redirect(`${locale === "en" ? "/en" : ""}/login?callbackUrl=/admin`);
    notFound();
  }
  const t = await getTranslations("admin");
  return (
    <div className="min-h-dvh bg-bg">
      <Topbar user={user} />
      <div className="mx-auto max-w-6xl px-4 py-10 md:px-6">
        <h1 className="heading text-3xl text-white">{t("title")}</h1>
        <nav
          aria-label={t("title")}
          className="mt-6 flex gap-1 overflow-x-auto border-b border-border pb-px"
        >
          {tabs.map(([href, key]) => (
            <Link
              key={key}
              href={`/admin${href}`}
              className="shrink-0 rounded-t-md px-3 py-2 text-sm text-fg-muted hover:text-white"
            >
              {t(key)}
            </Link>
          ))}
        </nav>
        <main id="main" className="pt-8">
          {children}
        </main>
      </div>
    </div>
  );
}
