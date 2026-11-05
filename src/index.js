require('dotenv').config();
const fs = require('fs');
const path = require('path');
const {
  Client,
  GatewayIntentBits,
  Partials,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  StringSelectMenuBuilder,
  UserSelectMenuBuilder,
  ModalBuilder,
  TextInputBuilder,
  TextInputStyle,
  ChannelType,
  PermissionFlagsBits,
  SlashCommandBuilder,
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
  FileBuilder,
} = require('discord.js');
const config = require('./config');

const client = new Client({
  intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.GuildMembers],
  partials: [Partials.Channel],
});

const S = config.sep;
const V2 = MessageFlags.IsComponentsV2;
const EPH = MessageFlags.Ephemeral | MessageFlags.IsComponentsV2;

// ───────────── zapis danych ─────────────
const DATA_DIR = path.join(__dirname, '..', 'data');
const DB_FILE = path.join(DATA_DIR, 'tickets.json');
function loadDb() {
  try { return JSON.parse(fs.readFileSync(DB_FILE, 'utf8')); } catch { return { counter: 0, tickets: {} }; }
}
let db = loadDb();
function saveDb() {
  if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2));
}
const getTicket = (channelId) => db.tickets[channelId] || null;

// ───────────── pomocnicze ─────────────
const getCategory = (id) => config.categories.find((c) => c.id === id);
const fancy = (s) => s.toUpperCase().replace(/[EĘ]/g, 'Ɛ').replace(/Ó/g, 'O').replace(/Ż|Ź/g, 'Z');
const isStaff = (member) =>
  member.permissions.has(PermissionFlagsBits.Administrator) ||
  config.staffRoleIds.some((r) => member.roles.cache.has(r));
const staffRole = () => config.staffRoleIds[0];
const now = () => Math.floor(Date.now() / 1000);
const clean = (s) => String(s ?? '').replace(/`/g, 'ˋ').trim();

const BANNER_PATH = path.join(__dirname, '..', config.bannerFile || '');
const hasBannerFile = () => !config.bannerUrl && config.bannerFile && fs.existsSync(BANNER_PATH);
const bannerFiles = () => (hasBannerFile() ? [new AttachmentBuilder(BANNER_PATH, { name: 'banner.png' })] : []);
const bannerSrc = () => config.bannerUrl || (hasBannerFile() ? 'attachment://banner.png' : null);

const text = (c) => new TextDisplayBuilder().setContent(c);
const sep = (big = false) => new SeparatorBuilder().setDivider(true).setSpacing(big ? SeparatorSpacingSize.Large : SeparatorSpacingSize.Small);

/**
 * Buduje kontener w stylu FutureShop:
 *  ┌ nagłówek w ramce (blok kodu) ┐
 *  ├ treść / sekcje ┤
 *  ├ menu / przyciski ┤
 *  ├ baner ┤
 *  └ © stopka ┘
 */
function shopContainer({ title, blocks = [], rows = [], banner = true, color = config.color, file = null }) {
  const c = new ContainerBuilder().setAccentColor(color);
  c.addTextDisplayComponents(text('```\n' + `${config.brandEmoji} ${S} ${config.brand}  ✖\n${fancy(title)}` + '\n```'));
  for (const b of blocks) {
    c.addSeparatorComponents(sep());
    for (const part of [].concat(b)) c.addTextDisplayComponents(text(part));
  }
  if (file) {
    c.addSeparatorComponents(sep());
    c.addFileComponents(new FileBuilder().setURL(`attachment://${file}`));
  }
  if (rows.length) {
    c.addSeparatorComponents(sep());
    c.addActionRowComponents(...rows);
  }
  const src = banner && bannerSrc();
  if (src) {
    c.addSeparatorComponents(sep());
    c.addMediaGalleryComponents(new MediaGalleryBuilder().addItems(new MediaGalleryItemBuilder().setURL(src)));
  }
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`-# © ${config.shopName}™ ${S} <t:${now()}:f>`));
  return c;
}
const withBanner = (container, banner = true) => ({
  components: [container],
  files: banner ? bannerFiles() : [],
  flags: V2,
});

