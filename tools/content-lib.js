/* ============================================================
   KotlinAZ — məzmun alətlərinin ortaq hissəsi (T3.2)

   build-content.js, build-locale.js, validate-locales.js və
   new-locale.js bu modulu paylaşır:
     - blok mətninin çıxarılması və axtarış indeksi
     - tərcümə olunan sahələrin («yarpaqların») siyahısı — overlay
       formatının vahid tərifi
     - dil qapısı (T4.7): tamamlanma faizi və aktivlərin yeri
       (tam dil → main/release, yarımçıq → debug)
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');
const K = require('./kod-serhleri');

const APP_SRC = path.resolve(__dirname, '..', 'app', 'src');
const AKTIV_KOKU = path.join(APP_SRC, 'main', 'assets', 'content');
/**
 * Yarımçıq dillərin aktivləri (T4.7) — yalnız debug APK-ya düşür. Android
 * debug source set-inin aktivlərini main-in üstünə birləşdirir: buradakı
 * locales.json debug-da əsasınkını əvəzləyir.
 */
const AKTIV_KOKU_DEBUG = path.join(APP_SRC, 'debug', 'assets', 'content');
const RES = path.join(APP_SRC, 'main', 'res');
const RES_DEBUG = path.join(APP_SRC, 'debug', 'res');
/** Release-in dil siyahısı (Android 13+ sistem menyusu) — build-locale.js yazır. */
const LOCALE_CONFIG = path.join(RES, 'xml', 'locales_config.xml');
/** Bütün dillər, AppDil sırası ilə — əl ilə saxlanılır, AppDilTest yoxlayır. */
const LOCALE_CONFIG_DEBUG = path.join(APP_SRC, 'debug', 'res', 'xml', 'locales_config.xml');
const LOCALIZATION = path.resolve(__dirname, '..', 'localization');
const BAZA = 'az';
const FAYLLAR = ['content', 'exercises', 'quiz', 'playground'];

/* ------------------------------------------------------------
   Mətn çıxarışı (build-content.js-dən köçürülüb)
   ------------------------------------------------------------ */

function spanMetni(spans) {
  return (spans || []).map((s) => (s.k === 'br' ? ' ' : s.v)).join('');
}

/**
 * Blokların mətni. Tətbiqdəki axtarış indeksi (AxtarisIndeksi.kt, T3.5)
 * eyni qaydanı Kotlin-də təkrarlayır — birini dəyişəndə o birini də dəyiş.
 */
function bloklardanMetn(blocks, topla) {
  (blocks || []).forEach((b) => {
    if (!b) return;
    switch (b.t) {
      case 'h2': case 'h3': case 'h4': case 'h5':
        topla.push(b.text); break;
      case 'p':
        topla.push(spanMetni(b.spans)); break;
      case 'code':
        topla.push(b.title); topla.push(b.code); break;
      case 'callout':
        topla.push(b.title); bloklardanMetn(b.blocks, topla); break;
      case 'cards':
        b.items.forEach((it) => { topla.push(it.title); bloklardanMetn(it.blocks, topla); });
        break;
      case 'table':
        (b.head || []).forEach((r) => r.forEach((c) => topla.push(spanMetni(c))));
        (b.rows || []).forEach((r) => r.forEach((c) => topla.push(spanMetni(c))));
        break;
      case 'list':
        b.items.forEach((it) => topla.push(spanMetni(it))); break;
      case 'timeline':
        b.items.forEach((it) => { topla.push(it.year); topla.push(it.title); topla.push(spanMetni(it.desc)); });
        break;
      case 'tabs':
        b.tabs.forEach((tb) => { topla.push(tb.label); bloklardanMetn(tb.blocks, topla); });
        break;
      case 'hero':
        topla.push(b.badge); topla.push(spanMetni(b.title)); topla.push(b.sub);
        (b.stats || []).forEach((s) => { topla.push(s.num); topla.push(s.lbl); });
        break;
      default: break;
    }
  });
}

