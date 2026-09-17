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
 * Məzmun mənbəyi. ViewModel-lər yalnız bu interfeysi görür — testdə
 * [AssetContentRepository] əvəzinə kiçik fake ötürmək mümkündür (T2.1).
 */
interface ContentRepository {
    suspend fun content(): Content
    suspend fun sections(): List<Section> = content().sections
    suspend fun exercises(): ExerciseBank
    suspend fun quiz(): QuizBank
    suspend fun playground(): PlaygroundData
    suspend fun searchIndex(): SearchIndex
}

/**
 * Bütün məzmun tətbiqin daxilindədir — şəbəkə tələb olunmur.
 * Aktivlər ilk müraciətdə oxunur və yaddaşda saxlanılır.
 */
class AssetContentRepository(private val context: Context) : ContentRepository {

    private val json = AktivJson

    /**
     * Bir aktivin keşi — öz kilidi ilə. İki ekran eyni anda eyni aktivi istəyə
     * bilər (məsələn dərs və axtarış); kilid olmasa hər ikisi eyni faylı
     * ayrı-ayrı oxuyub parse edərdi. Kilid aktiv başınadır (T2.6): müstəqil
     * aktivlər paralel oxunur — tək ümumi kilid başlanğıcı ardıcıl edirdi.
     */
    private inner class AktivKesi<T : Any>(
        private val ad: String,
        private val parse: (String) -> T
    ) {
        private val kilid = Mutex()
        private var deyer: T? = null

        // Keşdə varsa dərhal qaytarılır, yoxdursa parse edilir. Parse CPU
        // işidir, ona görə Dispatchers.Default — UI sapı bloklanmasın.
        // 183 KB JSON-un açılışı bir neçə yüz millisaniyə çəkir.
        suspend fun al(): T = kilid.withLock {
            deyer ?: withContext(Dispatchers.Default) { parse(oxu(ad)) }.also { deyer = it }
        }
    }

    private val contentKesi = AktivKesi("content.json") { json.decodeFromString<Content>(it) }
    private val exercisesKesi = AktivKesi("exercises.json") { json.decodeFromString<ExerciseBank>(it) }
    private val quizKesi = AktivKesi("quiz.json") { json.decodeFromString<QuizBank>(it) }
    private val playgroundKesi = AktivKesi("playground.json") { json.decodeFromString<PlaygroundData>(it) }
    private val searchKesi = AktivKesi("search.json") { json.decodeFromString<SearchIndex>(it) }

    // Fayl oxunuşu — IO sapında.
    private suspend fun oxu(ad: String): String = withContext(Dispatchers.IO) {
        context.assets.open(ad).bufferedReader().use { it.readText() }
    }

    override suspend fun content(): Content = contentKesi.al()

    override suspend fun exercises(): ExerciseBank = exercisesKesi.al()

    override suspend fun quiz(): QuizBank = quizKesi.al()

    override suspend fun playground(): PlaygroundData = playgroundKesi.al()

    override suspend fun searchIndex(): SearchIndex = searchKesi.al()
}
