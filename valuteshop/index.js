console.log('▶ Start bota ValuteShop...');
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
  StringSelectMenuBuilder,
  SlashCommandBuilder,
  PermissionFlagsBits,
  REST,
  Routes,
  ContainerBuilder,
  TextDisplayBuilder,
  SeparatorBuilder,
  SeparatorSpacingSize,
  MessageFlags,
  Options,
} = require('discord.js');

// ╭──────────────────────────────────────────────╮
// │          ValuteShop ︲ konfiguracja            │
// ╰──────────────────────────────────────────────╯
const config = {
  shopName: 'ValuteShop',
  sep: '︲',
  // Kolor paska kontenera (null = brak paska)
  color: 0x2b2d31,

  // Emotki serwerowe (bot musi być na serwerze, z którego pochodzą)
  emojis: {
    opis: '<:emoji_2:1575993715942498486>',
    wydales: { id: '1575994767257636974', name: 'emoji_24' },
    historia: { id: '1575994805652295703', name: 'emoji_25' },
    statystyki: '<:emoji_4:1575993758376267896>',
    historiaNaglowek: '<:emoji_24:1575994767257636974>',
    zakup: '<:emoji_23:1575994740070031454>',
  },

  // Role sprzedawców, które mogą dodawać zakupy (/zakup). Administratorzy mogą zawsze.
  // Np. ['123456789012345678']
  sellerRoleIds: [],

  // Token bota. Najlepiej w pliku .env (TOKEN=...), albo wklej go tutaj między ''.
  token: '',
};

const S = config.sep;
const E = config.emojis;
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
const clean = (s) => String(s ?? '').replace(/`/g, 'ˋ').trim();
const pad = (n) => String(n).padStart(4, '0');
const zl = (n) => new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(n);

function newContainer() {
  const c = new ContainerBuilder();
  if (config.color != null) c.setAccentColor(config.color);
  return c;
}
function notice(msg, ok = false) {
  return { components: [newContainer().addTextDisplayComponents(text(`${ok ? '✅' : '❌'} ${S} ${msg}`))], flags: EPH };
}
const isSeller = (member) =>
  member.permissions.has(PermissionFlagsBits.Administrator) || config.sellerRoleIds.some((r) => member.roles.cache.has(r));

// ───────────── zapis zakupów (plik zakupy.json obok index.js) ─────────────
const DB_FILE = path.join(__dirname, 'zakupy.json');
function loadDb() {
  try {
    return JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
  } catch {
    return { counter: 0, purchases: [] };
  }
}
const db = loadDb();
function saveDb() {
  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2));
  } catch (e) {
    console.error('❌ Nie udało się zapisać zakupy.json:', e.message);
  }
}
const purchasesOf = (userId, guildId) => db.purchases.filter((p) => p.clientId === userId && p.guildId === guildId);

// ───────────── PANEL KLIENTA ─────────────
//  Nagłówek → separator → opis → select menu
function buildPanel() {
  const c = newContainer();
  c.addTextDisplayComponents(text(`\`\`\`\n📁︲Panel Klienta × ${config.shopName}\n\`\`\``));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(`> ${E.opis}${S}Hejka, jeśli chcesz skorzystać z panelu Klienta, wybierz __opcję__ poniżej!`),
  );
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(
      new StringSelectMenuBuilder()
        .setCustomId('vs_panel')
        .setPlaceholder(`🌐${S}Wybierz Sekcję`)
        .addOptions(
          { label: `${S}Sprawdź ile wydałeś`, value: 'wydane', emoji: E.wydales },
          { label: `${S}Sprawdź swoją Historię`, value: 'historia', emoji: E.historia },
        ),
    ),
  );
  return { components: [c], flags: V2 };
}

// „Sprawdź ile wydałeś”
function buildSpent(userId, guildId) {
  const mine = purchasesOf(userId, guildId);
  const total = mine.reduce((sum, p) => sum + p.amount, 0);
  const c = newContainer();
  c.addTextDisplayComponents(text(`\`\`\`\n📕︲Ile wydałeś × ${config.shopName}\n\`\`\``));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`${E.statystyki}${S}**__Twoje Statystyki__**`));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(
    text(
      mine.length
        ? `> ${E.opis}${S}W naszym sklepie wydałeś okrągłe \`${zl(total)}\`\n` +
            `> ${E.zakup}${S}Liczba zakupów: \`${mine.length}\``
        : `> ${E.opis}${S}Nie masz jeszcze żadnych zakupów w naszym sklepie.`,
    ),
  );
  return { components: [c], flags: EPH };
}

