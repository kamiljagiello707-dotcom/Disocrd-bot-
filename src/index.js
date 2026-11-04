require('dotenv').config();
const fs = require('fs');
const path = require('path');
const {
  Client,
  GatewayIntentBits,
  Partials,
  EmbedBuilder,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  StringSelectMenuBuilder,
  ModalBuilder,
  TextInputBuilder,
  TextInputStyle,
  ChannelType,
  PermissionFlagsBits,
  SlashCommandBuilder,
  REST,
  Routes,
  AttachmentBuilder,
} = require('discord.js');
const config = require('./config');

const client = new Client({
  intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.GuildMembers],
  partials: [Partials.Channel],
});

// ─────────────── licznik ticketów ───────────────
const DATA_DIR = path.join(__dirname, '..', 'data');
const COUNTER_FILE = path.join(DATA_DIR, 'counter.json');
function nextTicketNumber() {
  if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });
  let n = 0;
  try { n = JSON.parse(fs.readFileSync(COUNTER_FILE, 'utf8')).count || 0; } catch {}
  n += 1;
  fs.writeFileSync(COUNTER_FILE, JSON.stringify({ count: n }));
  return String(n).padStart(4, '0');
}

// ─────────────── pomocnicze ───────────────
const LINE = '━━━━━━━━━━━━━━━━━━━━━━━━━━';
const footer = () => ({ text: `${config.shopName} ✦ System ticketów`, iconURL: client.user.displayAvatarURL() });
const getCategory = (id) => config.categories.find((c) => c.id === id);
const isStaff = (member) =>
  member.permissions.has(PermissionFlagsBits.Administrator) ||
  config.staffRoleIds.some((r) => member.roles.cache.has(r));

// Dane ticketu trzymamy w temacie kanału: ticket|ownerId|kategoria|numer|claimedById
function parseTopic(channel) {
  if (!channel?.topic?.startsWith('ticket|')) return null;
  const [, ownerId, categoryId, number, claimedBy] = channel.topic.split('|');
  return { ownerId, categoryId, number, claimedBy: claimedBy || null };
}
const buildTopic = (t) => `ticket|${t.ownerId}|${t.categoryId}|${t.number}|${t.claimedBy || ''}`;

async function sendLog(guild, embed, files = []) {
  const ch = guild.channels.cache.get(config.logChannelId);
  if (ch) await ch.send({ embeds: [embed], files }).catch(() => {});
}

function errorEmbed(text) {
  return new EmbedBuilder().setColor(config.colorDanger).setDescription(`❌ ┃ ${text}`);
}
function okEmbed(text) {
  return new EmbedBuilder().setColor(config.colorSuccess).setDescription(`✅ ┃ ${text}`);
}

// ─────────────── panel ───────────────
function buildPanel() {
  const list = config.categories
    .map((c) => `> ${c.emoji} **${c.label}** ➜ ${c.description}`)
    .join('\n');

  const embed = new EmbedBuilder()
    .setColor(config.color)
    .setAuthor({ name: `${config.shopName} ✦ Tickety`, iconURL: client.user.displayAvatarURL() })
    .setTitle('🎫 ┃ Stwórz ticket')
    .setDescription(
      [
        `✦ Witaj w **${config.shopName}**!`,
        'Potrzebujesz pomocy lub chcesz złożyć zamówienie?',
        'Wybierz odpowiednią kategorię z menu poniżej. 👇',
        '',
        LINE,
        '📂 **Dostępne kategorie:**',
        list,
        LINE,
        '',
        '📌 **Zasady:**',
        '> ・ Nie twórz ticketów bez powodu',
        '> ・ Nie oznaczaj administracji, odpiszemy najszybciej jak się da',
        '> ・ Bądź kulturalny wobec obsługi',
        '',
        '⏰ **Czas odpowiedzi:** zazwyczaj do `15 minut`',
      ].join('\n'),
    )
    .setFooter(footer())
    .setTimestamp();
  if (config.bannerUrl) embed.setImage(config.bannerUrl);

  const menu = new StringSelectMenuBuilder()
    .setCustomId('ticket_select')
    .setPlaceholder('📩 Wybierz kategorię ticketu...')
    .addOptions(
      config.categories.map((c) => ({ label: c.label, value: c.id, description: c.description, emoji: c.emoji })),
    );

  return { embeds: [embed], components: [new ActionRowBuilder().addComponents(menu)] };
}

