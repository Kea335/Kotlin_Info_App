# KotlinAZ — inkişaf taskları

Bu qovluqdakı hər fayl bir taskdır: bir branch, bir PR, bir «done» meyarı.
Tasklar `acab102` commit-indəki kodun tam auditindən çıxarılıb.

Hər task faylında: problem (kodda sübutu ilə), dəyişən fayllar, addımlar,
qəbul kriteriyası və test qeydləri var. Task-ı olduğu kimi götürüb işə
başlamaq mümkündür — əlavə konteks tələb etmir.

---

## Sıra

| Sprint | Tasklar | Fokus | Təxmin |
|---|---|---|---|
| **0 — Cəld düzəlişlər** | T0.1 – T0.4 | Sübut olunmuş buglar, repo təmizliyi | ≈ 2,5 gün |
| **1 — Test və CI** | T1.1 – T1.4 | Mövcud davranışı qıfılla | ≈ 4 gün |
| **2 — Arxitektura və state** | T2.1 – T2.7 | ViewModel ayrımı, process death | ≈ 7,5 gün |
| **3 — Localization mühərriki** | T3.1 – T3.7 | 15 dil texniki olaraq işləyir | ≈ 9 gün |
| **4 — Tərcümə dalğaları** | T4.0 – T4.7 | Məzmunun özü, dalğa-dalğa | 9–12 həftə |
| **5 — Funksiyalar və portfolio** | T5.1 – T5.7 | Room, səhvlərim, statistika, README | ≈ 12,5 gün |

Sprint 0–3 tərcümədən əvvəlki mühəndislik işidir: ≈ 23 iş günü.
Sprint 5 tərcümə dalğaları ilə paralel gedə bilər — məzmuna toxunmur.

## İlk həftə

```
T0.1 → T0.2 → T0.3 → T1.1 → T1.3
```

