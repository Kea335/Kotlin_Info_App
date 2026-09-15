package az.kotlinaz.app.ui

import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.QuizQuestion
import az.kotlinaz.app.data.model.TheoryExercise
import az.kotlinaz.app.ui.screens.QuizRejimi
import az.kotlinaz.app.ui.screens.hovuzlariQur
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** `hovuzlariQur` — bilik testinin rejim üzrə sual hovuzları. */
class QuizHovuzTest {

    private fun nezeri(id: String, level: String) =
        TheoryExercise(id = id, no = 1, level = level, q = "sual $id", code = null, opts = listOf("a", "b", "c", "d"), a = 2, exp = "izah")

    private val quiz = listOf(
        QuizQuestion(id = "1", q = "ümumi 1", opts = listOf("a", "b", "c", "d"), a = 0, exp = ""),
        QuizQuestion(id = "2", q = "ümumi 2", opts = listOf("a", "b", "c", "d"), a = 1, exp = "")
    )

    private val topics = listOf(
        ExerciseTopic(
            id = "sintaksis", title = "Sintaksis",
            nezeri = listOf(nezeri("sintaksis-t-01", "junior"), nezeri("sintaksis-t-02", "middle"))
        ),
        ExerciseTopic(
            id = "coroutines", title = "Coroutines",
            nezeri = listOf(nezeri("coroutines-t-01", "senior"), nezeri("coroutines-t-02", "junior"))
        )
    )

    private val hovuzlar = hovuzlariQur(quiz, topics)

    @Test
    fun `her rejim ucun hovuz var`() {
        assertEquals(QuizRejimi.entries.toSet(), hovuzlar.keys)
    }

    @Test
    fun `seviyye rejimleri yalniz oz seviyyesinin suallarini alir`() {
        assertEquals(listOf("q-sintaksis-t-01", "q-coroutines-t-02"), hovuzlar.getValue(QuizRejimi.JUNIOR).map { it.id })
        assertEquals(listOf("q-sintaksis-t-02"), hovuzlar.getValue(QuizRejimi.MIDDLE).map { it.id })
        assertEquals(listOf("q-coroutines-t-01"), hovuzlar.getValue(QuizRejimi.SENIOR).map { it.id })
    }

    @Test
    fun `seviyye rejimlerine quiz json suallari dusmur`() {
        for (r in listOf(QuizRejimi.JUNIOR, QuizRejimi.MIDDLE, QuizRejimi.SENIOR)) {
            assertTrue(hovuzlar.getValue(r).none { it.id in quiz.map { q -> q.id } }, "$r")
        }
    }

    @Test
    fun `qarisiq = quiz json + butun bank`() {
        val q = hovuzlar.getValue(QuizRejimi.QARISIQ)
        assertEquals(quiz.size + 4, q.size)
        // quiz.json sualları əvvəldədir, olduğu kimi.
        assertEquals(quiz, q.take(2))
        assertEquals(setOf("q-sintaksis-t-01", "q-sintaksis-t-02", "q-coroutines-t-01", "q-coroutines-t-02"), q.drop(2).map { it.id }.toSet())
    }

    @Test
    fun `bank suali q- onluyu ile gelir ve quiz json id-leri ile toqqusmur`() {
        val q = hovuzlar.getValue(QuizRejimi.QARISIQ)
        assertEquals(q.size, q.map { it.id }.distinct().size)
        assertTrue(q.drop(2).all { it.id.startsWith("q-") })
    }

    @Test
    fun `bank sualinin sahesi olduğu kimi kocurulur`() {
        val e = topics[0].nezeri[0]
        val q = hovuzlar.getValue(QuizRejimi.JUNIOR).first()
        assertEquals(e.q, q.q)
        assertEquals(e.opts, q.opts)
        assertEquals(e.a, q.a)
        assertEquals(e.exp, q.exp)
        assertEquals(e.code, q.code)
    }

    @Test
    fun `namelum seviyye Junior hovuzuna dusur`() {
        val h = hovuzlariQur(emptyList(), listOf(ExerciseTopic(id = "x", title = "x", nezeri = listOf(nezeri("x-1", "expert")))))
        assertEquals(listOf("q-x-1"), h.getValue(QuizRejimi.JUNIOR).map { it.id })
    }

    @Test
    fun `bos giris bos hovuzlar verir, cokmur`() {
        val h = hovuzlariQur(emptyList(), emptyList())
        assertEquals(QuizRejimi.entries.toSet(), h.keys)
        assertTrue(h.values.all { it.isEmpty() })
    }

    @Test
    fun `praktiki calismalar hovuza dusmur`() {
        // Yalnız nəzəri (çoxvariantlı) çalışmalar sual ola bilər.
        val t = ExerciseTopic(
            id = "p", title = "p",
            praktiki = listOf(
                az.kotlinaz.app.data.model.PracticeExercise("p-1", 1, "junior", "", "", "x", null, "")
            )
        )
        val h = hovuzlariQur(emptyList(), listOf(t))
        assertTrue(h.values.all { it.isEmpty() })
    }
}
