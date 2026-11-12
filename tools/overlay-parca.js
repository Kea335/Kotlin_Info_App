/* ============================================================
   KotlinAZ — tərcümə parçaları: overlay-dən çıxar, geri yaz (T4.1)

   Tərcümə hissə-hissə gedir (localization/PROMPT.md): bir mövzu, bir
   bölmə. Bu alət parçanın baza (az) mətnini overlay açarları ilə çıxarır
   və tərcümə olunmuş parçanı overlay-ə geri yazır.

   İşlətmək:
     node tools/overlay-parca.js siyahi <fayl>
         parçalar və yarpaq sayları (content: bölmələr, exercises: mövzular)
     node tools/overlay-parca.js cixar <dil> <fayl> [prefiks] [--hamisi]
         prefiks altındakı yarpaqların az mətni, JSON (stdout).
         Defolt: yalnız hələ tərcümə olunmamışlar; --hamisi: hamısı
         (dildən asılı olmayanlar da — bax neytral).
         prefiks nöqtə ilə: topics.sintaksis, sections.giris, questions
     node tools/overlay-parca.js neytral <fayl>
         dildən asılı olmayan yarpaqlar (yalnız kod/rəqəm) — faizə sayılmır
     node tools/overlay-parca.js yaz <dil> <fayl> <parca.json>
         parçanı overlay-ə yazır. Naməlum açar xətadır (heç nə yazılmır).

   <fayl>: content | exercises | quiz | playground
   Yazandan sonra: node tools/build-locale.js <dil> && node tools/validate-locales.js <dil>
   ============================================================ */
'use strict';

const fs = require('fs');
const L = require('./content-lib');

const args = process.argv.slice(2);
const HAMISI = args.includes('--hamisi');
const [emr, a1, a2, a3] = args.filter((a) => !a.startsWith('--'));

function dayan(msg) { console.error('XETA: ' + msg); process.exit(2); }

function faylYoxla(ad) {
  if (L.FAYLLAR.indexOf(ad) === -1) dayan('fayl ' + L.FAYLLAR.join(' | ') + ' olmalidir: ' + ad);
}

function yarpaqlar(ad) { return L.YARPAQ[ad](L.bazaOxu(ad)); }

function overlayOxu(dil, ad) {
  const f = L.overlayYolu(dil, ad);
  if (!fs.existsSync(f)) dayan(f + ' yoxdur — node tools/new-locale.js ' + dil);
  return JSON.parse(fs.readFileSync(f, 'utf8'));
}

function basdir(acar, prefiks) {
  return prefiks.every((p, i) => acar[i] === p);
}

if (emr === 'siyahi') {
  faylYoxla(a1);
  const qrup = {};
  const sira = [];
  yarpaqlar(a1).forEach((y) => {
    const k = y.acar.slice(0, 2).join('.');
    if (!qrup[k]) { qrup[k] = { say: 0, simvol: 0 }; sira.push(k); }
    qrup[k].say++;
    qrup[k].simvol += JSON.stringify(y.deyer).length;
  });
  sira.forEach((k) => console.log(k.padEnd(40) + String(qrup[k].say).padStart(5) + ' yarpaq' +
    String(Math.round(qrup[k].simvol / 100) / 10).padStart(8) + 'k'));
} else if (emr === 'neytral') {
  faylYoxla(a1);
  let say = 0;
  yarpaqlar(a1).forEach((y) => {
    if (!L.dilNeytraldir(y.deyer)) return;
    say++;
    console.log(y.acar.join(' › ') + '  ' + JSON.stringify(y.deyer).slice(0, 100));
  });
  console.error(say + ' dilden asili olmayan yarpaq');
} else if (emr === 'cixar') {
  faylYoxla(a2);
  const prefiks = a3 ? a3.split('.') : [];
  const overlay = overlayOxu(a1, a2);
  const cixis = {};
  let say = 0;
  yarpaqlar(a2).forEach((y) => {
    if (!basdir(y.acar, prefiks) || L.bosdur(y.deyer)) return;
    if (L.dilNeytraldir(y.deyer) && !HAMISI) return;
    const v = L.overlayOxu(overlay, y.acar);
    const tercume = !L.bosdur(v) && JSON.stringify(v) !== JSON.stringify(y.deyer) && !(y.yoxla && y.yoxla(v));
    if (tercume && !HAMISI) return;
    L.overlayYaz(cixis, y.acar, y.deyer);
    say++;
  });
  console.error(say + ' yarpaq');
  process.stdout.write(JSON.stringify(cixis, null, 2) + '\n');
} else if (emr === 'yaz') {
  faylYoxla(a2);
  if (!a3 || !fs.existsSync(a3)) dayan('parca fayli yoxdur: ' + a3);
  const parca = JSON.parse(fs.readFileSync(a3, 'utf8'));
  const overlay = overlayOxu(a1, a2);
  const bilinen = {};
  yarpaqlar(a2).forEach((y) => { bilinen[y.acar.join('\u0000')] = true; });
  const yollar = L.overlayYollari(parca, [], []);
  const namelum = yollar.filter((p) => !bilinen[p.join('\u0000')]);
  if (namelum.length) {
    namelum.slice(0, 20).forEach((p) => console.error('  naməlum açar: ' + p.join(' › ')));
    dayan(namelum.length + ' naməlum açar — heç nə yazılmadı');
  }
  yollar.forEach((p) => L.overlayYaz(overlay, p, L.overlayOxu(parca, p)));
  if (overlay._meta) overlay._meta.updated = new Date().toISOString().slice(0, 10);
  fs.writeFileSync(L.overlayYolu(a1, a2), JSON.stringify(overlay, null, 2) + '\n', 'utf8');
  console.log(yollar.length + ' yarpaq yazildi → ' + L.overlayYolu(a1, a2));
} else {
  console.error('Istifade: node tools/overlay-parca.js siyahi|cixar|yaz … (bax faylin basligi)');
  process.exit(2);
}
