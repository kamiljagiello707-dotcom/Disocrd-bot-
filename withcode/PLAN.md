# WithCode.pl / WithBot: plan projektu (etap 0, do akceptacji)

> Stan: **projekt do akceptacji**. Kod zacznę pisać dopiero po zatwierdzeniu.
> Aplikacja trafi do katalogu `withcode/`, więc obecny bot ticketów (`src/`) i statyczne strony (`website/`) zostają bez zmian.

---

## 1. Architektura

### 1.1 Stack i wersje

Przed każdym etapem sprawdzę aktualne API w dokumentacji i przypnę dokładne wersje w `package.json`. Poniżej to, co zakładam dziś, i jak to wpływa na projekt:

| Obszar                 | Biblioteka                                                                                                                           | Uwagi do API, które sprawdzę                                                                                 |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------ |
| Framework              | Next.js (najnowsza stabilna, App Router, React 19)                                                                                   | `proxy.ts` zamiast `middleware.ts` (od v16), `params` jako Promise, Turbopack, `experimental.viewTransition` |
| Język                  | TypeScript `strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`                                                        | ESLint: `no-explicit-any: error`                                                                             |
| Style                  | Tailwind CSS v4 (konfiguracja w CSS przez `@theme`)                                                                                  | tokeny jako zmienne CSS w `styles/tokens.css`, mapowane do `@theme inline`                                   |
| 3D                     | three, @react-three/fiber v9, @react-three/drei v10, @react-three/postprocessing v3                                                  | `View`, `PerformanceMonitor`, `useProgress`, `AdaptiveDpr`                                                   |
| Animacje               | gsap + ScrollTrigger + SplitText (GSAP jest teraz w pełni darmowy), `motion` (dawniej Framer Motion, import `motion/react`), `lenis` | jeden zegar: `gsap.ticker` napędza Lenis, Lenis wywołuje `ScrollTrigger.update`                              |
| Auth                   | Auth.js v5 (`next-auth@5`) + `@auth/prisma-adapter`, provider Discord                                                                | scope `identify guilds`, odświeżanie access tokena                                                           |
| DB                     | PostgreSQL 16 + Prisma (najnowsza; przy v7 nowy generator `prisma-client` + driver adapter `@prisma/adapter-pg`)                     |                                                                                                              |
| Walidacja / formularze | zod v4, react-hook-form + `@hookform/resolvers`                                                                                      | te same schematy na kliencie, w Server Actions i w testach                                                   |
| Dane klienta           | TanStack Query v5                                                                                                                    | hydratacja z RSC, optimistic updates                                                                         |
| Wykresy                | Recharts v3                                                                                                                          | monochromatyczny motyw, własny tooltip                                                                       |
| i18n                   | next-intl v4                                                                                                                         | `pl` domyślny (bez prefiksu w URL? patrz pytania), `en`                                                      |
| Wyszukiwanie           | Fuse.js (komendy), FlexSearch/Pagefind (dokumentacja, indeks generowany przy buildzie)                                               |                                                                                                              |
| MDX                    | `@next/mdx` lub `next-mdx-remote-client` + `rehype-pretty-code` (Shiki), `rehype-slug`, `remark-gfm`                                 |                                                                                                              |
| Sanityzacja            | `sanitize-html` (serwer); markdown Discorda parsowany własnym, białolistowym parserem                                                |                                                                                                              |
| Rate limit             | `@upstash/ratelimit` gdy jest Redis; w przeciwnym razie tabela w Postgres (sliding window)                                           |                                                                                                              |
| Captcha                | Cloudflare Turnstile (opcjonalnie, przez env)                                                                                        |                                                                                                              |
| Błędy                  | `@sentry/nextjs` (włączany, gdy ustawiony jest `SENTRY_DSN`)                                                                         |                                                                                                              |
| Testy                  | vitest + Testing Library, Playwright                                                                                                 |                                                                                                              |
| Jakość                 | ESLint (flat config), Prettier + `prettier-plugin-tailwindcss`, husky + lint-staged                                                  |                                                                                                              |
| Env                    | `@t3-oss/env-nextjs` + zod: build się wywali, jeśli brakuje zmiennej                                                                 |                                                                                                              |

### 1.2 Warstwy

```
┌──────────────────────────── Next.js (App Router) ─────────────────────────────┐
│  RSC (strony, dane)   │  Client Components (UI, 3D, animacje)  │  Route Handlers │
│          │                         │                                 │         │
│  Server Actions ── zod ── lib/permissions (Manage Server przy KAŻDYM zapisie)  │
│          │                                                         │           │
│  lib/db (Prisma)   lib/discord (REST + cache 60 s + rate limit)   lib/bot-api  │
└──────────┼──────────────────────────────┼─────────────────────────────┼────────┘
           ▼                              ▼                             ▼
      PostgreSQL ◄──── wspólna baza ────► WithBot ◄── HTTP + HMAC ── (wariant B)
                                          │
                     POST /api/webhooks/bot (HMAC-SHA256, timestamp, replay guard)
```

- **`lib/bot-api`**: interfejs `BotApi` z dwiema implementacjami wybieranymi przez `BOT_API_MODE=database|http`:
  - `DatabaseBotApi`: bot czyta konfigurację z tych samych tabel (zalecane, najprostsze). Strona po zapisie wysyła do bota „ping” (NOTIFY w Postgres albo HTTP), żeby bot unieważnił swój cache.
  - `HttpBotApi`: wewnętrzne REST API bota (`BOT_API_URL`, podpis HMAC), konfiguracja i statystyki przez bota.
  - Metody: `getStatus()`, `getGuildPresence(ids)`, `pushConfig(guildId, section)`, `setLockdown(guildId, on)`, `revokeCase(guildId, caseId)`, `getGlobalStats()`.
- **`lib/discord`**: `fetch` z tokenem bota (wyłącznie `server-only`), obsługa nagłówków `X-RateLimit-*` i `429 retry_after`, kolejka per bucket, cache przez `unstable_cache`/`'use cache'` z tagami (`guild:{id}:channels`, TTL 60 s), deduplikacja żądań.
- **Uprawnienia**: `assertCanManageGuild(session, guildId)` w każdej akcji i każdym handlerze: (1) gildia na liście użytkownika z bitem `MANAGE_GUILD (0x20)`, `ADMINISTRATOR (0x8)` albo użytkownik jest właścicielem; **lub** (2) wpis w `DashboardAccess` (użytkownik/rola, rola sprawdzana przez token bota); (3) bot jest na serwerze; (4) serwer i użytkownik nie są na blackliście. UI tylko ukrywa przyciski, decyzję zawsze podejmuje serwer.
- **Server Actions**: wspólny wrapper `guildAction(schema, handler)`: auth, zod, uprawnienia, rate limit, transakcja z wpisem do `ConfigAuditLog` (diff), `revalidateTag`, powiadomienie bota, wynik typu `Result<T, ActionError>` (bez rzucania wyjątków do UI).

### 1.3 Jeden Canvas dla całej części marketingowej

