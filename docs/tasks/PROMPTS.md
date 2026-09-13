# Kopyalanmaq üçün task promptları

Hər blok bir işə başlamaq üçün hazır mətndir. İstifadə: layihə qovluğunda yeni
sessiya aç, uyğun bloku kopyalayıb yapışdır. Prompt taskın öz faylına istinad
edir — bütün detallar orada var.

Sıra: **T0.1 → T0.2 → T0.3 → T1.1 → T1.3**, sonra sprint ardıcıllığı ilə.

---

## Bu sessiyadan qalanlar

```
Gradle versiya keçidini commit et: libs.versions.toml (AGP 9.4.0) və
gradle-wrapper.properties (Gradle 9.6.0). README.md-dəki "Alət / Versiya"
cədvəlini də eyni commit-də yenilə — Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.10,
Compose BOM 2026.08.00, minSdk/targetSdk 24/37. Bu, T0.3 taskının 1-ci addımıdır.
```

```
Birləşdirilmiş lokal branch-ı sil: git branch -d docs/inkisaf-tasklari
```

---

## Sprint 0 — Cəld düzəlişlər

```
T0.1 — Axtarışda İ/ı düzəlişi (P0, 0,5 gün)

docs/tasks/T0.1-axtaris-locale.md faylını oxu və taskı tam icra et.
Problem: AppViewModel.axtar() lowercase() ilə Locale.ROOT işlədir, ona görə
"İstifadəçi" → "i̇stifadəçi" (i + U+0307) olur və İ ilə başlayan söz tapılmır.
Həll: axtarışı data/AxtarisMotoru.kt saf sinfinə çıxar,
lowercase(Locale.forLanguageTag("az")) işlət.
Qəbul kriteriyasındakı hər maddəni ödə. Branch: fix/axtaris-locale.
```

```
T0.2 — Kompilyator versiyasını real siyahıya bağla (P0, 1 gün)

docs/tasks/T0.2-kompilyator-versiyasi.md faylını oxu və taskı tam icra et.
Problem: KotlinCompiler.kt:29-dakı 2.1.20 / 2.1.0 / 2.0.20 versiyalarının heç biri
api.kotlinlang.org/versions siyahısında yoxdur; icra yalnız serverin ən son
stabilə yönləndirməsi sayəsində işləyir.
Həll: ehtiyat siyahını 2.4.10 / 2.4.20 / 2.3.21 et, /versions-u runtime-da oxu və
24 saat keşlə, aktiv versiyanı Meydanda və Tənzimləmələrdə göstər.
Branch: fix/kompilyator-versiyasi.
```

```
T0.3 — Repo təmizliyi və lisenziya (P2, 0,5 gün)

docs/tasks/T0.3-repo-temizliyi.md faylını oxu və taskı tam icra et.
Gradle/AGP keçidini commit et və README cədvəlini yenilə, androidx.webkit
asılılığını sil, ölü funksiyaları sil (kotlinVisualTransformation,
rememberKotlinTransformation, ContentRepository.section), BuildConfig-i aç və
versiyanı oradan götür, MIT LICENSE əlavə et.
Branch: chore/repo-temizliyi.
```

```
T0.4 — Başlanğıc kodu həllə bərabər olan 16 çalışma (P2, 0,5 gün)

docs/tasks/T0.4-trivial-calismalar.md faylını oxu və taskı tam icra et.
16 praktiki çalışmada starter == hell, yəni istifadəçi heç nə yazmadan "Yoxla"
basıb keçir. Düzəliş sayt mənbəyində (Kotlin_Info_Web/js/exercises.js) edilməli,
sonra node tools/build-content.js ilə aktivlər yenidən qurulmalıdır.
verify-content.js-ə qalıcı qayda əlavə et. Branch: fix/trivial-calismalar.
```

---

## Sprint 1 — Test və CI

