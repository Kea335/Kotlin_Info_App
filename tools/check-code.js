/* ============================================================
   KotlinAZ — kod nümunələrinin kompilyasiya yoxlaması (T4.6)

   Tətbiqdəki icra olunan kodu JetBrains kompilyator xidmətində
   işlədir və nəticəni gözlənilənlə tutuşdurur:
     - content.json    → `fun main` olan kotlin blokları; `output`
                          varsa çıxış müqayisə olunur, yoxdursa kod
                          yalnız xətasız işləməlidir
     - exercises.json  → praktiki çalışmaların model həlli (`hell`)
                          və `gozlenilen`
     - playground.json → meydan nümunələri xətasız işləməlidir
   `fun main`-siz bloklar fraqmentdir (dərsdəki parça) — atlanır.

   Uğurlu nəticələr tools/.code-cache.json-a yazılır (açar: versiya +
   kod + gözlənilən). Dəyişməyən nümunə təkrar göndərilmir; kompilyator
   versiyası dəyişəndə hamısı yenidən yoxlanılır.

   Xidmət ictimaidir: paralellik 2, sorğular arası 400 ms.
   Keşsiz tam keçid ≈ 10–15 dəqiqə — CI-da gecəlik işləyir.

   İşlətmək:  node tools/check-code.js               # az (baza)
              node tools/check-code.js en            # bir dil
              node tools/check-code.js --all         # bütün dillər
              node tools/check-code.js --yeniden     # keşə baxmadan
              node tools/check-code.js --hesabat h.md  # markdown hesabat
              node tools/check-code.js --ilk 5       # tüstü sınağı
   Çıxış kodu: 0 hamısı ok, 1 uyğunsuzluq var, 2 istifadə/öz-test
               xətası, 3 xidmət əlçatan deyil (nəticə natamamdır).
   ============================================================ */
'use strict';

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const KOK = path.resolve(__dirname, '..');
const AKTIVLER = [
  path.join(KOK, 'app', 'src', 'main', 'assets', 'content'),
  // yarımçıq dillər yalnız debug APK-ya düşür (T4.7), amma yoxlanılmalıdır
  path.join(KOK, 'app', 'src', 'debug', 'assets', 'content')
];
const KES_FAYLI = path.join(__dirname, '.code-cache.json');
const ISTISNALAR = path.join(__dirname, 'check-code-istisnalar.json');
const HALLAR = path.join(__dirname, 'normallasdir-hallar.json');

const API = process.env.KOTLINAZ_COMPILER_API || 'https://api.kotlinlang.org';
const PARALEL = 2;
const FASILE_MS = 400;
const TIMEOUT_MS = 60000;
const CEHD = 3;

const args = process.argv.slice(2);
const bayraq = (ad) => args.includes(ad);
const deyer = (ad) => { const i = args.indexOf(ad); return i >= 0 ? args[i + 1] : null; };
const HESABAT = deyer('--hesabat');
const secilen = args.filter((a, i) => !a.startsWith('--') && args[i - 1] !== '--hesabat' && args[i - 1] !== '--ilk');

/* ---------- Normallaşdırma — KotlinCompiler.kt ilə eyni ---------- */

// Kotlin-in Char.isWhitespace(): Character.isWhitespace || isSpaceChar.
// JS-in trimEnd-i fərqlidir (U+FEFF-i atır, U+001C–U+001F-i saxlayır),
// ona görə sinif əl ilə qurulur. Hallar: tools/normallasdir-hallar.json.
const KT_SAG_BOSLUQ = /[\t\n\u000B\f\r\u001C-\u001F\p{Zs}\u2028\u2029]+$/u;

function normallasdir(s) {
  return (s == null ? '' : String(s))
    .replace(/\r\n/g, '\n')
    .split('\n')
    .map((x) => x.replace(KT_SAG_BOSLUQ, ''))
    .join('\n')
    .replace(/^\n+|\n+$/g, '');
}

/** `<outStream>`/`<errStream>` etiketlərini açır — `cixisiAyir` ilə eyni. */
function cixisiAyir(xam) {
  if (!xam || !xam.trim()) return '';
  let netice = '';
  ['outStream', 'errStream'].forEach((etiket) => {
    const re = new RegExp('<' + etiket + '>([\\s\\S]*?)</' + etiket + '>', 'g');
    let m;
    while ((m = re.exec(xam)) !== null) netice += m[1];
  });
  return netice === '' ? xam : netice;
}