- `components/three/CanvasRoot.tsx`: jeden `<Canvas>` w layoucie `(marketing)`, `position: fixed; inset: 0; pointer-events: none`, `eventSource={document.body}`, ładowany przez `next/dynamic({ ssr: false })` po `requestIdleCallback`.
- Sceny rejestrują się przez drei `<View track={ref}>` (każda sekcja ma `div`, który wyznacza viewport sceny). Wspólne zasoby (env map, materiały, geometria cząsteczek) są w jednym `SceneAssetsProvider`.
- Scroll: Lenis → `gsap.ticker` → ScrollTrigger; postęp sekcji zapisuję do store'u zustand **bez rerenderów** (`useStore.getState()` w `useFrame`).
- Pętla renderowania: `frameloop="always"` tylko wtedy, gdy jakiś `View` jest widoczny (IntersectionObserver), poza tym `"never"`; pauza przy `visibilitychange`; sceny statyczne (glob po zatrzymaniu) używają `invalidate()`.
- Jakość adaptacyjna: `PerformanceMonitor` → store `quality: 'high' | 'medium' | 'low'`. Od niej zależy DPR (2 → 1.5 → 1), liczba cząsteczek (100% → 50% → 20%), postprocessing (pełny → sam Bloom → brak) i `transmission` (prawdziwa → fake z env mapy).
- Startowa jakość z heurystyki: `navigator.hardwareConcurrency`, `deviceMemory`, rozmiar ekranu, `pointer: coarse` oraz test GPU (renderer string przez `WEBGL_debug_renderer_info`, opcjonalnie `detect-gpu`).
- Fallback bez WebGL lub po utracie kontekstu: statyczne grafiki AVIF/WebP (wyrenderowane z tych samych scen) + gradienty CSS.
- `prefers-reduced-motion`: brak Lenis, brak pinów i scrubów, sceny jako pojedyncza klatka (`frameloop="demand"`), zostają przejścia opacity/blur.

### 1.4 Wydajność i LCP

- Elementem LCP jest **tekst nagłówka hero** (SSR, font z `next/font` z `display: swap` i preloadem). Canvas dochodzi później i nie wpływa na LCP.
- Rezerwacja miejsca: sekcje 3D mają stałe proporcje/wysokości w CSS, a canvas jest `fixed`, więc CLS od scen wynosi 0.
- Obiekt hero jest **proceduralny** (geometria z kodu, bez pobierania GLB), więc scena startuje bez ciężkich assetów. Pipeline GLB jest gotowy na przyszłe modele: `gltf-transform optimize --compress meshopt --texture-compress ktx2`, `useGLTF` z `MeshoptDecoder` i `KTX2Loader` (transcoder z `/public/basis`).
- Env mapa: mała tekstura HDR/KTX2 (~100 KB) albo `Lightformer`y drei (zero pobierania); wybiorę Lightformery.
- Budżet JS: chunk 3D ładowany dopiero na stronach marketingowych, panel nie importuje three.js wcale (sprawdzę `@next/bundle-analyzer`).

---

## 2. Struktura katalogów

```
withcode/
├─ app/
│  ├─ [locale]/
│  │  ├─ layout.tsx                 # html/body, fonty, providery (intl, query, toasty)
│  │  ├─ (marketing)/
│  │  │  ├─ layout.tsx              # Nav, Footer, CanvasRoot, Lenis, Cursor, Grain, PageTransition
│  │  │  ├─ page.tsx                # strona główna
│  │  │  ├─ features/page.tsx, features/[module]/page.tsx
│  │  │  ├─ commands/page.tsx
│  │  │  ├─ premium/page.tsx
│  │  │  ├─ docs/[[...slug]]/page.tsx
│  │  │  ├─ status/page.tsx
│  │  │  ├─ changelog/page.tsx
│  │  │  ├─ demo/page.tsx
│  │  │  ├─ partners/page.tsx
│  │  │  ├─ contact/page.tsx
│  │  │  ├─ legal/{terms,privacy,cookies}/page.tsx
│  │  │  └─ not-found.tsx
│  │  ├─ (dashboard)/
│  │  │  ├─ layout.tsx              # lekki layout, bez 3D i bez Lenis
│  │  │  ├─ login/page.tsx
│  │  │  └─ dashboard/
│  │  │     ├─ page.tsx             # lista serwerów
│  │  │     └─ [guildId]/
│  │  │        ├─ layout.tsx        # sidebar, guard uprawnień, UnsavedChangesBar
│  │  │        ├─ page.tsx          # przegląd
│  │  │        ├─ automod/ antiraid/ cases/ cases/[caseId]/ punishments/
│  │  │        ├─ logs/ verification/ welcome/ roles/ access/ settings/ audit/
│  │  └─ admin/
│  │     ├─ layout.tsx              # guard ADMIN_DISCORD_IDS (+ notFound() dla reszty)
│  │     ├─ page.tsx                # statystyki globalne
│  │     └─ changelog/ incidents/ faq/ testimonials/ partners/ guilds/ blacklist/
│  ├─ api/
│  │  ├─ auth/[...nextauth]/route.ts
│  │  ├─ webhooks/bot/route.ts      # HMAC
│  │  ├─ status/route.ts            # status bota (cache 30 s)
│  │  ├─ commands/route.ts          # JSON komend (jedno źródło)
│  │  ├─ guilds/[guildId]/{channels,roles,stats}/route.ts
│  │  ├─ cases/export/route.ts      # CSV (stream)
│  │  └─ og/route.tsx               # dynamiczne obrazki OG
│  ├─ sitemap.ts, robots.ts, manifest.ts, icon.svg
│  └─ global-error.tsx
├─ components/
│  ├─ ui/          # Button, Card, Badge, Input, Textarea, Select, Switch, Slider, Tabs, Modal,
│  │               # Toast, Tooltip, Dropdown, Skeleton, Table, Pagination, Kbd, Logo
│  ├─ three/
│  │  ├─ CanvasRoot.tsx, ViewPortal.tsx, QualityProvider.tsx, Fallback.tsx, Loader.tsx
│  │  ├─ scenes/   # HeroCore, ParticleField, Story, Globe, NotFound404
│  │  ├─ materials/ # CoreMaterial, ParticlesMaterial, FieldMaterial, GlobeMaterial, TransitionMaterial
│  │  └─ shaders/  # *.vert.glsl, *.frag.glsl, chunks/{simplex3d,curl,fresnel}.glsl
│  ├─ sections/    # Hero, Trust, Story, Features, Stats, Comparison, Testimonials, Faq, FinalCta
│  ├─ motion/      # SplitReveal, Scramble, Magnetic, Tilt, Counter, PageTransition, SmoothScroll
│  ├─ marketing/   # Nav, MobileMenu, Footer, CookieBanner, CommandSearch, PricingToggle, DiscordMock…
│  └─ dashboard/   # Sidebar, GuildCard, StatChart, ChannelPicker, RolePicker, WelcomeEditor, DiscordPreview…
├─ lib/
│  ├─ auth/        # auth.ts (Auth.js), session helpers, refresh tokena
│  ├─ db/          # prisma client (singleton), repozytoria per moduł
│  ├─ bot-api/     # types.ts, database.ts, http.ts, index.ts (wybór wariantu)
│  ├─ discord/     # rest.ts, rate-limit.ts, permissions.ts (bitfield), cdn.ts, markdown.ts
│  ├─ validators/  # zod: automod, antiraid, punishments, logs, verification, welcome, roles, settings, contact
│  ├─ permissions/ # assertCanManageGuild, assertAdmin
│  ├─ actions/     # wrapper guildAction + akcje per moduł
│  ├─ security/    # csp.ts, hmac.ts, rate-limit.ts, sanitize.ts, turnstile.ts
│  ├─ search/      # fuzzy komend
│  └─ seo/         # metadata helpers, json-ld
├─ content/
│  ├─ docs/{pl,en}/**/*.mdx
│  ├─ commands.ts  # jedyne źródło komend (typowane, z i18n)
│  ├─ changelog/   # seed changeloga (później zarządzany z /admin)
│  └─ legal/{pl,en}/*.mdx
├─ i18n/           # routing.ts, request.ts, navigation.ts
├─ messages/       # pl.json, en.json
├─ prisma/         # schema.prisma, migrations/, seed.ts
├─ styles/         # tokens.css, globals.css, prose.css
├─ public/         # fonts?, fallback/*.avif, basis/, og-base.png
├─ tests/
│  ├─ unit/        # vitest
│  └─ e2e/         # Playwright
├─ proxy.ts        # i18n + CSP nonce + nagłówki + ochrona /dashboard i /admin
├─ Dockerfile, docker-compose.yml, .env.example, README.md
└─ next.config.ts, eslint.config.mjs, prettier.config.mjs, vitest.config.ts, playwright.config.ts
```

