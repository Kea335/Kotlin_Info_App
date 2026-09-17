package az.kotlinaz.app.ui.quiz

import androidx.lifecycle.SavedStateHandle
import az.kotlinaz.app.data.FakeContentRepository
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.YaddasDataStore
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.QuizQuestion
import az.kotlinaz.app.data.model.TheoryExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [QuizViewModel] — turun [SavedStateHandle]-da qorunması (T2.3): bal kilidi,
 * nəticənin bir dəfə yazılması, bərpa (eyni sual/bal/seçim), köhnəlmiş ID-lər,
 * səhvlərin toplanması, «Yenidən başla».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    // 20 junior sual: hər birində düzgün variant `a = i % 3`.
    private val topics = listOf(
        ExerciseTopic(
            id = "t", title = "T",
            nezeri = (1..20).map { i ->
                TheoryExercise(
                    id = "t-$i", no = i, level = "junior", q = "S$i",
                    opts = listOf("A", "B", "C"), a = i % 3, exp = "izah"
                )
            }
        )
    )
    // Qarışıq rejimə də düşən ümumi suallar.
    private val umumi = listOf(QuizQuestion(id = "g-1", q = "G1", opts = listOf("x", "y"), a = 1, exp = ""))

    private val store = YaddasDataStore()
    private val prefs = UserPrefs(store)

    private fun vm(state: SavedStateHandle = SavedStateHandle()) =
        QuizViewModel(FakeContentRepository(movzular = topics, suallar = umumi), prefs, state)

    private fun QuizViewModel.cariSual() = suallar.value[index.value]

    /** Turu sona qədər oynayır: hər sualda `duzgun` ilə cavab verir. */
    private fun QuizViewModel.turuBitir(duzgun: Boolean) {
        while (!bitdi.value) {
            val s = cariSual()
            cavabla(if (duzgun) s.a else (s.a + 1) % s.opts.size)
            novbeti()
        }
    }

    @Test
    fun `rejim secilende hovuzdan 15 sual secilir`() = runTest(dispatcher) {
        val vm = vm()
        assertNull(vm.rejim.value)
        assertEquals(20, vm.hovuzlar.value.getValue(QuizRejimi.JUNIOR).size)

        vm.rejimSec(QuizRejimi.JUNIOR)

        assertEquals(QuizRejimi.JUNIOR, vm.rejim.value)
        assertEquals(QUIZ_SUAL_SAYI, vm.suallar.value.size)
        assertEquals(QUIZ_SUAL_SAYI, vm.suallar.value.map { it.id }.toSet().size)
        assertEquals(0, vm.index.value)
        assertEquals(0, vm.bal.value)
        assertNull(vm.secim.value)
        assertFalse(vm.bitdi.value)
    }

    @Test
    fun `bal ikinci toxunusla artmir`() = runTest(dispatcher) {
        val vm = vm()
        vm.rejimSec(QuizRejimi.JUNIOR)
        val duzgun = vm.cariSual().a

        vm.cavabla(duzgun)
        assertEquals(1, vm.bal.value)
        assertEquals(duzgun, vm.secim.value)

        vm.cavabla(duzgun)          // eyni variant təkrar
        vm.cavabla((duzgun + 1) % 3) // başqa variant
        assertEquals(1, vm.bal.value)
        assertEquals(duzgun, vm.secim.value)
    }

    @Test
    fun `cavab verilmeden novbeti hecdir`() = runTest(dispatcher) {
        val vm = vm()
        vm.rejimSec(QuizRejimi.JUNIOR)
        vm.novbeti()
        assertEquals(0, vm.index.value)
    }

    @Test
    fun `bitdi yalniz bir defe netice yazir`() = runTest(dispatcher) {
        val vm = vm()
        vm.rejimSec(QuizRejimi.JUNIOR)
        vm.turuBitir(duzgun = true)

        assertTrue(vm.bitdi.value)
        assertEquals(QUIZ_SUAL_SAYI, vm.bal.value)
        assertEquals(QUIZ_SUAL_SAYI, prefs.quizRekordlari.first()["junior"])
        val yazma = store.yazmaSayi

        // «Nəticəyə bax» ikinci dəfə basılsa və ya cavab gəlsə — yazı təkrarlanmır.
        vm.novbeti()
        vm.cavabla(0)
        assertEquals(yazma, store.yazmaSayi)
        assertEquals(QUIZ_SUAL_SAYI, vm.bal.value)
    }

    @Test
    fun `sehv cavab verilen id-ler toplanir`() = runTest(dispatcher) {
        val vm = vm()
        vm.rejimSec(QuizRejimi.JUNIOR)
        val s1 = vm.cariSual()
        vm.cavabla((s1.a + 1) % 3)
        vm.novbeti()
        val s2 = vm.cariSual()
        vm.cavabla(s2.a)
        vm.novbeti()
        val s3 = vm.cariSual()
        vm.cavabla((s3.a + 2) % 3)

        assertEquals(listOf(s1.id, s3.id), vm.sehvIdleri)
        assertEquals(1, vm.bal.value)
    }

    @Test
    fun `berpa - eyni suallar, eyni indeks, eyni bal, eyni secim`() = runTest(dispatcher) {
        val birinci = vm()
        birinci.rejimSec(QuizRejimi.JUNIOR)
        repeat(6) { birinci.cavabla(birinci.cariSual().a); birinci.novbeti() }
        birinci.cavabla(birinci.cariSual().a) // 7-ci sualda cavab verilib, keçilməyib
        val idler = birinci.suallar.value.map { it.id }

        // Proses ölümü: SavedStateHandle-dakı dəyərlər yeni ViewModel-ə gəlir.
        val berpa = SavedStateHandle(
            mapOf(
                QuizViewModel.REJIM to QuizRejimi.JUNIOR,
                QuizViewModel.SUAL_IDLERI to ArrayList(idler),
                QuizViewModel.INDEX to 6,
                QuizViewModel.BAL to 7,
                QuizViewModel.SECIM to birinci.secim.value,
                QuizViewModel.BITDI to false
            )
        )
        val ikinci = vm(berpa)

        assertEquals(idler, ikinci.suallar.value.map { it.id })
        assertEquals(6, ikinci.index.value)
        assertEquals(7, ikinci.bal.value)
        assertEquals(birinci.secim.value, ikinci.secim.value)
        assertEquals(birinci.cariSual().id, ikinci.cariSual().id)
        // Bərpadan sonra tur normal davam edir.
        ikinci.novbeti()
        assertEquals(7, ikinci.index.value)
        assertNull(ikinci.secim.value)
    }

    @Test
    fun `sual idleri hovuzda yoxdursa tur temiz baslayir`() = runTest(dispatcher) {
        val berpa = SavedStateHandle(
            mapOf(
                QuizViewModel.REJIM to QuizRejimi.JUNIOR,
                QuizViewModel.SUAL_IDLERI to arrayListOf("q-t-1", "q-kohne-99"),
                QuizViewModel.INDEX to 1,
                QuizViewModel.BAL to 1,
                QuizViewModel.SECIM to 2
            )
        )
        val vm = vm(berpa)

        assertEquals(QuizRejimi.JUNIOR, vm.rejim.value)
        assertEquals(QUIZ_SUAL_SAYI, vm.suallar.value.size)
        assertTrue(vm.suallar.value.none { it.id == "q-kohne-99" })
        assertEquals(0, vm.index.value)
        assertEquals(0, vm.bal.value)
        assertNull(vm.secim.value)
    }

    @Test
    fun `yeniden basla yeni desde verir, bal ve indeks sifirlanir`() = runTest(dispatcher) {
        val vm = vm()
        vm.rejimSec(QuizRejimi.JUNIOR)
        val evvelki = vm.suallar.value.map { it.id }
        vm.turuBitir(duzgun = true)

        // 20-dən 15 seçimin eyni sıra ilə təkrarlanma ehtimalı sıfıra yaxındır;
        // yenə də bir dəfə təsadüfən üst-üstə düşsə ikinci cəhd fərqini göstərir.
        vm.yenidenBasla()
        var sonraki = vm.suallar.value.map { it.id }
        if (sonraki == evvelki) { vm.yenidenBasla(); sonraki = vm.suallar.value.map { it.id } }

        assertNotEquals(evvelki, sonraki)
        assertEquals(QUIZ_SUAL_SAYI, sonraki.size)
        assertEquals(QuizRejimi.JUNIOR, vm.rejim.value)
        assertEquals(0, vm.index.value)
        assertEquals(0, vm.bal.value)
        assertFalse(vm.bitdi.value)
    }

    @Test
    fun `rejimi deyis - secim ekranina qayidir, tur silinir`() = runTest(dispatcher) {
        val vm = vm()
        vm.rejimSec(QuizRejimi.QARISIQ)
        assertEquals(QUIZ_SUAL_SAYI, vm.suallar.value.size)
        vm.cavabla(vm.cariSual().a)

        vm.rejimiDeyis()

        assertNull(vm.rejim.value)
        assertTrue(vm.suallar.value.isEmpty())
        assertEquals(0, vm.bal.value)
        assertNull(vm.secim.value)
    }

    @Test
    fun `hovuz 15-den kicikdirse oldugu qeder sual verilir`() = runTest(dispatcher) {
        val vm = vm()
        vm.rejimSec(QuizRejimi.MIDDLE) // hovuz boşdur — middle sual yoxdur
        assertTrue(vm.suallar.value.isEmpty())

        vm.rejimSec(QuizRejimi.QARISIQ) // 20 + 1 = 21 sual → 15
        assertEquals(QUIZ_SUAL_SAYI, vm.suallar.value.size)
    }
}
