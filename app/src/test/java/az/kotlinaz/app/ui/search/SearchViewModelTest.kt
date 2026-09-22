package az.kotlinaz.app.ui.search

import androidx.lifecycle.SavedStateHandle
import az.kotlinaz.app.data.FakeContentRepository
import az.kotlinaz.app.data.model.SearchDoc
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

/** [SearchViewModel] — sorğu `SavedStateHandle`-da, nəticələr fake indeksdən (T2.5). */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    private val repo = FakeContentRepository(
        senedler = listOf(
            SearchDoc(id = "ns", title = "Null təhlükəsizliyi", text = "Elvis operatoru ?: null üçün"),
            SearchDoc(id = "fn", title = "Funksiyalar", text = "fun açar sözü")
        )
    )

    @Test
    fun `sorgu deyisende neticeler fake indeks uzerinde hesablanir`() = runTest(dispatcher) {
        val vm = SearchViewModel(repo, SavedStateHandle())
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
        val vm = SearchViewModel(repo, SavedStateHandle(mapOf("sorgu" to "fun")))
        assertEquals("fun", vm.sorgu.value)
        assertEquals(listOf("fn"), vm.neticeler.value.map { it.id })
    }

    @Test
    fun `dil deyisende indeks hemin dilin senedlerinden yeniden qurulur`() = runTest(dispatcher) {
        val ikiDilli = FakeContentRepository(
            dilUzreSenedler = mapOf(
                "az" to listOf(SearchDoc(id = "ns", title = "Null təhlükəsizliyi", text = "Elvis operatoru")),
                "en" to listOf(SearchDoc(id = "ns", title = "Null safety", text = "The Elvis operator"))
            )
        )
        val vm = SearchViewModel(ikiDilli, SavedStateHandle(mapOf("sorgu" to "safety")))
        assertTrue(vm.neticeler.value.isEmpty())

        ikiDilli.dilDeyisdi("en")
        assertEquals(listOf("Null safety"), vm.neticeler.value.map { it.title })
    }
}
