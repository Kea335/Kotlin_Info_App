/* ============================================================
   KotlinAZ — məzmun boru xətti
   Saytın index.html, exercises.js, demos.js və playground.js
   fayllarını oxuyub Android tətbiqi üçün oflayn JSON aktivləri
   yaradır.

   İşlətmək:  node tools/build-content.js
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');
const vm = require('vm');
const crypto = require('crypto');
const H = require('./html-parse');
const { bloklardanMetn } = require('./content-lib');

const SAYT = process.env.KOTLINAZ_WEB || path.resolve('C:/Users/PC-01/Desktop/kotlinweb');
// KOTLINAZ_HEDEF — testlərin aktivləri müvəqqəti qovluğa yazması üçün.
// Baza dil (az) saytdan qurulur — assets/content/az/. Digər dillər overlay-dən
// tools/build-locale.js ilə yaranır (T3.2).
const HEDEF = process.env.KOTLINAZ_HEDEF || path.resolve(__dirname, '..', 'app', 'src', 'main', 'assets', 'content', 'az');

if (!fs.existsSync(SAYT)) {
  console.error('XETA: sayt qovlugu tapilmadi: ' + SAYT);
  process.exit(1);
}
fs.mkdirSync(HEDEF, { recursive: true });

const xeberdarliqlar = [];
function xeber(m) { xeberdarliqlar.push(m); }

/* ============================================================
   1. Sətir daxili elementlər → span siyahısı
   ============================================================ */

const INLINE_KIND = { b: 'b', strong: 'b', i: 'i', em: 'i' };

function spansOf(node) {
  const out = [];

  function push(kind, text, extra) {
    if (!text) return;
    const s = { k: kind, v: text };
    if (extra) Object.assign(s, extra);
    out.push(s);
  }

  function walk(n, kind, href) {
    (n.children || []).forEach((c) => {
      if (c.type === 'text') {
        push(kind, c.text, href ? { href: href } : null);
        return;
      }
      if (c.type !== 'element') return;

      const name = c.name;
      if (name === 'br') { out.push({ k: 'br' }); return; }

      if (name === 'code') {
        push('code', H.textOf(c), href ? { href: href } : null);
        return;
      }
      if (name === 'a') {
        const target = c.attrs['href'] || '';
        walk(c, 'link', target);
        return;
      }
      if (INLINE_KIND[name]) { walk(c, INLINE_KIND[name], href); return; }
      if (name === 'span' && H.hasClass(c, 'kbd')) { push('kbd', H.textOf(c)); return; }
      if (name === 'span' && H.hasClass(c, 'grad')) { walk(c, 'grad', href); return; }

      walk(c, kind, href);
    });
  }

  walk(node, 't', null);

  // Bosluqlari sadelesdir, bos spanlari at
  const temiz = [];
  out.forEach((s) => {
    if (s.k === 'br') { temiz.push(s); return; }
    if (s.k === 'code' || s.k === 'kbd') { temiz.push(s); return; }
    const v = s.v.replace(/\s+/g, ' ');
    if (!v) return;
    const son = temiz[temiz.length - 1];
    if (son && son.k === s.k && son.href === s.href) {
      son.v += v;
    } else {
      temiz.push(Object.assign({}, s, { v: v }));
    }
  });

  // Bas ve son bosluqlari kirp
  if (temiz.length) {
    const ilk = temiz[0];
    if (ilk.k === 't') ilk.v = ilk.v.replace(/^\s+/, '');
    const son = temiz[temiz.length - 1];
    if (son.k === 't') son.v = son.v.replace(/\s+$/, '');
  }
  return temiz.filter((s) => s.k === 'br' || s.v !== '');
}

function duzMetn(node) {
  return H.textOf(node).replace(/\s+/g, ' ').trim();
}

/* ============================================================
   2. Blok elementləri
   ============================================================ */

const BASLIQ = { h2: 'h2', h3: 'h3', h4: 'h4', h5: 'h5' };

