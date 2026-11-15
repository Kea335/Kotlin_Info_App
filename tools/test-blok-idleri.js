/* ============================================================
   KotlinAZ — blok id-lərinin sabitlik testi (T3.1)

   1. content.json iki dəfə qurulur — id-lər eynidir.
   2. Saytın nüsxəsinə bir paraqraf əlavə olunur, yenidən qurulur —
      mövcud id-lər dəyişmir, yalnız yeni id əlavə olunur.
   3. Hər mətn daşıyan blokun id-si var və bölmə daxilində unikaldır.

   İşlətmək:  node tools/test-blok-idleri.js
   Sayt qovluğu lazımdır (KOTLINAZ_WEB); tapılmasa test ötürülür.
   ============================================================ */
'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const SAYT = process.env.KOTLINAZ_WEB || path.resolve('C:/Users/PC-01/Desktop/kotlinweb');
const QURUCU = path.join(__dirname, 'build-content.js');
const IDSIZ = { hr: 1, quiz: 1, playground: 1, demo: 1 };

if (!fs.existsSync(path.join(SAYT, 'index.html'))) {
  console.log('Sayt qovlugu yoxdur (' + SAYT + ') — blok id testi oturuldu.');
  process.exit(0);
}

const kok = fs.mkdtempSync(path.join(os.tmpdir(), 'kotlinaz-blok-id-'));
const saytNusxesi = path.join(kok, 'sayt');
fs.mkdirSync(saytNusxesi);
fs.copyFileSync(path.join(SAYT, 'index.html'), path.join(saytNusxesi, 'index.html'));
fs.cpSync(path.join(SAYT, 'js'), path.join(saytNusxesi, 'js'), { recursive: true });

function qur(ad) {
  const hedef = path.join(kok, ad);
  execFileSync(process.execPath, [QURUCU], {
    env: Object.assign({}, process.env, { KOTLINAZ_WEB: saytNusxesi, KOTLINAZ_HEDEF: hedef }),
    stdio: 'ignore'
  });
  return JSON.parse(fs.readFileSync(path.join(hedef, 'content.json'), 'utf8'));
}

/* Bölmə → id siyahısı (iç bloklar daxil). */
function idler(content) {
  const out = {};
  const walk = (blocks, topla, bolme) => (blocks || []).forEach((b) => {
    if (!b) return;
    if (IDSIZ[b.t]) {
      if (b.id) problemler.push(bolme + ': id-siz olmali blokda id var: ' + b.t);
      return;
    }
    if (!b.id) problemler.push(bolme + ': ' + b.t + ' blokunun id-si yoxdur');
    else if (!new RegExp('^' + bolme.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\.' + b.t + '\\.[0-9a-f]{8}(\\.\\d+)?$').test(b.id)) {
      problemler.push(bolme + ': id formati sehvdir: ' + b.id);
    }
    topla.push(b.id);
    if (b.t === 'callout') walk(b.blocks, topla, bolme);
    if (b.t === 'cards') b.items.forEach((i) => walk(i.blocks, topla, bolme));
    if (b.t === 'tabs') b.tabs.forEach((t) => walk(t.blocks, topla, bolme));
  });
  content.sections.forEach((s) => { out[s.id] = []; walk(s.blocks, out[s.id], s.id); });
  return out;
}

const problemler = [];
let kecdi = 0;
function yoxla(ad, sert, izah) {
  if (sert) { kecdi++; console.log('  ok  ' + ad); }
  else { problemler.push(ad + (izah ? ' — ' + izah : '')); console.log('  XETA ' + ad + (izah ? ' — ' + izah : '')); }
}

console.log('Blok id sabitlik testi');
console.log('Sayt : ' + SAYT);
console.log('');

/* ---------- 1. İki qurulma eynidir ---------- */
const a = qur('a');
const b = qur('b');
yoxla('iki qurulmada content.json eynidir', JSON.stringify(a) === JSON.stringify(b));

/* ---------- 3. Id var və unikaldır ---------- */
const idlerA = idler(a);
const evvelki = problemler.length;
let cemi = 0;
Object.keys(idlerA).forEach((bolme) => {
  const siyahi = idlerA[bolme];
  cemi += siyahi.length;
  if (new Set(siyahi).size !== siyahi.length) problemler.push(bolme + ': tekrar id var');
});
yoxla('her metn dasiyan blokun id-si var, formati duzdur', problemler.length === evvelki);
yoxla('bolme daxilinde id-ler unikaldir (' + cemi + ' id)', problemler.length === evvelki);

/* ---------- 2. Paraqraf əlavə edəndə köhnə id-lər qalır ---------- */
const htmlYolu = path.join(saytNusxesi, 'index.html');
let html = fs.readFileSync(htmlYolu, 'utf8');
// İkinci bölmənin (birincisi xüsusi «hero»dur) lead paraqrafından ƏVVƏL
// yeni paraqraf — ondan sonrakı hər blokun mövqeyi sürüşür; id-lər
// mövqeyə bağlı olsaydı hamısı dəyişərdi.
const birinci = html.indexOf('<section class="section"');
const ikinci = html.indexOf('<section class="section"', birinci + 1);
const ilkP = html.indexOf('<p class="lead"', ikinci);
if (ilkP === -1) { console.log('XETA: index.html-de lead paraqraf tapilmadi'); process.exit(1); }
html = html.slice(0, ilkP) + '<p>T3.1 test paraqrafi — sabitlik yoxlamasi.</p>' + html.slice(ilkP);
fs.writeFileSync(htmlYolu, html, 'utf8');

const c = qur('c');
const idlerC = idler(c);
const itenler = [];
let yeniler = 0;
Object.keys(idlerA).forEach((bolme) => {
  const sonra = new Set(idlerC[bolme] || []);
  idlerA[bolme].forEach((id) => { if (!sonra.has(id)) itenler.push(id); });
  yeniler += sonra.size - new Set(idlerA[bolme]).size;
});
yoxla('paraqraf elave edende movcud id-ler deyismir', itenler.length === 0,
  itenler.length ? 'deyisen: ' + itenler.slice(0, 5).join(', ') + (itenler.length > 5 ? ' …' : '') : '');
yoxla('yalniz bir yeni id elave olunub', yeniler === 1, 'yeni id sayi: ' + yeniler);

fs.rmSync(kok, { recursive: true, force: true });

console.log('');
console.log('Kecdi: ' + kecdi + ', problem: ' + problemler.length);
if (problemler.length) {
  problemler.slice(0, 20).forEach((p) => console.log('  ! ' + p));
  process.exit(1);
}