/* ------------------------------------------------------------
   Tərcümə yarpaqları
   Hər yarpaq: { acar: ['sections','sintaksis','blocks','<id>','spans'],
                 deyer: <bazadakı dəyər>, qoy: (yeni) => … , qrup }
   `acar` overlay-dəki yol, `qoy` isə həmin dəyəri baza sənədinə yazan
   funksiyadır — build-locale bu ikisini birləşdirir.
   Texniki sahələr (cavab indeksi, səviyyə, id, çıxış…) yarpaq DEYİL —
   overlay-də görünsələr validator xəta verir.

   Kod (T4.0): kod blokunun, başlanğıc kodun, model həllin və meydan
   nümunəsinin kodu yalnız ŞƏRHİ varsa yarpaqdır; tərcümədə yalnız
   şərh dəyişə bilər (`localizedIo` çalışmasında həm də literal mətni).
   Belə yarpağın `yoxla(v)` funksiyası var — kod dəyişibsə izah qaytarır;
   tamamlanma onu saymır, build-locale bazanı saxlayır, validator xəta verir.
   ------------------------------------------------------------ */

function yarpaq(siyahi, acar, obj, sahe, qrup, yoxla) {
  if (obj[sahe] === undefined) return;
  const y = {
    acar: acar.concat([sahe]),
    deyer: obj[sahe],
    qoy: (v) => { obj[sahe] = v; },
    qrup: qrup
  };
  if (yoxla) y.yoxla = yoxla;
  siyahi.push(y);
}

/** Kod yarpağı — yalnız tərcümə olunası şərh (və ya io literalı) varsa. */
function kodYarpagi(siyahi, acar, obj, sahe, qrup, literallar) {
  const baza = obj[sahe];
  if (!K.tercumeOlunur(baza, literallar)) return;
  yarpaq(siyahi, acar, obj, sahe, qrup, (v) => K.kodFerqi(baza, v, literallar));
}

/**
 * `localizedIo` çalışmaları (T4.0): giriş/çıxışı dilə bağlı olanlar —
 * starter, hell və gozlenilen BİRLİKDƏ tərcümə olunur. Siyahı baza
 * məzmunundan ayrıdır: exercises.json saytdan yenidən qurulur.
 */
const LOCALIZED_IO_FAYLI = path.join(LOCALIZATION, 'localizedIo.json');
let _localizedIo = null;
function localizedIo() {
  if (_localizedIo === null) {
    _localizedIo = fs.existsSync(LOCALIZED_IO_FAYLI)
      ? JSON.parse(fs.readFileSync(LOCALIZED_IO_FAYLI, 'utf8')).calismalar || {}
      : {};
  }
  return _localizedIo;
}

function blokYarpaqlari(siyahi, blocks, bolmeAcari, qrup) {
  (blocks || []).forEach((b) => {
    if (!b || !b.id) {
      // id-siz bloklar (hr, quiz, playground, demo) tərcümə olunmur
      return;
    }
    const acar = bolmeAcari.concat(['blocks', b.id]);
    switch (b.t) {
      case 'h2': case 'h3': case 'h4': case 'h5':
        yarpaq(siyahi, acar, b, 'text', qrup); break;
      case 'p':
        yarpaq(siyahi, acar, b, 'spans', qrup); break;
      case 'code':
        // başlıq və şərhlər; kodun özü və output bazadan (T4.0 siyasəti)
        yarpaq(siyahi, acar, b, 'title', qrup);
        if ((b.lang || 'kotlin') === 'kotlin') kodYarpagi(siyahi, acar, b, 'code', qrup, false);
        break;
      case 'callout':
        yarpaq(siyahi, acar, b, 'title', qrup);
        blokYarpaqlari(siyahi, b.blocks, bolmeAcari, qrup);
        break;
      case 'cards':
        (b.items || []).forEach((it, i) => {
          yarpaq(siyahi, acar.concat(['items', String(i)]), it, 'title', qrup);
          blokYarpaqlari(siyahi, it.blocks, bolmeAcari, qrup);
        });
        break;
      case 'table':
        yarpaq(siyahi, acar, b, 'head', qrup);
        yarpaq(siyahi, acar, b, 'rows', qrup);
        break;
      case 'list':
        yarpaq(siyahi, acar, b, 'items', qrup); break;
      case 'timeline':
        (b.items || []).forEach((it, i) => {
          const a = acar.concat(['items', String(i)]);
          yarpaq(siyahi, a, it, 'title', qrup);
          yarpaq(siyahi, a, it, 'desc', qrup);
        });
        break;
      case 'tabs':
        (b.tabs || []).forEach((tb, i) => {
          yarpaq(siyahi, acar.concat(['tabs', String(i)]), tb, 'label', qrup);
          blokYarpaqlari(siyahi, tb.blocks, bolmeAcari, qrup);
        });
        break;
      case 'hero':
        yarpaq(siyahi, acar, b, 'badge', qrup);
        yarpaq(siyahi, acar, b, 'title', qrup);
        yarpaq(siyahi, acar, b, 'sub', qrup);
        (b.stats || []).forEach((st, i) => yarpaq(siyahi, acar.concat(['stats', String(i)]), st, 'lbl', qrup));
        break;
      default: break;
    }
  });
}

