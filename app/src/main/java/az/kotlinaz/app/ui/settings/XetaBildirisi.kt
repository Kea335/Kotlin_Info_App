package az.kotlinaz.app.ui.settings

import java.net.URLEncoder

/* ============================================================
   «Xəta bildir» və məxfilik siyasəti (Mərhələ 5).

   Heç nə avtomatik göndərilmir: düymə brauzerdə GitHub-un yeni issue
   formunu açır, cihaz və versiya məlumatı gövdəyə yazılmış olur —
   istifadəçi hamısını görür, dəyişə və ya silə bilər, göndərməni özü
   edir. Tətbiqdə analitika və çökmə hesabatı SDK-sı yoxdur.
   ============================================================ */

/** Layihənin repozitoriyası — issue-lar və məxfilik siyasəti buradadır. */
internal const val REPO_URL = "https://github.com/Kea335/Kotlin_Info_App"

/** PRIVACY.md — Play Console-un «Privacy policy» sahəsinə də bu ünvan yazılır. */
internal const val MEXFILIK_URL = "$REPO_URL/blob/main/PRIVACY.md"

/** Hesabata yazılan cihaz məlumatı — şəxsi məlumat yoxdur. */
data class CihazMelumati(
    /** `1.0.0 (1)` — versionName (versionCode). */
    val tetbiq: String,
    /** `16 (API 37)`. */
    val android: String,
    /** İstehsalçı və model: `Google Pixel 9a`. */
    val cihaz: String,
    /** Tətbiq dili və məzmunun faktiki dili: `sistem → az`. */
    val dil: String,
    /** Kod icrasındakı Kotlin versiyası. */
    val kompilyator: String
)

/** GitHub-un yeni issue formunun ünvanı, başlıq və gövdə doldurulmuş. */
internal fun xetaBildirisiUrl(m: CihazMelumati): String {
    val govde = buildString {
        appendLine("**Nə baş verdi?**")
        appendLine()
        appendLine()
        appendLine("**Necə təkrarlamaq olar?**")
        appendLine("1. ")
        appendLine()
        appendLine("**Nə gözləyirdin?**")
        appendLine()
        appendLine()
        appendLine("---")
        appendLine("| | |")
        appendLine("|---|---|")
        appendLine("| Tətbiq | ${m.tetbiq} |")
        appendLine("| Android | ${m.android} |")
        appendLine("| Cihaz | ${m.cihaz} |")
        appendLine("| Dil | ${m.dil} |")
        append("| Kompilyator | Kotlin ${m.kompilyator} |")
    }
    return "$REPO_URL/issues/new?title=${kodla("[Xəta] ")}&body=${kodla(govde)}"
}

/** URL sorğu parametri: boşluq `%20` (GitHub `+`-ı başlıqda olduğu kimi saxlayır). */
private fun kodla(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

/** İstehsalçı modelin əvvəlindədirsə təkrarlanmır: `Google` + `Pixel 9a`, amma `samsung SM-S918B`. */
internal fun cihazAdi(istehsalci: String, model: String): String =
    if (model.startsWith(istehsalci, ignoreCase = true)) model else "$istehsalci $model"
