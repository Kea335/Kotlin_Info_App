# KotlinAZ — tərcümə siyasəti (T4.0)

Bu sənəd hər tərcümə tapşırığının məcburi hissəsidir — insan tərcüməçi də,
AI də ([`PROMPT.md`](PROMPT.md)) eyni qaydalarla işləyir. Terminlərin
təsdiqlənmiş qarşılıqları [`glossary.json`](glossary.json)-dadır.

Baza dil **az**-dır. Tərcümə bazanın üzərinə qoyulan **overlay**-dir:
`localization/<dil>/{content,exercises,quiz,playground}.json`. Overlay-də boş
qalan (`""`, `[]`) sahə bazadan götürülür (fallback).

Qaydaların əksəriyyətini `node tools/validate-locales.js <dil>` avtomatik
yoxlayır — aşağıda hər qaydanın yanında **[xəta]**, **[xəbərdarlıq]** və ya
**[əl ilə]** qeyd olunub.

---

## 1. Tərcümə OLUNMUR

- **Kotlin açar sözləri**: `fun`, `val`, `var`, `class`, `object`, `when`, `if`,
  `for`, `suspend`, `sealed`, `data class`, `lateinit`, `override`…
- **Stdlib / API / framework adları**: `listOf`, `println`, `StateFlow`,
  `Activity`, `Composable`, `ViewModel`… Tam siyahı: `glossary.json` →
  `doNotTranslate`. **[xəbərdarlıq]** — bazada olan ad tərcümədə yoxdursa.
- **Kod identifikatorları** — kod nümunələrində sinif, funksiya, dəyişən adları.
  **[xəta]** — kodun skeleti dəyişibsə (bax §3).
- **Inline kod** — mətndəki `` `kod` `` span-ları (`{"k":"code"}`) hərfi-hərfinə
  eyni qalır, yalnız cümlədəki yeri dəyişə bilər. **[xəbərdarlıq]**
- **Sabit ID-lər** — bölmə, blok, çalışma, quiz, preset ID-ləri. Overlay-də
  açardırlar; açarlara toxunulmur. **[xəta]** — naməlum açar.
- **Cavab indeksi** (`a`) və variantların **sırası**. `opts` massivində element
  sayı və sırası bazadakı kimidir — yalnız mətnləri tərcümə olunur. **[xəta]** — say fərqlidirsə; sıra **[əl ilə]**.
- **Texniki sahələr**: `level`, `no`, `index`, `special`, `sectionId`, `lang`,
  `output`, `code` (nəzəri sual), `href`… Overlay-də ümumiyyətlə yoxdurlar.
  **[xəta]** — görünsələr.

## 2. Tərcümə OLUNUR

- Tapşırıq mətni (`tapsiriq`), ipucu (`ipucu`), izah (`exp`), sual (`q`),
  variantların **mətni** (`opts[i]`)
- Başlıq, alt başlıq, `kicker`, `badge`, callout başlığı, kart başlığı, tab
  etiketi, cədvəl başlığı və xanaları, siyahı elementləri
- Kod bloklarının **başlığı** (`title`) — kodun özü yox
- Kod içindəki **şərhlər**: `// İstifadəçinin yaşını yoxlayırıq` → `// Check the user's age`
- Meydan nümunələrinin etiketi (`label`)
- UI sətirləri — `app/src/debug/res/values-<dil>/strings.xml`. Dil 100 %-ə
  çatanda `build-locale.js` faylı `src/main/res/`-ə köçürür (T4.7 qapısı):
  yarımçıq UI release-ə düşmür

### Span quruluşu

Mətn sahələrinin çoxu span massividir:
`[{"k":"t","v":"Kotlin dilini "},{"k":"grad","v":"sıfırdan"},{"k":"t","v":" öyrən"}]`.

- Span **növü** (`k`) dəyişmir, yalnız `v` tərcümə olunur. **[xəta]** — naməlum növ.
- Dilin söz sırası tələb edirsə span-ların sırası dəyişə, iki `t` span birləşə
  və ya bölünə bilər — vurğu (`b`, `i`, `grad`) mənaca eyni sözə düşməlidir.
- `link` span-ının `href`-i dəyişmir. **[xəta]** — `href` itibsə.
- Siyahı (`items`), cədvəl (`head`, `rows`) ölçüləri bazadakı kimidir. **[xəta]**

## 3. Kod: yalnız şərhlər

Kod blokunda (`content` → `code`), başlanğıc kodda (`starter`), model həldə
(`hell`) və meydan nümunəsində (`playground` → `code`) **yalnız şərhlər**
tərcümə olunur. Kodu olduğu kimi köçür, yalnız `//…` və `/* … */` daxilini dəyiş.

Yoxlama `tools/kod-serhleri.js` ilə aparılır: kod hissələrə bölünür, şərhlər
nişanla əvəzlənir, bazanın və tərcümənin «skeleti» müqayisə olunur.
Skelet fərqlidirsə — boşluq, sətir, dırnaq daxil — **[xəta]**, və
`build-locale.js` həmin sahə üçün bazanı saxlayır (tərcümə APK-ya düşmür).

Şərhi olmayan kod overlay-də ümumiyyətlə görünmür — tərcümə ediləsi heç nə yoxdur.

Kod bloku başlığı fayl adıdırsa (`Sinif.kt`, `build.gradle.kts`), o da dəyişmir —
fayl adı kodun içindəki identifikatorlarla bağlıdır.

### Dildən asılı olmayan dəyərlər

