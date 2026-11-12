/* ============================================================
   KotlinAZ — dil overlay-lərinin doğrulaması (T3.2, T4.7)

   Hər locale üçün yoxlayır:
     - struktur: overlay-dəki hər açar bazada mövcud id-yə və tərcümə
       olunan sahəyə uyğundur (naməlum id və ya texniki sahə — `a`,
       `code`, `level`, `starter`… — xətadır)
     - texniki sabitlik: `opts` sayı, cədvəl ölçüsü, siyahı uzunluğu,
       span növləri bazadakı ilə eynidir
     - keyfiyyət: bazadakı mətnin eynisi (tərcümə olunmayıb — xəbərdarlıq),
       təkrar açar, `%1$s` / `{0}` format nişanlarının qorunması
     - aktivlər: overlay-in cari halına uyğundur (build-locale.js
       işlədilib) və düzgün yerdədir: tam dil main/assets-də (release),
       yarımçıq dil debug/assets-də (T4.7)
     - siyahılar: main locales.json və locales_config.xml məhz baza +
       tam dillərdir — yarımçıq dil release-ə sızmır
     - faiz: tərcümə olunmuş sahə / ümumi sahə, qrup üzrə

   İşlətmək:  node tools/validate-locales.js            # bütün dillər
              node tools/validate-locales.js en --json  # CI üçün
              node tools/validate-locales.js --markdown # README dil cədvəli
              node tools/validate-locales.js --readme   # README.md-ni yerində yenilə
   Çıxış kodu: xəta varsa 2, əks halda 0.
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');
const L = require('./content-lib');

const args = process.argv.slice(2);
const JSON_CIXIS = args.includes('--json');
const MARKDOWN = args.includes('--markdown');
const README = args.includes('--readme');
const secilen = args.filter((a) => !a.startsWith('--'));
const QRUPLAR = ['ui', 'dersler', 'calismalar', 'quiz', 'meydan'];
const SPAN_NOVLERI = { t: 1, b: 1, i: 1, code: 1, link: 1, kbd: 1, grad: 1, br: 1 };

/* ---------- köməkçilər ---------- */

function metnDeyeri(v) {
  // Faiz və format müqayisəsi üçün dəyəri düz mətnə çevir.
  if (typeof v === 'string') return v;
  if (Array.isArray(v)) {
    return v.map((x) => {
      if (typeof x === 'string') return x;
      if (Array.isArray(x)) return metnDeyeri(x);
      if (x && typeof x === 'object') return x.k === 'br' ? ' ' : (x.v || '');
      return '';
    }).join(' ');
  }
  return String(v);
}

function formatNisanlari(metn) {
  return (metn.match(/%(\d+\$)?[sd]|\{\d+\}/g) || []).sort().join(',');
}

function deepEqual(a, b) { return JSON.stringify(a) === JSON.stringify(b); }

/* ---------- Lüğət və tərcümə olunmayanlar (T4.0) ---------- */

const LUGET_FAYLI = path.join(L.LOCALIZATION, 'glossary.json');
const luget = fs.existsSync(LUGET_FAYLI)
  ? JSON.parse(fs.readFileSync(LUGET_FAYLI, 'utf8'))
  : { terms: {}, doNotTranslate: [] };

function kicik(s, lc) {
  try { return s.toLocaleLowerCase(lc); } catch (e) { return s.toLowerCase(); }
}

/** Terminin dildəki qəbul olunan formaları, kiçik hərflə. */
function formalar(t, lc) {
  if (!t[lc]) return [];
  return [t[lc]].concat((t._formalar && t._formalar[lc]) || []).map((f) => kicik(f, lc));
}

const regexQacir = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

// Adi mətndə yalnız xüsusi adlar və çoxsözlü terminlər yoxlanılır: `var`,
// `val`, `when` kimi açar sözlər azərbaycanca mətndə adi söz də ola bilər
// («dəyişən var»). Onları inline kod span-larının eyniliyi qoruyur.
const QADAGAN = (luget.doNotTranslate || [])
  .filter((s) => /[A-Z ]/.test(s))
  .map((s) => ({ s, re: new RegExp('(^|[^\\p{L}\\p{N}_])' + regexQacir(s) + '(?=$|[^\\p{L}\\p{N}_])', 'u') }));

