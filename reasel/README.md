# REASEL.pl — sklep streetwear z 3D

Sklep internetowy z odzieżą premium streetwear: ciemny motyw, neonowy akcent **#c6ff00 (acid lime)**,
pełnoekranowa scena 3D, scroll-story sterowane GSAP ScrollTrigger, karuzela kolekcji w 3D, podgląd
produktu 360°, koszyk, ulubione, wyszukiwarka i kasa (mock płatności). Język: polski, waluta: PLN.

## Stack

| Obszar | Technologia |
| --- | --- |
| Framework | Next.js 14 (App Router) + TypeScript |
| Style | Tailwind CSS, fonty Syne + Inter (`@fontsource`, bez zapytań do Google Fonts) |
| 3D | Three.js, React Three Fiber, @react-three/drei, @react-three/postprocessing |
| Animacje | GSAP + ScrollTrigger, Lenis (smooth scroll), Framer Motion |
| Stan | Zustand (koszyk i ulubione zapisywane w `localStorage`) |
| Formularze | react-hook-form + zod |

## Uruchomienie

Wymagany Node.js 18.18+ (zalecany 20 LTS).

```bash
cd reasel
npm install
npm run dev        # http://localhost:3000
```

Pozostałe skrypty:

```bash
npm run build      # build produkcyjny (zawiera lint + sprawdzanie typów)
npm run start      # serwer produkcyjny po buildzie
npm run lint       # ESLint
npm run typecheck  # TypeScript bez emisji
```

Zmienna środowiskowa (opcjonalna, patrz `.env.example`):

```bash
NEXT_PUBLIC_SITE_URL=https://reasel.pl   # używana w metadanych, Open Graph, sitemap i JSON-LD
```

## Struktura projektu

```
reasel/
├─ public/
│  ├─ draco/              # dekoder Draco (lokalnie – bez CDN)
│  ├─ fonts/              # font typeface.json dla logo 3D (Text3D)
│  └─ models/             # tu wrzucasz własne modele .glb
└─ src/
   ├─ app/                # trasy App Router
   │  ├─ page.tsx                 # strona główna
   │  ├─ sklep/                   # lista produktów + filtry
   │  ├─ produkt/[slug]/          # strona produktu (SSG + JSON-LD Product)
   │  ├─ kasa/ i kasa/dziekujemy/ # checkout (mock) + podziękowanie
   │  ├─ o-nas, kontakt, regulamin, polityka-prywatnosci, zwroty, ulubione
   │  ├─ sitemap.ts, robots.ts, manifest.ts, opengraph-image.tsx, icon.svg
   ├─ components/
   │  ├─ 3d/              # sceny R3F: GlobalScene, StoryExperience, CarouselScene, ProductViewer…
   │  │  └─ garments/     # Garment (proceduralny / .glb + efekt eksplozji)
   │  ├─ sections/        # sekcje strony głównej (Hero, NewCollection, Bestsellers, BrandStory…)
   │  ├─ layout/          # Header, Footer, CartDrawer, SearchOverlay, Preloader, CookieBanner…
   │  ├─ ui/              # Cursor, Magnetic, SplitText, Marquee, Curtain, TransitionLink, Modal…
   │  ├─ product/ shop/ checkout/ forms/
   ├─ config/             # site.ts (dane sklepu, dostawy, płatności), models.ts (modele 3D)
   ├─ lib/                # types, products (mock), catalog (warstwa danych), seo, format…
   ├─ store/              # zustand: cart, favorites, ui, scene, transition
   └─ hooks/
```

## Jak to działa (najważniejsze mechanizmy)

- **Globalna scena 3D** (`components/3d/GlobalScene.tsx`) żyje w `layout.tsx`, więc nie jest
  przeładowywana przy zmianie strony – przejścia (kurtyna w `ui/Curtain.tsx`) zachowują ciągłość
  sceny. Na stronie głównej to hero + scroll-story, na podstronach przygaszone cząsteczki.
  W trakcie przejścia renderowanie jest pauzowane.
- **Scroll-story („4D”)** – każda sekcja strony głównej rejestruje się hookiem
  `useStorySection(ref, index)`, który zapisuje ciągłą wartość `sceneState.story` (np. `3.42`).
  `StoryExperience.tsx` interpoluje klatki kluczowe (`KEYFRAMES`): pozycję kamery, położenie modelu,
  eksplozję części, preset materiału (onyx → acid → chrome → bone) i widoczność logo. Edytując tablicę
  `KEYFRAMES` zmienisz całą choreografię.
- **Wydajność** – Three.js ładowany dynamicznie (`next/dynamic`, `ssr: false`), `PerformanceMonitor`
  obniża DPR i wyłącza postprocessing na słabszym sprzęcie, telefony dostają mniej cząsteczek i brak
  efektów, canvas karuzeli renderuje się tylko w viewporcie, a shader przejścia zdjęć tworzy kontekst
  WebGL dopiero przy najechaniu i zwalnia go po zjechaniu kursora.
- **Dostępność** – semantyczny HTML, skip-link, pułapki fokusu w dialogach (koszyk, wyszukiwarka,
  modal, menu), obsługa klawiatury (karuzela: strzałki; wyszukiwarka: `Ctrl/Cmd + K` lub `/`),
  `prefers-reduced-motion` wyłącza Lenisa, tłumienie animacji 3D i animacje CSS.

