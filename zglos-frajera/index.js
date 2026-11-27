console.log('▶ Start bota ZgłośFrajera...');
try {
  require('dotenv').config({ path: require('path').join(__dirname, '.env') });
} catch {
  /* brak dotenv – token można wpisać niżej w config */
}
const fs = require('fs');
const path = require('path');
const {
  Client,
  GatewayIntentBits,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  ModalBuilder,
  TextInputBuilder,
  TextInputStyle,
  SlashCommandBuilder,
  PermissionFlagsBits,
  REST,
  Routes,
  AttachmentBuilder,
  ContainerBuilder,
  TextDisplayBuilder,
  SeparatorBuilder,
  SeparatorSpacingSize,
  MediaGalleryBuilder,
  MediaGalleryItemBuilder,
  MessageFlags,
  Options,
  ChannelType,
} = require('discord.js');
// ╭──────────────────────────────────────────────╮
// │        ZgłośFrajera ︲ konfiguracja            │
// ╰──────────────────────────────────────────────╯
const config = {
  // Znak oddzielający emotkę od tekstu
  sep: '︲',
  // Kolor paska kontenera (null = brak paska)
  color: 0x2b2d31,

  // Emotki serwerowe (bot musi być na serwerze, z którego pochodzą)
  emojis: {
    opis: '<:emoji_9:1564588745863340173>',
    przycisk: { id: '1564588950415343736', name: 'emoji_11' },
    stopka: '<:emoji_4:1564588437397573694>',
    // Panel Oszustów + Historia
    panelOpis: '<:emoji_11:1564588865971556432>',
    statystyki: { id: '1564588387355201546', name: 'emoji_3' },
    historia: '<:emoji_11:1564588950415343736>',
    wpis: '<:emoji_7:1564588614686482524>',
  },

  // Baner: link https://... albo '' żeby użyć pliku banner.png (obok index.js)
  bannerUrl: '',
  bannerFile: 'banner.png',

  // Kanał, na który trafiają zgłoszenia (PPM na kanał → Kopiuj ID).
  // Zostaw puste '' – bot sam utworzy prywatny kanał (widoczny tylko dla administracji).
  reportChannelId: '',
  reportChannelName: '🚨︲zgłoszenia',

  // Token bota. Najlepiej w pliku .env (TOKEN=...), ale jak nie umiesz
  // zrobić .env na hostingu, możesz go wkleić tutaj między ''.
  token: '',
};


const S = config.sep;
const E = config.emojis;
const V2 = MessageFlags.IsComponentsV2;
const EPH = MessageFlags.Ephemeral | MessageFlags.IsComponentsV2;

