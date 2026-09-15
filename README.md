# KotlinAZ — Android tətbiqi

[![Android CI](https://github.com/Kea335/Kotlin_Info_App/actions/workflows/android.yml/badge.svg)](https://github.com/Kea335/Kotlin_Info_App/actions/workflows/android.yml)

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
| Kod meydanı | Boş redaktor + 10 nümunə, söz tamamlama, real icra | ❌ internet lazımdır |

---

## Arxitektura

```
tools/                        məzmun boru xətti (Node.js)
  html-parse.js               asılılıqsız HTML parseri
  build-content.js            sayt → JSON aktivləri
  verify-content.js           məzmun itkisinin yoxlanması

app/src/main/assets/          oflayn məzmun (≈840 KB)
  content.json                32 bölmə, bloklara ayrılmış
  exercises.json              1250 çalışma
  quiz.json                   15 ümumi sual (qarışıq rejimin hovuzuna əlavə olunur)
  playground.json             10 nümunə kod
  search.json                 axtarış indeksi

app/src/main/java/az/kotlinaz/app/
  data/                       modellər, repozitoriya, tərəqqi, kompilyator xidməti
  ui/theme/                   saytın rəng palitrası (açıq + qaranlıq)
  ui/highlight/               Kotlin sintaksis rəngləyicisi
  ui/components/              blok renderi (paraqraf, kod, cədvəl, callout, …)
  ui/editor/                  kod redaktoru və söz tamamlama lüğəti
  ui/demos/                   6 interaktiv nümayiş
  ui/screens/                 dərslər, çalışmalar, test, meydan, axtarış, tənzimləmə
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
node tools/verify-content.js --assets-only    # aktivlərin bütövlüyü (sayt olmadan)
```

CI (`.github/workflows/android.yml`) hər push və PR-da eyni addımları
işlədir: məzmun yoxlaması → test → lint → debug build; APK artifact kimi
yüklənir.

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

APK: `app/build/outputs/apk/release/app-release.apk` (≈1.9 MB)

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

## Kod icrası

Praktiki çalışmada «Yoxla» düyməsi kodu JetBrains-in rəsmi kompilyator
xidmətinə göndərir (`api.kotlinlang.org`) və çıxışı gözlənilən nəticə ilə
tutuşdurur — müqayisə qaydası saytdakı `normallasdir()` funksiyası ilə eynidir.

Bağlantı yoxdursa tətbiq bunu açıq deyir və oflayn yolu təklif edir: model
həlli aç, öz kodunla müqayisə et, «Həll etdim» ilə işarələ.

---

## Mənbə

Məzmun müəllifi: [Kea335/Kotlin_Info_Web](https://github.com/Kea335/Kotlin_Info_Web)

Lisenziya: MIT — bax [LICENSE](LICENSE).
