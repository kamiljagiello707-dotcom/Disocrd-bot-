const {
  ActionRowBuilder, ButtonBuilder, ButtonStyle, StringSelectMenuBuilder, UserSelectMenuBuilder,
  ContainerBuilder, TextDisplayBuilder, SeparatorBuilder, SeparatorSpacingSize, SectionBuilder,
  ThumbnailBuilder, MediaGalleryBuilder, MediaGalleryItemBuilder, FileBuilder, MessageFlags,
  ModalBuilder, LabelBuilder, TextInputBuilder, TextInputStyle, FileUploadBuilder, parseEmoji,
} = require('discord.js');
const config = require('./config');
const db = require('./db');

const V2 = MessageFlags.IsComponentsV2;
const EPH = V2 | MessageFlags.Ephemeral;

const methods = config.methods;
const m = (id) => methods[id] || { label: id, emoji: '' };
const mName = (id) => `${m(id).emoji} ${m(id).label}`.trim();
const pair = (from, to) => `${from}>${to}`;
const feeFor = (from, to) => db.data.fees[pair(from, to)] ?? config.fees[pair(from, to)] ?? config.defaultFee;
const calc = (from, to, amount) => {
  const fee = feeFor(from, to);
  return { fee, receive: Math.floor(amount * (100 - fee)) / 100 };
};
const zl = (n) => `${Number(n).toLocaleString('pl-PL', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} zł`;
const parseAmount = (s) => {
  const n = Number(String(s).replace(/zł|pln/gi, '').replace(/\s/g, '').replace(',', '.'));
  return Number.isFinite(n) && n > 0 ? Math.round(n * 100) / 100 : null;
};
const ts = (sec, f = 'R') => `<t:${sec}:${f}>`;
const now = () => Math.floor(Date.now() / 1000);
const num = (n) => String(n).padStart(4, '0');
const plain = (s) => String(s ?? '').replace(/```/g, "'''").trim();
const emojiOf = (e) => (e ? parseEmoji(e) : undefined);

const STATUS = {
  new: { icon: '🟡', label: 'Czeka na obsługę', color: config.colors.warn },
  progress: { icon: '🔵', label: 'W trakcie', color: config.colors.info },
  waiting: { icon: '🟠', label: 'Czeka na płatność', color: config.colors.warn },
  sent: { icon: '🟣', label: 'Klient wysłał płatność', color: config.colors.sent },
  paid: { icon: '🟢', label: 'Płatność potwierdzona', color: config.colors.ok },
  done: { icon: '✅', label: 'Zrealizowana', color: config.colors.ok },
  cancel: { icon: '🔴', label: 'Anulowana', color: config.colors.bad },
};

const text = (s) => new TextDisplayBuilder().setContent(s);
const line = (big = false) => new SeparatorBuilder().setDivider(true).setSpacing(big ? SeparatorSpacingSize.Large : SeparatorSpacingSize.Small);
const box = (color = config.color) => new ContainerBuilder().setAccentColor(color);

function head(c, content, thumb) {
  if (thumb) {
    c.addSectionComponents(new SectionBuilder().addTextDisplayComponents(text(content)).setThumbnailAccessory(new ThumbnailBuilder().setURL(thumb)));
  } else {
    c.addTextDisplayComponents(text(content));
  }
  return c;
}

function footer(c, extra) {
  if (config.banner) {
    c.addMediaGalleryComponents(new MediaGalleryBuilder().addItems(new MediaGalleryItemBuilder().setURL(config.banner)));
  }
  c.addTextDisplayComponents(text(`-# ${config.name}${extra ? ` • ${extra}` : ''}`));
  return c;
}

const send = (c, extra = {}) => ({ components: [c], flags: V2, ...extra });
const eph = (c, extra = {}) => ({ components: [c], flags: EPH, ...extra });
const notice = (msg, type = 'bad') => eph(box(config.colors[type]).addTextDisplayComponents(text(msg)));
const small = (msg, type = 'info') => send(box(config.colors[type]).addTextDisplayComponents(text(msg)));

// panel

function popularList() {
  return config.popular
    .map((p) => p.split('>'))
    .filter(([a, b]) => methods[a] && methods[b])
    .map(([a, b]) => `${mName(a)} → ${mName(b)} \`${feeFor(a, b)}%\``)
    .join('\n');
}