```
T1.1 — Test infrastrukturu və biznes məntiqi testləri (P0, 2 gün)

docs/tasks/T1.1-test-bazasi.md faylını oxu və taskı tam icra et.
JUnit + kotlin-test + coroutines-test qur, app/src/test strukturunu yarat və
faylda sadalanan 7 test faylını yaz: Tereqqi, neticeniNormallasdir, Tamamlama,
highlightKotlin, UserPrefs kodeki, hovuzlariQur, AxtarisMotoru.
Testlər mövcud davranışı olduğu kimi qıfıllamalıdır — davranış dəyişmir.
Ən azı 40 test metodu. Branch: test/biznes-mentigi.
```

```
T1.2 — Aktivlərin smoke testləri (P0, 0,5 gün)

docs/tasks/T1.2-aktiv-testleri.md faylını oxu və taskı tam icra et.
5 JSON aktivini ContentRepository ilə eyni Json konfiqurasiyası ilə decode et,
struktur sabitlərini (32 bölmə, 625+625 çalışma, 15 quiz) və bütövlüyü yoxla:
ID unikallığı, opts.size == 4, a in 0..3, starter != hell, sectionId uyğunluğu.
Branch: test/aktivler.
```

```
T1.3 — GitHub Actions CI (P0, 1 gün)

docs/tasks/T1.3-github-actions.md faylını oxu və taskı tam icra et.
.github/workflows/android.yml yarat: JDK 25 (gradle-daemon-jvm.properties
toolchainVersion=25 tələb edir), setup-gradle, test → lint → assembleDebug →
node tools/verify-content.js --assets-only.
verify-content.js-ə --assets-only rejimi əlavə et (CI-da sayt repo-su yoxdur,
skript indiki halda çökür). Branch protection və README badge də daxildir.
Branch: ci/github-actions.
```

```
T1.4 — Lint baseline (P1, 0,5 gün)

docs/tasks/T1.4-lint-baseline.md faylını oxu və taskı tam icra et.
lint { baseline; abortOnError = true } qur, mövcud xəbərdarlıqları baseline-a al,
yeniləri build-i sındırsın. Baseline-dakı ciddi maddələri T5.6-ya köçür.
Branch: ci/lint-baseline.
```

---

## Sprint 2 — Arxitektura və state

```
T2.1 — AppContainer və Application sinfi (P1, 1 gün)

docs/tasks/T2.1-appcontainer.md faylını oxu və taskı tam icra et.
KotlinAzApp : Application + AppContainer yarat; ContentRepository, UserPrefs və
CompilerRepository bir dəfə orada yaransın. ViewModel-lər viewModelFactory ilə
constructor injection alsın; AndroidViewModel-dən adi ViewModel-ə keç.
Hilt işlətmə. Branch: refactor/appcontainer.
```

```
T2.2 — CompilerRepository və icra ViewModel-ləri (P1, 2 gün)

docs/tasks/T2.2-compiler-repository.md faylını oxu və taskı tam icra et.
Problem: compiler.isle() birbaşa Composable-dan çağırılır (ExercisePlayerScreen.kt:460,
PlaygroundScreen.kt:201) — istifadəçi ekrandan çıxsa sorğu səssiz ləğv olur.
CompilerRepository interfeysi yarat, ExerciseViewModel və PlaygroundViewModel-ə
keçir, yoxlama məntiqini (normallaşdırma + müqayisə + "həll edildi") ViewModel-ə al.
FakeCompilerRepository ilə 6 halın testi. Branch: refactor/compiler-viewmodel.
```

```
T2.3 — QuizViewModel və turun qorunması (P0, 1 gün)

docs/tasks/T2.3-quiz-viewmodel.md faylını oxu və taskı tam icra et.
Testin bütün vəziyyəti remember-dədir — proses öldürüləndə 15 sualın 12-si itir.
SavedStateHandle-a keçir: seçilmiş sualların ID siyahısı (shuffle nəticəsi, seed yox),
indeks, bal, seçim, bitdi. Səhv cavab verilən ID-ləri topla (T5.2 üçün).
Branch: refactor/quiz-viewmodel.
```

