package az.kotlinaz.app.ui.exercises

import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.oyrenme.FakeOyrenmeRepository
import az.kotlinaz.app.data.model.TheoryExercise
import az.kotlinaz.app.ui.quiz.bankSualId
import az.kotlinaz.app.data.KompilyasiyaXetasi
import az.kotlinaz.app.ui.IcraXetasi
import az.kotlinaz.app.ui.XetaSetri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import az.kotlinaz.app.data.FakeCompilerRepository
import az.kotlinaz.app.data.RunResult
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.YaddasDataStore
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.model.PracticeExercise
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [ExerciseViewModel] fake kompilyator və yaddaşdakı prefs ilə (T2.2):
 * altı [RunResult] halı, «həll edildi» yazısı, kilid və çalışma dəyişməsi.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    private val calisma = PracticeExercise(
        id = "intro-p-1", no = 1, level = "junior",
        tapsiriq = "Salam yaz", starter = "fun main() {\n    \n}",
        gozlenilen = "Salam\n", ipucu = "println", hell = "fun main() = println(\"Salam\")"
    )
    private val basqaCalisma = calisma.copy(id = "intro-p-2", starter = "// 2", gozlenilen = "2")

    private val prefs = UserPrefs(YaddasDataStore())
    private val oyrenme = FakeOyrenmeRepository()

    private fun vm(fake: FakeCompilerRepository) = ExerciseViewModel(fake, prefs, oyrenme, SavedStateHandle())

    private suspend fun hellEdilib(id: String) = prefs.hellEdilmis.first().contains(id)

    /* ---------- altı RunResult halı ---------- */

    @Test
    fun `Ok uygun - Duzgun ve hell edilmis isarelenir`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository(RunResult.Ok("Salam  \n\n")))
        vm.calismaniAc(calisma)

        vm.yoxla(calisma)

        assertEquals(YoxlamaVeziyyeti.Duzgun("Salam"), vm.yoxlama.value)
        assertTrue(hellEdilib(calisma.id))
    }

    @Test
    fun `Ok uygunsuz - Sehv ve hell edilmis yazilmir`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository(RunResult.Ok("Sagol")))
        vm.calismaniAc(calisma)

        vm.yoxla(calisma)

        assertEquals(YoxlamaVeziyyeti.Sehv(gozlenilen = "Salam", alinan = "Sagol"), vm.yoxlama.value)
        assertFalse(hellEdilib(calisma.id))
    }

    @Test
    fun `Ok bos cixis - Sehv, alinan bosdur (UI «(boş)» yazir)`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository(RunResult.Ok("")))
        vm.calismaniAc(calisma)
        vm.yoxla(calisma)
        assertEquals(YoxlamaVeziyyeti.Sehv("Salam", ""), vm.yoxlama.value)
    }

    @Test
    fun `CompileError - Xeta paneli setir nomreleri ile`() = runTest(dispatcher) {
        val vm = vm(
            FakeCompilerRepository(
                RunResult.CompileError(listOf(KompilyasiyaXetasi(1, "x"), KompilyasiyaXetasi(null, "y")))
            )
        )
        vm.calismaniAc(calisma)
        vm.yoxla(calisma)
        assertEquals(
            YoxlamaVeziyyeti.Xeta(IcraXetasi.KOMPILYASIYA, listOf(XetaSetri("x", 1), XetaSetri("y"))),
            vm.yoxlama.value
        )
        assertFalse(hellEdilib(calisma.id))
    }

    @Test
    fun `Crashed - calisma vaxti xetasi`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository(RunResult.Crashed("qismen", "ArithmeticException")))
        vm.calismaniAc(calisma)
        vm.yoxla(calisma)
        assertEquals(YoxlamaVeziyyeti.Xeta(IcraXetasi.ICRA, listOf(XetaSetri("ArithmeticException"))), vm.yoxlama.value)
    }

    @Test
    fun `Offline - Oflayn paneli`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository(RunResult.Offline))
        vm.calismaniAc(calisma)
        vm.yoxla(calisma)
        assertEquals(YoxlamaVeziyyeti.Oflayn, vm.yoxlama.value)
        assertFalse(hellEdilib(calisma.id))
    }

    @Test
    fun `Failed - baglanti problemi`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository(RunResult.Failed("timeout")))
        vm.calismaniAc(calisma)
        vm.yoxla(calisma)
        assertEquals(YoxlamaVeziyyeti.Xeta(IcraXetasi.BAGLANTI, listOf(XetaSetri("timeout"))), vm.yoxlama.value)
    }

    /* ---------- davranış ---------- */

    @Test
    fun `yoxla redaktordaki cari kodu gonderir`() = runTest(dispatcher) {
        val fake = FakeCompilerRepository(RunResult.Ok("Salam"))
        val vm = vm(fake)
        vm.calismaniAc(calisma).value = TextFieldValue("fun main() = println(\"Salam\")")

        vm.yoxla(calisma)

        assertEquals(listOf("fun main() = println(\"Salam\")"), fake.gonderilenKodlar)
    }

    @Test
    fun `icra gederken ikinci yoxla cagirisi servere getmir`() = runTest(dispatcher) {
        val qapi = CompletableDeferred<Unit>()
        val fake = FakeCompilerRepository(RunResult.Ok("Salam"), dayandir = qapi)
        val vm = vm(fake)
        vm.calismaniAc(calisma)

        vm.yoxla(calisma)
        assertEquals(YoxlamaVeziyyeti.Isleyir, vm.yoxlama.value)
        vm.yoxla(calisma)
        assertEquals(1, fake.cagirisSayi)

        qapi.complete(Unit)
        assertEquals(YoxlamaVeziyyeti.Duzgun("Salam"), vm.yoxlama.value)
    }

    @Test
    fun `eyni calisma tekrar acilanda kod ve netice qalir, basqasi acilanda sifirlanir`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository(RunResult.Offline))
        val kod = vm.calismaniAc(calisma)
        kod.value = TextFieldValue("yazdigim")
        vm.yoxla(calisma)
        vm.ipucunuGoster()

        // Döndərmə / geri qayıtma: eyni id → heç nə itmir.
        assertSame(kod, vm.calismaniAc(calisma))
        assertEquals("yazdigim", vm.kod.value.text)
        assertEquals(YoxlamaVeziyyeti.Oflayn, vm.yoxlama.value)
        assertTrue(vm.ipucuGorunur.value)

        // «Növbəti»: başqa çalışma → başlanğıc kod, boş panel.
        vm.calismaniAc(basqaCalisma)
        assertEquals("// 2", vm.kod.value.text)
        assertEquals(YoxlamaVeziyyeti.Bos, vm.yoxlama.value)
        assertFalse(vm.ipucuGorunur.value)
    }

    @Test
    fun `cavab axini - sorus, goster, sifirla`() {
        val vm = vm(FakeCompilerRepository())
        vm.calismaniAc(calisma)

        vm.cavabSorus()
        assertTrue(vm.cavabSorusulur.value)
        vm.cavabdanImtina()
        assertFalse(vm.cavabSorusulur.value)
        assertEquals(calisma.starter, vm.kod.value.text)

        vm.cavabSorus()
        vm.cavabiGoster(calisma)
        assertFalse(vm.cavabSorusulur.value)
        assertTrue(vm.hellGorunur.value)
        assertEquals(calisma.hell, vm.kod.value.text)

        vm.sifirla(calisma)
        assertFalse(vm.hellGorunur.value)
        assertEquals(calisma.starter, vm.kod.value.text)
    }

    @Test
    fun `rejim ve seviyye secimi indeksi sifirlayir`() {
        val vm = vm(FakeCompilerRepository())
        vm.indeksQoy(7)
        assertEquals(7, vm.index.value)

        vm.rejimSec(Rejim.PRAKTIKI)
        assertEquals(Rejim.PRAKTIKI, vm.rejim.value)
        assertEquals(0, vm.index.value)

        vm.indeksQoy(3)
        vm.seviyyeSec(Level.SENIOR)
        assertEquals(Level.SENIOR, vm.seviyye.value)
        assertEquals(0, vm.index.value)
    }

    @Test
    fun `hellIsaretle - nezeri duzgun cavab ve oflayn Hell etdim`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository())
        vm.hellIsaretle("intro-t-4")
        assertTrue(hellEdilib("intro-t-4"))
    }

    /* ---------- Nəzəri cavab (T5.2) ---------- */

    private val nezeri = TheoryExercise(
        id = "n-1", no = 1, level = "junior", q = "Sual", opts = listOf("a", "b", "c"), a = 2, exp = ""
    )

    @Test
    fun `nezeri sehv cavab Sehvlerim-e dusur, hell edilmir`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository())
        vm.nezeriCavab(nezeri, 0)
        assertEquals(listOf(bankSualId("n-1")), oyrenme.sehvler.first().map { it.sualId })
        assertFalse(hellEdilib("n-1"))
    }

    @Test
    fun `nezeri duzgun cavab hell edir ve sehvi menimsemeye yaxinlasdirir`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository())
        vm.nezeriCavab(nezeri, 0)
        vm.nezeriCavab(nezeri, 2)
        assertTrue(hellEdilib("n-1"))
        assertEquals(1, oyrenme.sehvler.first().single().ardicilDuzgun)
    }

    /* ---------- İşarədən açılış (T5.3) ---------- */

    @Test
    fun `hedefe kec - praktiki calisma filtrsiz siyahida acilir, bir defe`() {
        val movzu = ExerciseTopic(id = "t", title = "T", nezeri = listOf(nezeri), praktiki = listOf(calisma, calisma.copy(id = "p-2")))
        val vm = vm(FakeCompilerRepository())
        vm.seviyyeSec(Level.SENIOR)
        vm.hedefeKec(movzu, "p-2")
        assertEquals(Rejim.PRAKTIKI, vm.rejim.value)
        assertEquals(null, vm.seviyye.value)
        assertEquals(1, vm.index.value)

        // İkinci çağırış (döndərmə, geri qayıtma) istifadəçinin gəzişini pozmur.
        vm.indeksQoy(0)
        vm.hedefeKec(movzu, "p-2")
        assertEquals(0, vm.index.value)
    }

    /* ---------- Günlük fəallıq (T5.4) ---------- */

    @Test
    fun `fealiyyet yalniz ilk hellde artir`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository())
        vm.nezeriCavab(nezeri, 2)
        vm.nezeriCavab(nezeri, 2)
        vm.hellIsaretle("n-1")
        vm.hellIsaretle("p-yeni")
        assertEquals(2, oyrenme.fealiyyet.first().sumOf { it.hellSayi })
    }
}