---

## 3. Mapa podstron

### 3.1 Część publiczna (`/` = PL, `/en/...` = EN)

| Ścieżka                                            | Zawartość                                                                                                                                                                                                                | 3D / animacje                            | Render                    |
| -------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------- | ------------------------- |
| `/`                                                | Hero (CTA „Dodaj do Discorda” / „Panel”), Zaufanie (3 liczniki), Storytelling „Jak WithBot chroni serwer”, Funkcje (karty tilt), Statystyki + glob, Porównanie, Opinie, FAQ, Finalne CTA                                 | Hero, pole cząsteczek, Story, Glob       | ISR (statystyki co 5 min) |
| `/features`                                        | Siatka modułów                                                                                                                                                                                                           | karty tilt                               | SSG                       |
| `/features/[module]`                               | `automod` (anty-spam, anty-raid, anty-link, filtr słów, anty-caps, anty-mention), `warnings`, `logs`, `verification`, `reaction-roles`, `welcome`, `tickets`; opis, konfiguracja krok po kroku, interaktywne demo modułu | mini-demo w DiscordMock (DOM, nie WebGL) | SSG                       |
| `/commands`                                        | Wyszukiwarka fuzzy (Fuse.js, skrót `/`), filtry kategorii, karty: opis, składnia, uprawnienia, przykład, kopiowanie; stan filtrów w URL                                                                                  | stagger listy                            | SSG + klient              |
| `/premium`                                         | Karty planów (Free / Premium / Pro), przełącznik miesięcznie/rocznie (animacja cyfr), tabela porównawcza, FAQ płatności                                                                                                  | rolling digits                           | SSG                       |
| `/docs/[[...slug]]`                                | MDX, sidebar, TOC (scrollspy), wyszukiwarka (⌘K), bloki kodu z kopiowaniem, poprzedni/następny                                                                                                                           | brak                                     | SSG                       |
| `/status`                                          | Status usług (Bot, API, Baza, Shardy), pasek uptime z 90 dni (tooltip per dzień), incydenty                                                                                                                              | brak                                     | ISR 60 s                  |
| `/changelog`                                       | Wpisy wersji, tagi nowość/poprawka/zmiana, filtr, RSS (`/changelog/rss.xml`)                                                                                                                                             | reveal                                   | ISR                       |
| `/demo`                                            | Symulowany kanał Discorda: użytkownik pisze, silnik reguł (te same funkcje co w testach, uruchamiane w przeglądarce) usuwa, ostrzega, wycisza; panel „co się stało i dlaczego”                                           | animacje wiadomości                      | SSG + klient              |
| `/partners`                                        | Serwery partnerskie (z DB), CTA „Zostań partnerem”                                                                                                                                                                       | hover                                    | ISR                       |
| `/contact`                                         | Formularz (RHF + zod, honeypot, rate limit, Turnstile), link do serwera supportu                                                                                                                                         | brak                                     | dynamiczny (akcja)        |
| `/legal/terms`, `/legal/privacy`, `/legal/cookies` | MDX + baner zgód RODO (kategorie: niezbędne / analityczne / marketingowe, zapis w cookie, „Odrzuć wszystkie” równorzędny z „Akceptuj”)                                                                                   | brak                                     | SSG                       |
| 404                                                | „404” złożone z cząsteczek, które rozsypują się pod kursorem, CTA powrotu                                                                                                                                                | scena 404                                | statyczny                 |

### 3.2 Panel (`/dashboard`)

| Ścieżka                   | Zawartość                                                                                                                       |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| `/login`                  | Logowanie przez Discord                                                                                                         |
| `/dashboard`              | Serwery z Manage Server: z botem („Zarządzaj”) i bez bota („Dodaj WithBota”, link OAuth z `guild_id` i `disable_guild_select`)  |
| `/dashboard/[g]`          | Przegląd: wykresy 24h/7d/30d (usunięte przez AutoMod, kary, nowi członkowie, raidy), ostatnie akcje                             |
| `…/automod`               | Moduły (przełączniki), czułość (slidery), wyjątki (kanały, role), akcja, zakazane słowa (tagi + import .txt/.csv, dzikie karty) |
| `…/antiraid`              | Próg dołączeń / okno czasu, wiek konta, akcja, **Lockdown** jednym kliknięciem (modal potwierdzenia)                            |
| `…/cases`, `…/cases/[id]` | Tabela (szukaj, filtry typ/moderator/status/data, paginacja kursorowa), szczegóły, cofnięcie kary, eksport CSV                  |
| `…/punishments`           | Progi automatycznych kar (np. 3 ostrzeżenia → timeout 1h), sortowalna lista                                                     |
| `…/logs`                  | Kanał logów dla każdego typu zdarzenia                                                                                          |
| `…/verification`          | Tryb (przycisk / captcha), kanał, rola po weryfikacji, wiadomość                                                                |
| `…/welcome`               | Edytor powitań i pożegnań: zmienne `{user}` `{server}` `{memberCount}`, podgląd na żywo w stylu Discorda, embed                 |
| `…/roles`                 | Role reakcyjne (wiadomość + emoji/przycisk → rola, tryby: normal/unique/verify) i auto-role                                     |
| `…/access`                | Kto ma dostęp do panelu (role/użytkownicy)                                                                                      |
| `…/settings`              | Prefiks, język bota, strefa czasowa                                                                                             |
| `…/audit`                 | Historia zmian konfiguracji: kto, co (diff), kiedy                                                                              |

Wspólne: pasek „Masz niezapisane zmiany” (Zapisz/Cofnij, ostrzeżenie przy opuszczeniu strony), optimistic updates, toasty (`aria-live`), skeletony.

### 3.3 Admin (`/admin`, tylko `ADMIN_DISCORD_IDS`; dla innych 404, nie 403)

