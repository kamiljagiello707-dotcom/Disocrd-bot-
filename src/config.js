// ╭──────────────────────────────────────────────╮
// │          FutureShop ˈ konfiguracja            │
// ╰──────────────────────────────────────────────╯
// Uzupełnij ID poniżej (PPM na kanał/rolę → Kopiuj ID, wymaga trybu dewelopera).

module.exports = {
  shopName: 'FutureShop',
  // Nazwa w nagłówkach (stylizowana jak na wzorze)
  brand: 'FUTURƐSHOP™',
  brandEmoji: '⚡',
  // Znak oddzielający emotkę od tekstu
  sep: 'ˈ',
  // Kolor paska po lewej (czerwony)
  color: 0xff1f3d,
  colorSuccess: 0x22c55e,

  // Baner pod wiadomościami. Podaj link (https://...) albo zostaw '' żeby użyć pliku assets/banner.png
  bannerUrl: '',
  bannerFile: 'assets/banner.png',

  // ID kategorii, w której będą tworzone tickety
  ticketCategoryId: 'ID_KATEGORII',
  // ID kanału z logami (otwarcie/zamknięcie/transkrypty/oceny)
  logChannelId: 'ID_KANALU_LOGOW',
  // Role obsługi, które widzą wszystkie tickety (pierwsza jest pokazywana jako „Obsługa”)
  staffRoleIds: ['ID_ROLI_SPRZEDAWCY'],

  // Prefiks nazw kanałów ticketów, np. „||🛒ˈzamówienie•nick”
  channelPrefix: '||',
  maxTicketsPerUser: 1,

  // display: 'block' = odpowiedź w dużym polu kodu, 'inline' = w linii obok etykiety
  categories: [
    {
      id: 'zamowienie',
      label: 'Zamówienie',
      emoji: '🛒',
      channel: 'zamówienie',
      description: 'Chcesz coś kupić? Złóż zamówienie!',
      intro: 'Zamówienie jest obsługiwane przez sprzedawcę i administrację.',
      questions: [
        { id: 'produkt', label: 'Produkt', emoji: '🛒', placeholder: 'np. Konto Premium 30 dni', style: 'short', display: 'block' },
        { id: 'ilosc', label: 'Ilość', emoji: '⚙️', placeholder: 'np. 1', style: 'short', display: 'inline' },
        { id: 'platnosc', label: 'Płatność', emoji: '💵', placeholder: 'BLIK / PSC / PayPal / Krypto', style: 'short', display: 'inline' },
        { id: 'znizka', label: 'Zniżka', emoji: '✨', placeholder: 'Kod rabatowy (opcjonalnie)', style: 'short', display: 'inline', required: false },
      ],
    },
    {
      id: 'pomoc',
      label: 'Pomoc',
      emoji: '🆘',
      channel: 'pomoc',
      description: 'Masz problem lub pytanie? Pomożemy!',
      intro: 'Zgłoszenie jest obsługiwane przez support i administrację.',
      questions: [
        { id: 'temat', label: 'Temat', emoji: '📌', placeholder: 'np. Problem z płatnością', style: 'short', display: 'inline' },
        { id: 'opis', label: 'Opis problemu', emoji: '📝', placeholder: 'Opisz dokładnie z czym masz problem...', style: 'paragraph', display: 'block' },
      ],
    },
    {
      id: 'reklamacja',
      label: 'Reklamacja',
      emoji: '⚠️',
      channel: 'reklamacja',
      description: 'Coś nie działa z zamówieniem?',
      intro: 'Reklamacja jest rozpatrywana przez sprzedawcę i administrację.',
      questions: [
        { id: 'zamowienie', label: 'Zamówienie', emoji: '🧾', placeholder: 'np. #0042 lub co kupiłeś', style: 'short', display: 'inline' },
        { id: 'opis', label: 'Co jest nie tak?', emoji: '❗', placeholder: 'Opisz problem z produktem...', style: 'paragraph', display: 'block' },
      ],
    },
    {
      id: 'partnerstwo',
      label: 'Partnerstwo',
      emoji: '🤝',
      channel: 'partnerstwo',
      description: 'Chcesz nawiązać współpracę?',
      intro: 'Partnerstwa rozpatruje administracja serwera.',
      questions: [
        { id: 'serwer', label: 'Link do serwera', emoji: '🔗', placeholder: 'https://discord.gg/...', style: 'short', display: 'inline' },
        { id: 'osoby', label: 'Ilość osób', emoji: '👥', placeholder: 'np. 500', style: 'short', display: 'inline' },
        { id: 'propozycja', label: 'Propozycja', emoji: '💡', placeholder: 'Opisz propozycję współpracy...', style: 'paragraph', display: 'block', required: false },
      ],
    },
    {
      id: 'nagroda',
      label: 'Odbiór nagrody',
      emoji: '🎁',
      channel: 'nagroda',
      description: 'Wygrałeś konkurs? Odbierz nagrodę!',
      intro: 'Nagrody wydaje administracja po sprawdzeniu wyniku.',
      questions: [
        { id: 'konkurs', label: 'Konkurs', emoji: '🏆', placeholder: 'np. Giveaway 01.11', style: 'short', display: 'inline' },
        { id: 'nagroda', label: 'Nagroda', emoji: '🎁', placeholder: 'Co wygrałeś?', style: 'short', display: 'block' },
      ],
    },
  ],
};