/** content.json → yarpaqlar */
function contentYarpaqlari(doc) {
  const siyahi = [];
  (doc.sections || []).forEach((s) => {
    const acar = ['sections', s.id];
    yarpaq(siyahi, acar, s, 'title', 'dersler');
    yarpaq(siyahi, acar, s, 'heading', 'dersler');
    yarpaq(siyahi, acar, s, 'kicker', 'dersler');
    yarpaq(siyahi, acar, s, 'group', 'dersler');
    blokYarpaqlari(siyahi, s.blocks, acar, 'dersler');
  });
  return siyahi;
}

/**
 * exercises.json → yarpaqlar. starter/hell yalnız şərhlərinə görə;
 * gozlenilen yalnız localizedIo çalışmasında; nəzəri `code` texnikidir.
 */
function exercisesYarpaqlari(doc) {
  const siyahi = [];
  (doc.topics || []).forEach((t) => {
    const acar = ['topics', t.id];
    yarpaq(siyahi, acar, t, 'title', 'calismalar');
    (t.nezeri || []).forEach((e) => {
      const a = acar.concat(['nezeri', e.id]);
      yarpaq(siyahi, a, e, 'q', 'calismalar');
      yarpaq(siyahi, a, e, 'opts', 'calismalar');
      yarpaq(siyahi, a, e, 'exp', 'calismalar');
    });
    (t.praktiki || []).forEach((e) => {
      const a = acar.concat(['praktiki', e.id]);
      yarpaq(siyahi, a, e, 'tapsiriq', 'calismalar');
      yarpaq(siyahi, a, e, 'ipucu', 'calismalar');
      const io = Object.prototype.hasOwnProperty.call(localizedIo(), e.id);
      kodYarpagi(siyahi, a, e, 'starter', 'calismalar', io);
      kodYarpagi(siyahi, a, e, 'hell', 'calismalar', io);
      if (io) yarpaq(siyahi, a, e, 'gozlenilen', 'calismalar');
    });
  });
  return siyahi;
}

/** quiz.json → yarpaqlar. `a` və `code` texnikidir. */
function quizYarpaqlari(doc) {
  const siyahi = [];
  (doc.questions || []).forEach((q) => {
    const a = ['questions', q.id];
    yarpaq(siyahi, a, q, 'q', 'quiz');
    yarpaq(siyahi, a, q, 'opts', 'quiz');
    yarpaq(siyahi, a, q, 'exp', 'quiz');
  });
  return siyahi;
}

/** playground.json → yarpaqlar. `label` və `code`-un şərhləri. */
function playgroundYarpaqlari(doc) {
  const siyahi = [];
  (doc.presets || []).forEach((p) => {
    yarpaq(siyahi, ['presets', p.id], p, 'label', 'meydan');
    kodYarpagi(siyahi, ['presets', p.id], p, 'code', 'meydan', false);
  });
  return siyahi;
}

const YARPAQ = {
  content: contentYarpaqlari,
  exercises: exercisesYarpaqlari,
  quiz: quizYarpaqlari,
  playground: playgroundYarpaqlari
};

/* ------------------------------------------------------------
   Overlay oxu / yaz köməkçiləri
   ------------------------------------------------------------ */

/** Overlay-dən yol üzrə dəyəri oxu; yoxdursa undefined. */
function overlayOxu(overlay, acar) {
  let cari = overlay;
  for (let i = 0; i < acar.length; i++) {
    if (cari === null || typeof cari !== 'object') return undefined;
    cari = cari[acar[i]];
    if (cari === undefined) return undefined;
  }
  return cari;
}

/** Overlay-ə yol üzrə dəyəri yaz (ara obyektləri yaradır). */
function overlayYaz(overlay, acar, deyer) {
  let cari = overlay;
  for (let i = 0; i < acar.length - 1; i++) {
    if (cari[acar[i]] === undefined) cari[acar[i]] = {};
    cari = cari[acar[i]];
  }
  cari[acar[acar.length - 1]] = deyer;
}

