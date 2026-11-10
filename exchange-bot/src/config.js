module.exports = {
  name: 'Exchange',
  tagline: 'Szybkie i bezpieczne wymiany 24/7',
  hours: '10:00 - 23:00',
  status: 'wymiany | /kursy',

  color: 0x3b82f6,
  colors: { ok: 0x22c55e, warn: 0xf59e0b, bad: 0xef4444, info: 0x3b82f6, sent: 0xa855f7 },

  // linki do grafik (zostaw puste jeśli nie chcesz)
  logo: '',
  banner: '',

  ticketCategory: 'ID_KATEGORII',
  logsChannel: 'ID_KANALU_LOGOW',
  vouchChannel: 'ID_KANALU_OPINII',
  staffRoles: ['ID_ROLI_OBSLUGI'],

  maxOpen: 1,
  closeDelay: 5,

  // send/receive: false wyłącza metodę po danej stronie
  methods: {
    blik: { label: 'BLIK', emoji: '📱', min: 10, max: 5000, payout: 'Numer telefonu' },
    paypal: { label: 'PayPal', emoji: '🅿️', min: 10, max: 5000, payout: 'E-mail PayPal' },
    psc: { label: 'PaySafeCard', emoji: '💳', min: 20, max: 1000, receive: false },
    przelew: { label: 'Przelew', emoji: '🏦', min: 20, max: 10000, payout: 'Numer konta i imię' },
    revolut: { label: 'Revolut', emoji: '💠', min: 10, max: 5000, payout: 'Revtag' },
    btc: { label: 'Bitcoin', emoji: '🟠', min: 50, max: 10000, payout: 'Adres BTC' },
    ltc: { label: 'Litecoin', emoji: '🔷', min: 20, max: 10000, payout: 'Adres LTC' },
    usdt: { label: 'USDT TRC20', emoji: '💵', min: 20, max: 10000, payout: 'Adres TRC20' },
  },

  // prowizja w % dla par "wysyłam>otrzymuję", reszta bierze defaultFee
  defaultFee: 10,
  fees: {
    'blik>paypal': 6, 'blik>revolut': 4, 'blik>btc': 6, 'blik>ltc': 5, 'blik>usdt': 5, 'blik>przelew': 3,
    'paypal>blik': 8, 'paypal>przelew': 8, 'paypal>revolut': 8, 'paypal>btc': 9, 'paypal>ltc': 9, 'paypal>usdt': 9,
    'psc>blik': 22, 'psc>paypal': 24, 'psc>przelew': 22, 'psc>revolut': 22, 'psc>btc': 24, 'psc>ltc': 24, 'psc>usdt': 24,
    'przelew>blik': 2, 'przelew>paypal': 6, 'revolut>blik': 3, 'revolut>paypal': 6,
    'btc>blik': 4, 'btc>paypal': 6, 'ltc>blik': 4, 'ltc>paypal': 6, 'usdt>blik': 3, 'usdt>paypal': 5,
  },
  // pary pokazywane w panelu
  popular: ['paypal>blik', 'blik>paypal', 'psc>blik', 'btc>blik', 'usdt>blik'],

  exchangeChannel: 'wymiana',
  exchangeEmoji: '💱',

  categories: [
    {
      id: 'pomoc', label: 'Pomoc', emoji: '❓', channel: 'pomoc',
      description: 'Pytania i ogólna pomoc',
      questions: [
        { id: 'temat', label: 'W czym pomóc?', placeholder: 'np. pytanie o limity', max: 100 },
        { id: 'opis', label: 'Opis', style: 'long', placeholder: 'Opisz dokładnie sprawę', max: 1000 },
      ],
    },
    {
      id: 'problem', label: 'Problem z wymianą', emoji: '⚠️', channel: 'problem',
      description: 'Coś poszło nie tak z wymianą',
      questions: [
        { id: 'numer', label: 'Numer wymiany', placeholder: 'np. 0042', max: 20 },
        { id: 'opis', label: 'Co się stało?', style: 'long', max: 1000 },
      ],
    },
    {
      id: 'wspolpraca', label: 'Współpraca', emoji: '🤝', channel: 'wspolpraca',
      description: 'Partnerstwa i współpraca',
      questions: [
        { id: 'link', label: 'Link do serwera / strony', placeholder: 'https://', max: 150 },
        { id: 'opis', label: 'Propozycja', style: 'long', max: 1000 },
      ],
    },
  ],
};
