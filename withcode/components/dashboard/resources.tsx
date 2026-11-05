"use client";
import { createContext, useContext, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";

export type Option = { id: string; name: string };
type Resources = {
  guildId: string;
  guildName: string;
  memberCount: number;
  channels: Option[];
  roles: Option[];
};

const Ctx = createContext<Resources | null>(null);

async function fetchJson<T>(url: string): Promise<T> {
  const r = await fetch(url, { credentials: "same-origin" });
  if (!r.ok) throw new Error(String(r.status));
  return (await r.json()) as T;
}

/** Kanały i role serwera: dane startowe z RSC + odświeżanie przez TanStack Query (cache 60 s). */
export function GuildResourcesProvider({
  initial,
  children,
}: {
  initial: Resources;
  children: ReactNode;
}) {
  const channels = useQuery({
    queryKey: ["guild", initial.guildId, "channels"],
    queryFn: () => fetchJson<Option[]>(`/api/guilds/${initial.guildId}/channels`),
    initialData: initial.channels,
    staleTime: 60_000,
  });
  const roles = useQuery({
    queryKey: ["guild", initial.guildId, "roles"],
    queryFn: () => fetchJson<Option[]>(`/api/guilds/${initial.guildId}/roles`),
    initialData: initial.roles,
    staleTime: 60_000,
  });
  return (
    <Ctx.Provider value={{ ...initial, channels: channels.data, roles: roles.data }}>
      {children}
    </Ctx.Provider>
  );
}

export function useGuildResources(): Resources {
  const v = useContext(Ctx);
  if (!v) throw new Error("GuildResourcesProvider missing");
  return v;
}
