/**
 * Przykładowe treści: używane przez `prisma db seed` oraz jako fallback, gdy baza jest
 * niedostępna (np. podczas pierwszego buildu). Po seedzie zarządzasz nimi w /admin.
 */
export const seedChangelog = [
  {
    version: "2.4.0",
    publishedAt: "2026-10-28",
    titlePl: "Panel 2.0 i historia zmian",
    titleEn: "Dashboard 2.0 and audit log",
    items: [
      {
        tag: "NEW",
        textPl: "Nowy panel z podglądem powitań na żywo.",
        textEn: "New dashboard with a live greeting preview.",
      },
      {
        tag: "NEW",
        textPl: "Historia zmian konfiguracji: kto, co i kiedy.",
        textEn: "Configuration audit log: who, what and when.",
      },
      {
        tag: "CHANGE",
        textPl: "Suwaki czułości zamiast ręcznych progów.",
        textEn: "Sensitivity sliders instead of manual thresholds.",
      },
      {
        tag: "FIX",
        textPl: "Eksport CSV poprawnie koduje polskie znaki.",
        textEn: "CSV export now encodes Polish characters correctly.",
      },
    ],
  },
  {
    version: "2.3.2",
    publishedAt: "2026-09-30",
    titlePl: "Poprawki stabilności",
    titleEn: "Stability fixes",
    items: [
      {
        tag: "FIX",
        textPl: "Anty-raid nie reaguje już na powroty po krótkiej przerwie w połączeniu.",
        textEn: "Anti-raid no longer reacts to rejoins after a short connection drop.",
      },
      {
        tag: "FIX",
        textPl: "Timeout z automatycznych kar respektuje limit 28 dni.",
        textEn: "Auto punishment timeouts respect the 28-day limit.",
      },
    ],
  },
  {
    version: "2.3.0",
    publishedAt: "2026-09-02",
    titlePl: "Filtr słów nowej generacji",
    titleEn: "Next-generation word filter",
    items: [
      {
        tag: "NEW",
        textPl: "Filtr słów rozpoznaje l33t speak i powtórzone litery.",
        textEn: "The word filter recognises l33t speak and repeated letters.",
      },
      {
        tag: "NEW",
        textPl: "Import zakazanych słów z plików .txt i .csv.",
        textEn: "Banned word import from .txt and .csv files.",
      },
      {
        tag: "CHANGE",
        textPl: "Anty-link ma białą listę domen.",
        textEn: "Anti-link now has a domain allowlist.",
      },
    ],
  },
  {
    version: "2.2.0",
    publishedAt: "2026-07-15",
    titlePl: "Weryfikacja z captchą",
    titleEn: "Captcha verification",
    items: [
      {
        tag: "NEW",
        textPl: "Captcha obrazkowa jako metoda weryfikacji.",
        textEn: "Image captcha as a verification method.",
      },
      {
        tag: "FIX",
        textPl: "Role reakcyjne działają po restarcie bota bez ponownej konfiguracji.",
        textEn: "Reaction roles keep working after a bot restart without reconfiguration.",
      },
    ],
  },
] as const;

