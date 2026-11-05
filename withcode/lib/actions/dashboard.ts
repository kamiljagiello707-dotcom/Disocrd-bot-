"use server";
import { z } from "zod";
import type { Prisma } from "@/lib/db";
import { autoModConfigSchema } from "@/lib/validators/automod";
import {
  accessSchema,
  antiRaidSchema,
  greetingSchema,
  lockdownSchema,
  logEvents,
  logsSchema,
  punishmentsSchema,
  revokeCaseSchema,
  rolesSchema,
  settingsSchema,
  verificationSchema,
} from "@/lib/validators/guild";
import { sanitizeUserText } from "@/lib/security/sanitize";
import { getBotApi } from "@/lib/bot-api";
import {
  ensureGuild,
  loadAccess,
  loadAntiRaid,
  loadAutoMod,
  loadGreetings,
  loadLogs,
  loadPunishments,
  loadRoles,
  loadSettings,
  loadVerification,
} from "@/lib/dashboard/config";
import { assertVersion, ConflictError, guildAction } from "./guild-action";
import type { ActionResult } from "./result";

export async function saveAutoMod(
  guildId: string,
  input: unknown,
): Promise<ActionResult<{ versions: Record<string, number> }>> {
  return guildAction(
    guildId,
    autoModConfigSchema,
    input,
    { section: "automod" },
    async ({ tx, access, data }) => {
      const before = await loadAutoMod(guildId);
      await ensureGuild(tx, access.guild);
      const versions: Record<string, number> = {};
      for (const rule of data.rules) {
        const current = await tx.autoModRule.findUnique({
          where: { guildId_module: { guildId, module: rule.module } },
        });
        assertVersion(current?.version, data.versions[rule.module] ?? 1);
        const payload = {
          enabled: rule.enabled,
          sensitivity: rule.sensitivity,
          actions: rule.actions,
          muteDuration: rule.actions.includes("TIMEOUT") ? rule.muteDuration : null,
          exemptChannels: rule.exemptChannels,
          exemptRoles: rule.exemptRoles,
          options: rule.options as Prisma.InputJsonValue,
        };
        const saved = await tx.autoModRule.upsert({
          where: { guildId_module: { guildId, module: rule.module } },
          create: { guildId, module: rule.module, ...payload },
          update: { ...payload, version: { increment: 1 } },
        });
        versions[rule.module] = saved.version;
      }
      const words = [...new Set(data.bannedWords)];
      await tx.bannedWord.deleteMany({ where: { guildId, pattern: { notIn: words } } });
      if (words.length) {
        await tx.bannedWord.createMany({
          data: words.map((pattern) => ({
            guildId,
            pattern,
            matchType: pattern.includes("*") ? "WILDCARD" : "CONTAINS",
            createdBy: access.user.discordId,
          })),
          skipDuplicates: true,
        });
      }
      return {
        result: { versions },
        before: { rules: before.rules, bannedWords: before.bannedWords },
        after: { rules: data.rules, bannedWords: words },
      };
    },
  );
}

export async function saveAntiRaid(
  guildId: string,
  input: unknown,
): Promise<ActionResult<{ version: number }>> {
  return guildAction(
    guildId,
    antiRaidSchema,
    input,
    { section: "antiraid" },
    async ({ tx, access, data }) => {
      const before = await loadAntiRaid(guildId);
      await ensureGuild(tx, access.guild);
      const current = await tx.antiRaidConfig.findUnique({ where: { guildId } });
      assertVersion(current?.version, data.version);
      const { version: _v, ...fields } = data;
      const saved = await tx.antiRaidConfig.upsert({
        where: { guildId },
        create: { guildId, ...fields },
        update: { ...fields, version: { increment: 1 } },
      });
      const { lockdownActive: _a, lockdownAt: _b, version: _c, ...beforeFields } = before;
      return { result: { version: saved.version }, before: beforeFields, after: fields };
    },
  );
}

export async function setLockdown(
  guildId: string,
  input: unknown,
): Promise<ActionResult<{ active: boolean }>> {
  return guildAction(
    guildId,
    lockdownSchema,
    input,
    { section: "antiraid", auditSection: "antiraid.lockdown" },
    async ({ tx, access, data }) => {
      await ensureGuild(tx, access.guild);
      const current = await tx.antiRaidConfig.findUnique({ where: { guildId } });
      await getBotApi().setLockdown(guildId, data.active, access.user.discordId);
      return {
        result: { active: data.active },
        before: { lockdownActive: current?.lockdownActive ?? false },
        after: { lockdownActive: data.active },
      };
    },
  );
}

