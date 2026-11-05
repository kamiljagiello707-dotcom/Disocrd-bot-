"use client";
import { useState } from "react";
import { useFormatter, useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { ChartMetric, Range, StatsPayload } from "@/lib/dashboard/stats";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

const METRICS: ChartMetric[] = ["AUTOMOD_DELETED", "PUNISHMENTS", "MEMBER_JOINS", "RAIDS"];

export function StatsChart({ guildId, initial }: { guildId: string; initial: StatsPayload }) {
  const t = useTranslations("dashboard.overview");
  const format = useFormatter();
  const [range, setRange] = useState<Range>(initial.range);
  const [metric, setMetric] = useState<ChartMetric>("AUTOMOD_DELETED");
  const { data, isFetching } = useQuery({
    queryKey: ["stats", guildId, range],
    queryFn: async () => {
      const r = await fetch(`/api/guilds/${guildId}/stats?range=${range}`);
      if (!r.ok) throw new Error("stats");
      return (await r.json()) as StatsPayload;
    },
    ...(range === initial.range ? { initialData: initial } : {}),
    placeholderData: (prev) => prev,
  });

  const delta = (m: ChartMetric) => {
    if (!data) return null;
    const prev = data.previous[m];
    if (prev === 0) return null;
    return Math.round(((data.totals[m] - prev) / prev) * 100);
  };

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <Tabs value={range} onValueChange={(v) => setRange(v as Range)}>
          <TabsList aria-label="Range">
            {(["24h", "7d", "30d"] as const).map((r) => (
              <TabsTrigger key={r} value={r}>
                {t(`range.${r}`)}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
        {isFetching ? <span className="text-xs text-fg-subtle">…</span> : null}
      </div>
      <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {METRICS.map((m) => {
          const d = delta(m);
          return (
            <li key={m}>
              <button
                type="button"
                onClick={() => setMetric(m)}
                aria-pressed={metric === m}
                className={cn(
                  "w-full rounded-xl border p-4 text-left transition-colors",
                  metric === m
                    ? "border-white/60 bg-white/[0.04]"
                    : "border-border bg-surface-1 hover:border-border-strong",
                )}
              >
                <p className="text-xs text-fg-subtle">{t(`metrics.${m}`)}</p>
                <p className="heading mt-2 text-3xl text-white tabular-nums">
                  {data ? format.number(data.totals[m]) : "—"}
                </p>
                {d !== null ? (
                  <p
                    className={cn(
                      "mt-1 text-xs",
                      d > 0 && m !== "MEMBER_JOINS" ? "text-warning" : "text-fg-muted",
                    )}
                  >
                    {d > 0 ? "+" : ""}
                    {d}% {t("vsPrevious")}
                  </p>
                ) : (
                  <p className="mt-1 text-xs text-transparent">.</p>
                )}
              </button>
            </li>
          );
        })}
      </ul>
      <div
        className="mt-4 h-72 rounded-xl border border-border bg-surface-1 p-4"
        role="img"
        aria-label={t(`metrics.${metric}`)}
      >
        {data ? (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data.points} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
              <defs>
                <linearGradient id="wc-area" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#ffffff" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#ffffff" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid stroke="rgba(255,255,255,0.06)" vertical={false} />
              <XAxis
                dataKey="t"
                tickLine={false}
                axisLine={false}
                tick={{ fill: "#6b6b6b", fontSize: 11 }}
                minTickGap={32}
                tickFormatter={(v: string) =>
                  format.dateTime(
                    new Date(v),
                    range === "24h"
                      ? { hour: "2-digit", minute: "2-digit" }
                      : { day: "numeric", month: "short" },
                  )
                }
              />
              <YAxis
                tickLine={false}
                axisLine={false}
                tick={{ fill: "#6b6b6b", fontSize: 11 }}
                allowDecimals={false}
                width={40}
              />
              <Tooltip
                cursor={{ stroke: "rgba(255,255,255,0.3)" }}
                contentStyle={{
                  background: "#141414",
                  border: "1px solid rgba(255,255,255,0.16)",
                  borderRadius: 10,
                  fontSize: 12,
                }}
                labelStyle={{ color: "#a1a1a1" }}
                itemStyle={{ color: "#fff" }}
                labelFormatter={(v) =>
                  format.dateTime(new Date(String(v)), { dateStyle: "medium", timeStyle: "short" })
                }
                formatter={(v) => [format.number(Number(v)), t(`metrics.${metric}`)]}
              />
              <Area
                type="monotone"
                dataKey={metric}
                stroke="#ffffff"
                strokeWidth={1.5}
                fill="url(#wc-area)"
                animationDuration={600}
              />
            </AreaChart>
          </ResponsiveContainer>
        ) : (
          <Skeleton className="h-full w-full" />
        )}
      </div>
    </div>
  );
}
