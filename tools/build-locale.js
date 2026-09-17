/* ============================================================
   KotlinAZ — tərcümə overlay-lərindən dil aktivləri (T3.2)

   Bazanı (assets/content/az) oxuyur, localization/<lc>/ overlay-ini
   ID üzrə tətbiq edir və assets/content/<lc>/ yazır. Çatışmayan və ya
   boş sahə bazadan qalır («fallback»). search.json tərcümə olunmuş
   məzmundan yenidən qurulur. Sonda assets/content/locales.json
   yenilənir.

   İşlətmək:  node tools/build-locale.js en
              node tools/build-locale.js --all
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');
const L = require('./content-lib');

const args = process.argv.slice(2);
const hamisi = args.includes('--all');
const secilen = args.filter((a) => !a.startsWith('--'));

const localelar = hamisi ? L.localeSiyahisi() : secilen;
if (!localelar.length) {
  console.error('Istifade: node tools/build-locale.js <locale> | --all');
  process.exit(1);
}

function localeQur(locale) {
  const qovluq = path.join(L.LOCALIZATION, locale);
  if (!fs.existsSync(qovluq)) {
    console.error('XETA: overlay qovlugu yoxdur: ' + qovluq + '  (node tools/new-locale.js ' + locale + ')');
    return false;
  }
  const hedef = path.join(L.AKTIV_KOKU, locale);
  fs.mkdirSync(hedef, { recursive: true });

  console.log(locale + ':');
  let content = null;
  L.FAYLLAR.forEach((ad) => {
    const baza = L.bazaOxu(ad);
    const overlayFayli = L.overlayYolu(locale, ad);
    const overlay = fs.existsSync(overlayFayli) ? JSON.parse(fs.readFileSync(overlayFayli, 'utf8')) : {};
    const yarpaqlar = L.YARPAQ[ad](baza);
    let tetbiq = 0; let fallback = 0;
    yarpaqlar.forEach((y) => {
      const v = L.overlayOxu(overlay, y.acar);
      if (L.bosdur(v)) { fallback++; return; }
      y.qoy(v); tetbiq++;
    });
    if (ad === 'content') content = baza;
    const yol = path.join(hedef, ad + '.json');
    fs.writeFileSync(yol, JSON.stringify(baza), 'utf8');
    const kb = (fs.statSync(yol).size / 1024).toFixed(1);
    console.log('  ' + (ad + '.json').padEnd(18) + kb.padStart(7) + ' KB   tercume ' + String(tetbiq).padStart(5) + '   fallback ' + String(fallback).padStart(5));
  });

  // Axtarış indeksi tərcümə olunmuş məzmundan — az-dakı ilə eyni qayda.
  const indeks = { docs: L.axtarisIndeksi(content.sections) };
  const yol = path.join(hedef, 'search.json');
  fs.writeFileSync(yol, JSON.stringify(indeks), 'utf8');
  console.log('  ' + 'search.json'.padEnd(18) + (fs.statSync(yol).size / 1024).toFixed(1).padStart(7) + ' KB');
  return true;
}

let ugursuz = 0;
localelar.forEach((lc) => { if (!localeQur(lc)) ugursuz++; });

// Tətbiqin bildiyi dillər: baza + aktivi mövcud olan overlay dilləri.
const movcud = [L.BAZA].concat(
  L.localeSiyahisi().filter((lc) => fs.existsSync(path.join(L.AKTIV_KOKU, lc, 'content.json')))
);
fs.writeFileSync(path.join(L.AKTIV_KOKU, 'locales.json'), JSON.stringify({ base: L.BAZA, locales: movcud }) + '\n', 'utf8');
console.log('');
console.log('locales.json: ' + movcud.join(', '));

if (ugursuz) process.exit(1);
