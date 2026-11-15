package az.kotlinaz.app.ui.lessons

import az.kotlinaz.app.data.FakeContentRepository
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.YaddasDataStore
import az.kotlinaz.app.data.model.Section
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
import kotlin.test.assertNull

/** [LessonsViewModel] — bölmələr, qruplar (sıra qorunur), son bölmə və «oxundu» (T2.5). */
@OptIn(ExperimentalCoroutinesApi::class)
class LessonsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    private val bolmeler = listOf(
        Section(id = "intro", index = 0, title = "Giriş", group = "Başlanğıc"),
        Section(id = "vars", index = 1, title = "Dəyişənlər", group = "Dilin əsasları"),
        Section(id = "types", index = 2, title = "Tiplər", group = "Dilin əsasları"),
        Section(id = "end", index = 3, title = "Yekun", group = "Yekun")
    )
    private val prefs = UserPrefs(YaddasDataStore())
    // `lazy`: ViewModel yalnız test dispatcher-i qurulandan sonra (@Before) yaranmalıdır.
    private val vm by lazy { LessonsViewModel(FakeContentRepository(bolmeler = bolmeler), prefs) }

    @Test
    fun `bolmeler yuklenir, qruplar saytdaki sira ile duzulur`() = runTest(dispatcher) {
        assertEquals(listOf("intro", "vars", "types", "end"), vm.sections.value.map { it.id })
        assertEquals(listOf("Başlanğıc", "Dilin əsasları", "Yekun"), vm.qruplar.value.keys.toList())
        assertEquals(listOf("vars", "types"), vm.qruplar.value.getValue("Dilin əsasları").map { it.id })
        assertEquals("Tiplər", vm.bolme("types")?.title)
        assertNull(vm.bolme("yoxdur"))
    }

    @Test
    fun `son bolme dersi acanda yazilir, oxundu isaresi de son bolmeni yenileyir`() = runTest(dispatcher) {
        assertNull(vm.sonBolme.value)

        vm.sonBolmeniYaz("vars")
        assertEquals("vars", vm.sonBolme.value)
        assertEquals(emptySet(), prefs.oxunanBolmeler.first())

        vm.bolmeniOxunmusIsaretle("types")
        assertEquals(setOf("types"), prefs.oxunanBolmeler.first())
        assertEquals("types", vm.sonBolme.value)
    }
}