function panel() {
  const c = box();
  head(c, `## ${config.name}\n${config.tagline}`, config.logo);
  c.addSeparatorComponents(line());
  c.addTextDisplayComponents(
    text(
      [
        '**Jak wymienić?**',
        '`1` Wybierz kategorię z listy',
        '`2` Uzupełnij formularz (metody i kwota)',
        '`3` Poczekaj na obsługę i postępuj zgodnie z instrukcją',
      ].join('\n'),
    ),
  );
  c.addTextDisplayComponents(text(`🕒 Obsługa: **${config.hours}**\n🔒 Płatność wysyłasz dopiero, gdy obsługa poda dane na tickecie.`));
  const pop = popularList();
  if (pop) {
    c.addSeparatorComponents(line());
    c.addTextDisplayComponents(text(`**Popularne kursy**\n${pop}`));
  }
  c.addSeparatorComponents(line());
  const options = [
    { label: 'Wymiana', value: 'exchange', description: 'Wymień środki między metodami', emoji: emojiOf(config.exchangeEmoji) },
    ...config.categories.map((k) => ({ label: k.label, value: k.id, description: k.description, emoji: emojiOf(k.emoji) })),
  ];
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(new StringSelectMenuBuilder().setCustomId('panel:open').setPlaceholder('Wybierz kategorię').addOptions(options)),
    new ActionRowBuilder().addComponents(
      new ButtonBuilder().setCustomId('panel:rates').setLabel('Kursy').setEmoji('📊').setStyle(ButtonStyle.Secondary),
      new ButtonBuilder().setCustomId('panel:calc').setLabel('Kalkulator').setEmoji('🧮').setStyle(ButtonStyle.Secondary),
    ),
  );
  return send(footer(c));
}

function ratesView() {
  const c = box();
  c.addTextDisplayComponents(text(`### 📊 Kursy ${config.name}\n-# prowizja pobierana od kwoty, którą wysyłasz`));
  c.addSeparatorComponents(line());
  const sendable = Object.keys(methods).filter((id) => methods[id].send !== false);
  const recv = Object.keys(methods).filter((id) => methods[id].receive !== false);
  const blocks = sendable.map((a) => {
    const list = recv.filter((b) => b !== a).map((b) => `${m(b).label} \`${feeFor(a, b)}%\``).join(' · ');
    return `**${mName(a)}** → ${list}\n-# min ${zl(m(a).min)}, max ${zl(m(a).max)}`;
  });
  c.addTextDisplayComponents(text(blocks.join('\n')));
  return footer(c, 'kursy mogą się zmieniać');
}

// formularze

const methodSelect = (id, side, placeholder, picked) =>
  new StringSelectMenuBuilder()
    .setCustomId(id)
    .setPlaceholder(placeholder)
    .addOptions(
      Object.entries(methods)
        .filter(([, v]) => v[side] !== false)
        .map(([k, v]) => ({ label: v.label, value: k, emoji: emojiOf(v.emoji), default: k === picked })),
    );

function exchangeModal() {
  return new ModalBuilder()
    .setCustomId('form:exchange')
    .setTitle('Nowa wymiana')
    .addLabelComponents(
      new LabelBuilder().setLabel('Co wysyłasz?').setStringSelectMenuComponent(methodSelect('from', 'send', 'Wybierz metodę')),
      new LabelBuilder().setLabel('Co chcesz otrzymać?').setStringSelectMenuComponent(methodSelect('to', 'receive', 'Wybierz metodę')),
      new LabelBuilder()
        .setLabel('Kwota (PLN)')
        .setDescription('Ile wysyłasz, prowizję policzymy sami')
        .setTextInputComponent(new TextInputBuilder().setCustomId('amount').setStyle(TextInputStyle.Short).setPlaceholder('np. 150').setMaxLength(12)),
      new LabelBuilder()
        .setLabel('Dane do wypłaty')
        .setDescription('Numer telefonu, e-mail, adres portfela itd. Możesz podać później.')
        .setTextInputComponent(new TextInputBuilder().setCustomId('payout').setStyle(TextInputStyle.Short).setRequired(false).setMaxLength(150)),
      new LabelBuilder()
        .setLabel('Uwagi')
        .setTextInputComponent(new TextInputBuilder().setCustomId('note').setStyle(TextInputStyle.Paragraph).setRequired(false).setMaxLength(500)),
    );
}