function ticketButtons(claimed = false) {
  return new ActionRowBuilder().addComponents(
    new ButtonBuilder().setCustomId('ticket_close').setLabel('Zamknij').setEmoji('🔒').setStyle(ButtonStyle.Danger),
    new ButtonBuilder()
      .setCustomId('ticket_claim')
      .setLabel(claimed ? 'Przejęty' : 'Przejmij')
      .setEmoji('🙋')
      .setStyle(ButtonStyle.Success)
      .setDisabled(claimed),
    new ButtonBuilder().setCustomId('ticket_transcript').setLabel('Transkrypt').setEmoji('📄').setStyle(ButtonStyle.Secondary),
  );
}

// ─────────────── tworzenie ticketu ───────────────
async function createTicket(interaction, category, answers) {
  const guild = interaction.guild;
  const user = interaction.user;

  const open = guild.channels.cache.filter((ch) => parseTopic(ch)?.ownerId === user.id);
  if (open.size >= config.maxTicketsPerUser) {
    return interaction.editReply({ embeds: [errorEmbed(`Masz już otwarty ticket: ${open.first()}`)] });
  }

  const number = nextTicketNumber();
  const safeName = user.username.toLowerCase().replace(/[^a-z0-9]/g, '').slice(0, 20) || 'user';

  const overwrites = [
    { id: guild.roles.everyone.id, deny: [PermissionFlagsBits.ViewChannel] },
    {
      id: user.id,
      allow: [
        PermissionFlagsBits.ViewChannel,
        PermissionFlagsBits.SendMessages,
        PermissionFlagsBits.ReadMessageHistory,
        PermissionFlagsBits.AttachFiles,
        PermissionFlagsBits.EmbedLinks,
      ],
    },
    {
      id: client.user.id,
      allow: [
        PermissionFlagsBits.ViewChannel,
        PermissionFlagsBits.SendMessages,
        PermissionFlagsBits.ManageChannels,
        PermissionFlagsBits.ReadMessageHistory,
      ],
    },
    ...config.staffRoleIds
      .filter((r) => guild.roles.cache.has(r))
      .map((r) => ({
        id: r,
        allow: [
          PermissionFlagsBits.ViewChannel,
          PermissionFlagsBits.SendMessages,
          PermissionFlagsBits.ReadMessageHistory,
          PermissionFlagsBits.AttachFiles,
        ],
      })),
  ];

  const channel = await guild.channels.create({
    name: `${category.emoji}・${category.id}-${safeName}`,
    type: ChannelType.GuildText,
    parent: guild.channels.cache.has(config.ticketCategoryId) ? config.ticketCategoryId : null,
    topic: buildTopic({ ownerId: user.id, categoryId: category.id, number }),
    permissionOverwrites: overwrites,
  });

  const fields = category.questions.map((q) => ({
    name: `➜ ${q.label}`,
    value: '```' + (answers[q.id]?.trim() || 'Brak') + '```',
  }));

  const embed = new EmbedBuilder()
    .setColor(config.color)
    .setAuthor({ name: `${user.username} ✦ Ticket #${number}`, iconURL: user.displayAvatarURL() })
    .setTitle(`${category.emoji} ┃ ${category.label}`)
    .setDescription(
      [
        `👋 Cześć ${user}!`,
        `Dziękujemy za kontakt z **${config.shopName}**.`,
        'Obsługa odpowie najszybciej jak to możliwe. ⏳',
        '',
        LINE,
        `👤 **Użytkownik:** ${user} \`(${user.id})\``,
        `📂 **Kategoria:** \`${category.label}\``,
        `🔢 **Numer:** \`#${number}\``,
        `🙋 **Przejęty przez:** \`Nikt\``,
        LINE,
      ].join('\n'),
    )
    .addFields(fields)
    .setFooter(footer())
    .setTimestamp();

  const staffPing = config.staffRoleIds.filter((r) => guild.roles.cache.has(r)).map((r) => `<@&${r}>`).join(' ');
  await channel.send({ content: `${user} ${staffPing}`.trim(), embeds: [embed], components: [ticketButtons()] });

  await interaction.editReply({ embeds: [okEmbed(`Twój ticket został utworzony ➜ ${channel}`)] });

  await sendLog(
    guild,
    new EmbedBuilder()
      .setColor(config.colorSuccess)
      .setTitle('📥 ┃ Ticket otwarty')
      .setDescription(
        `> 👤 **Użytkownik:** ${user} \`(${user.id})\`\n> 📂 **Kategoria:** \`${category.label}\`\n> 🔢 **Numer:** \`#${number}\`\n> 💬 **Kanał:** ${channel}`,
      )
      .setFooter(footer())
      .setTimestamp(),
  );
}

