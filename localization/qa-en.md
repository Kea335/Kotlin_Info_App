# QA — English (en), T4.1

Tərcümə: AI (Claude), 2026-11-12. UI və quiz/meydan bir sessiyada, dərslər və
çalışmalar 9 paralel agentlə (3 dərs, 6 çalışma qrupu), hamısı
[`POLICY.md`](POLICY.md), [`PROMPT.md`](PROMPT.md) və [`glossary.json`](glossary.json)
ilə. Hər parça agentin öz worktree-sində validatordan keçib, sonra birləşdirilib
və bütövlükdə yenidən yoxlanılıb.

**Bu sənəd ikinci oxucu tərəfindən tamamlanıb imzalanmalıdır** (T4.1 → QA).
Avtomatik yoxlamalar keçib; insan baxışı hələ olmayıb.

## 1. Avtomatik yoxlamalar — 2026-11-12

| Yoxlama | Nəticə |
|---|---|
| `validate-locales.js en` | 100 % hər qrupda; xəta 0, xəbərdarlıq 0 |
| Lüğət və «tərcümə olunmamalıdır» xəbərdarlıqları | 0 |
| Variant sayı/sırası, cədvəl ölçüləri, span növləri | validator — xəta 0 |
| Kod: yalnız şərh dəyişib (skelet) | `build-locale` 4 118 yarpağın heç birini rədd etməyib |
| `check-code.js en` (T4.6) | 84/84 blok, 625/625 həll, 10/10 meydan |
| Lint, unit testlər, `assembleRelease` | yaşıl |
| Release APK | `(en)` resursları və `assets/content/en/` var; `locales.json` = `["az","en"]` |
| APK büdcəsi | 2 915 KB / 8 192 KB |

## 2. İnsan yoxlaması (ikinci oxucu)

- [ ] Bütün 32 dərs başlığı, alt başlıqlar və callout başlıqları
- [ ] 100 təsadüfi çalışma (nəzəri + praktiki qarışıq) — `overlay-parca.js cixar en exercises --hamisi` ilə az↔en yan-yana
- [ ] Bütün 15 quiz sualı
- [ ] Cihazda gəzinti (dil: English): hər ekran, hər dialoq, hər boş vəziyyət
- [ ] Aşağıdakı «Qərarlar» bölməsi təsdiqlənib

Yoxladı: ____________________  Tarix: ____________

## 3. Təsdiq tələb edən qərarlar

- **«Azərbaycan dilində» → «in English».** Giriş bölməsinin hero nişanı və
  alt başlığı tətbiqin dilindən danışır; en versiyada «in Azerbaijani» yanlış olardı.
- **Literallar azərbaycanca qalır.** Tapşırıqdakı sitat (`"Salam, Kotlin!"`),
  gözlənilən çıxış və kod içindəki mətn dəyişmir (POLICY §3) — ingilis oxucu
  azərbaycanca çıxış sətirləri görəcək. Bir yerdə mənası mötərizədə verilib
  (`test-n-25`).
- **Rəsmi «siz» → qeyri-rəsmi «you».** Bazada `tarixce` və `qurasdirma`-da rəsmi
  forma qalıb; en-də üslub vahiddir.
- **Neytral dəyərlər** (`neytral.json`, 408 yarpaq) — en-də bazadakı kimi qalır.
  Agentlərin xəbərdarlığı susdurmaq üçün dəyişdirdiyi 17 dəyər bazadakı formaya
  qaytarılıb (məs. «1 · Safe call — ?.», «try/catch/finally», cədvəl başlığı «Operator»).

## 4. Bazada (az) tapılan problemlər

Tərcüməçi-agentlər «auditor» rolunda tapıb. en tərcümədə çox vaxt düzgün variant
yazılıb; **az mətni düzəldilməyib** — sayt mənbəyində düzəldilməlidir (T0.4 yolu),
sonra `build-content.js` və `new-locale.js en --tamamla`.