export const seedFaq = [
  {
    scope: "HOME",
    questionPl: "Czy WithBot jest darmowy?",
    questionEn: "Is WithBot free?",
    answerPl:
      "Tak. Darmowy plan zawiera wszystkie moduły AutoModu, anty-raid, sprawy i logi. Premium dodaje wyższe limity, captchę i dłuższą historię.",
    answerEn:
      "Yes. The free plan includes every AutoMod module, anti-raid, cases and logs. Premium adds higher limits, captcha and longer history.",
  },
  {
    scope: "HOME",
    questionPl: "Jakich uprawnień potrzebuje bot?",
    questionEn: "Which permissions does the bot need?",
    answerPl:
      "Zarządzanie wiadomościami, moderowanie członków, wyrzucanie, banowanie, zarządzanie rolami i kanałami. Rola bota musi być wyżej niż role, którymi ma zarządzać.",
    answerEn:
      "Manage Messages, Moderate Members, Kick, Ban, Manage Roles and Manage Channels. The bot's role must be above the roles it manages.",
  },
  {
    scope: "HOME",
    questionPl: "Czy WithBot czyta moje wiadomości?",
    questionEn: "Does WithBot read my messages?",
    answerPl:
      "Bot analizuje wiadomości w pamięci, żeby wykryć naruszenia. Treść zapisujemy tylko wtedy, gdy włączysz logi usuniętych wiadomości — i tylko na Twoim kanale logów.",
    answerEn:
      "The bot analyses messages in memory to detect violations. Content is stored only if you enable deleted-message logs — and only in your log channel.",
  },
  {
    scope: "HOME",
    questionPl: "Kto ma dostęp do panelu serwera?",
    questionEn: "Who can access the server dashboard?",
    answerPl:
      "Każdy z uprawnieniem Zarządzanie serwerem. Dodatkowe role i osoby możesz dodać w zakładce Dostęp do panelu.",
    answerEn:
      "Anyone with the Manage Server permission. You can add extra roles and people in the Dashboard access tab.",
  },
  {
    scope: "HOME",
    questionPl: "Czy mogę przenieść ustawienia z innego bota?",
    questionEn: "Can I migrate settings from another bot?",
    answerPl:
      "Listę zakazanych słów zaimportujesz z pliku. Pozostałe ustawienia zajmą Ci kilka minut w panelu — napisz na supporcie, a pomożemy.",
    answerEn:
      "You can import the banned word list from a file. The rest takes a few minutes in the dashboard — ask on support and we will help.",
  },
  {
    scope: "PREMIUM",
    questionPl: "Jak mogę zapłacić?",
    questionEn: "How can I pay?",
    answerPl: "Kartą, BLIK-iem lub przelewem. Fakturę VAT wystawiamy automatycznie.",
    answerEn: "By card, BLIK or bank transfer. VAT invoices are issued automatically.",
  },
  {
    scope: "PREMIUM",
    questionPl: "Czy mogę anulować w każdej chwili?",
    questionEn: "Can I cancel at any time?",
    answerPl:
      "Tak. Premium działa do końca opłaconego okresu, potem serwer wraca na plan Free bez utraty konfiguracji.",
    answerEn:
      "Yes. Premium stays active until the end of the paid period, then the server returns to Free without losing its configuration.",
  },
  {
    scope: "PREMIUM",
    questionPl: "Czy jest okres próbny?",
    questionEn: "Is there a trial?",
    answerPl: "Każdy serwer może raz przetestować Premium przez 7 dni za darmo.",
    answerEn: "Every server can try Premium once for 7 days for free.",
  },
  {
    scope: "PREMIUM",
    questionPl: "Czy przenosicie Premium między serwerami?",
    questionEn: "Can Premium be moved between servers?",
    answerPl: "Tak, raz na 30 dni możesz przypisać subskrypcję do innego serwera.",
    answerEn: "Yes, once every 30 days you can assign the subscription to another server.",
  },
] as const;

export const seedTestimonials = [
  {
    author: "Marta „Mrt” K.",
    rolePl: "Administratorka, serwer gamingowy (12 tys. członków)",
    roleEn: "Admin, gaming server (12k members)",
    quotePl:
      "Po włączeniu anty-raidu przestaliśmy budzić się do zalanych kanałów. Lockdown jednym kliknięciem uratował nam weekend.",
    quoteEn:
      "After enabling anti-raid we stopped waking up to flooded channels. One-click lockdown saved our weekend.",
  },
  {
    author: "Paweł D.",
    rolePl: "Moderator, społeczność programistów",
    roleEn: "Moderator, developer community",
    quotePl:
      "Filtr słów w końcu rozumie polskie znaki i kombinacje z cyframi. Ręcznej moderacji jest o połowę mniej.",
    quoteEn:
      "The word filter finally understands Polish characters and digit tricks. Manual moderation dropped by half.",
  },
  {
    author: "Oliwia S.",
    rolePl: "Właścicielka serwera RP",
    roleEn: "Owner of an RP server",
    quotePl:
      "Panel jest tak czytelny, że nowi moderatorzy ogarniają go bez instrukcji. Historia zmian kończy dyskusje „kto to wyłączył”.",
    quoteEn:
      "The dashboard is so clear that new moderators get it without a manual. The audit log ends every “who turned this off” debate.",
  },
  {
    author: "Kamil W.",
    rolePl: "Administrator, serwer szkolny",
    roleEn: "Admin, school server",
    quotePl:
      "Weryfikacja z captchą odcięła boty w pierwszy dzień. Uczniowie nawet nie zauważyli różnicy.",
    quoteEn:
      "Captcha verification cut off bots on day one. Students didn't even notice the difference.",
  },
] as const;

