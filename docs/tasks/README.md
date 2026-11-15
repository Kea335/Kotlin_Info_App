# KotlinAZ — yol xəritəsi

Bu qovluq planın özüdür. Hər fayl bir taskdır: bir branch, bir PR, bir «done»
meyarı. Task faylında problem (kodda sübutu ilə), dəyişən fayllar, addımlar,
qəbul kriteriyası və tətbiq qeydi var — task-ı olduğu kimi götürüb işə başlamaq
mümkündür.

İlk plan `acab102` commit-inin auditindən (2026-09-13) çıxıb. Bu səhifə
**2026-11-16 baxışından** sonrakı vəziyyəti və növbəti sıranı göstərir.

---

## Vəziyyət (2026-11-16)

| Sprint | Tasklar | Kod | Açıq qalan |
|---|---|---|---|
| **0 — Cəld düzəlişlər** | T0.1 – T0.4 | ✅ bitib | — |
| **1 — Test və CI** | T1.1 – T1.4 | ✅ bitib | `main` branch protection (sahibin işi) |
| **2 — Arxitektura və state** | T2.1 – T2.7 | ✅ bitib | cihazda yoxlama: T2.2, T2.3, T2.4, T2.5, T2.6 (10 bənd) |
| **3 — Localization mühərriki** | T3.1 – T3.7 | ✅ bitib | cihazda yoxlama: T3.3 (1 bənd), T3.6 RTL (5 bənd) |
| **4 — Tərcümə dalğaları** | T4.0 – T4.7 | 4 / 15 dil | `es` 64 %; 10 dil başlanmayıb; QA imzaları; heç bir release yoxdur |
| **5 — Funksiyalar və portfolio** | T5.1 – T5.7 | ✅ bitib | T5.7: screenshot-lar |
| **6 — Keyfiyyət və böyümə** | T6.1 – T6.10 | yeni | hamısı (bax aşağıda) |

Rəqəmlər: 311 unit test + 7 cihaz testi · release-də 4 dil (`az`, `en`, `ru`,
`tr`) · `es` debug-da · versiya hələ `1.0.0`, teq və GitHub Release yoxdur.

Plandan kənar görülənlər (ilk planda yox idi, `main`-dədir): CI-da imzasız
release build + R8 serializator yoxlaması, kompilyator cavab parserinin
testləri, Dependabot; kod meydanında real «Dayandır», kodun diskdə qalması və
paylaşma; `PRIVACY.md`, «Xəta bildir», backup qaydaları.

### Baxışın tapıntıları

1. **Heç nə buraxılmayıb.** Plan «hər dalğa ayrıca release» deyir, amma `en`,
   `tr`, `ru` 100 %-dir və istifadəçiyə çatmayıb. Ən böyük borc budur — dəyər
   repo-da dayanır. → Mərhələ B.
