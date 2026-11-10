require('dotenv').config();
const {
  Client, GatewayIntentBits, Partials, ChannelType, PermissionFlagsBits, AttachmentBuilder, ActivityType,
  MediaGalleryBuilder, MediaGalleryItemBuilder, FileBuilder, REST, Routes,
} = require('discord.js');
const config = require('./config');
const db = require('./db');
const ui = require('./ui');
const commands = require('./commands');
const { renderHtml } = require('./transcript');

const client = new Client({
  intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.GuildMembers],
  partials: [Partials.Channel],
});

const T = () => db.data.tickets;
const ticketOf = (channelId) => T()[channelId] || null;
const isStaff = (member) =>
  !!member && (member.permissions?.has(PermissionFlagsBits.Administrator) || config.staffRoles.some((r) => member.roles?.cache?.has(r)));
const staffRoles = (guild) => config.staffRoles.filter((r) => guild.roles.cache.has(r));
const slug = (s) => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/ł/g, 'l').replace(/[^a-z0-9]/g, '').slice(0, 16) || 'user';

async function log(guild, payload) {
  const ch = guild.channels.cache.get(config.logsChannel);
  if (ch) await ch.send(payload).catch((e) => console.error('log:', e.message));
}

async function refresh(channel, t) {
  const msg = await channel.messages.fetch(t.messageId).catch(() => null);
  if (!msg) return;
  const member = await channel.guild.members.fetch(t.owner).catch(() => null);
  await msg.edit({ components: [ui.ticketView(t, member)] }).catch((e) => console.error('refresh:', e.message));
}

// tworzenie ticketu

const creating = new Set();
async function openTicket(interaction, data) {
  const { guild, user } = interaction;
  if (db.data.blacklist[user.id]) {
    return interaction.editReply(ui.notice(`Nie możesz otwierać ticketów.\nPowód: ${db.data.blacklist[user.id].reason || 'brak'}`));
  }
  const open = Object.entries(T()).filter(([id, t]) => t.owner === user.id && guild.channels.cache.has(id));
  if (open.length >= config.maxOpen) return interaction.editReply(ui.notice(`Masz już otwarty ticket: <#${open[0][0]}>`));
  if (creating.has(user.id)) return interaction.editReply(ui.notice('Ticket już się tworzy, chwila.'));
  creating.add(user.id);

  try {
    db.data.counter += 1;
    const t = { id: db.data.counter, owner: user.id, status: 'new', claimedBy: null, createdAt: ui.now(), messageId: null, ...data };
    const name = t.type === 'exchange' ? `${config.exchangeEmoji}・${config.exchangeChannel}-${slug(user.username)}` : `${t.emoji}・${t.channel}-${slug(user.username)}`;
    const perms = [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory, PermissionFlagsBits.AttachFiles, PermissionFlagsBits.EmbedLinks];

    const channel = await guild.channels.create({
      name,
      type: ChannelType.GuildText,
      parent: guild.channels.cache.has(config.ticketCategory) ? config.ticketCategory : null,
      topic: `#${ui.num(t.id)} | ${user.username} (${user.id})`,
      permissionOverwrites: [
        { id: guild.roles.everyone.id, deny: [PermissionFlagsBits.ViewChannel] },
        { id: user.id, allow: perms },
        { id: client.user.id, allow: [...perms, PermissionFlagsBits.ManageChannels, PermissionFlagsBits.ManageMessages] },
        ...staffRoles(guild).map((id) => ({ id, allow: perms })),
      ],
    });

    const member = await guild.members.fetch(user.id).catch(() => null);
    const msg = await channel.send(ui.send(ui.ticketView(t, member)));
    t.messageId = msg.id;
    T()[channel.id] = t;
    db.save();

    const pings = [`<@${user.id}>`, ...staffRoles(guild).map((r) => `<@&${r}>`)].join(' ');
    channel.send(pings).then((m) => setTimeout(() => m.delete().catch(() => {}), 4000)).catch(() => {});

    if (t.type === 'exchange') {
      channel
        .send(ui.small(`Cześć <@${user.id}>! Obsługa zaraz się odezwie.\nNie wysyłaj płatności, dopóki nie dostaniesz danych tutaj na tickecie.`, 'info'))
        .catch(() => {});
    }

    await interaction.editReply(ui.notice(`Ticket utworzony: ${channel}`, 'ok'));
    await log(guild, ui.logOpen(t, channel.id));
  } finally {
    creating.delete(user.id);
  }
}

