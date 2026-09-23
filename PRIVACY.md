# KotlinAZ — Məxfilik siyasəti

Son yenilənmə: 23 sentyabr 2026

KotlinAZ Kotlin dilini öyrədən oflayn tətbiqdir. Hesab, reklam, analitika,
çökmə hesabatı və izləmə **yoxdur**. Tətbiq heç bir şəxsi məlumat toplamır.

## Cihazdan nə çıxır

**Yalnız kod icrası.** Kod meydanında və praktiki çalışmalarda «İşlə» və ya
«Yoxla» düyməsini basanda redaktordakı kod JetBrains-in rəsmi Kotlin
kompilyator xidmətinə (`api.kotlinlang.org`) göndərilir, orada kompilyasiya
və icra olunur, nəticə geri qayıdır. Sorğuda kodun özündən başqa heç nə
yoxdur — ad, cihaz identifikatoru, yer və s. göndərilmir. Bu, saytdakı
Kotlin Playground-un işlətdiyi eyni xidmətdir; JetBrains-in öz məxfilik
siyasəti tətbiq olunur: <https://www.jetbrains.com/legal/docs/privacy/privacy/>.

Kompilyatorun versiya siyahısı da (`api.kotlinlang.org/versions`) ilk icradan
əvvəl, gündə ən çox bir dəfə oxunur — bu sorğuda istifadəçi məlumatı yoxdur.

Kod göndərmədən öyrənmək mümkündür: dərslər, kod nümunələri, nəzəri
çalışmalar və bilik testi internetsiz işləyir.

## Cihazda nə saxlanılır

- Tərəqqi: oxunmuş bölmələr, həll edilmiş çalışmalar, test rekordları
- Tənzimləmələr: tema, dil, yazı ölçüsü, kod tamamlama
- Kod meydanındakı son kod
- Kompilyator versiyalarının keşi

Bunlar tətbiqin öz qovluğundadır və başqa tətbiqlərə açıq deyil. Tənzimləmələr
ekranından tərəqqini sıfırlamaq, tətbiqi silməklə isə hamısını silmək olar.

**Android yedəyi.** Cihazda yedəkləmə açıqdırsa, Android tərəqqini,
tənzimləmələri və meydanın kodunu Google hesabınızın yedəyinə yaza bilər
(Android-in öz mexanizmidir; KotlinAZ bu məlumatı görmür). Kompilyator keşi
yedəklənmir.

## «Xəta bildir»

Tənzimləmələrdəki «Xəta bildir» düyməsi brauzerdə GitHub-un yeni issue
formunu açır. Formaya tətbiq versiyası, Android versiyası, cihaz modeli,
dil və kompilyator versiyası yazılmış olur. Heç nə avtomatik göndərilmir —
formanı görüb dəyişə, silə və ya göndərməyə bilərsiniz. Göndərilən issue
GitHub-da açıq görünür və GitHub-un məxfilik siyasətinə tabedir.

## Uşaqlar

Tətbiq heç bir yaş qrupundan məlumat toplamır.

## Dəyişikliklər və əlaqə

Siyasət dəyişsə bu fayl yenilənir; tarix yuxarıdadır. Suallar üçün:
<https://github.com/Kea335/Kotlin_Info_App/issues>

---

## English summary

KotlinAZ is an offline app for learning Kotlin. It has no accounts, ads,
analytics, crash reporting or tracking, and collects no personal data.

- **Leaves the device:** only the code you run. Pressing "Run" in the
  playground or "Check" in a practice exercise sends that code — and nothing
  else — to JetBrains' Kotlin compiler service (`api.kotlinlang.org`), the
  same service used by the Kotlin Playground. JetBrains' privacy policy
  applies. The list of compiler versions is fetched at most once a day,
  without any user data.
- **Stays on the device:** progress, settings, the playground's last code and
  a compiler-version cache. Android backup may include progress, settings and
  the playground code (not the cache).
- **Bug reports:** "Report a bug" opens a pre-filled GitHub issue form (app
  version, Android version, device model, language, compiler version) in the
  browser. Nothing is sent unless you submit it yourself.

Contact: <https://github.com/Kea335/Kotlin_Info_App/issues>
