/* ============================================================
   KotlinAZ — APK ölçüsü hesabatı və büdcə (T3.7)

   Release APK-nın (assembleRelease) zip kataloqunu oxuyur və hissələr
   üzrə sıxılmış ölçünü göstərir: kod (dex), resurslar, native kitabxanalar,
   dil aktivləri (assets/content/<dil>/). Sonda 15 dil üçün proqnoz verir
   və APK büdcəni (8 MB) aşırsa çıxış kodu 2 qaytarır.

   İşlətmək:  ./gradlew assembleRelease && node tools/apk-budcesi.js [apk]
   CI-da GITHUB_STEP_SUMMARY varsa hesabat oraya da yazılır.
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');

const KOK = path.resolve(__dirname, '..');
const BUDCE = 8 * 1024 * 1024;
const DIL_SAYI = 15;

function apkTap() {
  if (process.argv[2]) return path.resolve(process.argv[2]);
  const q = path.join(KOK, 'app', 'build', 'outputs', 'apk', 'release');
  // Lokalda imzalı `app-release.apk`, CI-da (keystore yoxdur) `app-release-unsigned.apk`.
  const f = fs.existsSync(q) ? fs.readdirSync(q).filter((a) => a.endsWith('.apk')) : [];
  if (!f.length) {
    console.error('XETA: release APK tapilmadi — evvel ./gradlew assembleRelease');
    process.exit(2);
  }
  return path.join(q, f[0]);
}

/** Zip-in mərkəzi kataloqu: { ad, sixilmis, xam } siyahısı. */
function zipKataloqu(buf) {
  let eocd = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 22 - 65535); i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error('zip kataloqu tapilmadi');
  const say = buf.readUInt16LE(eocd + 10);
  let p = buf.readUInt32LE(eocd + 16);
  const netice = [];
  for (let k = 0; k < say; k++) {
    if (buf.readUInt32LE(p) !== 0x02014b50) throw new Error('zip kataloqu zedelidir');
    const sixilmis = buf.readUInt32LE(p + 20);
    const xam = buf.readUInt32LE(p + 24);
    const adUz = buf.readUInt16LE(p + 28);
    const elave = buf.readUInt16LE(p + 30);
    const serh = buf.readUInt16LE(p + 32);
    netice.push({ ad: buf.toString('utf8', p + 46, p + 46 + adUz), sixilmis, xam });
    p += 46 + adUz + elave + serh;
  }
  return netice;
}

function qrup(ad) {
  let m = ad.match(/^assets\/content\/([^/]+)\//);
  if (m) return 'assets/content/' + m[1];
  if (/^classes\d*\.dex$/.test(ad)) return 'dex';
  if (ad === 'resources.arsc' || ad.startsWith('res/')) return 'res';
  m = ad.match(/^lib\/([^/]+)\//);
  if (m) return 'lib/' + m[1];
  if (ad.startsWith('assets/')) return 'assets (diger)';
  if (ad.startsWith('META-INF/')) return 'META-INF';
  return 'diger';
}

const kb = (b) => (b / 1024).toFixed(1).padStart(8) + ' KB';

const apk = apkTap();
const olcu = fs.statSync(apk).size;
const qruplar = {};
zipKataloqu(fs.readFileSync(apk)).forEach((e) => {
  const g = qrup(e.ad);
  qruplar[g] = qruplar[g] || { sixilmis: 0, xam: 0 };
  qruplar[g].sixilmis += e.sixilmis;
  qruplar[g].xam += e.xam;
});

const diller = Object.keys(qruplar).filter((g) => g.startsWith('assets/content/'));
const dilOrta = diller.length
  ? diller.reduce((c, g) => c + qruplar[g].sixilmis, 0) / diller.length
  : 0;
// Proqnoz: hər əlavə dil təxminən mövcud dillərin ortası qədər tutur.
// Latın olmayan əlifbalarda UTF-8 mətni uzundur — 1,3 ehtiyat əmsalı.
const proqnoz = olcu + Math.max(0, DIL_SAYI - diller.length) * dilOrta * 1.3;

const setirler = [];
setirler.push('APK: ' + path.relative(KOK, apk).split(path.sep).join('/'));
setirler.push('');
setirler.push('hisse                         sixilmis          xam');
Object.entries(qruplar).sort((a, b) => b[1].sixilmis - a[1].sixilmis).forEach(([g, v]) => {
  setirler.push(g.padEnd(26) + kb(v.sixilmis) + '  ' + kb(v.xam));
});
setirler.push('');
setirler.push('APK olcusu            : ' + kb(olcu));
setirler.push('dil (' + diller.length + ') ortasi         : ' + kb(dilOrta) + '  (sixilmis)');
setirler.push(DIL_SAYI + ' dil proqnozu        : ' + kb(proqnoz) + '  (x1,3 ehtiyat)');
setirler.push('budce                 : ' + kb(BUDCE));
console.log(setirler.join('\n'));

if (process.env.GITHUB_STEP_SUMMARY) {
  fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY,
    '### APK ölçüsü\n\n```\n' + setirler.join('\n') + '\n```\n', 'utf8');
}

if (olcu > BUDCE) {
  console.error('\nXETA: APK ' + kb(olcu).trim() + ' — 8 MB budcesini asdi (T3.7 ehtiyat variantlari: exercises.json-u movzulara bol, AAB + asset pack)');
  process.exit(2);
}
if (proqnoz > BUDCE) {
  console.log('\nXEBERDARLIQ: ' + DIL_SAYI + ' dil proqnozu budceni asir — T3.7 ehtiyat variantlarina bax');
}
