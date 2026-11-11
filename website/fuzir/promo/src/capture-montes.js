const { chromium } = require('playwright');
const OUT = process.argv[2];
(async () => {
  const b = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const q = await b.newPage({ viewport: { width: 1280, height: 800 } });
  await q.goto('http://localhost:8090/montes/');
  await q.waitForFunction(() => !document.getElementById('loader'), null, { timeout: 30000 });
  await q.mouse.move(700, 400); await q.waitForTimeout(2500);
  await q.screenshot({ path: `${OUT}/montes-hero.jpg`, type: 'jpeg', quality: 86 });
  await q.evaluate(() => { const e = document.getElementById('sklep'); scrollTo(0, e.offsetTop + 260); });
  await q.waitForTimeout(3500);
  await q.screenshot({ path: `${OUT}/montes-shop.jpg`, type: 'jpeg', quality: 86 });
  await b.close();
})();
