package az.kotlinaz.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.KotlinCompiler
import az.kotlinaz.app.data.Tereqqi
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.tereqqiHesabla
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.PlaygroundPreset
import az.kotlinaz.app.data.model.QuizQuestion
import az.kotlinaz.app.data.model.SearchDoc
import az.kotlinaz.app.data.model.Section
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    // Aktivlər oxunub bitəndə true olur — AppRoot ona qədər fırlanğıc göstərir.
    private val _hazir = MutableStateFlow(false)
    val hazir: StateFlow<Boolean> = _hazir.asStateFlow()

    // Aktiv oxunuşu sınarsa səbəbi burada qalır. Əvvəllər belə hal yalnız
    // sonsuz fırlanğıc kimi görünürdü — indi ekranda izahı çıxır.
    private val _yukleneXetasi = MutableStateFlow<String?>(null)
    val yukleneXetasi: StateFlow<String?> = _yukleneXetasi.asStateFlow()

    val tema: StateFlow<ThemeMode> =
        prefs.tema.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val sriftOlcusu: StateFlow<Int> =
        prefs.sriftOlcusu.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val kodTamamlama: StateFlow<Boolean> =
        prefs.kodTamamlama.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val oxunanBolmeler: StateFlow<Set<String>> =
        prefs.oxunanBolmeler.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val hellEdilmis: StateFlow<Set<String>> =
        prefs.hellEdilmis.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Bilik testinin rejim üzrə rekordları: `"junior"` → bal. */
    val quizRekordlari: StateFlow<Map<String, Int>> =
        prefs.quizRekordlari.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val sonBolme: StateFlow<String?> =
        prefs.sonBolme.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Tərəqqi mənzərəsi — bölmə mənimsəməsi və səviyyə.
     *
     * Dörd mənbədən yığılır, ona görə `combine`: məzmun gec gəlir, tərəqqi isə
     * istifadəçi çalışma həll etdikcə dəyişir. Nəticə tək yerdə hesablanır —
     * dərslər, çalışmalar və tənzimləmələr ekranları eyni rəqəmi görür.
     */
    val tereqqi: StateFlow<Tereqqi> = combine(
        sections, topics, hellEdilmis, oxunanBolmeler
    ) { bolmeler, movzular, hell, oxunan ->
        tereqqiHesabla(bolmeler, movzular, hell, oxunan)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        tereqqiHesabla(emptyList(), emptyList(), emptySet(), emptySet())
    )

    /** Kod meydanına göndərilən kod (kod kartındakı «Meydan» düyməsi). */
    private val _meydanKodu = MutableStateFlow<String?>(null)
    val meydanKodu: StateFlow<String?> = _meydanKodu.asStateFlow()

    init {
        viewModelScope.launch {
            // DÜZƏLİŞ: əvvəl bu blokda try/catch yox idi. Hər hansı aktiv oxunmasa
            // (məsələn release qurulusunda R8 serializatoru silsə) korutin çökür,
            // `hazir` heç vaxt true olmur və tətbiq əbədi fırlanğıcda ilişirdi.
            // İndi səbəb `yukleneXetasi`-yə yazılır və istifadəçi onu görür.
            try {
                _sections.value = repo.sections()
                _topics.value = repo.exercises().topics
                _quiz.value = repo.quiz().questions
                _presets.value = repo.playground().presets
                _searchDocs.value = repo.searchIndex().docs

                // Axtarış üçün kiçik hərfli surət — bir dəfə, burada.
                kicikIndeks = _searchDocs.value.map {
                    AxtarisSened(it, it.title.lowercase(), it.text.lowercase())
                }
            } catch (e: Exception) {
                _yukleneXetasi.value = e.message ?: e.javaClass.simpleName
            } finally {
                // `finally` vacibdir: xəta olsa da ekran fırlanğıcdan çıxmalıdır.
                _hazir.value = true
            }
        }
    }

    fun bolme(id: String): Section? = _sections.value.firstOrNull { it.id == id }

    fun movzu(id: String): ExerciseTopic? = _topics.value.firstOrNull { it.id == id }

    fun temaSec(mode: ThemeMode) = viewModelScope.launch { prefs.temaSec(mode) }

    fun sriftSec(olcu: Int) = viewModelScope.launch { prefs.sriftSec(olcu) }

    fun tamamlamaSec(acıq: Boolean) = viewModelScope.launch { prefs.tamamlamaSec(acıq) }

    fun bolmeniOxunmusIsaretle(id: String) =
        viewModelScope.launch { prefs.bolmeniOxunmusIsaretle(id) }

    // DÜZƏLİŞ: UserPrefs.sonBolmeniYaz() yazılmışdı, amma heç yerdən
    // çağırılmırdı. Nəticədə «Davam et» kartı yalnız SONA QƏDƏR oxunmuş
    // bölməni göstərirdi — yəni yarımçıq qoyulan dərsə qayıtmaq mümkün deyildi.
    // İndi dərs açılan kimi son mövqe yazılır.
    fun sonBolmeniYaz(id: String) = viewModelScope.launch { prefs.sonBolmeniYaz(id) }

    fun calismaniHellIsaretle(id: String) =
        viewModelScope.launch { prefs.calismaniHellIsaretle(id) }

    fun quizNeticesiniYaz(rejim: String, bal: Int) =
        viewModelScope.launch { prefs.quizNeticesiniYaz(rejim, bal) }

    fun oxumaTereqqisiniSifirla() = viewModelScope.launch { prefs.oxumaTereqqisiniSifirla() }

    fun calismaTereqqisiniSifirla() = viewModelScope.launch { prefs.calismaTereqqisiniSifirla() }

    fun hamisiniSifirla() = viewModelScope.launch { prefs.hamisiniSifirla() }

    fun meydanaGonder(kod: String) { _meydanKodu.value = kod }

    fun meydanKodunuTemizle() { _meydanKodu.value = null }

    /* ---------- Oflayn axtarış ---------- */

    // Axtarış indeksi ~120 KB mətndir. Əvvəllər hər hərf yazılanda bütün 32 sənəd
    // yenidən lowercase() edilirdi — hər vuruşda onlarla KB artıq yaddaş və UI
    // sapında lüzumsuz iş. İndi kiçik hərfli variant bir dəfə, sənədlər gələndə
    // hazırlanır; `axtar()` yalnız hazır sətirlərdə axtarır.
    private data class AxtarisSened(
        val doc: SearchDoc,
        val basliqKicik: String,
        val metnKicik: String
    )

    // İndeks `init` blokunda, sənədlər oxunan kimi bir dəfə qurulur.
    private var kicikIndeks: List<AxtarisSened> = emptyList()

    fun axtar(sorgu: String): List<SearchNetice> {
        val q = sorgu.trim().lowercase()
        // Bir hərflik sorğu bütün sənədləri qaytarardı — mənasızdır.
        if (q.length < 2) return emptyList()

        return kicikIndeks.mapNotNull { (doc, basliqKicik, metnKicik) ->
            val basliqda = basliqKicik.contains(q)
            val yer = metnKicik.indexOf(q)
            // Nə başlıqda, nə mətndə varsa — bu sənəd nəticəyə düşmür.
            if (!basliqda && yer < 0) return@mapNotNull null

            // Tapılan yerin ətrafından parça kəsilir: 60 simvol öncə, 90 sonra.
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
                // Başlıqda tapılanlar 0 bal alır və yuxarı qalxır; mətndə
                // tapılanlar 1. sortedBy sabitdir, ona görə bərabər ballılar
                // sənəd sırasını (yəni dərs sırasını) saxlayır.
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