// Minimalny cache = minimalne zużycie RAM (bot potrzebuje tylko kanałów i serwera)
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
const clean = (s) => String(s ?? '').replace(/`/g, 'ˋ').trim();

const BANNER_PATH = path.join(__dirname, config.bannerFile || '');
const hasBannerFile = () => !config.bannerUrl && config.bannerFile && fs.existsSync(BANNER_PATH);
const bannerFiles = () => (hasBannerFile() ? [new AttachmentBuilder(BANNER_PATH, { name: 'banner.png' })] : []);
const bannerSrc = () => config.bannerUrl || (hasBannerFile() ? 'attachment://banner.png' : null);

function newContainer() {
  const c = new ContainerBuilder();
  if (config.color != null) c.setAccentColor(config.color);
  return c;
}

function notice(msg, ok = false) {
  return { components: [newContainer().addTextDisplayComponents(text(`${ok ? '✅' : '❌'} ${S} ${msg}`))], flags: EPH };
}

// ───────────── zapis zgłoszeń (plik zgloszenia.json obok index.js) ─────────────
const DB_FILE = path.join(__dirname, 'zgloszenia.json');
function loadDb() {
  try {
    return JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
  } catch {
    return { counter: 0, reports: [] };
  }
}
const db = loadDb();
function saveReport(r) {
  db.counter += 1;
  const report = { number: db.counter, ...r };
  db.reports.push(report);
  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2));
  } catch (e) {
    console.error('❌ Nie udało się zapisać zgloszenia.json:', e.message);
  }
  return report;
}
const pad = (n) => String(n).padStart(4, '0');

// ───────────── PANEL ─────────────
//  Nagłówek → separator → opis → separator → 2 przyciski → separator → baner → separator → stopka
function buildPanel() {
  const c = newContainer();

  c.addTextDisplayComponents(text('```\n🔔︲Zgłoś Frajera × Panel\n```'));
  c.addSeparatorComponents(sep());

  c.addTextDisplayComponents(
    text(`> ${E.opis}${S}Hejka Chciałbyś może zgłosić Frajera który cię oszukał? Jeśli tak, to zrób to poniżej`),
  );
  c.addSeparatorComponents(sep());

  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(
      new ButtonBuilder()
        .setCustomId('zf_oszust')
        .setStyle(ButtonStyle.Secondary)
        .setEmoji(E.przycisk)
        .setLabel(`${S}Zgłoś Oszusta`),
      new ButtonBuilder()
        .setCustomId('zf_scammer')
        .setStyle(ButtonStyle.Secondary)
        .setEmoji(E.przycisk)
        .setLabel(`${S}Zgłoś Scammera`),
    ),
  );

  const src = bannerSrc();
  if (src) {
    c.addSeparatorComponents(sep());
    c.addMediaGalleryComponents(new MediaGalleryBuilder().addItems(new MediaGalleryItemBuilder().setURL(src)));
  }

  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`-# ${E.stopka}${S}Panel zgłoś oszusta`));

  return { components: [c], files: bannerFiles(), flags: V2 };
}

// ───────────── formularz zgłoszenia ─────────────
const TYPES = {
  zf_oszust: { label: 'Oszust', title: 'Zgłoś Oszusta' },
  zf_scammer: { label: 'Scammer', title: 'Zgłoś Scammera' },
};

function buildModal(typeId) {
  const input = (id, label, style, placeholder, required = true, max = 1000) =>
    new ActionRowBuilder().addComponents(
      new TextInputBuilder()
        .setCustomId(id)
        .setLabel(label)
        .setStyle(style)
        .setPlaceholder(placeholder)
        .setRequired(required)
        .setMaxLength(max),
    );
  return new ModalBuilder()
    .setCustomId(`zfm_${typeId}`)
    .setTitle(TYPES[typeId].title)
    .addComponents(
      input('nick', 'Nick / ID frajera', TextInputStyle.Short, 'np. frajer123 lub 123456789012345678', true, 100),
      input('co', 'Co się stało?', TextInputStyle.Paragraph, 'Opisz dokładnie jak cię oszukał...'),
      input('strata', 'Strata', TextInputStyle.Short, 'np. 50 zł / konto / item', false, 100),
      input('dowody', 'Dowody (linki do screenów)', TextInputStyle.Paragraph, 'https://...', false),
    );
}

function buildReport(interaction, typeId, number) {
  const f = (id) => clean(interaction.fields.getTextInputValue(id)) || 'Brak';
  const c = newContainer();
  c.addTextDisplayComponents(text(`\`\`\`\n🚨︲Zgłoszenie #${pad(number)} × ${TYPES[typeId].label}\n\`\`\``));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(
      `> 👤${S}**Zgłaszający:** <@${interaction.user.id}>\n` +
        `> 🎯${S}**Frajer:** \`${f('nick').slice(0, 100)}\`\n` +
        `> 💸${S}**Strata:** \`${f('strata').slice(0, 100)}\`\n` +
        `> 📅${S}**Data:** <t:${Math.floor(Date.now() / 1000)}:f>`,
    ),
  );
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`> 📝${S}**Co się stało:**\n\`\`\`\n${f('co').slice(0, 1000)}\n\`\`\``));
  c.addTextDisplayComponents(text(`> 📎${S}**Dowody:**\n${f('dowody').slice(0, 1000)}`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`-# ${E.stopka}${S}Panel zgłoś oszusta`));
  return { components: [c], flags: V2, allowedMentions: { parse: [] } };
}

