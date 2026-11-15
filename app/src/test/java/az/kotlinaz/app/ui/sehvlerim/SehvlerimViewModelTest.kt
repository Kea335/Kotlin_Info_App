package az.kotlinaz.app.ui.sehvlerim

import az.kotlinaz.app.data.FakeContentRepository
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.QuizQuestion
import az.kotlinaz.app.data.model.TheoryExercise
import az.kotlinaz.app.data.oyrenme.FakeOyrenmeRepository
import az.kotlinaz.app.data.oyrenme.SehvCavab
import az.kotlinaz.app.ui.quiz.bankSualId
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

/** «Səhvlərim» (T5.2): sətirlər məzmundan id ilə, dil dəyişəndə yeni dildə. */
@OptIn(ExperimentalCoroutinesApi::class)
class SehvlerimViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    private fun movzu(sual: String) = ExerciseTopic(
        id = "t", title = "Mövzu",
        nezeri = listOf(TheoryExercise(id = "n1", no = 1, level = "junior", q = sual, opts = listOf("x", "y"), a = 1, exp = ""))
    )
    private val umumi = QuizQuestion(id = "quiz-01", q = "Ümumi", opts = listOf("a", "b"), a = 0, exp = "")

    @Test
    fun `setirler sehv sirasi ile, movzu ve say ile`() {
        val xerite = sualXeritesi(listOf(umumi), listOf(movzu("Bank sualı")))
        val s = sehvSetirleri(
            listOf(SehvCavab(bankSualId("n1"), 3, 0, 20), SehvCavab("quiz-01", 1, 1, 10)),
            xerite
        )
        assertEquals(listOf("Bank sualı", "Ümumi"), s.map { it.sual.q })
        assertEquals(listOf("Mövzu", null), s.map { it.movzu })
        assertEquals(listOf(3, 1), s.map { it.say })
    }

    @Test
    fun `mezmunda olmayan id sessizce atlanir`() {
        val s = sehvSetirleri(listOf(SehvCavab("silinib", 1, 0, 1)), sualXeritesi(listOf(umumi), emptyList()))
        assertTrue(s.isEmpty())
    }

    @Test
    fun `dil deyisende sual yeni dilde gorunur, sehv itmir`() = runTest(dispatcher) {
        val repo = FakeContentRepository(
            movzular = listOf(movzu("Sual AZ")),
            dilUzreMovzular = mapOf("en" to listOf(movzu("Question EN")))
        )
        val oyrenme = FakeOyrenmeRepository()
        val vm = SehvlerimViewModel(repo, oyrenme)
        assertEquals(emptyList(), vm.setirler.value)

        oyrenme.cavabYaz(bankSualId("n1"), duzgun = false)
        assertEquals(listOf("Sual AZ"), vm.setirler.value?.map { it.sual.q })

        repo.dilDeyisdi("en")
        assertEquals(listOf("Question EN"), vm.setirler.value?.map { it.sual.q })
    }

    @Test
    fun `mezmun oxunmasa cokmur, siyahi bos olur`() = runTest(dispatcher) {
        val oyrenme = FakeOyrenmeRepository()
        oyrenme.cavabYaz("quiz-01", duzgun = false)
        val vm = SehvlerimViewModel(FakeContentRepository(calismaXetasi = IllegalStateException("yox")), oyrenme)
        assertEquals(emptyList(), vm.setirler.value)
    }
}