/** <div class="code-card"> → kod bloku */
function kodBloku(el) {
  const pre = H.findFirst(el, (n) => n.name === 'pre');
  if (!pre) { xeber('code-card daxilinde <pre> yoxdur'); return null; }
  const codeEl = H.findFirst(pre, (n) => n.name === 'code') || pre;
  const kod = H.textOf(codeEl).replace(/^\n/, '').replace(/\s+$/, '');

  const basliqEl = H.findFirst(el, (n) => H.hasClass(n, 'code-title'));
  const basliq = basliqEl ? duzMetn(basliqEl) : '';
  const netice = el.attrs['data-output'];

  const dil = /\.java$/i.test(basliq) ? 'java'
    : /\.(xml|gradle)$/i.test(basliq) ? 'xml'
      : /\.(kts|kt)$/i.test(basliq) ? 'kotlin'
        : /^(Terminal|Konsol|Quraşdırma)/i.test(basliq) ? 'shell'
          : 'kotlin';

  const blok = { t: 'code', title: basliq, lang: dil, code: kod };
  if (netice !== undefined && netice !== null && netice !== '') blok.output = netice;
  return blok;
}

/** <div class="callout tip|warn|info|danger"> */
function calloutBloku(el) {
  const siniflər = H.classesOf(el);
  const kind = ['tip', 'warn', 'info', 'danger'].find((k) => siniflər.indexOf(k) !== -1) || 'info';
  const icoEl = H.findFirst(el, (n) => H.hasClass(n, 'callout-ico'));
  const basliqEl = H.findFirst(el, (n) => H.hasClass(n, 'callout-title'));
  const govdeEl = H.findFirst(el, (n) => H.hasClass(n, 'callout-body')) || el;

  const blocks = [];
  H.elementChildren(govdeEl).forEach((c) => {
    if (H.hasClass(c, 'callout-title')) return;
    bloklaraElave(c, blocks);
  });

  return {
    t: 'callout',
    kind: kind,
    ico: icoEl ? duzMetn(icoEl) : '',
    title: basliqEl ? duzMetn(basliqEl) : '',
    blocks: blocks
  };
}

/** <table> */
function cedvelBloku(tableEl) {
  const head = [];
  const rows = [];
  H.findAll(tableEl, (n) => n.name === 'tr').forEach((tr) => {
    const hucreler = H.elementChildren(tr).filter((c) => c.name === 'th' || c.name === 'td');
    const setir = hucreler.map((c) => spansOf(c));
    if (hucreler.length && hucreler[0].name === 'th') head.push(setir);
    else rows.push(setir);
  });
  return { t: 'table', head: head, rows: rows };
}

/** <div class="grid"> daxilindeki kartlar */
function kartlarBloku(el) {
  const cols = H.hasClass(el, 'grid-2') ? 2 : H.hasClass(el, 'grid-3') ? 3 : 2;
  const items = [];
  H.elementChildren(el).forEach((kart) => {
    if (!H.hasClass(kart, 'card')) return;
    const icoEl = H.findFirst(kart, (n) => H.hasClass(n, 'card-ico'));
    const basliqEl = H.findFirst(kart, (n) => n.name === 'h4' || n.name === 'h3');
    const blocks = [];
    H.elementChildren(kart).forEach((c) => {
      if (c === icoEl || c === basliqEl) return;
      bloklaraElave(c, blocks);
    });
    items.push({
      ico: icoEl ? duzMetn(icoEl) : '',
      title: basliqEl ? duzMetn(basliqEl) : '',
      blocks: blocks
    });
  });
  return items.length ? { t: 'cards', cols: cols, items: items } : null;
}

