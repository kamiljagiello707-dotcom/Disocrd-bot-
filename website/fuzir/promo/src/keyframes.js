const { chromium } = require('playwright');
const dir = process.argv[2];
const times = process.argv.slice(3).map(Number);
(async () => {
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: 1920, height: 1080 } });
  const errs = []; p.on('pageerror', (e) => errs.push(e.message));
  await p.goto('file://' + dir + '/promo.html');
  const dur = await p.evaluate(() => window.READY);
  console.log('czas filmu', dur.toFixed(2), 's');
  for (const t of times) {
    await p.evaluate((x) => window.render(x), t);
    await p.screenshot({ path: `${dir}/kf-${t}.jpg`, type: 'jpeg', quality: 80 });
  }
  console.log('ERR', JSON.stringify(errs));
  await b.close();
})();