export async function savePunishments(guildId: string, input: unknown): Promise<ActionResult> {
  return guildAction(
    guildId,
    punishmentsSchema,
    input,
    { section: "punishments" },
    async ({ tx, access, data }) => {
      const before = await loadPunishments(guildId);
      await ensureGuild(tx, access.guild);
      await tx.punishmentThreshold.deleteMany({ where: { guildId } });
      if (data.thresholds.length) {
        await tx.punishmentThreshold.createMany({
          data: data.thresholds.map((t) => ({
            guildId,
            warnCount: t.warnCount,
            action: t.action,
            duration: t.action === "TIMEOUT" ? t.duration : null,
          })),
        });
      }
      const sorted = [...data.thresholds].sort((a, b) => a.warnCount - b.warnCount);
      return { result: undefined, before, after: { thresholds: sorted } };
    },
  );
}

export async function saveLogs(guildId: string, input: unknown): Promise<ActionResult> {
  return guildAction(
    guildId,
    logsSchema,
    input,
    { section: "logs" },
    async ({ tx, access, data }) => {
      const before = await loadLogs(guildId);
      await ensureGuild(tx, access.guild);
      for (const eventType of logEvents) {
        const channelId = data.channels[eventType] ?? null;
        await tx.logChannel.upsert({
          where: { guildId_eventType: { guildId, eventType } },
          create: { guildId, eventType, channelId, enabled: channelId !== null },
          update: { channelId, enabled: channelId !== null },
        });
      }
      return { result: undefined, before, after: data };
    },
  );
}

export async function saveVerification(
  guildId: string,
  input: unknown,
): Promise<ActionResult<{ version: number }>> {
  return guildAction(
    guildId,
    verificationSchema,
    input,
    { section: "verification" },
    async ({ tx, access, data }) => {
      const before = await loadVerification(guildId);
      await ensureGuild(tx, access.guild);
      const current = await tx.verificationConfig.findUnique({ where: { guildId } });
      assertVersion(current?.version, data.version);
      const { version: _v, ...fields } = data;
      const clean = {
        ...fields,
        message: fields.message ? sanitizeUserText(fields.message) : null,
      };
      const saved = await tx.verificationConfig.upsert({
        where: { guildId },
        create: { guildId, ...clean },
        update: { ...clean, version: { increment: 1 } },
      });
      const { version: _bv, ...beforeFields } = before;
      return { result: { version: saved.version }, before: beforeFields, after: clean };
    },
  );
}

const greetingsSchema = z.object({ welcome: greetingSchema, goodbye: greetingSchema });

export async function saveGreetings(
  guildId: string,
  input: unknown,
): Promise<ActionResult<{ versions: { WELCOME: number; GOODBYE: number } }>> {
  return guildAction(
    guildId,
    greetingsSchema,
    input,
    { section: "welcome" },
    async ({ tx, access, data }) => {
      const before = await loadGreetings(guildId);
      await ensureGuild(tx, access.guild);
      const versions = { WELCOME: 1, GOODBYE: 1 };
      for (const g of [data.welcome, data.goodbye]) {
        const current = await tx.greetingConfig.findUnique({
          where: { guildId_type: { guildId, type: g.type } },
        });
        assertVersion(current?.version, g.version);
        const fields = {
          enabled: g.enabled,
          channelId: g.channelId,
          dm: g.dm,
          content: g.content ? sanitizeUserText(g.content) : null,
          embed: g.embed
            ? ({
                title: g.embed.title ? sanitizeUserText(g.embed.title) : null,
                description: g.embed.description ? sanitizeUserText(g.embed.description) : null,
                imageUrl: g.embed.imageUrl,
              } as Prisma.InputJsonValue)
            : undefined,
        };
        const saved = await tx.greetingConfig.upsert({
          where: { guildId_type: { guildId, type: g.type } },
          create: { guildId, type: g.type, ...fields },
          update: { ...fields, version: { increment: 1 } },
        });
        versions[g.type] = saved.version;
      }
      const strip = (g: typeof data.welcome) => ({
        enabled: g.enabled,
        channelId: g.channelId,
        content: g.content,
        embed: g.embed,
        dm: g.dm,
      });
      return {
        result: { versions },
        before: { welcome: strip(before.welcome), goodbye: strip(before.goodbye) },
        after: { welcome: strip(data.welcome), goodbye: strip(data.goodbye) },
      };
    },
  );
}