function calcModal() {
  return new ModalBuilder()
    .setCustomId('form:calc')
    .setTitle('Kalkulator wymiany')
    .addLabelComponents(
      new LabelBuilder().setLabel('Wysyłam').setStringSelectMenuComponent(methodSelect('from', 'send', 'Wybierz metodę')),
      new LabelBuilder().setLabel('Otrzymuję').setStringSelectMenuComponent(methodSelect('to', 'receive', 'Wybierz metodę')),
      new LabelBuilder().setLabel('Kwota (PLN)').setTextInputComponent(new TextInputBuilder().setCustomId('amount').setStyle(TextInputStyle.Short).setPlaceholder('np. 200').setMaxLength(12)),
    );
}

function categoryModal(cat) {
  const modal = new ModalBuilder().setCustomId(`form:cat:${cat.id}`).setTitle(cat.label.slice(0, 45));
  modal.addLabelComponents(
    cat.questions.slice(0, 5).map((q) =>
      new LabelBuilder()
        .setLabel(q.label)
        .setTextInputComponent(
          new TextInputBuilder()
            .setCustomId(q.id)
            .setStyle(q.style === 'long' ? TextInputStyle.Paragraph : TextInputStyle.Short)
            .setPlaceholder(q.placeholder || '')
            .setRequired(q.required !== false)
            .setMaxLength(q.max || 200),
        ),
    ),
  );
  return modal;
}

function proofModal() {
  return new ModalBuilder()
    .setCustomId('form:proof')
    .setTitle('Potwierdzenie płatności')
    .addLabelComponents(
      new LabelBuilder()
        .setLabel('Zrzut ekranu lub plik')
        .setDescription('Maksymalnie 3 pliki')
        .setFileUploadComponent(new FileUploadBuilder().setCustomId('files').setMinValues(1).setMaxValues(3)),
      new LabelBuilder()
        .setLabel('Komentarz')
        .setTextInputComponent(new TextInputBuilder().setCustomId('text').setStyle(TextInputStyle.Short).setRequired(false).setMaxLength(200)),
    );
}

function reasonModal() {
  return new ModalBuilder()
    .setCustomId('form:close')
    .setTitle('Zamknięcie ticketu')
    .addLabelComponents(
      new LabelBuilder().setLabel('Powód').setTextInputComponent(new TextInputBuilder().setCustomId('reason').setStyle(TextInputStyle.Paragraph).setMaxLength(400)),
    );
}

function vouchModal(id) {
  return new ModalBuilder()
    .setCustomId(`form:vouch:${id}`)
    .setTitle('Twoja opinia')
    .addLabelComponents(
      new LabelBuilder()
        .setLabel('Komentarz (opcjonalnie)')
        .setDescription('Pojawi się na kanale z opiniami')
        .setTextInputComponent(new TextInputBuilder().setCustomId('text').setStyle(TextInputStyle.Paragraph).setRequired(false).setMaxLength(300)),
    );
}

function calcResult(from, to, amount) {
  const { fee, receive } = calc(from, to, amount);
  const c = box(config.colors.info);
  c.addTextDisplayComponents(text('### 🧮 Wynik'));
  c.addSeparatorComponents(line());
  c.addTextDisplayComponents(
    text(`Wysyłasz **${zl(amount)}** przez ${mName(from)}\nOtrzymasz **${zl(receive)}** na ${mName(to)}\n-# prowizja ${fee}%`),
  );
  return eph(c);
}

// ticket