```
T2.4 — Qalan UI state-in bərpası və configChanges (P0, 1 gün)

docs/tasks/T2.4-process-death.md faylını oxu və taskı tam icra et.
Çalışma rejimi/filtri/indeksi, tab seçimi, axtarış sorğusu, dialoq bayraqları və
kod kartının nəticə paneli rememberSaveable-a keçir.
AndroidManifest.xml:21-dəki configChanges sətrini tamamilə sil.
"Don't keep activities" və am kill ssenariləri ilə yoxla. Branch: fix/process-death.
```

```
T2.5 — AppViewModel-i feature ViewModel-lərə böl (P1, 1 gün)

docs/tasks/T2.5-viewmodel-bolgusu.md faylını oxu və taskı tam icra et.
LessonsViewModel, SearchViewModel, SettingsViewModel yarat; AppRoot-dakı 14
collectAsStateWithLifecycle çağırışını ekranların özünə köçür.
Faylları ui/lessons, ui/exercises, ui/quiz, ui/playground, ui/search, ui/settings
qovluqlarına git mv ilə böl. Davranış dəyişmir, testlər yaşıl qalmalıdır.
Branch: refactor/feature-viewmodels.
```

```
T2.6 — Başlanğıc sürəti (P1, 0,5 gün)

docs/tasks/T2.6-baslangic-sureti.md faylını oxu və taskı tam icra et.
Hazırda 5 aktiv ardıcıl oxunur və hamısı bitməyincə ekran fırlanğıcdadır.
content.json gələn kimi hazir = true et, qalanları async ilə paralel yüklə,
ContentRepository-də hər keş üçün ayrı Mutex qoy.
adb shell am start -W ilə əvvəl/sonra ölç və nəticəni PR-a yaz.
Branch: perf/baslangic.
```

```
T2.7 — Tip-təhlükəsiz naviqasiya (P2, 1 gün)

docs/tasks/T2.7-tip-tehlukesiz-naviqasiya.md faylını oxu və taskı tam icra et.
String route-ları (@Serializable object/data class) tip-təhlükəsiz route-larla əvəzlə,
composable<T> və toRoute() işlət. saveState/restoreState və launchSingleTop
davranışı qalmalıdır. Branch: refactor/type-safe-nav.
```

---

## Sprint 3 — Localization mühərriki

```
T3.1 — Sabit blok ID-ləri (P0, 1 gün)

docs/tasks/T3.1-sabit-blok-idleri.md faylını oxu və taskı tam icra et.
Bu task T3.2-dən ƏVVƏL gəlir: bloklar hazırda yalnız mövqe ilə tanınır
(key = "b$index"), ona görə mövqeyə bağlı tərcümə sayt dəyişəndə səssizcə sürüşər.
build-content.js hər bloka məzmun hash-indən sabit id versin
(<sectionId>.<tip>.<hash8>), Block modellərinə id sahəsi əlavə olunsun.
Sabitlik testi: sayta paraqraf əlavə edəndə köhnə id-lər dəyişməməlidir.
Branch: content/blok-idleri.
```

```
T3.2 — Çoxdilli məzmun boru xətti (P0, 2 gün)

docs/tasks/T3.2-coxdilli-boru-xetti.md faylını oxu və taskı tam icra et.
Bazanı assets/content/az/ altına köçür, localization/<locale>/ overlay formatını
qur (yalnız tərcümə olunan sahələr, ID ilə açarlanmış) və üç alət yaz:
build-locale.js, validate-locales.js, new-locale.js.
Texniki sahələr (a, opts.size, level, index, special, sectionId) locale-lər
arasında dəyişə bilməz — validator bunu yoxlasın.
Branch: content/coxdilli-boru-xetti.
```

