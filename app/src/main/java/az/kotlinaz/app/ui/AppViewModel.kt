package az.kotlinaz.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.KotlinCompiler
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.PlaygroundPreset
import az.kotlinaz.app.data.model.QuizQuestion
import az.kotlinaz.app.data.model.SearchDoc
import az.kotlinaz.app.data.model.Section
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Bütün ekranların paylaşdığı vəziyyət. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ContentRepository(app)
    private val prefs = UserPrefs(app)
    val compiler = KotlinCompiler(app)

    private val _sections = MutableStateFlow<List<Section>>(emptyList())
    val sections: StateFlow<List<Section>> = _sections.asStateFlow()

    private val _topics = MutableStateFlow<List<ExerciseTopic>>(emptyList())
    val topics: StateFlow<List<ExerciseTopic>> = _topics.asStateFlow()

    private val _quiz = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quiz: StateFlow<List<QuizQuestion>> = _quiz.asStateFlow()

    private val _presets = MutableStateFlow<List<PlaygroundPreset>>(emptyList())
    val presets: StateFlow<List<PlaygroundPreset>> = _presets.asStateFlow()

    private val _searchDocs = MutableStateFlow<List<SearchDoc>>(emptyList())
    val searchDocs: StateFlow<List<SearchDoc>> = _searchDocs.asStateFlow()

    private val _hazir = MutableStateFlow(false)
    val hazir: StateFlow<Boolean> = _hazir.asStateFlow()

    val tema: StateFlow<ThemeMode> =
        prefs.tema.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val sriftOlcusu: StateFlow<Int> =
        prefs.sriftOlcusu.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val oxunanBolmeler: StateFlow<Set<String>> =
        prefs.oxunanBolmeler.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val hellEdilmis: StateFlow<Set<String>> =
        prefs.hellEdilmis.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val quizRekord: StateFlow<Int> =
        prefs.quizRekord.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val sonBolme: StateFlow<String?> =
        prefs.sonBolme.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Kod meydanına göndərilən kod (kod kartındakı «Meydan» düyməsi). */
    private val _meydanKodu = MutableStateFlow<String?>(null)
    val meydanKodu: StateFlow<String?> = _meydanKodu.asStateFlow()

    init {
        viewModelScope.launch {
            _sections.value = repo.sections()
            _topics.value = repo.exercises().topics
            _quiz.value = repo.quiz().questions
            _presets.value = repo.playground().presets
            _searchDocs.value = repo.searchIndex().docs
            _hazir.value = true
        }
    }

    fun bolme(id: String): Section? = _sections.value.firstOrNull { it.id == id }

    fun movzu(id: String): ExerciseTopic? = _topics.value.firstOrNull { it.id == id }

    fun temaSec(mode: ThemeMode) = viewModelScope.launch { prefs.temaSec(mode) }

    fun sriftSec(olcu: Int) = viewModelScope.launch { prefs.sriftSec(olcu) }

    fun bolmeniOxunmusIsaretle(id: String) =
        viewModelScope.launch { prefs.bolmeniOxunmusIsaretle(id) }

    fun calismaniHellIsaretle(id: String) =
        viewModelScope.launch { prefs.calismaniHellIsaretle(id) }

    fun quizNeticesiniYaz(bal: Int) = viewModelScope.launch { prefs.quizNeticesiniYaz(bal) }

    fun oxumaTereqqisiniSifirla() = viewModelScope.launch { prefs.oxumaTereqqisiniSifirla() }

    fun calismaTereqqisiniSifirla() = viewModelScope.launch { prefs.calismaTereqqisiniSifirla() }

    fun hamisiniSifirla() = viewModelScope.launch { prefs.hamisiniSifirla() }

    fun meydanaGonder(kod: String) { _meydanKodu.value = kod }

    fun meydanKodunuTemizle() { _meydanKodu.value = null }

    /* ---------- Oflayn axtarış ---------- */

    fun axtar(sorgu: String): List<SearchNetice> {
        val q = sorgu.trim().lowercase()
        if (q.length < 2) return emptyList()

        return _searchDocs.value.mapNotNull { doc ->
            val basliqda = doc.title.lowercase().contains(q)
            val yer = doc.text.lowercase().indexOf(q)
            if (!basliqda && yer < 0) return@mapNotNull null

            val parca = if (yer >= 0) {
                val bas = (yer - 60).coerceAtLeast(0)
                val son = (yer + q.length + 90).coerceAtMost(doc.text.length)
                buildString {
                    if (bas > 0) append("…")
                    append(doc.text.substring(bas, son).trim())
                    if (son < doc.text.length) append("…")
                }
            } else {
                doc.text.take(140)
            }

            SearchNetice(
                id = doc.id,
                title = doc.title,
                group = doc.group,
                parca = parca,
                bal = if (basliqda) 0 else 1
            )
        }.sortedBy { it.bal }
    }
}

data class SearchNetice(
    val id: String,
    val title: String,
    val group: String,
    val parca: String,
    val bal: Int
)
