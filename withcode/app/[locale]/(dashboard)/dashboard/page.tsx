import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getFormatter, getTranslations, setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Topbar } from "@/components/dashboard/topbar";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { AccessError, getManageableGuilds, requireUser } from "@/lib/permissions";
import { guildIconUrl } from "@/lib/discord/rest";
import { inviteUrl } from "@/lib/utils";

export const metadata: Metadata = { title: "Panel", robots: { index: false } };

export default async function ServersPage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  setRequestLocale(locale);
  let user;
  try {
    user = await requireUser();
  } catch (e) {
    if (e instanceof AccessError) redirect(locale === "en" ? "/en/login" : "/login");
    throw e;
  }
  const t = await getTranslations("dashboard");
  const format = await getFormatter();
  const guilds = await getManageableGuilds();
  return (
    <>
      <Topbar user={user} />
      <main id="main" className="mx-auto max-w-6xl px-4 py-12 md:px-6">
        <h1 className="heading text-3xl text-white">{t("servers")}</h1>
        <p className="mt-2 text-sm text-fg-muted">{t("serversSubtitle")}</p>
        {guilds.length === 0 ? <p className="mt-10 text-fg-muted">{t("noServers")}</p> : null}
        <ul className="mt-10 grid gap-3 sm:grid-cols-2 lg:grid-cols-3" data-testid="guild-list">
          {guilds.map((g) => {
            const icon = guildIconUrl(g);
            return (
              <li
                key={g.id}
                className="flex flex-col rounded-xl border border-border bg-surface-1 p-5 transition-colors hover:border-border-strong"
              >
                <div className="flex items-center gap-3">
                  <span className="flex size-12 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-border-strong bg-surface-3 font-display text-lg text-white">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    {icon ? <img src={icon} alt="" width={48} height={48} /> : g.name.slice(0, 1)}
                  </span>
                  <div className="min-w-0">
                    <p className="truncate font-medium text-white">{g.name}</p>
                    {g.approximate_member_count ? (
                      <p className="text-xs text-fg-subtle">
                        {t("members", { count: format.number(g.approximate_member_count) })}
                      </p>
                    ) : null}
                  </div>
                </div>
                <div className="mt-5 flex items-center justify-between gap-2">
                  {g.botPresent ? (
                    <Badge variant="success">WithBot</Badge>
                  ) : (
                    <Badge>{t("botMissing")}</Badge>
                  )}
                  {g.botPresent ? (
                    <Button asChild size="sm" magnetic={false}>
                      <Link href={`/dashboard/${g.id}`}>{t("manage")}</Link>
                    </Button>
                  ) : (
                    <Button asChild size="sm" variant="outline" magnetic={false}>
                      <a href={inviteUrl(g.id)} target="_blank" rel="noopener noreferrer">
                        {t("invite")}
                      </a>
                    </Button>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      </main>
    </>
  );
}