/** Dəyərdəki inline kod span-larının mətni (`{k:'code'}`), sıralı. */
function kodSpanlari(v, topla) {
  topla = topla || [];
  if (Array.isArray(v)) v.forEach((x) => kodSpanlari(x, topla));
  else if (v && typeof v === 'object' && v.k === 'code') topla.push(v.v);
  return topla;
}

/** Lüğət qaydaları — xəbərdarlıq mətnləri (boş massiv: problem yoxdur). */
function lugetYoxla(baza, terc, lc) {
  const x = [];
  const bk = kodSpanlari(baza).sort();
  const tk = kodSpanlari(terc).sort();
  if (bk.length && !deepEqual(bk, tk)) {
    const itən = bk.filter((s) => tk.indexOf(s) === -1);
    x.push('inline kod deyisib' + (itən.length ? ' — ' + itən.slice(0, 3).map((s) => '`' + s + '`').join(', ') : ''));
  }
  const bm = metnDeyeri(baza);
  const tm = metnDeyeri(terc);
  QADAGAN.forEach(({ s, re }) => {
    if (re.test(bm) && tm.indexOf(s) === -1) x.push('«' + s + '» tercume olunmamalidir');
  });
  const bmK = kicik(bm, L.BAZA);
  const tmK = kicik(tm, lc);
  Object.keys(luget.terms || {}).forEach((ad) => {
    const t = luget.terms[ad];
    const hedef = formalar(t, lc);
    if (!hedef.length || !formalar(t, L.BAZA).some((f) => bmK.includes(f))) return;
    if (!hedef.some((f) => tmK.includes(f))) x.push('luget: «' + t[L.BAZA] + '» → «' + t[lc] + '» gozlenilirdi');
  });
  return x;
}

/** glossary.json və localizedIo.json-un özü — xəta mətnləri. */
function lugetFayllariniYoxla() {
  const xetalar = [];
  const rel = (f) => path.relative(process.cwd(), f);
  const diller = Object.keys(dilAdlari());
  Object.keys(luget.terms || {}).forEach((ad) => {
    const t = luget.terms[ad];
    const yox = diller.filter((lc) => !t[lc]);
    if (yox.length) xetalar.push(rel(LUGET_FAYLI) + ': «' + ad + '» — ' + yox.join(', ') + ' qarşılığı yoxdur');
  });
  const calismalar = {};
  L.bazaOxu('exercises').topics.forEach((t) => t.praktiki.forEach((e) => { calismalar[e.id] = true; }));
  Object.keys(L.localizedIo()).forEach((id) => {
    if (!calismalar[id]) xetalar.push(rel(L.LOCALIZED_IO_FAYLI) + ': «' + id + '» praktiki calisma deyil');
  });
  return xetalar;
}

/** Tərcümənin quruluşu bazadakı ilə uyğundurmu — uyğunsuzluq mesajı və ya null. */
function qurulusYoxla(baza, terc, sahe) {
  if (typeof baza === 'string') {
    return typeof terc === 'string' ? null : 'setir gozlenilirdi';
  }
  if (!Array.isArray(baza)) return null;
  if (!Array.isArray(terc)) return 'massiv gozlenilirdi';
  if (sahe === 'opts' && terc.length !== baza.length) return 'opts sayi ' + terc.length + ' ≠ ' + baza.length;
  if ((sahe === 'items' || sahe === 'head' || sahe === 'rows') && terc.length !== baza.length) {
    return sahe + ' uzunlugu ' + terc.length + ' ≠ ' + baza.length;
  }
  if (sahe === 'head' || sahe === 'rows') {
    for (let i = 0; i < baza.length; i++) {
      if (!Array.isArray(terc[i]) || terc[i].length !== baza[i].length) return 'cedvel setri ' + i + ' hucre sayi ferqlidir';
    }
    return null;
  }
  // span siyahısı (spans, desc, title[hero], list items[i])
  const spanlar = (sahe === 'spans' || sahe === 'desc' || sahe === 'title') ? terc
    : (sahe === 'items' && baza.length && Array.isArray(baza[0])) ? [].concat.apply([], terc) : null;
  if (spanlar) {
    for (let i = 0; i < spanlar.length; i++) {
      const s = spanlar[i];
      if (!s || typeof s !== 'object' || !SPAN_NOVLERI[s.k]) return 'span ' + i + ' novu namelumdur: ' + JSON.stringify(s && s.k);
      if (s.k === 'link' && !s.href) return 'span ' + i + ': link href-siz';
    }
  }
  return null;
}

