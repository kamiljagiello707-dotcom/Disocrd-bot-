-- CreateEnum
CREATE TYPE "PremiumTier" AS ENUM ('FREE', 'PREMIUM', 'PRO');

-- CreateEnum
CREATE TYPE "BotLocale" AS ENUM ('PL', 'EN');

-- CreateEnum
CREATE TYPE "AutoModModule" AS ENUM ('ANTI_SPAM', 'ANTI_LINK', 'ANTI_INVITE', 'WORD_FILTER', 'ANTI_CAPS', 'ANTI_MENTION');

-- CreateEnum
CREATE TYPE "ModAction" AS ENUM ('DELETE', 'WARN', 'TIMEOUT', 'KICK', 'BAN');

-- CreateEnum
CREATE TYPE "WordMatch" AS ENUM ('EXACT', 'CONTAINS', 'WILDCARD');

-- CreateEnum
CREATE TYPE "LogEvent" AS ENUM ('MESSAGE_DELETE', 'MESSAGE_EDIT', 'MEMBER_JOIN', 'MEMBER_LEAVE', 'MEMBER_BAN', 'MEMBER_UNBAN', 'ROLE_UPDATE', 'NICK_UPDATE', 'CHANNEL_UPDATE', 'VOICE', 'MOD_ACTION', 'AUTOMOD');

-- CreateEnum
CREATE TYPE "VerificationMode" AS ENUM ('BUTTON', 'CAPTCHA');

-- CreateEnum
CREATE TYPE "GreetingType" AS ENUM ('WELCOME', 'GOODBYE');

-- CreateEnum
CREATE TYPE "ReactionRoleMode" AS ENUM ('NORMAL', 'UNIQUE', 'VERIFY');

-- CreateEnum
CREATE TYPE "ReactionRoleStyle" AS ENUM ('REACTIONS', 'BUTTONS', 'SELECT');

-- CreateEnum
CREATE TYPE "AccessSubject" AS ENUM ('USER', 'ROLE');

-- CreateEnum
CREATE TYPE "AccessLevel" AS ENUM ('VIEWER', 'EDITOR');

-- CreateEnum
CREATE TYPE "CaseType" AS ENUM ('WARN', 'TIMEOUT', 'KICK', 'BAN', 'SOFTBAN', 'UNBAN', 'NOTE');

-- CreateEnum
CREATE TYPE "CaseSource" AS ENUM ('MANUAL', 'AUTOMOD', 'AUTO_PUNISHMENT', 'ANTI_RAID');

-- CreateEnum
CREATE TYPE "StatMetric" AS ENUM ('AUTOMOD_DELETED', 'PUNISHMENTS', 'MEMBER_JOINS', 'MEMBER_LEAVES', 'RAIDS', 'MESSAGES');

-- CreateEnum
CREATE TYPE "ChangelogTag" AS ENUM ('NEW', 'FIX', 'CHANGE');

-- CreateEnum
CREATE TYPE "ServiceKey" AS ENUM ('BOT', 'API', 'DATABASE', 'SHARDS', 'DASHBOARD');

-- CreateEnum
CREATE TYPE "IncidentSeverity" AS ENUM ('MINOR', 'MAJOR', 'CRITICAL', 'MAINTENANCE');

-- CreateEnum
CREATE TYPE "IncidentStatus" AS ENUM ('INVESTIGATING', 'IDENTIFIED', 'MONITORING', 'RESOLVED');

-- CreateEnum
CREATE TYPE "FaqScope" AS ENUM ('HOME', 'PREMIUM');

-- CreateEnum
CREATE TYPE "BlacklistType" AS ENUM ('GUILD', 'USER');

-- CreateEnum
CREATE TYPE "ContactTopic" AS ENUM ('SUPPORT', 'BUG', 'PARTNERSHIP', 'PREMIUM', 'OTHER');