function ticketView(t, member) {
  const st = STATUS[t.status] || STATUS.new;
  const c = box(st.color);
  const title = t.type === 'exchange' ? `${config.exchangeEmoji} Wymiana #${num(t.id)}` : `${t.emoji} ${t.label} #${num(t.id)}`;
  head(c, `### ${title}\n-# otwarty ${ts(t.createdAt)}`, member?.displayAvatarURL?.({ size: 128 }));
  c.addSeparatorComponents(line());
  c.addTextDisplayComponents(
    text(
      [
        `**Klient:** <@${t.owner}>`,
        `**Obsługa:** ${t.claimedBy ? `<@${t.claimedBy}>` : '-'}`,
        `**Status:** ${st.icon} ${st.label}`,
      ].join('\n'),
    ),
  );
  c.addSeparatorComponents(line());

  if (t.type === 'exchange') {
    c.addTextDisplayComponents(
      text(
        [
          `**Wysyłasz:** ${mName(t.from)} • \`${zl(t.amount)}\``,
          `**Otrzymujesz:** ${mName(t.to)} • \`${zl(t.receive)}\``,
          `-# prowizja ${t.fee}%`,
        ].join('\n'),
      ),
    );
    const label = m(t.to).payout || 'Dane do wypłaty';
    c.addTextDisplayComponents(text(`**${label}:**\n\`\`\`\n${plain(t.payout) || 'podasz na tickecie'}\n\`\`\``));
    if (t.note) c.addTextDisplayComponents(text(`**Uwagi:**\n> ${plain(t.note).replace(/\n/g, '\n> ')}`));
  } else {
    const parts = t.questions.map((q) => {
      const v = plain(t.answers[q.id]) || '-';
      return q.style === 'long' ? `**${q.label}:**\n\`\`\`\n${v}\n\`\`\`` : `**${q.label}:** ${v}`;
    });
    c.addTextDisplayComponents(text(parts.join('\n')));
  }

  c.addSeparatorComponents(line());
  const buttons = [
    new ButtonBuilder()
      .setCustomId('t:claim')
      .setLabel(t.claimedBy ? 'Przejęty' : 'Przejmij')
      .setEmoji('🙋')
      .setStyle(t.claimedBy ? ButtonStyle.Secondary : ButtonStyle.Primary)
      .setDisabled(!!t.claimedBy),
  ];
  if (t.type === 'exchange') buttons.push(new ButtonBuilder().setCustomId('t:proof').setLabel('Wyślij potwierdzenie').setEmoji('🧾').setStyle(ButtonStyle.Secondary));
  buttons.push(new ButtonBuilder().setCustomId('t:close').setLabel('Zamknij').setEmoji('🔒').setStyle(ButtonStyle.Danger));

  const opts = [];
  if (t.type === 'exchange') {
    for (const key of ['waiting', 'paid', 'done', 'cancel']) {
      if (t.status !== key) opts.push({ label: STATUS[key].label, value: `status:${key}`, emoji: emojiOf(STATUS[key].icon), description: 'Zmień status (obsługa)' });
    }
  }
  opts.push(
    { label: 'Przypomnij klientowi', value: 'remind', emoji: emojiOf('🔔') },
    { label: 'Dodaj osobę', value: 'add', emoji: emojiOf('➕') },
    { label: 'Usuń osobę', value: 'remove', emoji: emojiOf('➖') },
    { label: 'Transkrypt', value: 'transcript', emoji: emojiOf('📄') },
    { label: 'Zamknij z powodem', value: 'close_reason', emoji: emojiOf('📝') },
  );
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(...buttons),
    new ActionRowBuilder().addComponents(new StringSelectMenuBuilder().setCustomId('t:menu').setPlaceholder('Zarządzaj ticketem').addOptions(opts)),
  );
  return footer(c, `ticket #${num(t.id)}`);
}

function closeConfirm() {
  const c = box(config.colors.bad);
  c.addTextDisplayComponents(text('Na pewno zamknąć ticket?'));
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(
      new ButtonBuilder().setCustomId('close:yes').setLabel('Zamknij').setStyle(ButtonStyle.Danger),
      new ButtonBuilder().setCustomId('close:why').setLabel('Z powodem').setStyle(ButtonStyle.Secondary),
      new ButtonBuilder().setCustomId('close:no').setLabel('Anuluj').setStyle(ButtonStyle.Secondary),
    ),
  );
  return send(c);
}

function userPicker(kind) {
  const c = box();
  c.addTextDisplayComponents(text(kind === 'add' ? 'Kogo dodać do ticketu?' : 'Kogo usunąć z ticketu?'));
  c.addActionRowComponents(new ActionRowBuilder().addComponents(new UserSelectMenuBuilder().setCustomId(`pick:${kind}`).setPlaceholder('Wybierz osobę')));
  return eph(c);
}

function summaryLines(t) {
  const lines = [`**Klient:** <@${t.owner}> \`${t.owner}\``];
  if (t.type === 'exchange') lines.push(`**Wymiana:** ${mName(t.from)} → ${mName(t.to)}`, `**Kwota:** ${zl(t.amount)} → ${zl(t.receive)} (${t.fee}%)`);
  else lines.push(`**Kategoria:** ${t.label}`);
  lines.push(`**Status:** ${(STATUS[t.status] || STATUS.new).icon} ${(STATUS[t.status] || STATUS.new).label}`);
  lines.push(`**Obsługa:** ${t.claimedBy ? `<@${t.claimedBy}>` : '-'}`);
  return lines;
}