// „Sprawdź swoją Historię”
const HISTORY_LIMIT = 10;
function buildHistory(userId, guildId) {
  const mine = purchasesOf(userId, guildId);
  const c = newContainer();
  c.addTextDisplayComponents(text(`\`\`\`\n💸︲Twoja Historia × ${config.shopName}\n\`\`\``));
  c.addSeparatorComponents(sep());
  c.addTextDisplayComponents(text(`# ${E.historiaNaglowek}${S}__Historia zakupów__`));
  c.addSeparatorComponents(sep());
  if (!mine.length) {
    c.addTextDisplayComponents(text(`> ${E.zakup}${S}Brak zakupów.`));
  } else {
    const lines = mine
      .slice(-HISTORY_LIMIT)
      .reverse()
      .map(
        (p) =>
          `> ${E.zakup}${S}Zakup za \`${zl(p.amount)}\` • <t:${p.createdAt}:f> • Sprzedał: <@${p.sellerId}>` +
          (p.note ? `\n> -# #${pad(p.number)} • ${clean(p.note).slice(0, 80)}` : `\n> -# #${pad(p.number)}`),
      );
    if (mine.length > HISTORY_LIMIT) lines.push(`-# …i ${mine.length - HISTORY_LIMIT} starszych zakupów`);
    c.addTextDisplayComponents(text(lines.join('\n')));
  }
  return { components: [c], flags: EPH, allowedMentions: { parse: [] } };
}

// ───────────── komendy ─────────────
const commands = [
  new SlashCommandBuilder()
    .setName('panel-klienta')
    .setDescription('Wysyła Panel Klienta na ten kanał')
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
    .then(() => console.log(`✅ Komendy /panel-klienta i /zakup dodane na serwerze: ${guild.name}`))
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
    if (interaction.isChatInputCommand() && interaction.commandName === 'panel-klienta') {
      await interaction.deferReply({ flags: MessageFlags.Ephemeral });
      await interaction.channel.send(buildPanel());
      return interaction.editReply(notice('Panel Klienta wysłany!', true));
    }

    if (interaction.isChatInputCommand() && interaction.commandName === 'zakup') {
      if (!isSeller(interaction.member)) return interaction.reply(notice('Tylko sprzedawcy mogą dodawać zakupy.'));
      const sub = interaction.options.getSubcommand();

      if (sub === 'dodaj') {
        const klient = interaction.options.getUser('klient');
        const kwota = Math.round(interaction.options.getNumber('kwota') * 100) / 100;
        const opis = interaction.options.getString('opis') || '';
        db.counter += 1;
        const p = {
          number: db.counter,
          guildId: interaction.guildId,
          clientId: klient.id,
          sellerId: interaction.user.id,
          amount: kwota,
          note: opis,
          createdAt: Math.floor(Date.now() / 1000),
        };
        db.purchases.push(p);
        saveDb();
        const total = purchasesOf(klient.id, interaction.guildId).reduce((s, x) => s + x.amount, 0);
        return interaction.reply({
          ...notice(
            `Dodano zakup **#${pad(p.number)}**: <@${klient.id}> za \`${zl(kwota)}\`${opis ? ` (${clean(opis)})` : ''}.\n` +
              `-# Klient wydał łącznie \`${zl(total)}\``,
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
        saveDb();
        return interaction.reply({
          ...notice(`Usunięto zakup **#${pad(p.number)}** (<@${p.clientId}>, \`${zl(p.amount)}\`).`, true),
          allowedMentions: { parse: [] },
        });
      }
    }

    if (interaction.isStringSelectMenu() && interaction.customId === 'vs_panel') {
      const choice = interaction.values[0];
      const payload =
        choice === 'wydane'
          ? buildSpent(interaction.user.id, interaction.guildId)
          : buildHistory(interaction.user.id, interaction.guildId);
      await interaction.reply(payload);
      // reset wyboru w menu, żeby dało się wybrać tę samą opcję jeszcze raz
      return interaction.message.edit(buildPanel()).catch(() => {});
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
    console.error("❌ Brak tokena! Wpisz TOKEN=... w pliku .env albo token: '...' na górze index.js");
    process.exit(1);
  }
  return client.login(token).catch((e) => {
    console.error('❌ Nie udało się zalogować:', e.message);
    if (/token/i.test(e.message)) console.error('   → Token jest zły. Zresetuj go na discord.com/developers → Bot → Reset Token.');
    process.exit(1);
  });
}

module.exports = { buildPanel, buildSpent, buildHistory, start };
if (require.main === module) start();
