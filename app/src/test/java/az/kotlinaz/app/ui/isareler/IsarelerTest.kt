package az.kotlinaz.app.ui.isareler

import az.kotlinaz.app.data.FakeContentRepository
import az.kotlinaz.app.data.model.Callout
import az.kotlinaz.app.data.model.CodeBlock
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.HeadingH2
import az.kotlinaz.app.data.model.PracticeExercise
import az.kotlinaz.app.data.model.Section
import az.kotlinaz.app.data.model.TabItem
import az.kotlinaz.app.data.model.TabsBlock
import az.kotlinaz.app.data.model.TheoryExercise
import az.kotlinaz.app.data.oyrenme.Bookmark
import az.kotlinaz.app.data.oyrenme.BookmarkTipi
import az.kotlinaz.app.data.oyrenme.FakeOyrenmeRepository
import az.kotlinaz.app.data.oyrenme.Qeyd
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

/** İşarə və qeydlərin hədəfləri, ekran sətirləri və ViewModel-lər (T5.3). */
@OptIn(ExperimentalCoroutinesApi::class)
class IsarelerTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    private fun bolme(basliq: String) = Section(
        id = "ns", index = 0, title = basliq, group = "Əsaslar",
        blocks = listOf(
            HeadingH2("Giriş", id = "ns.h2.1"),
            CodeBlock(title = "Elvis", code = "val x = a ?: 0", id = "ns.code.1"),
            Callout(title = "Qeyd", blocks = listOf(CodeBlock(code = "\n  println(x)\n", id = "ns.code.2"))),
            TabsBlock(tabs = listOf(TabItem("A", listOf(CodeBlock(code = "val y = 1", id = "ns.code.3")))))
        )
    )

    private fun movzu(sual: String) = ExerciseTopic(
        id = "t", title = "Null",
        nezeri = listOf(TheoryExercise(id = "t-n1", no = 1, level = "junior", q = sual, opts = listOf("a"), a = 0, exp = "")),
        praktiki = listOf(PracticeExercise(id = "t-p1", no = 1, level = "junior", tapsiriq = "Yaz", starter = "", gozlenilen = "", hell = ""))
    )

    /* ---------- Saf funksiyalar ---------- */

    @Test
    fun `hedefler - bolme, ic-ice kod bloklari, calismalar`() {
        val h = hedefXeritesi(listOf(bolme("Null təhlükəsizliyi")), listOf(movzu("Sual?")))
        assertEquals(Hedef(BookmarkTipi.BOLME, "Null təhlükəsizliyi", "Əsaslar", Kecid.Ders("ns")), h["ns"])
        assertEquals(Hedef(BookmarkTipi.KOD, "Elvis", "Null təhlükəsizliyi", Kecid.Ders("ns", "ns.code.1")), h["ns.code.1"])
        // Başlıqsız kod bloku ilk dolu sətri ilə; callout və tab içindəkilər də tapılır.
        assertEquals("println(x)", h["ns.code.2"]?.basliq)
        assertEquals(Kecid.Ders("ns", "ns.code.3"), h["ns.code.3"]?.kecid)
        assertEquals(Kecid.Calisma("t", "t-n1"), h["t-n1"]?.kecid)
        assertEquals("Yaz", h["t-p1"]?.basliq)
    }

    @Test
    fun `hedefi olmayan isare ve qeyd atlanir`() {
        val h = hedefXeritesi(listOf(bolme("B")), emptyList())
        assertEquals(listOf("ns"), isareSetirleri(listOf(Bookmark("ns", BookmarkTipi.BOLME, 1), Bookmark("silinib", BookmarkTipi.KOD, 2)), h).map { it.id })
        assertTrue(qeydSetirleri(listOf(Qeyd("silinib", "mətn", 1)), h).isEmpty())
    }

    @Test
    fun `ust blok indeksi - birbasa ve ic-ice`() {
        val b = bolme("B").blocks
        assertEquals(1, ustBlokIndeksi(b, "ns.code.1"))
        assertEquals(2, ustBlokIndeksi(b, "ns.code.2"))
        assertEquals(3, ustBlokIndeksi(b, "ns.code.3"))
        assertEquals(-1, ustBlokIndeksi(b, "yoxdur"))
    }

    /* ---------- ViewModel-lər ---------- */

    @Test
    fun `isare deyis ve qeyd yaz - bos qeyd silinir`() = runTest(dispatcher) {
        val oyrenme = FakeOyrenmeRepository()
        val vm = IsareViewModel(oyrenme)
        vm.isareDeyis("ns", BookmarkTipi.BOLME)
        assertEquals(setOf("ns"), vm.isareliIdler.value)
        vm.isareDeyis("ns", BookmarkTipi.BOLME)
        assertTrue(vm.isareliIdler.value.isEmpty())

        vm.qeydYaz("ns", "  Vacib  ")
        assertEquals(mapOf("ns" to "Vacib"), vm.qeydler.value)
        vm.qeydYaz("ns", " ")
        assertTrue(vm.qeydler.value.isEmpty())
    }

    @Test
    fun `dil deyisende qeyd metni qalir, hedef basligi yeni dilde`() = runTest(dispatcher) {
        val repo = FakeContentRepository(
            bolmeler = listOf(bolme("Null təhlükəsizliyi")),
            dilUzreBolmeler = mapOf("en" to listOf(bolme("Null safety")))
        )
        val oyrenme = FakeOyrenmeRepository()
        oyrenme.bookmarkDeyis("ns", BookmarkTipi.BOLME)
        oyrenme.qeydYaz("ns", "Mənim qeydim")
        val vm = IsarelerViewModel(repo, oyrenme)
        assertEquals(listOf("Null təhlükəsizliyi"), vm.isareler.value?.map { it.hedef.basliq })

        repo.dilDeyisdi("en")
        assertEquals(listOf("Null safety"), vm.isareler.value?.map { it.hedef.basliq })
        assertEquals(listOf("Mənim qeydim"), vm.qeydler.value?.map { it.metn })
    }
}
