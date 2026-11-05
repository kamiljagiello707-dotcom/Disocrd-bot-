import type { Metadata } from "next";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { redirect } from "next/navigation";
import { Link } from "@/i18n/navigation";
import { Logo } from "@/components/ui/logo";
import { Button } from "@/components/ui/button";
import { Discord } from "@/components/ui/icons";
import { signInWithDiscord } from "@/lib/actions/auth";
import { getOptionalUser } from "@/lib/permissions";
import { devLoginEnabled } from "@/lib/auth/dev";
import { pageMetadata } from "@/lib/seo/metadata";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  return pageMetadata({ locale, path: "/login", titleKey: "login", noindex: true });
}

export default async function LoginPage({
  params,
  searchParams,
}: {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{ callbackUrl?: string; error?: string }>;
}) {
  const { locale } = await params;
  const { callbackUrl, error } = await searchParams;
  setRequestLocale(locale);
  const user = await getOptionalUser();
  const target =
    callbackUrl && callbackUrl.startsWith("/") && !callbackUrl.startsWith("//")
      ? callbackUrl
      : locale === "en"
        ? "/en/dashboard"
        : "/dashboard";
  if (user) redirect(target);
  const t = await getTranslations("auth");
  return (
    <main
      id="main"
      className="relative flex min-h-dvh items-center justify-center overflow-hidden px-4"
    >
      <div
        aria-hidden
        className="absolute inset-0 bg-[radial-gradient(40%_40%_at_50%_40%,rgba(255,255,255,0.08),transparent_70%)]"
      />
      <div className="glass relative w-full max-w-md rounded-2xl p-8">
        <Link href="/" className="inline-flex">
          <Logo />
        </Link>
        <h1 className="heading mt-10 text-3xl text-white">{t("title")}</h1>
        <p className="mt-3 text-sm leading-relaxed text-fg-muted">{t("subtitle")}</p>
        {error ? (
          <p
            role="alert"
            className="mt-5 rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-sm text-danger"
          >
            {t("error")}
          </p>
        ) : null}
        <form action={signInWithDiscord} className="mt-8">
          <input type="hidden" name="callbackUrl" value={target} />
          <Button type="submit" size="lg" className="w-full">
            <Discord className="size-5" /> {t("button")}
          </Button>
        </form>
        <p className="mt-4 text-center font-mono text-[11px] text-fg-subtle">{t("scopes")}</p>
        {devLoginEnabled() ? (
          <a
            href={`/api/dev/login?callbackUrl=${encodeURIComponent(target)}`}
            data-testid="dev-login"
            className="mt-6 block rounded-md border border-dashed border-border-strong px-3 py-2 text-center text-xs text-fg-muted hover:text-white"
          >
            {t("devLogin")}
          </a>
        ) : null}
      </div>
    </main>
  );
}
