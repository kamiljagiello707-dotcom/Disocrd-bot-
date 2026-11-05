import { describe, expect, it } from "vitest";
import {
  canEdit,
  canManageAccess,
  resolveAccess,
  type AccessInput,
} from "@/lib/permissions/resolve";
import { canManageGuild, hasPermission, Permission } from "@/lib/discord/permissions";

const base: AccessInput = {
  userDiscordId: "111111111111111111",
  userGuild: { owner: false, permissions: "0" },
  accessEntries: [],
  memberRoleIds: [],
  botPresent: true,
  blacklisted: { guild: false, user: false },
};

describe("discord permission bits", () => {
  it("detects MANAGE_GUILD and ADMINISTRATOR", () => {
    expect(hasPermission(String(Permission.MANAGE_GUILD), Permission.MANAGE_GUILD)).toBe(true);
    expect(canManageGuild({ owner: false, permissions: String(Permission.ADMINISTRATOR) })).toBe(
      true,
    );
    expect(canManageGuild({ owner: false, permissions: String(Permission.MANAGE_ROLES) })).toBe(
      false,
    );
    expect(canManageGuild({ owner: true, permissions: "0" })).toBe(true);
  });
  it("handles big permission integers and garbage", () => {
    expect(hasPermission("2147483647", Permission.MANAGE_GUILD)).toBe(true);
    expect(hasPermission("not-a-number", Permission.MANAGE_GUILD)).toBe(false);
  });
});

describe("resolveAccess", () => {
  it("owner and manager", () => {
    expect(resolveAccess({ ...base, userGuild: { owner: true, permissions: "0" } })).toBe("OWNER");
    expect(
      resolveAccess({
        ...base,
        userGuild: { owner: false, permissions: String(Permission.MANAGE_GUILD) },
      }),
    ).toBe("MANAGER");
  });
  it("denies regular member without access entries", () => {
    expect(resolveAccess(base)).toBeNull();
  });
  it("grants via user entry and role entry", () => {
    expect(
      resolveAccess({
        ...base,
        accessEntries: [{ subjectType: "USER", subjectId: base.userDiscordId, level: "VIEWER" }],
      }),
    ).toBe("VIEWER");
    expect(
      resolveAccess({
        ...base,
        memberRoleIds: ["222222222222222222"],
        accessEntries: [{ subjectType: "ROLE", subjectId: "222222222222222222", level: "EDITOR" }],
      }),
    ).toBe("EDITOR");
  });
  it("editor wins over viewer", () => {
    expect(
      resolveAccess({
        ...base,
        memberRoleIds: ["222222222222222222"],
        accessEntries: [
          { subjectType: "USER", subjectId: base.userDiscordId, level: "VIEWER" },
          { subjectType: "ROLE", subjectId: "222222222222222222", level: "EDITOR" },
        ],
      }),
    ).toBe("EDITOR");
  });
  it("denies when bot missing or blacklisted, even for owner", () => {
    const owner = { ...base, userGuild: { owner: true, permissions: "0" } };
    expect(resolveAccess({ ...owner, botPresent: false })).toBeNull();
    expect(resolveAccess({ ...owner, blacklisted: { guild: true, user: false } })).toBeNull();
    expect(resolveAccess({ ...owner, blacklisted: { guild: false, user: true } })).toBeNull();
  });
  it("global admin bypasses blacklist but not missing bot", () => {
    const owner = { ...base, userGuild: { owner: true, permissions: "0" }, isGlobalAdmin: true };
    expect(resolveAccess({ ...owner, blacklisted: { guild: true, user: false } })).toBe("OWNER");
    expect(resolveAccess({ ...owner, botPresent: false })).toBeNull();
  });
  it("edit helpers", () => {
    expect(canEdit("VIEWER")).toBe(false);
    expect(canEdit("EDITOR")).toBe(true);
    expect(canManageAccess("EDITOR")).toBe(false);
    expect(canManageAccess("MANAGER")).toBe(true);
    expect(canEdit(null)).toBe(false);
  });
});
