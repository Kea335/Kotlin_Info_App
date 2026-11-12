/* ============================================================
   KotlinAZ — yeni dil üçün boş overlay skeleti (T3.2)

   Bazadan (assets/content/az) tərcümə olunan bütün sahələri
   ID ilə açarlanmış, boş dəyərli overlay kimi localization/<lc>/
   qovluğuna yazır — tərcüməçi faylı sıfırdan yazmasın.

   İşlətmək:  node tools/new-locale.js en
              node tools/new-locale.js en --force     (mövcud faylların üstünə)
              node tools/new-locale.js en --tamamla   (mövcud overlay-i bazaya uyğunlaşdır)

   --tamamla: skelet bazanın cari halından yenidən qurulur, overlay-dəki
   tərcümələr yerində qalır. Bazada yeni sahə (məs. kod şərhi, T4.0) və
   ya dəyişmiş blok id-si (mətn dəyişib, T3.1) olanda işlədilir. Artıq
   mövcud olmayan açarda TƏRCÜMƏ varsa fayl yazılmır — o mətn əl ilə
   yeni id-yə köçürülməlidir (--force ilə atılır).
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');
const L = require('./content-lib');

const args = process.argv.slice(2).filter((a) => !a.startsWith('--'));
const FORCE = process.argv.includes('--force');
const TAMAMLA = process.argv.includes('--tamamla');
const locale = args[0];

if (!locale || !/^[a-z]{2,3}(-[A-Za-z]{2,4})?$/.test(locale)) {
  console.error('Istifade: node tools/new-locale.js <locale>   (mes. en, tr, ru)');
  process.exit(1);
}
if (locale === L.BAZA) {
  console.error('XETA: ' + L.BAZA + ' baza dildir — overlay lazim deyil.');
  process.exit(1);
}

const qovluq = path.join(L.LOCALIZATION, locale);
fs.mkdirSync(qovluq, { recursive: true });

const bugun = new Date().toISOString().slice(0, 10);
let yazildi = 0;

L.FAYLLAR.forEach((ad) => {
  const hedef = L.overlayYolu(locale, ad);
  if (TAMAMLA && fs.existsSync(hedef)) {
    if (tamamla(ad, hedef)) yazildi++;
    return;
  }
  if (fs.existsSync(hedef) && !FORCE) {
    console.log('  var     ' + path.relative(process.cwd(), hedef) + '  (--force ile ust-uste yaz)');
    return;
  }
  const baza = L.bazaOxu(ad);
  const overlay = { _meta: { locale: locale, base: L.BAZA, updated: bugun } };
  const yarpaqlar = L.YARPAQ[ad](baza);
  yarpaqlar.forEach((y) => L.overlayYaz(overlay, y.acar, L.bosNusxe(y.deyer)));
  fs.writeFileSync(hedef, JSON.stringify(overlay, null, 2) + '\n', 'utf8');
  yazildi++;
  console.log('  yazildi ' + path.relative(process.cwd(), hedef).padEnd(36) + yarpaqlar.length + ' sahe');
});

console.log('');
console.log(locale + ': ' + yazildi + ' fayl ' + (TAMAMLA ? 'yenilendi' : 'yaradildi') + '. Sonra: node tools/build-locale.js ' + locale);

/** Mövcud overlay-i bazanın cari yarpaqlarına uyğunlaşdırır; itən tərcümə olarsa yazmır. */
function tamamla(ad, hedef) {
  const kohne = JSON.parse(fs.readFileSync(hedef, 'utf8'));
  const yeni = { _meta: Object.assign({}, kohne._meta, { updated: bugun }) };
  const yarpaqlar = L.YARPAQ[ad](L.bazaOxu(ad));
  const saxlanan = {};
  let elave = 0;
  yarpaqlar.forEach((y) => {
    const v = L.overlayOxu(kohne, y.acar);
    if (v === undefined) elave++;
    L.overlayYaz(yeni, y.acar, v === undefined ? L.bosNusxe(y.deyer) : v);
    saxlanan[y.acar.join('\u0000')] = true;
  });
  const ovsuz = Object.assign({}, kohne); delete ovsuz._meta;
  const atilan = L.overlayYollari(ovsuz, [], []).filter((yol) => !saxlanan[yol.join('\u0000')]);
  const itenTercume = atilan.filter((yol) => !L.bosdur(L.overlayOxu(kohne, yol)));
  const rel = path.relative(process.cwd(), hedef);
  if (!elave && !atilan.length) {
    console.log('  eynidir   ' + rel);
    return false;
  }
  if (itenTercume.length && !FORCE) {
    console.log('  XETA    ' + rel + ': ' + itenTercume.length + ' tercume bazada artiq yoxdur (id deyisib?) — fayl yazilmadi:');
    itenTercume.slice(0, 10).forEach((yol) => console.log('          ' + yol.join(' › ')));
    console.log('          Metni yeni id-ye kocur ve ya --force ile at.');
    process.exitCode = 2;
    return false;
  }
  fs.writeFileSync(hedef, JSON.stringify(yeni, null, 2) + '\n', 'utf8');
  console.log('  yenilendi ' + rel.padEnd(34) + '+' + elave + ' sahe, -' + atilan.length + ' kohne acar' +
    (itenTercume.length ? ' (' + itenTercume.length + ' tercume atildi)' : ''));
  return true;
}
