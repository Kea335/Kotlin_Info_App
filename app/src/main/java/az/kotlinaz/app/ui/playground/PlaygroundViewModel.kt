package az.kotlinaz.app.ui.playground

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.read
import androidx.savedstate.savedState
import az.kotlinaz.app.data.CompilerRepository
import az.kotlinaz.app.data.MeydanQaralamasi
import az.kotlinaz.app.data.RunResult
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.ui.IcraXetasi
import az.kotlinaz.app.ui.XetaSetri
import az.kotlinaz.app.ui.xetaSetrleri
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Kod meydanının nəticə panelinin vəziyyəti. Sealed interface seçilib ki,
 * `when` bütün halları məcburi əhatə etsin — yeni hal əlavə olunanda
 * kompilyator unudulmuş yerləri göstərir.
 */
sealed interface MeydanVeziyyeti {
    data object Bos : MeydanVeziyyeti
    data object Isleyir : MeydanVeziyyeti
    /** [metn] boşdursa UI «(çıxış yoxdur)» yazır. */
    data class Cixis(val metn: String) : MeydanVeziyyeti
    data class Xetalar(val nov: IcraXetasi, val setirler: List<XetaSetri>) : MeydanVeziyyeti
    data object Oflayn : MeydanVeziyyeti
    /** İstifadəçi «Dayandır» basdı — sorğu kəsildi, nəticə yoxdur. */
    data object Dayandirildi : MeydanVeziyyeti
}

/** Boş redaktorun başlanğıc kodu — kursor gövdənin içində durur. */
const val MEYDAN_BOS_KOD = "fun main() {\n    \n}"

/**
 * Kod meydanı — icra `viewModelScope`-dadır, ona görə istifadəçi «İşlə»
 * basıb ekrandan çıxsa (geri, başqa tab, döndərmə) sorğu itmir, qayıdanda
 * nəticə yerindədir (T2.2).
 *
 * Redaktor mətni [SavedStateHandle]-a yalnız sistem vəziyyəti saxlayanda
 * yazılır ([SavedStateRegistry.SavedStateProvider]) — hər vuruşda deyil.
 *
 * Kod diskdə də saxlanılır (Mərhələ 4): yazı dayanandan [AVTO_SAXLAMA_MS]
 * sonra və tətbiq arxa plana keçəndə. Tətbiq tam bağlanıb açılanda
 * redaktor boş şablonla deyil, son kodla açılır. Process death-dən
 * qayıdanda [SavedStateHandle]-dakı hal daha təzədir — disk oxunmur.
 */