// ─────────────── transkrypt ───────────────
async function buildTranscript(channel) {
  let all = [];
  let lastId;
  while (true) {
    const batch = await channel.messages.fetch({ limit: 100, before: lastId });
    if (!batch.size) break;
    all = all.concat([...batch.values()]);
    lastId = batch.last().id;
    if (batch.size < 100 || all.length >= 2000) break;
  }
  all.reverse();

  const lines = [
    `╭─ ${config.shopName} ✦ Transkrypt ticketu`,
    `│ Kanał: #${channel.name}`,
    `│ Data:  ${new Date().toLocaleString('pl-PL')}`,
    `╰─ Wiadomości: ${all.length}`,
    '',
  ];
  for (const m of all) {
    const time = m.createdAt.toLocaleString('pl-PL');
    let content = m.content || '';
    for (const e of m.embeds) {
      content += `\n  [embed] ${e.title || ''} ${e.description || ''}`;
      for (const f of e.fields || []) content += `\n    ${f.name}: ${f.value}`;
    }
    for (const a of m.attachments.values()) content += `\n  [załącznik] ${a.url}`;
    lines.push(`[${time}] ${m.author.tag}: ${content}`);
  }
  return new AttachmentBuilder(Buffer.from(lines.join('\n'), 'utf8'), { name: `transkrypt-${channel.name}.txt` });
}

// ─────────────── zamykanie ───────────────
async function closeTicket(interaction, reason = 'Brak') {
  const channel = interaction.channel;
  const t = parseTopic(channel);
  const guild = interaction.guild;
  const category = getCategory(t.categoryId);

  await interaction.editReply({
    embeds: [
      new EmbedBuilder()
        .setColor(config.colorDanger)
        .setDescription(`🔒 ┃ Ticket zostanie zamknięty za \`5 sekund\`...\n> 📝 **Powód:** ${reason}`),
    ],
    components: [],
  });

  const transcript = await buildTranscript(channel);
  const logEmbed = new EmbedBuilder()
    .setColor(config.colorDanger)
    .setTitle('📤 ┃ Ticket zamknięty')
    .setDescription(
      [
        `> 👤 **Właściciel:** <@${t.ownerId}> \`(${t.ownerId})\``,
        `> 📂 **Kategoria:** \`${category?.label || t.categoryId}\``,
        `> 🔢 **Numer:** \`#${t.number}\``,
        `> 🙋 **Przejęty przez:** ${t.claimedBy ? `<@${t.claimedBy}>` : '`Nikt`'}`,
        `> 🔒 **Zamknięty przez:** ${interaction.user}`,
        `> 📝 **Powód:** ${reason}`,
      ].join('\n'),
    )
    .setFooter(footer())
    .setTimestamp();
  await sendLog(guild, logEmbed, [transcript]);

  // DM do właściciela + prośba o ocenę
  const owner = await client.users.fetch(t.ownerId).catch(() => null);
  if (owner) {
    const dmEmbed = new EmbedBuilder()
      .setColor(config.color)
      .setAuthor({ name: `${config.shopName} ✦ Ticket zamknięty`, iconURL: client.user.displayAvatarURL() })
      .setDescription(
        [
          `🔒 Twój ticket **#${t.number}** na serwerze **${guild.name}** został zamknięty.`,
          '',
          `> 📝 **Powód:** ${reason}`,
          `> 🔒 **Zamknął:** ${interaction.user.username}`,
          '',
          LINE,
          '⭐ **Oceń obsługę!** Kliknij ile gwiazdek nam dajesz:',
        ].join('\n'),
      )
      .setFooter(footer())
      .setTimestamp();
    const stars = new ActionRowBuilder().addComponents(
      [1, 2, 3, 4, 5].map((n) =>
        new ButtonBuilder()
          .setCustomId(`rate_${n}_${guild.id}_${t.number}`)
          .setLabel('⭐'.repeat(n))
          .setStyle(n >= 4 ? ButtonStyle.Success : n === 3 ? ButtonStyle.Primary : ButtonStyle.Secondary),
      ),
    );
    const dmTranscript = await buildTranscript(channel);
    await owner.send({ embeds: [dmEmbed], components: [stars], files: [dmTranscript] }).catch(() => {});
  }

  setTimeout(() => channel.delete().catch(() => {}), 5000);
}

