const { SlashCommandBuilder, PermissionFlagsBits } = require('discord.js');
const { methods } = require('./config');

const choices = Object.entries(methods).map(([value, m]) => ({ name: m.label, value }));
const admin = PermissionFlagsBits.Administrator;
const staff = PermissionFlagsBits.ManageMessages;

module.exports = [
  new SlashCommandBuilder().setName('panel').setDescription('Wysyła panel ticketów na ten kanał').setDefaultMemberPermissions(admin),

  new SlashCommandBuilder().setName('kursy').setDescription('Aktualne prowizje i limity'),

  new SlashCommandBuilder()
    .setName('prowizja')
    .setDescription('Zmiana prowizji dla pary metod')
    .setDefaultMemberPermissions(admin)
    .addSubcommand((s) =>
      s
        .setName('ustaw')
        .setDescription('Ustaw prowizję')
        .addStringOption((o) => o.setName('z').setDescription('Wysyłana metoda').setRequired(true).addChoices(...choices))
        .addStringOption((o) => o.setName('na').setDescription('Otrzymywana metoda').setRequired(true).addChoices(...choices))
        .addNumberOption((o) => o.setName('procent').setDescription('Prowizja w %').setRequired(true).setMinValue(0).setMaxValue(90)),
    )
    .addSubcommand((s) =>
      s
        .setName('reset')
        .setDescription('Przywróć prowizję z configu')
        .addStringOption((o) => o.setName('z').setDescription('Wysyłana metoda').setRequired(true).addChoices(...choices))
        .addStringOption((o) => o.setName('na').setDescription('Otrzymywana metoda').setRequired(true).addChoices(...choices)),
    ),

  new SlashCommandBuilder()
    .setName('ticket')
    .setDescription('Zarządzanie ticketem')
    .addSubcommand((s) => s.setName('dodaj').setDescription('Dodaj osobę').addUserOption((o) => o.setName('osoba').setDescription('Kogo').setRequired(true)))
    .addSubcommand((s) => s.setName('usun').setDescription('Usuń osobę').addUserOption((o) => o.setName('osoba').setDescription('Kogo').setRequired(true)))
    .addSubcommand((s) => s.setName('nazwa').setDescription('Zmień nazwę kanału').addStringOption((o) => o.setName('nazwa').setDescription('Nowa nazwa').setRequired(true).setMaxLength(90)))
    .addSubcommand((s) => s.setName('zamknij').setDescription('Zamknij ticket').addStringOption((o) => o.setName('powod').setDescription('Powód').setMaxLength(400))),

  new SlashCommandBuilder()
    .setName('blacklist')
    .setDescription('Blokada otwierania ticketów')
    .setDefaultMemberPermissions(staff)
    .addSubcommand((s) =>
      s
        .setName('dodaj')
        .setDescription('Zablokuj osobę')
        .addUserOption((o) => o.setName('osoba').setDescription('Kogo').setRequired(true))
        .addStringOption((o) => o.setName('powod').setDescription('Powód').setMaxLength(200)),
    )
    .addSubcommand((s) => s.setName('usun').setDescription('Odblokuj osobę').addUserOption((o) => o.setName('osoba').setDescription('Kogo').setRequired(true)))
    .addSubcommand((s) => s.setName('lista').setDescription('Pokaż zablokowanych')),

  new SlashCommandBuilder()
    .setName('statystyki')
    .setDescription('Statystyki wymian')
    .setDefaultMemberPermissions(staff)
    .addUserOption((o) => o.setName('osoba').setDescription('Statystyki konkretnej osoby z obsługi')),
].map((c) => c.toJSON());
