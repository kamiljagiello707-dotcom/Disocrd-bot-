// renderuje klatki: node render60.js <katalog> <strona> <folder> <workerów>
const { chromium } = require('playwright');
const fs = require('fs');
const [dir, page, outDir, workers] = process.argv.slice(2);
const W = Number(workers || 3), FPS = 30;
(async () => {
  fs.mkdirSync(`${dir}/${outDir}`, { recursive: true });
  const b = await chromium.launch();
  const probe = await b.newPage({ viewport: { width: 1920, height: 1080 } });
  await probe.goto(`file://${dir}/${page}`);
  const dur = await probe.evaluate(() => window.READY);
  await probe.close();
  const total = Math.round(dur * FPS);
  console.log('klatek', total);
  let done = 0; const t0 = Date.now();
  await Promise.all(Array.from({ length: W }, async (_, w) => {
    const p = await b.newPage({ viewport: { width: 1920, height: 1080 } });
    await p.goto(`file://${dir}/${page}`);
    await p.evaluate(() => window.READY);
    for (let f = w; f < total; f += W) {
      await p.evaluate((t) => window.render(t), f / FPS);
      await p.screenshot({ path: `${dir}/${outDir}/f${String(f).padStart(4, '0')}.jpg`, type: 'jpeg', quality: 93 });
      if (++done % 200 === 0) console.log(done, '/', total, Math.round((Date.now() - t0) / 1000) + 's');
    }
  }));
  await b.close();
  console.log('gotowe', Math.round((Date.now() - t0) / 1000) + 's');
})();