```
T3.3 — Dil modeli və seçim mexanizmi (P0, 1,5 gün)

docs/tasks/T3.3-dil-secimi.md faylını oxu və taskı tam icra et.
AppDil enum-u (15 dil + sistem), resurs qovluqları, locales_config.xml,
AppCompatDelegate.setApplicationLocales ilə dil dəyişməsi.
DİQQƏT: bu, AppCompatActivity və AppCompat teması tələb edir — hazırda
ComponentActivity + android:Theme.Material var. Taskdakı hər iki variantı oxu.
ContentRepository seçilmiş dilin qovluğunu oxusun, fallback en → az.
Tərəqqi ID-lərə bağlı olduğu üçün dil dəyişəndə itməməlidir (test).
Branch: i18n/dil-secimi.
```

```
T3.4 — Bütün hardcoded mətnləri resources-a çıxar (P0, 2 gün)

docs/tasks/T3.4-hardcoded-metnler.md faylını oxu və taskı tam icra et.
strings.xml-də hazırda tək sətir var; 68 literal + enum etiketləri (Level.label,
ThemeMode.label, QuizRejimi.label/izah, Rejim.etiket) + contentDescription-lar +
6 demonun mətnləri resources-a çıxarılmalıdır.
Sayğaclar üçün %1$d formatı və plurals işlət (rus/ərəb dilləri üçün məcburidir).
CI-a grep qaydası əlavə et — Android Lint Compose-dakı Text("…") literal-larını tutmur.
DemoHost.kt-ni ayrıca kiçik PR-a ayır. Branch: i18n/strings.
```

```
T3.5 — Runtime axtarış indeksi və dil üzrə normallaşdırma (P0, 1 gün)

docs/tasks/T3.5-axtaris-indeksi.md faylını oxu və taskı tam icra et.
search.json (120 KB) məzmunun surətidir — sil və indeksi runtime-da yüklənmiş
dilin content-indən qur (spansToPlain onsuz da var, çağırılmır).
Normallaşdırma dil üzrə: az/tr Locale ilə, ar/fa NFKC + hərəkə silmə,
CJK substring (tokenizasiya etmə), qalanlar NFKC + ROOT.
Hər dil qrupu üçün test. Branch: i18n/axtaris.
```

```
T3.6 — RTL dəstəyi (P0, 1 gün)

docs/tasks/T3.6-rtl.md faylını oxu və taskı tam icra et.
supportsRtl="true" et, layout auditi apar (TableView, TimelineView, zolaqlar),
kod bloklarını / redaktoru / konsol çıxışını LocalLayoutDirection provides Ltr ilə
məcburi LTR saxla. ar-XB pseudo-locale ilə hər ekranı gəz və screenshot əlavə et.
Branch: i18n/rtl.
```

```
T3.7 — APK və yaddaş büdcəsi (P1, 0,5 gün)

docs/tasks/T3.7-apk-budcesi.md faylını oxu və taskı tam icra et.
Dil dəyişəndə köhnə dilin obyektlərinin buraxıldığını Memory Profiler ilə yoxla,
CI-a APK ölçüsü hesabatı əlavə et (büdcə 8 MB). Aşılarsa exercises.json-u
mövzu-mövzu bölmə variantını qiymətləndir. Branch: perf/apk-budcesi.
```

---

## Sprint 4 — Tərcümə dalğaları

```
T4.0 — Lüğət və tərcümə siyasəti (P0, 1 gün)

docs/tasks/T4.0-terceume-siyaseti.md faylını oxu və taskı tam icra et.
localization/glossary.json (≥20 termin × 15 dil), POLICY.md və PROMPT.md yaz.
Əsas qayda: kod içindəki string literal-lar DEFOLT DƏYİŞMİR; yalnız
"localizedIo": true bayraqlı çalışmalarda starter/hell/gozlenilen birlikdə
tərcümə olunur. Validator glossary qaydalarını yoxlasın. Branch: i18n/policy.
```