// mała odpowiedź (błąd / sukces) bez banera
function notice(msg, ok = false) {
  const c = new ContainerBuilder()
    .setAccentColor(ok ? config.colorSuccess : config.color)
    .addTextDisplayComponents(text(`${ok ? '✅' : '❌'} ${S} ${msg}`));
  return { components: [c], flags: EPH };
}

async function sendLog(guild, payload) {
  const ch = guild.channels.cache.get(config.logChannelId);
  if (ch) await ch.send(payload).catch((e) => console.error('Log:', e.message));
}

// ───────────── panel ─────────────
function buildPanel() {
  const menu = new StringSelectMenuBuilder()
    .setCustomId('ticket_select')
    .setPlaceholder(`📁 ${S} Wybierz kategorię ticketa`)
    .addOptions(config.categories.map((c) => ({ label: c.label, value: c.id, description: c.description, emoji: c.emoji })));

  const container = shopContainer({
    title: 'Stwórz tickɛta',
    blocks: [
      [
        `> 🔀 ${S} **Wybierz kategorię** odpowiadającą Twojej sprawie.\n` +
          `> ℹ️ ${S} Podaj __komplet informacji__ w formularzu - szybciej ogarniemy temat.\n` +
          `> ⏰ ${S} Średni czas odpowiedzi: \`15 minut\``,
      ],
    ],
    rows: [new ActionRowBuilder().addComponents(menu)],
  });
  return withBanner(container);
}

// ───────────── wiadomość ticketu ─────────────
function ticketActions(t) {
  return new ActionRowBuilder().addComponents(
    new StringSelectMenuBuilder()
      .setCustomId('ticket_action')
      .setPlaceholder(`📁 ${S} Wybierz akcję ticketa`)
      .addOptions(
        { label: t.claimedBy ? 'Ticket przejęty' : 'Przejmij ticket', value: 'claim', emoji: '🙋', description: 'Tylko obsługa' },
        { label: 'Przypomnij klientowi', value: 'remind', emoji: '🔔', description: 'Ping + wiadomość prywatna' },
        { label: 'Dodaj osobę', value: 'add', emoji: '➕', description: 'Dodaj kogoś do ticketu' },
        { label: 'Usuń osobę', value: 'remove', emoji: '➖', description: 'Usuń kogoś z ticketu' },
        { label: 'Transkrypt', value: 'transcript', emoji: '📄', description: 'Pobierz zapis rozmowy' },
        { label: 'Zamknij ticket', value: 'close', emoji: '🔒', description: 'Zamknięcie z potwierdzeniem' },
        { label: 'Zamknij z powodem', value: 'close_reason', emoji: '📝', description: 'Podaj powód zamknięcia' },
      ),
  );
}

function renderTicket(t) {
  const cat = getCategory(t.categoryId);
  const blocks = [[`> ${cat.emoji} ${S} ${cat.intro}`]];

  // sekcja z danymi: cytaty, a odpowiedzi „block” w osobnym polu kodu
  const parts = [];
  let quote = [
    `> 👥 ${S} **Użytkownik:** <@${t.ownerId}>`,
    `> ↪️ ${S} **Obsługa:** ${t.claimedBy ? `<@${t.claimedBy}>` : staffRole() ? `<@&${staffRole()}>` : '`Brak`'}`,
    `> 🔢 ${S} **Numer:** \`#${t.number}\``,
  ];
  const flush = () => { if (quote.length) parts.push(quote.join('\n')); quote = []; };
  for (const q of cat.questions) {
    const val = clean(t.answers[q.id]) || 'Brak';
    if (q.display === 'block') {
      quote.push(`> ${q.emoji} ${S} **${q.label}:**`);
      flush();
      parts.push('```\n' + val.slice(0, 1000) + '\n```');
    } else {
      quote.push(`> ${q.emoji} ${S} **${q.label}:** \`${val.slice(0, 100)}\``);
    }
  }
  quote.push(`> 📅 ${S} **Utworzono:** <t:${t.createdAt}:R>`);
  flush();
  blocks.push(parts);

  return shopContainer({ title: cat.label, blocks, rows: [ticketActions(t)] });
}

