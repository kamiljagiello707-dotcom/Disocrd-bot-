# WithCode.pl: strona i panel WithBota

Strona marketingowa ze scenami 3D (WebGL) i panel do zarządzania botem moderacyjnym **WithBot** dla Discorda.

**Stack:** Next.js 16 (App Router, Server Components, Server Actions), TypeScript strict, Tailwind CSS v4, three.js + React Three Fiber, GSAP + ScrollTrigger + SplitText, Motion, Lenis, Auth.js v5 (Discord OAuth2), PostgreSQL + Prisma 7, zod, react-hook-form, TanStack Query, Recharts, next-intl (PL/EN), vitest, Playwright.

---

## Spis treści

1. [Szybki start (development)](#szybki-start-development)
2. [Konfiguracja aplikacji Discord](#konfiguracja-aplikacji-discord)
3. [Zmienne środowiskowe](#zmienne-środowiskowe)
4. [Baza danych i migracje](#baza-danych-i-migracje)
5. [Uruchomienie produkcyjne (Docker / VPS / Vercel)](#uruchomienie-produkcyjne)
6. [Integracja z botem](#integracja-z-botem)
7. [Dodawanie komend do listy](#dodawanie-komend-do-listy)
8. [Wpisy do changeloga i inne treści](#wpisy-do-changeloga-i-inne-treści)
9. [Testy i jakość kodu](#testy-i-jakość-kodu)
10. [Architektura i struktura katalogów](#architektura)
11. [Wydajność 3D](#wydajność-3d)
12. [Bezpieczeństwo](#bezpieczeństwo)

---

## Szybki start (development)

Wymagania: **Node.js ≥ 20.9**, **pnpm 10** (`corepack enable`), **PostgreSQL 16** (lokalnie albo `docker compose up postgres`).

```bash
cd withcode
pnpm install                       # instaluje zależności i generuje klienta Prisma
cp .env.example .env               # uzupełnij przynajmniej DATABASE_URL i AUTH_SECRET
pnpm db:migrate                    # tworzy tabele
pnpm db:seed                       # przykładowe treści + serwery demo
pnpm dev                           # http://localhost:3000
```

### Praca bez aplikacji Discord

Ustaw w `.env`:

```env
DISCORD_API_MOCK=1   # przykładowe serwery, kanały i role zamiast Discord API
AUTH_DEV_LOGIN=1     # przycisk „Logowanie testowe” na /login
ADMIN_DISCORD_IDS="100000000000000001"   # użytkownik testowy ma dostęp do /admin
```

Na `/login` pojawi się **Logowanie testowe**. Tworzy ono zwykłą sesję Auth.js w bazie, bez przechodzenia przez Discord. W produkcji ten mechanizm jest wyłączony (działa tylko, gdy `NODE_ENV !== production` albo jawnie `E2E=1`).

### Podgląd scen 3D na maszynie bez GPU

Strona celowo wyłącza sceny, gdy WebGL działa programowo (SwiftShader, llvmpipe) i pokazuje wtedy statyczny fallback. Żeby mimo to zobaczyć sceny, dodaj do adresu `?gl=force`.

---

## Konfiguracja aplikacji Discord

1. Wejdź na <https://discord.com/developers/applications> i kliknij **New Application** (albo użyj istniejącej aplikacji WithBota).
2. **OAuth2 → General**:
   - skopiuj **Client ID** do `AUTH_DISCORD_ID` i `NEXT_PUBLIC_DISCORD_CLIENT_ID`,
   - kliknij **Reset Secret** i skopiuj sekret do `AUTH_DISCORD_SECRET`,
   - w **Redirects** dodaj adres:
     - dev: `http://localhost:3000/api/auth/callback/discord`
     - prod: `https://withcode.pl/api/auth/callback/discord`
3. **Bot → Token**: skopiuj token do `DISCORD_BOT_TOKEN`. Token jest używany tylko po stronie serwera (pobieranie kanałów, ról i członków, z cache 60 s i obsługą rate limitów).
4. Panel prosi użytkownika wyłącznie o scope'y `identify guilds`. Link „Dodaj do Discorda” używa scope'ów `bot applications.commands` i zestawu uprawnień moderacyjnych. Zmienisz go w `lib/utils.ts` → `inviteUrl()`.

---

## Zmienne środowiskowe

Pełna, opisana lista jest w [`.env.example`](./.env.example). Najważniejsze:

| Zmienna                                                  | Wymagana   | Opis                                                |
| -------------------------------------------------------- | ---------- | --------------------------------------------------- |
| `DATABASE_URL`                                           | ✅         | PostgreSQL (wspólny z botem w trybie `database`)    |
| `AUTH_SECRET`                                            | ✅         | sekret Auth.js (`openssl rand -base64 32`)          |
| `AUTH_DISCORD_ID`, `AUTH_DISCORD_SECRET`                 | ✅ prod    | OAuth2 Discorda                                     |
| `DISCORD_BOT_TOKEN`                                      | ✅ prod    | token bota (tylko serwer)                           |
| `TOKEN_ENCRYPTION_KEY`                                   | zalecana   | szyfruje tokeny OAuth w bazie (AES-256-GCM)         |
| `NEXT_PUBLIC_SITE_URL`                                   | ✅ prod    | adres publiczny (canonical, sitemap, OG)            |
| `BOT_API_MODE`                                           | –          | `database` (domyślnie) albo `http`                  |
| `BOT_API_URL`, `BOT_API_SECRET`                          | dla `http` | wewnętrzne API bota                                 |
| `BOT_WEBHOOK_SECRET`                                     | ✅ prod    | HMAC dla webhooka bot → strona                      |
| `ADMIN_DISCORD_IDS`                                      | –          | ID Discorda z dostępem do `/admin`                  |
| `RATE_LIMIT_STORE`                                       | –          | `memory` (domyślnie) / `database` (wiele instancji) |
| `NEXT_PUBLIC_TURNSTILE_SITE_KEY`, `TURNSTILE_SECRET_KEY` | –          | Cloudflare Turnstile w formularzu kontaktowym       |
| `SENTRY_DSN`, `NEXT_PUBLIC_SENTRY_DSN`                   | –          | logowanie błędów; bez nich Sentry się nie ładuje    |

Zmienne są walidowane przy starcie (`lib/env.ts`, zod). Przy błędnej konfiguracji build się nie powiedzie. Do budowania obrazu bez sekretów służy `SKIP_ENV_VALIDATION=1`.

---

## Baza danych i migracje

Schemat jest w [`prisma/schema.prisma`](./prisma/schema.prisma), a konfiguracja Prisma 7 w `prisma.config.ts`. Klient generuje się do `lib/db/generated` (katalog w `.gitignore`).

```bash
pnpm db:migrate     # dev: tworzy nową migrację po zmianie schematu
pnpm db:deploy      # prod: stosuje istniejące migracje (prisma migrate deploy)
pnpm db:seed        # przykładowe dane (changelog, FAQ, opinie, partnerzy, incydenty, serwery demo)
pnpm db:generate    # ręczne wygenerowanie klienta
```

Najważniejsze tabele:

- **Konfiguracja serwera:** `GuildSettings`, `AutoModRule`, `BannedWord`, `AntiRaidConfig`, `PunishmentThreshold`, `LogChannel`, `VerificationConfig`, `GreetingConfig`, `ReactionRole*`, `AutoRole`, `DashboardAccess`.
- **Moderacja i statystyki:** `ModCase`, `GuildStatBucket` (godzinowe agregaty), `ConfigAuditLog` (kto, co i kiedy, z diffem).
- **Treści z `/admin`:** `ChangelogEntry`, `Incident`, `Faq`, `Testimonial`, `Partner`, `BlacklistEntry`.

Sekcje konfiguracji mają pole `version`, które zapewnia optimistic concurrency. Zapis jest odrzucany, jeśli ktoś w międzyczasie zmienił te same ustawienia.

---

## Uruchomienie produkcyjne

### Docker Compose (VPS)

```bash
cd withcode
cp .env.example .env          # uzupełnij sekrety
docker compose up -d --build  # postgres → migrate (migracje + seed) → app na :3000
```

Usługa `migrate` uruchamia `prisma migrate deploy`, a przy `SEED=1` również seed. Po pierwszym wdrożeniu ustaw `SEED=0`. Aplikację postaw za reverse proxy (Caddy albo nginx) z HTTPS.

### Bez Dockera

```bash
pnpm install --frozen-lockfile
pnpm db:deploy
NEXT_OUTPUT=standalone pnpm build
node .next/standalone/server.js     # skopiuj też .next/static i public obok (jak w Dockerfile)
# albo po prostu: pnpm build && pnpm start
```

### Vercel

1. Root Directory: `withcode`.
2. Build Command: `pnpm db:deploy && pnpm build`.
3. Dodaj zmienne z `.env.example`. Baza: dowolny zarządzany PostgreSQL (Neon, Supabase, RDS).
4. Przy wielu instancjach ustaw `RATE_LIMIT_STORE=database`.

---

## Integracja z botem

Warstwa `lib/bot-api` to abstrakcja z dwiema implementacjami, wybieranymi zmienną `BOT_API_MODE`.

### `database` (zalecany)

Bot i strona korzystają z tej samej bazy.

- Bot czyta konfigurację z tabel opisanych wyżej.
- Po każdym zapisie strona wysyła `NOTIFY withbot_config` z payloadem `{"guildId","section"}`. Bot nasłuchuje `LISTEN withbot_config` i odświeża swój cache.
- Lockdown zapisywany jest w `AntiRaidConfig.lockdownActive`.

### `http`

Strona woła wewnętrzne API bota (`BOT_API_URL`). Każde żądanie jest podpisane:

```
X-WithBot-Timestamp: <unix>
X-WithBot-Nonce: <uuid>
X-WithBot-Signature: sha256=HMAC_SHA256(BOT_API_SECRET, "<timestamp>.<body>")
```

Endpointy, które bot powinien wystawić:

| Endpoint                                | Opis                                 |
| --------------------------------------- | ------------------------------------ |
| `GET /status`                           | status bota                          |
| `POST /guilds/presence`                 | `{guildIds}` → `{present}`           |
| `POST /guilds/:id/config-changed`       | powiadomienie o zmianie konfiguracji |
| `POST /guilds/:id/lockdown`             | włączenie/wyłączenie lockdownu       |
| `POST /guilds/:id/cases/:caseId/revoke` | cofnięcie kary                       |

### Webhook bot → strona

`POST /api/webhooks/bot` z tym samym schematem podpisu, ale sekretem `BOT_WEBHOOK_SECRET`. Obowiązuje okno ±5 min, a nonce jest jednorazowy. Obsługiwane zdarzenia (schemat w `lib/validators/webhook.ts`):

- `guild.upsert`, `guild.remove`: bot dołączył do serwera albo z niego wyszedł (region wyznaczany z języka serwera, bez danych osobowych),
- `stats.increment`: liczniki do wykresów w panelu,
- `case.create`: nowa sprawa moderacyjna (numeracja per serwer),
- `heartbeat`: ping i shardy (strona statusu).

Przykład w Node.js:

```js
import { createHmac, randomUUID } from "node:crypto";
const body = JSON.stringify({ type: "heartbeat", pingMs: client.ws.ping });
const ts = Math.floor(Date.now() / 1000).toString();
await fetch("https://withcode.pl/api/webhooks/bot", {
  method: "POST",
  headers: {
    "content-type": "application/json",
    "x-withbot-timestamp": ts,
    "x-withbot-nonce": randomUUID(),
    "x-withbot-signature":
      "sha256=" +
      createHmac("sha256", process.env.BOT_WEBHOOK_SECRET).update(`${ts}.${body}`).digest("hex"),
  },
  body,
});
```

---

## Dodawanie komend do listy

Jedynym źródłem listy jest [`content/commands.ts`](./content/commands.ts). Dopisz obiekt do tablicy `commands`:

```ts
{
  name: "slowmode",                       // bez ukośnika
  category: "moderation",                 // id z commandCategories
  description: { pl: "…", en: "…" },
  syntax: "/slowmode <czas> [kanał]",
  permissions: ["Manage Channels"],
  example: "/slowmode 10s",
  aliases: ["sm"],                        // opcjonalnie
  premium: false,                         // opcjonalnie
},
```

Z tego pliku korzystają strona `/commands` (wyszukiwarka fuzzy Fuse.js, filtry, kopiowanie), sekcja „Powiązane komendy” na stronach funkcji i publiczny endpoint `GET /api/commands`, z którego bot może synchronizować komendy. Test `tests/unit/command-search.test.ts` pilnuje unikalnych nazw i poprawnych kategorii.

---

## Wpisy do changeloga i inne treści

- **Changelog, incydenty, FAQ, opinie, partnerzy, blacklista:** panel **`/admin`**, dostępny tylko dla ID z `ADMIN_DISCORD_IDS`. Dla innych użytkowników zwraca 404. Zmiany są widoczne od razu (`revalidateTag`).
  - Format pozycji changeloga, jedna w linii: `NEW | tekst PL | tekst EN` (tagi: `NEW`, `FIX`, `CHANGE`).
- **Dane startowe:** `content/seed-data.ts`. Strona używa ich też jako fallbacku, gdy baza jest pusta lub niedostępna.
- **Dokumentacja:** pliki MDX w `content/docs/{pl,en}/<slug>.mdx`. Kolejność i tytuły ustawiasz w `content/docs/manifest.ts`, a spis treści i wyszukiwarka generują się automatycznie.
- **Strony prawne:** `content/legal/{pl,en}/{terms,privacy,cookies}.mdx`.
- **Teksty interfejsu:** `messages/pl.json` i `messages/en.json`. Test pilnuje, żeby oba pliki miały identyczne klucze.
- **Logo:** `components/ui/logo.tsx` i `public/icon.svg` (tymczasowy znak „W”).

---

## Testy i jakość kodu

```bash
pnpm lint        # ESLint (flat config, next/core-web-vitals, zakaz any)
pnpm typecheck   # tsc --noEmit (strict, noUncheckedIndexedAccess)
pnpm test        # vitest: walidatory, uprawnienia, wyszukiwarka komend, silnik AutoModu, HMAC, sanityzacja, diff, i18n
pnpm build
pnpm test:e2e    # Playwright na buildzie produkcyjnym (wymaga bazy z seedem)
```

E2E obejmują:

- ładowanie strony głównej (desktop i mobile),
- fallback bez WebGL,
- przełączanie języka,
- axe WCAG AA na 6 stronach,
- 404,
- wyszukiwarkę komend,
- walidację formularza kontaktowego,
- demo kanału,
- nagłówki bezpieczeństwa,
- odrzucanie niepodpisanego webhooka,
- logowanie (mock),
- zapis AutoModu z paskiem niezapisanych zmian,
- przycisk „Cofnij”,
- brak dostępu bez uprawnień.

Husky + lint-staged uruchamiają ESLint, Prettier i typecheck przed commitem. Aktywacja hooka (raz, z katalogu `withcode`): `pnpm prepare`.

---

## Architektura

```
withcode/
├─ app/[locale]/(marketing)   strony publiczne (SSG/ISR), jeden Canvas 3D w layoucie
├─ app/[locale]/(dashboard)   /login, /dashboard, /dashboard/[guildId]/* (dynamiczne, bez 3D)
├─ app/[locale]/admin         panel właściciela
├─ app/api                    auth, webhook bota, status, komendy, OG, API panelu (kanały, role, statystyki, CSV)
├─ components/ui              Button, Card, Badge, Input, Select, Switch, Slider, Tabs, Modal, Toast, Tooltip, Dropdown, Skeleton, Table…
├─ components/three           CanvasRoot, sceny (hero, pole, storytelling, glob, 404), shadery GLSL, store jakości
├─ components/motion          SplitReveal, Scramble, TiltCard, Counter, Cursor, SmoothScroll (Lenis+GSAP)
├─ components/sections        sekcje strony głównej
├─ components/marketing       nawigacja, stopka, baner cookies, wyszukiwarka komend, cennik, symulator Discorda
├─ components/dashboard       formularze sekcji panelu, pickery, wykresy, pasek niezapisanych zmian
├─ lib/                       auth, db, bot-api, discord, permissions, actions, validators, security, automod, seo
├─ content/                   komendy, funkcje, cennik, seed, MDX (docs, prawne)
├─ messages/                  pl.json, en.json
├─ prisma/                    schema, migracje, seed
├─ styles/                    tokens.css (design tokeny), globals.css, prose.css
└─ tests/                     unit (vitest), e2e (Playwright)
```

**Zapisy w panelu** przechodzą przez `lib/actions/guild-action.ts`: sesja → walidacja zod → uprawnienia → rate limit → transakcja z wpisem do audit logu → powiadomienie bota → odświeżenie danych. Uprawnienia (Manage Server / owner / Administrator albo wpis w „Dostęp do panelu”, obecność bota, blacklista) sprawdzane są po stronie serwera przy każdym zapisie i w każdym Route Handlerze. Logika jest w czystej funkcji `lib/permissions/resolve.ts`, pokrytej testami.

**Formularze** używają react-hook-form i zod (te same schematy co na serwerze) z optimistic update. Przy błędzie przywracany jest poprzedni stan, a pasek „Masz niezapisane zmiany” wraca. Wyjście ze strony z niezapisanymi zmianami wymaga potwierdzenia.

---

## Wydajność 3D

- **Jeden Canvas R3F** na całą część marketingową. Sekcje rejestrują sceny przez `SceneSlot` (tunel DOM → R3F), a pozycje obiektów śledzą prostokąty sekcji (`components/three/track.ts`). Nie powstają dodatkowe konteksty WebGL.
- **Leniwe ładowanie:** three.js i sceny (`next/dynamic`, `ssr: false`) ładują się dopiero po zarejestrowaniu sceny i w czasie bezczynności przeglądarki. Storytelling, glob i 404 to osobne chunki. Panel nie importuje three.js.
- **Adaptacyjna jakość:** startowa heurystyka (rdzenie, pamięć, ekran, dotyk) plus `PerformanceMonitor` sterują DPR, liczbą cząsteczek, transmission i postprocessingiem.
- **Pauza:** `frameloop="never"`, gdy żadna scena nie jest widoczna albo karta jest ukryta. Przy `prefers-reduced-motion` używany jest tryb `demand` (pojedyncze klatki), bez Lenisa i pinów.
- **Fallback:** brak WebGL, renderer programowy albo utrata kontekstu oznaczają statyczne grafiki (`html[data-webgl="off"]`).
- **Bez GLB:** tarcza jest generowana w kodzie, więc nie ma ciężkich assetów. Pipeline dla przyszłych modeli: `npx @gltf-transform/cli optimize in.glb out.glb --compress meshopt --texture-compress ktx2`, ładowanie przez `useGLTF` z Meshopt/KTX2.

Wyniki Lighthouse z tego środowiska (headless Chromium, build produkcyjny):

| Strona          | Desktop: wydajność | Mobile: wydajność | Dostępność | Best practices | CLS |
| --------------- | ------------------ | ----------------- | ---------- | -------------- | --- |
| `/`             | 99                 | 76–77             | 100        | 100            | 0   |
| `/commands`     | 99                 | 77                | 98         | 100            | 0   |
| `/docs/automod` | 99                 | 82                | 100        | 100            | 0   |

LCP w symulacji mobilnej Lighthouse wynosi ~4,5 s. W rzeczywistym śladzie (4× spowolnienie CPU, bez dławienia sieci) LCP = FCP ≈ 0,35 s. Różnica wynika z modelu symulacji (lantern), ale cel „LCP < 2,5 s” w mobilnym raporcie Lighthouse **nie jest jeszcze spełniony**. SEO 92 w tych pomiarach wynika z różnicy portu między `NEXT_PUBLIC_SITE_URL` a serwerem testowym (canonical).

---

## Bezpieczeństwo

- **CSP:**
  - panel i `/admin`: nonce + `strict-dynamic` (strony dynamiczne),
  - strony marketingowe: CSP bez `unsafe-eval`; `unsafe-inline` dla skryptów jest konieczne, żeby zachować statyczne generowanie (Next.js nie wstrzykuje nonce do stron SSG).
- **Nagłówki:** HSTS, X-Frame-Options DENY + `frame-ancestors 'none'`, Referrer-Policy, Permissions-Policy, COOP.
- **CSRF:** Server Actions mają wbudowaną kontrolę `Origin`, a ciasteczka sesji mają `SameSite=Lax` i `HttpOnly`.
- **Rate limit:** formularz kontaktowy (5/h/IP), zapisy w panelu (30/min/użytkownik/serwer), webhook bota.
- **Formularz kontaktowy:** honeypot i opcjonalnie Turnstile. IP zapisywane wyłącznie jako hash z solą.
- **Sanityzacja:** treści użytkownika (powitania, weryfikacja, role) są czyszczone z HTML, a `@everyone`/`@here` neutralizowane. Podgląd w panelu renderuje tekst, nie HTML. Eksport CSV jest chroniony przed formula injection.
- **Tokeny:** tokeny OAuth są szyfrowane w bazie, a token bota istnieje wyłącznie w modułach `server-only`.