## Podmiana modelu 3D

Domyślnie ubrania są budowane proceduralnie z geometrii Three.js
(`components/3d/garments/ProceduralGarment.tsx`) – typy: `hoodie`, `jacket`, `tshirt`, `pants`,
`cap`, `bag`. Aby użyć własnego modelu:

1. Skompresuj model Draco (np. [gltf-transform](https://gltf-transform.dev/)):

   ```bash
   npx @gltf-transform/cli optimize bluza.glb public/models/hoodie.glb --compress draco
   ```

2. W pliku `src/config/models.ts` odkomentuj / dodaj wpis:

   ```ts
   export const GARMENT_MODELS = {
     hoodie: '/models/hoodie.glb',
   };
   ```

3. Gotowe. Model jest automatycznie wyśrodkowany i przeskalowany do wysokości ~2.4 jednostki.
   Siatki otrzymują materiał tkaniny sterowany wybranym kolorem wariantu, **chyba że** nazwa siatki
   zawiera `zip`, `metal`, `button` lub `logo` – wtedy zachowują oryginalny materiał.
   Efekt „eksplozji” działa automatycznie – każda siatka modelu to osobna część, więc warto
   eksportować rękawy, kaptur, kieszenie itp. jako oddzielne obiekty.

Typ modelu dla produktu ustawiasz polem `model` w danych produktu.
Logo 3D korzysta z fontu `public/fonts/helvetiker_bold.typeface.json` – własny font możesz
przekonwertować w [facetype.js](https://gero3.github.io/facetype.js/) i podmienić ścieżkę `LOGO_FONT`.

## Podmiana produktów i zdjęć

- **Produkty** są w `src/lib/products.ts` (21 pozycji w 5 kategoriach). Ceny podajemy
  **w groszach** (`34900` = 349,00 zł) – tak jak Stripe i Shopify. Każdy produkt ma warianty kolorów
  (`hex` barwi model 3D, `family` służy do filtrów), rozmiary, rozmiary wyprzedane, skład, pielęgnację
  i tagi (`nowosc`, `bestseller`, `limitowana`).
- **Zdjęcia** to obecnie placeholdery z Unsplash (`unsplash('photo-…')`). Możesz:
  - wrzucić pliki do `public/images/…` i podać ścieżkę `src: '/images/bluza-1.jpg'`, albo
  - użyć zewnętrznego CDN – dodaj jego domenę do `images.remotePatterns` w `next.config.mjs`.
- **Lookbook** – tablica `lookbook` w tym samym pliku.
- **Tabele rozmiarów** – `src/lib/size-chart.ts`.
- **Dane sklepu, koszty dostawy, metody płatności, próg darmowej dostawy** – `src/config/site.ts`.

### Podpięcie prawdziwego backendu (Shopify, Stripe, CMS)

Strony pobierają dane wyłącznie przez funkcje w `src/lib/catalog.ts`
(`getProducts`, `getProductBySlug`, `getProductsByTag`, `getRelatedProducts`). Są asynchroniczne –
wystarczy zastąpić ich ciało zapytaniem `fetch` do Shopify Storefront API / Stripe Products / CMS
i zmapować odpowiedź na typ `Product` z `src/lib/types.ts`. Płatność w kasie jest zamockowana
w `components/checkout/CheckoutForm.tsx` (funkcja `onSubmit`) – tam podłączysz np. Stripe Checkout,
Przelewy24 lub PayU (najlepiej przez Route Handler w `src/app/api/...`).

## Wdrożenie na Vercel

1. Wypchnij repozytorium na GitHub.
2. Na [vercel.com/new](https://vercel.com/new) zaimportuj repozytorium.
3. W ustawieniach projektu ustaw **Root Directory** na `reasel` (aplikacja jest w podkatalogu).
   Framework zostanie wykryty automatycznie (Next.js), domyślne komendy `npm install` / `npm run build`
   są właściwe.
4. Dodaj zmienną środowiskową `NEXT_PUBLIC_SITE_URL` z docelowym adresem (np. `https://reasel.pl`).
5. Kliknij **Deploy**. Po wdrożeniu podepnij domenę w zakładce *Domains*.

Strony produktów są generowane statycznie (SSG), `/sklep` renderowany po stronie serwera (filtr
kategorii z URL trafia do HTML), a sitemap i robots.txt generują się automatycznie.

## Wydajność – uwagi

- Cele: Lighthouse Performance 85+ (desktop), Accessibility 95+. Lokalny audyt wersji produkcyjnej:
  Accessibility 100 (strona główna, sklep, o nas), SEO 100, Best Practices 100, CLS < 0.03.
- Wynik Performance mierz na maszynie z prawdziwym GPU – w środowiskach bez akceleracji (np.
  kontenery CI z programowym WebGL „SwiftShader”) renderowanie 3D obciąża CPU i sztucznie zawyża
  Total Blocking Time.
- Zdjęcia serwuje `next/image` (AVIF/WebP, responsywne rozmiary), fonty są self-hosted, a dekoder
  Draco i font logo mają nagłówki długiego cache.

## Treści prawne

Regulamin, polityka prywatności i strona zwrotów to **wzory** – przed uruchomieniem sprzedaży
uzupełnij dane firmy (NIP, KRS, adres) i zweryfikuj treść z prawnikiem.