// transkrypt i zamykanie

async function fetchAll(channel) {
  let all = [];
  let before;
  while (all.length < 3000) {
    const batch = await channel.messages.fetch({ limit: 100, before });
    if (!batch.size) break;
    all = all.concat([...batch.values()]);
    before = batch.last().id;
    if (batch.size < 100) break;
  }
  return all.reverse();
}

async function transcript(channel, t, closedBy, reason) {
  const messages = await fetchAll(channel);
  const html = renderHtml({ channel, messages, ticket: { ...t, number: ui.num(t.id) }, closedBy, reason });
  const name = `ticket-${ui.num(t.id)}.html`;
  return { name, file: () => new AttachmentBuilder(Buffer.from(html, 'utf8'), { name }) };
}

const closing = new Set();
async function closeTicket(interaction, reason = '') {
  const channel = interaction.channel;
  const t = ticketOf(channel.id);
  if (!t || closing.has(channel.id)) return;
  closing.add(channel.id);

  try {
    await channel.send(ui.small(`Ticket zostanie zamknięty <t:${ui.now() + config.closeDelay + 1}:R>${reason ? `\nPowód: ${ui.plain(reason)}` : ''}`, 'bad'));
    const tr = await transcript(channel, t, interaction.user.username, reason);

    await log(interaction.guild, { components: [ui.logClose(t, interaction.user.id, reason, tr.name)], files: [tr.file()], flags: ui.V2, allowedMentions: { parse: [] } });

    db.data.history[t.id] = {
      id: t.id, guild: interaction.guildId, owner: t.owner, type: t.type, label: t.label,
      from: t.from, to: t.to, amount: t.amount, receive: t.receive, status: t.status, claimedBy: t.claimedBy, closedAt: ui.now(),
    };
    const keys = Object.keys(db.data.history);
    if (keys.length > 2000) for (const k of keys.slice(0, keys.length - 2000)) delete db.data.history[k];

    const owner = await client.users.fetch(t.owner).catch(() => null);
    if (owner) {
      await owner
        .send({ components: [ui.dmClosed(t, interaction.guildId, interaction.guild.name, reason, tr.name)], files: [tr.file()], flags: ui.V2 })
        .catch(() => {});
    }

    delete T()[channel.id];
    db.save();
    setTimeout(() => channel.delete().catch(() => {}).finally(() => closing.delete(channel.id)), config.closeDelay * 1000);
  } catch (e) {
    closing.delete(channel.id);
    throw e;
  }
}

// status wymiany

async function setStatus(interaction, t, status) {
  const prev = t.status;
  t.status = status;
  if (!t.claimedBy && ['waiting', 'paid', 'done'].includes(status)) t.claimedBy = interaction.user.id;

  if (status === 'done' && !t.counted) {
    t.counted = true;
    const who = t.claimedBy || interaction.user.id;
    const st = db.data.stats;
    st.done += 1;
    st.volume = Math.round((st.volume + t.amount) * 100) / 100;
    st.staff[who] = st.staff[who] || { done: 0, volume: 0 };
    st.staff[who].done += 1;
    st.staff[who].volume = Math.round((st.staff[who].volume + t.amount) * 100) / 100;
  }
  if (prev === 'done' && status !== 'done' && t.counted) {
    t.counted = false;
    const who = t.claimedBy || interaction.user.id;
    const st = db.data.stats;
    st.done = Math.max(0, st.done - 1);
    st.volume = Math.max(0, Math.round((st.volume - t.amount) * 100) / 100);
    if (st.staff[who]) {
      st.staff[who].done = Math.max(0, st.staff[who].done - 1);
      st.staff[who].volume = Math.max(0, Math.round((st.staff[who].volume - t.amount) * 100) / 100);
    }
  }
  db.save();

  const s = ui.STATUS[status];
  const extra = {
    waiting: `\n<@${t.owner}>, możesz wysłać płatność według instrukcji obsługi. Potem kliknij **Wyślij potwierdzenie**.`,
    paid: '\nPłatność dotarła, realizujemy wypłatę.',
    done: `\n<@${t.owner}>, wymiana zakończona. Dzięki! Po zamknięciu ticketu możesz zostawić opinię.`,
    cancel: '',
  }[status] || '';
  await interaction.channel.send(ui.small(`${s.icon} Status: **${s.label}**${extra}`, status === 'cancel' ? 'bad' : status === 'waiting' ? 'warn' : 'ok'));
  await refresh(interaction.channel, t);
  await log(interaction.guild, ui.small(`Ticket #${ui.num(t.id)}: ${s.icon} ${s.label} (zmienił <@${interaction.user.id}>)`));
}

