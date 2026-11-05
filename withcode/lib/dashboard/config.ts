import "server-only";
import { db, type Prisma } from "@/lib/db";
import {
  autoModModules,
  defaultRule,
  moduleDefaults,
  moduleOptionsSchemas,
  type AutoModRuleInput,
} from "@/lib/validators/automod";
import {
  logEvents,
  type AntiRaidInput,
  type AccessInput,
  type GreetingInput,
  type LogsInput,
  type PunishmentsInput,
  type RolesInput,
  type SettingsInput,
  type VerificationInput,
} from "@/lib/validators/guild";

export async function ensureGuild(
  tx: Prisma.TransactionClient,
  guild: { id: string; name: string; icon: string | null },
) {
  await tx.guild.upsert({
    where: { id: guild.id },
    create: { id: guild.id, name: guild.name, icon: guild.icon, ownerId: "0" },
    update: {},
  });
}

export type AutoModFormValues = {
  rules: AutoModRuleInput[];
  bannedWords: string[];
  versions: Record<string, number>;
};

export async function loadAutoMod(guildId: string): Promise<AutoModFormValues> {
  const [rows, words] = await Promise.all([
    db.autoModRule.findMany({ where: { guildId } }),
    db.bannedWord.findMany({ where: { guildId }, orderBy: { createdAt: "asc" } }),
  ]);
  const versions: Record<string, number> = {};
  const rules = autoModModules.map((m) => {
    const row = rows.find((r) => r.module === m);
    versions[m] = row?.version ?? 1;
    if (!row) return defaultRule(m);
    const options = moduleOptionsSchemas[m].safeParse(row.options);
    return {
      module: m,
      enabled: row.enabled,
      sensitivity: row.sensitivity,
      actions: row.actions.length ? row.actions : ["DELETE"],
      muteDuration: row.muteDuration,
      exemptChannels: row.exemptChannels,
      exemptRoles: row.exemptRoles,
      options: options.success ? options.data : moduleDefaults[m],
    } as AutoModRuleInput;
  });
  return { rules, bannedWords: words.map((w) => w.pattern), versions };
}

export async function loadAntiRaid(
  guildId: string,
): Promise<AntiRaidInput & { lockdownActive: boolean; lockdownAt: string | null }> {
  const r = await db.antiRaidConfig.findUnique({ where: { guildId } });
  return {
    enabled: r?.enabled ?? false,
    joinThreshold: r?.joinThreshold ?? 10,
    joinWindowSec: r?.joinWindowSec ?? 10,
    minAccountAgeDays: r?.minAccountAgeDays ?? 0,
    action: r?.action === "BAN" || r?.action === "TIMEOUT" ? r.action : "KICK",
    version: r?.version ?? 1,
    lockdownActive: r?.lockdownActive ?? false,
    lockdownAt: r?.lockdownAt?.toISOString() ?? null,
  };
}

export async function loadPunishments(guildId: string): Promise<PunishmentsInput> {
  const rows = await db.punishmentThreshold.findMany({
    where: { guildId },
    orderBy: { warnCount: "asc" },
  });
  return {
    thresholds: rows.map((r) => ({
      warnCount: r.warnCount,
      action: r.action === "KICK" || r.action === "BAN" ? r.action : "TIMEOUT",
      duration: r.duration,
    })),
  };
}

export async function loadLogs(guildId: string): Promise<LogsInput> {
  const rows = await db.logChannel.findMany({ where: { guildId } });
  return {
    channels: Object.fromEntries(
      logEvents.map((e) => [
        e,
        rows.find((r) => r.eventType === e && r.enabled)?.channelId ?? null,
      ]),
    ) as LogsInput["channels"],
  };
}

export async function loadVerification(guildId: string): Promise<VerificationInput> {
  const v = await db.verificationConfig.findUnique({ where: { guildId } });
  return {
    enabled: v?.enabled ?? false,
    mode: v?.mode ?? "BUTTON",
    channelId: v?.channelId ?? null,
    roleId: v?.roleId ?? null,
    message: v?.message ?? null,
    version: v?.version ?? 1,
  };
}

export async function loadGreetings(
  guildId: string,
): Promise<{ welcome: GreetingInput; goodbye: GreetingInput }> {
  const rows = await db.greetingConfig.findMany({ where: { guildId } });
  const pick = (type: "WELCOME" | "GOODBYE"): GreetingInput => {
    const g = rows.find((r) => r.type === type);
    const embed = g?.embed as GreetingInput["embed"] | undefined;
    return {
      type,
      enabled: g?.enabled ?? false,
      channelId: g?.channelId ?? null,
      content:
        g?.content ??
        (type === "WELCOME"
          ? "Cześć {user}! Witamy na {server} — jesteś {memberCount}. osobą 🎉"
          : "{username} opuścił(a) serwer. Do zobaczenia!"),
      embed: embed ?? null,
      dm: g?.dm ?? false,
      version: g?.version ?? 1,
    };
  };
  return { welcome: pick("WELCOME"), goodbye: pick("GOODBYE") };
}

export async function loadRoles(guildId: string): Promise<RolesInput> {
  const [groups, autoRoles] = await Promise.all([
    db.reactionRoleMessage.findMany({
      where: { guildId },
      include: { entries: { orderBy: { position: "asc" } } },
      orderBy: { createdAt: "asc" },
    }),
    db.autoRole.findMany({ where: { guildId } }),
  ]);
  return {
    groups: groups.map((g) => ({
      id: g.id,
      channelId: g.channelId,
      title: g.title,
      mode: g.mode,
      style: g.style,
      entries: g.entries.map((e) => ({ emoji: e.emoji, label: e.label, roleId: e.roleId })),
    })),
    autoRoles: autoRoles.map((a) => ({
      roleId: a.roleId,
      delaySec: a.delaySec,
      botsOnly: a.botsOnly,
    })),
  };
}

export async function loadAccess(guildId: string): Promise<AccessInput> {
  const rows = await db.dashboardAccess.findMany({ where: { guildId } });
  return {
    entries: rows.map((r) => ({
      subjectType: r.subjectType,
      subjectId: r.subjectId,
      level: r.level,
    })),
  };
}

export async function loadSettings(guildId: string): Promise<SettingsInput> {
  const s = await db.guildSettings.findUnique({ where: { guildId } });
  const tz = s?.timezone as SettingsInput["timezone"] | undefined;
  return {
    prefix: s?.prefix ?? "!",
    locale: s?.locale ?? "PL",
    timezone: tz ?? "Europe/Warsaw",
    version: s?.version ?? 1,
  };
}
