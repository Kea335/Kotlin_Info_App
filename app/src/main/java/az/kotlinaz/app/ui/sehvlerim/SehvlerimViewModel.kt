package az.kotlinaz.app.ui.sehvlerim

import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.QuizQuestion
import az.kotlinaz.app.data.oyrenme.OyrenmeRepository
import az.kotlinaz.app.data.oyrenme.SehvCavab
import az.kotlinaz.app.ui.quiz.QuizRejimi
import az.kotlinaz.app.ui.quiz.bankSualId
import az.kotlinaz.app.ui.quiz.hovuzlariQur
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** «Səhvlərim» siyahısının bir sətri. */
data class SehvSetri(
    val sual: QuizQuestion,
    /** Çalışma bankının mövzusu; quiz.json sualında null (UI «Bilik testi» yazır). */
    val movzu: String?,
    val say: Int,
    val sonTarix: Long
)

/** Sualın mətni və mövzusu — seçilmiş dilin məzmunundan, id ilə. */
data class SualMenbesi(val sual: QuizQuestion, val movzu: String?)

/**
 * Bütün sualların id üzrə xəritəsi: quiz.json + çalışma bankının nəzəri
 * sualları (`q-<id>` önlüyü ilə — [bankSualId]). Saf funksiya.
 */
internal fun sualXeritesi(questions: List<QuizQuestion>, topics: List<ExerciseTopic>): Map<String, SualMenbesi> {
    val movzuUzre = HashMap<String, String>()
    topics.forEach { t -> t.nezeri.forEach { movzuUzre[bankSualId(it.id)] = t.title } }
    return hovuzlariQur(questions, topics)[QuizRejimi.QARISIQ].orEmpty()
        .associate { it.id to SualMenbesi(it, movzuUzre[it.id]) }
}

/**
 * Səhvlər + məzmun → ekran sətirləri, səhvlərin sırası ilə. Məzmunda artıq
 * olmayan id (sual silinib və ya dəyişib) səssizcə atlanır. Saf funksiya.
 */
internal fun sehvSetirleri(sehvler: List<SehvCavab>, suallar: Map<String, SualMenbesi>): List<SehvSetri> =
    sehvler.mapNotNull { s ->
        val m = suallar[s.sualId] ?: return@mapNotNull null
        SehvSetri(m.sual, m.movzu, s.say, s.sonTarix)
    }

/**
 * «Səhvlərim» (T5.2). Sual mətni seçilmiş dilin məzmunundan id ilə götürülür —
 * dil dəyişəndə səhvlər itmir, yeni dildə görünür.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SehvlerimViewModel(
    repo: ContentRepository,
    oyrenme: OyrenmeRepository
) : ViewModel() {

    // Məzmun oxunmasa (xəta ekranı onsuz da AppRoot-dadır) boş xəritə — ekran çökmür.
    private val suallar = repo.dil.mapLatest {
        try {
            sualXeritesi(repo.quiz().questions, repo.exercises().topics)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** null = hələ yüklənir. */
    val setirler: StateFlow<List<SehvSetri>?> =
        combine(suallar, oyrenme.sehvler) { m, s -> sehvSetirleri(s, m) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
