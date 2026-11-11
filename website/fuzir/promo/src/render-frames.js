// renderuje klatki filmu: node render-frames.js <katalog> <workerów>
const { chromium } = require('playwright');
const fs = require('fs');
const dir = process.argv[2];
const W = Number(process.argv[3] || 3);
const FPS = 30;
(async () => {
  fs.mkdirSync(`${dir}/frames`, { recursive: true });
  const b = await chromium.launch();
  const probe = await b.newPage({ viewport: { width: 1920, height: 1080 } });
  await probe.goto('file://' + dir + '/promo.html');
  const dur = await probe.evaluate(() => window.READY);
  await probe.close();
  const total = Math.ceil(dur * FPS);
  console.log('klatek', total);
  let done = 0;
  const t0 = Date.now();
  await Promise.all(
    Array.from({ length: W }, async (_, w) => {
      const p = await b.newPage({ viewport: { width: 1920, height: 1080 } });
      await p.goto('file://' + dir + '/promo.html');
      await p.evaluate(() => window.READY);
      for (let f = w; f < total; f += W) {
        await p.evaluate((t) => window.render(t), f / FPS);
        await p.screenshot({ path: `${dir}/frames/f${String(f).padStart(4, '0')}.jpg`, type: 'jpeg', quality: 93 });
        if (++done % 100 === 0) console.log(done, '/', total, Math.round((Date.now() - t0) / 1000) + 's');
      }
    }),
  );
  await b.close();
  console.log('gotowe', Math.round((Date.now() - t0) / 1000) + 's');
})();
