# KotlinAZ — Android tətbiqi

[![Android CI](https://github.com/Kea335/Kotlin_Info_App/actions/workflows/android.yml/badge.svg)](https://github.com/Kea335/Kotlin_Info_App/actions/workflows/android.yml)
[![Lisenziya: MIT](https://img.shields.io/badge/lisenziya-MIT-blue.svg)](LICENSE)
<!-- dil-nisani:basla -->
[![Dillər](https://img.shields.io/badge/dill%C9%99r-4%20%2F%2015-7F52FF)](#dil-dəstəyi)
<!-- dil-nisani:son -->

**Offline Kotlin learning app** — 32 lessons, 1250 exercises, a real compiler,
15 languages planned. Kotlin · Jetpack Compose · StateFlow · DataStore · Room.

[KotlinAZ](https://github.com/Kea335/Kotlin_Info_Web) saytının nativ Android
qarşılığı. Bütün məzmun — 32 bölmə, 144 kod nümunəsi, 6 interaktiv nümayiş,
1250 çalışma və bilik testi — tətbiqin içindədir və **internet olmadan işləyir**.

Yeganə istisna kodun real icrasıdır: Kotlin kompilyatoru JetBrains
serverlərindədir, ona görə kod meydanı və praktiki çalışmalardakı «Yoxla»
düyməsi bağlantı tələb edir.

---

## Nə var

| Bölmə | Məzmun | Oflayn? |
|---|---|---|
| Dərslər | 32 bölmə, səviyyə (Junior/Middle/Senior) və mənimsəmə tərəqqisi ilə | ✅ |
| Kod nümunələri | 144 blok — sintaksis rəngləməsi və hazır nəticələrlə | ✅ |
| İnteraktiv nümayişlər | Tip çıxarışı, null-safety, kolleksiya zənciri, `when`, scope funksiyaları, coroutine vizualizatoru | ✅ |
| Çalışmalar | 25 mövzu × (25 nəzəri + 25 praktiki) = 1250 | ✅ (yoxlama istisna) |
| Bilik testi | Junior / Middle / Senior / Qarışıq rejimləri, hər turda 15 təsadüfi sual | ✅ |
| Axtarış | Bütün bölmələr üzrə tam mətn axtarışı | ✅ |
| Kod meydanı | Boş redaktor + 10 nümunə, söz tamamlama, real icra, dayandırma; kod yadda qalır, paylaşılır | ❌ internet lazımdır |
| Öyrənmə yolu | Bölmələr arası asılılıq, «Növbəti addım» tövsiyəsi, yumşaq kilid | ✅ |
| Səhvlərim | Testdə və çalışmada səhv cavab verilən suallar; yalnız onlardan test | ✅ |
| İşarələr və qeydlər | Dərs, çalışma və kod nümunəsini işarələmək, öz qeydlərin | ✅ |
| Statistika | Sayğaclar, 30 günlük fəallıq qrafiki, seriya, quiz tarixçəsi | ✅ |
| Dillər | 15 dil üçün hazır mühərrik; tam tərcümə olunan dil avtomatik görünür, RTL | ✅ |

---

## Dil dəstəyi

Cədvəl `node tools/validate-locales.js --readme` ilə yenilənir. Yalnız 100 %
tərcümə olunmuş dil tətbiqdə görünür və release APK-ya düşür (T4.7);
yarımçıq dil yalnız debug build-də, tərcüməçi üçündür.

<!-- diller:basla -->
| Dil | Kod | Tərcümə | Vəziyyət |
|---|---|---|---|
| Azərbaycan | `az` | 100 % | baza |
| English | `en` | 100 % | tətbiqdə |
| Русский | `ru` | 100 % | tətbiqdə |
| Türkçe | `tr` | 100 % | tətbiqdə |

Planlaşdırılan (overlay hələ yoxdur): Español, Português (Brasil), Deutsch, Français, العربية, فارسی, हिन्दी, Bahasa Indonesia, 简体中文, 日本語, 한국어.
<!-- diller:son -->

---

## Arxitektura

```
UI (Compose)  →  ViewModel  →  Repository  →  Assets (JSON) / DataStore / Room
                                    └──────→  JetBrains kompilyator API (yalnız icra)
```

Ekranlar yalnız vəziyyəti render edir; vəziyyət və proses ölümündən sağ
çıxan hissə (`SavedStateHandle`) ViewModel-dədir. Məzmun, tərəqqi (DataStore)
və öyrənmə tarixçəsi (Room) repository sərhədinin arxasındadır — testlərdə
fake ilə əvəz olunur.

```
tools/                        məzmun boru xətti (Node.js)
  html-parse.js               asılılıqsız HTML parseri
  content-lib.js              ortaq: blok mətni, axtarış indeksi, tərcümə sahələri
  build-content.js            sayt → baza (az) JSON aktivləri
  verify-content.js           məzmun itkisinin yoxlanması
  test-blok-idleri.js         blok id-lərinin sabitlik testi (sayt lazımdır)
  new-locale.js               yeni dil üçün boş overlay skeleti
  build-locale.js             overlay + baza → dil aktivləri
  validate-locales.js         overlay-lərin doğrulaması və tərcümə faizi
  check-r8.js                 release-də @Serializable serializatorları saxlanılıbmı
  check-i18n.js               Kotlin kodunda hardcoded UI mətni (strings.xml-ə)
  path.json                   öyrənmə yolu: bölmə → əvvəlcə tamamlanmalı bölmələr
  apk-budcesi.js              release APK-nın hissələr üzrə ölçüsü, 8 MB büdcə
  versiya.js                  versiyanın artırılması (gradle.properties → versionCode)
  check-code.js               kod nümunələri real kompilyatorda: çıxış = gözlənilən
  check-code-istisnalar.json  bilərəkdən işləməyən nümunələr, səbəbi ilə
  normallasdir-hallar.json    çıxış müqayisəsi qaydası — Kotlin və JS eyni hallarla

localization/<lc>/            tərcümə overlay-ləri — yalnız dəyişən sahələr, ID ilə
  content.json, exercises.json, quiz.json, playground.json

app/src/main/assets/content/  oflayn məzmun, dil başına bir qovluq
  locales.json                release dilləri: baza + 100 % tərcümə olunmuşlar
  az/                         baza — saytdan qurulur (≈840 KB)
    content.json              32 bölmə, bloklara ayrılmış, hər blokun sabit id-si
    exercises.json            1250 çalışma
    quiz.json                 15 ümumi sual (qarışıq rejimin hovuzuna əlavə olunur)
    playground.json           10 nümunə kod
  <lc>/                       tam tərcümə olunmuş dil — overlay-dən qurulur

app/src/debug/assets/content/ yarımçıq dillər — yalnız debug APK-ya düşür (T4.7)
  locales.json                release dilləri + yarımçıqlar (debug-da əsasını əvəzləyir)
  en/                         overlay-dən qurulur; boş sahələr bazadan gəlir

app/src/main/java/az/kotlinaz/app/
  data/                       modellər, repozitoriyalar, tərəqqi, statistika,
                              öyrənmə yolu, axtarış, kompilyator xidməti
  data/oyrenme/               Room: səhvlər, işarələr, qeydlər, quiz turları, fəallıq
  ui/theme/                   saytın rəng palitrası (açıq + qaranlıq, WCAG AA)
  ui/highlight/               Kotlin sintaksis rəngləyicisi
  ui/components/              blok renderi (paraqraf, kod, cədvəl, callout, …)
  ui/editor/                  kod redaktoru və söz tamamlama lüğəti
  ui/demos/                   6 interaktiv nümayiş
  ui/lessons, exercises, quiz, playground, search, settings,
     sehvlerim, isareler, statistika   ekranlar və onların ViewModel-ləri
app/schemas/                  Room sxemi (miqrasiyalar üçün commit olunur)
```

Sayt HTML-i çalışma vaxtında oxunmur — qurma mərhələsində struktur JSON-a
çevrilir və Compose ilə nativ göstərilir. WebView işlədilmir.

---

## Məzmunun yenilənməsi

Sayt dəyişəndə:

```bash
node tools/build-content.js
node tools/verify-content.js
```

`verify-content.js` saytdakı hər görünən mətn parçasının JSON-a köçdüyünü
yoxlayır. Ötürmə 100% olmalıdır, əks halda çıxış kodu 2 verir.

Sayt başqa yerdədirsə: `KOTLINAZ_WEB=C:/yol/kotlinweb node tools/build-content.js`

---

## Qurma

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Tələblər: **JDK 25** (Android Studio-nun `jbr` qovluğu işləyir), Android SDK 37.
Kod Java 17-yə hədəflənir, amma `gradle/gradle-daemon-jvm.properties` Gradle
daemon-u üçün JDK 25 tələb edir — başqa JDK ilə Gradle onu foojay-dan özü
endirməyə çalışır.

Testlər və məzmun yoxlaması:

```bash
./gradlew test                                # JVM unit testləri
./gradlew lint                                # Android Lint — yeni xəbərdarlıq = xəta
node tools/verify-content.js --assets-only    # aktivlərin bütövlüyü (sayt olmadan)
node tools/validate-locales.js                # dil overlay-ləri və tərcümə faizi
./gradlew assembleRelease && node tools/check-r8.js   # R8 serializatorları silməyib
```

Yeni dil əlavə etmək:

```bash
node tools/new-locale.js tr        # localization/tr/ — boş skelet
node tools/build-locale.js tr      # tr aktivləri — çatışmayan sahə bazadan
node tools/validate-locales.js tr  # struktur, texniki sahələr, faiz
```

Dil qapısı (T4.7): `build-locale.js` yarımçıq dili `app/src/debug/assets`-ə
yazır — o, yalnız debug build-də (tərcüməçi üçün) görünür, release APK-ya
düşmür. Dil 100 %-ə çatanda növbəti `build-locale.js` onu `src/main/assets`-ə
keçirir, `locales.json`-u və `res/xml/locales_config.xml`-i (Android 13+
sistem menyusu) yeniləyir — kod dəyişikliyi lazım deyil. `az` həmişə görünür.

Lint `app/lint-baseline.xml`-ə görə işləyir (hazırda boşdur): baseline-da
olmayan hər tapıntı, xəbərdarlıq da olsa, build-i sındırır. Lokalda
`local.properties`-də `sdk.dir` yolundakı `:` `\:` kimi yazılmalıdır
(`sdk.dir=C\:/…`), yoxsa lint `PropertyEscape` xətası verir.

Versiya `gradle.properties`-dəki `kotlinaz.versiya`-dadır, `versionCode`
ondan hesablanır. Buraxılış addımları: [`docs/RELEASE.md`](docs/RELEASE.md).

### İmzalı release

`keystore.properties` faylı repoda **yoxdur** — imza açarları heç vaxt commit
edilmir. Öz açarını yarat:

```bash
keytool -genkeypair -v -keystore kotlinaz-release.jks -alias kotlinaz -keyalg RSA -keysize 4096 -validity 10000
```

Sonra layihənin kökündə `keystore.properties` yarat:

```properties
storeFile=kotlinaz-release.jks
storePassword=...
keyAlias=kotlinaz
keyPassword=...
```

```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk` (≈2.6 MB; hissələr üzrə: `node tools/apk-budcesi.js`)

Fayl yoxdursa release yenə qurulur, sadəcə imzasız qalır. Release qurulusunda
R8 minifikasiyası və resurs təmizlənməsi aktivdir; `proguard-rules.pro`
kotlinx-serialization üçün lazımi `keep` qaydalarını saxlayır — onlarsız JSON
oxunuşu sınır.

> **Diqqət:** `kotlinaz-release.jks` tətbiqin imza kimliyidir. Google Play-ə
> yükləndikdən sonra bütün yeniləmələr məhz bu açarla imzalanmalıdır. Faylı və
> parolları itirsən, tətbiqi yeniləmək mümkün olmayacaq.

| Alət | Versiya |
|---|---|
| Gradle | 9.6.0 |
| Android Gradle Plugin | 9.4.0 |
| Kotlin | 2.4.10 (AGP-nin daxili dəstəyi) |
| Compose BOM | 2026.08.00 |
| minSdk / targetSdk | 24 / 37 |

---

## Test və CI

```bash
./gradlew test                          # JVM unit testləri (ViewModel, qaydalar, aktivlər)
./gradlew connectedDebugAndroidTest     # cihaz testləri — Room DAO-ları (emulyator lazımdır)
./gradlew lint                          # Android Lint — yeni xəbərdarlıq = xəta
node tools/check-i18n.js                # Kotlin kodunda hardcoded UI mətni
node tools/validate-locales.js          # dil overlay-ləri və tərcümə faizi
node tools/verify-content.js --assets-only
node tools/check-code.js                # kod nümunələri kompilyatorda (≈10 dəq, keşlə saniyələr)
```

CI (`.github/workflows/android.yml`) hər push və PR-da iki job işlədir:

- **build** — məzmun və dil yoxlaması → hardcoded mətn → unit testlər → lint →
  imzasız release build → R8 serializator yoxlaması (`tools/check-r8.js`) →
  APK ölçüsü və 8 MB büdcə (`tools/apk-budcesi.js`, Job Summary-də hesabat) →
  debug build; APK artifact kimi yüklənir.
- **cihaz-testleri** — API 34 emulyatorunda `connectedDebugAndroidTest`.

Gecəlik ayrıca workflow (`.github/workflows/kod-yoxlamasi.yml`) dərslərdəki
`fun main`-li kod bloklarını, 625 model həlli və meydan nümunələrini
JetBrains kompilyatorunda işlədib çıxışı gözlənilənlə tutuşdurur;
uyğunsuzluq olsa `kod-yoxlamasi` nişanlı issue açır. Dəyişməyən nümunələr
keşdən keçir — xidmət ictimaidir.

Asılılıqları Dependabot (`.github/dependabot.yml`) yeniləyir: Gradle
həftəlik, GitHub Actions aylıq.

---

## Kod icrası

Praktiki çalışmada «Yoxla» düyməsi kodu JetBrains-in rəsmi kompilyator
xidmətinə göndərir (`api.kotlinlang.org`) və çıxışı gözlənilən nəticə ilə
tutuşdurur — müqayisə qaydası saytdakı `normallasdir()` funksiyası ilə eynidir.

Bağlantı yoxdursa tətbiq bunu açıq deyir və oflayn yolu təklif edir: model
həlli aç, öz kodunla müqayisə et, «Həll etdim» ilə işarələ.

---

## Məxfilik

Kod meydanında və praktiki çalışmalarda yazdığınız kod icra üçün JetBrains
serverlərinə göndərilir. Başqa heç bir data cihazdan çıxmır — hesab, reklam,
analitika yoxdur. Tam mətn: [PRIVACY.md](PRIVACY.md) (Play Console-un
«Privacy policy» ünvanı).

- Android yedəyi tərəqqini, tənzimləmələri və meydanın kodunu saxlayır;
  kompilyator keşi ayrıca faylda (`kotlinaz_kes`) və yedəkdən çıxarılıb —
  `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`.
- Tənzimləmələrdəki «Xəta bildir» GitHub issue formunu versiya və cihaz
  məlumatı ilə doldurub brauzerdə açır; heç nə avtomatik göndərilmir.

---

## Mənbə

Məzmun müəllifi: [Kea335/Kotlin_Info_Web](https://github.com/Kea335/Kotlin_Info_Web)

Lisenziya: MIT — bax [LICENSE](LICENSE).