export const seedPartners = [
  {
    name: "Polski Minecraft",
    memberCount: 48210,
    descPl: "Największa polska społeczność Minecrafta.",
    descEn: "The largest Polish Minecraft community.",
    inviteUrl: "https://discord.gg/example1",
  },
  {
    name: "Dev Corner PL",
    memberCount: 15340,
    descPl: "Programiści, code review i oferty pracy.",
    descEn: "Developers, code review and job offers.",
    inviteUrl: "https://discord.gg/example2",
  },
  {
    name: "Anime Station",
    memberCount: 22990,
    descPl: "Anime, manga i cotygodniowe watch-party.",
    descEn: "Anime, manga and weekly watch parties.",
    inviteUrl: "https://discord.gg/example3",
  },
  {
    name: "Valorant Polska",
    memberCount: 31500,
    descPl: "Szukanie drużyn, turnieje i poradniki.",
    descEn: "Team finding, tournaments and guides.",
    inviteUrl: "https://discord.gg/example4",
  },
  {
    name: "Studenci IT",
    memberCount: 8120,
    descPl: "Materiały, notatki i pomoc przed sesją.",
    descEn: "Materials, notes and exam-season help.",
    inviteUrl: "https://discord.gg/example5",
  },
  {
    name: "Retro Gaming PL",
    memberCount: 5640,
    descPl: "Konsole, emulatory i kolekcjonerstwo.",
    descEn: "Consoles, emulators and collecting.",
    inviteUrl: "https://discord.gg/example6",
  },
] as const;

export const seedIncidents = [
  {
    titlePl: "Opóźnienia w reakcji AutoModu na shardzie 3",
    titleEn: "AutoMod response delays on shard 3",
    severity: "MINOR",
    status: "RESOLVED",
    services: ["SHARDS", "BOT"],
    startedAt: "2026-10-12T19:40:00Z",
    resolvedAt: "2026-10-12T20:25:00Z",
    updates: [
      {
        status: "INVESTIGATING",
        messagePl: "Widzimy opóźnienia do 3 s na części serwerów.",
        messageEn: "We are seeing delays of up to 3 s on some servers.",
        createdAt: "2026-10-12T19:45:00Z",
      },
      {
        status: "RESOLVED",
        messagePl: "Shard 3 został zrestartowany, czasy reakcji wróciły do normy.",
        messageEn: "Shard 3 was restarted, response times are back to normal.",
        createdAt: "2026-10-12T20:25:00Z",
      },
    ],
  },
  {
    titlePl: "Planowana migracja bazy danych",
    titleEn: "Scheduled database migration",
    severity: "MAINTENANCE",
    status: "RESOLVED",
    services: ["DATABASE", "DASHBOARD"],
    startedAt: "2026-09-20T03:00:00Z",
    resolvedAt: "2026-09-20T03:18:00Z",
    updates: [
      {
        status: "MONITORING",
        messagePl: "Panel działa w trybie tylko do odczytu na czas migracji.",
        messageEn: "The dashboard is read-only during the migration.",
        createdAt: "2026-09-20T03:00:00Z",
      },
      {
        status: "RESOLVED",
        messagePl: "Migracja zakończona bez utraty danych.",
        messageEn: "Migration completed with no data loss.",
        createdAt: "2026-09-20T03:18:00Z",
      },
    ],
  },
] as const;

/** Zagregowane regiony dla globu: [lat, lon, liczba serwerów]. */
export const seedRegions: { region: string; lat: number; lon: number; count: number }[] = [
  { region: "PL", lat: 52.2, lon: 21.0, count: 9120 },
  { region: "DE", lat: 52.5, lon: 13.4, count: 1240 },
  { region: "GB", lat: 51.5, lon: -0.1, count: 980 },
  { region: "US-E", lat: 40.7, lon: -74.0, count: 1630 },
  { region: "US-W", lat: 37.8, lon: -122.4, count: 870 },
  { region: "BR", lat: -23.5, lon: -46.6, count: 540 },
  { region: "UA", lat: 50.4, lon: 30.5, count: 610 },
  { region: "CZ", lat: 50.1, lon: 14.4, count: 420 },
  { region: "FR", lat: 48.9, lon: 2.35, count: 510 },
  { region: "ES", lat: 40.4, lon: -3.7, count: 380 },
  { region: "JP", lat: 35.7, lon: 139.7, count: 260 },
  { region: "AU", lat: -33.9, lon: 151.2, count: 190 },
  { region: "IN", lat: 19.1, lon: 72.9, count: 230 },
  { region: "TR", lat: 41.0, lon: 28.9, count: 300 },
  { region: "NL", lat: 52.4, lon: 4.9, count: 280 },
  { region: "SE", lat: 59.3, lon: 18.1, count: 210 },
];
