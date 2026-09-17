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

/** Aktiv oxumayan repository: sabit kiçik məzmun, ya da istənilən istisna. */
class FakeContentRepository(
    private val bolmeler: List<Section> = emptyList(),
    private val movzular: List<ExerciseTopic> = emptyList(),
    private val senedler: List<SearchDoc> = emptyList(),
    private val suallar: List<QuizQuestion> = emptyList(),
    private val xeta: Exception? = null
) : ContentRepository {
    override suspend fun content(): Content {
        xeta?.let { throw it }
        return Content(sections = bolmeler)
    }
    override suspend fun exercises() = ExerciseBank(movzular)
    override suspend fun quiz() = QuizBank(suallar)
    override suspend fun playground() = PlaygroundData()
    override suspend fun searchIndex() = SearchIndex(senedler)
}
