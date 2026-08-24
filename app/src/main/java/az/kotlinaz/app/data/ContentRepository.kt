package az.kotlinaz.app.data

import android.content.Context
import az.kotlinaz.app.data.model.Content
import az.kotlinaz.app.data.model.ExerciseBank
import az.kotlinaz.app.data.model.PlaygroundData
import az.kotlinaz.app.data.model.QuizBank
import az.kotlinaz.app.data.model.SearchIndex
import az.kotlinaz.app.data.model.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Bütün məzmun tətbiqin daxilindədir — şəbəkə tələb olunmur.
 * Aktivlər ilk müraciətdə oxunur və yaddaşda saxlanılır.
 */
class ContentRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val kilid = Mutex()

    private var contentCache: Content? = null
    private var exercisesCache: ExerciseBank? = null
    private var quizCache: QuizBank? = null
    private var playgroundCache: PlaygroundData? = null
    private var searchCache: SearchIndex? = null

    private suspend fun oxu(ad: String): String = withContext(Dispatchers.IO) {
        context.assets.open(ad).bufferedReader().use { it.readText() }
    }

    suspend fun content(): Content = kilid.withLock {
        contentCache ?: withContext(Dispatchers.Default) {
            json.decodeFromString<Content>(oxu("content.json"))
        }.also { contentCache = it }
    }

    suspend fun sections(): List<Section> = content().sections

    suspend fun section(id: String): Section? = sections().firstOrNull { it.id == id }

    suspend fun exercises(): ExerciseBank = kilid.withLock {
        exercisesCache ?: withContext(Dispatchers.Default) {
            json.decodeFromString<ExerciseBank>(oxu("exercises.json"))
        }.also { exercisesCache = it }
    }

    suspend fun quiz(): QuizBank = kilid.withLock {
        quizCache ?: withContext(Dispatchers.Default) {
            json.decodeFromString<QuizBank>(oxu("quiz.json"))
        }.also { quizCache = it }
    }

    suspend fun playground(): PlaygroundData = kilid.withLock {
        playgroundCache ?: withContext(Dispatchers.Default) {
            json.decodeFromString<PlaygroundData>(oxu("playground.json"))
        }.also { playgroundCache = it }
    }

    suspend fun searchIndex(): SearchIndex = kilid.withLock {
        searchCache ?: withContext(Dispatchers.Default) {
            json.decodeFromString<SearchIndex>(oxu("search.json"))
        }.also { searchCache = it }
    }
}
