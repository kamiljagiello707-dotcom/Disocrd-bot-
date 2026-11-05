export type ActionError =
  | "unauthorized"
  | "forbidden"
  | "validation"
  | "conflict"
  | "rate_limited"
  | "not_found"
  | "server";
export type ActionResult<T = undefined> =
  | { ok: true; data: T }
  | { ok: false; error: ActionError; issues?: { path: string; message: string }[] };
