console.log('▶ Start bota ValuteShop...');
try {
  require('dotenv').config({ path: require('path').join(__dirname, '.env') });
} catch {
  /* brak dotenv – token można wpisać niżej w config */
}
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const {
  Client,
  GatewayIntentBits,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  StringSelectMenuBuilder,
  UserSelectMenuBuilder,
  ModalBuilder,
  LabelBuilder,
  TextInputBuilder,
  TextInputStyle,
  SlashCommandBuilder,
  PermissionFlagsBits,
  OverwriteType,
  ChannelType,
  REST,
  Routes,
  AttachmentBuilder,
  ContainerBuilder,
  TextDisplayBuilder,
  SeparatorBuilder,
  SeparatorSpacingSize,
  SectionBuilder,
  ThumbnailBuilder,
  MediaGalleryBuilder,
  MediaGalleryItemBuilder,
  FileBuilder,
  MessageFlags,
  Options,
} = require('discord.js');

// ╭──────────────────────────────────────────────╮
// │          ValuteShop ︲ konfiguracja            │
// ╰──────────────────────────────────────────────╯

// Emotki serwerowe (bot musi być na serwerze, z którego pochodzą)
const E = {
  e2: '<:emoji_2:1575993715942498486>',
  e4: '<:emoji_4:1575993758376267896>',
  e8: '<:emoji_8:1575993871891038348>',
  e10: '<:emoji_10:1575994490521526413>',
  e12: '<:emoji_12:1575994530140922026>',
  e13: '<:emoji_13:1575994546918137947>',
  e16: '<:emoji_16:1575994582234169364>',
  e23: '<:emoji_23:1575994740070031454>',
  e24: '<:emoji_24:1575994767257636974>',
  e25: '<:emoji_25:1575994805652295703>',
};

const config = {
  shopName: 'ValuteShop',
  sep: '︲',
  // Kolor paska kontenera (null = brak paska)
  color: 0x2b2d31,
  // Kolor akcentu w transkrypcie HTML
  transcriptColor: '#22c55e',

  // Baner pod panelami: link https://... albo '' żeby użyć pliku banner.jpg (obok index.js)
  bannerUrl: '',
  bannerFile: 'banner.jpg',

  // Role sprzedawców (widzą tickety, mogą je przejmować/zamykać, dodawać zakupy).
  // Administratorzy mogą zawsze. Np. ['123456789012345678']
  sellerRoleIds: [],

  // Kategoria na tickety – puste '' = bot sam utworzy „🎫︲Tickety”
  ticketCategoryId: '',
  ticketCategoryName: '🎫︲Tickety',
  // Kanał z logami/transkryptami – puste '' = bot sam utworzy prywatny kanał w kategorii ticketów
  logChannelId: '',
  logChannelName: '📁︲logi-ticketów',
  // Ile otwartych ticketów naraz może mieć jedna osoba
  maxOpenTickets: 1,

  // Kategorie ticketów i pytania w formularzu (max 5 pytań, type: 'text' | 'paragraph' | 'select')
  categories: {
    zakup: {
      label: 'Zakup',
      emoji: E.e25,
      prefix: '🛒',
      title: 'Zakup waluty',
      questions: [
        {
          id: 'serwer',
          type: 'select',
          label: 'Na jakim serwerze chcesz kupić walutę?',
          name: 'Serwer',
          placeholder: 'Wybierz serwer',
          options: [
            { label: 'anarchia.gg', emoji: E.e24 },
            { label: 'minestar.gg', emoji: E.e24 },
            { label: 'Rapy.gg', emoji: E.e24 },
          ],
        },
        { id: 'ilosc', label: 'Ile waluty chcesz kupić?', name: 'Ilość waluty', placeholder: 'np. 10kk' },
        { id: 'nick', label: 'Twój nick w Minecraft', name: 'Nick w Minecraft', placeholder: 'np. Steve', max: 32 },
        {
          id: 'platnosc',
          type: 'select',
          label: 'Metoda płatności',
          name: 'Metoda płatności',
          placeholder: 'Wybierz metodę płatności',
          options: [
            { label: 'Blik', emoji: E.e10 },
            { label: 'PSC', emoji: E.e13 },
            { label: 'LTC', emoji: E.e12 },
            { label: 'PayPal', emoji: E.e16 },
          ],
        },
        { id: 'kod', label: 'Kod rabatowy (opcjonalnie)', name: 'Kod rabatowy', placeholder: 'Jeśli masz kod, wpisz go tutaj', required: false },
      ],
    },
    reklamacja: {
      label: 'Reklamacja / Zwrot',
      emoji: E.e4,
      prefix: '⚠️',
      title: 'Reklamacja / Zwrot',
      questions: [
        { id: 'nick', label: 'Twój nick w Minecraft', name: 'Nick w Minecraft', placeholder: 'np. Steve', max: 32 },
        { id: 'zamowienie', label: 'Czego dotyczy reklamacja?', name: 'Czego dotyczy', placeholder: 'np. 10kk na anarchia.gg z 27.11' },
        { id: 'opis', type: 'paragraph', label: 'Opisz problem', name: 'Opis problemu', placeholder: 'Co poszło nie tak?', max: 1000 },
      ],
    },
    pomoc: {
      label: 'Pomoc Ogólna',
      emoji: E.e24,
      prefix: '❓',
      title: 'Pomoc Ogólna',
      questions: [
        { id: 'nick', label: 'Twój nick w Minecraft', name: 'Nick w Minecraft', placeholder: 'np. Steve', max: 32 },
        { id: 'opis', type: 'paragraph', label: 'W czym możemy pomóc?', name: 'Opis', placeholder: 'Opisz swoją sprawę...', max: 1000 },
      ],
    },
  },

  // Token bota. Najlepiej w pliku .env (TOKEN=...), albo wklej go tutaj między ''.
  token: '',
};

const S = config.sep;
const SHOP = config.shopName;
const V2 = MessageFlags.IsComponentsV2;
const EPH = MessageFlags.Ephemeral | MessageFlags.IsComponentsV2;

// Minimalny cache = minimalne zużycie RAM
const client = new Client({
  intents: [GatewayIntentBits.Guilds],
  makeCache: Options.cacheWithLimits({
    ...Options.DefaultMakeCacheSettings,
    MessageManager: 0,
    GuildMemberManager: { maxSize: 0, keepOverLimit: (m) => m.id === m.client.user.id },
    UserManager: { maxSize: 0, keepOverLimit: (u) => u.id === u.client.user.id },
    PresenceManager: 0,
    ReactionManager: 0,
    GuildEmojiManager: 0,
    GuildStickerManager: 0,
    VoiceStateManager: 0,
    ThreadManager: 0,
    GuildScheduledEventManager: 0,
    StageInstanceManager: 0,
    AutoModerationRuleManager: 0,
    GuildInviteManager: 0,
    GuildBanManager: 0,
  }),
});