### Faktiki / texniki səhv
| Yer | Problem |
|---|---|
| `sintaksis.p.740f380b`, `sintaksis-n-11 exp` | `?:` «ternary operator» adlanır — Kotlin-də o Elvis operatorudur |
| `idiomlar-n-19 q` | «İki nöqtəli assert operatoru (!!)» — iki nida işarəsidir |
| `null-safety-n-05 exp` | `!!` `KotlinNullPointerException` atır deyilir — 1.4-dən `java.lang.NullPointerException` |
| `genislenmeler-n-11 exp` | «Xəbərdarlıq da verilmir» — üzv funksiya genişlənməni kölgələyəndə IDE/kompilyator xəbərdarlıq verir |
| `genislenmeler-n-23` | `String.uppercase()` da genişlənmə funksiyasıdır — ikinci düzgün cavab |
| `generics-n-10 exp` | `where` «ikidən çox» məhdudiyyət üçün deyilir — birdən çox üçündür |
| `dovrler-n-03 exp`, `dovrler.code.0ead546a` | «`..<` (əvvəllər `until`)» — `until` köhnəlməyib, hələ var |
| `sertler-n-18` | Variant «xəbərdarlıq» deyir, izah «kompilyasiya xətası» |
| `kolleksiyalar.table.b29c3f53` | Set «sırasız» — `setOf` daxil etmə sırasını saxlayır |
| `kolleksiyalar.p.b139ebbb` | read-only kolleksiya «dəyişməz» adlanır — eyni bölmədəki «Read-only ≠ immutable» callout-u ilə ziddiyyət |
| `null-safety.p.de6a159f` | smart cast «başqa modulda təyin olunmuş open val» — hər `open` xassə və başqa moduldan hər xassə |
| `lambda.code.75d87bb8` | Şərh kodla uyğun gəlmir (tip yazılmayıb, parametr tipləri isə yazılıb) |
| `java-muqayise.p.7d88a996`, `java-muqayise-n-17 q` | record və sealed «Java 21+» ilə gəlib deyilir — Java 16 və 17 |
| `resurslar.p.89954c12` | «Kotlin in Action»-ı dilin yaradıcıları yazıb deyilir — müəlliflər komanda üzvləridir |
| `resurslar.p.2d7361bd` | Fleet tövsiyə olunur — JetBrains Fleet-i dayandırıb; IDEA Community ayrıca nəşr deyil |
| `idiomlar.table.3f6ab771` | «≤ 120 — ktlint defolt limiti» — ktlint_official 140, android_studio 100 |
| `backend.code.64de14f9` | Exposed `.select { }` — 0.46+ köhnəlib (`selectAll().where { }`) |
| `backend.code.796c71e7` | suspend kontroller üçün WebFlux lazımdır deyilir — Spring MVC 6 da dəstəkləyir |
| `multiplatform-n-17 exp` | Moshi də refleksiyasız (codegen) işləyə bilir |

### Tapşırıq ilə kod uyğunsuzluğu
| Yer | Problem |
|---|---|
| `xususi-sinifler-p-22` | «üç halı emal edən funksiya» — `sealed interface`-də iki hal var |
| `genislenmeler-p-25` | «Int üçün get operatoru» — həll adi funksiyadır (`operator`, `[]` yoxdur) |
| `generics-p-19` | «Generik Map yarat» — kod `Lugat<K, V>` sinfi qurur |
| `multiplatform-p-18` | «map ilə modelləşdir» — başlanğıc kod `when`-dir |
| `sintaksis-p-22` | «21-dən yuxarı» — ipucu `> 20` (21 daxil) |
| `dovrler-p-22` | «i*j kombinasiyaları» — çıxış vurma deyil, yan-yana yazılış (`11 21 31`) |
| `delegatlar-p-07` | şərh «mənfi olmayan», ipucu `yeni > 0` (0-ı rədd edir) |
| `lambda-p-22` | `hell` `"1 2 4 "` (sonda boşluq) çap edir, `gozlenilen` `"1 2 4"` — yoxlayıcı kəsir |
| `sertler-p-17` | uzun il qaydasında `% 400` halı yoxdur |
| `sertler.code.f83969db` | `when`-də `0` heç bir budağa düşmür; bir budağın etiketi paltar adı deyil |
| `oop.p.b5831c1e` | «üç çağırış forması» — adlı arqumentlə dörd |

### Mətn / üslub
| Yer | Problem |
|---|---|
| `xususi-sinifler-n-22 opts[0]` | «Mümkün deyir» → «Mümkün deyil» |
| `giris` (hero, `p.86b925f2`, `h3.9d286303`, `callout.a85f49eb`) | sayt ifadələri tətbiqdə: «bu sayt», «bir səhifədə», Ctrl+K, «sol panel» |
| `kod-meydani.list.a94459fb` | «Boş redaktor» düyməsi — tətbiqdə çipin adı «Kod meydanı»dır |
| `tarixce.p.2bf6dd15`, `qurasdirma.p.09d77442` | rəsmi «istifadə edin» — qalan mətn «sən» formasındadır |
| `funksiyalar-n-18 exp` | «Unit qayıdır amma dəyəri yoxdur» — Unit-in tək dəyəri var |
| `java-muqayise-n-11 exp` | «Java-da null məlumatı yoxdur» — `@Nullable`/`@NotNull` var |
