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
 * Aktivlərin JSON konfiqurasiyası. Fayl səviyyəsindədir ki, smoke testlər
 * (T1.2) tətbiqin işlətdiyi EYNİ nüsxə ilə decode etsin — ayrıca qurulmuş
 * `Json` real davranışı yoxlamazdı.
 */
internal val AktivJson = Json {
    // Aktivlərə yeni sahə əlavə olunsa köhnə model onu sadəcə atsın —
    // məzmun boru xətti (tools/build-content.js) modeldən irəli gedə bilər.
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Bütün məzmun tətbiqin daxilindədir — şəbəkə tələb olunmur.
 * Aktivlər ilk müraciətdə oxunur və yaddaşda saxlanılır.
 */
class ContentRepository(private val context: Context) {

    private val json = AktivJson

    // İki ekran eyni anda eyni aktivi istəyə bilər (məsələn dərs və axtarış).
    // Kilid olmasa hər ikisi eyni faylı ayrı-ayrı oxuyub parse edərdi.
    private val kilid = Mutex()

    private var contentCache: Content? = null
    private var exercisesCache: ExerciseBank? = null
    private var quizCache: QuizBank? = null
    private var playgroundCache: PlaygroundData? = null
    private var searchCache: SearchIndex? = null

    // Fayl oxunuşu — IO sapında.
    private suspend fun oxu(ad: String): String = withContext(Dispatchers.IO) {
        context.assets.open(ad).bufferedReader().use { it.readText() }
    }

    // Nümunə oxu: keşdə varsa dərhal qaytarılır, yoxdursa parse edilir.
    // Parse CPU işidir, ona görə Dispatchers.Default — UI sapı bloklanmasın.
    // 183 KB JSON-un açılışı bir neçə yüz millisaniyə çəkir.
    suspend fun content(): Content = kilid.withLock {
        contentCache ?: withContext(Dispatchers.Default) {
            json.decodeFromString<Content>(oxu("content.json"))
        }.also { contentCache = it }
    }

    suspend fun sections(): List<Section> = content().sections

    // Aşağıdakılar eyni nümunə üzrədir — hər aktiv öz keşi ilə.

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