async function refreshTicketMessage(channel, t) {
  const msg = await channel.messages.fetch(t.messageId).catch(() => null);
  if (msg) await msg.edit({ components: [renderTicket(t)], files: bannerFiles(), attachments: [], flags: V2 }).catch(console.error);
}

// ───────────── tworzenie ticketu ─────────────
async function createTicket(interaction, category, answers) {
  const guild = interaction.guild;
  const user = interaction.user;

  const open = Object.entries(db.tickets).filter(([id, t]) => t.ownerId === user.id && guild.channels.cache.has(id));
  if (open.length >= config.maxTicketsPerUser) {
    return interaction.editReply(notice(`Masz już otwarty ticket: <#${open[0][0]}>`));
  }

  db.counter += 1;
  const number = String(db.counter).padStart(4, '0');
  const nick = user.username.toLowerCase().replace(/[^a-z0-9._]/g, '').slice(0, 20) || 'user';

  const allowUser = [
    PermissionFlagsBits.ViewChannel,
    PermissionFlagsBits.SendMessages,
    PermissionFlagsBits.ReadMessageHistory,
    PermissionFlagsBits.AttachFiles,
    PermissionFlagsBits.EmbedLinks,
  ];
  const channel = await guild.channels.create({
    name: `${config.channelPrefix}${category.emoji}${S}${category.channel}•${nick}`,
    type: ChannelType.GuildText,
    parent: guild.channels.cache.has(config.ticketCategoryId) ? config.ticketCategoryId : null,
    topic: `${category.emoji} ${S} Ticket #${number} ${S} ${user.tag} (${user.id})`,
    permissionOverwrites: [
      { id: guild.roles.everyone.id, deny: [PermissionFlagsBits.ViewChannel] },
      { id: user.id, allow: allowUser },
      {
        id: client.user.id,
        allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ManageChannels, PermissionFlagsBits.ReadMessageHistory, PermissionFlagsBits.AttachFiles],
      },
      ...config.staffRoleIds.filter((r) => guild.roles.cache.has(r)).map((r) => ({ id: r, allow: allowUser })),
    ],
  });

  const t = { ownerId: user.id, categoryId: category.id, number, claimedBy: null, answers, createdAt: now(), messageId: null };
  const msg = await channel.send(withBanner(renderTicket(t)));
  t.messageId = msg.id;
  db.tickets[channel.id] = t;
  saveDb();

  // ping (osobna wiadomość, bo wiadomości V2 nie mają zwykłej treści) – usuwany po chwili
  const pings = [`<@${user.id}>`, ...config.staffRoleIds.filter((r) => guild.roles.cache.has(r)).map((r) => `<@&${r}>`)].join(' ');
  channel.send(pings).then((m) => setTimeout(() => m.delete().catch(() => {}), 3000)).catch(() => {});

  await interaction.editReply(notice(`Twój ticket został utworzony ➜ ${channel}`, true));

  await sendLog(
    guild,
    withBanner(
      shopContainer({
        title: 'Ticket otwarty',
        color: config.colorSuccess,
        banner: false,
        blocks: [[
          `> 👥 ${S} **Użytkownik:** ${user} \`(${user.id})\`\n` +
            `> ${category.emoji} ${S} **Kategoria:** \`${category.label}\`\n` +
            `> 🔢 ${S} **Numer:** \`#${number}\`\n` +
            `> 💬 ${S} **Kanał:** ${channel}`,
        ]],
      }),
      false,
    ),
  );
}

