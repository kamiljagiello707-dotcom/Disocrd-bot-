const fs = require('fs');
const path = require('path');

const file = path.join(__dirname, '..', 'data', 'db.json');
const fresh = () => ({
  counter: 0,
  tickets: {},
  history: {},
  fees: {},
  blacklist: {},
  stats: { done: 0, volume: 0, staff: {} },
});

let data = fresh();
try {
  data = { ...fresh(), ...JSON.parse(fs.readFileSync(file, 'utf8')) };
} catch {}

let timer = null;
function flush() {
  clearTimeout(timer);
  timer = null;
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file + '.tmp', JSON.stringify(data, null, 2));
  fs.renameSync(file + '.tmp', file);
}
function save() {
  if (!timer) timer = setTimeout(flush, 500);
}

process.on('exit', () => timer && flush());
for (const sig of ['SIGINT', 'SIGTERM']) process.on(sig, () => process.exit(0));

module.exports = { data, save, flush };
