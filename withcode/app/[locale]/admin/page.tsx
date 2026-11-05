import { getFormatter, getTranslations } from "next-intl/server";
import { db } from "@/lib/db";
import { getBotApi } from "@/lib/bot-api";

const daysAgo = (n: number) => new Date(Date.now() - n * 864e5);

export default async function AdminOverview() {
  const t = await getTranslations("admin");
  const format = await getFormatter();
  const since = daysAgo(30);
  const [guilds, users, cases, incidents, status, members] = await Promise.all([
    db.guild.count({ where: { botPresent: true } }),
    db.user.count(),
    db.modCase.count({ where: { createdAt: { gte: since } } }),
    db.incident.count({ where: { status: { not: "RESOLVED" } } }),
    getBotApi().getStatus(),
    db.guild.aggregate({ _sum: { memberCount: true } }),
  ]);
  const cards = [
    [t("totalGuilds"), guilds],
    [t("totalUsers"), users],
    [t("totalCases"), cases],
    [t("openIncidents"), incidents],
    ["Members", members._sum.memberCount ?? 0],
    ["Ping (ms)", status.pingMs],
  ] as const;
  return (
    <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {cards.map(([label, value]) => (
        <li key={label} className="rounded-xl border border-border bg-surface-1 p-5">
          <p className="text-xs text-fg-subtle">{label}</p>
          <p className="heading mt-2 text-3xl text-white tabular-nums">{format.number(value)}</p>
        </li>
      ))}
    </ul>
  );
}