/** <div class="timeline"> */
function xronologiyaBloku(el) {
  const items = [];
  H.elementChildren(el).forEach((it) => {
    if (!H.hasClass(it, 'tl-item')) return;
    const il = H.findFirst(it, (n) => H.hasClass(n, 'tl-year'));
    const bas = H.findFirst(it, (n) => H.hasClass(n, 'tl-title'));
    const tes = H.findFirst(it, (n) => H.hasClass(n, 'tl-desc'));
    items.push({
      year: il ? duzMetn(il) : '',
      title: bas ? duzMetn(bas) : '',
      desc: tes ? spansOf(tes) : []
    });
  });
  return items.length ? { t: 'timeline', items: items } : null;
}

/** <div class="tabs"> */
function tablarBloku(el) {
  const bar = H.findFirst(el, (n) => H.hasClass(n, 'tab-bar'));
  if (!bar) return null;
  const adlar = {};
  H.elementChildren(bar).forEach((b) => {
    const key = b.attrs['data-tab'];
    if (key) adlar[key] = duzMetn(b);
  });

  const tabs = [];
  H.elementChildren(el).forEach((panel) => {
    if (!H.hasClass(panel, 'tab-panel')) return;
    const key = panel.attrs['data-tab'];
    const blocks = [];
    H.elementChildren(panel).forEach((c) => bloklaraElave(c, blocks));
    tabs.push({ label: adlar[key] || key || '', blocks: blocks });
  });
  return tabs.length ? { t: 'tabs', tabs: tabs } : null;
}

/** <div class="hero"> — yalniz giris bolmesinde */
function heroBloku(el) {
  const badge = H.findFirst(el, (n) => H.hasClass(n, 'hero-badge'));
  const h1 = H.findFirst(el, (n) => n.name === 'h1');
  const sub = H.findFirst(el, (n) => H.hasClass(n, 'hero-sub'));
  const stats = [];
  H.findAll(el, (n) => H.hasClass(n, 'stat')).forEach((s) => {
    const num = H.findFirst(s, (n) => H.hasClass(n, 'stat-num'));
    const lbl = H.findFirst(s, (n) => H.hasClass(n, 'stat-lbl'));
    stats.push({ num: num ? duzMetn(num) : '', lbl: lbl ? duzMetn(lbl) : '' });
  });
  return {
    t: 'hero',
    badge: badge ? duzMetn(badge) : '',
    title: h1 ? spansOf(h1) : [],
    sub: sub ? duzMetn(sub) : '',
    stats: stats
  };
}

/** Interaktiv demolarin tetbiqdeki qarsiliqlari */
const DEMO_ADLARI = {
  tiDemo: 'tip-cixarisi',
  whenDemo: 'when',
  scopeDemo: 'scope',
  nsDemo: 'null-safety',
  colDemo: 'kolleksiyalar',
  coroDemo: 'coroutines'
};

/* ------------------------------------------------------------
   Bir DOM elementini bloklara cevirib siyahiya elave edir
   ------------------------------------------------------------ */