Statystyki globalne, lista serwerów (szukaj/sortuj), changelog, incydenty, FAQ, opinie, partnerzy, blacklista serwerów i użytkowników.

---

## 4. Sceny 3D: techniki

| #   | Scena                                 | Technika                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | Mobile / low                                                                       |
| --- | ------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| 1   | **Hero: rdzeń/tarcza WithBot**        | Proceduralna tarcza: `ExtrudeGeometry` z bevelem z kształtu SVG, wewnątrz ikosfera „rdzenia” (detail 64). Dwa materiały: chrom (`MeshPhysicalMaterial`, metalness 1, roughness 0.15, clearcoat) i czarne szkło (`transmission`, `thickness`, `ior 1.5`, `attenuationColor` #111). „Oddychanie”: `onBeforeCompile` z wstrzykniętym simplex noise 3D w vertex shaderze (przesunięcie wzdłuż normalnej, przeliczanie normalnych przez sąsiednie próbki) + fresnel rim w fragmencie. Parallax: lerp rotacji i pozycji do znormalizowanego kursora (damping z `maath/easing`). Orbitujące cząsteczki: `Points` (2–6 tys.) z orbitami liczonymi w vertex shaderze (zero pracy CPU). **Rozpad przy scrollu**: `MeshSurfaceSampler` próbkuje powierzchnię tarczy do bufora `aStart`; drugi bufor `aEnd` (rozproszenie + curl noise); `uProgress` z ScrollTrigger miesza pozycje, mesh zanika, a cząsteczki płyną do pozycji startowych sceny 2 | transmission → fake (env + fresnel), 40% cząsteczek, bez CA                        |
| 2   | **Tło: pole punktów**                 | `InstancedBufferGeometry` / `Points` na siatce 160×100 (16 tys., GPU). Vertex shader: fala sinusoidalna + noise, „fala od kursora” jako promień od `uMouse` (ray → płaszczyzna) z zanikaniem `exp(-d)` i ringiem impulsu przy kliknięciu; głębia = mniejsze i ciemniejsze punkty; `additive blending`, alpha w fragmencie (okrągły punkt bez tekstury)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | 4 tys. punktów, bez fali od kursora na dotyku                                      |
| 3   | **Storytelling „Jak WithBot chroni”** | Sekcja przypięta (ScrollTrigger `pin`, `scrub`) na ~400vh, 4 etapy zsynchronizowane z tekstem (SplitText). Kamera po `CatmullRomCurve3`, `lookAt` po drugiej krzywej. Wiadomości to instancjonowane karty (`InstancedMesh` z zaokrąglonym `RoundedBox`/SDF w shaderze); treść z **atlasu tekstur generowanego w canvas 2D** (avatar + nick + tekst, bez pobierania). Filtr: pierścień/brama z liniami (`Line2`) i świetlistym fresnelem. Spam: karta → wymiana na cząsteczki z tej samej pozycji (shader explode, kierunek + noise), czyste wiadomości przechodzą dalej i łagodnie rozjaśniają się. Etapy: 1. strumień wiadomości, 2. analiza (skan), 3. rozbicie spamu, 4. czysty kanał + licznik                                                                                                                                                                                                                                     | mniej kart (12 zamiast 40), uproszczona kamera; reduced-motion → 4 statyczne kadry |
| 4   | **Karty funkcji: tilt**               | DOM + CSS 3D (`perspective`, `rotateX/Y` przez CSS vars), połysk jako `radial-gradient` podążający za kursorem i maska krawędzi `conic-gradient`. Celowo bez WebGL: tańsze, ostre, dostępne                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | bez tiltu, tylko połysk przy dotyku                                                |
| 5   | **Statystyki: glob**                  | Kula z punktów rozłożonych spiralą Fibonacciego (~12 tys.), filtrowanych maską lądów (mała tekstura 512×256, odczyt przy inicjalizacji), atmosfera jako fresnel na drugiej sferze (BackSide). Punkty serwerów jako instancje z pulsem; łuki połączeń (`QuadraticBezier` + animowany dash w shaderze). Obrót leniwy, drag OrbitControls ograniczony do osi Y. Liczniki obok: GSAP z `snap` + `Intl.NumberFormat`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | 4 tys. punktów, bez łuków                                                          |
| 6   | **Przejścia stron**                   | Overlay w DOM nad treścią (zawsze czarny, więc nie ma białego mrugnięcia): maska `clip-path`/SVG ze szumem animowana przez `motion` w `template.tsx`. Na przeglądarkach z View Transitions API używam `document.startViewTransition` (Next `experimental.viewTransition`) dla płynności, a overlay to fallback. Shader przejścia w canvasie (dissolve z noise) tylko między stronami marketingowymi, bo canvas jest współdzielony                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | tylko fade                                                                         |
| 7   | **Tekst**                             | `SplitText` (GSAP) z maską linii + `autoSplit` (przeliczanie przy resize i po załadowaniu fontu), znak po znaku dla hero, linia po linii dla sekcji; `Scramble` dla wybranych słów (własny hook, znaki z puli `!<>-_\/[]{}—=+*^?#`). `aria-label` z pełnym tekstem, podzielone spany mają `aria-hidden`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | reduced-motion → fade                                                              |
| 8   | **404**                               | „404” z tekstu (`TextGeometry`/troika → próbkowanie punktów), cząsteczki uciekają od kursora (siła w vertex shaderze) i wracają sprężyną                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | statyczne SVG                                                                      |

Postprocessing (z umiarem): `Bloom` (luminanceThreshold 0.8, mipmapBlur), `ChromaticAberration` (offset 0.0006, tylko hero), `Noise` (opacity 0.03, premultiply), `Vignette` (0.35). Grain na stronie robię w CSS (SVG `feTurbulence` jako tło nakładki), nie w postprocessingu, żeby działał też w panelu i bez WebGL.

**Loader** (pierwsze wejście, `sessionStorage`): logo „W” + procent z `useProgress` w rogu ekranu, **nie zasłania nagłówka** (inaczej LCP by ucierpiał). Rdzeń 3D „materializuje się” z cząsteczek, gdy assety są gotowe. Maksymalnie ~1,2 s, potem znika niezależnie od stanu.

---

## 5. Schemat bazy danych (Prisma)

```prisma
generator client {
  provider = "prisma-client"        // przy Prisma 7; przy v6 "prisma-client-js"
  output   = "../lib/db/generated"
}

datasource db {
  provider = "postgresql"
}

// ───────────── Auth.js ─────────────
model User {
  id            String    @id @default(cuid())
  discordId     String    @unique
  name          String?
  image         String?
  email         String?   @unique        // nie pobieramy (brak scope email), pole dla adaptera
  emailVerified DateTime?
  createdAt     DateTime  @default(now())
  accounts      Account[]
  sessions      Session[]
  auditLogs     ConfigAuditLog[]
}

model Account {
  userId            String
  type              String
  provider          String
  providerAccountId String
  refresh_token     String?  @db.Text   // szyfrowane AES-GCM (AUTH_TOKEN_ENC_KEY)
  access_token      String?  @db.Text   // szyfrowane
  expires_at        Int?
  token_type        String?
  scope             String?
  id_token          String?  @db.Text
  session_state     String?
  user              User     @relation(fields: [userId], references: [id], onDelete: Cascade)
  @@id([provider, providerAccountId])
}

model Session {
  sessionToken String   @unique
  userId       String
  expires      DateTime
  user         User     @relation(fields: [userId], references: [id], onDelete: Cascade)
}

model VerificationToken {
  identifier String
  token      String
  expires    DateTime
  @@id([identifier, token])
}

// ───────────── Serwer i ustawienia ─────────────
model Guild {
  id           String   @id                     // Discord guild ID (snowflake)
  name         String
  icon         String?
  ownerId      String
  memberCount  Int      @default(0)
  region       String?                          // zagregowany region z preferredLocale (dla globu)
  botPresent   Boolean  @default(true)
  premiumTier  PremiumTier @default(FREE)
  premiumUntil DateTime?
  joinedAt     DateTime @default(now())
  leftAt       DateTime?
  updatedAt    DateTime @updatedAt

  settings      GuildSettings?
  automod       AutoModRule[]
  bannedWords   BannedWord[]
  antiRaid      AntiRaidConfig?
  thresholds    PunishmentThreshold[]
  logChannels   LogChannel[]
  verification  VerificationConfig?
  greetings     GreetingConfig[]
  reactionRoles ReactionRoleMessage[]
  autoRoles     AutoRole[]
  access        DashboardAccess[]
  cases         ModCase[]
  stats         GuildStatBucket[]
  auditLogs     ConfigAuditLog[]
  @@index([botPresent])
}

model GuildSettings {
  guildId  String @id
  prefix   String @default("!")   @db.VarChar(5)
  locale   BotLocale @default(PL)
  timezone String @default("Europe/Warsaw")
  version  Int    @default(1)      // optimistic concurrency przy zapisie
  guild    Guild  @relation(fields: [guildId], references: [id], onDelete: Cascade)
}

// ───────────── AutoMod ─────────────
model AutoModRule {
  id              String       @id @default(cuid())
  guildId         String
  module          AutoModModule
  enabled         Boolean      @default(false)
  sensitivity     Int          @default(50)        // 0–100, mapowane na progi modułu
  actions         ModAction[]                      // np. [DELETE, WARN]
  muteDuration    Int?                             // sekundy (dla TIMEOUT)
  exemptChannels  String[]
  exemptRoles     String[]
  options         Json         @default("{}")      // typowane schematem zod per moduł
  version         Int          @default(1)
  guild           Guild        @relation(fields: [guildId], references: [id], onDelete: Cascade)
  @@unique([guildId, module])
}

model BannedWord {
  id        String   @id @default(cuid())
  guildId   String
  pattern   String   @db.VarChar(100)
  matchType WordMatch @default(CONTAINS)
  createdBy String
  createdAt DateTime @default(now())
  guild     Guild    @relation(fields: [guildId], references: [id], onDelete: Cascade)
  @@unique([guildId, pattern])
}

model AntiRaidConfig {
  guildId          String  @id
  enabled          Boolean @default(false)
  joinThreshold    Int     @default(10)      // dołączeń…
  joinWindowSec    Int     @default(10)      // …w ciągu N sekund
  minAccountAgeDays Int    @default(0)
  action           ModAction @default(KICK)
  lockdownActive   Boolean @default(false)
  lockdownAt       DateTime?
  lockdownBy       String?
  version          Int     @default(1)
  guild            Guild   @relation(fields: [guildId], references: [id], onDelete: Cascade)
}

model PunishmentThreshold {
  id        String    @id @default(cuid())
  guildId   String
  warnCount Int                      // np. 3
  action    ModAction                // np. TIMEOUT
  duration  Int?                     // sekundy
  guild     Guild     @relation(fields: [guildId], references: [id], onDelete: Cascade)
  @@unique([guildId, warnCount])
}

// ───────────── Logi, weryfikacja, powitania, role ─────────────
model LogChannel {
  guildId   String
  eventType LogEvent
  channelId String?
  enabled   Boolean @default(true)
  guild     Guild   @relation(fields: [guildId], references: [id], onDelete: Cascade)
  @@id([guildId, eventType])
}

model VerificationConfig {
  guildId   String  @id
  enabled   Boolean @default(false)
  mode      VerificationMode @default(BUTTON)
  channelId String?
  roleId    String?
  message   String? @db.VarChar(2000)
  version   Int     @default(1)
  guild     Guild   @relation(fields: [guildId], references: [id], onDelete: Cascade)
}

model GreetingConfig {
  guildId   String
  type      GreetingType            // WELCOME | GOODBYE
  enabled   Boolean @default(false)
  channelId String?
  content   String? @db.VarChar(2000)   // sanityzowane, zmienne {user} {server} {memberCount}
  embed     Json?                       // tytuł, opis, kolor (mono), obraz; walidowane zod
  dm        Boolean @default(false)
  version   Int     @default(1)
  guild     Guild   @relation(fields: [guildId], references: [id], onDelete: Cascade)
  @@id([guildId, type])
}

model ReactionRoleMessage {
  id        String  @id @default(cuid())
  guildId   String
  channelId String
  messageId String?                // ustawiany przez bota po wysłaniu
  title     String  @db.VarChar(256)
  mode      ReactionRoleMode @default(NORMAL)
  style     ReactionRoleStyle @default(BUTTONS)
  entries   ReactionRoleEntry[]
  guild     Guild   @relation(fields: [guildId], references: [id], onDelete: Cascade)
}

model ReactionRoleEntry {
  id        String @id @default(cuid())
  messageId String
  emoji     String?
  label     String? @db.VarChar(80)
  roleId    String
  position  Int
  message   ReactionRoleMessage @relation(fields: [messageId], references: [id], onDelete: Cascade)
}

model AutoRole {
  guildId  String
  roleId   String
  delaySec Int     @default(0)
  botsOnly Boolean @default(false)
  guild    Guild   @relation(fields: [guildId], references: [id], onDelete: Cascade)
  @@id([guildId, roleId])
}

model DashboardAccess {
  id        String  @id @default(cuid())
  guildId   String
  subjectType AccessSubject           // USER | ROLE
  subjectId String
  level     AccessLevel @default(EDITOR)  // VIEWER | EDITOR
  guild     Guild   @relation(fields: [guildId], references: [id], onDelete: Cascade)
  @@unique([guildId, subjectType, subjectId])
}

// ───────────── Moderacja i statystyki ─────────────
model ModCase {
  id          String    @id @default(cuid())
  guildId     String
  caseNumber  Int                         // numeracja per serwer
  type        CaseType
  targetId    String
  targetTag   String?
  moderatorId String                      // "automod" dla akcji automatycznych
  reason      String?   @db.VarChar(1000)
  source      CaseSource @default(MANUAL)
  automodModule AutoModModule?
  duration    Int?
  expiresAt   DateTime?
  revokedAt   DateTime?
  revokedBy   String?
  revokeReason String?  @db.VarChar(500)
  createdAt   DateTime  @default(now())
  guild       Guild     @relation(fields: [guildId], references: [id], onDelete: Cascade)
  @@unique([guildId, caseNumber])
  @@index([guildId, createdAt(sort: Desc)])
  @@index([guildId, targetId])
  @@index([guildId, type])
}

model GuildStatBucket {
  guildId     String
  metric      StatMetric
  bucketStart DateTime          // pełna godzina (UTC)
  value       Int      @default(0)
  guild       Guild    @relation(fields: [guildId], references: [id], onDelete: Cascade)
  @@id([guildId, metric, bucketStart])
}

model ConfigAuditLog {
  id        String   @id @default(cuid())
  guildId   String
  actorId   String
  section   String                // "automod.ANTI_SPAM", "settings"…
  diff      Json                  // { path, before, after }[]
  createdAt DateTime @default(now())
  guild     Guild    @relation(fields: [guildId], references: [id], onDelete: Cascade)
  actor     User     @relation(fields: [actorId], references: [id])
  @@index([guildId, createdAt(sort: Desc)])
}

// ───────────── Treści (admin) ─────────────
model ChangelogEntry {
  id          String   @id @default(cuid())
  version     String   @unique
  publishedAt DateTime
  titlePl     String
  titleEn     String
  items       ChangelogItem[]
  published   Boolean  @default(true)
}

model ChangelogItem {
  id      String @id @default(cuid())
  entryId String
  tag     ChangelogTag
  textPl  String
  textEn  String
  position Int
  entry   ChangelogEntry @relation(fields: [entryId], references: [id], onDelete: Cascade)
}

model ServiceCheck {               // surowe próbki (retencja 7 dni)
  id        BigInt   @id @default(autoincrement())
  service   ServiceKey
  ok        Boolean
  latencyMs Int?
  checkedAt DateTime @default(now())
  @@index([service, checkedAt])
}

model UptimeDay {                  // agregat do paska 90 dni
  service   ServiceKey
  day       DateTime @db.Date
  uptimePct Float
  @@id([service, day])
}

model Incident {
  id         String   @id @default(cuid())
  titlePl    String
  titleEn    String
  severity   IncidentSeverity
  status     IncidentStatus
  services   ServiceKey[]
  startedAt  DateTime
  resolvedAt DateTime?
  updates    IncidentUpdate[]
}

model IncidentUpdate {
  id         String @id @default(cuid())
  incidentId String
  status     IncidentStatus
  messagePl  String
  messageEn  String
  createdAt  DateTime @default(now())
  incident   Incident @relation(fields: [incidentId], references: [id], onDelete: Cascade)
}

model Faq {
  id         String @id @default(cuid())
  scope      FaqScope              // HOME | PREMIUM
  questionPl String
  questionEn String
  answerPl   String
  answerEn   String
  position   Int
  published  Boolean @default(true)
}

model Testimonial {
  id        String  @id @default(cuid())
  author    String
  role      String                 // "Właściciel serwera X"
  avatarUrl String?
  quotePl   String
  quoteEn   String
  position  Int
  published Boolean @default(true)
}

model Partner {
  id          String  @id @default(cuid())
  guildId     String?
  name        String
  iconUrl     String?
  inviteUrl   String?
  memberCount Int?
  descPl      String?
  descEn      String?
  position    Int
  published   Boolean @default(true)
}

model BlacklistEntry {
  id        String        @id @default(cuid())
  type      BlacklistType         // GUILD | USER
  targetId  String
  reason    String?
  createdBy String
  createdAt DateTime      @default(now())
  expiresAt DateTime?
  @@unique([type, targetId])
}

model ContactMessage {
  id        String   @id @default(cuid())
  name      String
  email     String
  topic     ContactTopic
  message   String   @db.VarChar(5000)
  ipHash    String                 // SHA-256 z solą, nie surowe IP
  createdAt DateTime @default(now())
  handled   Boolean  @default(false)
}

model RateLimitHit {               // fallback, gdy nie ma Redis
  key       String
  windowStart DateTime
  count     Int
  @@id([key, windowStart])
}

model WebhookNonce {               // ochrona przed replay
  nonce     String   @id
  createdAt DateTime @default(now())
}

// ───────────── Enumy ─────────────
enum PremiumTier      { FREE PREMIUM PRO }
enum BotLocale        { PL EN }
enum AutoModModule    { ANTI_SPAM ANTI_RAID ANTI_LINK WORD_FILTER ANTI_CAPS ANTI_MENTION ANTI_INVITE }
enum ModAction        { DELETE WARN TIMEOUT KICK BAN }
enum WordMatch        { EXACT CONTAINS WILDCARD }
enum LogEvent         { MESSAGE_DELETE MESSAGE_EDIT MEMBER_JOIN MEMBER_LEAVE MEMBER_BAN MEMBER_UNBAN ROLE_UPDATE NICK_UPDATE CHANNEL_UPDATE VOICE MOD_ACTION AUTOMOD }
enum VerificationMode { BUTTON CAPTCHA }
enum GreetingType     { WELCOME GOODBYE }
enum ReactionRoleMode { NORMAL UNIQUE VERIFY }
enum ReactionRoleStyle { REACTIONS BUTTONS SELECT }
enum AccessSubject    { USER ROLE }
enum AccessLevel      { VIEWER EDITOR }
enum CaseType         { WARN TIMEOUT KICK BAN SOFTBAN UNBAN NOTE }
enum CaseSource       { MANUAL AUTOMOD AUTO_PUNISHMENT ANTI_RAID }
enum StatMetric       { AUTOMOD_DELETED PUNISHMENTS MEMBER_JOINS MEMBER_LEAVES RAIDS MESSAGES }
enum ChangelogTag     { NEW FIX CHANGE }
enum ServiceKey       { BOT API DATABASE SHARDS DASHBOARD }
enum IncidentSeverity { MINOR MAJOR CRITICAL MAINTENANCE }
enum IncidentStatus   { INVESTIGATING IDENTIFIED MONITORING RESOLVED }
enum FaqScope         { HOME PREMIUM }
enum BlacklistType    { GUILD USER }
enum ContactTopic     { SUPPORT BUG PARTNERSHIP PREMIUM OTHER }
```

Decyzje:

- **Komendy nie trafiają do DB**: `content/commands.ts` to jedyne źródło (typowane, PL/EN), wystawiane też przez `/api/commands`, więc bot może je stamtąd synchronizować.
- `options Json` w AutoModRule ma sens, bo każdy moduł ma inne pola (np. anty-caps: `%` i min. długość; anty-mention: limit). Zawsze walidowane zod: rozróżnialna unia po `module`, typy wyprowadzane z `z.infer`.
- `version` daje optimistic concurrency: zapis odrzucany, jeśli ktoś w międzyczasie zmienił sekcję („Ktoś zmienił te ustawienia, odśwież”).
- Statystyki są godzinowe (bot inkrementuje `upsert ... value = value + 1` albo wysyła paczki przez webhook). 30 dni × 6 metryk × 24 h to mała tabela per serwer.
- Glob używa wyłącznie `Guild.region` zagregowanego do liczby serwerów na region. Discord nie podaje lokalizacji serwera, więc region wyprowadzam z `preferredLocale`. To przybliżenie i zaznaczę je w podpisie.

---

## 6. Design system

### 6.1 Tokeny (`styles/tokens.css`, szkic)

```css
:root {
  /* Kolor */
  --color-bg: #000000;
  --color-bg-elevated: #050505;
  --color-surface-1: #0a0a0a;
  --color-surface-2: #0f0f0f;
  --color-surface-3: #141414;
  --color-border: rgb(255 255 255 / 0.08);
  --color-border-strong: rgb(255 255 255 / 0.16);
  --color-fg: #ffffff;
  --color-fg-muted: #a1a1a1; /* 8.0:1 na #000 */
  --color-fg-subtle: #6b6b6b; /* 4.0:1 na #000: tylko duży tekst i dekoracje, drobny tekst ≥ #8a8a8a */
  --color-success: #7fae8f; /* stonowane, tylko panel */
  --color-warning: #c8a96a;
  --color-danger: #c27c7c;
  --color-glass: rgb(20 20 20 / 0.55);
  --color-glass-edge: rgb(255 255 255 / 0.1);
  --color-focus: #ffffff;

  /* Typografia (fluid, clamp 360 → 1920 px) */
  --font-sans: var(--font-geist-sans), system-ui, sans-serif;
  --font-display: var(--font-display-face), var(--font-sans);
  --font-mono: var(--font-geist-mono), ui-monospace, monospace;
  --text-xs: 0.75rem;
  --text-sm: 0.875rem;
  --text-base: 1rem;
  --text-lg: 1.125rem;
  --text-xl: clamp(1.25rem, 1.1rem + 0.5vw, 1.5rem);
  --text-2xl: clamp(1.5rem, 1.2rem + 1vw, 2.25rem);
  --text-3xl: clamp(2rem, 1.4rem + 2.2vw, 3.5rem);
  --text-display: clamp(3rem, 1.2rem + 8vw, 11rem);
  --leading-display: 0.88;
  --tracking-display: -0.045em;
  --tracking-tight: -0.02em;

  /* Spacing (bazowo 4 px) */
  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-6: 1.5rem;
  --space-8: 2rem;
  --space-12: 3rem;
  --space-16: 4rem;
  --space-24: 6rem;
  --space-32: 8rem;
  --section-y: clamp(5rem, 3rem + 8vw, 12rem);

  /* Siatka */
  --container-max: 90rem; /* 1440 */
  --gutter: clamp(1rem, 0.5rem + 2vw, 2.5rem);
  --grid-columns: 12;

  /* Promienie */
  --radius-xs: 4px;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 20px;
  --radius-xl: 28px;
  --radius-full: 9999px;

  /* Efekty */
  --blur-glass: 18px;
  --shadow-lift: 0 10px 30px -10px rgb(255 255 255 / 0.15);
  --grain-opacity: 0.045;

  /* Ruch */
  --ease-out-expo: cubic-bezier(0.16, 1, 0.3, 1);
  --ease-in-out-quart: cubic-bezier(0.76, 0, 0.24, 1);
  --dur-fast: 150ms;
  --dur-base: 300ms;
  --dur-slow: 700ms;
  --dur-page: 900ms;

  /* Warstwy */
  --z-canvas: 0;
  --z-content: 10;
  --z-nav: 50;
  --z-overlay: 80;
  --z-toast: 90;
  --z-cursor: 100;
}

@media (prefers-reduced-motion: reduce) {
  :root {
    --dur-base: 0ms;
    --dur-slow: 150ms;
    --dur-page: 200ms;
  }
}
```

Tailwind v4: `@theme inline { --color-bg: var(--color-bg); … }`, więc klasy `bg-bg`, `text-fg-muted`, `rounded-lg` itd. czytają tokeny bezpośrednio.

### 6.2 Typografia

- Treść i UI: **Geist Sans** (+ Geist Mono do kodu i składni komend), przez pakiet `geist` / `next/font`.
- Display (do wyboru, patrz pytania): **A)** Clash Display (Fontshare; licencja ITF FFL, pozwala na użycie komercyjne, sprawdzę polskie znaki) **B)** Bricolage Grotesque (Google Fonts, variable, `latin-ext`, pewne polskie znaki) **C)** Inter Tight 700–800 z mocnym ujemnym trackingiem (najbezpieczniejsza, mniej charakteru).
- Hero: `--text-display`, `leading 0.88`, `tracking -0.045em`, `text-wrap: balance`.