// ───────────── transkrypt ─────────────
function flattenComponents(comps, out = []) {
  for (const c of comps || []) {
    if (c.content) out.push(c.content);
    if (c.components) flattenComponents(c.components, out);
  }
  return out;
}
async function buildTranscript(channel) {
  let all = [];
  let before;
  while (all.length < 2000) {
    const batch = await channel.messages.fetch({ limit: 100, before });
    if (!batch.size) break;
    all = all.concat([...batch.values()]);
    before = batch.last().id;
    if (batch.size < 100) break;
  }
  all.reverse();
  const lines = [
    `╭─ ${config.shopName} ${S} Transkrypt ticketu`,
    `│ Kanał: #${channel.name}`,
    `│ Data:  ${new Date().toLocaleString('pl-PL')}`,
    `╰─ Wiadomości: ${all.length}`,
    '',
  ];
  for (const m of all) {
    let content = m.content || '';
    for (const e of m.embeds) content += `\n  [embed] ${e.title || ''} ${e.description || ''}`;
    const v2 = flattenComponents(m.components?.map((c) => c.toJSON?.() ?? c));
    if (v2.length) content += '\n  ' + v2.join('\n  ');
    for (const a of m.attachments.values()) content += `\n  [załącznik] ${a.url}`;
    lines.push(`[${m.createdAt.toLocaleString('pl-PL')}] ${m.author.tag}: ${content}`);
  }
  return Buffer.from(lines.join('\n'), 'utf8');
}
const transcriptName = (channel) => `transkrypt-${channel.id}.txt`;
const transcriptFile = (buf, channel) => new AttachmentBuilder(buf, { name: transcriptName(channel) });

// ───────────── zamykanie ─────────────
function closeConfirm() {
  const c = new ContainerBuilder()
    .setAccentColor(config.color)
    .addTextDisplayComponents(text(`> ❓ ${S} Czy na pewno chcesz **zamknąć** ten ticket?`))
    .addSeparatorComponents(sep())
    .addActionRowComponents(
      new ActionRowBuilder().addComponents(
        new ButtonBuilder().setCustomId('close_yes').setLabel('Tak, zamknij').setEmoji('🔒').setStyle(ButtonStyle.Danger),
        new ButtonBuilder().setCustomId('close_reason').setLabel('Z powodem').setEmoji('📝').setStyle(ButtonStyle.Primary),
        new ButtonBuilder().setCustomId('close_no').setLabel('Anuluj').setEmoji('✖️').setStyle(ButtonStyle.Secondary),
      ),
    );
  return { components: [c], flags: V2 };
}