// ───────────── pomocnicze ─────────────
const text = (c) => new TextDisplayBuilder().setContent(c);
const sep = () => new SeparatorBuilder().setDivider(true).setSpacing(SeparatorSpacingSize.Small);
const head = (t) => text('```\n' + t + '\n```');
const foot = (t) => text(`-# © ${new Date().getFullYear()} ${SHOP}™ × ${t}`);
const clean = (s) => String(s ?? '').replace(/`/g, 'ˋ').trim();
const pad = (n) => String(n).padStart(4, '0');
const now = () => Math.floor(Date.now() / 1000);
const zl = (n) => new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(n);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const ticketId = () => Array.from(crypto.randomBytes(8), (b) => 'ABCDEFGHJKLMNPQRSTUVWXYZ0123456789'[b % 34]).join('');
// '<:nazwa:id>' → { id, name } do przycisków / menu
const ce = (s) => {
  const m = /^<(a?):(\w+):(\d+)>$/.exec(s || '');
  return m ? { id: m[3], name: m[2], animated: !!m[1] } : { name: s };
};

const BANNER_PATH = path.join(__dirname, config.bannerFile || '');
const BANNER_NAME = path.basename(config.bannerFile || 'banner.jpg');
const hasBannerFile = () => !config.bannerUrl && config.bannerFile && fs.existsSync(BANNER_PATH);
const bannerFiles = () => (hasBannerFile() ? [new AttachmentBuilder(BANNER_PATH, { name: BANNER_NAME })] : []);
const bannerSrc = () => config.bannerUrl || (hasBannerFile() ? `attachment://${BANNER_NAME}` : null);
function addBanner(c) {
  const src = bannerSrc();
  if (!src) return;
  c.addSeparatorComponents(sep());
  c.addMediaGalleryComponents(new MediaGalleryBuilder().addItems(new MediaGalleryItemBuilder().setURL(src)));
}

function newContainer() {
  const c = new ContainerBuilder();
  if (config.color != null) c.setAccentColor(config.color);
  return c;
}
function notice(msg, ok = false) {
  return { components: [newContainer().addTextDisplayComponents(text(`${ok ? '✅' : '❌'} ${S} ${msg}`))], flags: EPH };
}
// do interaction.update() – bez flagi Ephemeral
const asUpdate = (p) => ({ components: p.components, flags: V2, allowedMentions: { parse: [] } });
const isSeller = (member) =>
  member?.permissions?.has(PermissionFlagsBits.Administrator) || config.sellerRoleIds.some((r) => member?.roles?.cache?.has(r));

// ───────────── zapis danych (pliki .json obok index.js) ─────────────
function jsonDb(file, empty) {
  const full = path.join(__dirname, file);
  let data;
  try {
    data = { ...empty, ...JSON.parse(fs.readFileSync(full, 'utf8')) };
  } catch {
    data = empty;
  }
  data.save = () => {
    try {
      const { save, ...rest } = data;
      fs.writeFileSync(full, JSON.stringify(rest, null, 2));
    } catch (e) {
      console.error(`❌ Nie udało się zapisać ${file}:`, e.message);
    }
  };
  return data;
}
const db = jsonDb('zakupy.json', { counter: 0, purchases: [] });
const tdb = jsonDb('tickety.json', { tickets: {}, sellers: {} });

const purchasesOf = (userId, guildId) => db.purchases.filter((p) => p.clientId === userId && p.guildId === guildId);
function addPurchase({ guildId, clientId, sellerId, amount, note, ticketId: tid }) {
  db.counter += 1;
  const p = { number: db.counter, guildId, clientId, sellerId, amount, note: note || '', ticketId: tid, createdAt: now() };
  db.purchases.push(p);
  db.save();
  return p;
}
const sellerEarnings = (sellerId, guildId) =>
  db.purchases.filter((p) => p.sellerId === sellerId && p.guildId === guildId).reduce((s, p) => s + p.amount, 0);
const sellerCompleted = (sellerId) => tdb.sellers[sellerId]?.completed || 0;
const getTicket = (channelId) => {
  const t = tdb.tickets[channelId];
  return t && !t.closed ? t : null;
};

// ╭──────────────────────────────────────────────╮
// │                PANEL KLIENTA                  │
// ╰──────────────────────────────────────────────╯
function buildPanel() {
  const c = newContainer();
  c.addTextDisplayComponents(head(`📁${S}Panel Klienta × ${SHOP}`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`> ${E.e2}${S}Hejka, jeśli chcesz skorzystać z panelu Klienta, wybierz __opcję__ poniżej!`));
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(
      new StringSelectMenuBuilder()
        .setCustomId('vs_panel')
        .setPlaceholder(`🌐${S}Wybierz Sekcję`)
        .addOptions(
          { label: `${S}Sprawdź ile wydałeś`, value: 'wydane', emoji: ce(E.e24) },
          { label: `${S}Sprawdź swoją Historię`, value: 'historia', emoji: ce(E.e25) },
        ),
    ),
  );
  return { components: [c], flags: V2 };
}

function buildSpent(userId, guildId) {
  const mine = purchasesOf(userId, guildId);
  const total = mine.reduce((sum, p) => sum + p.amount, 0);
  const c = newContainer();
  c.addTextDisplayComponents(head(`📕${S}Ile wydałeś × ${SHOP}`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`${E.e4}${S}**__Twoje Statystyki__**`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(
      mine.length
        ? `> ${E.e2}${S}W naszym sklepie wydałeś okrągłe \`${zl(total)}\`\n> ${E.e23}${S}Liczba zakupów: \`${mine.length}\``
        : `> ${E.e2}${S}Nie masz jeszcze żadnych zakupów w naszym sklepie.`,
    ),
  );
  return { components: [c], flags: EPH };
}

const HISTORY_LIMIT = 10;
function buildHistory(userId, guildId) {
  const mine = purchasesOf(userId, guildId);
  const c = newContainer();
  c.addTextDisplayComponents(head(`💸${S}Twoja Historia × ${SHOP}`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`# ${E.e24}${S}__Historia zakupów__`));
  c.addSeparatorComponents(sep());
  if (!mine.length) {
    c.addTextDisplayComponents(text(`> ${E.e23}${S}Brak zakupów.`));
  } else {
    const lines = mine
      .slice(-HISTORY_LIMIT)
      .reverse()
      .map(
        (p) =>
          `> ${E.e23}${S}Zakup za \`${zl(p.amount)}\` • <t:${p.createdAt}:f> • Sprzedał: <@${p.sellerId}>` +
          `\n> -# #${pad(p.number)}${p.note ? ` • ${clean(p.note).slice(0, 80)}` : ''}`,
      );
    if (mine.length > HISTORY_LIMIT) lines.push(`-# …i ${mine.length - HISTORY_LIMIT} starszych zakupów`);
    c.addTextDisplayComponents(text(lines.join('\n')));
  }
  return { components: [c], flags: EPH, allowedMentions: { parse: [] } };
}

// ╭──────────────────────────────────────────────╮
// │                   TICKETY                     │
// ╰──────────────────────────────────────────────╯

// Panel z wyborem kategorii
function buildTicketPanel() {
  const c = newContainer();
  c.addTextDisplayComponents(head(`🌐${S}${SHOP} × Tickety`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(
      `> ${E.e25}${S}Hej! Chcesz kupić __Walutę__ na anarchia.gg lub coś innego? A może masz problem? Stwórz Ticketa **poniżej**!`,
    ),
  );
  c.addSeparatorComponents(sep());
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(
      new StringSelectMenuBuilder()
        .setCustomId('vs_ticket')
        .setPlaceholder(`🔍${S}Wybierz odpowiednią kategorię`)
        .addOptions(
          Object.entries(config.categories).map(([id, cat]) => ({ label: `${S}${cat.label}`, value: id, emoji: ce(cat.emoji) })),
        ),
    ),
  );
  addBanner(c);
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`-# 🔍${S}Panel Tickets`));
  return { components: [c], files: bannerFiles(), flags: V2 };
}