### 6.3 Komponenty i zachowania

| Komponent               | Zachowanie                                                                                                                                                                                                                                                                                       |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `Button` primary        | białe tło, czarny tekst; hover: magnes (siła 0.3, `motion` spring, wyłączony na dotyku i przy reduced-motion), połysk (pseudo-element z gradientem `translateX` −100% → 100%), `translateY(-2px)` + `--shadow-lift`; `asChild` (Slot) dla linków                                                 |
| `Button` secondary      | transparentne, `1px` biała ramka; hover: biały `scale(0) → scale(1)` od środka (pseudo-element, `clip-path: circle()` od punktu kursora), tekst odwraca się na czarny                                                                                                                            |
| `Button` ghost / danger | panel                                                                                                                                                                                                                                                                                            |
| `Card`                  | glass: `--color-glass` + `backdrop-filter: blur(var(--blur-glass))` + krawędź 1px z gradientem maski; wariant `solid` do panelu (bez blur = szybciej)                                                                                                                                            |
| Focus                   | `outline: 2px solid #fff; outline-offset: 3px` (tylko `:focus-visible`), na białych przyciskach dodatkowy czarny wewnętrzny ring                                                                                                                                                                 |
| Cursor                  | punkt 6 px (natychmiastowy) + pierścień 36 px (lerp 0.15) w `mix-blend-mode: difference`, stany `hover` (powiększenie ×1.8), `text` (pionowa kreska), `drag`; ukryty dla `(pointer: coarse)` i reduced-motion; natywny kursor zostaje na polach formularzy                                       |
| Grain                   | `position: fixed`, SVG `feTurbulence` jako data URI, `pointer-events: none`, `opacity var(--grain-opacity)`, lekko animowany `steps()` (wyłączony przy reduced-motion)                                                                                                                           |
| Pozostałe               | Badge, Input, Textarea, Select, Switch, Slider, Tabs, Modal, Toast, Tooltip, Dropdown, Skeleton, Table, Pagination: na **Radix UI primitives** (dostępność, focus trap, obsługa klawiatury), własne style. Toasty przez `sonner` ostylowane tokenami lub własny komponent z `aria-live="polite"` |

