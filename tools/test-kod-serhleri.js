/* ============================================================
   KotlinAZ — kod-serhleri.js testləri (T4.0)

   Tərcümədə kodun yalnız şərhləri (localizedIo-da həm də literal
   mətnləri) dəyişə bilər. Bu testlər həm icazəli dəyişikliklərin
   keçdiyini, həm də kodun özünə toxunan dəyişikliyin tutulduğunu
   yoxlayır — sonuncu vacibdir: onu buraxan validator sınıq kodu
   tərcüməyə salar.

   İşlətmək:  node tools/test-kod-serhleri.js
   ============================================================ */
'use strict';

const K = require('./kod-serhleri');

let ugursuz = 0;
let say = 0;
function yoxla(ad, sert) {
  say++;
  if (!sert) { ugursuz++; console.log('  ✗ ' + ad); }
}

const baza = [
  'fun main() {',
  '    // İstifadəçinin yaşını yoxlayırıq',
  '    val yas = 17   // tam ədəd',
  '    val url = "https://kotlinlang.org" // literal içindəki // şərh deyil',
  '    /* blok /* iç-içə */ şərh */',
  '    println("Yaş: $yas, ${yas + 1} olacaq")',
  '    println(\'"\')',
  '}'
].join('\n');

const serhTercume = [
  'fun main() {',
  "    // Checking the user's age",
  '    val yas = 17 // integer',
  '    val url = "https://kotlinlang.org" // a // inside a literal is not a comment',
  '    /* nested /* block */ comment */',
  '    println("Yaş: $yas, ${yas + 1} olacaq")',
  '    println(\'"\')',
  '}'
].join('\n');

// 1. Yalnız şərh dəyişib — keçir; hizalama boşluqları fərq sayılmır
yoxla('serh tercumesi kecir', K.kodFerqi(baza, serhTercume, false) === null);

// 2. Literal dəyişib, localizedIo yoxdur — tutulur
const literalTercume = serhTercume.replace('"Yaş: $yas, ${yas + 1} olacaq"', '"Age: $yas, will be ${yas + 1}"');
yoxla('literal deyisikliyi localizedIo-suz tutulur', K.kodFerqi(baza, literalTercume, false) !== null);

// 3. localizedIo — literalın mətni dəyişə bilər, şablonlar qalmalıdır
yoxla('localizedIo-da literal tercumesi kecir', K.kodFerqi(baza, literalTercume, true) === null);
const sablonItdi = serhTercume.replace('${yas + 1}', '${yas + 2}');
yoxla('sablon ifadesi deyisse localizedIo-da da tutulur', K.kodFerqi(baza, sablonItdi, true) !== null);

// 4. Kodun özü dəyişib — hər rejimdə tutulur
yoxla('identifikator deyisikliyi tutulur', K.kodFerqi(baza, serhTercume.replace('val yas', 'val age'), true) !== null);
yoxla('reqem deyisikliyi tutulur', K.kodFerqi(baza, serhTercume.replace('17', '18'), false) !== null);
yoxla('elave setir tutulur', K.kodFerqi(baza, serhTercume.replace('fun main() {', 'fun main() {\n    val x = 1'), false) !== null);
yoxla('serh koda cevrilende tutulur', K.kodFerqi(baza, serhTercume.replace("// Checking the user's age", "println(\"age\")"), false) !== null);
yoxla('serh silinende tutulur', K.kodFerqi(baza, serhTercume.replace(' // integer', ''), false) !== null);
yoxla('literalin icindeki // saxlanilir', K.kodFerqi(baza, serhTercume.replace('"https://kotlinlang.org"', '"https://example.org"'), false) !== null);

// 5. Çoxsətirli blok şərhinin sətir sayı dəyişə bilər
const blokBaza = 'fun f() {\n    /* bir sətir */\n    return\n}';
const blokTerc = 'fun f() {\n    /* first line\n       second line */\n    return\n}';
// Skelet şərhi bütöv əvəzləyir — şərhin içindəki yeni sətir kodun
// sətir quruluşunu dəyişmir.
yoxla('blok serhinin daxili setirleri serbestdir', K.kodFerqi(blokBaza, blokTerc, false) === null);
yoxla('blok serhi skeletde tek nisandir', K.skelet(blokTerc, false) === K.skelet(blokBaza, false));

// 6. Nə tərcümə olunur
yoxla('herfli serh tercume olunur', K.tercumeOlunur('val x = 1 // bir', false));
yoxla('reqem serhi tercume olunmur', !K.tercumeOlunur('println(PI) // 3.141592653589793', false));
yoxla('serhsiz kod tercume olunmur', !K.tercumeOlunur('fun main() {\n    println("Salam")\n}', false));
yoxla('localizedIo-da herfli literal tercume olunur', K.tercumeOlunur('fun main() {\n    println("Salam")\n}', true));
yoxla('yalniz sablonlu literal tercume olunmur', !K.tercumeOlunur('println("$a ${b}")', true));
yoxla('escape herf sayilmir', !K.tercumeOlunur('println("\\n\\t")', true));

// 7. Xam sətir və char literal
const xam = 'val s = """\n    Salam // şərh deyil\n""".trimIndent()';
yoxla('xam setirdeki // serh deyil', !K.tercumeOlunur(xam, false));
yoxla('char literal "\'"\' tek parca qalir', K.parcala('val c = \'"\' // q').filter((h) => h.nov === 'serh').length === 1);

// 8. İç-içə şablon: "${"a".length}" — içdəki literal şablonun hissəsidir
const icice = 'println("uzunluq: ${"abc".length}")';
yoxla('ic-ice sablon tek literaldir', K.parcala(icice).filter((h) => h.nov === 'setir').length === 1);
yoxla('ic-ice sablon localizedIo-da qorunur',
  K.kodFerqi(icice, 'println("length: ${"abc".length}")', true) === null &&
  K.kodFerqi(icice, 'println("length: ${"xyz".length}")', true) !== null);

console.log('kod-serhleri: ' + (say - ugursuz) + ' / ' + say + ' test kecdi');
process.exit(ugursuz ? 1 : 0);
