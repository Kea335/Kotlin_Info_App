/* ============================================================
   KotlinAZ — R8 serializator yoxlaması

   Release build-dən (assembleRelease) sonra işlədilir. Mənbədəki hər
   @Serializable sinfin serializatoru R8-dən sağ çıxmalıdır: R8 onu
   silsə debug build və unit testlər yaşıl qalır, amma release APK
   ilk JSON oxunuşunda (məzmun, kompilyator cavabı, naviqasiya
   route-u) çökür. mapping.txt R8-in saxladığı bütün sinifləri sadalayır.

     class / data class    → <Ad>$$serializer saxlanılıb
     object / data object  → obyektin özü saxlanılıb (serializatoru
                             özündədir)

   İşlətmək:  ./gradlew assembleRelease && node tools/check-r8.js
   Çıxış kodu 2 — serializator silinib və ya mapping.txt yoxdur.
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');

const KOK = path.resolve(__dirname, '..');
const MENBE = path.join(KOK, 'app', 'src', 'main', 'java');
const MAPPING = path.join(KOK, 'app', 'build', 'outputs', 'mapping', 'release', 'mapping.txt');

function kotlinFayllari(qovluq) {
  return fs.readdirSync(qovluq, { withFileTypes: true }).flatMap((f) => {
    const yol = path.join(qovluq, f.name);
    if (f.isDirectory()) return kotlinFayllari(yol);
    return f.name.endsWith('.kt') ? [yol] : [];
  });
}

/** @Serializable tiplər: { ad, obyekt, fayl }. Annotasiya ilə bəyan arasında başqa annotasiya ola bilər. */
function serializableTipler() {
  const netice = [];
  const re = /@Serializable\b[^\n]*?(?:\n\s*@[^\n]*)*\s*(?:(?:private|internal|public)\s+)?(data\s+object|object|data\s+class|class)\s+(\w+)/g;
  kotlinFayllari(MENBE).forEach((fayl) => {
    const metn = fs.readFileSync(fayl, 'utf8');
    let m;
    while ((m = re.exec(metn)) !== null) {
      netice.push({ ad: m[2], obyekt: m[1].endsWith('object'), fayl: path.relative(KOK, fayl).split(path.sep).join('/') });
    }
  });
  return netice;
}

if (!fs.existsSync(MAPPING)) {
  console.error('XETA: ' + path.relative(KOK, MAPPING) + ' yoxdur — evvel ./gradlew assembleRelease');
  process.exit(2);
}

// Yalnız sinif sətirləri: «orijinal.Ad -> qisa:» (üzvlər boşluqla başlayır).
const saxlanilan = new Set(
  fs.readFileSync(MAPPING, 'utf8').split('\n')
    .filter((s) => s && !s.startsWith(' ') && !s.startsWith('#'))
    .map((s) => s.slice(0, s.indexOf(' -> ')))
);
// Paket və xarici sinif adı bilinmir — sonluq üzrə axtarılır (.Ad və ya $Ad).
const sonluqlar = new Set();
saxlanilan.forEach((ad) => {
  for (let j = 0; j < ad.length; j++) {
    if (ad[j] === '.' || ad[j] === '$') sonluqlar.add(ad.slice(j + 1));
  }
});

const tipler = serializableTipler();
const itenler = tipler.filter((t) => !sonluqlar.has(t.obyekt ? t.ad : t.ad + '$$serializer'));

console.log('@Serializable tip: ' + tipler.length + ', R8-den sonra saxlanilan: ' + (tipler.length - itenler.length));
if (itenler.length) {
  console.log('');
  console.log('R8 serializatoru silib (' + itenler.length + ') — proguard-rules.pro-ya keep qaydasi lazimdir:');
  itenler.forEach((t) => console.log('  ! ' + t.ad + (t.obyekt ? '' : '$$serializer') + '  (' + t.fayl + ')'));
  process.exit(2);
}
if (!tipler.length) {
  console.error('XETA: @Serializable tip tapilmadi — skanerin regex-i kohnelib');
  process.exit(2);
}