Beş kiçik PR: iki bug bağlanır, testlər yaranır, CI işə düşür.
Bundan sonra hər refaktor yaşıl testlə gəlir.

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
```

## Prioritetlər

| Nişan | Mənası |
|---|---|
| **P0** | Release-dən və növbəti refaktordan əvvəl edilməlidir |
| **P1** | Arxitektura və davamlı inkişaf üçün yüksək dəyər |
| **P2** | Keyfiyyət, təqdimat, rahatlıq |


## Fayllar

| Task | Fayl |
|---|---|
| T0.1 — Axtarışda İ/ı düzəlişi | [`T0.1-axtaris-locale.md`](T0.1-axtaris-locale.md) |
| T0.2 — Kompilyator versiyasını real siyahıya bağla | [`T0.2-kompilyator-versiyasi.md`](T0.2-kompilyator-versiyasi.md) |
| T0.3 — Repo təmizliyi və lisenziya | [`T0.3-repo-temizliyi.md`](T0.3-repo-temizliyi.md) |
| T0.4 — Başlanğıc kodu həllə bərabər olan 16 çalışma | [`T0.4-trivial-calismalar.md`](T0.4-trivial-calismalar.md) |
| T1.1 — Test infrastrukturu və biznes məntiqi testləri | [`T1.1-test-bazasi.md`](T1.1-test-bazasi.md) |
| T1.2 — Aktivlərin smoke testləri | [`T1.2-aktiv-testleri.md`](T1.2-aktiv-testleri.md) |
| T1.3 — GitHub Actions CI | [`T1.3-github-actions.md`](T1.3-github-actions.md) |
| T1.4 — Lint baseline | [`T1.4-lint-baseline.md`](T1.4-lint-baseline.md) |
| T2.1 — AppContainer və Application sinfi | [`T2.1-appcontainer.md`](T2.1-appcontainer.md) |
| T2.2 — CompilerRepository və icra ViewModel-ləri | [`T2.2-compiler-repository.md`](T2.2-compiler-repository.md) |
| T2.3 — QuizViewModel və turun qorunması | [`T2.3-quiz-viewmodel.md`](T2.3-quiz-viewmodel.md) |
| T2.4 — Qalan UI state-in bərpası və configChanges | [`T2.4-process-death.md`](T2.4-process-death.md) |
| T2.5 — AppViewModel-i feature ViewModel-lərə böl | [`T2.5-viewmodel-bolgusu.md`](T2.5-viewmodel-bolgusu.md) |
| T2.6 — Başlanğıc sürəti | [`T2.6-baslangic-sureti.md`](T2.6-baslangic-sureti.md) |
| T2.7 — Tip-təhlükəsiz naviqasiya | [`T2.7-tip-tehlukesiz-naviqasiya.md`](T2.7-tip-tehlukesiz-naviqasiya.md) |
| T3.1 — Sabit blok ID-ləri | [`T3.1-sabit-blok-idleri.md`](T3.1-sabit-blok-idleri.md) |
| T3.2 — Çoxdilli məzmun boru xətti | [`T3.2-coxdilli-boru-xetti.md`](T3.2-coxdilli-boru-xetti.md) |
| T3.3 — Dil modeli və seçim mexanizmi | [`T3.3-dil-secimi.md`](T3.3-dil-secimi.md) |
| T3.4 — Bütün hardcoded mətnləri resources-a çıxar | [`T3.4-hardcoded-metnler.md`](T3.4-hardcoded-metnler.md) |
| T3.5 — Runtime axtarış indeksi və dil üzrə normallaşdırma | [`T3.5-axtaris-indeksi.md`](T3.5-axtaris-indeksi.md) |
| T3.6 — RTL dəstəyi (ərəb və fars) | [`T3.6-rtl.md`](T3.6-rtl.md) |
| T3.7 — APK və yaddaş büdcəsi | [`T3.7-apk-budcesi.md`](T3.7-apk-budcesi.md) |
| T4.0 — Lüğət və tərcümə siyasəti | [`T4.0-terceume-siyaseti.md`](T4.0-terceume-siyaseti.md) |
| T4.1 — Dalğa 1: ingilis dili (pivot) | [`T4.1-dalga1-en.md`](T4.1-dalga1-en.md) |
| T4.2 – T4.5 — Qalan tərcümə dalğaları | [`T4.2-T4.5-qalan-dalgalar.md`](T4.2-T4.5-qalan-dalgalar.md) |
| T4.6 — Kod nümunələrinin kompilyasiya yoxlaması | [`T4.6-kod-kompilyasiya-yoxlamasi.md`](T4.6-kod-kompilyasiya-yoxlamasi.md) |
| T4.7 — Dil üzrə release qapısı | [`T4.7-dil-release-qapisi.md`](T4.7-dil-release-qapisi.md) |
| T5.1 — Yaddaş qatı: Room | [`T5.1-room.md`](T5.1-room.md) |
| T5.2 — «Səhvlərim» bölməsi | [`T5.2-sehvlerim.md`](T5.2-sehvlerim.md) |
| T5.3 — Bookmark və şəxsi qeydlər | [`T5.3-bookmark-qeyd.md`](T5.3-bookmark-qeyd.md) |
| T5.4 — Statistika | [`T5.4-statistika.md`](T5.4-statistika.md) |
| T5.5 — Learning Path | [`T5.5-learning-path.md`](T5.5-learning-path.md) |
| T5.6 — Əlçatanlıq | [`T5.6-elcatanliq.md`](T5.6-elcatanliq.md) |
| T5.7 — README, release axını və portfolio təqdimatı | [`T5.7-readme-release.md`](T5.7-readme-release.md) |

Release qapısı: [`RELEASE-GATE.md`](RELEASE-GATE.md)

## Tam audit

Tapıntıların izahı, GPT sənədinə münasibət və vaxt qrafiki:
[KotlinAZ Yol Xəritəsi](https://claude.ai/code/artifact/e1856bdf-e7b2-41d5-a0cc-f2f833a4a1f7)