function ozTest() {
  const hallar = JSON.parse(fs.readFileSync(HALLAR, 'utf8')).hallar;
  const sehv = hallar.filter((h) => normallasdir(h.giris) !== h.netice);
  if (sehv.length) {
    console.error('XETA: normallasdir Kotlin qaydasindan ferqlenir (' + HALLAR + '):');
    sehv.forEach((h) => console.error('  ' + JSON.stringify(h.giris) + ' → ' + JSON.stringify(normallasdir(h.giris)) +
      ', gozlenilen ' + JSON.stringify(h.netice)));
    process.exit(2);
  }
}

/* ---------- Nümunələrin toplanması ---------- */

function dilQovlugu(lc) {
  for (const k of AKTIVLER) {
    const q = path.join(k, lc);
    if (fs.existsSync(path.join(q, 'content.json'))) return q;
  }
  return null;
}

function butunDiller() {
  const diller = new Set();
  AKTIVLER.forEach((k) => {
    if (!fs.existsSync(k)) return;
    fs.readdirSync(k).forEach((lc) => { if (dilQovlugu(lc)) diller.add(lc); });
  });
  return [...diller].sort((a, b) => (a === 'az' ? -1 : b === 'az' ? 1 : a.localeCompare(b)));
}

const oxu = (q, f) => {
  const yol = path.join(q, f);
  return fs.existsSync(yol) ? JSON.parse(fs.readFileSync(yol, 'utf8')) : null;
};

const MAIN = /\bfun\s+main\s*\(/;

/**
 * Bir dilin nümunələri. Hər biri: { qrup, id, kod, gozlenilen|null }.
 * gozlenilen null — kod yalnız xətasız işləməlidir.
 */
function numuneler(lc) {
  const q = dilQovlugu(lc);
  const netice = { bloklar: [], helller: [], meydan: [], fraqment: 0 };

  const gez = (o) => {
    if (Array.isArray(o)) return o.forEach(gez);
    if (!o || typeof o !== 'object') return;
    if (o.t === 'code') {
      if ((o.lang || 'kotlin') === 'kotlin' && MAIN.test(o.code)) {
        netice.bloklar.push({ qrup: 'bloklar', id: o.id, kod: o.code, gozlenilen: o.output == null ? null : o.output });
      } else {
        netice.fraqment++;
      }
    }
    Object.keys(o).forEach((k) => { if (o[k] && typeof o[k] === 'object') gez(o[k]); });
  };
  const content = oxu(q, 'content.json');
  if (content) gez(content.sections);

  const calismalar = oxu(q, 'exercises.json');
  if (calismalar) {
    calismalar.topics.forEach((t) => t.praktiki.forEach((p) => {
      netice.helller.push({ qrup: 'helller', id: p.id, kod: p.hell, gozlenilen: p.gozlenilen });
    }));
  }

  const meydan = oxu(q, 'playground.json');
  if (meydan) {
    meydan.presets.forEach((p) => netice.meydan.push({ qrup: 'meydan', id: 'meydan.' + p.id, kod: p.code, gozlenilen: null }));
  }
  return netice;
}

/* ---------- Kompilyator xidməti ---------- */

const gozle = (ms) => new Promise((r) => setTimeout(r, ms));

/** Tətbiqin Kotlin versiyası — gradle/libs.versions.toml (KotlinVersion.CURRENT-in mənbəyi). */
function tetbiqinVersiyasi() {
  const toml = fs.readFileSync(path.join(KOK, 'gradle', 'libs.versions.toml'), 'utf8');
  const m = toml.match(/^kotlin\s*=\s*"([^"]+)"/m);
  if (!m) throw new Error('libs.versions.toml: kotlin versiyasi tapilmadi');
  return m[1];
}