function bloklaraElave(el, blocks) {
  if (el.type !== 'element') return;
  const name = el.name;
  const sinif = H.classesOf(el);

  // Naviqasiya tetbiqde nativdir
  if (H.hasClass(el, 'page-nav') || name === 'nav') return;
  // Yalniz gorunus ucun olan bezekler
  if (H.hasClass(el, 'hero-blobs')) return;

  if (BASLIQ[name]) {
    const metn = duzMetn(el);
    if (metn) blocks.push({ t: BASLIQ[name], text: metn });
    return;
  }

  if (name === 'p') {
    if (H.hasClass(el, 'section-kicker')) return;   // bolme metadatasi
    const spans = spansOf(el);
    if (!spans.length) return;
    const blok = { t: 'p', spans: spans };
    if (H.hasClass(el, 'lead')) blok.lead = true;
    blocks.push(blok);
    return;
  }

  if (name === 'ul' || name === 'ol') {
    const items = H.elementChildren(el)
      .filter((li) => li.name === 'li')
      .map((li) => spansOf(li));
    if (items.length) blocks.push({ t: 'list', ordered: name === 'ol', items: items });
    return;
  }

  if (name === 'table') { blocks.push(cedvelBloku(el)); return; }
  if (name === 'hr') { blocks.push({ t: 'hr' }); return; }
  if (name === 'pre') {
    const kod = H.textOf(el).replace(/^\n/, '').replace(/\s+$/, '');
    if (kod) blocks.push({ t: 'code', title: '', lang: 'kotlin', code: kod });
    return;
  }

  if (name === 'div' || name === 'section' || name === 'aside') {
    if (H.hasClass(el, 'code-card')) { const b = kodBloku(el); if (b) blocks.push(b); return; }
    if (H.hasClass(el, 'callout')) { blocks.push(calloutBloku(el)); return; }
    if (H.hasClass(el, 'table-wrap')) {
      const t = H.findFirst(el, (n) => n.name === 'table');
      if (t) blocks.push(cedvelBloku(t));
      return;
    }
    if (H.hasClass(el, 'timeline')) { const b = xronologiyaBloku(el); if (b) blocks.push(b); return; }
    if (H.hasClass(el, 'tabs')) { const b = tablarBloku(el); if (b) blocks.push(b); return; }
    if (H.hasClass(el, 'grid')) { const b = kartlarBloku(el); if (b) blocks.push(b); return; }
    if (H.hasClass(el, 'hero')) { blocks.push(heroBloku(el)); return; }
    if (H.hasClass(el, 'quiz-card')) { blocks.push({ t: 'quiz' }); return; }

    if (H.hasClass(el, 'demo')) {
      const id = el.attrs['id'];
      if (id && DEMO_ADLARI[id]) { blocks.push({ t: 'demo', demo: DEMO_ADLARI[id] }); return; }
      // kod meydani demosu
      blocks.push({ t: 'playground' });
      return;
    }

    // Sade saricilar (wrap, hero-inner ve s.) — icine gir
    H.elementChildren(el).forEach((c) => bloklaraElave(c, blocks));
    return;
  }

  // Qalanlar: metn varsa paraqraf kimi gotur
  const metn = duzMetn(el);
  if (metn && name !== 'button' && name !== 'input' && name !== 'select' && name !== 'label') {
    blocks.push({ t: 'p', spans: spansOf(el) });
  }
}

/* ============================================================
   3. index.html → bölmələr
   ============================================================ */

function bolmeleriQur() {
  const html = fs.readFileSync(path.join(SAYT, 'index.html'), 'utf8');
  const dom = H.parse(html);
  const sectionEls = H.findAll(dom, (n) => n.name === 'section' && H.hasClass(n, 'section'));

  const XUSUSI = {
    'kod-meydani': 'playground',
    'calismalar': 'exercises',
    'quiz': 'quiz'
  };

  const sections = sectionEls.map((el, idx) => {
    const id = el.attrs['id'];
    const kickerEl = H.findFirst(el, (n) => H.hasClass(n, 'section-kicker'));
    const h2El = H.findFirst(el, (n) => n.name === 'h2');

    const blocks = [];
    H.elementChildren(el).forEach((c) => bloklaraElave(c, blocks));

    // Bolme basligini bloklardan cixar (native basliqda gosterilir)
    const basliqMetni = h2El ? duzMetn(h2El) : '';
    const ilkH2 = blocks.findIndex((b) => b.t === 'h2' && b.text === basliqMetni);
    if (ilkH2 !== -1) blocks.splice(ilkH2, 1);

    return {
      id: id,
      index: idx,
      title: el.attrs['data-title'] || basliqMetni || id,
      heading: basliqMetni,
      group: el.attrs['data-group'] || 'Bölmələr',
      kicker: kickerEl ? duzMetn(kickerEl) : '',
      special: XUSUSI[id] || null,
      blocks: blocks
    };
  });

  return sections;
}

