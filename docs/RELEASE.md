# Release axını

KotlinAZ-ın hər buraxılışı bu addımlarla gedir. Yoxlama siyahısı
[`docs/tasks/RELEASE-GATE.md`](tasks/RELEASE-GATE.md)-dədir — bir maddə keçmirsə
release dayandırılır.

## 1. Hazırlıq

- `main` yaşıldır: GitHub Actions-da `build` və `cihaz-testleri` job-ları keçib.
- Release qapısının siyahısı keçib (build, məzmun, davranış, görünüş, paylama).
- Kod nümunələri kompilyatorda gözlənilən nəticəni verir (gecəlik
  «Kod yoxlaması» workflow-u yaşıldır, ya da lokalda):

  ```bash
  node tools/check-code.js --all
  ```

- Dil cədvəli təzədir:

  ```bash
  node tools/validate-locales.js --readme   # README-dəki cədvəl və nişan
  ```

## 2. Versiya

Versiya tək yerdədir — `gradle.properties` → `kotlinaz.versiya`. `versionCode`
ondan hesablanır (`X * 10000 + Y * 100 + Z`), əl ilə yazılmır:

```bash
node tools/versiya.js minor      # yeni funksiya/dil: 1.2.3 → 1.3.0
node tools/versiya.js patch      # yalnız düzəliş:    1.2.3 → 1.2.4
node tools/versiya.js major      # böyük dəyişiklik:  1.2.3 → 2.0.0
```

Skript versiyanın geri getməsinə icazə vermir. Dəyişiklik ayrıca commit-dir:

```bash
git commit -am "Versiya 1.3.0"
```

## 3. Teq və imzalı build

```bash
git tag -a v1.3.0 -m "v1.3.0"
git push origin main --tags

./gradlew assembleRelease            # keystore.properties lazımdır (README → İmzalı release)
node tools/check-r8.js               # R8 serializatorları silməyib
node tools/apk-budcesi.js            # ölçü büdcədədir (8 MB)
```

APK: `app/build/outputs/apk/release/app-release.apk`. İmzasız çıxırsa
(`app-release-unsigned.apk`) — `keystore.properties` tapılmayıb, buraxma.

## 4. GitHub Release

```bash
gh release create v1.3.0 app/build/outputs/apk/release/app-release.apk \
  --title "v1.3.0 — <qısa başlıq>" --notes-file qeydler.md
```

### Qeydlərin şablonu

```markdown
## v1.3.0 — <qısa başlıq>

### Yeni
- …

### Düzəlişlər
- …

### Texniki
- …

### Bilinən məhdudiyyətlər
- Release-dən əvvəl də olan və ayrıca taskda planlaşdırılan problemlər (RELEASE-GATE qaydası).
```

Nümunə:

```markdown
## v1.1.0 — Türkçe və Русский

### Yeni
- Türk və rus dilləri (UI + 32 dərs + 1250 çalışma + test)

### Düzəlişlər
- Axtarışda ərəb hərəkələri nəticəyə təsir etmir

### Texniki
- APK: 3.4 MB (büdcə 8 MB)
```

## 5. Sonra

- README-dəki dil cədvəli və screenshot-lar yenidir.
- Google Play-ə yükləmə olacaqsa: «Data safety» bəyanatı
  [`PRIVACY.md`](../PRIVACY.md) ilə eynidir — yalnız icra olunan kod
  JetBrains serverinə gedir, başqa data toplanmır.
