package az.kotlinaz.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import az.kotlinaz.app.R
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.ui.exercises.Rejim
import az.kotlinaz.app.ui.quiz.QuizRejimi

/* ============================================================
   Enum-ların ekrandakı adları (T3.4). Data qatı resursdan asılı
   deyil — adlar burada, `ui/` qatında resursa bağlanır, data
   testləri Android resursu olmadan işləyir.
   ============================================================ */

@get:StringRes
val Level.etiketRes: Int
    get() = when (this) {
        Level.JUNIOR -> R.string.seviyye_junior
        Level.MIDDLE -> R.string.seviyye_middle
        Level.SENIOR -> R.string.seviyye_senior
    }

@Composable
fun Level.etiket(): String = stringResource(etiketRes)

@Composable
fun ThemeMode.etiket(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.tema_sistem
        ThemeMode.LIGHT -> R.string.tema_isiqli
        ThemeMode.DARK -> R.string.tema_qaranliq
    }
)

@Composable
fun QuizRejimi.etiket(): String = when (this) {
    QuizRejimi.QARISIQ -> stringResource(R.string.quiz_rejim_qarisiq)
    QuizRejimi.SEHVLER -> stringResource(R.string.quiz_rejim_sehvler)
    else -> checkNotNull(seviyye).etiket()
}

@Composable
fun QuizRejimi.izah(): String = stringResource(
    when (this) {
        QuizRejimi.JUNIOR -> R.string.quiz_rejim_junior_izah
        QuizRejimi.MIDDLE -> R.string.quiz_rejim_middle_izah
        QuizRejimi.SENIOR -> R.string.quiz_rejim_senior_izah
        QuizRejimi.QARISIQ -> R.string.quiz_rejim_qarisiq_izah
        QuizRejimi.SEHVLER -> R.string.quiz_rejim_sehvler_izah
    }
)

@Composable
fun Rejim.etiket(): String = stringResource(
    when (this) {
        Rejim.NEZERI -> R.string.calisma_rejim_nezeri
        Rejim.PRAKTIKI -> R.string.calisma_rejim_praktiki
    }
)

/* ---------- Kod icrasının xətaları (meydan və çalışma) ---------- */

/** Xətanın növü — panel başlığı resursdan gəlir. */
enum class IcraXetasi(@param:StringRes val basliqRes: Int) {
    KOMPILYASIYA(R.string.icra_xeta_kompilyasiya),
    ICRA(R.string.icra_xeta_calisma_vaxti),
    BAGLANTI(R.string.icra_xeta_baglanti)
}

/** Xəta panelinin bir sətri; kompilyasiya xətasında sətir nömrəsi də var. */
data class XetaSetri(val metn: String, val setir: Int? = null)

/** Boş olmayan mətn → bir sətir; boş → heç nə. */
internal fun xetaSetrleri(metn: String): List<XetaSetri> =
    if (metn.isBlank()) emptyList() else listOf(XetaSetri(metn))

/** Panel mətni: «Sətir N: …» prefiksi tətbiqin dilindədir. */
@Composable
fun xetaMetni(setirler: List<XetaSetri>): String = buildString {
    setirler.forEachIndexed { i, s ->
        if (i > 0) append('\n')
        append(if (s.setir != null) stringResource(R.string.icra_setir, s.setir, s.metn) else s.metn)
    }
}
