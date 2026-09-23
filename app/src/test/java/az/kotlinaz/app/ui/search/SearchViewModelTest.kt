package az.kotlinaz.app.ui.search

import androidx.lifecycle.SavedStateHandle
import az.kotlinaz.app.data.FakeContentRepository
import az.kotlinaz.app.data.model.Paragraph
import az.kotlinaz.app.data.model.Section
import az.kotlinaz.app.data.model.Span
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
import kotlin.test.assertTrue

/** [SearchViewModel] — sorğu `SavedStateHandle`-da, indeks fake məzmundan qurulur (T2.5, T3.5). */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    private fun bolme(id: String, basliq: String, metn: String) =
        Section(id = id, index = 0, title = basliq, blocks = listOf(Paragraph(listOf(Span(Span.TEXT, metn)))))

    private val repo = FakeContentRepository(
        bolmeler = listOf(
            bolme("ns", "Null təhlükəsizliyi", "Elvis operatoru ?: null üçün"),
            bolme("fn", "Funksiyalar", "fun açar sözü")
        )
    )

    private fun vm(r: FakeContentRepository, state: SavedStateHandle = SavedStateHandle()) =
        SearchViewModel(r, state, hesab = dispatcher)

    @Test
    fun `sorgu deyisende neticeler fake indeks uzerinde hesablanir`() = runTest(dispatcher) {
        val vm = vm(repo)
        assertEquals("", vm.sorgu.value)
        assertTrue(vm.neticeler.value.isEmpty())

        vm.sorguDeyisdi("elvis")
        assertEquals("elvis", vm.sorgu.value)
        assertEquals(listOf("ns"), vm.neticeler.value.map { it.id })

        vm.sorguDeyisdi("")
        assertTrue(vm.neticeler.value.isEmpty())
    }

    @Test
    fun `berpa - saxlanmis sorgu ile neticeler derhal gelir`() = runTest(dispatcher) {
        val vm = vm(repo, SavedStateHandle(mapOf("sorgu" to "fun")))
        assertEquals("fun", vm.sorgu.value)
        assertEquals(listOf("fn"), vm.neticeler.value.map { it.id })
    }

    @Test
    fun `dil deyisende indeks hemin dilin bolmelerinden yeniden qurulur`() = runTest(dispatcher) {
        val ikiDilli = FakeContentRepository(
            dilUzreBolmeler = mapOf(
                "az" to listOf(bolme("ns", "Null təhlükəsizliyi", "Elvis operatoru")),
                "en" to listOf(bolme("ns", "Null safety", "The Elvis operator"))
            )
        )
        val vm = vm(ikiDilli, SavedStateHandle(mapOf("sorgu" to "safety")))
        assertTrue(vm.neticeler.value.isEmpty())

        ikiDilli.dilDeyisdi("en")
        assertEquals(listOf("Null safety"), vm.neticeler.value.map { it.title })
    }
}
