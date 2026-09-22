package az.kotlinaz.app.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.model.QuizQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Bilik testinin turu — kompozisiyadan kənarda, [SavedStateHandle]-da (T2.3).
 *
 * Əvvəl bütün vəziyyət `remember`-də idi: tətbiq arxa planda öldürülsə 12-ci
 * sualdakı tur sıfırlanırdı. İndi seçilmiş suallar ID siyahısı kimi saxlanılır
 * (shuffle nəticəsi, seed yox — hovuz özü dəyişə bilər), indeks, bal, seçim və
 * «bitdi» bayrağı ilə birlikdə. Proses ölümündən sonra eyni sual, eyni bal,
 * eyni seçim qayıdır.
 *
 * Hovuzlar məzmundan burada qurulur — ekran yalnız render edir.
 */
class QuizViewModel(
    repo: ContentRepository,
    private val prefs: UserPrefs,
    private val state: SavedStateHandle
) : ViewModel() {

    private val _hovuzlar = MutableStateFlow<Map<QuizRejimi, List<QuizQuestion>>>(emptyMap())
    /** Rejim üzrə sual hovuzları; məzmun oxunana qədər boş xəritə. */
    val hovuzlar: StateFlow<Map<QuizRejimi, List<QuizQuestion>>> = _hovuzlar.asStateFlow()

    /** null = rejim seçimi ekranı. */
    val rejim: StateFlow<QuizRejimi?> = state.getStateFlow<QuizRejimi?>(REJIM, null)

    /** Turun sualları — ID siyahısı. `ArrayList`: Bundle-a yazıla bilən tip. */
    private val sualIdleri: StateFlow<ArrayList<String>> =
        state.getStateFlow(SUAL_IDLERI, ArrayList())

    val index: StateFlow<Int> = state.getStateFlow(INDEX, 0)
    val bal: StateFlow<Int> = state.getStateFlow(BAL, 0)
    /** Cari sualda seçilən variant; null = hələ cavab verilməyib. */
    val secim: StateFlow<Int?> = state.getStateFlow<Int?>(SECIM, null)
    val bitdi: StateFlow<Boolean> = state.getStateFlow(BITDI, false)

    /** Rejim üzrə rekordlar: `"junior"` → bal. Rejim kartlarında və nəticədə göstərilir. */
    val rekordlar: StateFlow<Map<String, Int>> =
        prefs.quizRekordlari.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /**
     * Turun sualları, saxlanmış sıra ilə. ID hovuzda yoxdursa (məzmun dəyişib)
     * o sual atılır; belə hal yalnız bərpada mümkündür və [init] onu tutub
     * turu təmiz başladır.
     */
    val suallar: StateFlow<List<QuizQuestion>> =
        combine(hovuzlar, rejim, sualIdleri) { h, r, idler ->
            val idUzre = h[r].orEmpty().associateBy { it.id }
            idler.mapNotNull { idUzre[it] }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Səhv cavab verilən sualların ID-ləri — T5.2 «Səhvlərim» üçün toplanır.
    // TODO(T5.1): Room sxemi gələndə diskə yazılacaq; indilik yalnız yaddaşda.
    private val _sehvler = mutableListOf<String>()
    val sehvIdleri: List<String> get() = _sehvler.toList()

    init {
        // Hovuzlar məzmun dilində qurulur; dil dəyişəndə (T3.3) yenidən. Sual
        // id-ləri dildən asılı deyil, ona görə gedən tur eyni suallarla, yeni
        // dildə davam edir — `suallar` hovuzdan id ilə yığılır.
        viewModelScope.launch {
            repo.dil.collect {
                _hovuzlar.value = hovuzlariQur(repo.quiz().questions, repo.exercises().topics)
                berpaniYoxla()
            }
        }
    }

    /**
     * Bərpa: saxlanmış ID-lərin hamısı hovuzdadırsa tur olduğu kimi davam edir;
     * biri belə yoxdursa (məzmun yenilənib) və ya siyahı boşdursa təmiz başlayır.
     */
    private fun berpaniYoxla() {
        val r = rejim.value ?: return
        val movcud = _hovuzlar.value[r].orEmpty().mapTo(HashSet()) { it.id }
        val idler = sualIdleri.value
        if (idler.isEmpty() || idler.any { it !in movcud }) turaBasla(r)
    }

    /* ---------- Hadisələr ---------- */

    fun rejimSec(r: QuizRejimi) {
        state[REJIM] = r
        turaBasla(r)
    }

    /** Sistemin «geri» jesti və «Səviyyəni dəyiş» — rejim seçiminə qayıdır. */
    fun rejimiDeyis() {
        state[REJIM] = null
        state[SUAL_IDLERI] = ArrayList<String>()
        sifirla()
    }

    /** Hovuzdan təsadüfi [QUIZ_SUAL_SAYI] sual — hər turda yeni dəst. */
    private fun turaBasla(r: QuizRejimi) {
        val hovuz = _hovuzlar.value[r].orEmpty()
        state[SUAL_IDLERI] = ArrayList(hovuz.shuffled().take(QUIZ_SUAL_SAYI).map { it.id })
        sifirla()
    }

    private fun sifirla() {
        state[INDEX] = 0
        state[BAL] = 0
        state[SECIM] = null
        state[BITDI] = false
    }

    /**
     * Variant seçimi. Cavab verildikdən sonra kilidlənir — ikinci toxunuş balı
     * təkrar artıra bilməz (UI-da düymə də sönükdür, bu ikinci qorunmadır).
     */
    fun cavabla(variant: Int) {
        if (secim.value != null || bitdi.value) return
        val sual = suallar.value.getOrNull(index.value) ?: return
        state[SECIM] = variant
        if (variant == sual.a) state[BAL] = bal.value + 1 else _sehvler += sual.id
    }

    /** «Növbəti sual» / «Nəticəyə bax». Cavab verilməyibsə heçdir. */
    fun novbeti() {
        if (secim.value == null || bitdi.value) return
        if (index.value >= suallar.value.size - 1) bitir()
        else {
            state[INDEX] = index.value + 1
            state[SECIM] = null
        }
    }

    // Nəticə yalnız bir dəfə yazılır — `bitdi` false→true keçidində.
    private fun bitir() {
        if (bitdi.value) return
        state[BITDI] = true
        val r = rejim.value ?: return
        val netice = bal.value
        viewModelScope.launch { prefs.quizNeticesiniYaz(r.id, netice) }
    }

    /** «Yenidən başla» — eyni rejimdə yeni təsadüfi dəst. */
    fun yenidenBasla() {
        rejim.value?.let { turaBasla(it) }
    }

    internal companion object {
        const val REJIM = "rejim"
        const val SUAL_IDLERI = "sual_idleri"
        const val INDEX = "index"
        const val BAL = "bal"
        const val SECIM = "secim"
        const val BITDI = "bitdi"
    }
}