/* ============================================================
   3b. Sabit blok id-ləri (T3.1)
   Hər mətn daşıyan blok (iç bloklar daxil) məzmunundan qurulan
   sabit id alır: `<bolme>.<tip>.<sha1-8>`. Dəyişməyən blok həmişə
   eyni id-ni alır; mətn dəyişsə id də dəyişir — tərcümə köhnəlib
   deməkdir. Eyni bölmədə eyni mətnli bloklar `.2`, `.3` sonluğu alır.
   ============================================================ */

const IDSIZ = { hr: 1, quiz: 1, playground: 1, demo: 1 };

function blokMetni(b) {
  const topla = [];
  bloklardanMetn([b], topla);
  return topla.filter(Boolean).join(' ').replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim();
}

function blokId(sectionId, tip, metn) {
  const h = crypto.createHash('sha1').update(metn, 'utf8').digest('hex').slice(0, 8);
  return sectionId + '.' + tip + '.' + h;
}

function idleriVer(sectionId, blocks, gorulen) {
  return (blocks || []).map((b) => {
    if (!b || IDSIZ[b.t]) return b;
    const esas = blokId(sectionId, b.t, blokMetni(b));
    const n = (gorulen[esas] || 0) + 1;
    gorulen[esas] = n;
    const id = n === 1 ? esas : esas + '.' + n;
    // `id` `t`-dən dərhal sonra gəlsin deyə obyekt yenidən qurulur.
    const yeni = { t: b.t, id: id };
    Object.keys(b).forEach((k) => { if (k !== 't') yeni[k] = b[k]; });
    if (b.t === 'callout') yeni.blocks = idleriVer(sectionId, b.blocks, gorulen);
    if (b.t === 'cards') yeni.items = b.items.map((it) => Object.assign({}, it, { blocks: idleriVer(sectionId, it.blocks, gorulen) }));
    if (b.t === 'tabs') yeni.tabs = b.tabs.map((tb) => Object.assign({}, tb, { blocks: idleriVer(sectionId, tb.blocks, gorulen) }));
    return yeni;
  });
}

function blokIdleriniVer(sections) {
  return sections.map((s) => Object.assign({}, s, { blocks: idleriVer(s.id, s.blocks, {}) }));
}

/* ============================================================
   4. JS mənbələrindən data çıxarışı
   ============================================================ */

function jsSandbox(faylYolu, extra) {
  const kod = fs.readFileSync(faylYolu, 'utf8');
  const sandbox = Object.assign({
    window: {},
    document: {
      querySelector: () => null,
      querySelectorAll: () => [],
      addEventListener: () => { },
      documentElement: { setAttribute: () => { }, getAttribute: () => null }
    },
    localStorage: { getItem: () => null, setItem: () => { } },
    setTimeout: () => 0,
    clearTimeout: () => { },
    console: console
  }, extra || {});
  sandbox.globalThis = sandbox;
  vm.createContext(sandbox);
  try {
    vm.runInContext(kod, sandbox, { filename: path.basename(faylYolu) });
  } catch (e) {
    xeber(path.basename(faylYolu) + ' icra xetasi: ' + e.message);
  }
  return sandbox;
}

/** exercises.js → çalışmalar bankı */
function calismalariQur() {
  const sb = jsSandbox(path.join(SAYT, 'js', 'exercises.js'));
  const bank = sb.window.KExercises;
  if (!bank || !Array.isArray(bank.topics)) {
    console.error('XETA: window.KExercises tapilmadi');
    process.exit(1);
  }

  let nezeriSay = 0;
  let praktikiSay = 0;

  const topics = bank.topics.map((t) => {
    const nezeri = (t.nezeri || []).map((n, i) => {
      nezeriSay++;
      return {
        id: t.id + '-n-' + String(i + 1).padStart(2, '0'),
        no: i + 1,
        level: n.level,
        q: n.q,
        code: n.code || null,
        opts: n.opts,
        a: n.a,
        exp: n.exp
      };
    });
    const praktiki = (t.praktiki || []).map((p, i) => {
      praktikiSay++;
      return {
        id: t.id + '-p-' + String(i + 1).padStart(2, '0'),
        no: i + 1,
        level: p.level,
        tapsiriq: p.tapsiriq,
        starter: p.starter,
        gozlenilen: p.gozlenilen,
        ipucu: p.ipucu || null,
        hell: p.hell
      };
    });
    return { id: t.id, title: t.title, sectionId: t.sectionId || t.id, nezeri: nezeri, praktiki: praktiki };
  });

  return { topics: topics, stats: { nezeri: nezeriSay, praktiki: praktikiSay } };
}

