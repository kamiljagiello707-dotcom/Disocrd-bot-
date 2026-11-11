const { chromium } = require('playwright');
const dir = process.argv[2];
const page = process.argv[3];
const times = process.argv.slice(4).map(Number);
(async () => {
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: 1920, height: 1080 } });
  const errs = []; p.on('pageerror', (e) => errs.push(e.message));
  await p.goto('file://' + dir + '/' + page);
  const dur = await p.evaluate(() => window.READY);
  console.log('czas filmu', dur.toFixed(2), 's');
  for (const t of times) {
    await p.evaluate((x) => window.render(x), t);
    await p.screenshot({ path: `${dir}/k60-${t}.jpg`, type: 'jpeg', quality: 78 });
  }
  console.log('ERR', JSON.stringify(errs));
  await b.close();
})();
