package az.kotlinaz.app.ui.playground

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import az.kotlinaz.app.data.FakeCompilerRepository
import az.kotlinaz.app.data.RunResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** [PlaygroundViewModel] fake kompilyatorla — şəbəkə yoxdur (T2.2). */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaygroundViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    private fun vm(fake: FakeCompilerRepository) = PlaygroundViewModel(fake, SavedStateHandle())

    @Test
    fun `baslangicda bos kod ve kursor main govdesindedir`() {
        val vm = vm(FakeCompilerRepository())
        assertEquals(MEYDAN_BOS_KOD, vm.kod.value.text)
        assertEquals(MEYDAN_BOS_KOD.length - 2, vm.kod.value.selection.start)
        assertEquals(MeydanVeziyyeti.Bos, vm.ui.value)
        assertNull(vm.seciliPreset.value)
    }

    @Test
    fun `isle redaktordaki kodu gonderir ve cixisi gosterir`() = runTest(dispatcher) {
        val fake = FakeCompilerRepository(RunResult.Ok("salam\n"))
        val vm = vm(fake)
        vm.kod.value = TextFieldValue("fun main() = println(\"salam\")")

        vm.isle()

        assertEquals(listOf("fun main() = println(\"salam\")"), fake.gonderilenKodlar)
        assertEquals(MeydanVeziyyeti.Cixis("salam\n"), vm.ui.value)
    }

    @Test
    fun `icra gederken ikinci isle cagirisi serverə getmir`() = runTest(dispatcher) {
        val qapi = CompletableDeferred<Unit>()
        val fake = FakeCompilerRepository(RunResult.Ok("1"), dayandir = qapi)
        val vm = vm(fake)

        vm.isle()
        assertEquals(MeydanVeziyyeti.Isleyir, vm.ui.value)
        vm.isle() // düymə kilidlidir — ViewModel də ikinci sorğunu buraxmır
        assertEquals(1, fake.cagirisSayi)

        qapi.complete(Unit)
        assertEquals(MeydanVeziyyeti.Cixis("1"), vm.ui.value)
    }

    @Test
    fun `preset secimi kodu evezleyir ve neticeni baglayir`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository(RunResult.Offline))
        vm.isle()
        assertEquals(MeydanVeziyyeti.Oflayn, vm.ui.value)

        vm.presetSec("hello", "println(1)")
        assertEquals("hello", vm.seciliPreset.value)
        assertEquals("println(1)", vm.kod.value.text)
        assertEquals(MeydanVeziyyeti.Bos, vm.ui.value)

        vm.presetSec(null, null)
        assertNull(vm.seciliPreset.value)
        assertEquals(MEYDAN_BOS_KOD, vm.kod.value.text)
    }

    @Test
    fun `xarici kod preset secimini sifirlayir`() {
        val vm = vm(FakeCompilerRepository())
        vm.presetSec("hello", "println(1)")
        vm.xariciKodQoy("val x = 2")
        assertNull(vm.seciliPreset.value)
        assertEquals("val x = 2", vm.kod.value.text)
        assertEquals("val x = 2".length, vm.kod.value.selection.start)
    }

    @Test
    fun `temizle yalniz neticeni baglayir, kod qalir`() = runTest(dispatcher) {
        val vm = vm(FakeCompilerRepository(RunResult.Failed("timeout")))
        vm.kod.value = TextFieldValue("x")
        vm.isle()
        assertIs<MeydanVeziyyeti.Xetalar>(vm.ui.value)

        vm.temizle()
        assertEquals(MeydanVeziyyeti.Bos, vm.ui.value)
        assertEquals("x", vm.kod.value.text)
    }

    @Test
    fun `her RunResult hali panele cevrilir`() {
        assertEquals(MeydanVeziyyeti.Cixis("(çıxış yoxdur)"), meydanNeticesi(RunResult.Ok("  ")))
        assertEquals(
            MeydanVeziyyeti.Xetalar("Kompilyasiya xətası", "a\nb"),
            meydanNeticesi(RunResult.CompileError(listOf("a", "b")))
        )
        assertEquals(
            MeydanVeziyyeti.Xetalar("Çalışma vaxtı xətası", "cixis\nNPE"),
            meydanNeticesi(RunResult.Crashed("cixis", "NPE"))
        )
        assertEquals(
            MeydanVeziyyeti.Xetalar("Çalışma vaxtı xətası", "NPE"),
            meydanNeticesi(RunResult.Crashed("", "NPE"))
        )
        assertEquals(MeydanVeziyyeti.Oflayn, meydanNeticesi(RunResult.Offline))
        assertEquals(
            MeydanVeziyyeti.Xetalar("Bağlantı problemi", "timeout"),
            meydanNeticesi(RunResult.Failed("timeout"))
        )
    }
}
