"use server";
import { signIn, signOut } from "@/lib/auth";

export async function signInWithDiscord(formData: FormData) {
  const raw = String(formData.get("callbackUrl") ?? "/dashboard");
  const redirectTo = raw.startsWith("/") && !raw.startsWith("//") ? raw : "/dashboard";
  await signIn("discord", { redirectTo });
}

export async function signOutAction() {
  await signOut({ redirectTo: "/" });
}
