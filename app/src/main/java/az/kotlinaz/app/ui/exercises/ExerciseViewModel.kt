package az.kotlinaz.app.ui.exercises

import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.oyrenme.OyrenmeRepository
import az.kotlinaz.app.data.model.TheoryExercise
import az.kotlinaz.app.ui.quiz.bankSualId
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import az.kotlinaz.app.data.CompilerRepository
import az.kotlinaz.app.data.RunResult
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.ui.IcraXetasi
import az.kotlinaz.app.ui.XetaSetri
import az.kotlinaz.app.ui.xetaSetrleri
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.model.PracticeExercise
import az.kotlinaz.app.data.neticeniNormallasdir
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Praktiki çalışmanın yoxlama panelinin vəziyyəti. */
sealed interface YoxlamaVeziyyeti {
    data object Bos : YoxlamaVeziyyeti
    data object Isleyir : YoxlamaVeziyyeti
    data class Duzgun(val cixis: String) : YoxlamaVeziyyeti
    /** [alinan] boşdursa UI «(boş)» yazır. */
    data class Sehv(val gozlenilen: String, val alinan: String) : YoxlamaVeziyyeti
    data class Xeta(val nov: IcraXetasi, val setirler: List<XetaSetri>) : YoxlamaVeziyyeti
    data object Oflayn : YoxlamaVeziyyeti
}

/** Çalışma rejimi; ekrandakı adı `ui/Etiketler.kt`-dədir. */
enum class Rejim { NEZERI, PRAKTIKI }

/**
 * Çalışma mühərrikinin vəziyyəti — bir mövzu üçün bir nüsxə (naviqasiya
 * girişinə bağlıdır, başqa mövzuya keçəndə yenisi yaranır).
 *
 * Yoxlama `viewModelScope`-dadır: «Yoxla» basıb ekrandan çıxan istifadəçi
 * qayıdanda nəticəni görür, sorğu ləğv olmur (T2.2). Düzgün nəticədə
 * «həll edildi» yazısı da burada gedir — UI yalnız vəziyyəti göstərir.
 *
 * Rejim, səviyyə filtri, indeks və açıq panellər [SavedStateHandle]-dadır;
 * redaktor mətni ora yalnız sistem vəziyyəti saxlayanda yazılır.
 */
