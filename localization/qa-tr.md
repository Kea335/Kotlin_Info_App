# QA — Türkçe (tr), T4.2

Tərcümə: AI (Claude), 2026-11-13. Bir sessiyada, ardıcıl (paralel agentsiz),
**azərbaycancadan birbaşa** — ingiliscə pivotdan keçmədən (T4.2 spesifikasiyası).
Qaydalar: [`POLICY.md`](POLICY.md), [`glossary.json`](glossary.json). Hər bölmə və
mövzu ayrıca overlay-ə yazılıb və dərhal validatordan keçirilib.

**Bu sənədi ikinci oxucu tamamlayıb imzalamalıdır.** Avtomatik yoxlamalar keçib,
insan baxışı hələ olmayıb. Mümkünsə türkcəni ana dili kimi bilən oxucu.

## 1. Avtomatik yoxlamalar — 2026-11-13

| Yoxlama | Nəticə |
|---|---|
| `validate-locales.js tr` | Hər qrupda 100 %; xəta 0, xəbərdarlıq 0 |
| Lüğət və «tərcümə olunmamalıdır» xəbərdarlıqları | 0 |
| Variant sayı/sırası, cədvəl ölçüləri, span növləri | validator — xəta 0 |
| Kod: yalnız şərh dəyişib (skelet) | `build-locale` 4 118 yarpağın heç birini rədd etməyib |
| `check-code.js tr` (T4.6) | 84/84 blok, 625/625 həll, 10/10 meydan |
| Lint, unit testlər, `assembleRelease` | yaşıl |
| Release | `values-tr`, `assets/content/tr/` main-dədir; `locales.json` = `["az","en","tr"]` |
| APK büdcəsi | 3 112 KB / 8 192 KB |

## 2. İnsan yoxlaması (ikinci oxucu)

- [ ] Bütün 32 dərs başlığı, alt başlıqlar və callout başlıqları
- [ ] 100 təsadüfi çalışma (nəzəri + praktiki qarışıq) — `overlay-parca.js cixar tr exercises --hamisi` ilə az↔tr yan-yana
- [ ] Bütün 15 quiz sualı
- [ ] Cihazda gəzinti (dil: Türkçe): hər ekran, hər dialoq, hər boş vəziyyət
- [ ] Uzun sözlər: düymə və çiplərdə kəsilmə yoxdur («Alıştırmalar», «Kontrol et», «Oyun Alanı»)
- [ ] **İ/ı axtarışı** real məzmunla (T3.5): «İstisna», «ısı», «KOTLIN» → tapılır
- [ ] Aşağıdakı «Qərarlar» bölməsi təsdiqlənib

Yoxladı: ____________________  Tarix: ____________

## 3. Təsdiq tələb edən qərarlar

- **Terminlər.** «Meydan» → **Oyun Alanı** (kod meydanı → «Kod oyun alanı»),
  «Çalışma» → **Alıştırma**, «Bilik testi» → **Bilgi testi**, «mənimsənilib» →
  «öğrenildi». Proqramçı terminləri lüğətdən: kalıtım, uzantı fonksiyonu,
  jenerik, delege, tür çıkarımı, çok biçimlilik, kapsülleme.
- **İngilis adları saxlanılıb** — Türk proqramçıların gündəlik işlətdiyi kimi:
  `coroutine`, `lambda`, `scope fonksiyonu`, `smart cast`, `safe call`, `receiver`,
  `override`, `backing field`, `thread`, `DSL`.
- **Lüğətə əlavə olunan `_formalar`** (tr pivot dildir — insan təsdiqi lazımdır):
  `scope-function` → «scope fonksiyon» (cəm: «scope fonksiyonları»),
  `polymorphism` → «çok biçimlili» (türkcə k→ğ yumşalması: «çok biçimliliği»).
- **Literallar azərbaycanca qalır** (POLICY §3) — tapşırıqdakı sitat, gözlənilən
  çıxış, kod içindəki mətn (`"Salam, Kotlin!"`, `"Xos geldin"`) dəyişmir.
- **Müraciət «sen»** — bazadakı rəsmi «siz» yerləri də daxil (en ilə eyni qərar).
- **Tətbiq, sayt yox.** Bazadakı sayt ifadələri («bu sayt», «sol panel») tərcümədə
  «uygulama», «menü» olub (qa-en §4 — bazada düzəldilməlidir).
- **Az ilə eyni yazılan türk sözləri.** «yaz», «sonra», «ve», «Etiket» bəzən
  azərbaycan mətni ilə hərfən eyni çıxır və validator «tərcümə olunmayıb» deyir.
  Belə ~25 yerdə məna dəyişmədən başqa söz seçilib («yaz» → «ekle», «sonra» →
  «ardından», `// lambda yaz` → `// lambda'yı buraya yaz`).
- **Bazadakı faktiki səhvlər tərcümədə düzəldilib:**
  `idiomlar-n-19 q` («İki nöqtəli assert operatoru») → «!! (not-null assertion)
  operatörü»; `xususi-sinifler-n-22 opts[0]` «Mümkün deyir» → «Mümkün değil».
  Digərləri ([`qa-en.md`](qa-en.md) §4) bazadakı kimi saxlanılıb — onlar bazada
  düzəldilməlidir, sonra `new-locale.js tr --tamamla`.

## 4. Bazada (az) tapılan yeni problemlər

[`qa-en.md`](qa-en.md) §4-dəki siyahı tr üçün də keçərlidir. Əlavə olaraq:

| Yer | Problem |
|---|---|
| `android-p-22` (starter) | şərhdə qarışıq yazı: «dəyişdiriciyi» — qalan şərhlər ASCII-dir |