// interakcje

async function onCommand(i) {
  const name = i.commandName;

  if (name === 'panel') {
    await i.channel.send(ui.panel());
    return i.reply(ui.notice('Panel wysłany.', 'ok'));
  }

  if (name === 'kursy') return i.reply(ui.eph(ui.ratesView()));

  if (name === 'prowizja') {
    const sub = i.options.getSubcommand();
    const from = i.options.getString('z');
    const to = i.options.getString('na');
    const key = `${from}>${to}`;
    if (from === to) return i.reply(ui.notice('Wybierz dwie różne metody.'));
    if (sub === 'ustaw') {
      const p = i.options.getNumber('procent');
      db.data.fees[key] = p;
      db.save();
      return i.reply(ui.notice(`${ui.mName(from)} → ${ui.mName(to)}: **${p}%**`, 'ok'));
    }
    delete db.data.fees[key];
    db.save();
    return i.reply(ui.notice(`Przywrócono domyślną prowizję: **${ui.feeFor(from, to)}%**`, 'ok'));
  }

  if (name === 'blacklist') {
    const sub = i.options.getSubcommand();
    if (sub === 'lista') {
      const list = Object.entries(db.data.blacklist);
      const body = list.length ? list.slice(0, 40).map(([id, b]) => `<@${id}> \`${id}\` - ${b.reason || 'brak powodu'}`).join('\n') : 'Lista jest pusta.';
      return i.reply(ui.eph(ui.box().addTextDisplayComponents(ui.text(`### Blacklista\n${body}`)), { allowedMentions: { parse: [] } }));
    }
    const u = i.options.getUser('osoba');
    if (sub === 'dodaj') {
      db.data.blacklist[u.id] = { reason: i.options.getString('powod') || '', by: i.user.id, at: ui.now() };
      db.save();
      await log(i.guild, ui.small(`<@${i.user.id}> dodał <@${u.id}> do blacklisty. ${i.options.getString('powod') || ''}`, 'bad'));
      return i.reply(ui.notice(`${u} jest na blackliście.`, 'ok'));
    }
    delete db.data.blacklist[u.id];
    db.save();
    return i.reply(ui.notice(`Usunięto ${u} z blacklisty.`, 'ok'));
  }

  if (name === 'statystyki') {
    const u = i.options.getUser('osoba');
    const hist = Object.values(db.data.history);
    const rated = (fn) => {
      const r = hist.filter((h) => h.rating && fn(h));
      return { rating: r.length ? r.reduce((a, h) => a + h.rating, 0) / r.length : 0, votes: r.length };
    };
    if (u) {
      const s = db.data.stats.staff[u.id] || { done: 0, volume: 0 };
      return i.reply(ui.eph(ui.statsView(`Statystyki: ${u.username}`, { ...s, ...rated((h) => h.claimedBy === u.id) })));
    }
    const open = Object.keys(T()).filter((id) => i.guild.channels.cache.has(id)).length;
    return i.reply(ui.eph(ui.statsView(config.name, { done: db.data.stats.done, volume: db.data.stats.volume, open, ...rated(() => true) })));
  }

  if (name === 'ticket') {
    const t = ticketOf(i.channelId);
    if (!t) return i.reply(ui.notice('To nie jest kanał ticketu.'));
    const sub = i.options.getSubcommand();
    const staff = isStaff(i.member);
    if (sub === 'zamknij') {
      if (!staff && i.user.id !== t.owner) return i.reply(ui.notice('Nie możesz zamknąć tego ticketu.'));
      await i.reply(ui.notice('Zamykam...', 'ok'));
      return closeTicket(i, i.options.getString('powod') || '');
    }
    if (!staff) return i.reply(ui.notice('Tylko obsługa.'));
    if (sub === 'dodaj') {
      const u = i.options.getUser('osoba');
      await i.channel.permissionOverwrites.edit(u.id, { ViewChannel: true, SendMessages: true, ReadMessageHistory: true, AttachFiles: true });
      await i.channel.send(ui.small(`Dodano <@${u.id}> do ticketu.`));
      return i.reply(ui.notice('Dodano.', 'ok'));
    }
    if (sub === 'usun') {
      const u = i.options.getUser('osoba');
      if (u.id === t.owner) return i.reply(ui.notice('Nie można usunąć właściciela ticketu.'));
      await i.channel.permissionOverwrites.delete(u.id);
      await i.channel.send(ui.small(`Usunięto <@${u.id}> z ticketu.`));
      return i.reply(ui.notice('Usunięto.', 'ok'));
    }
    if (sub === 'nazwa') {
      await i.channel.setName(i.options.getString('nazwa'));
      return i.reply(ui.notice('Zmieniono nazwę.', 'ok'));
    }
  }
}