```
T4.1 — Dalğa 1: ingilis dili (P0, 1–2 həftə)

docs/tasks/T4.1-dalga1-en.md faylını oxu və taskı tam icra et.
en pivotdur — sonrakı 13 dil ondan tərcümə olunacaq.
Ardıcıllıq: strings.xml → dərs başlıqları → dərs mətni (bölmə-bölmə) →
nəzəri çalışmalar (mövzu-mövzu) → praktiki → quiz/playground/demo.
Hər hissədən sonra build-locale.js + validate-locales.js işlət.
Release-dən əvvəl T4.6 kompilyasiya yoxlaması keçməlidir. Branch: i18n/locale-en.
```

```
T4.2 — Dalğa 2: tr, ru (P0, 1–2 həftə)

docs/tasks/T4.2-T4.5-qalan-dalgalar.md faylının T4.2 bölməsini oxu və icra et.
tr azərbaycancadan, ru ingiliscədən tərcümə olunur.
Əlavə yoxlama: türk dilində İ/ı casefold (T3.5 qaydası), rus dilində plurals
(one/few/many/other). Release v1.2.
```

```
T4.3 — Dalğa 3: es, pt-BR, de, fr (P1, 2–3 həftə)

docs/tasks/T4.2-T4.5-qalan-dalgalar.md faylının T4.3 bölməsini oxu və icra et.
Əlavə yoxlama: alman dilindəki uzun sözlərin düymə/çip etiketlərində kəsilməsi,
pt-BR üçün values-b+pt+BR qovluğu. Release v1.3.
```

```
T4.4 — Dalğa 4: ar, fa (P1, 2 həftə)

docs/tasks/T4.2-T4.5-qalan-dalgalar.md faylının T4.4 bölməsini oxu və icra et.
T3.6 (RTL) tamamlanmış olmalıdır. QA real cihazda: quiz variantları, cədvəllər,
timeline, kod kartı başlığı, redaktor, axtarış normallaşdırması.
localization/qa-rtl.md siyahısını doldur. Release v1.4.
```

```
T4.5 — Dalğa 5: hi, id, zh-CN, ja, ko (P1, 3 həftə)

docs/tasks/T4.2-T4.5-qalan-dalgalar.md faylının T4.5 bölməsini oxu və icra et.
Əlavə yoxlama: CJK axtarışı (substring), monospace şriftdə CJK şərhlərin sətir
hündürlüyü, devanaqari diakritikləri, values-b+id qovluğu.
Release v1.5 — 15/15 dil.
```

```
T4.6 — Kod nümunələrinin kompilyasiya yoxlaması (P0, 1 gün)

docs/tasks/T4.6-kod-kompilyasiya-yoxlamasi.md faylını oxu və taskı tam icra et.
769 kod nümunəsi (144 blok + 625 model həll) heç vaxt avtomatik yoxlanmayıb.
tools/check-code.js yaz: hash keşi, rate limit (300–500 ms, paralellik 2),
çıxışı normallasdir qaydası ilə müqayisə, nightly workflow.
İlk işləmədə az məzmununda da səhv tapılacağını gözlə. Branch: ci/kod-yoxlamasi.
```

```
T4.7 — Dil üzrə release qapısı (P0, 0,5 gün)

docs/tasks/T4.7-dil-release-qapisi.md faylını oxu və taskı tam icra et.
Qapı dilə tətbiq olunur, release-ə yox: 100 %-dən aşağı dil seçim siyahısında
görünmür, amma release-i bloklamır. Debug build-də bütün dillər görünsün.
Tövsiyə olunan variant: build-locale.js yalnız 100 % dillər üçün assets qovluğu
yaratsın — həm ölçü, həm mürəkkəblik azalır. Branch: i18n/release-qapisi.
```

---

## Sprint 5 — Funksiyalar və portfolio

Bu sprint tərcümə dalğaları ilə **paralel** gedə bilər — məzmuna toxunmur.

```
T5.1 — Yaddaş qatı: Room (P1, 2 gün)

docs/tasks/T5.1-room.md faylını oxu və taskı tam icra et.
5 entity: SehvCavab, Bookmark, Qeyd, QuizTuru, GunlukFealiyyet.
KSP + schemaLocation, schemas/1.json commit olunur.
DataStore-dakı mövcud data yerində qalır — miqrasiya lazım deyil.
DAO-lar birbaşa ViewModel-ə verilmir, OyrenmeRepository interfeysi arxasındadır.
Branch: feature/room.
```