// ───────────── PANEL OSZUSTÓW ─────────────
//  Nagłówek → separator → opis → separator → przycisk → separator → baner → separator → stopka
function buildFraudPanel() {
  const c = newContainer();
  c.addTextDisplayComponents(text('```\n🖌️︲Panel × Oszustów\n```'));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(`> ${E.panelOpis}${S}Witamy w Panelu Oszustów! Poniżej możesz sprawdzić, ilu oszustów już zgłosiłeś`),
  );
  c.addSeparatorComponents(sep());
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(
      new ButtonBuilder()
        .setCustomId('zf_stats')
        .setStyle(ButtonStyle.Secondary)
        .setEmoji(E.statystyki)
        .setLabel(`${S}Sprawdź Statystyki`),
    ),
  );
  const src = bannerSrc();
  if (src) {
    c.addSeparatorComponents(sep());
    c.addMediaGalleryComponents(new MediaGalleryBuilder().addItems(new MediaGalleryItemBuilder().setURL(src)));
  }
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`-# ${E.wpis}${S}Panel Oszustów`));
  return { components: [c], files: bannerFiles(), flags: V2 };
}

// Historia zgłoszeń danej osoby (widzi tylko ona)
const HISTORY_LIMIT = 10;
function buildHistory(userId, guildId) {
  const mine = db.reports.filter((r) => r.reporterId === userId && r.guildId === guildId);
  const count = (t) => mine.filter((r) => r.type === t).length;

  const c = newContainer();
  c.addTextDisplayComponents(text('```\n✏️︲Zgłoś Oszusta × Historia\n```'));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`> # ${E.historia}${S}__Historia poniżej__`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(
      `> 📊${S}**Wszystkich zgłoszeń:** \`${mine.length}\`\n` +
        `> 🎯${S}**Oszuści:** \`${count('zf_oszust')}\`\n` +
        `> 💀${S}**Scammerzy:** \`${count('zf_scammer')}\``,
    ),
  );
  c.addSeparatorComponents(sep());

  if (!mine.length) {
    c.addTextDisplayComponents(text(`> ${E.wpis}${S}Nie zgłosiłeś jeszcze nikogo.`));
  } else {
    const lines = mine
      .slice(-HISTORY_LIMIT)
      .reverse()
      .map(
        (r) =>
          `> ${E.wpis}${S}**#${pad(r.number)}** \`${clean(r.nick).slice(0, 40)}\` • ${TYPES[r.type]?.label || 'Zgłoszenie'} • <t:${r.createdAt}:R>`,
      );
    if (mine.length > HISTORY_LIMIT) lines.push(`-# …i ${mine.length - HISTORY_LIMIT} starszych zgłoszeń`);
    c.addTextDisplayComponents(text(lines.join('\n')));
  }

  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`-# ${E.wpis}${S}Panel Oszustów`));
  return { components: [c], flags: EPH, allowedMentions: { parse: [] } };
}

// ───────────── kanał zgłoszeń ─────────────
// Ustawiony w config → ten. Jeśli nie ma, szuka po nazwie, a jak nie znajdzie – tworzy prywatny kanał.
async function getReportChannel(guild) {
  if (/^\d{17,20}$/.test(config.reportChannelId)) {
    const ch = await guild.channels.fetch(config.reportChannelId).catch(() => null);
    if (ch) return ch;
  }
  const channels = await guild.channels.fetch();
  const found = channels.find((c) => c && c.type === ChannelType.GuildText && c.name === config.reportChannelName);
  if (found) return found;
  const created = await guild.channels.create({
    name: config.reportChannelName,
    type: ChannelType.GuildText,
    topic: 'Zgłoszenia oszustów z panelu Zgłoś Frajera',
    permissionOverwrites: [
      { id: guild.roles.everyone.id, deny: [PermissionFlagsBits.ViewChannel] },
      { id: guild.members.me.id, allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.AttachFiles] },
    ],
  });
  console.log(`✅ Utworzono kanał zgłoszeń: #${created.name}`);
  return created;
}