/** demos.js → QUIZ massivi */
function quizQur() {
  const mənbə = fs.readFileSync(path.join(SAYT, 'js', 'demos.js'), 'utf8');
  const bas = mənbə.indexOf('var QUIZ = [');
  if (bas === -1) { xeber('demos.js: QUIZ massivi tapilmadi'); return []; }

  // Massivin sonunu mötərizə balansı ilə tap
  const acilis = mənbə.indexOf('[', bas);
  let derinlik = 0;
  let son = -1;
  let sətir = null;
  for (let i = acilis; i < mənbə.length; i++) {
    const c = mənbə[i];
    if (sətir) {
      if (c === '\\') { i++; continue; }
      if (c === sətir) sətir = null;
      continue;
    }
    if (c === '"' || c === "'" || c === '`') { sətir = c; continue; }
    if (c === '[') derinlik++;
    else if (c === ']') { derinlik--; if (derinlik === 0) { son = i; break; } }
  }
  if (son === -1) { xeber('demos.js: QUIZ massivinin sonu tapilmadi'); return []; }

  const ifade = mənbə.slice(acilis, son + 1);
  const sandbox = {};
  vm.createContext(sandbox);
  const quiz = vm.runInContext('(' + ifade + ')', sandbox, { filename: 'quiz.js' });

  return quiz.map((q, i) => ({
    id: 'quiz-' + String(i + 1).padStart(2, '0'),
    q: q.q,
    code: q.code || null,
    opts: q.opts,
    a: q.a,
    exp: q.exp
  }));
}

/** playground.js → nümunə kodlar */
function meydanNumuneleri() {
  const mənbə = fs.readFileSync(path.join(SAYT, 'js', 'playground.js'), 'utf8');
  const bas = mənbə.indexOf('var PRESETS = [');
  if (bas === -1) { xeber('playground.js: PRESETS tapilmadi'); return []; }
  const acilis = mənbə.indexOf('[', bas);
  let derinlik = 0;
  let son = -1;
  let sətir = null;
  for (let i = acilis; i < mənbə.length; i++) {
    const c = mənbə[i];
    if (sətir) {
      if (c === '\\') { i++; continue; }
      if (c === sətir) sətir = null;
      continue;
    }
    if (c === '"' || c === "'" || c === '`') { sətir = c; continue; }
    if (c === '[') derinlik++;
    else if (c === ']') { derinlik--; if (derinlik === 0) { son = i; break; } }
  }
  if (son === -1) { xeber('playground.js: PRESETS sonu tapilmadi'); return []; }

  const sandbox = {};
  vm.createContext(sandbox);
  const presets = vm.runInContext('(' + mənbə.slice(acilis, son + 1) + ')', sandbox, { filename: 'presets.js' });
  return presets.map((p) => ({
    id: p.id,
    label: p.label,
    code: Array.isArray(p.code) ? p.code.join('\n') : String(p.code)
  }));
}

/* ============================================================
   5. Axtarış indeksi
   ============================================================ */

// search.json artıq yazılmır (T3.5): tətbiq indeksi yüklənmiş məzmundan özü
// qurur (AxtarisIndeksi.kt) — qayda content-lib.js-dəki bloklardanMetn-dir.

