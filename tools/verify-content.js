/* ============================================================
   KotlinAZ — məzmun doğrulaması
   Saytdakı görünən mətnin tətbiqin JSON aktivlərinə tam
   köçdüyünü yoxlayır. İtən parçaları siyahılayır.

   İşlətmək:  node tools/verify-content.js
              node tools/verify-content.js --assets-only   (sayt olmadan, CI)
   ============================================================ */
'use strict';

const fs = require('fs');
const path = require('path');
const H = require('./html-parse');

const SAYT = process.env.KOTLINAZ_WEB || path.resolve('C:/Users/PC-01/Desktop/kotlinweb');
const AKTIV = path.resolve(__dirname, '..', 'app', 'src', 'main', 'assets');

/* Saytda tetbiqde qesden gosterilmeyen elementler (UI idareedicileri) */
const GOZ_ARDI = [
  'page-nav', 'code-head', 'code-actions', 'code-dots', 'code-out', 'code-out-label',
  'hero-blobs', 'sidebar', 'header', 'footer', 'to-top', 'toast', 'search',
  'tab-bar', 'quiz-card', 'demo', 'chip', 'field'
];

function normalize(s) {
  return String(s)
    .replace(/\u00a0/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

/* ---------- 1. Saytdan gozlenilen metn parcalari ---------- */

function saytParcalari() {
  const html = fs.readFileSync(path.join(SAYT, 'index.html'), 'utf8');
  const dom = H.parse(html);
  const sections = H.findAll(dom, (n) => n.name === 'section' && H.hasClass(n, 'section'));
  const nəticə = {};

  sections.forEach((sec) => {
    const parcalar = [];

    function keçilmə(node) {
      const sinif = H.classesOf(node);
      return GOZ_ARDI.some((g) => sinif.indexOf(g) !== -1);
    }

    function walk(node) {
      (node.children || []).forEach((c) => {
        if (c.type !== 'element') return;
        if (keçilmə(c)) return;
        if (c.name === 'button' || c.name === 'select' || c.name === 'input' || c.name === 'label') return;

        // Yarpaq mətn daşıyıcıları
        if (['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'li', 'td', 'th'].indexOf(c.name) !== -1) {
          const t = normalize(H.textOf(c));
          if (t) parcalar.push(t);
          return;
        }
        if (c.name === 'pre') {
          const t = normalize(H.textOf(c));
          if (t) parcalar.push(t);
          return;
        }
        if (['tl-year', 'tl-title', 'stat-num', 'stat-lbl', 'hero-badge', 'card-ico', 'callout-title', 'callout-ico']
          .some((k) => H.hasClass(c, k))) {
          const t = normalize(H.textOf(c));
          if (t) parcalar.push(t);
          return;
        }
        walk(c);
      });
    }

    walk(sec);
    nəticə[sec.attrs['id']] = parcalar;
  });

  return nəticə;
}

/* ---------- 2. Tetbiqin JSON-undan toplanan metn ---------- */

function spanMetni(spans) {
  return (spans || []).map((s) => (s.k === 'br' ? ' ' : s.v)).join('');
}

function bloklardanMetn(blocks, topla) {
  (blocks || []).forEach((b) => {
    if (!b) return;
    switch (b.t) {
      case 'h2': case 'h3': case 'h4': case 'h5': topla.push(b.text); break;
      case 'p': topla.push(spanMetni(b.spans)); break;
      case 'code': topla.push(b.title); topla.push(b.code); if (b.output) topla.push(b.output); break;
      case 'callout': topla.push(b.ico); topla.push(b.title); bloklardanMetn(b.blocks, topla); break;
      case 'cards': b.items.forEach((it) => { topla.push(it.ico); topla.push(it.title); bloklardanMetn(it.blocks, topla); }); break;
      case 'table':
        (b.head || []).forEach((r) => r.forEach((c) => topla.push(spanMetni(c))));
        (b.rows || []).forEach((r) => r.forEach((c) => topla.push(spanMetni(c))));
        break;
      case 'list': b.items.forEach((it) => topla.push(spanMetni(it))); break;
      case 'timeline': b.items.forEach((it) => { topla.push(it.year); topla.push(it.title); topla.push(spanMetni(it.desc)); }); break;
      case 'tabs': b.tabs.forEach((tb) => { topla.push(tb.label); bloklardanMetn(tb.blocks, topla); }); break;
      case 'hero':
        topla.push(b.badge); topla.push(spanMetni(b.title)); topla.push(b.sub);
        (b.stats || []).forEach((s) => { topla.push(s.num); topla.push(s.lbl); });
        break;
      default: break;
    }
  });
}

/* ---------- 3. Muqayise ---------- */

// --assets-only: sayt repo-su olmayan mühitdə (CI) yalnız aktivlərin öz
// bütövlüyü yoxlanılır — saytla müqayisə (1–3-cü bölmələr) atlanılır.
const YALNIZ_AKTIV = process.argv.includes('--assets-only');

const content = JSON.parse(fs.readFileSync(path.join(AKTIV, 'content.json'), 'utf8'));
const gozlenilen = YALNIZ_AKTIV ? {} : saytParcalari();

let umumi = 0;
let itən = 0;
const problemler = [];

content.sections.forEach((s) => {
  if (YALNIZ_AKTIV) return;
  // Bolme basligi ve nomresi bloklarda yox, metadatadadir
  const topla = [s.title, s.heading, s.kicker, s.group];
  bloklardanMetn(s.blocks, topla);
  const hovuz = normalize(topla.join(' \u0001 '));

  const parcalar = gozlenilen[s.id] || [];
  parcalar.forEach((p) => {
    umumi++;
    if (hovuz.indexOf(p) === -1) {
      itən++;
      if (problemler.length < 40) {
        problemler.push(s.id + ' :: ' + (p.length > 110 ? p.slice(0, 110) + '…' : p));
      }
    }
  });
});

console.log('KotlinAZ mezmun dogrulamasi');
console.log('');
console.log('Bolme sayi        : ' + content.sections.length);
if (YALNIZ_AKTIV) {
  console.log('Yalniz aktivler rejimi — saytla muqayise atlanildi.');
} else {
  console.log('Yoxlanan parca    : ' + umumi);
  console.log('Itən parca        : ' + itən);
  console.log('Ortu              : ' + (umumi ? ((umumi - itən) / umumi * 100).toFixed(2) : '0') + '%');
}

/* ---------- 4. Calismalar ve quiz butovlugu ---------- */

const ex = JSON.parse(fs.readFileSync(path.join(AKTIV, 'exercises.json'), 'utf8'));
let n = 0, p = 0;
const exProblem = [];
ex.topics.forEach((t) => {
  t.nezeri.forEach((q) => {
    n++;
    if (!q.q || !Array.isArray(q.opts) || q.opts.length !== 4) exProblem.push(q.id + ': variant sayi yanlis');
    if (typeof q.a !== 'number' || q.a < 0 || q.a > 3) exProblem.push(q.id + ': cavab indeksi yanlis');
    if (!q.exp) exProblem.push(q.id + ': izah yoxdur');
  });
  t.praktiki.forEach((x) => {
    p++;
    if (!x.tapsiriq) exProblem.push(x.id + ': tapsiriq yoxdur');
    if (!x.starter) exProblem.push(x.id + ': baslangic kodu yoxdur');
    if (!x.hell) exProblem.push(x.id + ': model hell yoxdur');
    if (typeof x.gozlenilen !== 'string' || !x.gozlenilen.length) exProblem.push(x.id + ': gozlenilen netice yoxdur');
    // T0.4: başlanğıc kodu həllə bərabərdirsə istifadəçi heç nə yazmadan «Yoxla» keçir.
    if (x.starter && x.hell && x.starter.trim() === x.hell.trim()) exProblem.push(x.id + ': baslangic kodu hell ile eynidir');
  });
});

const quiz = JSON.parse(fs.readFileSync(path.join(AKTIV, 'quiz.json'), 'utf8'));
quiz.questions.forEach((q) => {
  if (!q.q || !Array.isArray(q.opts) || q.opts.length !== 4) exProblem.push(q.id + ': quiz variant sayi yanlis');
  if (typeof q.a !== 'number' || q.a < 0 || q.a > 3) exProblem.push(q.id + ': quiz cavab indeksi yanlis');
});

console.log('');
console.log('Nezeri calisma    : ' + n);
console.log('Praktiki calisma  : ' + p);
console.log('Quiz suali        : ' + quiz.questions.length);
console.log('Struktur problemi : ' + exProblem.length);
exProblem.slice(0, 15).forEach((e) => console.log('  ! ' + e));

if (problemler.length) {
  console.log('');
  console.log('Itən parcalar (ilk ' + problemler.length + '):');
  problemler.forEach((x) => console.log('  - ' + x));
}

process.exit(itən > 0 || exProblem.length > 0 ? 2 : 0);