// ───────────── komendy ─────────────
const commands = [
  new SlashCommandBuilder()
    .setName('panel-zglos')
    .setDescription('Wysyła panel „Zgłoś Frajera” na ten kanał')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),
  new SlashCommandBuilder()
    .setName('panel-oszustow')
    .setDescription('Wysyła „Panel Oszustów” (statystyki zgłoszeń) na ten kanał')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),
].map((c) => c.toJSON());

// Rejestruje /panel-zglos na serwerze (od razu widoczne, bez czekania)
async function registerCommands(guild) {
  const rest = new REST({ version: '10' }).setToken(client.token);
  await rest
    .put(Routes.applicationGuildCommands(client.user.id, guild.id), { body: commands })
    .then(() => console.log(`✅ Komendy /panel-zglos i /panel-oszustow dodane na serwerze: ${guild.name}`))
    .catch((e) => console.error(`❌ Komendy (${guild.name}):`, e.message));
}

client.once('clientReady', async () => {
  console.log(`✅ Zalogowano jako ${client.user.tag}`);
  if (!client.guilds.cache.size) console.log('⚠️ Bot nie jest na żadnym serwerze – zaproś go.');
  for (const guild of client.guilds.cache.values()) await registerCommands(guild);
});
client.on('guildCreate', registerCommands);

client.on('interactionCreate', async (interaction) => {
  try {
    if (interaction.isChatInputCommand() && interaction.commandName === 'panel-zglos') {
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      await interaction.channel.send(buildPanel());
      const reports = await getReportChannel(interaction.guild).catch((e) => console.error('Kanał zgłoszeń:', e.message));
      return interaction.editReply(
        notice(`Panel wysłany! Zgłoszenia będą trafiać na ${reports ? `<#${reports.id}>` : 'kanał zgłoszeń (nie udało się go utworzyć – sprawdź uprawnienia bota)'}`, true),
      );
    }

    if (interaction.isChatInputCommand() && interaction.commandName === 'panel-oszustow') {
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      await interaction.channel.send(buildFraudPanel());
      return interaction.editReply(notice('Panel Oszustów wysłany!', true));
    }

    if (interaction.isButton() && interaction.customId === 'zf_stats') {
      return interaction.reply(buildHistory(interaction.user.id, interaction.guildId));
    }

    if (interaction.isButton() && TYPES[interaction.customId]) {
      return interaction.showModal(buildModal(interaction.customId));
    }

    if (interaction.isModalSubmit() && interaction.customId.startsWith('zfm_')) {
      const typeId = interaction.customId.slice(4);
      if (!TYPES[typeId]) return;
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      const ch = await getReportChannel(interaction.guild).catch((e) => console.error('Kanał zgłoszeń:', e.message));
      if (!ch) return interaction.editReply(notice('Nie udało się wysłać zgłoszenia - napisz do administracji.'));
      const report = saveReport({
        reporterId: interaction.user.id,
        guildId: interaction.guildId,
        type: typeId,
        nick: interaction.fields.getTextInputValue('nick'),
        strata: interaction.fields.getTextInputValue('strata') || '',
        createdAt: Math.floor(Date.now() / 1000),
      });
      await ch.send(buildReport(interaction, typeId, report.number));
      return interaction.editReply(notice('Dzięki! Twoje zgłoszenie trafiło do administracji.', true));
    }
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
    console.error('❌ Brak tokena! Wpisz TOKEN=... w pliku .env albo token: \'...\' na górze index.js');
    process.exit(1);
  }
  return client.login(token).catch((e) => {
    console.error('❌ Nie udało się zalogować:', e.message);
    if (/token/i.test(e.message)) console.error('   → Token jest zły. Zresetuj go na discord.com/developers → Bot → Reset Token.');
    process.exit(1);
  });
}

module.exports = { buildPanel, buildFraudPanel, buildHistory, start };
if (require.main === module) start();
