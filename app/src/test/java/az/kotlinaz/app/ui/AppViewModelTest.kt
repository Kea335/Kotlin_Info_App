package az.kotlinaz.app.ui

import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.FakeContentRepository
import az.kotlinaz.app.data.KotlinCompiler
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.YaddasDataStore
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.SearchDoc
import az.kotlinaz.app.data.model.Section
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [AppViewModel] fake asılılıqlarla (T2.1): `Context` yoxdur, aktiv yoxdur —
 * ViewModel yalnız [ContentRepository] interfeysini və yaddaşdakı
 * [UserPrefs]-i görür.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    private fun vm(repo: ContentRepository, prefs: UserPrefs = UserPrefs(YaddasDataStore())) =
        AppViewModel(repo, prefs, KotlinCompiler(prefs, sebekeVar = { false }, io = dispatcher))

    @Test
    fun `fake repository ile bolmeler yuklenir ve hazir olur`() = runTest(dispatcher) {
        val repo = FakeContentRepository(
            bolmeler = listOf(Section(id = "intro", index = 0, title = "Giriş")),
            movzular = listOf(ExerciseTopic(id = "intro", title = "Giriş"))
        )
        val vm = vm(repo)

        assertTrue(vm.hazir.value)
        assertNull(vm.yukleneXetasi.value)
        assertEquals(listOf("intro"), vm.sections.value.map { it.id })
        assertNotNull(vm.bolme("intro"))
        assertNotNull(vm.movzu("intro"))
        assertNull(vm.bolme("yoxdur"))
    }

    @Test
    fun `yuklenme xetasi hazir bayragini bloklamir`() = runTest(dispatcher) {
        val vm = vm(FakeContentRepository(xeta = IllegalStateException("aktiv pozulub")))

        // `finally` qaydası: xəta olsa da fırlanğıc bitməlidir, səbəb görünməlidir.
        assertTrue(vm.hazir.value)
        assertEquals("aktiv pozulub", vm.yukleneXetasi.value)
        assertTrue(vm.sections.value.isEmpty())
    }

    @Test
    fun `axtaris fake sened indeksi uzerinde isleyir`() = runTest(dispatcher) {
        val repo = FakeContentRepository(
            senedler = listOf(
                SearchDoc(id = "ns", title = "Null təhlükəsizliyi", text = "Elvis operatoru ?: null üçün"),
                SearchDoc(id = "fn", title = "Funksiyalar", text = "fun açar sözü")
            )
        )
        val vm = vm(repo)

        assertEquals(listOf("ns"), vm.axtar("elvis").map { it.id })
        assertTrue(vm.axtar("").isEmpty())
    }

    @Test
    fun `tenzimleme yaddasdaki prefs-e yazilir ve axinda gorunur`() = runTest(dispatcher) {
        val prefs = UserPrefs(YaddasDataStore())
        val vm = vm(FakeContentRepository(), prefs)

        assertEquals(ThemeMode.SYSTEM, vm.tema.value)
        vm.temaSec(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.tema.value)

        vm.calismaniHellIsaretle("intro-t-1")
        assertEquals(setOf("intro-t-1"), vm.hellEdilmis.value)

        vm.hamisiniSifirla()
        assertTrue(vm.hellEdilmis.value.isEmpty())
    }
}
