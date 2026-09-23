package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.Content
import az.kotlinaz.app.data.model.ExerciseBank
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.PlaygroundData
import az.kotlinaz.app.data.model.QuizBank
import az.kotlinaz.app.data.model.QuizQuestion
import az.kotlinaz.app.data.model.SearchDoc
import az.kotlinaz.app.data.model.SearchIndex
import az.kotlinaz.app.data.model.Section
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Aktiv oxumayan repository: sabit kiçik məzmun, ya da istənilən istisna. */
class FakeContentRepository(
    private val bolmeler: List<Section> = emptyList(),
    private val movzular: List<ExerciseTopic> = emptyList(),
    private val senedler: List<SearchDoc> = emptyList(),
    private val suallar: List<QuizQuestion> = emptyList(),
    private val xeta: Exception? = null,
    /** Yalnız çalışma bankı sınsın — ikinci mərhələnin xətası üçün (T2.6). */
    private val calismaXetasi: Exception? = null,
    /** Qurulmuş dillər — `locales.json` əvəzi (T3.3). */
    private val diller: List<String> = listOf(AssetContentRepository.BAZA_LOCALE),
    /** Dil üzrə bölmələr; dil burada yoxdursa [bolmeler] qaytarılır. */
    private val dilUzreBolmeler: Map<String, List<Section>> = emptyMap(),
    /** Dil üzrə axtarış sənədləri; dil burada yoxdursa [senedler]. */
    private val dilUzreSenedler: Map<String, List<SearchDoc>> = emptyMap()
) : ContentRepository {

    private val _dil = MutableStateFlow(AssetContentRepository.BAZA_LOCALE)
    override val dil: StateFlow<String> = _dil.asStateFlow()

    /** Neçə dəfə oxunub — «dil dəyişəndə yenidən oxunur» testləri üçün. */
    var oxunmaSayi = 0
        private set

    override fun dilDeyisdi(teq: String) { _dil.value = teq }

    override suspend fun diller(): List<String> = diller

    override suspend fun content(): Content {
        xeta?.let { throw it }
        oxunmaSayi++
        return Content(sections = dilUzreBolmeler[_dil.value] ?: bolmeler)
    }
    override suspend fun exercises(): ExerciseBank {
        calismaXetasi?.let { throw it }
        return ExerciseBank(movzular)
    }
    override suspend fun quiz() = QuizBank(suallar)
    override suspend fun playground() = PlaygroundData()
    override suspend fun searchIndex() = SearchIndex(dilUzreSenedler[_dil.value] ?: senedler)
}
