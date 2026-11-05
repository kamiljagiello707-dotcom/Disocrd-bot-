import type { Localized } from "@/lib/utils";
import type { CommandCategory } from "./commands";
import type { AutoModModuleKey } from "@/lib/validators/automod";

export type FeatureModule = {
  slug: string;
  icon:
    | "shield"
    | "zap"
    | "link"
    | "filter"
    | "caps"
    | "at"
    | "gavel"
    | "scroll"
    | "check"
    | "tags"
    | "wave"
    | "ticket";
  name: Localized;
  tagline: Localized;
  description: Localized;
  bullets: Localized[];
  steps: Localized[];
  commandCategory: CommandCategory;
  /** moduły silnika uruchamiane w demie danej podstrony */
  demoModules: AutoModModuleKey[];
  demoMessages: Localized[];
  premium?: boolean;
};

export const features: FeatureModule[] = [
  {
    slug: "automod",
    icon: "shield",
    name: { pl: "AutoMod", en: "AutoMod" },
    tagline: { pl: "Sześć filtrów, zero ręcznej roboty.", en: "Six filters, zero manual work." },
    description: {
      pl: "AutoMod sprawdza każdą wiadomość w czasie rzeczywistym: anty-spam, anty-link, anty-zaproszenia, filtr słów, anty-caps i anty-mention. Każdy moduł ma własną czułość, wyjątki i akcje.",
      en: "AutoMod checks every message in real time: anti-spam, anti-link, anti-invite, word filter, anti-caps and anti-mention. Each module has its own sensitivity, exemptions and actions.",
    },
    bullets: [
      {
        pl: "Anty-spam: limit wiadomości na 5 s i wykrywanie duplikatów",
        en: "Anti-spam: message rate limit per 5 s and duplicate detection",
      },
      {
        pl: "Anty-link z białą listą domen (np. youtube.com, github.com)",
        en: "Anti-link with a domain allowlist (e.g. youtube.com, github.com)",
      },
      {
        pl: "Filtr słów odporny na l33t speak, polskie znaki i powtórzenia liter",
        en: "Word filter resistant to l33t speak, diacritics and repeated letters",
      },
      {
        pl: "Anty-caps i anty-mention z progami procentowymi",
        en: "Anti-caps and anti-mention with percentage thresholds",
      },
      {
        pl: "Wyjątki dla kanałów i ról — np. #memy lub @Moderator",
        en: "Exemptions for channels and roles — e.g. #memes or @Moderator",
      },
    ],
    steps: [
      {
        pl: "Otwórz panel serwera i przejdź do zakładki AutoMod.",
        en: "Open the server dashboard and go to the AutoMod tab.",
      },
      {
        pl: "Włącz moduły przełącznikami i ustaw czułość suwakiem.",
        en: "Turn modules on with switches and set sensitivity with the slider.",
      },
      {
        pl: "Wybierz akcje: usuń, ostrzeż, wycisz, wyrzuć lub zbanuj.",
        en: "Pick actions: delete, warn, timeout, kick or ban.",
      },
      {
        pl: "Zapisz — bot stosuje zmiany w ciągu sekundy.",
        en: "Save — the bot applies changes within a second.",
      },
    ],
    commandCategory: "automod",
    demoModules: [
      "ANTI_SPAM",
      "ANTI_LINK",
      "ANTI_INVITE",
      "WORD_FILTER",
      "ANTI_CAPS",
      "ANTI_MENTION",
    ],
    demoMessages: [
      { pl: "KUPUJCIE TANIE KONTA NITRO TERAZ", en: "BUY CHEAP NITRO ACCOUNTS RIGHT NOW" },
      { pl: "wbijajcie discord.gg/darmowe-nitro", en: "join discord.gg/free-nitro" },
      { pl: "Cześć, ktoś gra dziś wieczorem?", en: "Hey, anyone playing tonight?" },
    ],
  },
  {
    slug: "anti-raid",
    icon: "zap",
    name: { pl: "Anty-raid", en: "Anti-raid" },
    tagline: { pl: "Raid zatrzymany w 3 sekundy.", en: "A raid stopped in 3 seconds." },
    description: {
      pl: "Anty-raid śledzi tempo dołączeń i wiek kont. Gdy w krótkim czasie wpada fala nowych profili, WithBot reaguje automatycznie, a Ty możesz jednym kliknięciem włączyć lockdown całego serwera.",
      en: "Anti-raid tracks join rate and account age. When a wave of new profiles arrives in a short time, WithBot reacts automatically, and you can lock down the whole server with one click.",
    },
    bullets: [
      {
        pl: "Próg dołączeń w oknie czasu, np. 10 kont w 10 s",
        en: "Join threshold within a time window, e.g. 10 accounts in 10 s",
      },
      {
        pl: "Minimalny wiek konta — świeże konta odfiltrowane",
        en: "Minimum account age — fresh accounts filtered out",
      },
      {
        pl: "Lockdown jednym kliknięciem w panelu lub komendą /lockdown",
        en: "One-click lockdown in the dashboard or with /lockdown",
      },
      { pl: "Raport z raidu na kanale logów", en: "Raid report in the log channel" },
    ],
    steps: [
      { pl: "Włącz ochronę w zakładce Anty-raid.", en: "Enable protection in the Anti-raid tab." },
      {
        pl: "Ustaw próg i okno czasu dopasowane do wielkości serwera.",
        en: "Set a threshold and window that fit your server size.",
      },
      {
        pl: "Wybierz akcję wobec kont z raidu: wyrzuć, zbanuj lub wycisz.",
        en: "Pick an action for raid accounts: kick, ban or timeout.",
      },
    ],
    commandCategory: "antiraid",
    demoModules: ["ANTI_SPAM", "ANTI_MENTION"],
    demoMessages: [
      { pl: "@everyone @here RAID RAID RAID", en: "@everyone @here RAID RAID RAID" },
      { pl: "Witam wszystkich!", en: "Hello everyone!" },
    ],
  },
  {
    slug: "warnings",
    icon: "gavel",
    name: { pl: "Ostrzeżenia i kary", en: "Warnings & punishments" },
    tagline: {
      pl: "Sprawiedliwie, konsekwentnie, z historią.",
      en: "Fair, consistent, with history.",
    },
    description: {
      pl: "Każda kara to numerowana sprawa z powodem, moderatorem i czasem trwania. Automatyczne progi eskalują kary bez udziału moderatora, a każdą decyzję możesz cofnąć w panelu.",
      en: "Every punishment is a numbered case with a reason, moderator and duration. Automatic thresholds escalate punishments without a moderator, and you can revoke any decision in the dashboard.",
    },
    bullets: [
      {
        pl: "Sprawy z numeracją per serwer i pełną historią",
        en: "Cases numbered per server with full history",
      },
      {
        pl: "Progi: 3 ostrzeżenia → timeout 1 h, 5 → ban",
        en: "Thresholds: 3 warnings → 1 h timeout, 5 → ban",
      },
      { pl: "Cofanie kar i eksport do CSV", en: "Revoking punishments and CSV export" },
    ],
    steps: [
      {
        pl: "Moderuj komendami /warn, /timeout, /ban.",
        en: "Moderate with /warn, /timeout, /ban.",
      },
      {
        pl: "Ustaw progi w zakładce Automatyczne kary.",
        en: "Set thresholds in the Auto punishments tab.",
      },
      {
        pl: "Przeglądaj i filtruj sprawy w zakładce Sprawy.",
        en: "Browse and filter cases in the Cases tab.",
      },
    ],
    commandCategory: "moderation",
    demoModules: ["WORD_FILTER"],
    demoMessages: [
      { pl: "jesteś idiotą", en: "you are an idiot" },
      { pl: "Dzięki za pomoc!", en: "Thanks for the help!" },
    ],
  },
  {
    slug: "logs",
    icon: "scroll",
    name: { pl: "Logi", en: "Logs" },
    tagline: { pl: "Nic nie znika bez śladu.", en: "Nothing disappears without a trace." },
    description: {
      pl: "Dwanaście typów zdarzeń — od usuniętych wiadomości po zmiany ról — każdy na wybranym kanale. Logi mają czytelny format, z linkami do spraw i autorów.",
      en: "Twelve event types — from deleted messages to role changes — each in a channel of your choice. Logs are easy to read, with links to cases and authors.",
    },
    bullets: [
      {
        pl: "Usunięte i edytowane wiadomości z treścią przed zmianą",
        en: "Deleted and edited messages with the original content",
      },
      {
        pl: "Dołączenia, wyjścia, bany, zmiany nicków i ról",
        en: "Joins, leaves, bans, nickname and role changes",
      },
      { pl: "Osobny kanał dla akcji AutoModu", en: "A separate channel for AutoMod actions" },
    ],
    steps: [
      {
        pl: "Utwórz kanał logów widoczny tylko dla moderacji.",
        en: "Create a log channel visible only to moderators.",
      },
      {
        pl: "W zakładce Logi przypisz kanał do zdarzeń.",
        en: "Assign the channel to events in the Logs tab.",
      },
    ],
    commandCategory: "logs",
    demoModules: ["ANTI_LINK"],
    demoMessages: [
      { pl: "sprawdź to http://free-skins.ru/login", en: "check this http://free-skins.ru/login" },
      {
        pl: "Nowy film: https://youtube.com/watch?v=abc",
        en: "New video: https://youtube.com/watch?v=abc",
      },
    ],
  },
  {
    slug: "verification",
    icon: "check",
    name: { pl: "Weryfikacja", en: "Verification" },
    tagline: { pl: "Ludzie wchodzą, boty zostają za drzwiami.", en: "Humans in, bots out." },
    description: {
      pl: "Nowi członkowie widzą tylko kanał weryfikacji. Po kliknięciu przycisku lub rozwiązaniu captchy dostają rolę i pełny dostęp do serwera.",
      en: "New members see only the verification channel. After clicking a button or solving a captcha they get a role and full access to the server.",
    },
    bullets: [
      { pl: "Przycisk lub captcha obrazkowa", en: "Button or image captcha" },
      { pl: "Własna treść wiadomości weryfikacyjnej", en: "Custom verification message" },
      { pl: "Rola nadawana po weryfikacji", en: "Role granted after verification" },
    ],
    steps: [
      {
        pl: "Utwórz rolę „Zweryfikowany” i ukryj kanały przed @everyone.",
        en: "Create a “Verified” role and hide channels from @everyone.",
      },
      {
        pl: "W zakładce Weryfikacja wybierz kanał, rolę i metodę.",
        en: "In the Verification tab pick the channel, role and method.",
      },
    ],
    commandCategory: "verification",
    demoModules: ["ANTI_INVITE"],
    demoMessages: [
      { pl: "discord.gg/abc123 lepszy serwer", en: "discord.gg/abc123 better server" },
      { pl: "Zweryfikowałem się, cześć!", en: "Verified, hi!" },
    ],
  },
  {
    slug: "reaction-roles",
    icon: "tags",
    name: { pl: "Role reakcyjne", en: "Reaction roles" },
    tagline: {
      pl: "Członkowie sami wybierają, co ich interesuje.",
      en: "Members pick what they care about.",
    },
    description: {
      pl: "Wiadomości z przyciskami, reakcjami lub menu wyboru. Tryby: dowolne role, tylko jedna rola albo tylko dodawanie. Do tego auto-role nadawane przy dołączeniu.",
      en: "Messages with buttons, reactions or select menus. Modes: any roles, only one role or add only. Plus auto roles granted on join.",
    },
    bullets: [
      { pl: "Do 25 ról w jednej wiadomości", en: "Up to 25 roles per message" },
      {
        pl: "Tryb „tylko jedna” dla kolorów i regionów",
        en: "“Only one” mode for colours and regions",
      },
      {
        pl: "Auto-role z opóźnieniem — dobre przeciw botom",
        en: "Delayed auto roles — great against bots",
      },
    ],
    steps: [
      {
        pl: "W zakładce Role utwórz wiadomość i dodaj role.",
        en: "In the Roles tab create a message and add roles.",
      },
      {
        pl: "Zapisz — bot opublikuje wiadomość na wybranym kanale.",
        en: "Save — the bot posts the message in the chosen channel.",
      },
    ],
    commandCategory: "roles",
    demoModules: ["ANTI_MENTION"],
    demoMessages: [
      {
        pl: "@Ola @Kuba @Ania @Tomek @Ela @Piotr @Zosia @Bartek @Iga daj role",
        en: "@Ola @Kuba @Ania @Tomek @Ela @Piotr @Zosia @Bartek @Iga give roles",
      },
      { pl: "Wybrałem rolę Minecraft!", en: "I picked the Minecraft role!" },
    ],
  },
  {
    slug: "welcome",
    icon: "wave",
    name: { pl: "Powitania", en: "Greetings" },
    tagline: { pl: "Pierwsze wrażenie, które zostaje.", en: "A first impression that sticks." },
    description: {
      pl: "Powitania i pożegnania z embedem, obrazkiem i zmiennymi. Edytor w panelu pokazuje podgląd na żywo w stylu Discorda.",
      en: "Welcome and goodbye messages with embeds, images and variables. The dashboard editor shows a live Discord-style preview.",
    },
    bullets: [
      {
        pl: "Zmienne {user}, {server}, {memberCount}",
        en: "Variables {user}, {server}, {memberCount}",
      },
      { pl: "Embed z tytułem, opisem i obrazkiem", en: "Embed with title, description and image" },
      { pl: "Opcjonalnie w wiadomości prywatnej", en: "Optionally as a direct message" },
    ],
    steps: [
      {
        pl: "Otwórz zakładkę Powitania i wybierz kanał.",
        en: "Open the Greetings tab and pick a channel.",
      },
      { pl: "Napisz treść, korzystając ze zmiennych.", en: "Write the text using the variables." },
      { pl: "Sprawdź podgląd i zapisz.", en: "Check the preview and save." },
    ],
    commandCategory: "welcome",
    demoModules: ["ANTI_CAPS"],
    demoMessages: [
      { pl: "WITAM WSZYSTKICH NA SERWERZE!!!", en: "HELLO EVERYONE ON THE SERVER!!!" },
      { pl: "Hej, miło Was poznać", en: "Hey, nice to meet you all" },
    ],
  },
  {
    slug: "tickets",
    icon: "ticket",
    name: { pl: "Tickety", en: "Tickets" },
    tagline: { pl: "Support bez chaosu na kanałach.", en: "Support without channel chaos." },
    description: {
      pl: "Prywatne kanały pomocy otwierane przyciskiem, z kategoriami, przejmowaniem przez moderatora i transkryptem po zamknięciu.",
      en: "Private help channels opened with a button, with categories, moderator claiming and a transcript on close.",
    },
    bullets: [
      { pl: "Panel z kategoriami zgłoszeń", en: "Panel with ticket categories" },
      { pl: "Transkrypt HTML w stylu Discorda", en: "Discord-style HTML transcript" },
      { pl: "Limit otwartych ticketów na osobę", en: "Limit of open tickets per person" },
    ],
    steps: [
      { pl: "Użyj /ticket panel na kanale pomocy.", en: "Use /ticket panel in the help channel." },
      {
        pl: "Ustaw kategorię kanałów i rolę obsługi.",
        en: "Set the channel category and support role.",
      },
    ],
    commandCategory: "tickets",
    demoModules: ["ANTI_SPAM"],
    demoMessages: [
      { pl: "pomocy pomocy pomocy", en: "help help help" },
      { pl: "Mam problem z zamówieniem #1042", en: "I have a problem with order #1042" },
    ],
    premium: false,
  },
];

export function getFeature(slug: string): FeatureModule | undefined {
  return features.find((f) => f.slug === slug);
}
