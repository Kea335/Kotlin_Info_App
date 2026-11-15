/* KotlinAZ — dildən asılı olmayan yarpaq qaydasının testləri (T4.1).
   İşlətmək: node tools/test-neytral.js  (CI-da) */
'use strict';

const L = require('./content-lib');

// [dəyər, neytraldırmı]
const HALLAR = [
  // yalnız kod, rəqəm, açar söz — hər dildə eyni qalır
  [['null', '-1', '0', 'NullPointerException'], true],
  [['[1, 9, 25]', '[4, 16]'], true],
  ['true / false', true],
  ['data class', true],
  ['start()', true],
  ['( )', true],
  ['Int?', true],
  ['Sinif.kt', true],
  ['s.sortedBy { it.length }', true],
  [[{ k: 'code', v: 'a + b' }], true],
  // adi söz varsa — tərcümə olunur
  ['Kotlin nədir?', false],
  ['2 (tam bölmə!)', false],
  ['(boş)', false],
  ['Xeyr', false],
  ['Bəli, həmişə', false],
  ['println(5 + 3) yaz.', false],
  [['print() və println()', 'write()'], false],
  [[{ k: 'code', v: 'ad?.length' }, { k: 't', v: ' — null olsa null qaytarır' }], false],
  // boş dəyər neytral deyil — onu bosdur ayrıca tutur
  ['', false]
];

let ugursuz = 0;
HALLAR.forEach(([v, gozlenilen]) => {
  const n = L.dilNeytraldir(v);
  if (n !== gozlenilen) {
    ugursuz++;
    console.log('SEHV: ' + JSON.stringify(v) + ' → ' + n + ', gozlenilirdi ' + gozlenilen);
  }
});
console.log('neytral: ' + (HALLAR.length - ugursuz) + ' / ' + HALLAR.length + ' test kecdi');
process.exit(ugursuz ? 1 : 0);
