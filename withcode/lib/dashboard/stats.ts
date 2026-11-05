import "server-only";
import { db } from "@/lib/db";

export const ranges = {
  "24h": { hours: 24, bucket: 1 },
  "7d": { hours: 168, bucket: 6 },
  "30d": { hours: 720, bucket: 24 },
} as const;
export type Range = keyof typeof ranges;
export const chartMetrics = ["AUTOMOD_DELETED", "PUNISHMENTS", "MEMBER_JOINS", "RAIDS"] as const;
export type ChartMetric = (typeof chartMetrics)[number];

export type StatsPayload = {
  range: Range;
  points: ({ t: string } & Record<ChartMetric, number>)[];
  totals: Record<ChartMetric, number>;
  previous: Record<ChartMetric, number>;
};

/** Agreguje godzinowe kubełki statystyk do punktów wykresu dla zakresu 24h/7d/30d. */
export async function getGuildStats(guildId: string, range: Range): Promise<StatsPayload> {
  const { hours, bucket } = ranges[range];
  const now = new Date();
  now.setMinutes(0, 0, 0);
  const from = new Date(now.getTime() - hours * 3600_000);
  const prevFrom = new Date(from.getTime() - hours * 3600_000);
  const rows = await db.guildStatBucket.findMany({
    where: { guildId, metric: { in: [...chartMetrics] }, bucketStart: { gte: prevFrom } },
  });
  const empty = () =>
    Object.fromEntries(chartMetrics.map((m) => [m, 0])) as Record<ChartMetric, number>;
  const slots = Math.ceil(hours / bucket);
  const points = Array.from({ length: slots }, (_, i) => ({
    t: new Date(from.getTime() + i * bucket * 3600_000).toISOString(),
    ...empty(),
  }));
  const totals = empty();
  const previous = empty();
  for (const r of rows) {
    const m = r.metric as ChartMetric;
    if (r.bucketStart < from) {
      previous[m] += r.value;
      continue;
    }
    const idx = Math.min(
      slots - 1,
      Math.floor((r.bucketStart.getTime() - from.getTime()) / (bucket * 3600_000)),
    );
    const p = points[idx];
    if (p) p[m] += r.value;
    totals[m] += r.value;
  }
  return { range, points, totals, previous };
}