/* ---------- bir locale ---------- */

function localeYoxla(locale) {
  const n = { xetalar: [], xeberdarliqlar: [], say: {}, tam: L.tamdir(locale) };
  const aktivKoku = n.tam ? L.AKTIV_KOKU : L.AKTIV_KOKU_DEBUG;
  QRUPLAR.forEach((q) => { n.say[q] = { var: 0, cemi: 0 }; });
  n.say.ui = L.uiSayi(locale);

  if (locale === L.BAZA) {
    L.FAYLLAR.forEach((ad) => {
      L.YARPAQ[ad](L.bazaOxu(ad)).forEach((y) => {
        if (L.bosdur(y.deyer)) return;
        n.say[y.qrup].var++; n.say[y.qrup].cemi++;
      });
    });
    return n;
  }

  L.FAYLLAR.forEach((ad) => {
    const baza = L.bazaOxu(ad);
    const yarpaqlar = L.YARPAQ[ad](baza);
    const fayl = L.overlayYolu(locale, ad);
    const rel = path.relative(process.cwd(), fayl);

    if (!fs.existsSync(fayl)) {
      n.xeberdarliqlar.push(rel + ': overlay yoxdur — hamisi bazadan gelir');
      yarpaqlar.forEach((y) => { if (!L.bosdur(y.deyer)) n.say[y.qrup].cemi++; });
      return;
    }
    const metn = fs.readFileSync(fayl, 'utf8');
    let overlay;
    try { overlay = JSON.parse(metn); } catch (e) { n.xetalar.push(rel + ': JSON oxunmur — ' + e.message); return; }
    L.tekrarAcarlar(metn).forEach((y) => n.xetalar.push(rel + ': tekrar acar ' + y));

    const meta = overlay._meta || {};
    if (meta.locale !== locale) n.xetalar.push(rel + ': _meta.locale «' + meta.locale + '» qovluq adina (' + locale + ') uygun deyil');
    if (meta.base !== L.BAZA) n.xetalar.push(rel + ': _meta.base «' + meta.base + '» — baza ' + L.BAZA + ' olmalidir');

    // Naməlum / texniki açarlar
    const melum = {};
    yarpaqlar.forEach((y) => { melum[y.acar.join('\u0000')] = true; });
    const ovsuz = Object.assign({}, overlay); delete ovsuz._meta;
    L.overlayYollari(ovsuz, [], []).forEach((yol) => {
      if (!melum[yol.join('\u0000')]) {
        const son = yol[yol.length - 1];
        const texniki = ['a', 'code', 'output', 'level', 'index', 'special', 'sectionId', 'starter', 'gozlenilen', 'hell', 'no', 'id', 'year', 'num', 'lang', 'kind', 'ico', 'cols', 'ordered', 'lead', 't', 'demo', 'href', 'k'];
        n.xetalar.push(rel + ': ' + (texniki.indexOf(son) !== -1 ? 'texniki sahe tercume oluna bilmez' : 'namelum acar') + ' — ' + yol.join(' › '));
      }
    });

    // Yarpaq-yarpaq
    yarpaqlar.forEach((y) => {
      // Bazada boş sahə tərcümə edilə bilməz — faizə sayılmır (L.tamamlanma ilə eyni).
      if (L.bosdur(y.deyer)) return;
      const q = n.say[y.qrup]; q.cemi++;
      const v = L.overlayOxu(overlay, y.acar);
      if (L.bosdur(v)) return;
      const yol = y.acar.join(' › ');
      const problem = y.yoxla ? y.yoxla(v) : qurulusYoxla(y.deyer, v, y.acar[y.acar.length - 1]);
      if (problem) { n.xetalar.push(rel + ': ' + yol + ' — ' + problem); return; }
      if (deepEqual(v, y.deyer)) { n.xeberdarliqlar.push(rel + ': ' + yol + ' — bazadaki metnin eynisi (tercume olunmayib)'); return; }
      // Kod yarpağında yalnız şərh dəyişib (y.yoxla) — mətn qaydaları ona aid deyil.
      if (y.yoxla) { q.var++; return; }
      lugetYoxla(y.deyer, v, locale).forEach((x) => n.xeberdarliqlar.push(rel + ': ' + yol + ' — ' + x));
      const bazaN = formatNisanlari(metnDeyeri(y.deyer));
      const tercN = formatNisanlari(metnDeyeri(v));
      if (bazaN !== tercN) { n.xetalar.push(rel + ': ' + yol + ' — format nisanlari ferqlidir (' + (bazaN || '—') + ' → ' + (tercN || '—') + ')'); return; }
      q.var++;
    });

    // Qurulmuş aktiv overlay-in cari halına uyğundurmu və düzgün yerdədirmi
    // (T4.7): tam dil main-də, yarımçıq dil yalnız debug-da.
    const aktiv = path.join(aktivKoku, locale, ad + '.json');
    const aktivRel = path.relative(process.cwd(), aktiv);
    if (!fs.existsSync(aktiv)) {
      n.xetalar.push(aktivRel + ' yoxdur — node tools/build-locale.js ' + locale);
    } else {
      const gozlenilen = L.bazaOxu(ad);
      // build-locale ilə eyni: kodu dəyişmiş tərcümə tətbiq olunmur.
      L.YARPAQ[ad](gozlenilen).forEach((y) => {
        const v = L.overlayOxu(overlay, y.acar);
        if (!L.bosdur(v) && !(y.yoxla && y.yoxla(v))) y.qoy(v);
      });
      if (JSON.stringify(gozlenilen) !== fs.readFileSync(aktiv, 'utf8')) {
        n.xetalar.push(aktivRel + ' kohnelib — node tools/build-locale.js ' + locale);
      }
    }
  });

  const yanlisYer = path.join(n.tam ? L.AKTIV_KOKU_DEBUG : L.AKTIV_KOKU, locale);
  if (fs.existsSync(yanlisYer)) {
    n.xetalar.push(path.relative(process.cwd(), yanlisYer) + ' artiq burada olmamalidir (' +
      (n.tam ? 'dil tamdir — release-e kecir' : 'dil yarimciqdir — release-e dusmemelidir') +
      ') — node tools/build-locale.js ' + locale);
  }
  // UI sətirləri də eyni qapıdan keçir: yarımçıq dilin strings.xml-i main-də
  // olsa, sistem dili həmin dil olan cihazda release yarımçıq UI göstərər.
  if (locale !== L.BAZA) {
    const uiYanlis = path.join(n.tam ? L.RES_DEBUG : L.RES, 'values-' + locale, 'strings.xml');
    if (fs.existsSync(uiYanlis)) {
      n.xetalar.push(path.relative(process.cwd(), uiYanlis) + ' artiq burada olmamalidir (' +
        (n.tam ? 'dil tamdir — release-e kecir' : 'dil yarimciqdir — release-e dusmemelidir') +
        ') — node tools/build-locale.js ' + locale);
    }
  }

  return n;
}