class ExerciseViewModel(
    private val compiler: CompilerRepository,
    private val prefs: UserPrefs,
    /** Nəzəri çalışmadakı səhvlər «Səhvlərim»-ə düşür (T5.2). */
    private val oyrenme: OyrenmeRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    val rejim: StateFlow<Rejim> = state.getStateFlow(REJIM, Rejim.NEZERI)

    /** null = «Hamısı» (səviyyə filtri yoxdur). */
    val seviyye: StateFlow<Level?> = state.getStateFlow<Level?>(SEVIYYE, null)

    val index: StateFlow<Int> = state.getStateFlow(INDEX, 0)

    val ipucuGorunur: StateFlow<Boolean> = state.getStateFlow(IPUCU, false)
    val hellGorunur: StateFlow<Boolean> = state.getStateFlow(HELL, false)
    /** «Cavab» düyməsinə basılıb, amma hələ təsdiqlənməyib. */
    val cavabSorusulur: StateFlow<Boolean> = state.getStateFlow(CAVAB_SORUSULUR, false)

    private val _yoxlama = MutableStateFlow<YoxlamaVeziyyeti>(YoxlamaVeziyyeti.Bos)
    val yoxlama: StateFlow<YoxlamaVeziyyeti> = _yoxlama.asStateFlow()

    // Redaktor hansı çalışmaya aiddir. Başqa çalışma açılanda mətn və
    // yoxlama sıfırlanır; eyni çalışma (döndərmə, geri qayıtma) yerində qalır.
    private var cariCalismaId: String? = state[CALISMA_ID]

    /**
     * Redaktorun vəziyyəti. `MutableState` olduğu kimi `KodRedaktoru`-ya verilir —
     * yazı zamanı yalnız redaktor yenidən qurulur, kartın qalanı yerində qalır.
     */
    val kod: MutableState<TextFieldValue> = mutableStateOf(
        TextFieldValue(state.get<SavedState>(KOD)?.read { getStringOrNull(METN) }.orEmpty())
    )

    init {
        state.setSavedStateProvider(KOD) { savedState { putString(METN, kod.value.text) } }
    }

    /* ---------- Naviqasiya ---------- */

    // Rejim və ya filtr dəyişəndə indeks sıfırlanır — 40-cı nəzəri çalışmadan
    // 40-cı praktikiyə tullanmaq gözlənilməz olardı.
    fun rejimSec(r: Rejim) { state[REJIM] = r; state[INDEX] = 0 }

    fun seviyyeSec(lv: Level?) { state[SEVIYYE] = lv; state[INDEX] = 0 }

    fun indeksQoy(i: Int) { state[INDEX] = i }

    /**
     * İşarədən gələn çalışmanı açır (T5.3): rejim, filtrsiz siyahıda indeks.
     * Bir dəfə — sonra istifadəçinin öz gəzişi qalır (döndərmədə təkrarlanmır).
     */
    fun hedefeKec(topic: ExerciseTopic, calismaId: String) {
        if (state.get<String>(HEDEF_TETBIQ) == calismaId) return
        state[HEDEF_TETBIQ] = calismaId
        val n = topic.nezeri.indexOfFirst { it.id == calismaId }
        val p = topic.praktiki.indexOfFirst { it.id == calismaId }
        when {
            n >= 0 -> { state[REJIM] = Rejim.NEZERI; state[SEVIYYE] = null; state[INDEX] = n }
            p >= 0 -> { state[REJIM] = Rejim.PRAKTIKI; state[SEVIYYE] = null; state[INDEX] = p }
        }
    }

    /* ---------- Praktiki çalışma ---------- */

    /**
     * Çalışmanı redaktora açır və onun kod vəziyyətini qaytarır. Eyni çalışma
     * təkrar açılanda (döndərmə, geri qayıtma) heç nə sıfırlanmır.
     */
    fun calismaniAc(calisma: PracticeExercise): MutableState<TextFieldValue> {
        if (cariCalismaId != calisma.id) {
            cariCalismaId = calisma.id
            state[CALISMA_ID] = calisma.id
            kod.value = TextFieldValue(calisma.starter)
            _yoxlama.value = YoxlamaVeziyyeti.Bos
            state[IPUCU] = false
            state[HELL] = false
            state[CAVAB_SORUSULUR] = false
        }
        return kod
    }

    /**
     * Kodu serverdə işlədib gözlənilən nəticə ilə tutuşdurur. İcra gedərkən
     * ikinci çağırış heçdir — düymə də kilidlidir.
     */
    fun yoxla(calisma: PracticeExercise) {
        if (_yoxlama.value is YoxlamaVeziyyeti.Isleyir) return
        _yoxlama.value = YoxlamaVeziyyeti.Isleyir
        val metn = kod.value.text
        viewModelScope.launch {
            val netice = yoxlamaNeticesi(compiler.isle(metn), calisma.gozlenilen)
            if (netice is YoxlamaVeziyyeti.Duzgun) prefs.calismaniHellIsaretle(calisma.id)
            _yoxlama.value = netice
        }
    }

    /** Nəzəri düzgün cavab və oflayn «Həll etdim» — yazma tək yerdən gedir. */
    fun hellIsaretle(id: String) = viewModelScope.launch { prefs.calismaniHellIsaretle(id) }

    /**
     * Nəzəri çalışmanın cavabı: düzgünsə həll edilmiş sayılır; hər iki halda
     * «Səhvlərim» cədvəli yenilənir — testdəki eyni sualla bir id (T5.2).
     */
    fun nezeriCavab(calisma: TheoryExercise, variant: Int) {
        val duzgun = variant == calisma.a
        if (duzgun) hellIsaretle(calisma.id)
        viewModelScope.launch { oyrenme.cavabYaz(bankSualId(calisma.id), duzgun) }
    }

    fun ipucunuGoster() { state[IPUCU] = true }

    fun cavabSorus() { state[CAVAB_SORUSULUR] = true }

    fun cavabdanImtina() { state[CAVAB_SORUSULUR] = false }

    /** Təsdiqdən sonra model həll redaktora yüklənir və yazılanın üstünü yazır. */
    fun cavabiGoster(calisma: PracticeExercise) {
        kod.value = TextFieldValue(calisma.hell)
        state[HELL] = true
        state[CAVAB_SORUSULUR] = false
    }

    fun sifirla(calisma: PracticeExercise) {
        kod.value = TextFieldValue(calisma.starter)
        _yoxlama.value = YoxlamaVeziyyeti.Bos
        state[HELL] = false
    }

    private companion object {
        const val REJIM = "rejim"
        const val SEVIYYE = "seviyye"
        const val INDEX = "index"
        const val IPUCU = "ipucu"
        const val HELL = "hell"
        const val CAVAB_SORUSULUR = "cavab_sorusulur"
        const val CALISMA_ID = "calisma_id"
        const val KOD = "kod"
        const val METN = "metn"
        const val HEDEF_TETBIQ = "hedef_tetbiq"
    }
}

/**
 * [RunResult] + gözlənilən çıxış → yoxlama nəticəsi. Saf funksiya — testdən
 * birbaşa çağırılır. Hər iki tərəf eyni qaydada normallaşdırılır (sağdakı
 * boşluqlar, kənar boş sətirlər atılır), yoxsa görünməyən boşluq düzgün
 * həlli sındırardı.
 */
internal fun yoxlamaNeticesi(r: RunResult, gozlenilenXam: String): YoxlamaVeziyyeti = when (r) {
    is RunResult.Ok -> {
        val alinan = neticeniNormallasdir(r.output)
        val gozlenilen = neticeniNormallasdir(gozlenilenXam)
        if (alinan == gozlenilen) YoxlamaVeziyyeti.Duzgun(alinan)
        else YoxlamaVeziyyeti.Sehv(gozlenilen, alinan)
    }
    is RunResult.CompileError ->
        YoxlamaVeziyyeti.Xeta(IcraXetasi.KOMPILYASIYA, r.errors.map { XetaSetri(it.mesaj, it.setir) })
    is RunResult.Crashed -> YoxlamaVeziyyeti.Xeta(IcraXetasi.ICRA, listOf(XetaSetri(r.exception)))
    RunResult.Offline -> YoxlamaVeziyyeti.Oflayn
    is RunResult.Failed -> YoxlamaVeziyyeti.Xeta(IcraXetasi.BAGLANTI, xetaSetrleri(r.message))
}