const closing = new Set();
async function closeTicket(interaction, reason = 'Brak') {
  const channel = interaction.channel;
  const t = getTicket(channel.id);
  if (!t || closing.has(channel.id)) return;
  closing.add(channel.id);
  const guild = interaction.guild;
  const cat = getCategory(t.categoryId);

  await channel.send({
    components: [
      new ContainerBuilder()
        .setAccentColor(config.color)
        .addTextDisplayComponents(text(`> 🔒 ${S} Ticket zostanie zamknięty <t:${now() + 6}:R>\n> 📝 ${S} **Powód:** ${reason}`)),
    ],
    flags: V2,
  });

  const buf = await buildTranscript(channel);
  const summary =
    `> 👥 ${S} **Właściciel:** <@${t.ownerId}> \`(${t.ownerId})\`\n` +
    `> ${cat?.emoji || '🎫'} ${S} **Kategoria:** \`${cat?.label || t.categoryId}\`\n` +
    `> 🔢 ${S} **Numer:** \`#${t.number}\`\n` +
    `> ↪️ ${S} **Przejął:** ${t.claimedBy ? `<@${t.claimedBy}>` : '`Nikt`'}\n` +
    `> 🔒 ${S} **Zamknął:** ${interaction.user}\n` +
    `> 📝 ${S} **Powód:** ${reason}`;

  await sendLog(guild, {
    components: [shopContainer({ title: 'Ticket zamknięty', banner: false, blocks: [[summary]], file: transcriptName(channel) })],
    files: [transcriptFile(buf, channel)],
    flags: V2,
  });

  const owner = await client.users.fetch(t.ownerId).catch(() => null);
  if (owner) {
    const stars = new ActionRowBuilder().addComponents(
      [1, 2, 3, 4, 5].map((n) =>
        new ButtonBuilder()
          .setCustomId(`rate_${n}_${guild.id}_${t.number}`)
          .setLabel(String(n))
          .setEmoji('⭐')
          .setStyle(n >= 4 ? ButtonStyle.Success : n === 3 ? ButtonStyle.Primary : ButtonStyle.Secondary),
      ),
    );
    const dm = shopContainer({
      title: 'Ticket zamknięty',
      blocks: [
        [`> 🔒 ${S} Twój ticket **#${t.number}** na serwerze **${guild.name}** został zamknięty.\n> 📝 ${S} **Powód:** ${reason}\n> 🔒 ${S} **Zamknął:** ${interaction.user.username}`],
        [`> ⭐ ${S} **Oceń obsługę!** Kliknij ile gwiazdek nam dajesz.\n> 📄 ${S} Transkrypt rozmowy wysłaliśmy poniżej.`],
      ],
      rows: [stars],
    });
    await owner.send({ components: [dm], files: bannerFiles(), flags: V2 }).catch(() => {});
    await owner.send({ content: `📄 ${S} Transkrypt ticketu **#${t.number}**`, files: [transcriptFile(buf, channel)] }).catch(() => {});
  }

  delete db.tickets[channel.id];
  saveDb();
  setTimeout(() => {
    channel.delete().catch(() => {});
    closing.delete(channel.id);
  }, 6000);
}

// ───────────── komendy ─────────────
const commands = [
  new SlashCommandBuilder()
    .setName('panel')
    .setDescription('🎫 Wysyła panel ticketów na ten kanał')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),
  new SlashCommandBuilder()
    .setName('ticket')
    .setDescription('🎫 Zarządzanie ticketem')
    .addSubcommand((s) => s.setName('dodaj').setDescription('➕ Dodaj osobę do ticketu').addUserOption((o) => o.setName('osoba').setDescription('Kogo dodać').setRequired(true)))
    .addSubcommand((s) => s.setName('usun').setDescription('➖ Usuń osobę z ticketu').addUserOption((o) => o.setName('osoba').setDescription('Kogo usunąć').setRequired(true)))
    .addSubcommand((s) => s.setName('nazwa').setDescription('✏️ Zmień nazwę ticketu').addStringOption((o) => o.setName('nazwa').setDescription('Nowa nazwa').setRequired(true)))
    .addSubcommand((s) => s.setName('zamknij').setDescription('🔒 Zamknij ticket').addStringOption((o) => o.setName('powod').setDescription('Powód zamknięcia'))),
].map((c) => c.toJSON());

client.once('ready', async () => {
  console.log(`${config.brandEmoji} ${config.shopName} ${S} Zalogowano jako ${client.user.tag}`);
  client.user.setActivity(`🎫 ${S} ${config.shopName} tickety`);
  // usuń z bazy tickety, których kanały już nie istnieją
  for (const id of Object.keys(db.tickets)) if (!client.channels.cache.has(id)) delete db.tickets[id];
  saveDb();
  try {
    const rest = new REST({ version: '10' }).setToken(process.env.TOKEN);
    const appId = process.env.CLIENT_ID || client.user.id;
    const route = process.env.GUILD_ID ? Routes.applicationGuildCommands(appId, process.env.GUILD_ID) : Routes.applicationCommands(appId);
    await rest.put(route, { body: commands });
    console.log('Komendy zarejestrowane');
  } catch (e) {
    console.error('Błąd rejestracji komend:', e);
  }
});

