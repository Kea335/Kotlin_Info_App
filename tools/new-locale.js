/* ============================================================
   KotlinAZ — yeni dil üçün boş overlay skeleti (T3.2)

   Bazadan (assets/content/az) tərcümə olunan bütün sahələri
   ID ilə açarlanmış, boş dəyərli overlay kimi localization/<lc>/
   qovluğuna yazır — tərcüməçi faylı sıfırdan yazmasın.

   İşlətmək:  node tools/new-locale.js en
              node tools/new-locale.js en --force   (mövcud faylların üstünə)
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');
const L = require('./content-lib');

const args = process.argv.slice(2).filter((a) => !a.startsWith('--'));
const FORCE = process.argv.includes('--force');
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
console.log(locale + ': ' + yazildi + ' fayl yaradildi. Sonra: node tools/build-locale.js ' + locale);
