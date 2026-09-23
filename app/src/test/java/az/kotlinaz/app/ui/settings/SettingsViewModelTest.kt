package az.kotlinaz.app.ui.settings

import az.kotlinaz.app.data.AppDil
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.YaddasDataStore
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [SettingsViewModel] — tənzimləmələr yaddaşdakı prefs-ə yazılır və axında görünür (T2.5). */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    private val store = YaddasDataStore()
    private val prefs = UserPrefs(store)
    /** AppCompat-a ötürülən seçimlər — sırayla. */
    private val tetbiqEdilen = mutableListOf<AppDil?>()
    // `lazy`: ViewModel yalnız test dispatcher-i qurulandan sonra (@Before) yaranmalıdır.
    private val vm by lazy { SettingsViewModel(prefs, dilTetbiqEt = { tetbiqEdilen += it }) }

    @Test
    fun `tema srift ve tamamlama yazilir`() = runTest(dispatcher) {
        assertEquals(ThemeMode.SYSTEM, vm.tema.value)
        assertEquals(1, vm.sriftOlcusu.value)
        assertTrue(vm.kodTamamlama.value)

        vm.temaSec(ThemeMode.DARK)
        vm.sriftSec(2)
        vm.tamamlamaSec(false)

        assertEquals(ThemeMode.DARK, vm.tema.value)
        assertEquals(2, vm.sriftOlcusu.value)
        assertFalse(vm.kodTamamlama.value)
    }

    @Test
    fun `rekordlar axinda gorunur`() = runTest(dispatcher) {
        assertTrue(vm.quizRekordlari.value.isEmpty())
        prefs.quizNeticesiniYaz("junior", 12)
        assertEquals(mapOf("junior" to 12), vm.quizRekordlari.value)
    }

    @Test
    fun `sifirlamalar yalniz oz sahesini silir, tema qalir`() = runTest(dispatcher) {
        vm.temaSec(ThemeMode.LIGHT)
        prefs.bolmeniOxunmusIsaretle("intro")
        prefs.calismaniHellIsaretle("intro-t-1")
        prefs.quizNeticesiniYaz("junior", 5)

        vm.oxumaTereqqisiniSifirla()
        assertTrue(prefs.oxunanBolmeler.first().isEmpty())
        assertEquals(setOf("intro-t-1"), prefs.hellEdilmis.first())

        vm.calismaTereqqisiniSifirla()
        assertTrue(prefs.hellEdilmis.first().isEmpty())
        assertEquals(mapOf("junior" to 5), vm.quizRekordlari.value)

        vm.hamisiniSifirla()
        assertTrue(vm.quizRekordlari.value.isEmpty())
        assertEquals(ThemeMode.LIGHT, vm.tema.value)
    }

    /* ---------- Dil (T3.3) ---------- */

    @Test
    fun `dil secimi hem prefs-e, hem sisteme yazilir - sistem dili acari silir`() = runTest(dispatcher) {
        assertNull(vm.dil.value)

        vm.dilSec(AppDil.TR)
        assertEquals(AppDil.TR, vm.dil.value)
        assertEquals(AppDil.TR, prefs.dil.first())
        assertEquals(listOf<AppDil?>(AppDil.TR), tetbiqEdilen)

        vm.dilSec(null)
        assertNull(vm.dil.value)
        assertEquals(listOf(AppDil.TR, null), tetbiqEdilen)
    }

    @Test
    fun `sifirlama dili silmir`() = runTest(dispatcher) {
        vm.dilSec(AppDil.EN)
        vm.hamisiniSifirla()
        assertEquals(AppDil.EN, vm.dil.value)
    }

    @Test
    fun `sistemle uzlasdirma yalniz ferq olanda yazir, AppCompat-a toxunmur`() = runTest(dispatcher) {
        vm.dilSec(AppDil.EN)
        val evvel = store.yazmaSayi

        // Eyni dil — yazı yoxdur.
        vm.sistemleUzlasdir(AppDil.EN)
        assertEquals(evvel, store.yazmaSayi)

        // Sistem tənzimləmələrindən dəyişdirilib — prefs ona uyğunlaşır.
        vm.sistemleUzlasdir(AppDil.RU)
        assertEquals(AppDil.RU, vm.dil.value)
        assertEquals(evvel + 1, store.yazmaSayi)

        // Sistemdə «sistem dili»nə qaytarılıb.
        vm.sistemleUzlasdir(null)
        assertNull(vm.dil.value)

        // Uzlaşdırma AppCompat-a geri yazmır — dövr yaranmasın.
        assertEquals(listOf<AppDil?>(AppDil.EN), tetbiqEdilen)
    }
}
