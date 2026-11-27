// ╭──────────────────────────────────────────────╮
// │        ZgłośFrajera ︲ konfiguracja            │
// ╰──────────────────────────────────────────────╯
module.exports = {
  // Znak oddzielający emotkę od tekstu
  sep: '︲',
  // Kolor paska kontenera (null = brak paska)
  color: 0x2b2d31,

  // Emotki serwerowe (bot musi być na serwerze, z którego pochodzą)
  emojis: {
    opis: '<:emoji_9:1564588745863340173>',
    przycisk: { id: '1564588950415343736', name: 'emoji_11' },
    stopka: '<:emoji_4:1564588437397573694>',
  },

  // Baner: link https://... albo '' żeby użyć pliku assets/banner.png
  bannerUrl: '',
  bannerFile: 'assets/banner.png',

  // Kanał, na który trafiają zgłoszenia (PPM na kanał → Kopiuj ID)
  reportChannelId: 'ID_KANALU_ZGLOSZEN',
};
