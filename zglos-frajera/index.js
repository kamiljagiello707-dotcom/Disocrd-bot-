require('dotenv').config({ path: require('path').join(__dirname, '.env') });
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
  },

  // Baner: link https://... albo '' żeby użyć pliku banner.png (obok index.js)
  bannerUrl: '',
  bannerFile: 'banner.png',

  // Kanał, na który trafiają zgłoszenia (PPM na kanał → Kopiuj ID)
  reportChannelId: 'ID_KANALU_ZGLOSZEN',
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

function buildReport(interaction, typeId) {
  const f = (id) => clean(interaction.fields.getTextInputValue(id)) || 'Brak';
  const c = newContainer();
  c.addTextDisplayComponents(text(`\`\`\`\n🚨︲Nowe zgłoszenie × ${TYPES[typeId].label}\n\`\`\``));
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

// ───────────── komendy ─────────────
const commands = [
  new SlashCommandBuilder()
    .setName('panel-zglos')
    .setDescription('Wysyła panel „Zgłoś Frajera” na ten kanał')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),
].map((c) => c.toJSON());

client.once('clientReady', async () => {
  console.log(`✅ Zalogowano jako ${client.user.tag}`);
  const rest = new REST({ version: '10' }).setToken(process.env.TOKEN);
  await rest
    .put(Routes.applicationGuildCommands(process.env.CLIENT_ID, process.env.GUILD_ID), { body: commands })
    .then(() => console.log('✅ Komendy zarejestrowane'))
    .catch((e) => console.error('Komendy:', e.message));
});

client.on('interactionCreate', async (interaction) => {
  try {
    if (interaction.isChatInputCommand() && interaction.commandName === 'panel-zglos') {
      await interaction.channel.send(buildPanel());
      return interaction.reply(notice('Panel wysłany!', true));
    }

    if (interaction.isButton() && TYPES[interaction.customId]) {
      return interaction.showModal(buildModal(interaction.customId));
    }

    if (interaction.isModalSubmit() && interaction.customId.startsWith('zfm_')) {
      const typeId = interaction.customId.slice(4);
      if (!TYPES[typeId]) return;
      const ch = interaction.guild.channels.cache.get(config.reportChannelId);
      if (!ch) return interaction.reply(notice('Kanał zgłoszeń nie jest ustawiony - napisz do administracji.'));
      await ch.send(buildReport(interaction, typeId));
      return interaction.reply(notice('Dzięki! Twoje zgłoszenie trafiło do administracji.', true));
    }
  } catch (e) {
    console.error(e);
    if (interaction.isRepliable() && !interaction.replied && !interaction.deferred) {
      interaction.reply(notice('Coś poszło nie tak, spróbuj ponownie.')).catch(() => {});
    }
  }
});

function start() {
  if (!process.env.TOKEN) {
    console.error('❌ Brak TOKEN w pliku .env (musi leżeć obok index.js)');
    process.exit(1);
  }
  return client.login(process.env.TOKEN);
}

module.exports = { buildPanel, start };
if (require.main === module) start();