// Formularz kategorii
function buildTicketModal(catId) {
  const cat = config.categories[catId];
  const modal = new ModalBuilder().setCustomId(`vsm_${catId}`).setTitle(cat.title.slice(0, 45));
  for (const q of cat.questions.slice(0, 5)) {
    const label = new LabelBuilder().setLabel(q.label.slice(0, 45));
    if (q.type === 'select') {
      label.setStringSelectMenuComponent(
        new StringSelectMenuBuilder()
          .setCustomId(q.id)
          .setPlaceholder(q.placeholder || 'Wybierz')
          .setRequired(q.required !== false)
          .addOptions(q.options.map((o) => ({ label: `${S}${o.label}`, value: o.label, ...(o.emoji ? { emoji: ce(o.emoji) } : {}) }))),
      );
    } else {
      label.setTextInputComponent(
        new TextInputBuilder()
          .setCustomId(q.id)
          .setStyle(q.type === 'paragraph' ? TextInputStyle.Paragraph : TextInputStyle.Short)
          .setPlaceholder((q.placeholder || '').slice(0, 100))
          .setRequired(q.required !== false)
          .setMaxLength(q.max || 100),
      );
    }
    modal.addLabelComponents(label);
  }
  return modal;
}

function readAnswers(interaction, catId) {
  const out = {};
  for (const q of config.categories[catId].questions) {
    try {
      out[q.id] =
        q.type === 'select'
          ? interaction.fields.getStringSelectValues(q.id)[0] || ''
          : interaction.fields.getTextInputValue(q.id) || '';
    } catch {
      out[q.id] = '';
    }
  }
  return out;
}

function buildTicketCreated(channelId) {
  const c = newContainer();
  c.addTextDisplayComponents(head(`✅${S}Ticket Utworzony × Powiadomienie`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`${E.e2}${S}Pomyślnie utworzyłeś Ticketa! Ticket znajduje się na kanale <#${channelId}>`));
  return { components: [c], flags: EPH };
}