---

## 7. Bezpieczeństwo (podsumowanie)

- **CSP z nonce** generowanym w `proxy.ts` (`script-src 'self' 'nonce-…' 'strict-dynamic'`, `worker-src blob:` dla dekoderów Draco/KTX2, `img-src 'self' cdn.discordapp.com data: blob:`, `frame-src challenges.cloudflare.com`). Uwaga: nonce wymusza dynamiczny render HTML. Alternatywa to CSP bez nonce dla stron statycznych. Zalecam nonce w panelu i admin, a dla marketingu SRI (`experimental.sri`) + `'self'`, żeby zachować SSG/ISR i wynik Lighthouse.
- HSTS (`max-age=63072000; includeSubDomains; preload`), `X-Frame-Options: DENY` + `frame-ancestors 'none'`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy`, `X-Content-Type-Options: nosniff`, COOP.
- CSRF: Server Actions mają wbudowaną kontrolę `Origin`; dla Route Handlerów z mutacją sprawdzam `Origin`/`Host` i stosuję cookies `SameSite=Lax`, `__Host-` prefix.
- Webhook bota: `X-WithBot-Signature: sha256=HMAC(secret, timestamp + "." + body)`, `timingSafeEqual`, okno ±5 min, nonce w `WebhookNonce`.
- Tokeny Discord użytkownika szyfrowane w DB (AES-256-GCM), token bota tylko w `server-only` modułach.
- Rate limit: logowanie, formularz kontaktowy (5/h/IP), Server Actions (30/min/użytkownik/serwer), API publiczne.
- Sanityzacja: treści powitań (limit znaków, usunięcie `@everyone/@here` jeśli nie zezwolono, białolista markdown), MDX tylko z repo (zaufane), opinie/partnerzy z admina escapowane.

## 8. SEO i dostępność

- `generateMetadata` per strona i język, `alternates.languages` (hreflang), OG przez `next/og` (czarne tło, „W”, tytuł w foncie display, grain), sitemap z wariantami językowymi, robots (blokada `/dashboard`, `/admin`, `/api`), JSON-LD: `SoftwareApplication` (home, premium z `offers`), `FAQPage` (home, premium), `BreadcrumbList` (docs).
- Sceny 3D: `role="img"` + `aria-label` z opisem, a cały storytelling ma tekst w DOM (scena tylko go ilustruje).
- Skip link, logiczna kolejność, focus trap w modalach i mobilnym menu, `aria-current` w nawigacji, kontrast AA (sprawdzany testem axe w Playwright).
- Breakpointy: 360 / 768 / 1280 / 1920+; mobilne menu pełnoekranowe (clip-path reveal + stagger linków).

## 9. Testy

- **vitest**: walidatory (każdy schemat: poprawne/niepoprawne przypadki, graniczne wartości sliderów, duplikaty progów), uprawnienia (bitfield, owner, admin, DashboardAccess, blacklista, brak bota), wyszukiwarka komend (literówki, aliasy, PL/EN), HMAC, sanityzacja i podstawianie zmiennych powitań, silnik reguł dema, diff audit logu, rate limiter.
- **Playwright**: strona główna się ładuje (H1, CTA, brak błędów konsoli, fallback przy wyłączonym WebGL), przełączanie języka, wyszukiwarka komend, formularz kontaktowy (walidacja), logowanie (mock: provider `Credentials` aktywny tylko przy `E2E_AUTH_MOCK=1` + mock Discord API przez MSW/route interception), zapis AutoMod (zmiana → pasek niezapisanych zmian → zapis → toast → odświeżenie zachowuje wartość), axe na kluczowych stronach.
- Po każdym etapie: `pnpm lint && pnpm typecheck && pnpm test && pnpm build` (+ E2E tam, gdzie dotyczy). Postgres w testach: kontener z `docker-compose` albo, jeśli w środowisku nie ma Dockera, PGlite / lokalny `postgres`. Sprawdzę, co jest dostępne.

---

## 10. Etapy wdrożenia

| Etap | Zakres                                                                                                                                                                                                       | Weryfikacja                                            |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------ |
| 1    | Szkielet: Next + TS strict, Tailwind v4 + tokeny, ESLint/Prettier/husky, env (zod), next-intl (PL/EN), vitest + Playwright, Docker + compose                                                                 | lint, typecheck, build, test „smoke”                   |
| 2    | Design system: fonty, `components/ui/*`, kursor, grain, Lenis + GSAP, nav + mobilne menu, footer, przejścia stron, motion (SplitReveal, Scramble, Magnetic, Tilt, Counter), strona `/styleguide` (tylko dev) | testy komponentów, axe                                 |
| 3    | Rdzeń 3D: CanvasRoot + View, QualityProvider, fallback, loader, scena hero (shader, cząsteczki, rozpad), pole punktów                                                                                        | Lighthouse lokalnie, test z wyłączonym WebGL           |
| 4    | Strona główna: wszystkie sekcje, storytelling 3D, glob, liczniki, porównanie, opinie, FAQ, CTA, treści PL/EN                                                                                                 | E2E home, Lighthouse                                   |
| 5    | Prisma + seed, Auth.js (Discord), `lib/discord`, `lib/bot-api`, uprawnienia, webhook HMAC, `/api/status`                                                                                                     | unit (uprawnienia, HMAC), migracje                     |
| 6    | Podstrony publiczne: funkcje + dema, komendy, premium, status, changelog, demo kanału, partnerzy, kontakt, prawne + baner cookies, 404                                                                       | E2E komendy, kontakt                                   |
| 7    | Dokumentacja MDX (sidebar, TOC, wyszukiwarka, kopiowanie kodu, poradniki)                                                                                                                                    | build, linki                                           |
| 8    | Panel: lista serwerów, przegląd z wykresami, AutoMod, anty-raid, sprawy + CSV, auto-kary, logi, weryfikacja, powitania (edytor + podgląd), role, dostęp, ustawienia, audit log, pasek niezapisanych zmian    | unit walidatorów, E2E zapis AutoMod                    |
| 9    | Admin: treści, incydenty, statystyki globalne, serwery, blacklista                                                                                                                                           | unit `assertAdmin`, E2E dostępu                        |
| 10   | Hardening: CSP/nagłówki, rate limit, Sentry, SEO (OG, sitemap, JSON-LD), przegląd a11y i responsywności, budżety wydajności, README                                                                          | Lighthouse (desktop ≥ 90, mobile ≥ 75), axe, pełne E2E |

---

## 11. Pytania przed startem

1. **Gdzie jest WithBot?** W repo jest tylko bot ticketów (NightShop/FutureShop, discord.js). Czy WithBot już istnieje (inne repo?), czy na razie strona ma działać na danych seed, a bot powstanie później według tego schematu? Od tego zależy domyślny wariant `lib/bot-api` (proponuję `database`).
2. **Umiejscowienie**: nowa aplikacja w `withcode/` w tym repo (obecny bot bez zmian). OK, czy wolisz osobne repo albo monorepo (pnpm workspaces: `apps/web`, `apps/bot`, `packages/db`)?
3. **Premium**: tylko cennik (CTA → Discord/kontakt), czy od razu płatności (Stripe Checkout + webhook, statusy w `Guild.premiumTier`)?
4. **Font display**: A) Clash Display, B) Bricolage Grotesque, C) Inter Tight.
5. **Porównanie z innymi botami**: z nazwami (MEE6, Dyno, Carl-bot; wymaga Twojej weryfikacji faktów) czy anonimowo („Typowy bot moderacyjny” vs WithBot)?
6. **URL dla PL**: bez prefiksu (`withcode.pl/funkcje`, EN: `/en/features`) czy zawsze z prefiksem (`/pl/...`)? Proponuję bez prefiksu i z polskimi slugami dla PL (`/funkcje`, `/komendy`, `/cennik`, `/dokumentacja`), przez `pathnames` w next-intl.
7. **Redis** (Upstash) dla rate limitu i cache, czy wszystko na Postgres (prościej na VPS)?
8. **Menedżer pakietów**: pnpm (proponuję) czy npm?