-- CreateTable
CREATE TABLE "User" (
    "id" TEXT NOT NULL,
    "discordId" TEXT,
    "name" TEXT,
    "image" TEXT,
    "email" TEXT,
    "emailVerified" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Account" (
    "userId" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "provider" TEXT NOT NULL,
    "providerAccountId" TEXT NOT NULL,
    "refresh_token" TEXT,
    "access_token" TEXT,
    "expires_at" INTEGER,
    "token_type" TEXT,
    "scope" TEXT,
    "id_token" TEXT,
    "session_state" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Account_pkey" PRIMARY KEY ("provider","providerAccountId")
);

-- CreateTable
CREATE TABLE "Session" (
    "sessionToken" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "expires" TIMESTAMP(3) NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL
);

-- CreateTable
CREATE TABLE "VerificationToken" (
    "identifier" TEXT NOT NULL,
    "token" TEXT NOT NULL,
    "expires" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "VerificationToken_pkey" PRIMARY KEY ("identifier","token")
);

-- CreateTable
CREATE TABLE "Guild" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "icon" TEXT,
    "ownerId" TEXT NOT NULL,
    "memberCount" INTEGER NOT NULL DEFAULT 0,
    "region" TEXT,
    "botPresent" BOOLEAN NOT NULL DEFAULT true,
    "premiumTier" "PremiumTier" NOT NULL DEFAULT 'FREE',
    "premiumUntil" TIMESTAMP(3),
    "joinedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "leftAt" TIMESTAMP(3),
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Guild_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "GuildSettings" (
    "guildId" TEXT NOT NULL,
    "prefix" VARCHAR(5) NOT NULL DEFAULT '!',
    "locale" "BotLocale" NOT NULL DEFAULT 'PL',
    "timezone" TEXT NOT NULL DEFAULT 'Europe/Warsaw',
    "version" INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT "GuildSettings_pkey" PRIMARY KEY ("guildId")
);

-- CreateTable
CREATE TABLE "AutoModRule" (
    "id" TEXT NOT NULL,
    "guildId" TEXT NOT NULL,
    "module" "AutoModModule" NOT NULL,
    "enabled" BOOLEAN NOT NULL DEFAULT false,
    "sensitivity" INTEGER NOT NULL DEFAULT 50,
    "actions" "ModAction"[],
    "muteDuration" INTEGER,
    "exemptChannels" TEXT[],
    "exemptRoles" TEXT[],
    "options" JSONB NOT NULL DEFAULT '{}',
    "version" INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT "AutoModRule_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "BannedWord" (
    "id" TEXT NOT NULL,
    "guildId" TEXT NOT NULL,
    "pattern" VARCHAR(100) NOT NULL,
    "matchType" "WordMatch" NOT NULL DEFAULT 'CONTAINS',
    "createdBy" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "BannedWord_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "AntiRaidConfig" (
    "guildId" TEXT NOT NULL,
    "enabled" BOOLEAN NOT NULL DEFAULT false,
    "joinThreshold" INTEGER NOT NULL DEFAULT 10,
    "joinWindowSec" INTEGER NOT NULL DEFAULT 10,
    "minAccountAgeDays" INTEGER NOT NULL DEFAULT 0,
    "action" "ModAction" NOT NULL DEFAULT 'KICK',
    "lockdownActive" BOOLEAN NOT NULL DEFAULT false,
    "lockdownAt" TIMESTAMP(3),
    "lockdownBy" TEXT,
    "version" INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT "AntiRaidConfig_pkey" PRIMARY KEY ("guildId")
);

-- CreateTable
CREATE TABLE "PunishmentThreshold" (
    "id" TEXT NOT NULL,
    "guildId" TEXT NOT NULL,
    "warnCount" INTEGER NOT NULL,
    "action" "ModAction" NOT NULL,
    "duration" INTEGER,

    CONSTRAINT "PunishmentThreshold_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "LogChannel" (
    "guildId" TEXT NOT NULL,
    "eventType" "LogEvent" NOT NULL,
    "channelId" TEXT,
    "enabled" BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT "LogChannel_pkey" PRIMARY KEY ("guildId","eventType")
);

