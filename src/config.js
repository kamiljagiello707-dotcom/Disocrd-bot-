// ╭──────────────────────────────────────────────╮
// │            NightShop ✦ konfiguracja           │
// ╰──────────────────────────────────────────────╯
// Uzupełnij ID poniżej (PPM na kanał/rolę → Kopiuj ID, wymaga trybu dewelopera).

module.exports = {
  shopName: 'NightShop',
  color: 0x8b5cf6, // fioletowy
  colorSuccess: 0x22c55e,
  colorDanger: 0xef4444,
  // Link do obrazka/banera pod panelem (opcjonalnie, zostaw '' aby wyłączyć)
  bannerUrl: '',

  // ID kategorii, w której będą tworzone tickety
  ticketCategoryId: 'ID_KATEGORII',
  // ID kanału z logami (otwarcie/zamknięcie/transkrypty/oceny)
  logChannelId: 'ID_KANALU_LOGOW',
  // Role obsługi, które widzą wszystkie tickety
  staffRoleIds: ['ID_ROLI_SUPPORTU'],

  // Maksymalna liczba otwartych ticketów na osobę
  maxTicketsPerUser: 1,

  // Kategorie ticketów widoczne w menu panelu
  categories: [
    {
      id: 'zakup',
      label: 'Zakup',
      emoji: '🛒',
      description: 'Chcesz coś kupić? Kliknij tutaj!',
      questions: [
        { id: 'produkt', label: 'Co chcesz kupić?', placeholder: 'np. Konto Premium 30 dni', style: 'short' },
        { id: 'platnosc', label: 'Metoda płatności', placeholder: 'BLIK / PSC / PayPal / Krypto', style: 'short' },
        { id: 'ilosc', label: 'Ilość', placeholder: 'np. 1', style: 'short', required: false },
      ],
    },
    {
      id: 'pomoc',
      label: 'Pomoc',
      emoji: '🆘',
      description: 'Masz problem lub pytanie? Pomożemy!',
      questions: [
        { id: 'problem', label: 'Opisz swój problem', placeholder: 'Napisz dokładnie z czym masz problem...', style: 'paragraph' },
      ],
    },
    {
      id: 'reklamacja',
      label: 'Reklamacja',
      emoji: '⚠️',
      description: 'Coś nie działa z zamówieniem?',
      questions: [
        { id: 'zamowienie', label: 'Numer / opis zamówienia', placeholder: 'np. #1234 lub co kupiłeś', style: 'short' },
        { id: 'opis', label: 'Co jest nie tak?', placeholder: 'Opisz problem z produktem...', style: 'paragraph' },
      ],
    },
    {
      id: 'partnerstwo',
      label: 'Partnerstwo',
      emoji: '🤝',
      description: 'Chcesz nawiązać współpracę?',
      questions: [
        { id: 'serwer', label: 'Link do serwera', placeholder: 'https://discord.gg/...', style: 'short' },
        { id: 'osoby', label: 'Ilość osób na serwerze', placeholder: 'np. 500', style: 'short' },
        { id: 'propozycja', label: 'Twoja propozycja', placeholder: 'Opisz propozycję współpracy...', style: 'paragraph', required: false },
      ],
    },
    {
      id: 'nagroda',
      label: 'Odbiór nagrody',
      emoji: '🎁',
      description: 'Wygrałeś konkurs? Odbierz nagrodę!',
      questions: [
        { id: 'konkurs', label: 'Jaki konkurs wygrałeś?', placeholder: 'np. Giveaway 01.11', style: 'short' },
      ],
    },
  ],
};
