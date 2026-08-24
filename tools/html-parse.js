/* ============================================================
   Kicik, asililiqsiz HTML parseri.
   KotlinAZ saytinin el ilə yazilmis, duzgun qurulmus HTML-i
   ucun kifayet edir — brauzer seviyyesinde tolerantliq yoxdur.
   ============================================================ */
'use strict';

const VOID = new Set(['area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input',
  'link', 'meta', 'param', 'source', 'track', 'wbr']);

// Bu teqlerin daxilinde HTML yox, xam metn var
const RAW_TEXT = new Set(['script', 'style']);

const ENTITIES = {
  amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ',
  hellip: '…', mdash: '—', ndash: '–', laquo: '«',
  raquo: '»', times: '×', check: '✓', rarr: '→',
  larr: '←', copy: '©', deg: '°', middot: '·'
};

function decodeEntities(s) {
  return s.replace(/&(#x?[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);/g, (m, body) => {
    if (body[0] === '#') {
      const hex = body[1] === 'x' || body[1] === 'X';
      const num = parseInt(hex ? body.slice(2) : body.slice(1), hex ? 16 : 10);
      return Number.isFinite(num) ? String.fromCodePoint(num) : m;
    }
    const key = body.toLowerCase();
    return Object.prototype.hasOwnProperty.call(ENTITIES, key) ? ENTITIES[key] : m;
  });
}

function parseAttrs(raw) {
  const attrs = {};
  const re = /([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*(?:=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+)))?/g;
  let m;
  while ((m = re.exec(raw)) !== null) {
    const name = m[1].toLowerCase();
    const val = m[2] !== undefined ? m[2] : m[3] !== undefined ? m[3] : m[4] !== undefined ? m[4] : '';
    attrs[name] = decodeEntities(val);
  }
  return attrs;
}

/** HTML metnini sade DOM agacina cevirir. */
function parse(html) {
  const root = { type: 'root', children: [] };
  const stack = [root];
  let i = 0;

  const pushText = (text) => {
    if (!text) return;
    stack[stack.length - 1].children.push({ type: 'text', text });
  };

  let sonrakiYer = -1;
  while (i < html.length) {
    // Muhafize: indeks her zaman irəli getməlidir
    if (i <= sonrakiYer) { i = sonrakiYer + 1; continue; }
    sonrakiYer = i;

    const lt = html.indexOf('<', i);
    if (lt === -1) {
      pushText(decodeEntities(html.slice(i)));
      break;
    }
    if (lt > i) pushText(decodeEntities(html.slice(i, lt)));

    // Serh
    if (html.startsWith('<!--', lt)) {
      const end = html.indexOf('-->', lt + 4);
      i = end === -1 ? html.length : end + 3;
      continue;
    }
    // Doctype ve s.
    if (html[lt + 1] === '!') {
      const end = html.indexOf('>', lt);
      i = end === -1 ? html.length : end + 1;
      continue;
    }
    // Bagli teq
    if (html[lt + 1] === '/') {
      const end = html.indexOf('>', lt);
      const name = html.slice(lt + 2, end).trim().toLowerCase();
      for (let s = stack.length - 1; s > 0; s--) {
        if (stack[s].name === name) {
          stack.length = s;
          break;
        }
      }
      i = end === -1 ? html.length : end + 1;
      continue;
    }

    // Acilan teq
    const end = html.indexOf('>', lt);
    if (end === -1) { pushText(decodeEntities(html.slice(lt))); break; }

    let inner = html.slice(lt + 1, end);
    const selfClosing = inner.endsWith('/');
    if (selfClosing) inner = inner.slice(0, -1);

    const sp = inner.search(/[\s]/);
    const name = (sp === -1 ? inner : inner.slice(0, sp)).toLowerCase();
    const attrs = sp === -1 ? {} : parseAttrs(inner.slice(sp));

    const node = { type: 'element', name, attrs, children: [] };
    stack[stack.length - 1].children.push(node);
    i = end + 1;

    if (VOID.has(name) || selfClosing) continue;

    if (RAW_TEXT.has(name)) {
      // DIQQET: butun sənədi toLowerCase() etmək olmaz — azərbaycan 'İ' hərfi
      // kicik hərfə iki koda cevrilir, indekslər surusur ve dovr geriyə qayidir.
      const bagliq = new RegExp('</' + name + '\\s*>', 'i');
      const qaliq = html.slice(i);
      const m = bagliq.exec(qaliq);
      if (m) {
        if (m.index > 0) node.children.push({ type: 'text', text: qaliq.slice(0, m.index) });
        i = i + m.index + m[0].length;
      } else {
        if (qaliq) node.children.push({ type: 'text', text: qaliq });
        i = html.length;
      }
      continue;
    }

    stack.push(node);
  }

  return root;
}

/* ---------- Agac uzerinde komekciler ---------- */

function classesOf(node) {
  if (!node || node.type !== 'element') return [];
  const c = node.attrs['class'];
  return c ? c.trim().split(/\s+/) : [];
}

function hasClass(node, name) {
  return classesOf(node).indexOf(name) !== -1;
}

function elementChildren(node) {
  return (node.children || []).filter((c) => c.type === 'element');
}

function findAll(node, pred, out) {
  out = out || [];
  (node.children || []).forEach((c) => {
    if (c.type === 'element') {
      if (pred(c)) out.push(c);
      findAll(c, pred, out);
    }
  });
  return out;
}

function findFirst(node, pred) {
  const list = [];
  (function walk(n) {
    if (list.length) return;
    (n.children || []).forEach((c) => {
      if (list.length) return;
      if (c.type === 'element') {
        if (pred(c)) { list.push(c); return; }
        walk(c);
      }
    });
  })(node);
  return list[0] || null;
}

/** Duyunun butun metnini birlesdirir. */
function textOf(node) {
  if (!node) return '';
  if (node.type === 'text') return node.text;
  return (node.children || []).map(textOf).join('');
}

module.exports = { parse, decodeEntities, classesOf, hasClass, elementChildren, findAll, findFirst, textOf };