/** Dəyər «boş»dursa (tərcümə yazılmayıb) true. */
function bosdur(v) {
  if (v === undefined || v === null) return true;
  if (typeof v === 'string') return v.trim() === '';
  if (Array.isArray(v)) return v.length === 0;
  return false;
}

/* ------------------------------------------------------------
   Dildən asılı olmayan yarpaq (T4.1)

   Bəzi dəyərlərdə tərcümə olunası söz yoxdur: `["null", "-1", "0",
   "NullPointerException"]` variantları, «data class» etiketi, `[1, 4, 9]`.
   Onlar hər dildə bazadakı kimi qalır — faizə sayılsalar dil heç vaxt
   100 %-ə çatmazdı. Bazada boş sahə kimi saydan çıxarılır, «bazadakının
   eynisi» xəbərdarlığı vermir, overlay-parca.js onları çıxarmır.

   Qayda mühafizəkardır: dəyərdəki HƏR söz kod kimi görünməlidir — inline
   kod span-ı, `f()`, `a.b`, camelCase, rəqəm/simvol, null/true/false,
   glossary.doNotTranslate və ya aşağıdakı stdlib tip adları. Bir adi söz
   («Bəli», «yalnız») kifayətdir ki, yarpaq tərcümə olunası sayılsın.
   Siyahı: node tools/overlay-parca.js neytral <fayl>
   ------------------------------------------------------------ */

const KOD_ADLARI = ['class', 'interface', 'enum', 'Int', 'Long', 'Short', 'Byte', 'Double', 'Float',
  'Boolean', 'Char', 'String', 'Unit', 'Any', 'Nothing', 'List', 'Set', 'Map', 'Array', 'Pair',
  'Triple', 'Sequence', 'Result', 'Regex', 'Exception', 'Throwable', 'Job', 'Deferred'];

let _neytralSozler = null;
function neytralSozler() {
  if (_neytralSozler === null) {
    const f = path.join(LOCALIZATION, 'glossary.json');
    const dnt = fs.existsSync(f) ? (JSON.parse(fs.readFileSync(f, 'utf8')).doNotTranslate || []) : [];
    // Uzun ifadələr əvvəl: «data class» «class»-dan qabaq silinsin.
    _neytralSozler = dnt.concat(KOD_ADLARI).sort((a, b) => b.length - a.length)
      .map((s) => new RegExp('(^|[^\\p{L}\\p{N}_])' + s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '(?=$|[^\\p{L}\\p{N}_])', 'gu'));
  }
  return _neytralSozler;
}

/** Dəyərin adi mətni; inline kod span-ları atılır (onlar onsuz da dəyişmir). */
function neytralMetn(v) {
  if (typeof v === 'string') return v;
  if (Array.isArray(v)) return v.map(neytralMetn).join(' ');
  if (v && typeof v === 'object') return v.k === 'code' ? ' ' : (v.v || '');
  return '';
}

function kodSozudur(t) {
  // Cümlə durğusu və mötərizə kod əlaməti deyil: «nədir?», «yaz.»,
  // «(tam bölmə!)» adi sözlərdir; `start()` isə koddur — `(` içindədir.
  t = t.replace(/^[([]+/, '');
  if (t.indexOf('(') === -1) t = t.replace(/[)\]]+$/, '');
  t = t.replace(/[.?!:]+$/, '');
  if (!/\p{L}/u.test(t)) return true;                    // rəqəm, simvol
  if (/[()<>{}=.?!_$:[\]]/.test(t)) return true;         // f(), a.b, x?.y, List<T>
  if (/^(null|true|false)$/.test(t)) return true;
  if (/^[a-z]+[A-Z]\w*$/.test(t) || /^[A-Z][a-z0-9]+[A-Z]\w*$/.test(t)) return true; // camelCase, PascalCase
  return false;
}

