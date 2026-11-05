import { getFormatter, getTranslations } from "next-intl/server";
import { db } from "@/lib/db";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { inputCls } from "@/components/admin/ui";

export default async function AdminGuilds({
  searchParams,
}: {
  searchParams: Promise<{ q?: string }>;
}) {
  const { q } = await searchParams;
  const t = await getTranslations("admin");
  const format = await getFormatter();
  const query = q?.trim().slice(0, 100);
  const guilds = await db.guild.findMany({
    where: query
      ? { OR: [{ name: { contains: query, mode: "insensitive" } }, { id: { contains: query } }] }
      : {},
    orderBy: { memberCount: "desc" },
    take: 100,
  });
  return (
    <>
      <form className="mb-4 max-w-md">
        <input
          name="q"
          defaultValue={query}
          placeholder={t("search")}
          aria-label={t("search")}
          className={inputCls}
        />
      </form>
      <Table>
        <THead>
          <tr>
            <TH>{t("name")}</TH>
            <TH>ID</TH>
            <TH>{t("members")}</TH>
            <TH>Plan</TH>
            <TH>Bot</TH>
          </tr>
        </THead>
        <TBody>
          {guilds.map((g) => (
            <TR key={g.id}>
              <TD className="text-white">{g.name}</TD>
              <TD className="font-mono text-xs">{g.id}</TD>
              <TD>{format.number(g.memberCount)}</TD>
              <TD>
                <Badge>{g.premiumTier}</Badge>
              </TD>
              <TD>{g.botPresent ? <Badge variant="success">ON</Badge> : <Badge>OFF</Badge>}</TD>
            </TR>
          ))}
        </TBody>
      </Table>
    </>
  );
}