async function onSelect(i) {
  if (i.customId === 'panel:open') {
    const v = i.values[0];
    if (v === 'exchange') await i.showModal(ui.exchangeModal());
    else {
      const cat = config.categories.find((c) => c.id === v);
      if (cat) await i.showModal(ui.categoryModal(cat));
    }
    i.message.edit({ components: ui.panel().components }).catch(() => {});
    return;
  }

  if (i.customId === 't:menu') {
    const t = ticketOf(i.channelId);
    if (!t) return i.reply(ui.notice('Nie znaleziono ticketu.'));
    const v = i.values[0];
    const staff = isStaff(i.member);
    const owner = i.user.id === t.owner;
    const reset = () => refresh(i.channel, t);

    if (v === 'close_reason') {
      if (!staff && !owner) return i.reply(ui.notice('Nie możesz zamknąć tego ticketu.'));
      await i.showModal(ui.reasonModal());
      return reset();
    }
    if (!staff) {
      await i.reply(ui.notice('Tylko obsługa może tego użyć.'));
      return reset();
    }
    if (v.startsWith('status:')) {
      await i.deferUpdate();
      return setStatus(i, t, v.slice(7));
    }
    if (v === 'remind') {
      await i.reply(ui.notice('Wysłano przypomnienie.', 'ok'));
      await i.channel.send(ui.small(`🔔 <@${t.owner}>, czekamy na Twoją odpowiedź.`, 'warn'));
      const u = await client.users.fetch(t.owner).catch(() => null);
      u?.send(ui.small(`🔔 Masz nową wiadomość na tickecie #${ui.num(t.id)}: ${i.channel}`, 'warn')).catch(() => {});
      return reset();
    }
    if (v === 'add' || v === 'remove') {
      await i.reply(ui.userPicker(v));
      return reset();
    }
    if (v === 'transcript') {
      await i.deferReply({ flags: ui.EPH });
      const tr = await transcript(i.channel, t, null, null);
      await i.editReply({
        components: [ui.box().addTextDisplayComponents(ui.text(`Transkrypt ticketu #${ui.num(t.id)}`)).addFileComponents(new FileBuilder().setURL(`attachment://${tr.name}`))],
        files: [tr.file()],
      });
      return reset();
    }
  }
}

async function onUserSelect(i) {
  const t = ticketOf(i.channelId);
  if (!t || !isStaff(i.member)) return i.reply(ui.notice('Brak uprawnień.'));
  const id = i.values[0];
  if (i.customId === 'pick:add') {
    await i.channel.permissionOverwrites.edit(id, { ViewChannel: true, SendMessages: true, ReadMessageHistory: true, AttachFiles: true });
    await i.channel.send(ui.small(`Dodano <@${id}> do ticketu.`));
    return i.update(ui.notice(`Dodano <@${id}>.`, 'ok'));
  }
  if (id === t.owner) return i.update(ui.notice('Nie można usunąć właściciela ticketu.'));
  await i.channel.permissionOverwrites.delete(id);
  await i.channel.send(ui.small(`Usunięto <@${id}> z ticketu.`));
  return i.update(ui.notice(`Usunięto <@${id}>.`, 'ok'));
}