/** KompilyatorVersiyalari.sec ilə eyni seçim: öz versiya → eyni minor-un son patch-i → latestStable. */
async function versiyaSec() {
  const tetbiqin = tetbiqinVersiyasi();
  let siyahi;
  try {
    const r = await fetch(API + '/versions', { signal: AbortSignal.timeout(15000) });
    if (!r.ok) throw new Error('HTTP ' + r.status);
    siyahi = (await r.json()).filter((v) => /^\d+\.\d+\.\d+$/.test(v.version));
  } catch (e) {
    console.error('XETA: ' + API + '/versions alinmadi: ' + e.message);
    process.exit(3);
  }
  if (siyahi.some((v) => v.version === tetbiqin)) return tetbiqin;
  const [x, y] = tetbiqin.split('.');
  const eyni = siyahi.filter((v) => v.version.startsWith(x + '.' + y + '.'))
    .sort((a, b) => Number(b.version.split('.')[2]) - Number(a.version.split('.')[2]));
  if (eyni.length) return eyni[0].version;
  return (siyahi.find((v) => v.latestStable) || siyahi[0]).version;
}

class XidmetXetasi extends Error {}

/** Kodu işlədir: { hal: 'ok'|'kompilyasiya'|'icra', cixis, xetalar[] }. */
async function isle(versiya, kod) {
  const govde = JSON.stringify({ args: '', confType: 'java', files: [{ name: 'File.kt', publicId: '', text: kod }] });
  let son = '';
  for (let c = 1; c <= CEHD; c++) {
    try {
      const r = await fetch(API + '/api/' + versiya + '/compiler/run?filename=File.kt', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: govde,
        signal: AbortSignal.timeout(TIMEOUT_MS)
      });
      if (r.ok) return cavabiOxu(await r.json());
      son = 'HTTP ' + r.status;
      // 429 — xidmət yavaşlamağı istəyir; 5xx — müvəqqəti nasazlıq
      if (r.status !== 429 && r.status < 500) break;
    } catch (e) {
      son = e.name === 'TimeoutError' ? 'timeout' : e.message;
    }
    await gozle(c * 3000);
  }
  throw new XidmetXetasi(son);
}

function cavabiOxu(c) {
  const xetalar = Object.values(c.errors || {}).flat()
    .filter((e) => String(e.severity).toUpperCase() === 'ERROR')
    .map((e) => (e.interval && e.interval.start ? 'Setir ' + (e.interval.start.line + 1) + ': ' : '') + e.message);
  if (xetalar.length) return { hal: 'kompilyasiya', cixis: '', xetalar };
  if (c.exception) {
    const ad = c.exception.fullName || 'Exception';
    return { hal: 'icra', cixis: cixisiAyir(c.text), xetalar: [ad + (c.exception.message ? ': ' + c.exception.message : '')] };
  }
  return { hal: 'ok', cixis: cixisiAyir(c.text), xetalar: [] };
}

/* ---------- Müqayisə ---------- */

/** Birinci fərqli sətir — uzun çıxışda nəyin pozulduğunu göstərmək üçün. */
function ferq(gozlenilen, alinan) {
  const a = gozlenilen.split('\n');
  const b = alinan.split('\n');
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    if (a[i] !== b[i]) {
      const g = a[i] === undefined ? '(yoxdur)' : JSON.stringify(a[i]);
      const x = b[i] === undefined ? '(yoxdur)' : JSON.stringify(b[i]);
      return 'setir ' + (i + 1) + ': gozlenilen ' + g + ', alinan ' + x;
    }
  }
  return '';
}

function qiymetlendir(n, r) {
  const tek = (s) => s.replace(/\s+/g, ' ').trim(); // çoxsətirli kompilyator mesajı
  if (r.hal === 'kompilyasiya') return 'Kompilyasiya xetasi — ' + r.xetalar.slice(0, 2).map(tek).join(' | ');
  if (r.hal === 'icra') return 'Icra xetasi — ' + tek(r.xetalar[0]);
  if (n.gozlenilen == null) return null;
  const g = normallasdir(n.gozlenilen);
  const a = normallasdir(r.cixis);
  return g === a ? null : ferq(g, a);
}

const acar = (versiya, n) => crypto.createHash('sha1')
  .update(versiya + '\0' + n.kod + '\0' + (n.gozlenilen == null ? '\u0001' : n.gozlenilen))
  .digest('hex');

