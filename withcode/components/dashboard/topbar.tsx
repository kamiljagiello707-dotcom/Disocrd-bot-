import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Logo } from "@/components/ui/logo";
import { LocaleSwitch } from "@/components/marketing/locale-switch";
import { signOutAction } from "@/lib/actions/auth";
import type { SessionUser } from "@/lib/permissions";

export async function Topbar({ user }: { user: SessionUser }) {
  const t = await getTranslations("common");
  return (
    <header className="sticky top-0 z-40 border-b border-border bg-black/80 backdrop-blur-xl">
      <div className="flex h-16 items-center justify-between gap-4 px-4 md:px-6">
        <Link href="/" aria-label="WithCode.pl">
          <Logo />
        </Link>
        <div className="flex items-center gap-3">
          <LocaleSwitch className="hidden sm:inline-flex" />
          {user.isAdmin ? (
            <Link
              href="/admin"
              className="hidden rounded-full border border-border px-3 py-1.5 text-xs text-fg-muted hover:text-white sm:inline-flex"
            >
              Admin
            </Link>
          ) : null}
          <span className="hidden text-sm text-fg-muted md:inline">{user.name}</span>
          <span className="flex size-8 items-center justify-center overflow-hidden rounded-full border border-border-strong bg-surface-3 text-xs text-white">
            {user.image ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={user.image} alt="" width={32} height={32} />
            ) : (
              (user.name ?? "?").slice(0, 1)
            )}
          </span>
          <form action={signOutAction}>
            <button type="submit" className="text-xs text-fg-subtle hover:text-white">
              {t("logout")}
            </button>
          </form>
        </div>
      </div>
    </header>
  );
}
