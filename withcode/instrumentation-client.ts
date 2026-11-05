/** Sentry w przeglądarce — ładowany leniwie i tylko z NEXT_PUBLIC_SENTRY_DSN. */
const dsn = process.env.NEXT_PUBLIC_SENTRY_DSN;
if (dsn) {
  void import("@sentry/nextjs").then((Sentry) =>
    Sentry.init({ dsn, tracesSampleRate: 0.05, replaysSessionSampleRate: 0 }),
  );
}