@OptIn(FlowPreview::class)
class PlaygroundViewModel(
    private val compiler: CompilerRepository,
    private val state: SavedStateHandle,
    private val prefs: UserPrefs
) : ViewModel() {

    private val berpaOlunmus: String? = state.get<SavedState>(KOD)?.read { getStringOrNull(METN) }
    private val ilkin: TextFieldValue = berpaOlunmus?.let { sonaKursor(it) } ?: bosKod()

    /**
     * Redaktorun vəziyyəti. `MutableState` olduğu kimi `KodRedaktoru`-ya verilir:
     * hər hərf yalnız redaktoru yeniləyir, ekranın qalanı yenidən qurulmur.
     */
    val kod: MutableState<TextFieldValue> = mutableStateOf(ilkin)

    /** null = boş redaktor («Kod meydanı» çipi seçilidir). */
    val seciliPreset: StateFlow<String?> = state.getStateFlow<String?>(PRESET, null)

    private val _ui = MutableStateFlow<MeydanVeziyyeti>(MeydanVeziyyeti.Bos)
    val ui: StateFlow<MeydanVeziyyeti> = _ui.asStateFlow()

    /** Gedən icra — «Dayandır» onu ləğv edir, `KotlinCompiler` bağlantını kəsir. */
    private var icra: Job? = null

    /** Disk qaralaması oxunana qədər yazılmır — boş şablon saxlanmış kodu əzməsin. */
    private var diskHazirdir = false

    init {
        state.setSavedStateProvider(KOD) {
            // Activity onStop — tətbiq arxa plana keçir. Sonra sistem və ya
            // istifadəçi onu öldürsə də kod diskdə qalır.
            diskeYaz()
            savedState { putString(METN, kod.value.text) }
        }
        viewModelScope.launch {
            if (berpaOlunmus == null) {
                prefs.meydanQaralamasi()?.let { q ->
                    // Oxunuş gedərkən istifadəçi yazıbsa və ya kod kartından
                    // kod gəlibsə diskdəki köhnə kod onu əvəzləməsin.
                    if (kod.value === ilkin) {
                        kod.value = sonaKursor(q.kod)
                        state[PRESET] = q.preset
                    }
                }
            }
            diskHazirdir = true
            combine(snapshotFlow { kod.value.text }, seciliPreset) { k, p -> MeydanQaralamasi(k, p) }
                .drop(1)
                .debounce(AVTO_SAXLAMA_MS)
                .collect { prefs.meydanQaralamasiniYaz(it) }
        }
    }

    private fun diskeYaz() {
        if (!diskHazirdir) return
        val q = MeydanQaralamasi(kod.value.text, seciliPreset.value)
        viewModelScope.launch { prefs.meydanQaralamasiniYaz(q) }
    }

    /** Hazır nümunə seçildi — kod əvəzlənir, nəticə paneli bağlanır. */
    fun presetSec(id: String?, kodMetni: String?) {
        icra?.cancel()
        state[PRESET] = id
        kod.value = if (kodMetni == null) bosKod() else sonaKursor(kodMetni)
        _ui.value = MeydanVeziyyeti.Bos
    }

    /** Kod kartındakı «Meydan» düyməsindən gələn kod. */
    fun xariciKodQoy(metn: String) {
        icra?.cancel()
        state[PRESET] = null
        kod.value = sonaKursor(metn)
        _ui.value = MeydanVeziyyeti.Bos
    }

    /** Yalnız nəticə panelini bağlayır; redaktordakı koda toxunmur. */
    fun temizle() { _ui.value = MeydanVeziyyeti.Bos }

    /**
     * Kodu serverdə işlədir. İcra gedərkən ikinci çağırış heçdir — eyni kodu
     * paralel iki dəfə göndərməyin mənası yoxdur (düymə də kilidlidir).
     */
    fun isle() {
        if (_ui.value is MeydanVeziyyeti.Isleyir) return
        _ui.value = MeydanVeziyyeti.Isleyir
        // Kod yalnız BURADA oxunur — yazı zamanı deyil.
        val metn = kod.value.text
        icra = viewModelScope.launch { _ui.value = meydanNeticesi(compiler.isle(metn)) }
    }

    /** Gedən icranı dayandırır: sorğu kəsilir, cavab gözlənilmir. */
    fun dayandir() {
        if (_ui.value !is MeydanVeziyyeti.Isleyir) return
        icra?.cancel()
        icra = null
        _ui.value = MeydanVeziyyeti.Dayandirildi
    }

    private companion object {
        const val KOD = "kod"
        const val METN = "metn"
        const val PRESET = "preset"
        /** Yazı bu qədər dayanandan sonra kod diskə yazılır. */
        const val AVTO_SAXLAMA_MS = 1_000L

        fun bosKod() = TextFieldValue(MEYDAN_BOS_KOD, TextRange(MEYDAN_BOS_KOD.length - 2))
        fun sonaKursor(metn: String) = TextFieldValue(metn, TextRange(metn.length))
    }
}

/** [RunResult] → nəticə paneli. Saf funksiya — testdən birbaşa çağırılır. */
internal fun meydanNeticesi(r: RunResult): MeydanVeziyyeti = when (r) {
    is RunResult.Ok -> MeydanVeziyyeti.Cixis(r.output)
    is RunResult.CompileError ->
        MeydanVeziyyeti.Xetalar(IcraXetasi.KOMPILYASIYA, r.errors.map { XetaSetri(it.mesaj, it.setir) })
    is RunResult.Crashed -> MeydanVeziyyeti.Xetalar(IcraXetasi.ICRA, xetaSetrleri(r.output) + XetaSetri(r.exception))
    RunResult.Offline -> MeydanVeziyyeti.Oflayn
    is RunResult.Failed -> MeydanVeziyyeti.Xetalar(IcraXetasi.BAGLANTI, xetaSetrleri(r.message))
}
