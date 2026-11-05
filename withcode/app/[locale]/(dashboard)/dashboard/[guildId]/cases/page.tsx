import { Suspense } from "react";
import { getFormatter, getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader } from "@/components/dashboard/page-header";
import { CasesFilters } from "@/components/dashboard/cases-filters";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/table";
import { Pagination } from "@/components/ui/pagination";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Download } from "@/components/ui/icons";
import { Link } from "@/i18n/navigation";
import { casesQuerySchema } from "@/lib/validators/guild";
import { listCases } from "@/lib/dashboard/cases";

export default async function CasesPage({
  params,
  searchParams,
}: {
  params: Promise<{ locale: string; guildId: string }>;
  searchParams: Promise<Record<string, string | undefined>>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const sp = await searchParams;
  const q = casesQuerySchema.safeParse(sp).data ?? casesQuerySchema.parse({});
  const t = await getTranslations("dashboard.cases");
  const tc = await getTranslations("common");
  const format = await getFormatter();
  const { rows, total, pages } = await listCases(guildId, q);
  const qs = new URLSearchParams(
    Object.entries(sp).filter(
      (e): e is [string, string] => typeof e[1] === "string" && e[0] !== "page",
    ),
  );
  const base = `${locale === "en" ? "/en" : ""}/dashboard/${guildId}/cases`;
  return (
    <>
      <DashHeader
        title={t("title")}
        subtitle={`${t("subtitle")} (${format.number(total)})`}
        actions={
          <Button asChild variant="outline" size="sm" magnetic={false}>
            <a href={`/api/guilds/${guildId}/cases/export?${qs.toString()}`} download>
              <Download className="size-4" /> {t("export")}
            </a>
          </Button>
        }
      />
      <Suspense fallback={null}>
        <CasesFilters />
      </Suspense>
      <Table>
        <THead>
          <tr>
            <TH>{t("number")}</TH>
            <TH>{t("type")}</TH>
            <TH>{t("user")}</TH>
            <TH>{t("reason")}</TH>
            <TH>{t("moderator")}</TH>
            <TH>{t("date")}</TH>
          </tr>
        </THead>
        <TBody>
          {rows.length === 0 ? (
            <tr>
              <TD colSpan={6} className="py-10 text-center">
                {t("empty")}
              </TD>
            </tr>
          ) : null}
          {rows.map((c) => (
            <TR key={c.id} className={c.revokedAt ? "opacity-50" : undefined}>
              <TD className="font-mono text-xs">
                <Link
                  href={`/dashboard/${guildId}/cases/${c.id}`}
                  className="text-white underline-offset-4 hover:underline"
                >
                  #{c.caseNumber}
                </Link>
              </TD>
              <TD>
                <Badge
                  variant={
                    c.type === "BAN" || c.type === "KICK"
                      ? "danger"
                      : c.type === "TIMEOUT"
                        ? "warning"
                        : "default"
                  }
                >
                  {t(`types.${c.type}`)}
                </Badge>
                {c.revokedAt ? <span className="ml-2 text-xs">{t("revoked")}</span> : null}
              </TD>
              <TD className="text-white">{c.targetTag ?? c.targetId}</TD>
              <TD className="max-w-xs truncate">{c.reason ?? t("noReason")}</TD>
              <TD>
                {c.moderatorTag ??
                  (c.source === "MANUAL" ? c.moderatorId : t(`sources.${c.source}`))}
              </TD>
              <TD className="text-xs whitespace-nowrap">
                {format.dateTime(c.createdAt, { dateStyle: "medium", timeStyle: "short" })}
              </TD>
            </TR>
          ))}
        </TBody>
      </Table>
      <Pagination
        page={q.page}
        totalPages={pages}
        hrefFor={(p) => {
          const n = new URLSearchParams(qs);
          n.set("page", String(p));
          return `${base}?${n.toString()}`;
        }}
        labels={{
          previous: tc("previous"),
          next: tc("next"),
          page: tc("page", { page: q.page, total: pages }),
        }}
      />
    </>
  );
}
