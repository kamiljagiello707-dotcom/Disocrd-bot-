import "server-only";
import { unstable_cache } from "next/cache";
import { db } from "@/lib/db";
import {
  seedChangelog,
  seedFaq,
  seedIncidents,
  seedPartners,
  seedRegions,
  seedTestimonials,
} from "@/content/seed-data";

const REVALIDATE = 300;

async function safe<T>(fn: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await fn();
  } catch (err) {
    if (process.env.NODE_ENV !== "production")
      console.warn("[content] DB unavailable, using seed data:", (err as Error).message);
    return fallback;
  }
}

export type FaqItem = {
  id: string;
  questionPl: string;
  questionEn: string;
  answerPl: string;
  answerEn: string;
};

export const getFaq = unstable_cache(
  async (scope: "HOME" | "PREMIUM"): Promise<FaqItem[]> =>
    safe<FaqItem[]>(
      async () => {
        const rows = await db.faq.findMany({
          where: { scope, published: true },
          orderBy: { position: "asc" },
        });
        if (rows.length === 0) throw new Error("empty");
        return rows;
      },
      seedFaq.filter((f) => f.scope === scope).map((f, i) => ({ id: `seed-${scope}-${i}`, ...f })),
    ),
  ["faq"],
  { tags: ["faq"], revalidate: REVALIDATE },
);

export type TestimonialItem = {
  id: string;
  author: string;
  rolePl: string;
  roleEn: string;
  quotePl: string;
  quoteEn: string;
  avatarUrl: string | null;
};

export const getTestimonials = unstable_cache(
  async (): Promise<TestimonialItem[]> =>
    safe<TestimonialItem[]>(
      async () => {
        const rows = await db.testimonial.findMany({
          where: { published: true },
          orderBy: { position: "asc" },
        });
        if (rows.length === 0) throw new Error("empty");
        return rows;
      },
      seedTestimonials.map((t, i) => ({ id: `seed-${i}`, avatarUrl: null, ...t })),
    ),
  ["testimonials"],
  { tags: ["testimonials"], revalidate: REVALIDATE },
);

export type ChangelogEntryView = {
  id: string;
  version: string;
  publishedAt: string;
  titlePl: string;
  titleEn: string;
  items: { id: string; tag: "NEW" | "FIX" | "CHANGE"; textPl: string; textEn: string }[];
};

export const getChangelog = unstable_cache(
  async (): Promise<ChangelogEntryView[]> =>
    safe<ChangelogEntryView[]>(
      async () => {
        const rows = await db.changelogEntry.findMany({
          where: { published: true },
          orderBy: { publishedAt: "desc" },
          include: { items: { orderBy: { position: "asc" } } },
        });
        if (rows.length === 0) throw new Error("empty");
        return rows.map((r) => ({ ...r, publishedAt: r.publishedAt.toISOString() }));
      },
      seedChangelog.map((e, i) => ({
        id: `seed-${i}`,
        version: e.version,
        publishedAt: new Date(e.publishedAt).toISOString(),
        titlePl: e.titlePl,
        titleEn: e.titleEn,
        items: e.items.map((it, j) => ({ id: `seed-${i}-${j}`, ...it })),
      })),
    ),
  ["changelog"],
  { tags: ["changelog"], revalidate: REVALIDATE },
);

export type PartnerItem = {
  id: string;
  name: string;
  iconUrl: string | null;
  inviteUrl: string | null;
  memberCount: number | null;
  descPl: string | null;
  descEn: string | null;
};

export const getPartners = unstable_cache(
  async (): Promise<PartnerItem[]> =>
    safe<PartnerItem[]>(
      async () => {
        const rows = await db.partner.findMany({
          where: { published: true },
          orderBy: { position: "asc" },
        });
        if (rows.length === 0) throw new Error("empty");
        return rows;
      },
      seedPartners.map((p, i) => ({ id: `seed-${i}`, iconUrl: null, ...p })),
    ),
  ["partners"],
  { tags: ["partners"], revalidate: REVALIDATE },
);

export type IncidentView = {
  id: string;
  titlePl: string;
  titleEn: string;
  severity: "MINOR" | "MAJOR" | "CRITICAL" | "MAINTENANCE";
  status: "INVESTIGATING" | "IDENTIFIED" | "MONITORING" | "RESOLVED";
  services: string[];
  startedAt: string;
  resolvedAt: string | null;
  updates: {
    id: string;
    status: IncidentView["status"];
    messagePl: string;
    messageEn: string;
    createdAt: string;
  }[];
};

