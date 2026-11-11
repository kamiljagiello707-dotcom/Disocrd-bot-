// dodatkowy materiał do wersji 60 s
const { chromium } = require('playwright');
const fs = require('fs');
const OUT = process.argv[2];
const PREVIEW = process.argv[3];
(async () => {
  const b = await chromium.launch({ args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const meta = {};

  // konfigurator na fuzir.tech + pozycje przycisków
  const p = await b.newPage({ viewport: { width: 1280, height: 800 } });
  await p.goto('http://localhost:8090/fuzir/'); await p.waitForTimeout(2500);
  await p.evaluate(() => { const e = document.getElementById('zlecenie'); scrollTo(0, e.offsetTop + 120); });
  await p.waitForTimeout(1800);
  // stan początkowy bez zaznaczeń
  await p.evaluate(() => { document.querySelectorAll('#qType button,#qFeat button,#qTime button,#qBud button').forEach((x) => x.classList.remove('on')); });
  const pick = async (sel) => p.evaluate((s) => { const r = document.querySelector(s).getBoundingClientRect(); return [r.left + r.width / 2, r.top + r.height / 2]; }, sel);
  meta.cfg = {
    shop: await pick('#qType [data-id="shop"]'),
    anim: await pick('#qFeat [data-id="anim"]'),
    cart: await pick('#qFeat [data-id="cart"]'),
    dc: await pick('#qFeat [data-id="dc"]'),
    copy: await pick('#bCopy'),
  };
  await p.screenshot({ path: `${OUT}/cfg-0.jpg`, type: 'jpeg', quality: 86 });
  await p.click('#qType [data-id="shop"]'); await p.waitForTimeout(500);
  await p.screenshot({ path: `${OUT}/cfg-1.jpg`, type: 'jpeg', quality: 86 });
  await p.click('#qFeat [data-id="cart"]'); await p.waitForTimeout(500);
  await p.screenshot({ path: `${OUT}/cfg-2.jpg`, type: 'jpeg', quality: 86 });
  await p.click('#qFeat [data-id="dc"]'); await p.waitForTimeout(500);
  await p.fill('#qDesc', 'Sklep z walutą do serwera Minecraft');
  await p.evaluate(() => { document.activeElement.blur(); const e = document.getElementById('zlecenie'); scrollTo(0, e.offsetTop + 120); });
  await p.waitForTimeout(900);
  await p.screenshot({ path: `${OUT}/cfg-3.jpg`, type: 'jpeg', quality: 86 });

  // podgląd bota exchange
  const q = await b.newPage({ viewport: { width: 1280, height: 800 } });
  await q.goto('file://' + PREVIEW + '#panel'); await q.waitForTimeout(600);
  await q.evaluate(() => { const i = document.querySelector('.intro'); if (i) i.remove(); });
  await q.screenshot({ path: `${OUT}/bot-panel.jpg`, type: 'jpeg', quality: 88 });
  await q.goto('file://' + PREVIEW + '#ticket'); await q.waitForTimeout(600);
  await q.screenshot({ path: `${OUT}/bot-ticket.jpg`, type: 'jpeg', quality: 88 });
  await q.evaluate(() => { document.querySelector('#feed').scrollTop = 900; }); await q.waitForTimeout(200);
  await q.screenshot({ path: `${OUT}/bot-proof.jpg`, type: 'jpeg', quality: 88 });
  await q.goto('file://' + PREVIEW + '#form'); await q.waitForTimeout(600);
  await q.screenshot({ path: `${OUT}/bot-form.jpg`, type: 'jpeg', quality: 88 });

  fs.writeFileSync(`${OUT}/meta.json`, JSON.stringify(meta, null, 1));
  await b.close();
  console.log(JSON.stringify(meta));
})();