// ─────────────── komendy ───────────────
const commands = [
  new SlashCommandBuilder()
    .setName('panel')
    .setDescription('🎫 Wysyła panel ticketów na ten kanał')
    .setDefaultMemberPermissions(PermissionFlagsBits.Administrator),
  new SlashCommandBuilder()
    .setName('ticket')
    .setDescription('🎫 Zarządzanie ticketem')
    .addSubcommand((s) =>
      s.setName('dodaj').setDescription('➕ Dodaj osobę do ticketu').addUserOption((o) => o.setName('osoba').setDescription('Kogo dodać').setRequired(true)),
    )
    .addSubcommand((s) =>
      s.setName('usun').setDescription('➖ Usuń osobę z ticketu').addUserOption((o) => o.setName('osoba').setDescription('Kogo usunąć').setRequired(true)),
    )
    .addSubcommand((s) =>
      s.setName('nazwa').setDescription('✏️ Zmień nazwę ticketu').addStringOption((o) => o.setName('nazwa').setDescription('Nowa nazwa').setRequired(true)),
    )
    .addSubcommand((s) =>
      s.setName('zamknij').setDescription('🔒 Zamknij ticket').addStringOption((o) => o.setName('powod').setDescription('Powód zamknięcia')),
    ),
].map((c) => c.toJSON());

client.once('ready', async () => {
  console.log(`✦ ${config.shopName} ┃ Zalogowano jako ${client.user.tag}`);
  client.user.setActivity(`🎫 ${config.shopName} ┃ Tickety`);
  try {
    const rest = new REST({ version: '10' }).setToken(process.env.TOKEN);
    const route = process.env.GUILD_ID
      ? Routes.applicationGuildCommands(process.env.CLIENT_ID || client.user.id, process.env.GUILD_ID)
      : Routes.applicationCommands(process.env.CLIENT_ID || client.user.id);
    await rest.put(route, { body: commands });
    console.log('✦ Komendy zarejestrowane');
  } catch (e) {
    console.error('Błąd rejestracji komend:', e);
  }
});