export const getIncidents = unstable_cache(
  async (): Promise<IncidentView[]> =>
    safe<IncidentView[]>(
      async () => {
        const rows = await db.incident.findMany({
          orderBy: { startedAt: "desc" },
          take: 20,
          include: { updates: { orderBy: { createdAt: "desc" } } },
        });
        return rows.map((r) => ({
          ...r,
          startedAt: r.startedAt.toISOString(),
          resolvedAt: r.resolvedAt?.toISOString() ?? null,
          updates: r.updates.map((u) => ({ ...u, createdAt: u.createdAt.toISOString() })),
        }));
      },
      seedIncidents.map((inc, i) => ({
        id: `seed-${i}`,
        titlePl: inc.titlePl,
        titleEn: inc.titleEn,
        severity: inc.severity,
        status: inc.status,
        services: [...inc.services],
        startedAt: inc.startedAt,
        resolvedAt: inc.resolvedAt,
        updates: inc.updates.map((u, j) => ({ id: `seed-${i}-${j}`, ...u })),
      })),
    ),
  ["incidents"],
  { tags: ["incidents"], revalidate: 60 },
);

export type UptimeView = Record<string, { day: string; pct: number }[]>;

/** Pasek 90 dni: z tabeli UptimeDay, a dla brakujących dni — deterministyczne dane przykładowe. */
export const getUptime = unstable_cache(
  async (): Promise<UptimeView> => {
    const services = ["BOT", "API", "DATABASE", "SHARDS", "DASHBOARD"] as const;
    const days = Array.from({ length: 90 }, (_, i) => {
      const d = new Date();
      d.setUTCHours(0, 0, 0, 0);
      d.setUTCDate(d.getUTCDate() - (89 - i));
      return d;
    });
    const rows = await safe(
      () => db.uptimeDay.findMany({ where: { day: { gte: days[0] } } }),
      [] as { service: string; day: Date; uptimePct: number }[],
    );
    const out: UptimeView = {};
    for (const s of services) {
      out[s] = days.map((d, i) => {
        const hit = rows.find(
          (r) =>
            r.service === s && r.day.toISOString().slice(0, 10) === d.toISOString().slice(0, 10),
        );
        // dane przykładowe: stabilne 100% z dwoma dniami spadków (spójne z incydentami seed)
        const sample =
          (s === "SHARDS" || s === "BOT") && i === 66
            ? 98.4
            : s === "DATABASE" && i === 44
              ? 99.6
              : 100;
        return { day: d.toISOString().slice(0, 10), pct: hit ? hit.uptimePct : sample };
      });
    }
    return out;
  },
  ["uptime"],
  { tags: ["uptime"], revalidate: 300 },
);

export type PublicStats = {
  servers: number;
  users: number;
  threats: number;
  regions: typeof seedRegions;
  sample: boolean;
};

export const getPublicStats = unstable_cache(
  async (): Promise<PublicStats> =>
    safe<PublicStats>(
      async () => {
        const [servers, members, threats, regionRows] = await Promise.all([
          db.guild.count({ where: { botPresent: true } }),
          db.guild.aggregate({ _sum: { memberCount: true }, where: { botPresent: true } }),
          db.guildStatBucket.aggregate({
            _sum: { value: true },
            where: { metric: "AUTOMOD_DELETED" },
          }),
          db.guild.groupBy({
            by: ["region"],
            _count: { _all: true },
            where: { botPresent: true, region: { not: null } },
          }),
        ]);
        if (servers < 50) throw new Error("not enough real data");
        const regions = seedRegions
          .map((r) => ({
            ...r,
            count: regionRows.find((x) => x.region === r.region)?._count._all ?? 0,
          }))
          .filter((r) => r.count > 0);
        return {
          servers,
          users: members._sum.memberCount ?? 0,
          threats: threats._sum.value ?? 0,
          regions,
          sample: false,
        };
      },
      {
        servers: 18_420,
        users: 4_870_000,
        threats: 12_600_000,
        regions: seedRegions,
        sample: true,
      },
    ),
  ["public-stats"],
  { tags: ["stats"], revalidate: 600 },
);