// Podsumowanie na kanale ticketu
function buildSummary(t) {
  const cat = config.categories[t.category];
  const c = newContainer();
  c.addTextDisplayComponents(head(`⚙️${S}${SHOP} × Podsumowanie`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(
      `# ${E.e4}${S}Informacje o Użytkowniku\n` +
        `> • ${E.e2}${S}**Ping:** <@${t.ownerId}>\n` +
        `> • ${E.e2}${S}**Tag:** \`${clean(t.ownerTag)}\`\n` +
        `> • ${E.e2}${S}**ID użytkownika:** \`${t.ownerId}\``,
    ),
  );
  c.addSeparatorComponents(sep());
  const lines = [
    `# ${E.e23}${S}Informacje o zleceniu`,
    `> • ${E.e2}${S}**ID zlecenia:** \`${t.id}\``,
    `> • ${E.e2}${S}**Kategoria Ticketa:** \`${cat.label}\``,
  ];
  for (const q of cat.questions) {
    const val = clean(t.answers[q.id]) || 'Brak';
    lines.push(
      q.type === 'paragraph'
        ? `> • ${E.e2}${S}**${q.name}:**\n> \`\`\`${val.slice(0, 900).replace(/\n/g, ' ')}\`\`\``
        : `> • ${E.e2}${S}**${q.name}:** \`${val.slice(0, 100)}\``,
    );
  }
  c.addTextDisplayComponents(text(lines.join('\n')));
  c.addSeparatorComponents(sep());
  const btn = (id, emoji, label) =>
    new ActionRowBuilder().addComponents(
      new ButtonBuilder().setCustomId(id).setStyle(ButtonStyle.Secondary).setEmoji(ce(emoji)).setLabel(`${S}${label}`),
    );
  c.addActionRowComponents(
    btn('vs_close', E.e8, 'Zamknij Ticketa'),
    btn('vs_claim', E.e4, 'Przejmij Ticketa'),
    btn('vs_settings', E.e24, 'Ustawienia Ticketa'),
  );
  addBanner(c);
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(foot('Podsumowanie Ticketa'));
  return { components: [c], files: bannerFiles(), flags: V2 };
}

// Powiadomienie po przejęciu
function buildClaimNotice(t, sellerId) {
  const c = newContainer();
  c.addTextDisplayComponents(head(`⚙️${S}${SHOP} × Powiadomienie`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(
      `> • ${E.e2}${S}**Ticketa przejął:** <@${sellerId}>\n` +
        `> • ${E.e2}${S}**Zarobek sprzedawcy:** \`${zl(sellerEarnings(sellerId, t.guildId))}\`\n` +
        `> • ${E.e2}${S}**Zrealizował ticketów:** \`${sellerCompleted(sellerId)}\``,
    ),
  );
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`${E.e2}${S}Od teraz ten sprzedawca będzie zajmował się Twoim Ticketem`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(foot('Przejęto Ticketa'));
  return { components: [c], flags: V2, allowedMentions: { parse: [] } };
}

// Ustawienia ticketu (dla sprzedawców)
function buildSettings() {
  const c = newContainer();
  c.addTextDisplayComponents(head(`⚙️${S}${SHOP} × Ustawienia Ticketa`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`${E.e2}${S}Chcesz kogoś dodać lub wyrzucić z Ticketa? Zrób to poniżej`));
  c.addSeparatorComponents(sep());
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(
      new StringSelectMenuBuilder()
        .setCustomId('vs_set')
        .setPlaceholder(`🔍${S}Wybierz Akcję Ticketa`)
        .addOptions(
          { label: `${S}Dodaj do Ticketa`, value: 'add', emoji: ce(E.e2) },
          { label: `${S}Usuń z Ticketa`, value: 'remove', emoji: ce(E.e2) },
          { label: `${S}Wezwij na Ticket`, value: 'summon', emoji: ce(E.e2) },
          { label: `${S}Oznacz jako zrealizowane`, value: 'done', emoji: ce(E.e2) },
        ),
    ),
  );
  return { components: [c], flags: EPH };
}

function buildUserPick(action) {
  const c = newContainer();
  c.addTextDisplayComponents(head(`⚙️${S}${SHOP} × Ustawienia Ticketa`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(`${E.e2}${S}Wybierz osoby, które chcesz **${action === 'add' ? 'dodać do' : 'usunąć z'}** Ticketa`),
  );
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(
      new UserSelectMenuBuilder()
        .setCustomId(`vs_${action}`)
        .setPlaceholder(action === 'add' ? '➕ Wybierz kogo dodać' : '➖ Wybierz kogo usunąć')
        .setMinValues(1)
        .setMaxValues(5),
    ),
  );
  return { components: [c], flags: EPH };
}

// Krótkie powiadomienie publiczne na kanale ticketu
function channelNotice(title, lines, footer) {
  const c = newContainer();
  c.addTextDisplayComponents(head(`${title}`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(lines.join('\n')));
  if (footer) {
    c.addSeparatorComponents(sep());
    c.addTextDisplayComponents(foot(footer));
  }
  return { components: [c], flags: V2 };
}

// PV do klienta po wezwaniu
function buildSummonDm(t, sellerId) {
  const c = newContainer();
  c.addTextDisplayComponents(head(`🛒${S}${SHOP} × Wezwanie na Ticket`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(
      `${E.e2}${S}Hej, zostałeś wezwany na Ticketa! Wezwał cię <@${sellerId}>\n` +
        `${E.e2}${S}Aby przejść do kanału, skorzystaj z przycisku poniżej`,
    ),
  );
  c.addSeparatorComponents(sep());
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(
      new ButtonBuilder()
        .setStyle(ButtonStyle.Link)
        .setURL(`https://discord.com/channels/${t.guildId}/${t.channelId}`)
        .setEmoji(ce(E.e2))
        .setLabel(`${S}Przejdź do Ticketa`),
    ),
  );
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(foot('Wezwanie na Ticket'));
  return { components: [c], flags: V2 };
}

function buildDoneModal(t) {
  const note = t.category === 'zakup' ? `${t.answers.ilosc || ''} • ${t.answers.serwer || ''}`.slice(0, 100) : '';
  const opis = new TextInputBuilder()
    .setCustomId('opis')
    .setStyle(TextInputStyle.Short)
    .setPlaceholder('np. 10kk waluty • anarchia.gg')
    .setRequired(false)
    .setMaxLength(100);
  if (note) opis.setValue(note);
  return new ModalBuilder()
    .setCustomId('vsm_done')
    .setTitle('Oznacz jako zrealizowane')
    .addLabelComponents(
      new LabelBuilder()
        .setLabel('Kwota zakupu (zł)')
        .setTextInputComponent(
          new TextInputBuilder().setCustomId('kwota').setStyle(TextInputStyle.Short).setPlaceholder('np. 25.50').setMaxLength(12),
        ),
      new LabelBuilder().setLabel('Opis zakupu').setTextInputComponent(opis),
    );
}

// PV do klienta po zamknięciu (z transkryptem)
function buildCloseDm(t, guild, fileName, buffer) {
  const cat = config.categories[t.category];
  const c = newContainer();
  const thumb = guild?.iconURL({ size: 256 }) || client.user.displayAvatarURL({ size: 256 });
  c.addSectionComponents(
    new SectionBuilder()
      .addTextDisplayComponents(head(`🔒${S}${SHOP}™ × Ticket Zamknięty`))
      .setThumbnailAccessory(new ThumbnailBuilder().setURL(thumb)),
  );
  c.addTextDisplayComponents(
    text(
      `${E.e25}${S}**Hejka <@${t.ownerId}>!** Twój ticket został __**zamknięty**__. ` +
        `Dziękujemy za **zaufanie** i skorzystanie z usług **${SHOP}™**!`,
    ),
  );
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(
      `### ${E.e4}${S}Informacje o Tickecie:\n` +
        `> - ${E.e2}${S}**ID Ticketa:** \`${t.id}\`\n` +
        `> - ${E.e2}${S}**Kategoria Ticketa:** \`${cat?.label || t.category}\`\n` +
        `> - ${E.e2}${S}**Zamknięto Ticketa:** <t:${t.closedAt}:F>\n` +
        `> - ${E.e2}${S}**Zamknięto przez:** <@${t.closedBy}>\n` +
        `> - ${E.e2}${S}**Powód:** \`${clean(t.closeReason)}\``,
    ),
  );
  c.addSeparatorComponents(sep());
  c.addFileComponents(new FileBuilder().setURL(`attachment://${fileName}`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(foot('Zamknięto Twojego Ticketa'));
  return {
    components: [c],
    files: [new AttachmentBuilder(buffer, { name: fileName })],
    flags: V2,
    allowedMentions: { parse: [] },
  };
}

// Log dla obsługi (z transkryptem)
function buildCloseLog(t, fileName, buffer, messageCount) {
  const cat = config.categories[t.category];
  const c = newContainer();
  c.addTextDisplayComponents(head(`📁${S}${SHOP} × Log Ticketa`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(
      `> - ${E.e2}${S}**ID Ticketa:** \`${t.id}\`\n` +
        `> - ${E.e2}${S}**Kategoria:** \`${cat?.label || t.category}\`\n` +
        `> - ${E.e2}${S}**Klient:** <@${t.ownerId}> \`${clean(t.ownerTag)}\`\n` +
        `> - ${E.e2}${S}**Przejął:** ${t.claimedBy ? `<@${t.claimedBy}>` : '`Nikt`'}\n` +
        `> - ${E.e2}${S}**Zrealizowane:** ${t.completed ? `\`Tak\`${t.amount ? ` • \`${zl(t.amount)}\`` : ''}` : '`Nie`'}\n` +
        `> - ${E.e2}${S}**Otwarto:** <t:${t.createdAt}:f>\n` +
        `> - ${E.e2}${S}**Zamknięto:** <t:${t.closedAt}:f> przez <@${t.closedBy}>\n` +
        `> - ${E.e2}${S}**Powód:** \`${clean(t.closeReason)}\`\n` +
        `> - ${E.e2}${S}**Wiadomości:** \`${messageCount}\``,
    ),
  );
  c.addSeparatorComponents(sep());
  c.addFileComponents(new FileBuilder().setURL(`attachment://${fileName}`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(foot('Log Ticketa'));
  return {
    components: [c],
    files: [new AttachmentBuilder(buffer, { name: fileName })],
    flags: V2,
    allowedMentions: { parse: [] },
  };
}

// ───────────── kanały: kategoria i logi ─────────────
const staffOverwrites = () =>
  config.sellerRoleIds.map((id) => ({
    id,
    type: OverwriteType.Role,
    allow: [
      PermissionFlagsBits.ViewChannel,
      PermissionFlagsBits.SendMessages,
      PermissionFlagsBits.ReadMessageHistory,
      PermissionFlagsBits.AttachFiles,
      PermissionFlagsBits.EmbedLinks,
    ],
  }));
const botOverwrite = (guild) => ({
  id: guild.members.me.id,
  type: OverwriteType.Member,
  allow: [
    PermissionFlagsBits.ViewChannel,
    PermissionFlagsBits.SendMessages,
    PermissionFlagsBits.ReadMessageHistory,
    PermissionFlagsBits.AttachFiles,
    PermissionFlagsBits.EmbedLinks,
    PermissionFlagsBits.ManageChannels,
  ],
});

async function findOrCreate(guild, id, name, type, extra = {}) {
  if (/^\d{17,20}$/.test(id || '')) {
    const ch = await guild.channels.fetch(id).catch(() => null);
    if (ch) return ch;
  }
  const all = await guild.channels.fetch();
  const found = all.find((c) => c && c.type === type && c.name === name);
  if (found) return found;
  const created = await guild.channels.create({
    name,
    type,
    permissionOverwrites: [
      { id: guild.roles.everyone.id, type: OverwriteType.Role, deny: [PermissionFlagsBits.ViewChannel] },
      botOverwrite(guild),
      ...staffOverwrites(),
    ],
    ...extra,
  });
  console.log(`✅ Utworzono: ${created.name}`);
  return created;
}
const getTicketCategory = (guild) => findOrCreate(guild, config.ticketCategoryId, config.ticketCategoryName, ChannelType.GuildCategory);
async function getLogChannel(guild) {
  const cat = await getTicketCategory(guild).catch(() => null);
  return findOrCreate(guild, config.logChannelId, config.logChannelName, ChannelType.GuildText, cat ? { parent: cat.id } : {});
}

async function openTicketsOf(guild, userId) {
  const open = Object.values(tdb.tickets).filter((t) => !t.closed && t.ownerId === userId && t.guildId === guild.id);
  const alive = [];
  for (const t of open) {
    const ch = await guild.channels.fetch(t.channelId).catch(() => null);
    if (ch) alive.push(t);
    else t.closed = true;
  }
  if (alive.length !== open.length) tdb.save();
  return alive;
}

// ───────────── tworzenie ticketu ─────────────
async function createTicket(interaction, catId) {
  const guild = interaction.guild;
  const cat = config.categories[catId];
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });

  const open = await openTicketsOf(guild, interaction.user.id);
  if (open.length >= config.maxOpenTickets) {
    return interaction.editReply(notice(`Masz już otwarty ticket: <#${open[0].channelId}>`));
  }

  const answers = readAnswers(interaction, catId);
  const nick = (answers.nick || interaction.user.username).replace(/[^\p{L}\p{N}_-]/gu, '').slice(0, 32) || interaction.user.username;
  const parent = await getTicketCategory(guild);

  const channel = await guild.channels.create({
    name: `${cat.prefix}${S}${nick}`.slice(0, 95),
    type: ChannelType.GuildText,
    parent: parent.id,
    topic: `Ticket ${cat.label} • ${interaction.user.username} (${interaction.user.id})`,
    permissionOverwrites: [
      { id: guild.roles.everyone.id, type: OverwriteType.Role, deny: [PermissionFlagsBits.ViewChannel] },
      botOverwrite(guild),
      {
        id: interaction.user.id,
        type: OverwriteType.Member,
        allow: [
          PermissionFlagsBits.ViewChannel,
          PermissionFlagsBits.SendMessages,
          PermissionFlagsBits.ReadMessageHistory,
          PermissionFlagsBits.AttachFiles,
          PermissionFlagsBits.EmbedLinks,
        ],
      },
      ...staffOverwrites(),
    ],
  });

  const t = {
    id: ticketId(),
    channelId: channel.id,
    guildId: guild.id,
    ownerId: interaction.user.id,
    ownerTag: interaction.user.username,
    category: catId,
    answers,
    createdAt: now(),
    claimedBy: null,
    completed: false,
  };
  tdb.tickets[channel.id] = t;
  tdb.save();

  await channel.send({ ...buildSummary(t), allowedMentions: { users: [t.ownerId] } });
  // powiadomienie dla sprzedawców (ping znika od razu)
  if (config.sellerRoleIds.length) {
    channel
      .send({ content: config.sellerRoleIds.map((r) => `<@&${r}>`).join(' '), allowedMentions: { roles: config.sellerRoleIds } })
      .then((m) => setTimeout(() => m.delete().catch(() => {}), 1500))
      .catch(() => {});
  }
  console.log(`🎫 Nowy ticket ${t.id} (${cat.label}) od ${interaction.user.username}`);
  return interaction.editReply(buildTicketCreated(channel.id));
}

// ───────────── transkrypt HTML ─────────────
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const fmtDate = (d) =>
  d.toLocaleString('pl-PL', { timeZone: 'Europe/Warsaw', day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });

function flatten(comps, out = []) {
  for (const c of comps || []) {
    if (c.type === 10 && c.content) out.push({ kind: 'text', content: c.content });
    else if (c.type === 14) out.push({ kind: 'sep' });
    else if (c.type === 12) for (const it of c.items || []) out.push({ kind: 'img', url: it.media?.url });
    else if (c.type === 13) out.push({ kind: 'file', url: c.file?.url });
    else if (c.type === 3 || c.type === 5) out.push({ kind: 'select', label: c.placeholder || 'Menu' });
    else if (c.type === 2) out.push({ kind: 'button', label: c.label || '', emoji: c.emoji });
    if (c.type === 17) out.push({ kind: 'container', items: flatten(c.components) });
    else if (c.components) flatten(c.components, out);
  }
  return out;
}

function makeMd(names) {
  const emojiImg = (id, name, a) =>
    `<img class="emoji" src="https://cdn.discordapp.com/emojis/${id}.${a ? 'gif' : 'png'}" alt=":${esc(name)}:">`;
  const inline = (s) => {
    let h = esc(s);
    const codes = [];
    const emojis = [];
    h = h.replace(/`([^`\n]+)`/g, (_, c) => {
      codes.push(c);
      return `\u0000${codes.length - 1}\u0000`;
    });
    // emotki chowamy przed resztą formatowania (żeby link w <img> nie był zamieniany na <a>)
    h = h.replace(/&lt;(a?):(\w+):(\d+)&gt;/g, (_, a, n, id) => {
      emojis.push(emojiImg(id, n, a));
      return `\u0001${emojis.length - 1}\u0001`;
    });
    h = h
      .replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')
      .replace(/__(.+?)__/g, '<u>$1</u>')
      .replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<i>$2</i>')
      .replace(/~~(.+?)~~/g, '<s>$1</s>')
      .replace(/&lt;@!?(\d+)&gt;/g, (_, id) => `<span class="mention">@${esc(names.get(id) || id)}</span>`)
      .replace(/&lt;@&amp;(\d+)&gt;/g, () => `<span class="mention">@rola</span>`)
      .replace(/&lt;#(\d+)&gt;/g, (_, id) => `<span class="mention">#${esc(names.get('c' + id) || 'kanał')}</span>`)
      .replace(/&lt;t:(\d+)(?::[a-zA-Z])?&gt;/g, (_, ts) => `<span class="ts">${fmtDate(new Date(ts * 1000))}</span>`)
      .replace(/(https?:\/\/[^\s<]+)/g, '<a href="$1">$1</a>');
    return h
      .replace(/\u0000(\d+)\u0000/g, (_, i) => `<code>${codes[i]}</code>`)
      .replace(/\u0001(\d+)\u0001/g, (_, i) => emojis[i]);
  };
  const line = (l) => {
    if (/^-#\s/.test(l)) return `<div class="sub">${inline(l.slice(3))}</div>`;
    if (/^#{1,3}\s/.test(l)) return `<div class="h">${inline(l.replace(/^#+\s/, ''))}</div>`;
    if (/^[-*]\s/.test(l)) return `<div>• ${inline(l.slice(2))}</div>`;
    return `<div>${inline(l)}</div>`;
  };
  return (src) => {
    const out = [];
    String(src)
      .split(/```(?:\w*\n)?([\s\S]*?)```/g)
      .forEach((p, idx) => {
        if (idx % 2) return out.push(`<pre>${esc(p.replace(/\n$/, ''))}</pre>`);
        let quote = [];
        const flushQ = () => {
          if (quote.length) out.push(`<blockquote>${quote.join('')}</blockquote>`);
          quote = [];
        };
        for (const l of p.split('\n')) {
          if (/^>\s?/.test(l)) {
            quote.push(line(l.replace(/^>\s?/, '')));
            continue;
          }
          flushQ();
          if (l.trim()) out.push(line(l));
        }
        flushQ();
      });
    return out.join('');
  };
}

function renderItems(items, md) {
  return items
    .map((it) => {
      if (it.kind === 'text') return `<div class="td">${md(it.content)}</div>`;
      if (it.kind === 'sep') return '<hr>';
      if (it.kind === 'img') return it.url ? `<img class="media" src="${esc(it.url)}" alt="">` : '';
      if (it.kind === 'file') return `<a class="file" href="${esc(it.url || '#')}">📄 ${esc((it.url || 'plik').split('/').pop().split('?')[0])}</a>`;
      if (it.kind === 'select') return `<div class="select"><span>${md(it.label)}</span><span>⌄</span></div>`;
      if (it.kind === 'button') {
        const em = it.emoji?.id ? `<img class="emoji" src="https://cdn.discordapp.com/emojis/${it.emoji.id}.png" alt="">` : esc(it.emoji?.name || '');
        return `<span class="btn">${em}${esc(it.label)}</span>`;
      }
      if (it.kind === 'container') return `<div class="container">${renderItems(it.items, md)}</div>`;
      return '';
    })
    .join('');
}

function renderTranscript({ guild, channel, messages, ticket }) {
  const names = new Map();
  for (const m of messages) {
    names.set(m.author.id, m.author.globalName || m.author.username);
    for (const u of m.mentions?.users?.values?.() || []) names.set(u.id, u.globalName || u.username);
  }
  names.set(ticket.ownerId, names.get(ticket.ownerId) || ticket.ownerTag);
  names.set('c' + channel.id, channel.name);
  const md = makeMd(names);
  const cat = config.categories[ticket.category];
  let last = null;
  const rows = messages.map((m) => {
    const a = m.author;
    const grouped = last && last.author.id === a.id && m.createdTimestamp - last.createdTimestamp < 5 * 60000;
    last = m;
    let body = m.content ? `<div class="content">${md(m.content)}</div>` : '';
    for (const e of m.embeds || []) {
      body += `<div class="container">${e.title ? `<div class="h">${esc(e.title)}</div>` : ''}${e.description ? `<div class="td">${md(e.description)}</div>` : ''}${(e.fields || []).map((f) => `<div class="td"><b>${esc(f.name)}</b>${md(f.value)}</div>`).join('')}</div>`;
    }
    body += renderItems(flatten((m.components || []).map((c) => (c.toJSON ? c.toJSON() : c))), md);
    for (const att of m.attachments?.values?.() || []) {
      const isImg = att.contentType?.startsWith('image/') || /\.(png|jpe?g|gif|webp)(\?|$)/i.test(att.url);
      body += isImg
        ? `<a href="${esc(att.url)}"><img class="media" src="${esc(att.url)}" alt=""></a>`
        : `<a class="file" href="${esc(att.url)}">📄 ${esc(att.name || 'plik')}</a>`;
    }
    const name = a.globalName || a.username;
    return grouped
      ? `<div class="msg g"><span class="gt">${m.createdAt.toLocaleTimeString('pl-PL', { timeZone: 'Europe/Warsaw', hour: '2-digit', minute: '2-digit' })}</span><div class="body">${body}</div></div>`
      : `<div class="msg"><img class="av" src="${esc(a.displayAvatarURL({ size: 64 }))}" alt=""><div class="body"><div class="meta"><span class="name">${esc(name)}</span>${a.bot ? '<span class="tag">APL.</span>' : ''}<span class="time">${fmtDate(m.createdAt)}</span></div>${body}</div></div>`;
  });
  const stat = (k, v) => `<div class="stat"><span>${k}</span><b>${esc(v)}</b></div>`;
  const icon = guild?.iconURL({ size: 128 }) || client.user?.displayAvatarURL({ size: 128 }) || '';
  return `<!doctype html><html lang="pl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Transkrypt ${esc(ticket.id)} × ${esc(SHOP)}</title>
<style>
:root{--bg:#313338;--panel:#2b2d31;--deep:#1e1f22;--line:#3f4147;--text:#dbdee1;--dim:#949ba4;--accent:${config.transcriptColor}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:15px/1.4 "gg sans","Noto Sans",Helvetica,Arial,sans-serif}
a{color:#00a8fc;text-decoration:none}a:hover{text-decoration:underline}
header{background:linear-gradient(120deg,#052e16,#0b0f0c 70%);border-bottom:3px solid var(--accent);padding:26px 20px}
.hw{max-width:980px;margin:0 auto;display:flex;flex-wrap:wrap;gap:20px;align-items:center;justify-content:space-between}
.id{display:flex;gap:14px;align-items:center}.logo{width:64px;height:64px;border-radius:16px;background:var(--deep)}
.brand{font:800 12px/1 ui-monospace,Menlo,Consolas,monospace;letter-spacing:.18em;color:#86efac}
h1{margin:6px 0 0;color:#fff;font-size:26px}h1 small{color:var(--dim);font-size:15px;font-weight:500}
.stats{display:flex;flex-wrap:wrap;gap:10px}.stat{background:rgba(0,0,0,.3);border:1px solid rgba(34,197,94,.35);border-radius:10px;padding:8px 12px;min-width:110px}
.stat span{display:block;font-size:11px;text-transform:uppercase;letter-spacing:.06em;color:var(--dim)}.stat b{color:#fff;font-size:14px}
main{max-width:980px;margin:0 auto;padding:16px 20px 40px}
.msg{display:flex;gap:16px;padding:6px 8px;border-radius:6px;margin-top:12px}.msg:hover{background:#2e3035}
.msg.g{margin-top:0;padding-top:1px;padding-bottom:1px}.gt{width:40px;flex:none;font-size:11px;color:transparent;text-align:right;padding-top:4px}.msg.g:hover .gt{color:var(--dim)}
.av{width:40px;height:40px;border-radius:50%;flex:none;background:var(--deep)}
.body{min-width:0;flex:1}.meta{display:flex;align-items:baseline;gap:6px;flex-wrap:wrap}.name{font-weight:600;color:#f2f3f5}.time{font-size:12px;color:var(--dim)}
.tag{background:#5865f2;color:#fff;font-size:10px;font-weight:600;padding:1px 5px;border-radius:4px}
.content,.td{overflow-wrap:anywhere}.td+.td{margin-top:6px}
.container{background:var(--panel);border:1px solid var(--line);border-radius:8px;padding:12px 14px;margin-top:6px;max-width:560px}
hr{border:0;border-top:1px solid var(--line);margin:10px 0}
pre{background:#1e1f22;border:1px solid #232428;border-radius:6px;padding:9px 12px;margin:4px 0;font:14px/1.5 ui-monospace,Menlo,Consolas,monospace;color:#dbdee1;white-space:pre-wrap}
code{background:#1e1f22;border-radius:4px;padding:0 4px;font:13px ui-monospace,Menlo,Consolas,monospace}
blockquote{margin:2px 0;border-left:4px solid #4e5058;padding-left:12px;line-height:1.6}
b{color:#f2f3f5}.sub{font-size:12px;color:var(--dim)}.h{font-weight:700;color:#fff;font-size:18px;margin:2px 0}
.emoji{width:20px;height:20px;vertical-align:-4px;object-fit:contain}.h .emoji{width:26px;height:26px;vertical-align:-6px}
.mention{background:rgba(88,101,242,.3);color:#c9cdfb;border-radius:3px;padding:0 2px}
.ts{background:rgba(255,255,255,.06);border-radius:3px;padding:0 3px}
.select{background:var(--deep);border:1px solid #45474e;border-radius:8px;padding:10px 12px;display:flex;justify-content:space-between;color:var(--dim)}
.btn{display:flex;width:max-content;align-items:center;gap:4px;margin:6px 0 0;padding:6px 14px;border-radius:8px;color:#fff;font-size:14px;background:#4e5058}
.media{display:block;max-width:100%;border-radius:8px;margin-top:6px;max-height:340px}
.file{display:inline-flex;gap:8px;background:var(--panel);border:1px solid var(--line);border-radius:8px;padding:10px 14px;margin-top:6px}
footer{max-width:980px;margin:0 auto;padding:16px 20px 30px;color:var(--dim);font-size:12px;border-top:1px solid var(--line)}
</style></head><body>
<header><div class="hw"><div class="id">${icon ? `<img class="logo" src="${esc(icon)}" alt="">` : ''}<div>
<div class="brand">${esc(SHOP.toUpperCase())}™ × TRANSKRYPT</div>
<h1>${esc(cat?.prefix || '🎫')} Ticket ${esc(ticket.id)} <small>#${esc(channel.name)}</small></h1>
</div></div><div class="stats">
${stat('Kategoria', cat?.label || '—')}
${stat('Klient', ticket.ownerTag)}
${stat('Przejął', ticket.claimedBy ? names.get(ticket.claimedBy) || ticket.claimedBy : 'Nikt')}
${stat('Zrealizowane', ticket.completed ? (ticket.amount ? zl(ticket.amount) : 'Tak') : 'Nie')}
${stat('Wiadomości', String(messages.length))}
</div></div></header>
<main>${rows.join('\n')}</main>
<footer>© ${new Date().getFullYear()} ${esc(SHOP)}™ × Otwarty ${fmtDate(new Date(ticket.createdAt * 1000))} × Zamknięty ${fmtDate(new Date((ticket.closedAt || now()) * 1000))} × Powód: ${esc(ticket.closeReason || 'Brak')}</footer>
</body></html>`;
}

async function fetchAllMessages(channel, max = 2000) {
  const all = [];
  let before;
  while (all.length < max) {
    const batch = await channel.messages.fetch({ limit: 100, before, cache: false });
    if (!batch.size) break;
    all.push(...batch.values());
    before = batch.last().id;
    if (batch.size < 100) break;
  }
  return all.reverse();
}

// ───────────── zamykanie ticketu ─────────────
async function closeTicket(interaction, t, reason = 'Ticket zamknięty.') {
  const channel = interaction.channel;
  const started = Date.now();
  await interaction.reply(
    channelNotice(`🔒${S}${SHOP} × Zamykanie`, [`${E.e2}${S}Ticket zostanie zamknięty za **5 sekund**...`]),
  );

  t.closed = true;
  t.closedAt = now();
  t.closedBy = interaction.user.id;
  t.closeReason = reason;
  tdb.save();

  const messages = await fetchAllMessages(channel).catch((e) => {
    console.error('Transkrypt:', e.message);
    return [];
  });
  const fileName = `transcript-${t.id}.html`;
  const buffer = Buffer.from(renderTranscript({ guild: interaction.guild, channel, messages, ticket: t }), 'utf8');

  const owner = await client.users.fetch(t.ownerId).catch(() => null);
  if (owner) await owner.send(buildCloseDm(t, interaction.guild, fileName, buffer)).catch(() => console.log(`ℹ️ ${t.ownerTag} ma zablokowane PV`));
  const log = await getLogChannel(interaction.guild).catch((e) => console.error('Logi:', e.message));
  if (log) await log.send(buildCloseLog(t, fileName, buffer, messages.length)).catch((e) => console.error('Logi:', e.message));

  await sleep(Math.max(0, 5000 - (Date.now() - started)));
  await channel.delete(`Ticket ${t.id} zamknięty`).catch((e) => console.error('Usuwanie kanału:', e.message));
  console.log(`🔒 Zamknięto ticket ${t.id}`);
}

// ───────────── oznaczanie jako zrealizowane ─────────────
async function markDone(interaction, t, amount, note) {
  const sellerId = t.claimedBy || interaction.user.id;
  t.completed = true;
  t.completedBy = sellerId;
  t.amount = amount || 0;
  tdb.sellers[sellerId] = { completed: sellerCompleted(sellerId) + 1 };
  tdb.save();
  if (amount) {
    addPurchase({ guildId: t.guildId, clientId: t.ownerId, sellerId, amount, note, ticketId: t.id });
  }
  await interaction.channel.send({
    ...channelNotice(
      `✅${S}${SHOP} × Zrealizowano`,
      [
        `> • ${E.e2}${S}**Zlecenie:** \`${t.id}\``,
        `> • ${E.e2}${S}**Zrealizował:** <@${sellerId}>`,
        ...(amount ? [`> • ${E.e2}${S}**Kwota:** \`${zl(amount)}\``] : []),
        ...(note ? [`> • ${E.e2}${S}**Opis:** \`${clean(note)}\``] : []),
        '',
        `${E.e2}${S}Dziękujemy <@${t.ownerId}> za zakupy w **${SHOP}**!`,
      ],
      'Zlecenie zrealizowane',
    ),
    allowedMentions: { users: [t.ownerId] },
  });
}

// ╭──────────────────────────────────────────────╮
// │                   KOMENDY                     │
// ╰──────────────────────────────────────────────╯
const commands = [
  new SlashCommandBuilder()
    .setName('panel-klienta')
    .setDescription('Wysyła Panel Klienta na ten kanał')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),
  new SlashCommandBuilder()
    .setName('panel-ticketow')
    .setDescription('Wysyła panel ticketów na ten kanał')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),
  new SlashCommandBuilder()
    .setName('zakup')
    .setDescription('Zakupy klientów (dla sprzedawców)')
    .addSubcommand((s) =>
      s
        .setName('dodaj')
        .setDescription('Dodaj zakup klienta')
        .addUserOption((o) => o.setName('klient').setDescription('Kto kupił').setRequired(true))
        .addNumberOption((o) => o.setName('kwota').setDescription('Kwota w zł, np. 25.50').setRequired(true).setMinValue(0.01))
        .addStringOption((o) => o.setName('opis').setDescription('Co kupił, np. 10kk waluty').setMaxLength(100)),
    )
    .addSubcommand((s) =>
      s
        .setName('usun')
        .setDescription('Usuń zakup (pomyłka)')
        .addIntegerOption((o) => o.setName('numer').setDescription('Numer zakupu, np. 12').setRequired(true).setMinValue(1)),
    ),
].map((c) => c.toJSON());

async function registerCommands(guild) {
  const rest = new REST({ version: '10' }).setToken(client.token);
  await rest
    .put(Routes.applicationGuildCommands(client.user.id, guild.id), { body: commands })
    .then(() => console.log(`✅ Komendy /panel-klienta, /panel-ticketow, /zakup dodane na serwerze: ${guild.name}`))
    .catch((e) => console.error(`❌ Komendy (${guild.name}):`, e.message));
}

client.once('clientReady', async () => {
  console.log(`✅ Zalogowano jako ${client.user.tag}`);
  if (!client.guilds.cache.size) console.log('⚠️ Bot nie jest na żadnym serwerze – zaproś go.');
  for (const guild of client.guilds.cache.values()) await registerCommands(guild);
});
client.on('guildCreate', registerCommands);
// ktoś ręcznie usunął kanał ticketu
client.on('channelDelete', (ch) => {
  const t = tdb.tickets[ch.id];
  if (t && !t.closed) {
    t.closed = true;
    t.closedAt = now();
    t.closeReason = 'Kanał usunięty ręcznie';
    tdb.save();
  }
});

// ╭──────────────────────────────────────────────╮
// │                 INTERAKCJE                    │
// ╰──────────────────────────────────────────────╯
const NO_PERMS = 'Nie masz uprawnień do tej akcji – może to zrobić tylko sprzedawca.';

async function handle(interaction) {
  if (!interaction.inGuild()) return;

  // ── komendy ──
  if (interaction.isChatInputCommand()) {
    if (interaction.commandName === 'panel-klienta' || interaction.commandName === 'panel-ticketow') {
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      await interaction.channel.send(interaction.commandName === 'panel-klienta' ? buildPanel() : buildTicketPanel());
      return interaction.editReply(notice('Panel wysłany!', true));
    }
    if (interaction.commandName === 'zakup') {
      if (!isSeller(interaction.member)) return interaction.reply(notice(NO_PERMS));
      const sub = interaction.options.getSubcommand();
      if (sub === 'dodaj') {
        const klient = interaction.options.getUser('klient');
        const kwota = Math.round(interaction.options.getNumber('kwota') * 100) / 100;
        const opis = interaction.options.getString('opis') || '';
        const p = addPurchase({ guildId: interaction.guildId, clientId: klient.id, sellerId: interaction.user.id, amount: kwota, note: opis });
        const total = purchasesOf(klient.id, interaction.guildId).reduce((s, x) => s + x.amount, 0);
        return interaction.reply({
          ...notice(
            `Dodano zakup **#${pad(p.number)}**: <@${klient.id}> za \`${zl(kwota)}\`${opis ? ` (${clean(opis)})` : ''}.\n-# Klient wydał łącznie \`${zl(total)}\``,
            true,
          ),
          allowedMentions: { parse: [] },
        });
      }
      if (sub === 'usun') {
        const numer = interaction.options.getInteger('numer');
        const i = db.purchases.findIndex((p) => p.number === numer && p.guildId === interaction.guildId);
        if (i === -1) return interaction.reply(notice(`Nie ma zakupu #${pad(numer)}.`));
        const [p] = db.purchases.splice(i, 1);
        db.save();
        return interaction.reply({
          ...notice(`Usunięto zakup **#${pad(p.number)}** (<@${p.clientId}>, \`${zl(p.amount)}\`).`, true),
          allowedMentions: { parse: [] },
        });
      }
    }
    return;
  }

  // ── panel klienta ──
  if (interaction.isStringSelectMenu() && interaction.customId === 'vs_panel') {
    const payload =
      interaction.values[0] === 'wydane'
        ? buildSpent(interaction.user.id, interaction.guildId)
        : buildHistory(interaction.user.id, interaction.guildId);
    await interaction.reply(payload);
    return interaction.message.edit(buildPanel()).catch(() => {});
  }

  // ── panel ticketów: wybór kategorii → formularz ──
  if (interaction.isStringSelectMenu() && interaction.customId === 'vs_ticket') {
    const catId = interaction.values[0];
    if (!config.categories[catId]) return;
    const open = Object.values(tdb.tickets).find((t) => !t.closed && t.ownerId === interaction.user.id && t.guildId === interaction.guildId);
    if (open && (await interaction.guild.channels.fetch(open.channelId).catch(() => null)) && config.maxOpenTickets <= 1) {
      await interaction.reply(notice(`Masz już otwarty ticket: <#${open.channelId}>`));
    } else {
      await interaction.showModal(buildTicketModal(catId));
    }
    // reset wyboru w menu
    return interaction.message.edit({ ...buildTicketPanel(), attachments: [] }).catch(() => {});
  }

  if (interaction.isModalSubmit() && interaction.customId.startsWith('vsm_') && config.categories[interaction.customId.slice(4)]) {
    return createTicket(interaction, interaction.customId.slice(4));
  }

  // ── akcje w tickecie ──
  const t = getTicket(interaction.channelId);
  const ticketIds = ['vs_close', 'vs_claim', 'vs_settings', 'vs_set', 'vs_add', 'vs_remove', 'vsm_done'];
  if (!ticketIds.includes(interaction.customId)) return;
  if (!t) return interaction.reply(notice('To nie jest aktywny ticket.'));
  if (!isSeller(interaction.member)) return interaction.reply(notice(NO_PERMS));

  if (interaction.customId === 'vs_close') return closeTicket(interaction, t);

  if (interaction.customId === 'vs_claim') {
    if (t.claimedBy) return interaction.reply({ ...notice(`Ten ticket już przejął <@${t.claimedBy}>.`), allowedMentions: { parse: [] } });
    t.claimedBy = interaction.user.id;
    tdb.save();
    return interaction.reply(buildClaimNotice(t, interaction.user.id));
  }

  if (interaction.customId === 'vs_settings') return interaction.reply(buildSettings());

  if (interaction.customId === 'vs_set') {
    const action = interaction.values[0];
    if (action === 'add' || action === 'remove') return interaction.update(asUpdate(buildUserPick(action)));

    if (action === 'summon') {
      const owner = await client.users.fetch(t.ownerId).catch(() => null);
      const dmOk = owner ? await owner.send(buildSummonDm(t, interaction.user.id)).then(() => true).catch(() => false) : false;
      await interaction.channel.send({
        ...channelNotice(`🛒${S}${SHOP} × Wezwanie`, [`${E.e2}${S}<@${t.ownerId}>, zostałeś wezwany na Ticketa przez <@${interaction.user.id}>!`]),
        allowedMentions: { users: [t.ownerId] },
      });
      return interaction.update(
        asUpdate(notice(dmOk ? 'Wezwano klienta – dostał wiadomość na PV.' : 'Klient ma zablokowane PV – oznaczyłem go na kanale.', dmOk)),
      );
    }

    if (action === 'done') {
      if (t.completed) return interaction.update(asUpdate(notice('Ten ticket jest już oznaczony jako zrealizowany.')));
      if (t.category === 'zakup') return interaction.showModal(buildDoneModal(t));
      await markDone(interaction, t, 0, '');
      return interaction.update(asUpdate(notice('Oznaczono ticket jako zrealizowany.', true)));
    }
    return;
  }

  if (interaction.customId === 'vs_add' || interaction.customId === 'vs_remove') {
    const adding = interaction.customId === 'vs_add';
    const done = [];
    for (const user of interaction.users.values()) {
      if (user.bot) continue;
      if (!adding && user.id === t.ownerId) continue;
      if (adding) {
        await interaction.channel.permissionOverwrites.edit(
          user.id,
          { ViewChannel: true, SendMessages: true, ReadMessageHistory: true, AttachFiles: true, EmbedLinks: true },
          { type: OverwriteType.Member },
        );
      } else {
        await interaction.channel.permissionOverwrites.delete(user.id).catch(() => {});
      }
      done.push(user.id);
    }
    if (!done.length) return interaction.update(asUpdate(notice(adding ? 'Nikogo nie dodano.' : 'Nie można usunąć właściciela ticketa.')));
    const list = done.map((id) => `<@${id}>`).join(', ');
    await interaction.channel.send({
      ...channelNotice(`⚙️${S}${SHOP} × Ustawienia Ticketa`, [
        `${E.e2}${S}${list} ${adding ? 'został(a) dodany(a) do' : 'został(a) usunięty(a) z'} Ticketa przez <@${interaction.user.id}>`,
      ]),
      allowedMentions: { users: adding ? done : [] },
    });
    return interaction.update(asUpdate(notice(`${adding ? 'Dodano' : 'Usunięto'}: ${list}`, true)));
  }

  if (interaction.customId === 'vsm_done') {
    if (t.completed) return interaction.reply(notice('Ten ticket jest już oznaczony jako zrealizowany.'));
    const amount = Math.round(parseFloat(interaction.fields.getTextInputValue('kwota').replace(',', '.').replace(/[^\d.]/g, '')) * 100) / 100;
    if (!amount || amount <= 0) return interaction.reply(notice('Podaj poprawną kwotę, np. `25.50`.'));
    const note = interaction.fields.getTextInputValue('opis') || '';
    await markDone(interaction, t, amount, note);
    return interaction.reply(notice(`Zrealizowano! Dodano zakup za \`${zl(amount)}\` do historii klienta.`, true));
  }
}

client.on('interactionCreate', async (interaction) => {
  try {
    await handle(interaction);
  } catch (e) {
    console.error(e);
    if (!interaction.isRepliable()) return;
    const msg = notice('Coś poszło nie tak, spróbuj ponownie.');
    if (interaction.deferred) interaction.editReply(msg).catch(() => {});
    else if (!interaction.replied) interaction.reply(msg).catch(() => {});
  }
});

function start() {
  const token = (process.env.TOKEN || config.token || '').trim();
  if (!token) {
    console.error("❌ Brak tokena! Wpisz TOKEN=... w pliku .env albo token: '...' na górze index.js");
    process.exit(1);
  }
  return client.login(token).catch((e) => {
    console.error('❌ Nie udało się zalogować:', e.message);
    if (/token/i.test(e.message)) console.error('   → Token jest zły. Zresetuj go na discord.com/developers → Bot → Reset Token.');
    if (/intent/i.test(e.message)) console.error('   → Włącz intenty na discord.com/developers → Bot → Privileged Gateway Intents.');
    process.exit(1);
  });
}

module.exports = {
  buildPanel,
  buildSpent,
  buildHistory,
  buildTicketPanel,
  buildTicketModal,
  buildTicketCreated,
  buildSummary,
  buildClaimNotice,
  buildSettings,
  buildUserPick,
  buildSummonDm,
  buildDoneModal,
  buildCloseDm,
  buildCloseLog,
  renderTranscript,
  _render: { makeMd, flatten, renderItems },
  _handle: handle,
  start,
};
if (require.main === module) start();