// ─────────────── obsługa interakcji ───────────────
client.on('interactionCreate', async (interaction) => {
  try {
    // ── /panel ──
    if (interaction.isChatInputCommand() && interaction.commandName === 'panel') {
      await interaction.channel.send(buildPanel());
      return interaction.reply({ embeds: [okEmbed('Panel ticketów został wysłany!')], ephemeral: true });
    }

    // ── /ticket ──
    if (interaction.isChatInputCommand() && interaction.commandName === 'ticket') {
      const t = parseTopic(interaction.channel);
      if (!t) return interaction.reply({ embeds: [errorEmbed('Ta komenda działa tylko w tickecie!')], ephemeral: true });
      const sub = interaction.options.getSubcommand();
      const staff = isStaff(interaction.member);

      if (sub === 'zamknij') {
        if (!staff && interaction.user.id !== t.ownerId)
          return interaction.reply({ embeds: [errorEmbed('Nie możesz zamknąć tego ticketu!')], ephemeral: true });
        await interaction.deferReply();
        return closeTicket(interaction, interaction.options.getString('powod') || 'Brak');
      }

      if (!staff) return interaction.reply({ embeds: [errorEmbed('Tylko obsługa może tego użyć!')], ephemeral: true });

      if (sub === 'dodaj') {
        const u = interaction.options.getUser('osoba');
        await interaction.channel.permissionOverwrites.edit(u.id, {
          ViewChannel: true,
          SendMessages: true,
          ReadMessageHistory: true,
          AttachFiles: true,
        });
        return interaction.reply({ embeds: [okEmbed(`Dodano ${u} do ticketu ➕`)] });
      }
      if (sub === 'usun') {
        const u = interaction.options.getUser('osoba');
        if (u.id === t.ownerId) return interaction.reply({ embeds: [errorEmbed('Nie możesz usunąć właściciela ticketu!')], ephemeral: true });
        await interaction.channel.permissionOverwrites.delete(u.id);
        return interaction.reply({ embeds: [okEmbed(`Usunięto ${u} z ticketu ➖`)] });
      }
      if (sub === 'nazwa') {
        const name = interaction.options.getString('nazwa');
        await interaction.channel.setName(name);
        return interaction.reply({ embeds: [okEmbed(`Zmieniono nazwę ticketu na \`${name}\` ✏️`)] });
      }
    }

    // ── wybór kategorii z menu ──
    if (interaction.isStringSelectMenu() && interaction.customId === 'ticket_select') {
      const category = getCategory(interaction.values[0]);
      if (!category) return;

      // resetuje wybór w menu, żeby można było wybrać ponownie tę samą opcję
      interaction.message.edit(buildPanel()).catch(() => {});

      const modal = new ModalBuilder().setCustomId(`ticket_modal_${category.id}`).setTitle(`${category.emoji} ${category.label} ┃ ${config.shopName}`.slice(0, 45));
      for (const q of category.questions.slice(0, 5)) {
        modal.addComponents(
          new ActionRowBuilder().addComponents(
            new TextInputBuilder()
              .setCustomId(q.id)
              .setLabel(q.label)
              .setPlaceholder(q.placeholder || '')
              .setStyle(q.style === 'paragraph' ? TextInputStyle.Paragraph : TextInputStyle.Short)
              .setRequired(q.required !== false)
              .setMaxLength(q.style === 'paragraph' ? 1000 : 200),
          ),
        );
      }
      return interaction.showModal(modal);
    }

    // ── wysłanie formularza ──
    if (interaction.isModalSubmit() && interaction.customId.startsWith('ticket_modal_')) {
      const category = getCategory(interaction.customId.replace('ticket_modal_', ''));
      if (!category) return;
      await interaction.deferReply({ ephemeral: true });
      const answers = {};
      for (const q of category.questions) answers[q.id] = interaction.fields.getTextInputValue(q.id);
      return createTicket(interaction, category, answers);
    }

    // ── przyciski w tickecie ──
    if (interaction.isButton()) {
      const id = interaction.customId;

      // oceny w DM
      if (id.startsWith('rate_')) {
        const [, stars, guildId, number] = id.split('_');
        const guild = client.guilds.cache.get(guildId);
        await interaction.update({
          components: [],
          embeds: [
            EmbedBuilder.from(interaction.message.embeds[0]).setDescription(
              `${interaction.message.embeds[0].description.split(LINE)[0]}${LINE}\n💜 Dziękujemy za ocenę: ${'⭐'.repeat(Number(stars))}`,
            ),
          ],
        });
        if (guild)
          await sendLog(
            guild,
            new EmbedBuilder()
              .setColor(config.color)
              .setTitle('⭐ ┃ Nowa ocena')
              .setDescription(`> 👤 **Użytkownik:** ${interaction.user} \`(${interaction.user.id})\`\n> 🔢 **Ticket:** \`#${number}\`\n> ⭐ **Ocena:** ${'⭐'.repeat(Number(stars))} \`(${stars}/5)\``)
              .setFooter(footer())
              .setTimestamp(),
          );
        return;
      }

      const t = parseTopic(interaction.channel);
      if (!t) return;
      const staff = isStaff(interaction.member);

      if (id === 'ticket_close') {
        if (!staff && interaction.user.id !== t.ownerId)
          return interaction.reply({ embeds: [errorEmbed('Nie możesz zamknąć tego ticketu!')], ephemeral: true });
        const row = new ActionRowBuilder().addComponents(
          new ButtonBuilder().setCustomId('ticket_close_confirm').setLabel('Tak, zamknij').setEmoji('✅').setStyle(ButtonStyle.Danger),
          new ButtonBuilder().setCustomId('ticket_close_reason').setLabel('Zamknij z powodem').setEmoji('📝').setStyle(ButtonStyle.Primary),
          new ButtonBuilder().setCustomId('ticket_close_cancel').setLabel('Anuluj').setEmoji('✖️').setStyle(ButtonStyle.Secondary),
        );
        return interaction.reply({
          embeds: [new EmbedBuilder().setColor(config.colorDanger).setDescription('❓ ┃ Czy na pewno chcesz **zamknąć** ten ticket?')],
          components: [row],
        });
      }

      if (id === 'ticket_close_cancel') {
        return interaction.update({ embeds: [okEmbed('Zamykanie anulowane.')], components: [] }).then(() =>
          setTimeout(() => interaction.deleteReply().catch(() => {}), 3000),
        );
      }

      if (id === 'ticket_close_confirm') {
        if (!staff && interaction.user.id !== t.ownerId)
          return interaction.reply({ embeds: [errorEmbed('Nie możesz zamknąć tego ticketu!')], ephemeral: true });
        await interaction.deferUpdate();
        return closeTicket(interaction, 'Brak');
      }

      if (id === 'ticket_close_reason') {
        if (!staff && interaction.user.id !== t.ownerId)
          return interaction.reply({ embeds: [errorEmbed('Nie możesz zamknąć tego ticketu!')], ephemeral: true });
        const modal = new ModalBuilder()
          .setCustomId('ticket_reason_modal')
          .setTitle('🔒 Zamknięcie ticketu')
          .addComponents(
            new ActionRowBuilder().addComponents(
              new TextInputBuilder().setCustomId('reason').setLabel('Powód zamknięcia').setStyle(TextInputStyle.Paragraph).setMaxLength(500).setRequired(true),
            ),
          );
        return interaction.showModal(modal);
      }

      if (id === 'ticket_claim') {
        if (!staff) return interaction.reply({ embeds: [errorEmbed('Tylko obsługa może przejąć ticket!')], ephemeral: true });
        if (t.claimedBy) return interaction.reply({ embeds: [errorEmbed(`Ticket jest już przejęty przez <@${t.claimedBy}>`)], ephemeral: true });
        t.claimedBy = interaction.user.id;
        await interaction.channel.setTopic(buildTopic(t));
        const old = interaction.message.embeds[0];
        const embed = EmbedBuilder.from(old).setDescription(
          old.description.replace(/🙋 \*\*Przejęty przez:\*\* .*/, `🙋 **Przejęty przez:** ${interaction.user}`),
        );
        await interaction.update({ embeds: [embed], components: [ticketButtons(true)] });
        return interaction.followUp({
          embeds: [new EmbedBuilder().setColor(config.color).setDescription(`🙋 ┃ ${interaction.user} przejął ten ticket i zaraz Ci pomoże!`)],
        });
      }

      if (id === 'ticket_transcript') {
        if (!staff) return interaction.reply({ embeds: [errorEmbed('Tylko obsługa może pobrać transkrypt!')], ephemeral: true });
        await interaction.deferReply({ ephemeral: true });
        const file = await buildTranscript(interaction.channel);
        return interaction.editReply({ embeds: [okEmbed('Oto transkrypt ticketu 📄')], files: [file] });
      }
    }

    // ── modal z powodem zamknięcia ──
    if (interaction.isModalSubmit() && interaction.customId === 'ticket_reason_modal') {
      if (!parseTopic(interaction.channel)) return;
      await interaction.deferUpdate();
      return closeTicket(interaction, interaction.fields.getTextInputValue('reason'));
    }
  } catch (err) {
    console.error(err);
    const payload = { embeds: [errorEmbed('Wystąpił błąd. Spróbuj ponownie.')], ephemeral: true };
    if (interaction.deferred || interaction.replied) interaction.followUp(payload).catch(() => {});
    else interaction.reply?.(payload).catch(() => {});
  }
});

client.login(process.env.TOKEN);
