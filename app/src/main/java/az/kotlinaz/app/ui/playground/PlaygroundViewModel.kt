package az.kotlinaz.app.ui.playground

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
import az.kotlinaz.app.data.RunResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Kod meydanının nəticə panelinin vəziyyəti. Sealed interface seçilib ki,
 * `when` bütün halları məcburi əhatə etsin — yeni hal əlavə olunanda
 * kompilyator unudulmuş yerləri göstərir.
 */
sealed interface MeydanVeziyyeti {
    data object Bos : MeydanVeziyyeti
    data object Isleyir : MeydanVeziyyeti
    data class Cixis(val metn: String) : MeydanVeziyyeti
    data class Xetalar(val basliq: String, val metn: String) : MeydanVeziyyeti
    data object Oflayn : MeydanVeziyyeti
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
 */
class PlaygroundViewModel(
    private val compiler: CompilerRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    /**
     * Redaktorun vəziyyəti. `MutableState` olduğu kimi `KodRedaktoru`-ya verilir:
     * hər hərf yalnız redaktoru yeniləyir, ekranın qalanı yenidən qurulmur.
     */
    val kod: MutableState<TextFieldValue> = mutableStateOf(
        state.get<SavedState>(KOD)?.read { getStringOrNull(METN) }?.let { sonaKursor(it) } ?: bosKod()
    )

    /** null = boş redaktor («Kod meydanı» çipi seçilidir). */
    val seciliPreset: StateFlow<String?> = state.getStateFlow<String?>(PRESET, null)

    private val _ui = MutableStateFlow<MeydanVeziyyeti>(MeydanVeziyyeti.Bos)
    val ui: StateFlow<MeydanVeziyyeti> = _ui.asStateFlow()

    init {
        state.setSavedStateProvider(KOD) { savedState { putString(METN, kod.value.text) } }
    }

    /** Hazır nümunə seçildi — kod əvəzlənir, nəticə paneli bağlanır. */
    fun presetSec(id: String?, kodMetni: String?) {
        state[PRESET] = id
        kod.value = if (kodMetni == null) bosKod() else sonaKursor(kodMetni)
        _ui.value = MeydanVeziyyeti.Bos
    }

    /** Kod kartındakı «Meydan» düyməsindən gələn kod. */
    fun xariciKodQoy(metn: String) {
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
        viewModelScope.launch { _ui.value = meydanNeticesi(compiler.isle(metn)) }
    }

    private companion object {
        const val KOD = "kod"
        const val METN = "metn"
        const val PRESET = "preset"

        fun bosKod() = TextFieldValue(MEYDAN_BOS_KOD, TextRange(MEYDAN_BOS_KOD.length - 2))
        fun sonaKursor(metn: String) = TextFieldValue(metn, TextRange(metn.length))
    }
}

/** [RunResult] → nəticə paneli. Saf funksiya — testdən birbaşa çağırılır. */
internal fun meydanNeticesi(r: RunResult): MeydanVeziyyeti = when (r) {
    is RunResult.Ok -> MeydanVeziyyeti.Cixis(r.output.ifBlank { "(çıxış yoxdur)" })
    is RunResult.CompileError ->
        MeydanVeziyyeti.Xetalar("Kompilyasiya xətası", r.errors.joinToString("\n"))
    is RunResult.Crashed -> MeydanVeziyyeti.Xetalar(
        "Çalışma vaxtı xətası",
        buildString {
            if (r.output.isNotBlank()) append(r.output).append('\n')
            append(r.exception)
        }
    )
    RunResult.Offline -> MeydanVeziyyeti.Oflayn
    is RunResult.Failed -> MeydanVeziyyeti.Xetalar("Bağlantı problemi", r.message)
}
