package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.Content
import az.kotlinaz.app.data.model.ExerciseBank
import az.kotlinaz.app.data.model.PlaygroundData
import az.kotlinaz.app.data.model.QuizBank
import az.kotlinaz.app.data.model.SearchIndex
import kotlinx.serialization.SerializationException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Aktivlərin smoke testləri — `src/main/assets` test resources kimi
 * qoşulub (build.gradle.kts), decode isə tətbiqin öz [AktivJson] nüsxəsi ilə
 * gedir.
 *
 * Sayılar (32 / 25 / 625 / 15 / 10) BİLƏRƏKDƏN sabitdir: məzmun boru xətti
 * səhvən bölmə itirsə test dərhal qırmızı olur. Məzmun genişlənəndə sayı
 * yeniləmək normaldır — əsas odur ki, itki səssiz qalmasın.
 */
class AktivTest {

    private fun aktiv(ad: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(ad)) { "$ad tapılmadı" }
            .bufferedReader().use { it.readText() }

    // Hər aktiv bir dəfə decode olunur — sinif nüsxəsi hər test üçün yenidir,
    // amma 840 KB JSON üçün bu, saniyənin hissəsidir.
    private val content by lazy { AktivJson.decodeFromString<Content>(aktiv("content.json")) }
    private val bank by lazy { AktivJson.decodeFromString<ExerciseBank>(aktiv("exercises.json")) }
    private val quiz by lazy { AktivJson.decodeFromString<QuizBank>(aktiv("quiz.json")) }
    private val playground by lazy { AktivJson.decodeFromString<PlaygroundData>(aktiv("playground.json")) }
    private val search by lazy { AktivJson.decodeFromString<SearchIndex>(aktiv("search.json")) }

    private val bolmeIdleri get() = content.sections.map { it.id }.toSet()

    /* ---------- Decode ---------- */

    @Test fun `content json decode olunur`() { assertTrue(content.sections.isNotEmpty()) }
    @Test fun `exercises json decode olunur`() { assertTrue(bank.topics.isNotEmpty()) }
    @Test fun `quiz json decode olunur`() { assertTrue(quiz.questions.isNotEmpty()) }
    @Test fun `playground json decode olunur`() { assertTrue(playground.presets.isNotEmpty()) }
    @Test fun `search json decode olunur`() { assertTrue(search.docs.isNotEmpty()) }

    /* ---------- Struktur sabitləri ---------- */

    @Test
    fun `32 bolme, index sahesi movqe ile eynidir`() {
        assertEquals(32, content.sections.size)
        content.sections.forEachIndexed { i, s -> assertEquals(i, s.index, "bölmə ${s.id}") }
        assertEquals(32, bolmeIdleri.size, "bölmə id-ləri unikal deyil")
    }

    @Test
    fun `25 movzu, 625 nezeri ve 625 praktiki calisma`() {
        assertEquals(25, bank.topics.size)
        assertEquals(625, bank.topics.sumOf { it.nezeri.size })
        assertEquals(625, bank.topics.sumOf { it.praktiki.size })
    }

    @Test
    fun `15 quiz suali, 10 meydan numunesi, 32 axtaris senedi`() {
        assertEquals(15, quiz.questions.size)
        assertEquals(10, playground.presets.size)
        assertEquals(32, search.docs.size)
    }

    @Test
    fun `her bolmenin basligi var`() {
        content.sections.forEach { assertTrue(it.title.isNotBlank(), "bölmə ${it.id}") }
    }

    /* ---------- Bütövlük ---------- */

    @Test
    fun `calisma ve quiz id-leri unikaldir`() {
        val idler = bank.topics.flatMap { t -> t.nezeri.map { it.id } + t.praktiki.map { it.id } } +
            quiz.questions.map { it.id }
        val tekrar = idler.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(tekrar.isEmpty(), "təkrar id: $tekrar")
        assertEquals(625 + 625 + 15, idler.size)
    }