/* ---------- Əsas axın ---------- */

async function main() {
  ozTest();

  const diller = bayraq('--all') ? butunDiller() : (secilen.length ? secilen : ['az']);
  diller.forEach((lc) => {
    if (!dilQovlugu(lc)) { console.error('XETA: ' + lc + ' ucun content.json yoxdur'); process.exit(2); }
  });

  // { id: { rejim: 'atla' | 'yalniz-icra', sebeb } } — bilərəkdən işləməyən
  // (fraqment, xarici kitabxana) və ya çıxışı qeyri-deterministik nümunələr.
  const istisnalar = fs.existsSync(ISTISNALAR) ? JSON.parse(fs.readFileSync(ISTISNALAR, 'utf8')).istisnalar : {};
  const gorulen = new Set();
  const kes = !bayraq('--yeniden') && fs.existsSync(KES_FAYLI) ? JSON.parse(fs.readFileSync(KES_FAYLI, 'utf8')) : {};
  const versiya = await versiyaSec();

  // Hər dil öz siyahısını alır; eyni kod (overlay kodu dəyişmir) bir dəfə göndərilir.
  const hesabat = [];
  const novbe = new Map(); // açar → nümunə
  diller.forEach((lc) => {
    const s = numuneler(lc);
    const hamisi = [...s.bloklar, ...s.helller, ...s.meydan];
    hamisi.forEach((n) => {
      const ist = istisnalar[n.id];
      if (ist) gorulen.add(n.id);
      if (ist && ist.rejim === 'yalniz-icra') n.gozlenilen = null; // işləsin, çıxış müqayisə olunmasın
      n.istisna = ist && ist.rejim === 'atla' ? ist : null;
      n.acar = acar(versiya, n);
      if (!n.istisna && !kes[n.acar] && !novbe.has(n.acar)) novbe.set(n.acar, n);
    });
    hesabat.push({ lc, s, hamisi });
  });

  const neticeler = new Map(); // açar → { xeta } | { xidmet }
  const ilk = Number(deyer('--ilk')) || Infinity; // tüstü sınağı: yalnız ilk N nümunə
  const isler = [...novbe.values()].slice(0, ilk);
  const cemi = isler.length;
  let bitdi = 0;
  let ardicilXidmet = 0;
  const addim = async () => {
    while (isler.length) {
      const n = isler.shift();
      try {
        const r = await isle(versiya, n.kod);
        neticeler.set(n.acar, { xeta: qiymetlendir(n, r) });
        ardicilXidmet = 0;
      } catch (e) {
        if (!(e instanceof XidmetXetasi)) throw e;
        neticeler.set(n.acar, { xidmet: e.message });
        // ardıcıl uğursuzluq — xidmət düşüb, qalanını göndərməyin mənası yoxdur
        if (++ardicilXidmet >= 6) isler.length = 0;
      }
      bitdi++;
      if (process.stdout.isTTY) process.stdout.write('\r  gonderilir ' + bitdi + ' / ' + cemi + '   ');
      else if (bitdi % 100 === 0) console.log('  gonderilir ' + bitdi + ' / ' + cemi); // CI jurnalı
      await gozle(FASILE_MS);
    }
  };
  console.log('KotlinAZ kod yoxlamasi — Kotlin ' + versiya + ', ' + cemi + ' numune gonderilir' +
    (cemi ? ' (≈' + Math.ceil(cemi * 1.6 / PARALEL / 60) + ' deq)' : ''));
  await Promise.all(Array.from({ length: PARALEL }, addim));
  if (process.stdout.isTTY && cemi) process.stdout.write('\n');

  // Uğurlular keşə — növbəti işləmədə göndərilmir.
  const yeniKes = bayraq('--yeniden') && fs.existsSync(KES_FAYLI) ? JSON.parse(fs.readFileSync(KES_FAYLI, 'utf8')) : kes;
  const bugun = new Date().toISOString().slice(0, 10);
  novbe.forEach((n, a) => {
    const r = neticeler.get(a);
    if (r && !r.xidmet && !r.xeta) yeniKes[a] = { id: n.id, versiya, tarix: bugun };
    else if (r && r.xeta) delete yeniKes[a];
  });
  fs.writeFileSync(KES_FAYLI, JSON.stringify(yeniKes, null, 1) + '\n', 'utf8');

  // Hesabat
  const setirler = [];
  const md = ['## Kod nümunələrinin yoxlaması', '', 'Kotlin `' + versiya + '` · ' + bugun, ''];
  let uygunsuz = 0;
  let natamam = 0;
  const QRUP_ADI = { bloklar: 'Kod bloklari     ', helller: 'Model helller    ', meydan: 'Meydan numuneleri' };

  hesabat.forEach(({ lc, s, hamisi }) => {
    setirler.push('');
    setirler.push('locale: ' + lc);
    const tapintilar = [];
    ['bloklar', 'helller', 'meydan'].forEach((qrup) => {
      const q = hamisi.filter((n) => n.qrup === qrup);
      if (!q.length) return;
      let ok = 0;
      let atlandi = 0;
      let gonderilmedi = 0;
      q.forEach((n) => {
        if (n.istisna) { atlandi++; return; }
        const r = neticeler.get(n.acar);
        if (!r) {
          if (kes[n.acar]) ok++; // keşdən — əvvəl keçib, dəyişməyib
          else { gonderilmedi++; natamam++; } // --ilk və ya xidmət düşdü
          return;
        }
        if (r.xidmet) { natamam++; tapintilar.push({ n, mesaj: 'yoxlanmadi — ' + r.xidmet, xidmet: true }); return; }
        if (r.xeta) { tapintilar.push({ n, mesaj: r.xeta }); return; }
        ok++;
      });
      const yalnizIcra = qrup === 'bloklar' ? q.filter((n) => n.gozlenilen == null && !n.istisna).length : 0;
      const elave = [];
      if (yalnizIcra) elave.push(yalnizIcra + ' yalniz icra');
      if (qrup === 'bloklar' && s.fraqment) elave.push(s.fraqment + ' fraqment atlandi');
      if (atlandi) elave.push(atlandi + ' istisna');
      if (gonderilmedi) elave.push(gonderilmedi + ' gonderilmedi');
      setirler.push('  ' + QRUP_ADI[qrup] + '  ' + ok + ' / ' + (q.length - atlandi) + ' ok' + (elave.length ? '   (' + elave.join(', ') + ')' : ''));
    });
    tapintilar.forEach((t) => {
      if (!t.xidmet) uygunsuz++;
      setirler.push('  ! ' + t.n.id + ' : ' + t.mesaj);
    });
    if (tapintilar.length) {
      md.push('### `' + lc + '`', '', '| Nümunə | Problem |', '|---|---|');
      tapintilar.forEach((t) => md.push('| `' + t.n.id + '` | ' + t.mesaj.replace(/\|/g, '\\|') + ' |'));
      md.push('');
    }
  });
  // Məzmunda artıq olmayan istisna (blok id-si mətnlə dəyişir, T3.1) — siyahı çürüməsin.
  const kohne = Object.keys(istisnalar).filter((id) => !gorulen.has(id));
  if (kohne.length && bayraq('--all')) {
    setirler.push('');
    kohne.forEach((id) => setirler.push('  ? istisna artiq yoxdur (id deyisib?): ' + id));
  }
  setirler.forEach((x) => console.log(x));

  if (!uygunsuz && !natamam) md.push('Bütün nümunələr gözlənilən nəticəni verir.');
  if (HESABAT) fs.writeFileSync(HESABAT, md.join('\n') + '\n', 'utf8');
  if (process.env.GITHUB_STEP_SUMMARY) fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, md.join('\n') + '\n', 'utf8');

  console.log('');
  if (uygunsuz) {
    console.log(uygunsuz + ' uygunsuzluq. Duzelt: model hell / gozlenilen / blokun output-u, ve ya');
    console.log('bilerekdirse ' + path.relative(KOK, ISTISNALAR) + '-e sebebi ile elave et.');
    process.exit(1);
  }
  if (natamam) {
    console.log(natamam + ' numune xidmet xetasina gore yoxlanmadi — sonra yeniden isle.');
    process.exit(3);
  }
  console.log('Hamisi ok.');
}

main().catch((e) => { console.error(e); process.exit(2); });