// ───────────── akcje wspólne ─────────────
async function addUser(interaction, t, userId) {
  await interaction.channel.permissionOverwrites.edit(userId, { ViewChannel: true, SendMessages: true, ReadMessageHistory: true, AttachFiles: true });
  await interaction.channel.send({
    components: [new ContainerBuilder().setAccentColor(config.color).addTextDisplayComponents(text(`> ➕ ${S} <@${userId}> został dodany do ticketu przez ${interaction.user}`))],
    flags: V2,
  });
}
async function removeUser(interaction, t, userId) {
  if (userId === t.ownerId) return false;
  await interaction.channel.permissionOverwrites.delete(userId);
  await interaction.channel.send({
    components: [new ContainerBuilder().setAccentColor(config.color).addTextDisplayComponents(text(`> ➖ ${S} <@${userId}> został usunięty z ticketu przez ${interaction.user}`))],
    flags: V2,
  });
  return true;
}
function reasonModal() {
  return new ModalBuilder()
    .setCustomId('close_reason_modal')
    .setTitle('🔒 Zamknięcie ticketu')
    .addComponents(
      new ActionRowBuilder().addComponents(
        new TextInputBuilder().setCustomId('reason').setLabel('Powód zamknięcia').setStyle(TextInputStyle.Paragraph).setMaxLength(500).setRequired(true),
      ),
    );
}