-- CreateTable
CREATE TABLE "VerificationConfig" (
    "guildId" TEXT NOT NULL,
    "enabled" BOOLEAN NOT NULL DEFAULT false,
    "mode" "VerificationMode" NOT NULL DEFAULT 'BUTTON',
    "channelId" TEXT,
    "roleId" TEXT,
    "message" VARCHAR(2000),
    "version" INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT "VerificationConfig_pkey" PRIMARY KEY ("guildId")
);

-- CreateTable
CREATE TABLE "GreetingConfig" (
    "guildId" TEXT NOT NULL,
    "type" "GreetingType" NOT NULL,
    "enabled" BOOLEAN NOT NULL DEFAULT false,
    "channelId" TEXT,
    "content" VARCHAR(2000),
    "embed" JSONB,
    "dm" BOOLEAN NOT NULL DEFAULT false,
    "version" INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT "GreetingConfig_pkey" PRIMARY KEY ("guildId","type")
);

-- CreateTable
CREATE TABLE "ReactionRoleMessage" (
    "id" TEXT NOT NULL,
    "guildId" TEXT NOT NULL,
    "channelId" TEXT NOT NULL,
    "messageId" TEXT,
    "title" VARCHAR(256) NOT NULL,
    "mode" "ReactionRoleMode" NOT NULL DEFAULT 'NORMAL',
    "style" "ReactionRoleStyle" NOT NULL DEFAULT 'BUTTONS',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ReactionRoleMessage_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ReactionRoleEntry" (
    "id" TEXT NOT NULL,
    "groupId" TEXT NOT NULL,
    "emoji" TEXT,
    "label" VARCHAR(80),
    "roleId" TEXT NOT NULL,
    "position" INTEGER NOT NULL,

    CONSTRAINT "ReactionRoleEntry_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "AutoRole" (
    "guildId" TEXT NOT NULL,
    "roleId" TEXT NOT NULL,
    "delaySec" INTEGER NOT NULL DEFAULT 0,
    "botsOnly" BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT "AutoRole_pkey" PRIMARY KEY ("guildId","roleId")
);

