/* ============================================================
   KotlinAZ — kod nümunəsində nə tərcümə oluna bilər (T4.0)

   Siyasət (localization/POLICY.md): kod blokunda, model həldə və
   başlanğıc kodda YALNIZ şərhlər tərcümə olunur. `localizedIo`
   çalışmalarında string literal-ların mətni də.

   Bu modul kodu leksik hissələrə bölür (kod / şərh / literal) və
   «skelet» qurur — tərcümə oluna bilən hissələr nişanla əvəzlənir.
   Tərcümə ilə bazanın skeleti eyni deyilsə, kodun özü dəyişib:
   validator xəta verir, build-locale bazanı saxlayır.

   Testlər: node tools/test-kod-serhleri.js
   ============================================================ */
'use strict';

/**
 * Kotlin kodunu hissələrə bölür: { nov: 'kod'|'serh'|'setir', metn, sablonlar? }.
 * `setir` — string literal (dırnaqlar daxil); `sablonlar` onun `$ad` və
 * `${…}` ifadələridir (onlar koddur, tərcümə olunmur).
 * Blok şərhləri Kotlin-dəki kimi iç-içə ola bilər.
 */
function parcala(kod) {
  const n = kod.length;
  const hisseler = [];
  let bas = 0;
  let i = 0;
  const kodBitir = (son) => { if (son > bas) hisseler.push({ nov: 'kod', metn: kod.slice(bas, son) }); };

  while (i < n) {
    const c2 = kod.substr(i, 2);
    if (c2 === '//') {
      kodBitir(i);
      const e = kod.indexOf('\n', i);
      const son = e === -1 ? n : e;
      hisseler.push({ nov: 'serh', metn: kod.slice(i, son) });
      i = bas = son;
    } else if (c2 === '/*') {
      kodBitir(i);
      let derinlik = 1;
      let j = i + 2;
      while (j < n && derinlik > 0) {
        if (kod.substr(j, 2) === '/*') { derinlik++; j += 2; } else if (kod.substr(j, 2) === '*/') { derinlik--; j += 2; } else j++;
      }
      hisseler.push({ nov: 'serh', metn: kod.slice(i, j) });
      i = bas = j;
    } else if (kod[i] === '"') {
      kodBitir(i);
      const son = setirSonu(kod, i);
      hisseler.push({ nov: 'setir', metn: kod.slice(i, son.i), sablonlar: son.sablonlar });
      i = bas = son.i;
    } else if (kod[i] === '\'') {
      // Char literal koddur: 'a', '\n', 'A'
      const e = kod.indexOf('\'', i + (kod[i + 1] === '\\' ? 2 : 1) + 1);
      i = e === -1 ? i + 1 : e + 1;
    } else {
      i++;
    }
  }
  kodBitir(n);
  return hisseler;
}

/** `"…"` və ya `"""…"""` literalının sonu və şablon ifadələri. */
function setirSonu(kod, i) {
  const n = kod.length;
  const xam = kod.substr(i, 3) === '"""';
  const sablonlar = [];
  i += xam ? 3 : 1;
  while (i < n) {
    if (xam ? kod.substr(i, 3) === '"""' : kod[i] === '"') {
      i += xam ? 3 : 1;
      // """…"""" — sondakı artıq dırnaqlar literalın mətnidir
      if (xam) while (kod[i] === '"') i++;
      break;
    }
    if (!xam && kod[i] === '\n') break;
    if (!xam && kod[i] === '\\') { i += 2; continue; }
    if (kod.substr(i, 2) === '${') {
      const son = sablonSonu(kod, i + 2);
      sablonlar.push(kod.slice(i, son));
      i = son;
      continue;
    }
    if (kod[i] === '$' && /[A-Za-z_]/.test(kod[i + 1] || '')) {
      const m = /^\$[A-Za-z_][A-Za-z0-9_]*/.exec(kod.slice(i));
      sablonlar.push(m[0]);
      i += m[0].length;
      continue;
    }
    i++;
  }
  return { i, sablonlar };
}

/** `${`-dən sonrakı ifadənin sonu (bağlayan `}` daxil); iç literal və blokları keçir. */
function sablonSonu(kod, i) {
  let derinlik = 0;
  while (i < kod.length) {
    const c = kod[i];
    if (c === '"') { i = setirSonu(kod, i).i; continue; }
    if (c === '{') derinlik++;
    if (c === '}') { if (derinlik === 0) return i + 1; derinlik--; }
    i++;
  }
  return i;
}

const HERF = /\p{L}/u;

/** Hissə tərcümə oluna bilən təbii dil daşıyırmı (rəqəm/simvol şərhi yox). */
function metnlidir(h, literallar) {
  if (h.nov === 'serh') return HERF.test(h.metn.replace(/^\/[/*]+|\*+\/$/g, ''));
  if (h.nov === 'setir' && literallar) {
    let icerik = h.metn;
    h.sablonlar.forEach((s) => { icerik = icerik.split(s).join(' '); });
    return HERF.test(icerik.replace(/\\./g, ' '));
  }
  return false;
}

/**
 * Kodda tərcümə olunası mətn varmı — yoxdursa kod overlay-ə düşmür,
 * tamamlanma faizinə də sayılmır.
 */
function tercumeOlunur(kod, literallar) {
  return typeof kod === 'string' && parcala(kod).some((h) => metnlidir(h, literallar));
}

/**
 * Skelet: şərhlər → `/*…*\/` nişanı, (literallar=true olanda) literalın
 * mətni → yalnız şablon ifadələri. Üfüqi boşluqlar sıxılır ki, tərcümədən
 * sonra şərhi yenidən düzləşdirmək (sütun hizası) xəta sayılmasın; sətir
 * quruluşu isə eyni qalmalıdır.
 */
function skelet(kod, literallar) {
  return parcala(kod).map((h) => {
    if (h.nov === 'serh') return h.metn.startsWith('//') ? '\u0001//' : '\u0001/**/';
    if (h.nov === 'setir' && literallar) return '"' + h.sablonlar.join('\u0002') + '"';
    return h.metn;
  }).join('')
    .split('\n')
    .map((s) => s.replace(/[ \t]+/g, ' ').replace(/ ?\u0001/g, '').trimEnd())
    .join('\n');
}

/**
 * Tərcümə olunmuş kod bazanın yerinə keçə bilərmi. Problem varsa
 * insan üçün izah (hansı sətir fərqlidir), yoxdursa null.
 */
function kodFerqi(baza, terc, literallar) {
  if (typeof terc !== 'string') return 'setir gozlenilirdi';
  const a = skelet(baza, literallar).split('\n');
  const b = skelet(terc, literallar).split('\n');
  if (a.length !== b.length) return 'setir sayi ' + b.length + ' ≠ ' + a.length + ' (serhden kenar setir elave/silinib)';
  for (let k = 0; k < a.length; k++) {
    if (a[k] !== b[k]) {
      return 'setir ' + (k + 1) + ': kod deyisib — yalniz ' + (literallar ? 'serh ve metn literallari' : 'serhler') +
        ' tercume oluna biler (' + JSON.stringify(b[k].trim().slice(0, 60)) + ')';
    }
  }
  return null;
}

module.exports = { parcala, skelet, tercumeOlunur, kodFerqi };