// ───────────── interakcje ─────────────
client.on('interactionCreate', async (interaction) => {
  try {
    // /panel
    if (interaction.isChatInputCommand() && interaction.commandName === 'panel') {
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      await interaction.channel.send(buildPanel());
      return interaction.editReply(notice('Panel ticketów został wysłany!', true));
    }

    // /ticket ...
    if (interaction.isChatInputCommand() && interaction.commandName === 'ticket') {
      const t = getTicket(interaction.channelId);
      if (!t) return interaction.reply(notice('Ta komenda działa tylko w tickecie!'));
      const sub = interaction.options.getSubcommand();
      const staff = isStaff(interaction.member);

      if (sub === 'zamknij') {
        if (!staff && interaction.user.id !== t.ownerId) return interaction.reply(notice('Nie możesz zamknąć tego ticketu!'));
        await interaction.reply(notice('Zamykam ticket...', true));
        return closeTicket(interaction, interaction.options.getString('powod') || 'Brak');
      }
      if (!staff) return interaction.reply(notice('Tylko obsługa może tego użyć!'));
      if (sub === 'dodaj') {
        await addUser(interaction, t, interaction.options.getUser('osoba').id);
        return interaction.reply(notice('Dodano osobę.', true));
      }
      if (sub === 'usun') {
        const ok = await removeUser(interaction, t, interaction.options.getUser('osoba').id);
        return interaction.reply(ok ? notice('Usunięto osobę.', true) : notice('Nie możesz usunąć właściciela ticketu!'));
      }
      if (sub === 'nazwa') {
        const name = interaction.options.getString('nazwa');
        await interaction.channel.setName(name);
        return interaction.reply(notice(`Zmieniono nazwę na \`${name}\``, true));
      }
    }

    // wybór kategorii w panelu → formularz
    if (interaction.isStringSelectMenu() && interaction.customId === 'ticket_select') {
      const cat = getCategory(interaction.values[0]);
      if (!cat) return;
      const modal = new ModalBuilder().setCustomId(`ticket_modal_${cat.id}`).setTitle(`${cat.emoji} ${cat.label} ${S} ${config.shopName}`.slice(0, 45));
      for (const q of cat.questions.slice(0, 5)) {
        modal.addComponents(
          new ActionRowBuilder().addComponents(
            new TextInputBuilder()
              .setCustomId(q.id)
              .setLabel(q.label)
              .setPlaceholder(q.placeholder || '')
              .setStyle(q.style === 'paragraph' ? TextInputStyle.Paragraph : TextInputStyle.Short)
              .setRequired(q.required !== false)
              .setMaxLength(q.style === 'paragraph' ? 1000 : 100),
          ),
        );
      }
      await interaction.showModal(modal);
      // reset menu, żeby można było wybrać tę samą kategorię ponownie
      interaction.message.edit({ ...buildPanel(), attachments: [] }).catch(() => {});
      return;
    }

    // formularz wysłany → ticket
    if (interaction.isModalSubmit() && interaction.customId.startsWith('ticket_modal_')) {
      const cat = getCategory(interaction.customId.replace('ticket_modal_', ''));
      if (!cat) return;
      await interaction.deferReply({ flags: EPH });
      const answers = {};
      for (const q of cat.questions) answers[q.id] = interaction.fields.getTextInputValue(q.id);
      return createTicket(interaction, cat, answers);
    }

    // menu akcji w tickecie
    if (interaction.isStringSelectMenu() && interaction.customId === 'ticket_action') {
      const t = getTicket(interaction.channelId);
      if (!t) return interaction.reply(notice('Ten ticket nie istnieje w bazie.'));
      const action = interaction.values[0];
      if (action !== 'claim') setTimeout(() => refreshTicketMessage(interaction.channel, t), 1500);
      const staff = isStaff(interaction.member);
      const owner = interaction.user.id === t.ownerId;

      if (action === 'close_reason') {
        if (!staff && !owner) return interaction.reply(notice('Nie możesz zamknąć tego ticketu!'));
        return interaction.showModal(reasonModal());
      }
      if (action === 'close') {
        if (!staff && !owner) return interaction.reply(notice('Nie możesz zamknąć tego ticketu!'));
        return interaction.reply(closeConfirm());
      }
      if (!staff) return interaction.reply(notice('Tylko obsługa może użyć tej akcji!'));

      if (action === 'claim') {
        if (t.claimedBy) return interaction.reply(notice(`Ticket jest już przejęty przez <@${t.claimedBy}>`));
        t.claimedBy = interaction.user.id;
        saveDb();
        await interaction.update({ components: [renderTicket(t)], files: bannerFiles(), attachments: [], flags: V2 });
        return interaction.channel.send({
          components: [new ContainerBuilder().setAccentColor(config.color).addTextDisplayComponents(text(`> 🙋 ${S} ${interaction.user} przejął ten ticket i zaraz Ci pomoże!`))],
          flags: V2,
        });
      }
      if (action === 'remind') {
        await interaction.reply(notice('Wysłano przypomnienie.', true));
        await interaction.channel.send({
          components: [new ContainerBuilder().setAccentColor(config.color).addTextDisplayComponents(text(`> 🔔 ${S} <@${t.ownerId}>, obsługa czeka na Twoją odpowiedź!`))],
          flags: V2,
          allowedMentions: { users: [t.ownerId] },
        });
        const u = await client.users.fetch(t.ownerId).catch(() => null);
        if (u)
          u.send({
            components: [new ContainerBuilder().setAccentColor(config.color).addTextDisplayComponents(text(`> 🔔 ${S} Masz nieodczytaną wiadomość w tickecie **#${t.number}** ➜ ${interaction.channel}`))],
            flags: V2,
          }).catch(() => {});
        return;
      }
      if (action === 'add' || action === 'remove') {
        const row = new ActionRowBuilder().addComponents(
          new UserSelectMenuBuilder().setCustomId(`pick_${action}`).setPlaceholder(action === 'add' ? `➕ ${S} Kogo dodać?` : `➖ ${S} Kogo usunąć?`),
        );
        return interaction.reply({
          components: [new ContainerBuilder().setAccentColor(config.color).addTextDisplayComponents(text(`> 👥 ${S} Wybierz osobę:`)).addActionRowComponents(row)],
          flags: EPH,
        });
      }
      if (action === 'transcript') {
        await interaction.deferReply({ flags: EPH });
        const buf = await buildTranscript(interaction.channel);
        return interaction.editReply({
          components: [
            new ContainerBuilder()
              .setAccentColor(config.color)
              .addTextDisplayComponents(text(`> 📄 ${S} Oto transkrypt ticketu **#${t.number}**`))
              .addFileComponents(new FileBuilder().setURL(`attachment://${transcriptName(interaction.channel)}`)),
          ],
          files: [transcriptFile(buf, interaction.channel)],
          flags: EPH,
        });
      }
    }

    // wybór osoby (dodaj/usuń)
    if (interaction.isUserSelectMenu() && interaction.customId.startsWith('pick_')) {
      const t = getTicket(interaction.channelId);
      if (!t || !isStaff(interaction.member)) return;
      const userId = interaction.values[0];
      if (interaction.customId === 'pick_add') {
        await addUser(interaction, t, userId);
        return interaction.update(notice(`Dodano <@${userId}>`, true));
      }
      const ok = await removeUser(interaction, t, userId);
      return interaction.update(ok ? notice(`Usunięto <@${userId}>`, true) : notice('Nie możesz usunąć właściciela ticketu!'));
    }

    // przyciski
    if (interaction.isButton()) {
      const id = interaction.customId;

      if (id.startsWith('rate_')) {
        const [, stars, guildId, number] = id.split('_');
        const n = Number(stars);
        await interaction.update({
          components: [
            new ContainerBuilder()
              .setAccentColor(config.color)
              .addTextDisplayComponents(text('```\n' + `${config.brandEmoji} ${S} ${config.brand}  ✖\n${fancy('Dziękujemy')}` + '\n```'))
              .addSeparatorComponents(sep())
              .addTextDisplayComponents(text(`> 💖 ${S} Twoja ocena: ${'⭐'.repeat(n)} \`(${n}/5)\`\n> 🛒 ${S} Do zobaczenia w **${config.shopName}**!`)),
          ],
          attachments: [],
          flags: V2,
        });
        const guild = client.guilds.cache.get(guildId);
        if (guild)
          await sendLog(guild, {
            components: [
              shopContainer({
                title: 'Nowa ocena',
                banner: false,
                blocks: [[`> 👥 ${S} **Użytkownik:** ${interaction.user} \`(${interaction.user.id})\`\n> 🔢 ${S} **Ticket:** \`#${number}\`\n> ⭐ ${S} **Ocena:** ${'⭐'.repeat(n)} \`(${n}/5)\``]],
              }),
            ],
            flags: V2,
          });
        return;
      }

      const t = getTicket(interaction.channelId);
      if (!t) return;
      const can = isStaff(interaction.member) || interaction.user.id === t.ownerId;

      if (id === 'close_no') return interaction.message.delete().catch(() => {});
      if (!can) return interaction.reply(notice('Nie możesz zamknąć tego ticketu!'));
      if (id === 'close_reason') return interaction.showModal(reasonModal());
      if (id === 'close_yes') {
        await interaction.update({ components: [new ContainerBuilder().setAccentColor(config.color).addTextDisplayComponents(text(`> 🔒 ${S} Zamykanie potwierdzone przez ${interaction.user}`))], flags: V2 });
        return closeTicket(interaction, 'Brak');
      }
    }

    // powód zamknięcia
    if (interaction.isModalSubmit() && interaction.customId === 'close_reason_modal') {
      if (!getTicket(interaction.channelId)) return;
      await interaction.reply(notice('Zamykam ticket...', true));
      return closeTicket(interaction, interaction.fields.getTextInputValue('reason'));
    }
  } catch (err) {
    console.error(err);
    const payload = notice('Wystąpił błąd. Spróbuj ponownie.');
    if (interaction.deferred || interaction.replied) interaction.followUp(payload).catch(() => {});
    else interaction.reply?.(payload).catch(() => {});
  }
});

client.login(process.env.TOKEN);