-- CreateTable
CREATE TABLE "DashboardAccess" (
    "id" TEXT NOT NULL,
    "guildId" TEXT NOT NULL,
    "subjectType" "AccessSubject" NOT NULL,
    "subjectId" TEXT NOT NULL,
    "level" "AccessLevel" NOT NULL DEFAULT 'EDITOR',

    CONSTRAINT "DashboardAccess_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ModCase" (
    "id" TEXT NOT NULL,
    "guildId" TEXT NOT NULL,
    "caseNumber" INTEGER NOT NULL,
    "type" "CaseType" NOT NULL,
    "targetId" TEXT NOT NULL,
    "targetTag" TEXT,
    "moderatorId" TEXT NOT NULL,
    "moderatorTag" TEXT,
    "reason" VARCHAR(1000),
    "source" "CaseSource" NOT NULL DEFAULT 'MANUAL',
    "automodModule" "AutoModModule",
    "duration" INTEGER,
    "expiresAt" TIMESTAMP(3),
    "revokedAt" TIMESTAMP(3),
    "revokedBy" TEXT,
    "revokeReason" VARCHAR(500),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ModCase_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "GuildStatBucket" (
    "guildId" TEXT NOT NULL,
    "metric" "StatMetric" NOT NULL,
    "bucketStart" TIMESTAMP(3) NOT NULL,
    "value" INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT "GuildStatBucket_pkey" PRIMARY KEY ("guildId","metric","bucketStart")
);

-- CreateTable
CREATE TABLE "ConfigAuditLog" (
    "id" TEXT NOT NULL,
    "guildId" TEXT NOT NULL,
    "actorId" TEXT NOT NULL,
    "section" TEXT NOT NULL,
    "diff" JSONB NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ConfigAuditLog_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ChangelogEntry" (
    "id" TEXT NOT NULL,
    "version" TEXT NOT NULL,
    "publishedAt" TIMESTAMP(3) NOT NULL,
    "titlePl" TEXT NOT NULL,
    "titleEn" TEXT NOT NULL,
    "published" BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT "ChangelogEntry_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ChangelogItem" (
    "id" TEXT NOT NULL,
    "entryId" TEXT NOT NULL,
    "tag" "ChangelogTag" NOT NULL,
    "textPl" TEXT NOT NULL,
    "textEn" TEXT NOT NULL,
    "position" INTEGER NOT NULL,

    CONSTRAINT "ChangelogItem_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ServiceCheck" (
    "id" BIGSERIAL NOT NULL,
    "service" "ServiceKey" NOT NULL,
    "ok" BOOLEAN NOT NULL,
    "latencyMs" INTEGER,
    "checkedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ServiceCheck_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "UptimeDay" (
    "service" "ServiceKey" NOT NULL,
    "day" DATE NOT NULL,
    "uptimePct" DOUBLE PRECISION NOT NULL,

    CONSTRAINT "UptimeDay_pkey" PRIMARY KEY ("service","day")
);

-- CreateTable
CREATE TABLE "Incident" (
    "id" TEXT NOT NULL,
    "titlePl" TEXT NOT NULL,
    "titleEn" TEXT NOT NULL,
    "severity" "IncidentSeverity" NOT NULL,
    "status" "IncidentStatus" NOT NULL,
    "services" "ServiceKey"[],
    "startedAt" TIMESTAMP(3) NOT NULL,
    "resolvedAt" TIMESTAMP(3),

    CONSTRAINT "Incident_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "IncidentUpdate" (
    "id" TEXT NOT NULL,
    "incidentId" TEXT NOT NULL,
    "status" "IncidentStatus" NOT NULL,
    "messagePl" TEXT NOT NULL,
    "messageEn" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "IncidentUpdate_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Faq" (
    "id" TEXT NOT NULL,
    "scope" "FaqScope" NOT NULL,
    "questionPl" TEXT NOT NULL,
    "questionEn" TEXT NOT NULL,
    "answerPl" TEXT NOT NULL,
    "answerEn" TEXT NOT NULL,
    "position" INTEGER NOT NULL,
    "published" BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT "Faq_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Testimonial" (
    "id" TEXT NOT NULL,
    "author" TEXT NOT NULL,
    "rolePl" TEXT NOT NULL,
    "roleEn" TEXT NOT NULL,
    "avatarUrl" TEXT,
    "quotePl" TEXT NOT NULL,
    "quoteEn" TEXT NOT NULL,
    "position" INTEGER NOT NULL,
    "published" BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT "Testimonial_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Partner" (
    "id" TEXT NOT NULL,
    "guildId" TEXT,
    "name" TEXT NOT NULL,
    "iconUrl" TEXT,
    "inviteUrl" TEXT,
    "memberCount" INTEGER,
    "descPl" TEXT,
    "descEn" TEXT,
    "position" INTEGER NOT NULL,
    "published" BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT "Partner_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "BlacklistEntry" (
    "id" TEXT NOT NULL,
    "type" "BlacklistType" NOT NULL,
    "targetId" TEXT NOT NULL,
    "reason" TEXT,
    "createdBy" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "expiresAt" TIMESTAMP(3),

    CONSTRAINT "BlacklistEntry_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ContactMessage" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "topic" "ContactTopic" NOT NULL,
    "message" VARCHAR(5000) NOT NULL,
    "ipHash" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "handled" BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT "ContactMessage_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "RateLimitHit" (
    "key" TEXT NOT NULL,
    "windowStart" TIMESTAMP(3) NOT NULL,
    "count" INTEGER NOT NULL,

    CONSTRAINT "RateLimitHit_pkey" PRIMARY KEY ("key","windowStart")
);

-- CreateTable
CREATE TABLE "WebhookNonce" (
    "nonce" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "WebhookNonce_pkey" PRIMARY KEY ("nonce")
);

-- CreateIndex
CREATE UNIQUE INDEX "User_discordId_key" ON "User"("discordId");

-- CreateIndex
CREATE UNIQUE INDEX "User_email_key" ON "User"("email");

-- CreateIndex
CREATE UNIQUE INDEX "Session_sessionToken_key" ON "Session"("sessionToken");

-- CreateIndex
CREATE INDEX "Guild_botPresent_idx" ON "Guild"("botPresent");

-- CreateIndex
CREATE UNIQUE INDEX "AutoModRule_guildId_module_key" ON "AutoModRule"("guildId", "module");

-- CreateIndex
CREATE UNIQUE INDEX "BannedWord_guildId_pattern_key" ON "BannedWord"("guildId", "pattern");

-- CreateIndex
CREATE UNIQUE INDEX "PunishmentThreshold_guildId_warnCount_key" ON "PunishmentThreshold"("guildId", "warnCount");

-- CreateIndex
CREATE INDEX "ReactionRoleMessage_guildId_idx" ON "ReactionRoleMessage"("guildId");

-- CreateIndex
CREATE UNIQUE INDEX "DashboardAccess_guildId_subjectType_subjectId_key" ON "DashboardAccess"("guildId", "subjectType", "subjectId");

-- CreateIndex
CREATE INDEX "ModCase_guildId_createdAt_idx" ON "ModCase"("guildId", "createdAt" DESC);

-- CreateIndex
CREATE INDEX "ModCase_guildId_targetId_idx" ON "ModCase"("guildId", "targetId");

-- CreateIndex
CREATE INDEX "ModCase_guildId_type_idx" ON "ModCase"("guildId", "type");

-- CreateIndex
CREATE UNIQUE INDEX "ModCase_guildId_caseNumber_key" ON "ModCase"("guildId", "caseNumber");

-- CreateIndex
CREATE INDEX "ConfigAuditLog_guildId_createdAt_idx" ON "ConfigAuditLog"("guildId", "createdAt" DESC);

-- CreateIndex
CREATE UNIQUE INDEX "ChangelogEntry_version_key" ON "ChangelogEntry"("version");

-- CreateIndex
CREATE INDEX "ServiceCheck_service_checkedAt_idx" ON "ServiceCheck"("service", "checkedAt");

-- CreateIndex
CREATE UNIQUE INDEX "BlacklistEntry_type_targetId_key" ON "BlacklistEntry"("type", "targetId");

-- AddForeignKey
ALTER TABLE "Account" ADD CONSTRAINT "Account_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Session" ADD CONSTRAINT "Session_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GuildSettings" ADD CONSTRAINT "GuildSettings_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "AutoModRule" ADD CONSTRAINT "AutoModRule_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "BannedWord" ADD CONSTRAINT "BannedWord_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "AntiRaidConfig" ADD CONSTRAINT "AntiRaidConfig_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "PunishmentThreshold" ADD CONSTRAINT "PunishmentThreshold_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "LogChannel" ADD CONSTRAINT "LogChannel_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "VerificationConfig" ADD CONSTRAINT "VerificationConfig_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GreetingConfig" ADD CONSTRAINT "GreetingConfig_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ReactionRoleMessage" ADD CONSTRAINT "ReactionRoleMessage_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ReactionRoleEntry" ADD CONSTRAINT "ReactionRoleEntry_groupId_fkey" FOREIGN KEY ("groupId") REFERENCES "ReactionRoleMessage"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "AutoRole" ADD CONSTRAINT "AutoRole_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "DashboardAccess" ADD CONSTRAINT "DashboardAccess_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ModCase" ADD CONSTRAINT "ModCase_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GuildStatBucket" ADD CONSTRAINT "GuildStatBucket_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ConfigAuditLog" ADD CONSTRAINT "ConfigAuditLog_guildId_fkey" FOREIGN KEY ("guildId") REFERENCES "Guild"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ConfigAuditLog" ADD CONSTRAINT "ConfigAuditLog_actorId_fkey" FOREIGN KEY ("actorId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ChangelogItem" ADD CONSTRAINT "ChangelogItem_entryId_fkey" FOREIGN KEY ("entryId") REFERENCES "ChangelogEntry"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "IncidentUpdate" ADD CONSTRAINT "IncidentUpdate_incidentId_fkey" FOREIGN KEY ("incidentId") REFERENCES "Incident"("id") ON DELETE CASCADE ON UPDATE CASCADE;
