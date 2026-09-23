package az.kotlinaz.app.data

import android.content.Context
import az.kotlinaz.app.data.model.Content
import az.kotlinaz.app.data.model.ExerciseBank
import az.kotlinaz.app.data.model.PlaygroundData
import az.kotlinaz.app.data.model.QuizBank
import az.kotlinaz.app.data.model.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

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
    /**
     * Cari məzmun dili — `assets/content/<teq>/` qovluğu (T3.3). Dəyişəndə
     * məzmun saxlayan ViewModel-lər aktivləri yenidən oxuyur; seçim özü
     * `UserPrefs.dil`-dədir, buradakı isə artıq mövcud qovluğa çözülmüş teqdir.
     */
    val dil: StateFlow<String>

    /** Məzmun dilini dəyişir; eyni dil üçün heçdir. Keşlər köhnə dildə qalmır. */
    fun dilDeyisdi(teq: String)

    /** Qurulmuş məzmun dilləri — `locales.json`. */
    suspend fun diller(): List<String>

    suspend fun content(): Content
    suspend fun sections(): List<Section> = content().sections
    suspend fun exercises(): ExerciseBank
    suspend fun quiz(): QuizBank
    suspend fun playground(): PlaygroundData
    // Axtarış indeksi aktiv deyil — yüklənmiş məzmundan qurulur (T3.5, axtarisSenedleri).
}

/**
 * Bütün məzmun tətbiqin daxilindədir — şəbəkə tələb olunmur.
 * Aktivlər ilk müraciətdə oxunur və yaddaşda saxlanılır.
 */
class AssetContentRepository(
    private val context: Context,
    /**
     * Başlanğıc məzmun dili — `assets/content/<locale>/` qovluğu (T3.2). Baza
     * `az` saytdan qurulur, qalanları `tools/build-locale.js` overlay-dən
     * yaradır. Seçim [dilDeyisdi] ilə gəlir (T3.3).
     */
    locale: String = BAZA_LOCALE
) : ContentRepository {

    companion object {
        const val BAZA_LOCALE = "az"
    }

    private val json = AktivJson

    private val _dil = MutableStateFlow(locale)
    override val dil: StateFlow<String> = _dil.asStateFlow()

    // Keşlər silinmir: hər keş dəyərini oxunduğu dillə birlikdə saxlayır və
    // dil uyğun gəlmirsə yenidən oxuyur. Beləcə dil dəyişən anda gedən oxunuş
    // köhnə dilin faylını «yeni» kimi keşə yaza bilmir.
    override fun dilDeyisdi(teq: String) { _dil.value = teq }

    override suspend fun diller(): List<String> = withContext(Dispatchers.IO) {
        context.assets.open("content/locales.json").bufferedReader().use { it.readText() }
    }.let { json.decodeFromString<LocaleSiyahisi>(it).locales }

    // Parse CPU işidir, ona görə Dispatchers.Default — UI sapı bloklanmasın.
    // 183 KB JSON-un açılışı bir neçə yüz millisaniyə çəkir.
    private fun <T : Any> aktivKesi(ad: String, parse: (String) -> T) =
        DilKesi({ _dil.value }) { teq -> withContext(Dispatchers.Default) { parse(oxu(teq, ad)) } }

    private val contentKesi = aktivKesi("content.json") { json.decodeFromString<Content>(it) }
    private val exercisesKesi = aktivKesi("exercises.json") { json.decodeFromString<ExerciseBank>(it) }
    private val quizKesi = aktivKesi("quiz.json") { json.decodeFromString<QuizBank>(it) }
    private val playgroundKesi = aktivKesi("playground.json") { json.decodeFromString<PlaygroundData>(it) }

    // Fayl oxunuşu — IO sapında, dil qovluğundan. Qovluq yoxdursa `en`, o da
    // yoxdursa baza: [mezmunDiliSec] onsuz da yalnız mövcud dilə çözür, bu
    // ikinci sığortadır (locales.json ilə qovluqlar uyğunsuz qalsa çökməsin).
    private suspend fun oxu(teq: String, ad: String): String = withContext(Dispatchers.IO) {
        val yollar = listOf(teq, MEZMUN_FALLBACK, BAZA_LOCALE).distinct()
        var sonXeta: IOException? = null
        for (dil in yollar) {
            try {
                return@withContext context.assets.open("content/$dil/$ad").bufferedReader().use { it.readText() }
            } catch (e: IOException) {
                sonXeta = e
            }
        }
        throw checkNotNull(sonXeta)
    }

    override suspend fun content(): Content = contentKesi.al()

    override suspend fun exercises(): ExerciseBank = exercisesKesi.al()

    override suspend fun quiz(): QuizBank = quizKesi.al()

    override suspend fun playground(): PlaygroundData = playgroundKesi.al()
}

/**
 * Bir aktivin dil üzrə keşi — öz kilidi ilə. İki ekran eyni anda eyni aktivi
 * istəyə bilər (məsələn dərs və axtarış); kilid olmasa hər ikisi eyni faylı
 * ayrı-ayrı oxuyub parse edərdi. Kilid aktiv başınadır (T2.6): müstəqil
 * aktivlər paralel oxunur — tək ümumi kilid başlanğıcı ardıcıl edirdi.
 *
 * TƏK yer var: dəyər oxunduğu dillə birlikdə saxlanılır, dil fərqlidirsə
 * yenidən yüklənir və köhnə dilin obyekti buraxılır — yaddaşda həmişə bir
 * dilin məzmunu qalır, dil dəyişdikcə keş böyümür (T3.7).
 */
internal class DilKesi<T : Any>(
    private val dil: () -> String,
    private val yukle: suspend (String) -> T
) {
    private val kilid = Mutex()
    /** Dəyər və oxunduğu dil — dil fərqlidirsə keş etibarsızdır. */
    private var deyer: Pair<String, T>? = null

    suspend fun al(): T = kilid.withLock {
        val teq = dil()
        deyer?.takeIf { it.first == teq }?.second ?: yukle(teq).also { deyer = teq to it }
    }
}

/** `assets/content/locales.json` — baza dili və qurulmuş dil qovluqları. */
@Serializable
data class LocaleSiyahisi(val base: String, val locales: List<String>)
