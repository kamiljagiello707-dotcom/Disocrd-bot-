import { z } from "zod";

export const contactTopics = ["SUPPORT", "BUG", "PARTNERSHIP", "PREMIUM", "OTHER"] as const;

export const contactSchema = z.object({
  name: z.string().trim().min(2).max(80),
  email: z.string().trim().email().max(200),
  topic: z.enum(contactTopics),
  message: z.string().trim().min(20).max(5000),
  /** honeypot — musi być pusty */
  website: z.string().max(0).optional().or(z.literal("")),
  turnstileToken: z.string().optional(),
});
export type ContactInput = z.infer<typeof contactSchema>;