function dilNeytraldir(v) {
  if (bosdur(v)) return false;
  let m = neytralMetn(v);
  neytralSozler().forEach((re) => { m = m.replace(re, '$1 '); });
  return m.split(/[\s,;/«»“”"'—–-]+/).filter(Boolean).every(kodSozudur);
}

/** Bazadakı dəyərin tipinə uyğun boş dəyər — skelet üçün. */
function bosNusxe(v) {
  return Array.isArray(v) ? [] : '';
}

/** Overlay-dəki bütün yarpaq yollarını (dəyəri obyekt olmayan) topla. */
function overlayYollari(obj, acar, topla) {
  Object.keys(obj).forEach((k) => {
    const v = obj[k];
    const yol = acar.concat([k]);
    if (v !== null && typeof v === 'object' && !Array.isArray(v)) overlayYollari(v, yol, topla);
    else topla.push(yol);
  });
  return topla;
}

function bazaOxu(ad, locale) {
  const yol = path.join(AKTIV_KOKU, locale || BAZA, ad + '.json');
  return JSON.parse(fs.readFileSync(yol, 'utf8'));
}

function overlayYolu(locale, ad) {
  return path.join(LOCALIZATION, locale, ad + '.json');
}

/** localization/ altındakı dil qovluqları (baza istisna). */
function localeSiyahisi() {
  if (!fs.existsSync(LOCALIZATION)) return [];
  return fs.readdirSync(LOCALIZATION, { withFileTypes: true })
    .filter((d) => d.isDirectory() && d.name !== BAZA)
    .map((d) => d.name)
    .sort();
}

/* ------------------------------------------------------------
   Dil qapısı (T4.7): yalnız tam tərcümə olunmuş dil release-ə düşür
   ------------------------------------------------------------ */

/**
 * strings.xml-dəki tərcümə olunan sətirlərin sayı. `translatable="false"`
 * (məsələn tətbiqin adı) sayılmır — onu tərcüməçi dəyişə bilməz.
 */
function uiSetirSayi(fayl) {
  if (!fs.existsSync(fayl)) return 0;
  return (fs.readFileSync(fayl, 'utf8').match(/<string\s[^>]*>/g) || [])
    .filter((teq) => /\sname="/.test(teq) && !/translatable="false"/.test(teq)).length;
}

/**
 * Dilin UI sətirləri faylı. Aktivlər kimi (T4.7): yarımçıq dil
 * src/debug/res-dədir, tam dil src/main/res-də — yoxsa sistem dili həmin
 * dil olan cihazda release yarımçıq UI göstərərdi. Faylı build-locale.js
 * köçürür; yeni dil debug-da başlayır. Hələ yoxdursa debug yolu qaytarılır.
 */
function uiFayli(locale) {
  const main = path.join(RES, 'values-' + locale, 'strings.xml');
  const debug = path.join(RES_DEBUG, 'values-' + locale, 'strings.xml');
  return fs.existsSync(main) ? main : debug;
}

/** UI sətirləri: tərcümə olunan / ümumi. */
function uiSayi(locale) {
  const baza = uiSetirSayi(path.join(RES, 'values', 'strings.xml'));
  if (locale === BAZA) return { var: baza, cemi: baza };
  return { var: Math.min(baza, uiSetirSayi(uiFayli(locale))), cemi: baza };
}

/**
 * Dilin ümumi tamamlanması: məzmun yarpaqları + UI sətirləri. Yarpaq o
 * zaman sayılır ki, overlay-də boş deyil və bazanın eynisi deyil —
 * validate-locales.js-in faizi ilə eyni qayda (struktur xətaları istisna:
 * onları validator ayrıca tutur və CI-ı sındırır).
 */
function tamamlanma(locale) {
  const ui = uiSayi(locale);
  let v = ui.var; let c = ui.cemi;
  FAYLLAR.forEach((ad) => {
    const baza = bazaOxu(ad);
    const fayl = overlayYolu(locale, ad);
    const overlay = locale !== BAZA && fs.existsSync(fayl) ? JSON.parse(fs.readFileSync(fayl, 'utf8')) : null;
    YARPAQ[ad](baza).forEach((y) => {
      // Bazada boş olan sahə (məsələn başlıqsız kod bloku) tərcümə edilə
      // bilməz — sayılsa dil heç vaxt 100 %-ə çatmazdı.
      if (bosdur(y.deyer) || dilNeytraldir(y.deyer)) return;
      c++;
      if (locale === BAZA) { v++; return; }
      const d = overlay ? overlayOxu(overlay, y.acar) : undefined;
      // Kodu dəyişmiş tərcümə sayılmır — build-locale onu onsuz da atır.
      if (!bosdur(d) && JSON.stringify(d) !== JSON.stringify(y.deyer) && !(y.yoxla && y.yoxla(d))) v++;
    });
  });
  return { var: v, cemi: c };
}

/** Dil istifadəçiyə görünürmü: baza həmişə, qalanı yalnız 100 %-də. */
function tamdir(locale) {
  if (locale === BAZA) return true;
  const s = tamamlanma(locale);
  return s.cemi > 0 && s.var === s.cemi;
}

/** Dilin aktivləri hara yazılır: tamdırsa main (release), deyilsə debug. */
function aktivKoku(locale) {
  return tamdir(locale) ? AKTIV_KOKU : AKTIV_KOKU_DEBUG;
}

/** locales_config.xml-dəki dillər, fayldakı sıra ilə. */
function localeConfigOxu(fayl) {
  if (!fs.existsSync(fayl)) return [];
  return (fs.readFileSync(fayl, 'utf8').match(/<locale\s+android:name="[^"]+"/g) || [])
    .map((s) => s.match(/"([^"]+)"/)[1]);
}

/**
 * Release-in locales_config.xml mətni: [diller] debug faylındakı (AppDil)
 * sıra ilə; orada olmayan dil (AppDil-ə hələ əlavə olunmayıb) sona düşür.
 */
function localeConfigMetni(diller) {
  const sira = localeConfigOxu(LOCALE_CONFIG_DEBUG);
  const siralanmis = sira.filter((lc) => diller.indexOf(lc) !== -1)
    .concat(diller.filter((lc) => sira.indexOf(lc) === -1));
  return '<?xml version="1.0" encoding="utf-8"?>\n' +
    '<!-- AVTOMATIK YARADILIR — node tools/build-locale.js. Əl ilə dəyişmə.\n' +
    '     Release-də Android 13+ sistem menyusunda yalnız tam tərcümə olunmuş\n' +
    '     dillər görünür (T4.7). Bütün dillər: src/debug/res/xml/locales_config.xml. -->\n' +
    '<locale-config xmlns:android="http://schemas.android.com/apk/res/android">\n' +
    siralanmis.map((lc) => '    <locale android:name="' + lc + '" />\n').join('') +
    '</locale-config>\n';
}

/**
 * JSON mətnində təkrar açar axtarışı. JSON.parse sonuncunu götürüb
 * susur — tərcüməçi eyni id-ni iki dəfə yazsa biri itərdi.
 * Kiçik skaner: sətir / obyekt yığını izlənir.
 */
function tekrarAcarlar(metn) {
  const problemler = [];
  const yigin = [];
  let i = 0;
  const n = metn.length;
  let sonAcar = null;
  while (i < n) {
    const c = metn[i];
    if (c === '"') {
      let j = i + 1; let s = '';
      while (j < n && metn[j] !== '"') { if (metn[j] === '\\') { s += metn[j]; j++; } s += metn[j]; j++; }
      // açar, yoxsa dəyər? — sonrakı boşluqsuz simvol ':'-dursa açardır
      let k = j + 1; while (k < n && /\s/.test(metn[k])) k++;
      if (metn[k] === ':' && yigin.length) {
        const cari = yigin[yigin.length - 1];
        if (cari.acarlar[s]) problemler.push(cari.yol.concat([s]).join('.'));
        cari.acarlar[s] = true;
        sonAcar = s;
      }
      i = j + 1; continue;
    }
    if (c === '{') { yigin.push({ acarlar: {}, yol: yigin.length ? yigin[yigin.length - 1].yol.concat([sonAcar]) : [] }); }
    else if (c === '}') { yigin.pop(); }
    else if (c === '[') { yigin.push({ acarlar: {}, yol: yigin.length ? yigin[yigin.length - 1].yol.concat([sonAcar]) : [], massiv: true }); }
    else if (c === ']') { yigin.pop(); }
    i++;
  }
  return problemler;
}

module.exports = {
  AKTIV_KOKU, AKTIV_KOKU_DEBUG, RES, RES_DEBUG, uiFayli, LOCALE_CONFIG, LOCALE_CONFIG_DEBUG, LOCALIZATION, BAZA, FAYLLAR,
  spanMetni, bloklardanMetn,
  YARPAQ, overlayOxu, overlayYaz, overlayYollari, bosdur, dilNeytraldir, bosNusxe,
  bazaOxu, overlayYolu, localeSiyahisi, tekrarAcarlar, localizedIo, LOCALIZED_IO_FAYLI,
  uiSayi, tamamlanma, tamdir, aktivKoku, localeConfigOxu, localeConfigMetni
};
