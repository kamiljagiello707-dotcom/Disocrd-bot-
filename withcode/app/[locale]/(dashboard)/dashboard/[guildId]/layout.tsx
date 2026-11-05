import type { ReactNode } from "react";
import { notFound, redirect } from "next/navigation";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { Topbar } from "@/components/dashboard/topbar";
import { Sidebar } from "@/components/dashboard/sidebar";
import { GuildResourcesProvider } from "@/components/dashboard/resources";
import { AccessError, getGuildAccess } from "@/lib/permissions";
import { assignableRoles, getGuildChannels, getGuildRoles, textChannels } from "@/lib/discord/rest";

export default async function GuildLayout({
  children,
  params,
}: {
  children: ReactNode;
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  let access;
  try {
    access = await getGuildAccess(guildId);
  } catch (e) {
    if (e instanceof AccessError) {
      if (e.code === "unauthorized")
        redirect(
          `${locale === "en" ? "/en" : ""}/login?callbackUrl=${encodeURIComponent(`/dashboard/${guildId}`)}`,
        );
      if (e.code === "not_found") notFound();
      const t = await getTranslations("dashboard");
      return (
        <main className="flex min-h-dvh items-center justify-center p-6 text-center">
          <p className="text-fg-muted">{t("forbidden")}</p>
        </main>
      );
    }
    throw e;
  }
  const [channels, roles] = await Promise.all([
    getGuildChannels(guildId).catch(() => []),
    getGuildRoles(guildId).catch(() => []),
  ]);
  return (
    <>
      <Topbar user={access.user} />
      <div className="flex">
        <Sidebar guildId={guildId} guildName={access.guild.name} />
        <GuildResourcesProvider
          initial={{
            guildId,
            guildName: access.guild.name,
            memberCount: access.guild.approximate_member_count ?? 0,
            channels: textChannels(channels).map((c) => ({ id: c.id, name: c.name })),
            roles: assignableRoles(roles).map((r) => ({ id: r.id, name: r.name })),
          }}
        >
          <main id="main" className="min-w-0 flex-1 px-4 pt-8 pb-32 md:px-8 lg:px-10">
            {children}
          </main>
        </GuildResourcesProvider>
      </div>
    </>
  );
}
