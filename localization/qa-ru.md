# QA — Русский (ru), T4.2

Tərcümə: AI (Claude), 2026-11-13 – 2026-11-14. Ardıcıl (paralel agentsiz),
**ingiliscə pivotdan** (T4.2 spesifikasiyası: `ru` ingiliscədən) — hər bölmə və
mövzu `en` overlay-in eyni yolundakı mətndən tərcümə olunub, sonra ayrıca
overlay-ə yazılıb və dərhal validatordan keçirilib.
Qaydalar: [`POLICY.md`](POLICY.md), [`glossary.json`](glossary.json).

**Bu sənədi ikinci oxucu tamamlayıb imzalamalıdır.** Avtomatik yoxlamalar keçib,
insan baxışı hələ olmayıb. Mümkünsə rus dilini ana dili kimi bilən oxucu.

## 1. Avtomatik yoxlamalar — 2026-11-14

| Yoxlama | Nəticə |
|---|---|
| `validate-locales.js ru` | Hər qrupda 100 %; xəta 0, xəbərdarlıq 0 |
| Lüğət və «tərcümə olunmamalıdır» xəbərdarlıqları | 0 |
| Variant sayı/sırası, cədvəl ölçüləri, span növləri | validator — xəta 0 |
| Kod: yalnız şərh dəyişib (skelet) | `build-locale` heç bir yarpağı rədd etməyib |
| `check-code.js ru` (T4.6) | 84/84 blok, 625/625 həll, 10/10 meydan |
| UI cəm formaları | 13 `<plurals>` — `one` / `few` / `many` / `other` |
| Lint, unit testlər, `assembleRelease` | yaşıl |
| Release | `values-ru`, `assets/content/ru/` main-dədir; `locales.json` = `["az","en","ru","tr"]` |
| APK büdcəsi | 3 346 KB / 8 192 KB |

## 2. İnsan yoxlaması (ikinci oxucu)

- [ ] Bütün 32 dərs başlığı, alt başlıqlar və callout başlıqları
- [ ] 100 təsadüfi çalışma (nəzəri + praktiki qarışıq) — `overlay-parca.js cixar ru exercises --hamisi` ilə en↔ru yan-yana
- [ ] Bütün 15 quiz sualı
- [ ] Cihazda gəzinti (dil: Русский): hər ekran, hər dialoq, hər boş vəziyyət
- [ ] **Cəm formaları** real saylarla: 1, 2, 5, 11, 21, 22, 25 — «%d урок / урока / уроков», «%d теоретическое / теоретических»
- [ ] Uzun sözlər: düymə və çiplərdə kəsilmə yoxdur («Упражнения», «Песочница», «Закладки · %d», «Начать тест»)
- [ ] Kiril axtarışı real məzmunla (T3.5): «корутина», «КОРУТИНА» → tapılır
- [ ] Aşağıdakı «Qərarlar» bölməsi təsdiqlənib

Yoxladı: ____________________  Tarix: ____________

## 3. Təsdiq tələb edən qərarlar

- **Müraciət «ты»** — dərslər, çalışmalar, quiz və kod şərhləri eyni formadadır
  (`en`/`tr`-dəki «you»/«sen» qərarı ilə uyğun). Tapşırıqlar «Выведи…»,
  «Напиши…», şərhlər `// допиши`, `// напиши функцию`. UI sətirləri
  neytral/infinitiv formadadır («Проверить», «Начать тест»).
- **Terminlər** lüğətdən: наследование, инкапсуляция, полиморфизм,
  внедрение зависимостей, функция-расширение, функция высшего порядка,
  функция области видимости, дженерик, делегат, вывод типов,
  неизменяемость, корутина, рекомпозиция, null-безопасность.
  «Meydan» → **Песочница**, «Çalışma» → **Упражнение**.
- **İngilis adları saxlanılıb** — rus proqramçıların gündəlik işlətdiyi kimi:
  `smart cast`, `backing field` (bəzən «поле (backing field)»), `trailing lambda`,
  `data class`, `sealed`, `inline`, `reified`, `state hoisting`, `flaky`, `DSL`.
- **Lüğətin `_formalar`-ına əlavə:** `delegate` → `"delegates."`
  (`Delegates.notNull()` kimi API adları rus mətnində termin sayılsın deyə).
- **Lüğət terminləri adlıq halda.** Validator termini hərfi axtarır, ona görə
  «внедрение зависимостей» bir neçə cümlədə adlıq halda saxlanılıb
  («…(принцип: внедрение зависимостей)», «Какие библиотеки обеспечивают
  внедрение зависимостей…»). Oxucu cümlələrin təbii səsləndiyini yoxlamalıdır.
- **`overflow` mötərizədə** — «переполнение (overflow)»: validator `flow`
  alt sətrini Flow termini kimi tutur; tr-də də eyni həll.
- **Literallar azərbaycanca qalır** (POLICY §3) — tapşırıqdakı sitat, gözlənilən
  çıxış, kod içindəki mətn (`"Salam, Kotlin!"`, `"Hesablanir"`) dəyişmir.
- **Pivot.** `en` bazadakı faktiki səhvləri artıq düzəldib ([`qa-en.md`](qa-en.md) §4);
  ru bu düzəlişləri en-dən miras alır. Bazanın özü düzəldiləndə
  `new-locale.js ru --tamamla`.

## 4. Açıq sual: axtarışda «ё» / «е»

`AxtarisMotoru.normallasdir` ru üçün yalnız NFKC + `lowercase(ROOT)` edir —
«ё» və «е» fərqli qalır. Rus istifadəçiləri çox vaxt «е» yazır, ona görə
«все» sorğusu «всё» olan mətni tapmır (və əksinə). Təklif: ru (və be/uk)
üçün `ё → е` birləşdirilməsi, `tr`-dəki `ı → i` qaydası kimi, test ilə.
Bu sənəd imzalanmazdan əvvəl qərar verilməlidir.

## 5. Bazada (az) tapılan yeni problemlər

[`qa-en.md`](qa-en.md) §4 və [`qa-tr.md`](qa-tr.md) §4-dəki siyahı ru üçün də
keçərlidir. Yeni problem tapılmayıb.
