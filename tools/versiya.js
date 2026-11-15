/* ============================================================
   KotlinAZ — versiyanın artırılması (T5.7)

   Versiya tək yerdədir: gradle.properties → kotlinaz.versiya=X.Y.Z.
   versionCode ondan hesablanır (app/build.gradle.kts):
       X * 10000 + Y * 100 + Z      (1.2.3 → 10203)
   Beləcə versionCode heç vaxt əl ilə yazılmır və geri getmir.

   İşlətmək:  node tools/versiya.js            # cari versiya
              node tools/versiya.js patch      # 1.2.3 → 1.2.4
              node tools/versiya.js minor      # 1.2.3 → 1.3.0
              node tools/versiya.js major      # 1.2.3 → 2.0.0
              node tools/versiya.js 1.4.0      # açıq versiya (yalnız irəli)
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');

const FAYL = path.resolve(__dirname, '..', 'gradle.properties');
const RE = /^kotlinaz\.versiya=(\d+)\.(\d+)\.(\d+)\s*$/m;

const kod = (v) => v[0] * 10000 + v[1] * 100 + v[2];
const ad = (v) => v.join('.');

const metn = fs.readFileSync(FAYL, 'utf8');
const m = metn.match(RE);
if (!m) {
  console.error('XETA: gradle.properties-de kotlinaz.versiya=X.Y.Z yoxdur');
  process.exit(2);
}
const cari = [Number(m[1]), Number(m[2]), Number(m[3])];
const emr = process.argv[2];

if (!emr) {
  console.log(ad(cari) + ' (versionCode ' + kod(cari) + ')');
  process.exit(0);
}

let yeni;
if (emr === 'major') yeni = [cari[0] + 1, 0, 0];
else if (emr === 'minor') yeni = [cari[0], cari[1] + 1, 0];
else if (emr === 'patch') yeni = [cari[0], cari[1], cari[2] + 1];
else if (/^\d+\.\d+\.\d+$/.test(emr)) yeni = emr.split('.').map(Number);
else {
  console.error('Istifade: node tools/versiya.js [major|minor|patch|X.Y.Z]');
  process.exit(2);
}

// versionCode sxemi hər hissə üçün 0–99 yer ayırır.
if (yeni[1] > 99 || yeni[2] > 99) {
  console.error('XETA: minor və patch 0–99 olmalıdır: ' + ad(yeni));
  process.exit(2);
}
if (kod(yeni) <= kod(cari)) {
  console.error('XETA: versiya irəli getməlidir: ' + ad(cari) + ' → ' + ad(yeni));
  process.exit(2);
}

fs.writeFileSync(FAYL, metn.replace(RE, 'kotlinaz.versiya=' + ad(yeni)), 'utf8');
console.log(ad(cari) + ' → ' + ad(yeni) + ' (versionCode ' + kod(cari) + ' → ' + kod(yeni) + ')');