async function onButton(i) {
  const id = i.customId;

  if (id === 'panel:rates') return i.reply(ui.eph(ui.ratesView()));
  if (id === 'panel:calc') return i.showModal(ui.calcModal());

  if (id.startsWith('rate:')) {
    const [, , ticketId] = id.split(':');
    const h = db.data.history[ticketId];
    if (h?.rating) return i.reply(ui.notice('Ten ticket został już oceniony.'));
    return i.showModal(ui.vouchModal(id.slice(5)));
  }

  const t = ticketOf(i.channelId);
  if (!t) return;
  const staff = isStaff(i.member);
  const owner = i.user.id === t.owner;

  if (id === 't:claim') {
    if (!staff) return i.reply(ui.notice('Tylko obsługa może przejąć ticket.'));
    if (t.claimedBy) return i.reply(ui.notice(`Ticket przejął już <@${t.claimedBy}>.`));
    t.claimedBy = i.user.id;
    if (t.status === 'new') t.status = 'progress';
    db.save();
    await i.deferUpdate();
    await refresh(i.channel, t);
    return i.channel.send(ui.small(`<@${i.user.id}> zajmie się Twoim ticketem.`));
  }
  if (id === 't:proof') {
    if (!staff && !owner) return i.reply(ui.notice('Tylko właściciel ticketu może wysłać potwierdzenie.'));
    return i.showModal(ui.proofModal());
  }
  if (id === 't:close') {
    if (!staff && !owner) return i.reply(ui.notice('Nie możesz zamknąć tego ticketu.'));
    return i.reply(ui.closeConfirm());
  }
  if (id === 'close:no') return i.message.delete().catch(() => {});
  if (id.startsWith('close:')) {
    if (!staff && !owner) return i.reply(ui.notice('Nie możesz zamknąć tego ticketu.'));
    if (id === 'close:why') return i.showModal(ui.reasonModal());
    await i.update({ components: [ui.box(ui.STATUS.cancel.color).addTextDisplayComponents(ui.text(`Zamykanie potwierdził <@${i.user.id}>`))] });
    return closeTicket(i, '');
  }
}

async function onModal(i) {
  const id = i.customId;

  if (id === 'form:exchange' || id === 'form:calc') {
    const from = i.fields.getStringSelectValues('from')[0];
    const to = i.fields.getStringSelectValues('to')[0];
    const amount = ui.parseAmount(i.fields.getTextInputValue('amount'));
    const mf = ui.m(from);
    if (from === to) return i.reply(ui.notice('Metoda wysyłki i odbioru muszą się różnić.'));
    if (!amount) return i.reply(ui.notice('Podaj poprawną kwotę, np. `150` albo `99,50`.'));
    if (amount < mf.min || amount > mf.max) return i.reply(ui.notice(`Dla ${ui.mName(from)} kwota musi być od ${ui.zl(mf.min)} do ${ui.zl(mf.max)}.`));
    if (id === 'form:calc') return i.reply(ui.calcResult(from, to, amount));

    await i.deferReply({ flags: ui.EPH });
    const { fee, receive } = ui.calc(from, to, amount);
    return openTicket(i, {
      type: 'exchange', from, to, amount, fee, receive,
      payout: i.fields.getTextInputValue('payout') || '',
      note: i.fields.getTextInputValue('note') || '',
    });
  }

  if (id.startsWith('form:cat:')) {
    const cat = config.categories.find((c) => c.id === id.slice(9));
    if (!cat) return;
    await i.deferReply({ flags: ui.EPH });
    const answers = {};
    for (const q of cat.questions.slice(0, 5)) answers[q.id] = i.fields.getTextInputValue(q.id) || '';
    return openTicket(i, {
      type: cat.id, label: cat.label, emoji: cat.emoji, channel: cat.channel,
      questions: cat.questions.slice(0, 5).map(({ id: qid, label, style }) => ({ id: qid, label, style })),
      answers,
    });
  }

  if (id === 'form:proof') {
    const t = ticketOf(i.channelId);
    if (!t) return;
    const files = [...(i.fields.getUploadedFiles('files') || new Map()).values()];
    if (!files.length) return i.reply(ui.notice('Nie dodano pliku.'));
    await i.deferReply({ flags: ui.EPH });
    const atts = files.map((f, n) => {
      const ext = (f.name.split('.').pop() || 'png').toLowerCase().replace(/[^a-z0-9]/g, '');
      return { name: `potwierdzenie-${n + 1}.${ext}`, url: f.url, image: (f.contentType || '').startsWith('image/') };
    });
    const c = ui.box(ui.STATUS.sent.color);
    const comment = i.fields.getTextInputValue('text');
    c.addTextDisplayComponents(ui.text(`🧾 **Potwierdzenie płatności** od <@${i.user.id}>${comment ? `\n> ${ui.plain(comment)}` : ''}`));
    const imgs = atts.filter((a) => a.image);
    if (imgs.length) c.addMediaGalleryComponents(new MediaGalleryBuilder().addItems(imgs.map((a) => new MediaGalleryItemBuilder().setURL(`attachment://${a.name}`))));
    for (const a of atts.filter((x) => !x.image)) c.addFileComponents(new FileBuilder().setURL(`attachment://${a.name}`));
    await i.channel.send({ components: [c], files: atts.map((a) => new AttachmentBuilder(a.url, { name: a.name })), flags: ui.V2, allowedMentions: { parse: [] } });
    if (t.type === 'exchange' && ['new', 'progress', 'waiting'].includes(t.status) && i.user.id === t.owner) {
      t.status = 'sent';
      db.save();
      await refresh(i.channel, t);
      const pings = staffRoles(i.guild).map((r) => `<@&${r}>`).join(' ');
      if (t.claimedBy || pings) i.channel.send(t.claimedBy ? `<@${t.claimedBy}>` : pings).then((m) => setTimeout(() => m.delete().catch(() => {}), 4000)).catch(() => {});
    }
    return i.editReply(ui.notice('Potwierdzenie wysłane.', 'ok'));
  }

  if (id === 'form:close') {
    const t = ticketOf(i.channelId);
    if (!t) return;
    if (!isStaff(i.member) && i.user.id !== t.owner) return i.reply(ui.notice('Nie możesz zamknąć tego ticketu.'));
    await i.reply(ui.notice('Zamykam...', 'ok'));
    return closeTicket(i, i.fields.getTextInputValue('reason'));
  }

  if (id.startsWith('form:vouch:')) {
    const [guildId, ticketId, stars] = id.slice(11).split(':');
    const n = Math.min(5, Math.max(1, Number(stars) || 5));
    const h = db.data.history[ticketId];
    if (!h || h.rating) return i.reply(ui.notice('Nie można już ocenić tego ticketu.'));
    const comment = i.fields.getTextInputValue('text') || '';
    h.rating = n;
    h.comment = comment;
    db.save();

    await i.update({
      components: [
        ui
          .box(ui.STATUS.done.color)
          .addTextDisplayComponents(ui.text(`### Dzięki za opinię!\n${'⭐'.repeat(n)}${comment ? `\n> ${ui.plain(comment)}` : ''}`))
          .addFileComponents(new FileBuilder().setURL(`attachment://ticket-${ui.num(h.id)}.html`)),
      ],
    });

    const guild = client.guilds.cache.get(guildId);
    if (!guild) return;
    await log(guild, ui.small(`Ocena ticketu #${ui.num(h.id)}: ${'⭐'.repeat(n)} od <@${h.owner}>${comment ? `\n> ${ui.plain(comment)}` : ''}`));
    const ch = guild.channels.cache.get(config.vouchChannel);
    if (ch && h.type === 'exchange' && h.status === 'done') {
      await ch.send(ui.vouchView(h, n, comment, i.user.displayAvatarURL({ size: 128 }))).catch((e) => console.error('opinia:', e.message));
    }
  }
}