/* ============================================================
   5a. Öyrənmə yolu (T5.5)
   tools/path.json-dakı asılılıqlar bölmələrə `requires` kimi yazılır.
   Saytın HTML-ində belə məlumat yoxdur — qraf əl ilə saxlanılır.
   Naməlum id və ya dövr qurulmanı dayandırır.
   ============================================================ */

function yolQur(sections) {
  const yol = JSON.parse(fs.readFileSync(path.join(__dirname, 'path.json'), 'utf8')).requires || {};
  const idler = new Set(sections.map((s) => s.id));
  Object.keys(yol).forEach((id) => {
    [id].concat(yol[id]).forEach((x) => {
      if (!idler.has(x)) throw new Error('path.json: namelum bolme id-si: ' + x);
    });
  });
  // Dövr yoxlaması: DFS, «yolda» olan düyünə qayıtmaq dövrdür.
  const hal = {};
  function gez(id, zencir) {
    if (hal[id] === 2) return;
    if (hal[id] === 1) throw new Error('path.json: dovr: ' + zencir.concat([id]).join(' -> '));
    hal[id] = 1;
    (yol[id] || []).forEach((x) => gez(x, zencir.concat([id])));
    hal[id] = 2;
  }
  Object.keys(yol).forEach((id) => gez(id, []));
  // Yalnız boş olmayan `requires` yazılır — asılılıqsız bölmədə sahə yoxdur.
  return sections.map((s) => (yol[s.id] && yol[s.id].length ? Object.assign({}, s, { requires: yol[s.id] }) : s));
}

/* ============================================================
   6. Yazı
   ============================================================ */

function yaz(ad, data) {
  const yol = path.join(HEDEF, ad);
  fs.writeFileSync(yol, JSON.stringify(data), 'utf8');
  const kb = (fs.statSync(yol).size / 1024).toFixed(1);
  console.log('  ' + ad.padEnd(24) + kb + ' KB');
}

console.log('KotlinAZ mezmun boru xetti');
console.log('Menbe : ' + SAYT);
console.log('Hedef : ' + HEDEF);
console.log('');

const sections = yolQur(blokIdleriniVer(bolmeleriQur()));
const calismalar = calismalariQur();
const quiz = quizQur();
const presets = meydanNumuneleri();

yaz('content.json', { version: '1.0.0', sections: sections });
yaz('exercises.json', { topics: calismalar.topics });
yaz('quiz.json', { questions: quiz });
yaz('playground.json', { presets: presets });
// Köhnə boru xəttindən qalmış indeks APK-ya düşməsin.
if (fs.existsSync(path.join(HEDEF, 'search.json'))) fs.rmSync(path.join(HEDEF, 'search.json'));

console.log('');
console.log('Bolme sayi       : ' + sections.length);
console.log('Movzu (calisma)  : ' + calismalar.topics.length);
console.log('Nezeri calisma   : ' + calismalar.stats.nezeri);
console.log('Praktiki calisma : ' + calismalar.stats.praktiki);
console.log('Quiz suali       : ' + quiz.length);
console.log('Meydan numunesi  : ' + presets.length);

const kodSayi = (function () {
  let n = 0;
  const say = (blocks) => (blocks || []).forEach((b) => {
    if (!b) return;
    if (b.t === 'code') n++;
    if (b.t === 'callout') say(b.blocks);
    if (b.t === 'cards') b.items.forEach((i) => say(i.blocks));
    if (b.t === 'tabs') b.tabs.forEach((t) => say(t.blocks));
  });
  sections.forEach((s) => say(s.blocks));
  return n;
})();
console.log('Kod numunesi     : ' + kodSayi);

if (xeberdarliqlar.length) {
  console.log('');
  console.log('Xeberdarliqlar (' + xeberdarliqlar.length + '):');
  xeberdarliqlar.slice(0, 25).forEach((w) => console.log('  ! ' + w));
}
