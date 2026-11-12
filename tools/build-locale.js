/* ============================================================
   KotlinAZ — tərcümə overlay-lərindən dil aktivləri (T3.2, T4.7)

   Bazanı (assets/content/az) oxuyur, localization/<lc>/ overlay-ini
   ID üzrə tətbiq edir və dil aktivlərini yazır. Çatışmayan və ya boş
   sahə bazadan qalır («fallback»). Axtarış indeksi yazılmır — tətbiq
   onu yüklənmiş məzmundan özü qurur (T3.5).

   Dil qapısı (T4.7): 100 % tərcümə olunmuş dil app/src/main/assets-ə
   (release), yarımçıq dil app/src/debug/assets-ə yazılır — yarımçıq
   tərcümə istifadəçiyə çatmır, amma tərcüməçi debug build-də öz işini
   görür. Dil 100 %-ə çatanda növbəti işlədilmədə özü main-ə keçir.

   Sonda yenilənir:
     main/assets/content/locales.json    baza + tam dillər (release)
     debug/assets/content/locales.json   + yarımçıq dillər (debug)
     main/res/xml/locales_config.xml     release-in sistem menyusu

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
  const s = L.tamamlanma(locale);
  const tam = L.tamdir(locale);
  const koku = L.aktivKoku(locale);
  const hedef = path.join(koku, locale);
  // Dil status dəyişibsə (yarımçıq ↔ tam) köhnə yerdəki surət silinir —
  // eyni dil iki source set-də qalsa debug-da hansının qalib gəldiyi bilinməz.
  const diger = path.join(tam ? L.AKTIV_KOKU_DEBUG : L.AKTIV_KOKU, locale);
  if (fs.existsSync(diger)) fs.rmSync(diger, { recursive: true, force: true });
  fs.mkdirSync(hedef, { recursive: true });

  const faiz = s.cemi ? Math.floor((s.var / s.cemi) * 100) : 100;
  console.log(locale + ': ' + faiz + '% (' + s.var + '/' + s.cemi + ') → ' + (tam ? 'release (main)' : 'yalniz debug'));
  L.FAYLLAR.forEach((ad) => {
    const baza = L.bazaOxu(ad);
    const overlayFayli = L.overlayYolu(locale, ad);
    const overlay = fs.existsSync(overlayFayli) ? JSON.parse(fs.readFileSync(overlayFayli, 'utf8')) : {};
    const yarpaqlar = L.YARPAQ[ad](baza);
    let tetbiq = 0; let fallback = 0; let redd = 0;
    yarpaqlar.forEach((y) => {
      const v = L.overlayOxu(overlay, y.acar);
      if (L.bosdur(v)) { fallback++; return; }
      // Şərhdən kənarda dəyişmiş kod APK-ya düşmür — baza qalır (T4.0).
      // Səbəbi validator göstərir: node tools/validate-locales.js <lc>
      if (y.yoxla && y.yoxla(v)) { redd++; fallback++; return; }
      y.qoy(v); tetbiq++;
    });
    const yol = path.join(hedef, ad + '.json');
    fs.writeFileSync(yol, JSON.stringify(baza), 'utf8');
    const kb = (fs.statSync(yol).size / 1024).toFixed(1);
    console.log('  ' + (ad + '.json').padEnd(18) + kb.padStart(7) + ' KB   tercume ' + String(tetbiq).padStart(5) + '   fallback ' + String(fallback).padStart(5) +
      (redd ? '   ! kodu deyismis ' + redd + ' (bax validate-locales)' : ''));
  });

  // Köhnə boru xəttinin search.json-u qalıbsa silinir (T3.5).
  const kohneIndeks = path.join(hedef, 'search.json');
  if (fs.existsSync(kohneIndeks)) fs.rmSync(kohneIndeks);
  return true;
}

let ugursuz = 0;
localelar.forEach((lc) => { if (!localeQur(lc)) ugursuz++; });

// Dil siyahıları aktivlərin faktiki yerindən qurulur — yalnız bir dil
// qurulanda da digər dillər siyahıdan düşmür.
const aktivVar = (koku, lc) => fs.existsSync(path.join(koku, lc, 'content.json'));
const release = [L.BAZA].concat(L.localeSiyahisi().filter((lc) => aktivVar(L.AKTIV_KOKU, lc)));
const debugYalniz = L.localeSiyahisi().filter((lc) => aktivVar(L.AKTIV_KOKU_DEBUG, lc));

fs.writeFileSync(path.join(L.AKTIV_KOKU, 'locales.json'), JSON.stringify({ base: L.BAZA, locales: release }) + '\n', 'utf8');
const debugSiyahi = path.join(L.AKTIV_KOKU_DEBUG, 'locales.json');
if (debugYalniz.length) {
  fs.mkdirSync(L.AKTIV_KOKU_DEBUG, { recursive: true });
  fs.writeFileSync(debugSiyahi, JSON.stringify({ base: L.BAZA, locales: release.concat(debugYalniz) }) + '\n', 'utf8');
} else if (fs.existsSync(debugSiyahi)) {
  fs.rmSync(debugSiyahi);
}
fs.writeFileSync(L.LOCALE_CONFIG, L.localeConfigMetni(release), 'utf8');

console.log('');
console.log('release: ' + release.join(', '));
console.log('debug:   ' + (debugYalniz.length ? release.concat(debugYalniz).join(', ') : '(release ile eyni)'));

if (ugursuz) process.exit(1);
