// zapis ticketu do html (wygląd jak discord)
const config = require('./config');

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const hex = (n) => '#' + Number(n).toString(16).padStart(6, '0');
const fmtDate = (d, withSec = false) =>
  d.toLocaleString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', ...(withSec ? { second: '2-digit' } : {}) });

function flatten(comps, out = []) {
  for (const c of comps || []) {
    if (c.type === 10 && c.content) out.push({ kind: 'text', content: c.content });
    else if (c.type === 14) out.push({ kind: 'sep' });
    else if (c.type === 12) for (const it of c.items || []) out.push({ kind: 'img', url: it.media?.url });
    else if (c.type === 3 || c.type === 5) out.push({ kind: 'select', label: c.placeholder || 'Menu' });
    else if (c.type === 13) out.push({ kind: 'file', name: (c.file?.url || '').replace('attachment://', '') });
    else if (c.type === 2) out.push({ kind: 'button', label: `${c.emoji?.name ? c.emoji.name + ' ' : ''}${c.label || ''}`, style: c.style });
    if (c.type === 17) out.push({ kind: 'container', color: c.accent_color, items: flatten(c.components) });
    else if (c.components) flatten(c.components, out);
  }
  return out;
}

function makeMd(ctx) {
  const inline = (s) => {
    let h = esc(s);
    const codes = [];
    h = h.replace(/`([^`\n]+)`/g, (_, c) => { codes.push(c); return `\u0000${codes.length - 1}\u0000`; });
    h = h
      .replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')
      .replace(/__(.+?)__/g, '<u>$1</u>')
      .replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<i>$2</i>')
      .replace(/~~(.+?)~~/g, '<s>$1</s>')
      .replace(/&lt;@!?(\d+)&gt;/g, (_, id) => `<span class="mention">@${esc(ctx.user(id))}</span>`)
      .replace(/&lt;@&amp;(\d+)&gt;/g, (_, id) => { const r = ctx.role(id); return `<span class="mention role" style="--rc:${r.color}">@${esc(r.name)}</span>`; })
      .replace(/&lt;#(\d+)&gt;/g, (_, id) => `<span class="mention">#${esc(ctx.channel(id))}</span>`)
      .replace(/&lt;t:(\d+)(?::[a-zA-Z])?&gt;/g, (_, ts) => `<span class="ts">${fmtDate(new Date(ts * 1000))}</span>`)
      .replace(/(https?:\/\/[^\s<]+)/g, '<a href="$1">$1</a>');
    return h.replace(/\u0000(\d+)\u0000/g, (_, i) => `<code>${codes[i]}</code>`);
  };
  return (src) => {
    const out = [];
    const parts = String(src).split(/```(?:\w*\n)?([\s\S]*?)```/g);
    parts.forEach((p, idx) => {
      if (idx % 2) { out.push(`<pre>${esc(p.replace(/\n$/, ''))}</pre>`); return; }
      let quote = [];
      const flushQ = () => { if (quote.length) out.push(`<blockquote>${quote.join('<br>')}</blockquote>`); quote = []; };
      for (const line of p.split('\n')) {
        if (/^>\s?/.test(line)) { quote.push(inline(line.replace(/^>\s?/, ''))); continue; }
        flushQ();
        if (!line.trim()) continue;
        if (/^-#\s/.test(line)) out.push(`<div class="sub">${inline(line.slice(3))}</div>`);
        else if (/^###?\s/.test(line)) out.push(`<div class="h">${inline(line.replace(/^#+\s/, ''))}</div>`);
        else out.push(`<div>${inline(line)}</div>`);
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
      if (it.kind === 'img') return it.url && !it.url.startsWith('attachment://') ? `<img class="media" src="${esc(it.url)}" alt="">` : '<div class="media ph">🖼️ obraz</div>';
      if (it.kind === 'select') return `<div class="select"><span>${esc(it.label)}</span><span>⌄</span></div>`;
      if (it.kind === 'file') return `<div class="file">📄 ${esc(it.name || 'plik')}</div>`;
      if (it.kind === 'button') return `<span class="btn s${it.style}">${esc(it.label)}</span>`;
      if (it.kind === 'container') return `<div class="container" style="--ac:${it.color != null ? hex(it.color) : 'var(--line)'}">${renderItems(it.items, md)}</div>`;
      return '';
    })
    .join('');
}

function renderHtml({ channel, messages, ticket, closedBy, reason }) {
  const guild = channel.guild;
  const ctx = {
    user: (id) => guild?.members.cache.get(id)?.displayName || channel.client?.users?.cache?.get(id)?.username || id,
    role: (id) => { const r = guild?.roles.cache.get(id); return { name: r?.name || 'rola', color: r?.color ? hex(r.color) : '#60a5fa' }; },
    channel: (id) => guild?.channels.cache.get(id)?.name || id,
  };
  const md = makeMd(ctx);
  const kind = ticket?.type === 'exchange' ? 'Wymiana' : ticket?.label || 'Ticket';
  const people = new Map();

  let last = null;
  const rows = messages.map((m) => {
    const a = m.author;
    people.set(a.id, a);
    const grouped = last && last.author.id === a.id && m.createdTimestamp - last.createdTimestamp < 5 * 60000;
    last = m;
    const name = m.member?.displayName || a.globalName || a.username;
    const color = m.member?.displayHexColor && m.member.displayHexColor !== '#000000' ? m.member.displayHexColor : a.bot ? '#60a5fa' : '#f2f3f5';
    let body = m.content ? `<div class="content">${md(m.content)}</div>` : '';
    for (const e of m.embeds || []) {
      body += `<div class="container" style="--ac:${e.hexColor || 'var(--line)'}">${e.title ? `<div class="h">${esc(e.title)}</div>` : ''}${e.description ? `<div class="td">${md(e.description)}</div>` : ''}${(e.fields || []).map((f) => `<div class="td"><b>${esc(f.name)}</b>${md(f.value)}</div>`).join('')}</div>`;
    }
    const comps = (m.components || []).map((c) => (c.toJSON ? c.toJSON() : c));
    body += renderItems(flatten(comps), md);
    for (const att of m.attachments?.values?.() || []) {
      const isImg = /\.(png|jpe?g|gif|webp)(\?|$)/i.test(att.url) || att.contentType?.startsWith('image/');
      body += isImg
        ? `<a href="${esc(att.url)}"><img class="media" src="${esc(att.url)}" alt="${esc(att.name || '')}"></a>`
        : `<a class="file" href="${esc(att.url)}">📄 ${esc(att.name || att.url.split('/').pop())}</a>`;
    }
    const av = a.displayAvatarURL ? a.displayAvatarURL({ size: 64 }) : '';
    const time = fmtDate(m.createdAt);
    return grouped
      ? `<div class="msg g"><span class="gt">${m.createdAt.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' })}</span><div class="body">${body}</div></div>`
      : `<div class="msg"><img class="av" src="${esc(av)}" alt=""><div class="body"><div class="meta"><span class="name" style="color:${color}">${esc(name)}</span>${a.bot ? '<span class="tag">APL.</span>' : ''}<span class="time">${time}</span></div>${body}</div></div>`;
  });

  const stat = (k, v) => `<div class="stat"><span>${k}</span><b>${v}</b></div>`;
  const accent = hex(config.color);
  return `<!doctype html><html lang="pl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Transkrypt #${esc(ticket?.number || '')} | ${esc(config.name)}</title>
<style>
:root{--bg:#313338;--panel:#2b2d31;--deep:#1e1f22;--line:#3f4147;--text:#dbdee1;--dim:#949ba4;--white:#f2f3f5;--accent:${accent}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:15px/1.4 "gg sans","Noto Sans",Helvetica,Arial,sans-serif}
a{color:#00a8fc;text-decoration:none}a:hover{text-decoration:underline}
header{background:linear-gradient(120deg,#0b1a33,#0a0d16 70%);border-bottom:3px solid var(--accent);padding:28px 20px}
.hw{max-width:980px;margin:0 auto;display:flex;flex-wrap:wrap;gap:20px;align-items:center;justify-content:space-between}
.brand{font:800 13px/1 ui-monospace,Menlo,Consolas,monospace;letter-spacing:.18em;color:#93c5fd}
h1{margin:8px 0 4px;color:#fff;font-size:28px}h1 small{color:var(--dim);font-size:16px;font-weight:500}
.stats{display:flex;flex-wrap:wrap;gap:10px}.stat{background:rgba(0,0,0,.3);border:1px solid rgba(59,130,246,.35);border-radius:10px;padding:8px 12px;min-width:110px}
.stat span{display:block;font-size:11px;text-transform:uppercase;letter-spacing:.06em;color:var(--dim)}.stat b{color:#fff;font-size:14px}
main{max-width:980px;margin:0 auto;padding:16px 20px 40px}
.msg{display:flex;gap:16px;padding:6px 8px;border-radius:6px;margin-top:12px}.msg:hover{background:#2e3035}
.msg.g{margin-top:0;padding-top:1px;padding-bottom:1px}.gt{width:40px;flex:none;font-size:11px;color:transparent;text-align:right;padding-top:4px}.msg.g:hover .gt{color:var(--dim)}
.av{width:40px;height:40px;border-radius:50%;flex:none;background:var(--deep)}
.body{min-width:0;flex:1}.meta{display:flex;align-items:baseline;gap:6px;flex-wrap:wrap}.name{font-weight:600}.time{font-size:12px;color:var(--dim)}
.tag{background:#5865f2;color:#fff;font-size:10px;font-weight:600;padding:1px 5px;border-radius:4px}
.content,.td{overflow-wrap:anywhere}.td+.td{margin-top:6px}
.container{background:var(--panel);border:1px solid var(--line);border-left:4px solid var(--ac);border-radius:8px;padding:12px 14px;margin-top:6px;max-width:560px}
hr{border:0;border-top:1px solid var(--line);margin:10px 0}
pre{background:#383a40;border:1px solid #45474e;border-radius:6px;padding:10px 12px;margin:4px 0;font:600 14px/1.5 ui-monospace,Menlo,Consolas,monospace;color:#fff;white-space:pre-wrap}
code{background:#383a40;border:1px solid #45474e;border-radius:4px;padding:0 4px;font:13px ui-monospace,Menlo,Consolas,monospace}
blockquote{margin:2px 0;border-left:4px solid #4e5058;padding-left:12px;line-height:1.55}
b{color:var(--white)}.sub{font-size:12px;color:var(--dim)}.h{font-weight:700;color:#fff;font-size:16px}
.mention{background:rgba(88,101,242,.3);color:#c9cdfb;border-radius:3px;padding:0 2px}
.mention.role{background:color-mix(in srgb,var(--rc) 20%,transparent);color:var(--rc)}
.ts{background:rgba(255,255,255,.06);border-radius:3px;padding:0 3px}
.select{background:var(--deep);border:1px solid #45474e;border-radius:8px;padding:10px 12px;display:flex;justify-content:space-between;color:var(--dim)}
.btn{display:inline-block;margin:4px 6px 0 0;padding:5px 12px;border-radius:4px;color:#fff;font-size:13px;background:#4e5058}.btn.s1{background:#5865f2}.btn.s3{background:#248046}.btn.s4{background:#da373c}
.media{display:block;max-width:100%;border-radius:8px;margin-top:6px;max-height:340px}
.media.ph{background:linear-gradient(120deg,#0b1a33,#0a0d16);color:#93c5fd;padding:28px;text-align:center;font-weight:700}
.file{display:inline-flex;gap:8px;background:var(--panel);border:1px solid var(--line);border-radius:8px;padding:10px 14px;margin-top:6px}
footer{max-width:980px;margin:0 auto;padding:0 20px 30px;color:var(--dim);font-size:12px;border-top:1px solid var(--line);padding-top:16px}
</style></head><body>
<header><div class="hw"><div>
<div class="brand">${esc(config.name.toUpperCase())} • TRANSKRYPT</div>
<h1>Ticket #${esc(ticket?.number || '????')} <small>#${esc(channel.name)}</small></h1>
</div><div class="stats">
${stat('Rodzaj', esc(kind))}
${stat('Klient', esc(ticket ? ctx.user(ticket.owner) : '-'))}
${stat('Przejął', esc(ticket?.claimedBy ? ctx.user(ticket.claimedBy) : 'Nikt'))}
${stat('Zamknął', esc(closedBy || '-'))}
${stat('Wiadomości', messages.length)}
${stat('Uczestnicy', people.size)}
</div></div></header>
<main>${rows.join('\n')}</main>
<footer>${esc(config.name)} • otwarty ${ticket ? fmtDate(new Date(ticket.createdAt * 1000)) : '-'} • zamknięty ${fmtDate(new Date())} • powód: ${esc(reason || 'brak')}</footer>
</body></html>`;
}

module.exports = { renderHtml };
