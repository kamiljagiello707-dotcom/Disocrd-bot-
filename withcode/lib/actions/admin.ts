"use server";
import { z } from "zod";
import { revalidatePath, revalidateTag } from "next/cache";
import { db } from "@/lib/db";
import { requireAdmin } from "@/lib/permissions";
import { sanitizeUserText } from "@/lib/security/sanitize";

/**
 * Akcje administracyjne (właściciel WithBota). Każda zaczyna się od requireAdmin() —
 * ID Discorda z ADMIN_DISCORD_IDS. Formularze działają także bez JS (FormData).
 */
const text = (max: number) =>
  z
    .string()
    .trim()
    .min(1)
    .max(max)
    .transform((v) => sanitizeUserText(v, { allowMassMentions: true }));
const optText = (max: number) =>
  z
    .string()
    .trim()
    .max(max)
    .optional()
    .transform((v) => (v ? sanitizeUserText(v) : null));
const snowflake = z.string().regex(/^\d{17,20}$/);
const fd = (f: FormData) =>
  Object.fromEntries([...f.entries()].map(([k, v]) => [k, typeof v === "string" ? v : ""]));

function done(tag: string, path: string) {
  revalidateTag(tag, "max");
  revalidatePath(path);
}

export async function adminDelete(formData: FormData) {
  await requireAdmin();
  const { entity, id } = z
    .object({
      entity: z.enum(["changelog", "incident", "faq", "testimonial", "partner", "blacklist"]),
      id: z.string().min(1),
    })
    .parse(fd(formData));
  switch (entity) {
    case "changelog":
      await db.changelogEntry.delete({ where: { id } });
      return done("changelog", "/[locale]/admin/changelog");
    case "incident":
      await db.incident.delete({ where: { id } });
      return done("incidents", "/[locale]/admin/incidents");
    case "faq":
      await db.faq.delete({ where: { id } });
      return done("faq", "/[locale]/admin/faq");
    case "testimonial":
      await db.testimonial.delete({ where: { id } });
      return done("testimonials", "/[locale]/admin/testimonials");
    case "partner":
      await db.partner.delete({ where: { id } });
      return done("partners", "/[locale]/admin/partners");
    case "blacklist":
      await db.blacklistEntry.delete({ where: { id } });
      return done("blacklist", "/[locale]/admin/blacklist");
  }
}

export async function createChangelog(formData: FormData) {
  await requireAdmin();
  const d = z
    .object({
      version: z.string().regex(/^\d+\.\d+\.\d+$/),
      titlePl: text(200),
      titleEn: text(200),
      items: z.string().max(10000),
    })
    .parse(fd(formData));
  const items = d.items
    .split("\n")
    .map((line) => line.split("|").map((s) => s.trim()))
    .filter((p) => p.length === 3 && ["NEW", "FIX", "CHANGE"].includes(p[0] ?? ""))
    .map(([tag, textPl, textEn], position) => ({
      tag: tag as "NEW" | "FIX" | "CHANGE",
      textPl: sanitizeUserText(textPl ?? ""),
      textEn: sanitizeUserText(textEn ?? ""),
      position,
    }));
  await db.changelogEntry.create({
    data: {
      version: d.version,
      titlePl: d.titlePl,
      titleEn: d.titleEn,
      publishedAt: new Date(),
      items: { create: items },
    },
  });
  done("changelog", "/[locale]/admin/changelog");
}

export async function createIncident(formData: FormData) {
  await requireAdmin();
  const d = z
    .object({
      titlePl: text(200),
      titleEn: text(200),
      severity: z.enum(["MINOR", "MAJOR", "CRITICAL", "MAINTENANCE"]),
      services: z.string().default("BOT"),
      messagePl: text(1000),
      messageEn: text(1000),
    })
    .parse(fd(formData));
  const services = d.services
    .split(",")
    .filter((s): s is "BOT" | "API" | "DATABASE" | "SHARDS" | "DASHBOARD" =>
      ["BOT", "API", "DATABASE", "SHARDS", "DASHBOARD"].includes(s),
    );
  await db.incident.create({
    data: {
      titlePl: d.titlePl,
      titleEn: d.titleEn,
      severity: d.severity,
      status: "INVESTIGATING",
      services,
      startedAt: new Date(),
      updates: {
        create: { status: "INVESTIGATING", messagePl: d.messagePl, messageEn: d.messageEn },
      },
    },
  });
  done("incidents", "/[locale]/admin/incidents");
}

export async function addIncidentUpdate(formData: FormData) {
  await requireAdmin();
  const d = z
    .object({
      id: z.string().min(1),
      status: z.enum(["INVESTIGATING", "IDENTIFIED", "MONITORING", "RESOLVED"]),
      messagePl: text(1000),
      messageEn: text(1000),
    })
    .parse(fd(formData));
  await db.incident.update({
    where: { id: d.id },
    data: {
      status: d.status,
      resolvedAt: d.status === "RESOLVED" ? new Date() : null,
      updates: { create: { status: d.status, messagePl: d.messagePl, messageEn: d.messageEn } },
    },
  });
  done("incidents", "/[locale]/admin/incidents");
}

export async function createFaq(formData: FormData) {
  await requireAdmin();
  const d = z
    .object({
      scope: z.enum(["HOME", "PREMIUM"]),
      questionPl: text(300),
      questionEn: text(300),
      answerPl: text(3000),
      answerEn: text(3000),
    })
    .parse(fd(formData));
  const position = await db.faq.count({ where: { scope: d.scope } });
  await db.faq.create({ data: { ...d, position } });
  done("faq", "/[locale]/admin/faq");
}

export async function createTestimonial(formData: FormData) {
  await requireAdmin();
  const d = z
    .object({
      author: text(100),
      rolePl: text(200),
      roleEn: text(200),
      quotePl: text(1000),
      quoteEn: text(1000),
    })
    .parse(fd(formData));
  const position = await db.testimonial.count();
  await db.testimonial.create({ data: { ...d, position } });
  done("testimonials", "/[locale]/admin/testimonials");
}

export async function createPartner(formData: FormData) {
  await requireAdmin();
  const d = z
    .object({
      name: text(100),
      inviteUrl: z.string().trim().url().startsWith("https://").optional().or(z.literal("")),
      memberCount: z.coerce.number().int().min(0).max(10_000_000).optional(),
      descPl: optText(300),
      descEn: optText(300),
    })
    .parse(fd(formData));
  const position = await db.partner.count();
  await db.partner.create({
    data: {
      name: d.name,
      inviteUrl: d.inviteUrl || null,
      memberCount: d.memberCount ?? null,
      descPl: d.descPl,
      descEn: d.descEn,
      position,
    },
  });
  done("partners", "/[locale]/admin/partners");
}

export async function addBlacklist(formData: FormData) {
  const admin = await requireAdmin();
  const d = z
    .object({ type: z.enum(["GUILD", "USER"]), targetId: snowflake, reason: optText(500) })
    .parse(fd(formData));
  await db.blacklistEntry.upsert({
    where: { type_targetId: { type: d.type, targetId: d.targetId } },
    create: { type: d.type, targetId: d.targetId, reason: d.reason, createdBy: admin.discordId },
    update: { reason: d.reason },
  });
  done("blacklist", "/[locale]/admin/blacklist");
}
