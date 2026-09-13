# Release qapısı

Hər release (v1.1-dən başlayaraq) bu siyahıdan keçir. Dil maddələri yalnız
**görünən** dillərə tətbiq olunur (bax T4.7) — yarımçıq dil release-i bloklamır.

## Build və kod

- [ ] `./gradlew test` yaşıl
- [ ] `./gradlew lint` yaşıl (yeni xəbərdarlıq yoxdur)
- [ ] `./gradlew assembleRelease` problemsiz qurulur və imzalanır
- [ ] GitHub Actions bütün mərhələlərdə yaşıl

## Məzmun

- [ ] `node tools/verify-content.js` — 100 % örtük, 0 struktur problemi
- [ ] `node tools/validate-locales.js` — `az` 100 %, hər görünən dil 100 %
- [ ] Heç bir çalışmada `starter == hell` (T0.4 qaydası)
- [ ] Cavab indeksləri və variant sırası bütün dillərdə eynidir
- [ ] `node tools/check-code.js --all` — kod nümunələri kompilyasiya olunur və
      gözlənilən nəticəni verir (T4.6)

## Davranış

- [ ] Kompilyator versiyası `/versions` siyahısında mövcuddur (T0.2)
- [ ] Kod meydanı və «Yoxla» real cihazda işləyir
- [ ] Oflayn rejimdə tətbiq tam açılır, yalnız icra bloklanır
- [ ] Process death ssenarisi: quiz turu və redaktor mətni qalır (T2.3, T2.4)
- [ ] Dil dəyişəndə tərəqqi itmir
- [ ] Axtarış hər görünən dildə nəticə verir (T3.5)

## Görünüş və əlçatanlıq

- [ ] RTL smoke testi (`ar`) real cihazda — RTL dilləri görünəndən sonra
- [ ] İşıqlı və qaranlıq temada 5 əsas ekran yoxlanılıb
- [ ] Şrift ölçüsü «böyük» seçimində mətn kəsilmir
- [ ] Accessibility Scanner-də yeni xəbərdarlıq yoxdur (T5.6-dan sonra)

## Paylama

- [ ] Release APK ölçüsü büdcədədir (T3.7)
- [ ] Yaddaşa yalnız seçilmiş dilin məzmunu yüklənir
- [ ] `versionCode` və `versionName` artırılıb
- [ ] Release notes yazılıb (`docs/RELEASE.md` şablonu)
- [ ] Teq qoyulub, imzalı APK GitHub Release-ə əlavə edilib
- [ ] README-dəki dil cədvəli yenilənib

---

**Qayda:** bu siyahıdakı bir maddə keçmirsə release dayandırılır. İstisna yalnız
o haldadır ki, problem yeni release-dən **əvvəl də mövcud olub** və düzəlişi
ayrıca taskda planlaşdırılıb — belə hal release notes-da açıq yazılmalıdır.