client.on('interactionCreate', async (i) => {
  try {
    if (i.isChatInputCommand()) return await onCommand(i);
    if (i.isStringSelectMenu()) return await onSelect(i);
    if (i.isUserSelectMenu()) return await onUserSelect(i);
    if (i.isButton()) return await onButton(i);
    if (i.isModalSubmit()) return await onModal(i);
  } catch (e) {
    console.error(e);
    const p = ui.notice('Coś poszło nie tak, spróbuj ponownie.');
    if (i.deferred || i.replied) i.followUp(p).catch(() => {});
    else if (i.isRepliable()) i.reply(p).catch(() => {});
  }
});

client.once('clientReady', async () => {
  console.log(`zalogowano jako ${client.user.tag}`);
  client.user.setPresence({ activities: [{ name: config.status, type: ActivityType.Watching }] });

  for (const id of Object.keys(T())) if (!client.channels.cache.has(id)) delete T()[id];
  db.save();

  try {
    const rest = new REST({ version: '10' }).setToken(process.env.TOKEN);
    const app = process.env.CLIENT_ID || client.user.id;
    const route = process.env.GUILD_ID ? Routes.applicationGuildCommands(app, process.env.GUILD_ID) : Routes.applicationCommands(app);
    await rest.put(route, { body: commands });
    console.log(`komendy: ${commands.length}`);
  } catch (e) {
    console.error('rejestracja komend:', e.message);
  }
});

process.on('unhandledRejection', (e) => console.error('unhandled:', e));

client.login(process.env.TOKEN);