/**
 * Release siyahıları (T4.7): main locales.json və locales_config.xml məhz
 * baza + tam dillər olmalıdır. Uyğunsuzluq — yarımçıq dil istifadəçiyə
 * sızır və ya tam dil görünmür.
 */
function siyahilariYoxla() {
  const xetalar = [];
  const gozlenilen = [L.BAZA].concat(L.localeSiyahisi().filter((lc) => L.tamdir(lc))).sort();
  const cem = (a) => a.slice().sort().join(', ');
  const rel = (f) => path.relative(process.cwd(), f);
  const siyahiFayli = path.join(L.AKTIV_KOKU, 'locales.json');
  const json = fs.existsSync(siyahiFayli) ? JSON.parse(fs.readFileSync(siyahiFayli, 'utf8')).locales || [] : [];
  if (cem(json) !== cem(gozlenilen)) {
    xetalar.push(rel(siyahiFayli) + ': [' + cem(json) + '] — gozlenilen [' + cem(gozlenilen) + '] — node tools/build-locale.js --all');
  }
  const config = L.localeConfigOxu(L.LOCALE_CONFIG);
  if (cem(config) !== cem(gozlenilen)) {
    xetalar.push(rel(L.LOCALE_CONFIG) + ': [' + cem(config) + '] — gozlenilen [' + cem(gozlenilen) + '] — node tools/build-locale.js --all');
  }
  const hamisi = L.localeConfigOxu(L.LOCALE_CONFIG_DEBUG);
  gozlenilen.filter((lc) => hamisi.indexOf(lc) === -1).forEach((lc) => {
    xetalar.push(rel(L.LOCALE_CONFIG_DEBUG) + ': «' + lc + '» yoxdur — AppDil-e ve bu fayla elave et');
  });
  return xetalar;
}