    @Test
    fun `her nezeri sual ve quiz sualinda 4 variant, cavab 0-3 arasi`() {
        bank.topics.flatMap { it.nezeri }.forEach {
            assertEquals(4, it.opts.size, it.id)
            assertTrue(it.a in 0 until 4, it.id)
            assertTrue(it.q.isNotBlank() && it.exp.isNotBlank(), it.id)
        }
        quiz.questions.forEach {
            assertEquals(4, it.opts.size, it.id)
            assertTrue(it.a in 0 until 4, it.id)
        }
    }

    @Test
    fun `her praktiki calismada tapsiriq, starter, hell, gozlenilen dolu`() {
        bank.topics.flatMap { it.praktiki }.forEach {
            assertTrue(it.tapsiriq.isNotBlank(), "${it.id}: tapsiriq")
            assertTrue(it.starter.isNotBlank(), "${it.id}: starter")
            assertTrue(it.hell.isNotBlank(), "${it.id}: hell")
            assertTrue(it.gozlenilen.isNotEmpty(), "${it.id}: gozlenilen")
        }
    }

    @Test
    fun `baslangic kodu hell ile eyni deyil - T0 4 reqressiyasi`() {
        val trivial = bank.topics.flatMap { it.praktiki }
            .filter { it.starter.trim() == it.hell.trim() }
            .map { it.id }
        assertTrue(trivial.isEmpty(), "starter == hell: $trivial")
    }

    @Test
    fun `calisma seviyyeleri melum deyerlerdir`() {
        val melum = setOf("junior", "middle", "senior")
        bank.topics.forEach { t ->
            (t.nezeri.map { it.id to it.level } + t.praktiki.map { it.id to it.level }).forEach { (id, lv) ->
                assertTrue(lv in melum, "$id: level=$lv")
            }
        }
    }

    @Test
    fun `her movzunun sectionId-si ucun bolme var`() {
        bank.topics.forEach {
            assertTrue(it.sectionId in bolmeIdleri, "mövzu ${it.id} → bölmə ${it.sectionId} yoxdur")
        }
    }

    @Test
    fun `xususi bolmeler movcuddur`() {
        val xususi = content.sections.filter { it.special != null }.associate { it.id to it.special }
        assertEquals(
            mapOf("kod-meydani" to "playground", "calismalar" to "exercises", "quiz" to "quiz"),
            xususi
        )
    }

    @Test
    fun `axtaris senedlerinin id-leri bolme id-leri ile ust-uste dusur`() {
        assertEquals(bolmeIdleri, search.docs.map { it.id }.toSet())
        search.docs.forEach { assertTrue(it.title.isNotBlank(), it.id) }
    }

    @Test
    fun `meydan numunelerinin id-si unikal, kodu dolu`() {
        assertEquals(playground.presets.size, playground.presets.map { it.id }.distinct().size)
        playground.presets.forEach { assertTrue(it.code.isNotBlank() && it.label.isNotBlank(), it.id) }
    }

    /* ---------- Dözümlülük ---------- */

    @Test
    fun `zedeli JSON istisna atir ve tutulur`() {
        // AppViewModel.init-dəki try/catch ilə eyni yol: istisna tutulur, tətbiq
        // «Məzmun yüklənmədi» göstərir, çökmür.
        assertFailsWith<SerializationException> { AktivJson.decodeFromString<Content>("{") }
        val netice = runCatching { AktivJson.decodeFromString<ExerciseBank>("{\"topics\": [{}]}") }
        assertTrue(netice.isFailure, "id-siz mövzu decode olunmamalıdır")
    }

    @Test
    fun `bos sened cokme yaratmir`() {
        val c = AktivJson.decodeFromString<Content>("{\"sections\":[]}")
        assertTrue(c.sections.isEmpty())
        assertEquals("1.0.0", c.version)
        assertTrue(AktivJson.decodeFromString<ExerciseBank>("{}").topics.isEmpty())
        assertTrue(AktivJson.decodeFromString<SearchIndex>("{}").docs.isEmpty())
    }

    @Test
    fun `namelum sahe atilir, decode sinmir`() {
        val c = AktivJson.decodeFromString<Content>("{\"sections\":[],\"yeniSahe\":1}")
        assertTrue(c.sections.isEmpty())
    }

    @Test
    fun `aktiv versiyasi teyin olunub`() {
        assertNotEquals("", content.version)
    }
}