2. **`main` CI qırmızı idi.** README-dəki dil cədvəli köhnəlmişdi (`es` 10 %
   yazılıb, əslində 64 %). Kök səbəb: cədvəl əl ilə yenilənirdi. İndi
   `build-locale.js` onu özü yeniləyir. Eyni səbəbdən 5 Dependabot PR-ı da
   qırmızıdır (#34 – #38).
3. **Üst-üstə yığılmış PR-lar işləmədi.** 30 PR (#3 – #32) bir-birinin üstündə
   iki ay gözlədi və sonda tək birləşmə ilə (#33) `main`-ə düşdü. Ayrı-ayrı
   baxış və geri qaytarma imkanı itdi. → «Proses» bölməsi.
4. **Cihaz yoxlamaları borcu.** Sprint 2 və 3-ün 16 bəndi «cihazda baxılmayıb»
   kimi açıqdır (process death, döndərmə, RTL ekranları). Kod və unit testlər
   düzgündür, amma gözlə təsdiq yoxdur — release qapısının «Davranış» bölməsi
   məhz bunları tələb edir. → Mərhələ B, sonra T6.1 ilə avtomatlaşdırılır.
5. **Tərcümə təxmini çox bədbin idi, QA təxmini çox nikbin.** Plan dalğalara
   9 – 12 həftə ayırmışdı; faktiki sürət dil başına ≈ 1 – 1,5 gündür. Darboğaz
   tərcümə deyil, **ikinci oxucudur**: 14 dil üçün ana dili daşıyıcısı tapmaq
   tək developer üçün real deyil. → T6.10 və «Qərar gözləyənlər».
6. **Sayt dəyişəndə dil release-dən düşə bilər.** Dil qapısı 100 % tələb edir;
   bazada bir paraqraf dəyişsə bütün tərcüməli dillər 100 %-dən aşağı düşür və
   növbəti build-də gizlənir. → «Risklər».

---

## Növbəti sıra

```
A  Sabitləşdirmə   →   B  İlk release (v1.2.0)   →   C  Tərcümə dalğaları
                                                       ↕ paralel
                                                     D  Sprint 6
```

### A — Sabitləşdirmə

| | İş | Kim |
|---|---|---|
| A1 | `main` yaşıl: README dil cədvəli + `build-locale.js` onu özü yeniləyir | ✅ bu baxışla |
| A2 | Dependabot PR-ları (#34 – #38): `main` yaşıl olandan sonra rebase; yaşıl olan merge olunur. #35 (Kotlin qrupu) cihaz testində də düşüb — ayrıca baxılmalıdır | Claude hazırlayır, sahib merge edir |
| A3 | `main` branch protection: merge yalnız yaşıl `build` + `cihaz-testleri` ilə (T1.3-ün son bəndi) | sahib |

### B — İlk release: v1.2.0

`en` + `tr` + `ru` + Sprint 5 funksiyaları bir release-də. v1.1 (yalnız `en`)
ayrıca buraxılmadı — dalğa 1 və 2 birlikdə v1.2.0 olur, sonrakı nömrələr
planda yazıldığı kimi qalır (v1.3 = dalğa 3 …).

| | İş | Kim |
|---|---|---|
| B1 | Cihaz yoxlamaları (emulyator): T2.2 – T2.6, T3.3, T3.6 açıq bəndləri; nəticə task fayllarına yazılır | Claude |
| B2 | Screenshot-lar: 5 ekran × işıqlı/qaranlıq → `docs/screenshots/`, README cərgəsi (T5.7) | Claude |
| B3 | [`RELEASE-GATE.md`](RELEASE-GATE.md) siyahısı maddə-maddə; keçməyən maddə ya düzəlir, ya release notes-da «Bilinən məhdudiyyətlər»ə yazılır | Claude |
| B4 | Versiya `1.2.0`, teq, imzalı APK, GitHub Release ([`docs/RELEASE.md`](../RELEASE.md)) | sahibin təsdiqi ilə |

### C — Tərcümə dalğaları (davam)

| Dalğa | Dillər | Vəziyyət | Release |
|---|---|---|---|
| 1 | `en` | ✅ 100 % | v1.2.0 |
| 2 | `tr`, `ru` | ✅ 100 % | v1.2.0 |
| 3 | `es`, `pt-BR`, `de`, `fr` | `es` 64 % (çalışmalar 13 / 25 mövzu); qalan 3 dil başlanmayıb | v1.3.0 |
| 4 | `ar`, `fa` | başlanmayıb; əvvəl T3.6-nın cihaz yoxlaması (B1) | v1.4.0 |
| 5 | `hi`, `id`, `zh-CN`, `ja`, `ko` | başlanmayıb | v1.5.0 — 15 / 15 |

Təfsilat: [`T4.2-T4.5-qalan-dalgalar.md`](T4.2-T4.5-qalan-dalgalar.md).
Qayda dəyişmir: **bir vaxtda bir dil**, dalğa bitməmiş növbətisi başlanmır.

### D — Sprint 6: keyfiyyət və böyümə

Tərcümə ilə paralel gedə bilər — məzmuna toxunmur.
Təfsilat: [`T6.1-T6.10-keyfiyyet-ve-boyume.md`](T6.1-T6.10-keyfiyyet-ve-boyume.md).

| Task | Nə | Prioritet | Təxmin |
|---|---|---|---|
| T6.1 | Compose UI və naviqasiya testləri (cihaz yoxlamalarını avtomatlaşdırır) | P1 | 2 gün |
| T6.10 | «Tərcümə səhvi bildir» — blok səviyyəsində, dil və ID ilə | P1 | 0,5 gün |
| T6.2 | Baseline Profile (soyuq başlanğıc) | P1 | 1 gün |
| T6.3 | AAB və Play Store materialları | P1 * | 1 gün |
| T6.4 | Gündəlik xatırlatma və seriya bildirişi | P2 | 1,5 gün |
| T6.5 | Aralıqlı təkrar («Səhvlərim»in üstündə) | P2 | 2 gün |
| T6.6 | Tərəqqinin ixracı / idxalı | P2 | 1 gün |
| T6.7 | Səviyyə testi (Junior-u atlamaq) | P2 | 1,5 gün |
| T6.8 | Planşet və qatlanan cihaz üçün adaptiv layout | P2 | 2 gün |
| T6.9 | App shortcut və deep link | P2 | 0,5 gün |

\* T6.3 yalnız Play-də yayım qərarı verilsə (bax «Qərar gözləyənlər»).

---

## Qərar gözləyənlər (sahib)

| | Sual | Təklif |
|---|---|---|
| Q1 | İkinci oxucu imzası olmadan dil buraxılsınmı? `en`, `tr`, `ru` avtomatik yoxlamalardan keçib, imza yoxdur | Bəli: release notes-da açıq yazılsın, T6.10 ilə istifadəçi səhvi bir toxunuşla bildirsin |
| Q2 | İlk release nömrəsi | `v1.2.0` (dalğa 1 + 2) |
| Q3 | Google Play-də yayım olacaqmı? | Qərar T6.3-ü açır; yoxdursa GitHub Release kifayətdir |
| Q4 | `main` branch protection | Açılsın — indiki proses (PR + yaşıl CI) onsuz da buna uyğundur |

## Proses

- **Branch `main`-dən açılır, PR `main`-ə gedir.** PR-lar bir-birinin üstünə
  yığılmır. Asılı iş varsa əvvəlki PR merge olunana qədər gözlənilir, ya da
  eyni PR-da ayrı commit olur.
- **`main` həmişə yaşıldır.** Qırmızı `main` hər şeydən əvvəl düzəldilir —
  onun üstündən açılan hər PR da qırmızı olur.
- **Tərcümə commit-i:** `build-locale.js <dil>` → `validate-locales.js <dil>`
  → commit. README cədvəli birinci addımda özü yenilənir.
- **Dil bitəndə:** `check-code.js <dil>` → `localization/qa-<dil>.md` → PR.
- **Həftəlik:** Dependabot PR-ları; gecəlik «Kod yoxlaması» workflow-unun
  açdığı issue-lar.
- **Task bitəndə** faylındakı qəbul bəndləri işarələnir və «Tətbiq qeydi»
  yazılır — bu səhifədəki cədvəl oradan yenilənir.

## Risklər

| Risk | Təsir | Tədbir |
|---|---|---|
| Sayt (baza) dəyişir | Tərcüməli dillər 100 %-dən düşür, release-də gizlənir | Release-dən əvvəl `build-content.js` işlədilmir («məzmun dondurması»); dəyişiklik gələndə fərq əvvəlcə bütün tam dillərə tərcümə olunur, sonra commit |
| İkinci oxucu yoxdur | Tərcümə səhvləri istifadəçidə üzə çıxır | T6.10; hər dil üçün `qa-<dil>.md`-də nümunə yoxlaması; lüğət (`glossary.json`) validatorda |
| JetBrains kompilyator API-si dəyişir | «Yoxla» və meydan dayanır | Gecəlik «Kod yoxlaması» (T4.6) ilk gündən xəbər verir; versiya siyahısı runtime-da oxunur (T0.2) |
| APK ölçüsü 15 dildə | 8 MB büdcə | `apk-budcesi.js` CI-da proqnoz yazır (T3.7); yaddaşa yalnız seçilmiş dil yüklənir |
| RTL yalnız kod auditindən keçib | `ar` / `fa` dalğasında gec tapılan layout səhvləri | B1-də debug build-də RTL ekran yoxlaması — dalğa 4-dən **əvvəl** |

---

## Prioritetlər

| Nişan | Mənası |
|---|---|
| **P0** | Release-dən və növbəti refaktordan əvvəl edilməlidir |
| **P1** | Arxitektura və davamlı inkişaf üçün yüksək dəyər |
| **P2** | Keyfiyyət, təqdimat, rahatlıq |

## Asılılıqlar

```
T0.1 ─┐
T0.2  ├─→ T1.1 ─→ T1.3 ─→ (bütün qalanlar)
T0.3 ─┘           │
T0.4 ─────────────┘

T2.1 ─→ T2.2 ─→ T2.3 ─→ T2.4
  └───→ T2.5
T3.1 ─→ T3.2 ─→ T3.3 ─→ T3.4 ─→ T3.5 ─→ T3.6
T4.0 ─→ T4.1 (en) ─→ T4.2 (tr, ru) ─→ T4.3 ─→ T4.4 ─→ T4.5
T5.1 ─→ T5.2, T5.3, T5.4

T3.6 (cihaz yoxlaması) ─→ T4.4 (ar, fa)
T5.2 ─→ T6.5        T5.4 ─→ T6.4        T5.1 ─→ T6.6
B1 (əl ilə yoxlama) ─→ T6.1 (eyni ssenarilər testdə)
```

## Fayllar

| Task | Fayl | Vəziyyət |
|---|---|---|
| T0.1 — Axtarışda İ/ı düzəlişi | [`T0.1-axtaris-locale.md`](T0.1-axtaris-locale.md) | ✅ |
| T0.2 — Kompilyator versiyasını real siyahıya bağla | [`T0.2-kompilyator-versiyasi.md`](T0.2-kompilyator-versiyasi.md) | ✅ |
| T0.3 — Repo təmizliyi və lisenziya | [`T0.3-repo-temizliyi.md`](T0.3-repo-temizliyi.md) | ✅ |
| T0.4 — Başlanğıc kodu həllə bərabər olan 16 çalışma | [`T0.4-trivial-calismalar.md`](T0.4-trivial-calismalar.md) | ✅ |
| T1.1 — Test infrastrukturu və biznes məntiqi testləri | [`T1.1-test-bazasi.md`](T1.1-test-bazasi.md) | ✅ |
| T1.2 — Aktivlərin smoke testləri | [`T1.2-aktiv-testleri.md`](T1.2-aktiv-testleri.md) | ✅ |
| T1.3 — GitHub Actions CI | [`T1.3-github-actions.md`](T1.3-github-actions.md) | ✅ · branch protection açıq |
| T1.4 — Lint baseline | [`T1.4-lint-baseline.md`](T1.4-lint-baseline.md) | ✅ |
| T2.1 — AppContainer və Application sinfi | [`T2.1-appcontainer.md`](T2.1-appcontainer.md) | ✅ |
| T2.2 — CompilerRepository və icra ViewModel-ləri | [`T2.2-compiler-repository.md`](T2.2-compiler-repository.md) | ✅ · cihaz 1 |
| T2.3 — QuizViewModel və turun qorunması | [`T2.3-quiz-viewmodel.md`](T2.3-quiz-viewmodel.md) | ✅ · cihaz 3 |
| T2.4 — Qalan UI state-in bərpası və configChanges | [`T2.4-process-death.md`](T2.4-process-death.md) | ✅ · cihaz 4 |
| T2.5 — AppViewModel-i feature ViewModel-lərə böl | [`T2.5-viewmodel-bolgusu.md`](T2.5-viewmodel-bolgusu.md) | ✅ · cihaz 1 |
| T2.6 — Başlanğıc sürəti | [`T2.6-baslangic-sureti.md`](T2.6-baslangic-sureti.md) | ✅ · ölçmə 1 |
| T2.7 — Tip-təhlükəsiz naviqasiya | [`T2.7-tip-tehlukesiz-naviqasiya.md`](T2.7-tip-tehlukesiz-naviqasiya.md) | ✅ |
| T3.1 — Sabit blok ID-ləri | [`T3.1-sabit-blok-idleri.md`](T3.1-sabit-blok-idleri.md) | ✅ |
| T3.2 — Çoxdilli məzmun boru xətti | [`T3.2-coxdilli-boru-xetti.md`](T3.2-coxdilli-boru-xetti.md) | ✅ |
| T3.3 — Dil modeli və seçim mexanizmi | [`T3.3-dil-secimi.md`](T3.3-dil-secimi.md) | ✅ · cihaz 1 |
| T3.4 — Bütün hardcoded mətnləri resources-a çıxar | [`T3.4-hardcoded-metnler.md`](T3.4-hardcoded-metnler.md) | ✅ |
| T3.5 — Runtime axtarış indeksi və dil üzrə normallaşdırma | [`T3.5-axtaris-indeksi.md`](T3.5-axtaris-indeksi.md) | ✅ |
| T3.6 — RTL dəstəyi (ərəb və fars) | [`T3.6-rtl.md`](T3.6-rtl.md) | ✅ kod · cihaz 5 |
| T3.7 — APK və yaddaş büdcəsi | [`T3.7-apk-budcesi.md`](T3.7-apk-budcesi.md) | ✅ |
| T4.0 — Lüğət və tərcümə siyasəti | [`T4.0-terceume-siyaseti.md`](T4.0-terceume-siyaseti.md) | ✅ |
| T4.1 — Dalğa 1: ingilis dili (pivot) | [`T4.1-dalga1-en.md`](T4.1-dalga1-en.md) | ✅ 100 % · QA imzası, release |
| T4.2 – T4.5 — Qalan tərcümə dalğaları | [`T4.2-T4.5-qalan-dalgalar.md`](T4.2-T4.5-qalan-dalgalar.md) | `tr`, `ru` ✅ · `es` 64 % · 10 dil qalıb |
| T4.6 — Kod nümunələrinin kompilyasiya yoxlaması | [`T4.6-kod-kompilyasiya-yoxlamasi.md`](T4.6-kod-kompilyasiya-yoxlamasi.md) | ✅ |
| T4.7 — Dil üzrə release qapısı | [`T4.7-dil-release-qapisi.md`](T4.7-dil-release-qapisi.md) | ✅ |
| T5.1 — Yaddaş qatı: Room | [`T5.1-room.md`](T5.1-room.md) | ✅ |
| T5.2 — «Səhvlərim» bölməsi | [`T5.2-sehvlerim.md`](T5.2-sehvlerim.md) | ✅ |
| T5.3 — Bookmark və şəxsi qeydlər | [`T5.3-bookmark-qeyd.md`](T5.3-bookmark-qeyd.md) | ✅ |
| T5.4 — Statistika | [`T5.4-statistika.md`](T5.4-statistika.md) | ✅ |
| T5.5 — Learning Path | [`T5.5-learning-path.md`](T5.5-learning-path.md) | ✅ |
| T5.6 — Əlçatanlıq | [`T5.6-elcatanliq.md`](T5.6-elcatanliq.md) | ✅ |
| T5.7 — README, release axını və portfolio təqdimatı | [`T5.7-readme-release.md`](T5.7-readme-release.md) | ✅ · screenshot-lar açıq |
| T6.1 – T6.10 — Keyfiyyət və böyümə | [`T6.1-T6.10-keyfiyyet-ve-boyume.md`](T6.1-T6.10-keyfiyyet-ve-boyume.md) | yeni |

«cihaz N» — task-ın kodu `main`-dədir, N qəbul bəndi emulyatorda gözlə
təsdiqlənməyib (Mərhələ B1).

Release qapısı: [`RELEASE-GATE.md`](RELEASE-GATE.md) · Release axını:
[`docs/RELEASE.md`](../RELEASE.md)

Kopyalanmaq üçün hazır promptlar: [`PROMPTS.md`](PROMPTS.md) — Sprint 0 – 5
taskları üçün, yeni sessiyaya yapışdırılmaq üçün.

## İlk audit

Sprint 0 – 5-in əsaslandığı tapıntılar, GPT sənədinə münasibət və ilkin vaxt
qrafiki (2026-09-13 vəziyyəti):
[KotlinAZ Yol Xəritəsi](https://claude.ai/code/artifact/e1856bdf-e7b2-41d5-a0cc-f2f833a4a1f7)