function logOpen(t, channelId) {
  const c = box(config.colors.ok);
  c.addTextDisplayComponents(text(`**Nowy ticket #${num(t.id)}**\n${summaryLines(t).join('\n')}\n**Kanał:** <#${channelId}>`));
  return send(c, { allowedMentions: { parse: [] } });
}

function logClose(t, by, reason, fileName) {
  const c = box(config.colors.bad);
  c.addTextDisplayComponents(text(`**Zamknięto ticket #${num(t.id)}**\n${summaryLines(t).join('\n')}\n**Zamknął:** <@${by}>\n**Powód:** ${plain(reason) || '-'}`));
  c.addFileComponents(new FileBuilder().setURL(`attachment://${fileName}`));
  return c;
}

function dmClosed(t, guildId, guildName, reason, fileName) {
  const c = box();
  c.addTextDisplayComponents(text(`### Ticket #${num(t.id)} zamknięty\n-# ${guildName}`));
  c.addSeparatorComponents(line());
  const lines = [];
  if (t.type === 'exchange') lines.push(`${mName(t.from)} → ${mName(t.to)} • ${zl(t.amount)}`, `Status: ${(STATUS[t.status] || STATUS.new).label}`);
  if (reason) lines.push(`Powód: ${plain(reason)}`);
  if (lines.length) c.addTextDisplayComponents(text(lines.join('\n')));
  c.addTextDisplayComponents(text('Jak oceniasz obsługę? Kliknij liczbę gwiazdek.'));
  c.addActionRowComponents(
    new ActionRowBuilder().addComponents(
      [1, 2, 3, 4, 5].map((n) =>
        new ButtonBuilder()
          .setCustomId(`rate:${guildId}:${t.id}:${n}`)
          .setLabel(String(n))
          .setEmoji('⭐')
          .setStyle(n >= 4 ? ButtonStyle.Success : ButtonStyle.Secondary),
      ),
    ),
  );
  c.addSeparatorComponents(line());
  c.addTextDisplayComponents(text('-# Zapis rozmowy w pliku poniżej'));
  c.addFileComponents(new FileBuilder().setURL(`attachment://${fileName}`));
  return c;
}

function vouchView(h, n, comment, avatar) {
  const c = box(config.colors.ok);
  head(c, `### ${'⭐'.repeat(n)}${'☆'.repeat(5 - n)}\n${comment ? `> ${plain(comment).replace(/\n/g, '\n> ')}` : '-# bez komentarza'}`, avatar);
  c.addSeparatorComponents(line());
  const lines = [];
  if (h.type === 'exchange') lines.push(`**Wymiana:** ${mName(h.from)} → ${mName(h.to)} • \`${zl(h.amount)}\``);
  lines.push(`**Klient:** <@${h.owner}>`);
  if (h.claimedBy) lines.push(`**Obsługa:** <@${h.claimedBy}>`);
  c.addTextDisplayComponents(text(lines.join('\n')));
  return send(footer(c, `#${num(h.id)}`), { allowedMentions: { parse: [] } });
}

function statsView(title, s) {
  const c = box();
  c.addTextDisplayComponents(text(`### 📈 ${title}`));
  c.addSeparatorComponents(line());
  c.addTextDisplayComponents(
    text(
      [
        `Zrealizowane wymiany: **${s.done}**`,
        `Obrót: **${zl(s.volume)}**`,
        `Średnia ocena: **${s.rating ? s.rating.toFixed(2).replace('.', ',') + ' ⭐' : 'brak'}**${s.votes ? ` (${s.votes})` : ''}`,
        s.open != null ? `Otwarte tickety: **${s.open}**` : null,
      ]
        .filter(Boolean)
        .join('\n'),
    ),
  );
  return c;
}

module.exports = {
  V2, EPH, STATUS, methods, m, mName, feeFor, calc, zl, parseAmount, now, num, plain,
  text, line, box, footer, send, eph, notice, small,
  panel, ratesView, exchangeModal, calcModal, categoryModal, proofModal, reasonModal, vouchModal, calcResult,
  ticketView, closeConfirm, userPicker, logOpen, logClose, dmClosed, vouchView, statsView,
};