/* ---------- icra ---------- */

const localelar = secilen.length ? secilen : [L.BAZA].concat(L.localeSiyahisi());
const netice = {};
localelar.forEach((lc) => { netice[lc] = localeYoxla(lc); });
const siyahiXetalari = siyahilariYoxla().concat(lugetFayllariniYoxla());

function faiz(s) { return s.cemi ? Math.round((s.var / s.cemi) * 100) : 100; }
function umumi(say) {
  let v = 0; let c = 0;
  QRUPLAR.forEach((q) => { v += say[q].var; c += say[q].cemi; });
  return faiz({ var: v, cemi: c });
}

/* ---------- README dil cədvəli (T5.7) ---------- */

/** Dillərin öz adları — AppDil.kt-dəki enum-dan (tək mənbə). */
function dilAdlari() {
  const kt = path.resolve(__dirname, '..', 'app', 'src', 'main', 'java', 'az', 'kotlinaz', 'app', 'data', 'AppDil.kt');
  const m = {};
  const re = /^\s*[A-Z_]+\("([^"]+)", "([^"]+)"/gm;
  let x;
  const metn = fs.readFileSync(kt, 'utf8');
  while ((x = re.exec(metn)) !== null) m[x[1]] = x[2];
  return m;
}

function markdownBloku() {
  const adlar = dilAdlari();
  const hazir = localelar.filter((lc) => netice[lc].tam);
  const setirler = [
    '| Dil | Kod | Tərcümə | Vəziyyət |',
    '|---|---|---|---|'
  ];
  localelar.forEach((lc) => {
    const n = netice[lc];
    const hal = lc === L.BAZA ? 'baza' : n.tam ? 'tətbiqdə' : 'hazırlanır (yalnız debug build)';
    setirler.push('| ' + (adlar[lc] || lc) + ' | `' + lc + '` | ' + umumi(n.say) + ' % | ' + hal + ' |');
  });
  const planli = Object.keys(adlar).filter((lc) => localelar.indexOf(lc) === -1);
  if (planli.length) {
    setirler.push('');
    setirler.push('Planlaşdırılan (overlay hələ yoxdur): ' + planli.map((lc) => adlar[lc]).join(', ') + '.');
  }
  const nisan = '[![Dillər](https://img.shields.io/badge/dill%C9%99r-' + hazir.length + '%20%2F%20' +
    Object.keys(adlar).length + '-7F52FF)](#dil-dəstəyi)';
  return { cedvel: setirler.join('\n'), nisan: nisan };
}

/** README.md-dəki `<!-- ad:basla -->…<!-- ad:son -->` bloklarını yeniləyir. */
function readmeYenile(blok) {
  const yol = path.resolve(__dirname, '..', 'README.md');
  let md = fs.readFileSync(yol, 'utf8');
  // Windows checkout-u (autocrlf) CRLF verir — blok da faylın sətir sonu ilə yazılır.
  const eol = md.includes('\r\n') ? '\r\n' : '\n';
  const evez = (ad, deyer) => {
    const re = new RegExp('(<!-- ' + ad + ':basla -->)[\\s\\S]*?(<!-- ' + ad + ':son -->)');
    if (!re.test(md)) throw new Error('README.md: <!-- ' + ad + ':basla/son --> nişanları yoxdur');
    md = md.replace(re, (_, a, b) => a + eol + deyer.split('\n').join(eol) + eol + b);
  };
  evez('diller', blok.cedvel);
  evez('dil-nisani', blok.nisan);
  fs.writeFileSync(yol, md, 'utf8');
}

let xetaVar = false;
if (MARKDOWN || README) {
  const blok = markdownBloku();
  if (README) readmeYenile(blok);
  console.log(blok.nisan + '\n\n' + blok.cedvel);
  localelar.forEach((lc) => { if (netice[lc].xetalar.length) xetaVar = true; });
  if (siyahiXetalari.length) xetaVar = true;
} else if (JSON_CIXIS) {
  const cixis = {};
  localelar.forEach((lc) => {
    const n = netice[lc];
    const f = {};
    QRUPLAR.forEach((q) => { f[q] = faiz(n.say[q]); });
    f.umumi = umumi(n.say);
    cixis[lc] = { faiz: f, gorunen: n.tam, say: n.say, xetalar: n.xetalar, xeberdarliq: n.xeberdarliqlar.length };
    if (n.xetalar.length) xetaVar = true;
  });
  if (siyahiXetalari.length) { cixis._siyahilar = { xetalar: siyahiXetalari }; xetaVar = true; }
  console.log(JSON.stringify(cixis, null, 2));
} else {
  console.log('locale  UI      dersler  calismalar  quiz    meydan  umumi   xeta  xeberdarliq  APK');
  localelar.forEach((lc) => {
    const n = netice[lc];
    const s = (q) => (faiz(n.say[q]) + '%').padStart(4);
    console.log(
      lc.padEnd(8) + s('ui') + '    ' + s('dersler') + '     ' + s('calismalar') + '        ' + s('quiz') + '    ' + s('meydan') + '    ' + (umumi(n.say) + '%').padStart(4) +
      '    ' + String(n.xetalar.length).padStart(4) + '  ' + String(n.xeberdarliqlar.length).padStart(4) +
      '         ' + (n.tam ? 'release' : 'debug')
    );
  });
  if (siyahiXetalari.length) {
    xetaVar = true;
    console.log('');
    console.log('siyahilar — xetalar (' + siyahiXetalari.length + '):');
    siyahiXetalari.forEach((x) => console.log('  ! ' + x));
  }
  localelar.forEach((lc) => {
    const n = netice[lc];
    if (n.xetalar.length) {
      xetaVar = true;
      console.log('');
      console.log(lc + ' — xetalar (' + n.xetalar.length + '):');
      n.xetalar.slice(0, 30).forEach((x) => console.log('  ! ' + x));
      if (n.xetalar.length > 30) console.log('  … daha ' + (n.xetalar.length - 30));
    }
    if (n.xeberdarliqlar.length) {
      console.log('');
      console.log(lc + ' — xeberdarliqlar (' + n.xeberdarliqlar.length + '):');
      n.xeberdarliqlar.slice(0, 10).forEach((x) => console.log('  ? ' + x));
      if (n.xeberdarliqlar.length > 10) console.log('  … daha ' + (n.xeberdarliqlar.length - 10));
    }
  });
}

process.exit(xetaVar ? 2 : 0);
