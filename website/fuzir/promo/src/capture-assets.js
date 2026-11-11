// nagrywa materiał do filmu: przewijanie fuzir.tech + zrzuty projektów
const { chromium } = require('playwright');
const OUT = process.argv[2];
const B = 'http://localhost:8090';
(async () => {
  const b = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });

  // przewijanie fuzir.tech
  const p = await b.newPage({ viewport: { width: 1280, height: 800 } });
  await p.goto(B + '/fuzir/'); await p.waitForTimeout(3000);
  await p.mouse.move(820, 330);
  await p.waitForTimeout(800);
  const H = await p.evaluate(() => document.getElementById('o-mnie').offsetTop);
  const N = 170;
  for (let i = 0; i < N; i++) {
    const t = i / (N - 1);
    const e = t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    await p.evaluate((y) => window.scrollTo(0, y), Math.round(e * H));
    await p.waitForTimeout(90);
    await p.screenshot({ path: `${OUT}/scroll/f${String(i).padStart(3, '0')}.jpg`, type: 'jpeg', quality: 82 });
  }
  await p.evaluate(() => scrollTo(0, 0)); await p.waitForTimeout(1500);
  await p.screenshot({ path: `${OUT}/fuzir-hero.jpg`, type: 'jpeg', quality: 88 });

  // telefon
  const m = await b.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true });
  await m.goto(B + '/fuzir/'); await m.waitForTimeout(3500);
  await m.screenshot({ path: `${OUT}/fuzir-mobile.jpg`, type: 'jpeg', quality: 86 });

  const shot = async (url, file, prep, wait = 3500) => {
    const q = await b.newPage({ viewport: { width: 1280, height: 800 } });
    await q.goto(B + url); await q.waitForTimeout(wait);
    if (prep) await prep(q);
    await q.screenshot({ path: `${OUT}/${file}`, type: 'jpeg', quality: 86 });
    await q.close();
  };
  await shot('/montes/', 'montes-hero.jpg', async (q) => { await q.mouse.move(700, 400); await q.waitForTimeout(800); }, 4500);
  await shot('/montes/', 'montes-shop.jpg', async (q) => { await q.evaluate(() => { const e = document.getElementById('sklep'); scrollTo(0, e.offsetTop + 260); }); await q.waitForTimeout(2600); }, 4500);
  await shot('/bublebot/', 'bublebot.jpg', null, 4000);
  await shot('/edzio/', 'edzio-login.jpg', null, 1500);
  await shot('/edzio/', 'edzio-grades.jpg', async (q) => {
    await q.click('[data-a=login][data-r=nauczyciel]'); await q.waitForTimeout(500);
    await q.evaluate(() => location.hash = '#/ocenianie'); await q.waitForTimeout(1200);
  }, 1500);
  await shot('/edzio/', 'edzio-student.jpg', async (q) => {
    await q.evaluate(() => { localStorage.removeItem('edzio-session'); }); await q.reload(); await q.waitForTimeout(800);
    await q.click('[data-a=login][data-r=uczen]'); await q.waitForTimeout(1500);
  }, 1500);
  await b.close();
  console.log('gotowe');
})();
