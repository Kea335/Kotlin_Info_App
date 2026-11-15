/* ============================================================
   KotlinAZ — hardcoded UI mətni yoxlaması (T3.4)

   Android Lint Compose-dakı `Text("…")` literal-larını tutmur
   (HardcodedText yalnız XML üçündür). Bu skript app/src/main/java
   altındakı Kotlin fayllarının string literal-larını tapır — şərhləri
   və `"…"` / `"""…"""` sərhədlərini tanıyan kiçik skanerlə, grep-dən
   fərqli olaraq `ClipData.newPlainText(` kimi adları səhv tutmur.

   Xəta sayılır:
     1. Azərbaycan hərfli literal (ə ğ ı ş ç ö ü və böyükləri) — UI
        mətni resursda olmalıdır: stringResource(R.string.…)
     2. Text("…"), text = "…", contentDescription = "…" — hərfi olan
        literal (məsələn "Kotlin" də), dilindən asılı olmayaraq

   İstisnalar (sətrə və ya bloka şərh):
     // i18n-ignore                    bu sətir (səbəbini yanında yaz)
     // i18n-ignore-start … -end       blok (məs. kod nümunəsi, dil adları)

   İşlətmək:  node tools/check-i18n.js     (çıxış kodu 2 — tapıntı var)
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');

const KOK = path.resolve(__dirname, '..');
const MENBE = path.join(KOK, 'app', 'src', 'main', 'java');
const AZ_HERF = /[ƏəĞğİıŞşÇçÖöÜü]/;
const HERF = /\p{L}{2}/u;
const UI_PARAM = /(?:^|[^\w.])(?:Text\(\s*|text\s*=\s*|contentDescription\s*=\s*)$/;

function fayllar(q) {
  return fs.readdirSync(q, { withFileTypes: true }).flatMap((f) => {
    const y = path.join(q, f.name);
    return f.isDirectory() ? fayllar(y) : f.name.endsWith('.kt') ? [y] : [];
  });
}

/**
 * Faylı skan edir: string literal-lar (sətir, mətn, literal-dan əvvəlki kod)
 * və şərhlər (istisna nişanları üçün). Şablon ifadəsinin (`${…}`) içi kod
 * kimi rekursiv skan olunur — `"a${f("b")}c"`-dəki `"b"` ayrıca literaldır,
 * xarici literalın mətnində isə ifadə yerinə `${}` qalır.
 */
function skan(metn) {
  const literallar = [];
  const serhler = []; // { setir, metn }
  const n = metn.length;
  // Sətir nömrəsi: indeksdən əvvəlki \n sayı + 1 (ikili axtarışla).
  const sonlar = [];
  for (let k = 0; k < n; k++) if (metn[k] === '\n') sonlar.push(k);
  const setirNo = (idx) => {
    let a = 0; let b = sonlar.length;
    while (a < b) { const m = (a + b) >> 1; if (sonlar[m] < idx) a = m + 1; else b = m; }
    return a + 1;
  };
  const setirBasi = (idx) => metn.lastIndexOf('\n', idx - 1) + 1;

  /** Kod: [i]-dən başlayır; `}` balansı sıfırdan aşağı düşəndə (şablon sonu) qayıdır. */
  function kod(i, sablon) {
    let derinlik = 0;
    while (i < n) {
      const c = metn[i];
      const c2 = metn.substr(i, 2);
      if (c2 === '//') {
        const e = metn.indexOf('\n', i);
        const son = e === -1 ? n : e;
        serhler.push({ setir: setirNo(i), metn: metn.slice(i, son) });
        i = son;
      } else if (c2 === '/*') {
        const e = metn.indexOf('*/', i + 2);
        const son = e === -1 ? n : e + 2;
        serhler.push({ setir: setirNo(i), metn: metn.slice(i, son) });
        i = son;
      } else if (c === '"') {
        i = setir(i);
      } else if (c === '\'') {
        // Char literal: 'a', '\n', 'A'
        const e = metn.indexOf('\'', i + (metn[i + 1] === '\\' ? 2 : 1) + 1);
        i = e === -1 ? i + 1 : e + 1;
      } else if (c === '{') {
        derinlik++; i++;
      } else if (c === '}') {
        if (sablon && derinlik === 0) return i + 1;
        derinlik--; i++;
      } else {
        i++;
      }
    }
    return i;
  }

  /** String literal: `"…"` və ya `"""…"""`; açılış dırnağının indeksindən. */
  function setir(i) {
    const xam = metn.substr(i, 3) === '"""';
    const bas = i;
    i += xam ? 3 : 1;
    let icerik = '';
    while (i < n) {
      if (xam ? metn.substr(i, 3) === '"""' : metn[i] === '"') { i += xam ? 3 : 1; break; }
      if (!xam && metn[i] === '\n') break;
      if (!xam && metn[i] === '\\') { icerik += metn.substr(i, 2); i += 2; continue; }
      if (metn.substr(i, 2) === '${') { icerik += '${}'; i = kod(i + 2, true); continue; }
      icerik += metn[i]; i++;
    }
    literallar.push({ setir: setirNo(bas), metn: icerik, evvel: metn.slice(setirBasi(bas), bas) });
    return i;
  }

  kod(0, false);
  return { literallar, serhler };
}

function istisnalar(serhler) {
  const setirler = new Set();
  const bloklar = [];
  let acilis = null;
  serhler.forEach((s) => {
    if (/i18n-ignore-start/.test(s.metn)) acilis = s.setir;
    else if (/i18n-ignore-end/.test(s.metn)) {
      if (acilis !== null) bloklar.push([acilis, s.setir]);
      acilis = null;
    } else if (/i18n-ignore\b/.test(s.metn)) setirler.add(s.setir);
  });
  return (setir) => setirler.has(setir) || bloklar.some(([a, b]) => setir >= a && setir <= b);
}

const tapintilar = [];
fayllar(MENBE).forEach((f) => {
  const { literallar, serhler } = skan(fs.readFileSync(f, 'utf8'));
  const ignor = istisnalar(serhler);
  literallar.forEach((l) => {
    if (ignor(l.setir)) return;
    const sebeb = AZ_HERF.test(l.metn) ? 'azərbaycanca mətn'
      : (UI_PARAM.test(l.evvel) && HERF.test(l.metn)) ? 'UI parametrində literal'
        : null;
    if (sebeb) {
      tapintilar.push(path.relative(KOK, f).split(path.sep).join('/') + ':' + l.setir +
        '  ' + sebeb + ': "' + l.metn.slice(0, 70) + (l.metn.length > 70 ? '…' : '') + '"');
    }
  });
});

if (tapintilar.length) {
  console.log('Hardcoded UI mətni (' + tapintilar.length + ') — res/values/strings.xml-ə köçür,');
  console.log('kod/brend/fayl adıdırsa sətrə «// i18n-ignore — səbəb» yaz:');
  tapintilar.forEach((t) => console.log('  ! ' + t));
  process.exit(2);
}
console.log('Hardcoded UI mətni yoxdur.');
