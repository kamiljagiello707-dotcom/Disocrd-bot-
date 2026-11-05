import { z } from "zod";

export const snowflake = z.string().regex(/^\d{17,20}$/, "Invalid Discord ID");
export const optionalSnowflake = snowflake.nullable();
export const version = z.number().int().min(1);

export const modActionSchema = z.enum(["DELETE", "WARN", "TIMEOUT", "KICK", "BAN"]);
export type ModActionValue = z.infer<typeof modActionSchema>;

export const durationSeconds = z
  .number()
  .int()
  .min(60)
  .max(60 * 60 * 24 * 28); // limit timeoutu Discorda: 28 dni

export const guildIdParam = z.object({ guildId: snowflake });