Bəzi dəyərlərdə tərcümə olunası söz yoxdur: yalnız koddan ibarət variantlar
(`["null", "-1", "0", "NullPointerException"]`), `data class` etiketi, fayl adı,
`[1, 4, 9]` kimi nəticə. Onlar hər dildə bazadakı kimi qalır, faizə sayılmır və
`overlay-parca.js cixar` onları vermir. Qayda mühafizəkardır: dəyərdə **bir**
adi söz də varsa («Bəli», «yaz.», «(tam bölmə!)»), o tərcümə olunur.
Siyahı: `node tools/overlay-parca.js neytral <fayl>`.

### String literal-lar — əsas qayda

Kod içindəki mətn literal-ları **defolt olaraq dəyişmir**:

```kotlin
// AZ və EN variantında eyni qalır:
println("Salam, Kotlin!")
```

Səbəb: `gozlenilen` (gözlənilən çıxış), `hell` (model həll) və `starter`
bir-birindən asılıdır. Literal dəyişsə üçü də eyni anda dəyişməlidir —
625 çalışma × 14 dil = 8 750 sinxronlaşma nöqtəsi. Bu riski **sıfırlayırıq**.

Nəticə: tapşırıq mətni literalı sitat gətirirsə, sitat da **dəyişmir**:

> az: `Ekrana "Salam, Kotlin!" mətnini çap et.`
> en: `Print the text "Salam, Kotlin!" to the screen.`

Lazım gələrsə mənası mötərizədə verilə bilər: `"Salam, Kotlin!" ("Hello, Kotlin!")`.

### İstisna: `localizedIo`

Dərsin məqsədi məhz mətnlə işləməkdirsə (məsələn `uppercase()`, `split()`
nümunəsi), çalışma `localization/localizedIo.json`-a əlavə olunur:

```json
{
  "calismalar": {
    "metnler-p-07": "uppercase() — çıxış dilin öz mətnində olmalıdır"
  }
}
```

Belə çalışmada `starter`, `hell` və `gozlenilen` **birlikdə** tərcümə olunur:
literalların mətni də dəyişə bilər, şablon ifadələri (`$ad`, `${…}`) və kodun
qalanı yox. Siyahı baza məzmunundan ayrıdır, çünki `exercises.json` saytdan
yenidən qurulur. **[xəta]** — siyahıdakı ID praktiki çalışma deyilsə.

`localizedIo` çalışmasında `hell` işlədiləndə çıxış `gozlenilen` ilə eyni
olmalıdır — bunu `node tools/check-code.js` yoxlayır (T4.6). **[əl ilə]** — işə sal.

## 4. Lüğət

- Terminin qarşılığı `glossary.json` → `terms`-dədir. Tərcüməçi **öz
  variantını uydurmur**. Bazada termin varsa, tərcümədə onun təsdiqlənmiş
  forması (və ya `_formalar`-dakı qəbul olunan forma — hal şəkilçili kök,
  ingiliscə ad) olmalıdır. **[xəbərdarlıq]**
- Lüğətdə olmayan, amma təkrarlanan termin lazımdırsa — əvvəl lüğətə əlavə
  olunur (15 dilin hamısı ilə), sonra tərcümədə işlədilir. **[xəta]** — dil əskikdirsə.
- `doNotTranslate` siyahısındakı adlar transliterasiya da olunmur:
  `ViewModel`, «Görünüş modeli» və ya «ВьюМодель» yox. **[xəbərdarlıq]**
- **Pivot dillər** — az, en, tr, ru. Onların lüğət qarşılıqlarını insan
  təsdiqləyir (`_meta.tesdiq`); qalan dillər bu dördündən yoxlanılır.

## 5. Rəqəmlər və formatlar

- Kod nümunələrində Avropa rəqəmləri (5, 10) — ərəb və fars daxil. Kod onsuz da dəyişmir.
- Mətndə tarix və say formatı dilin öz qaydası ilə.
- `%1$d`, `%2$s`, `{0}` nişanları **olduğu kimi** qalır, sırası dəyişə
  bilər; dəsti eyni olmalıdır. **[xəta]**
- `strings.xml`-də apostrof `\'` ilə qaçırılır, `%%` olduğu kimi qalır.

## 6. Üslub

- Müraciət: **sən** forması, birbaşa və mehriban — bazadakı kimi. Rəsmi
  «siz» forması olan dillərdə (de, fr, ru, tr…) qeyri-rəsmi forma (du, tu, ты, sen).
- Proqramçıların həmin dildə həqiqətən işlətdiyi terminlər — sözbəsöz
  kalka yox. Şübhədə: lüğət, sonra rəsmi Kotlin sənədlərinin həmin dildəki tərcüməsi.
- Mətni qısaltma, genişləndirmə, öz izahını əlavə etmə. Bazada səhv görsən —
  tərcüməni düz et, amma bazadakı səhvi ayrıca bildir (issue).
- RTL dillər (ar, fa): kod və identifikatorlar LTR qalır — tətbiq bunu
  özü təcrid edir (T3.6), mətnə əl ilə istiqamət simvolu qoyma.

## 7. Tərcümədən sonra

```
node tools/validate-locales.js <dil>   # xəta 0 olmalıdır
node tools/build-locale.js <dil>       # aktivləri yenidən qur
node tools/check-code.js               # localizedIo varsa
```

Xəbərdarlıqlara baxılır: «bazadakı mətnin eynisi» — adətən tərcümə
unudulub (istisna: yalnız koddan ibarət dəyər, məsələn `["start()", "main()"]`
variantları — onlar dəyişmir);
«lüğət» və «tərcümə olunmamalıdır» — çox vaxt real səhvdir, bəzən yanlış
pozitiv (məsələn termin başqa hal şəkilçisi ilə) — onda formanı lüğətin
`_formalar` hissəsinə əlavə et.
