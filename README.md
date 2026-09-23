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
| Kod meydanı | Boş redaktor + 10 nümunə, söz tamamlama, real icra, dayandırma; kod yadda qalır, paylaşılır | ❌ internet lazımdır |

---

## Arxitektura

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

localization/<lc>/            tərcümə overlay-ləri — yalnız dəyişən sahələr, ID ilə
  content.json, exercises.json, quiz.json, playground.json

app/src/main/assets/content/  oflayn məzmun, dil başına bir qovluq
  locales.json                release dilləri: baza + 100 % tərcümə olunmuşlar
  az/                         baza — saytdan qurulur (≈840 KB)
    content.json              32 bölmə, bloklara ayrılmış, hər blokun sabit id-si
    exercises.json            1250 çalışma
    quiz.json                 15 ümumi sual (qarışıq rejimin hovuzuna əlavə olunur)
    playground.json           10 nümunə kod
    search.json               axtarış indeksi
  <lc>/                       tam tərcümə olunmuş dil — overlay-dən qurulur

app/src/debug/assets/content/ yarımçıq dillər — yalnız debug APK-ya düşür (T4.7)
  locales.json                release dilləri + yarımçıqlar (debug-da əsasını əvəzləyir)
  en/                         overlay-dən qurulur; boş sahələr bazadan gəlir

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

CI (`.github/workflows/android.yml`) hər push və PR-da eyni addımları
işlədir: məzmun və dil yoxlaması → test → lint → imzasız release build +
R8 serializator yoxlaması (`tools/check-r8.js`) → debug build; APK artifact
kimi yüklənir. Asılılıqları Dependabot (`.github/dependabot.yml`) həftəlik
yeniləyir.

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