```
T5.2 — «Səhvlərim» bölməsi (P1, 2 gün)

docs/tasks/T5.2-sehvlerim.md faylını oxu və taskı tam icra et.
Səhv cavablar həm testdən, həm nəzəri çalışmadan yazılsın (nəzəri çalışmada
hazırda heç bir iz qalmır). İki ardıcıl düzgün cavabdan sonra sual siyahıdan çıxsın.
Sual mətni seçilmiş dilin məzmunundan ID ilə götürülsün — dil dəyişəndə itməsin.
"Yalnız səhvlərdən test" rejimi əlavə et. Branch: feature/sehvlerim.
```

```
T5.3 — Bookmark və şəxsi qeydlər (P1, 2 gün)

docs/tasks/T5.3-bookmark-qeyd.md faylını oxu və taskı tam icra et.
Üç hədəf: bölmə, çalışma, kod bloku (T3.1-dəki sabit ID ilə).
Qeyd mətni dildən asılı deyil. Ayrıca ekran, iki tab, filtr çipləri.
Kod kartının başlıq zolağında yer darıdır — zolağı sürüşən et.
Branch: feature/bookmark-qeyd.
```

```
T5.4 — Statistika (P1, 2 gün)

docs/tasks/T5.4-statistika.md faylını oxu və taskı tam icra et.
statistikaHesabla() saf funksiya olsun (bugun parametr kimi verilsin ki, streak
testləri sabit tarixlə yazılsın). Ekran: səviyyə, sayğaclar, son 30 gün sütun
qrafiki (Compose Canvas, kitabxana əlavə etmə), quiz tarixçəsi, streak.
Tənzimləmələrdəki "Tərəqqi" bölməsi bu ekrana keçidlə əvəzlənir.
Branch: feature/statistika.
```

```
T5.5 — Learning Path (P1, 2 gün)

docs/tasks/T5.5-learning-path.md faylını oxu və taskı tam icra et.
tools/path.json-da asılılıq qrafı qur (taskda təklif olunan zəncir var),
novbetiAddim() saf tövsiyə funksiyası yaz və test et.
Kilidli bölmə AÇILA BİLMƏLİDİR — tövsiyə istiqamət verir, qadağa qoymur.
requires texniki sahədir, bütün dillərdə eyni qalmalıdır.
Branch: feature/learning-path.
```

```
T5.6 — Əlçatanlıq (P1, 1,5 gün)

docs/tasks/T5.6-elcatanliq.md faylını oxu və taskı tam icra et.
Kodda sıfır Role/semantics var; bütün düymələr Text.clickable-dır və
toxunma hədəfləri 25–33 dp (Material minimumu 48 dp).
Ortaq KAzDuyme komponenti yarat: minimumInteractiveComponentSize() + Role.Button.
contentDescription auditi, quiz variantları üçün stateDescription,
textFaint kontrastı. Accessibility Scanner və TalkBack ilə yoxla.
Branch: a11y/duymeler-ve-hedefler.
```

```
T5.7 — README, release axını və portfolio təqdimatı (P2, 1 gün)

docs/tasks/T5.7-readme-release.md faylını oxu və taskı tam icra et.
README-yə value proposition, 5 screenshot (işıqlı + qaranlıq), dil statusu
cədvəli (validator çıxışından), arxitektura diaqramı, badge-lər.
docs/RELEASE.md release axını. Repo About və topics.
Data safety qeydi: kod JetBrains serverlərinə göndərilir — həm README-də,
həm tətbiqdə yazılmalıdır. Branch: docs/portfolio.
```

---

## Release-dən əvvəl

```
Release qapısını yoxla: docs/tasks/RELEASE-GATE.md faylındakı 25 maddənin
hamısını bir-bir keç və nəticəni bildir. Keçməyən maddə varsa release dayandırılır.
```