export async function saveRoles(guildId: string, input: unknown): Promise<ActionResult> {
  return guildAction(
    guildId,
    rolesSchema,
    input,
    { section: "roles" },
    async ({ tx, access, data }) => {
      const before = await loadRoles(guildId);
      await ensureGuild(tx, access.guild);
      const keepIds = data.groups.map((g) => g.id).filter((id): id is string => Boolean(id));
      await tx.reactionRoleMessage.deleteMany({ where: { guildId, id: { notIn: keepIds } } });
      for (const g of data.groups) {
        const fields = {
          channelId: g.channelId,
          title: sanitizeUserText(g.title),
          mode: g.mode,
          style: g.style,
        };
        const entries = g.entries.map((e, position) => ({
          emoji: e.emoji,
          label: e.label ? sanitizeUserText(e.label) : null,
          roleId: e.roleId,
          position,
        }));
        if (g.id) {
          const existing = await tx.reactionRoleMessage.findFirst({ where: { id: g.id, guildId } });
          if (!existing) throw new ConflictError();
          await tx.reactionRoleMessage.update({ where: { id: g.id }, data: fields });
          await tx.reactionRoleEntry.deleteMany({ where: { groupId: g.id } });
          await tx.reactionRoleEntry.createMany({
            data: entries.map((e) => ({ ...e, groupId: g.id as string })),
          });
        } else {
          await tx.reactionRoleMessage.create({
            data: { guildId, ...fields, entries: { create: entries } },
          });
        }
      }
      await tx.autoRole.deleteMany({ where: { guildId } });
      if (data.autoRoles.length) {
        const unique = [...new Map(data.autoRoles.map((a) => [a.roleId, a])).values()];
        await tx.autoRole.createMany({ data: unique.map((a) => ({ guildId, ...a })) });
      }
      const strip = (r: typeof data) => ({
        groups: r.groups.map(({ id: _id, ...g }) => g),
        autoRoles: r.autoRoles,
      });
      return { result: undefined, before: strip(before), after: strip(data) };
    },
  );
}

export async function saveAccess(guildId: string, input: unknown): Promise<ActionResult> {
  return guildAction(
    guildId,
    accessSchema,
    input,
    { section: "access", requireAccessManage: true },
    async ({ tx, access, data }) => {
      const before = await loadAccess(guildId);
      await ensureGuild(tx, access.guild);
      await tx.dashboardAccess.deleteMany({ where: { guildId } });
      const unique = [
        ...new Map(data.entries.map((e) => [`${e.subjectType}:${e.subjectId}`, e])).values(),
      ];
      if (unique.length)
        await tx.dashboardAccess.createMany({ data: unique.map((e) => ({ guildId, ...e })) });
      return { result: undefined, before, after: { entries: unique } };
    },
  );
}

export async function saveSettings(
  guildId: string,
  input: unknown,
): Promise<ActionResult<{ version: number }>> {
  return guildAction(
    guildId,
    settingsSchema,
    input,
    { section: "settings" },
    async ({ tx, access, data }) => {
      const before = await loadSettings(guildId);
      await ensureGuild(tx, access.guild);
      const current = await tx.guildSettings.findUnique({ where: { guildId } });
      assertVersion(current?.version, data.version);
      const { version: _v, ...fields } = data;
      const saved = await tx.guildSettings.upsert({
        where: { guildId },
        create: { guildId, ...fields },
        update: { ...fields, version: { increment: 1 } },
      });
      const { version: _bv, ...beforeFields } = before;
      return { result: { version: saved.version }, before: beforeFields, after: fields };
    },
  );
}

export async function revokeCase(guildId: string, input: unknown): Promise<ActionResult> {
  return guildAction(
    guildId,
    revokeCaseSchema,
    input,
    { section: "punishments", auditSection: "cases.revoke" },
    async ({ tx, access, data }) => {
      const c = await tx.modCase.findFirst({ where: { id: data.caseId, guildId } });
      if (!c || c.revokedAt) throw new ConflictError();
      await tx.modCase.update({
        where: { id: c.id },
        data: {
          revokedAt: new Date(),
          revokedBy: access.user.discordId,
          revokeReason: data.reason ? sanitizeUserText(data.reason) : null,
        },
      });
      await getBotApi().revokeCase(guildId, c.id, access.user.discordId);
      return {
        result: undefined,
        before: { case: c.caseNumber, revoked: false },
        after: { case: c.caseNumber, revoked: true },
      };
    },
  );
}
