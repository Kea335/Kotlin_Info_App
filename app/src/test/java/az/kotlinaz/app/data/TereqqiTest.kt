package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.model.PracticeExercise
import az.kotlinaz.app.data.model.Section
import az.kotlinaz.app.data.model.TheoryExercise
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** `tereqqiHesabla` — səviyyə qaydaları və bölmə mənimsəməsi. */
class TereqqiTest {

    /* ---------- Kiçik qurucu köməkçilər ---------- */

    private fun bolme(id: String, index: Int = 0) = Section(id = id, index = index, title = id)

    private fun nezeri(id: String, level: String) =
        TheoryExercise(id = id, no = 1, level = level, q = "?", opts = listOf("a", "b", "c", "d"), a = 0, exp = "")

    private fun praktiki(id: String, level: String) =
        PracticeExercise(id = id, no = 1, level = level, tapsiriq = "", starter = "", gozlenilen = "x", hell = "")

    /**
     * Bir mövzu: `n` ədəd `level` səviyyəli nəzəri çalışma. ID-lər
     * `<movzu>-t-1 … -t-n`. `sectionId` verilməsə mövzunun öz id-si.
     */
    private fun movzu(id: String, n: Int, level: String, sectionId: String? = null) = ExerciseTopic(
        id = id,
        title = id,
        sectionId = sectionId ?: id,
        nezeri = (1..n).map { nezeri("$id-t-$it", level) }
    )

    private fun idler(movzu: String, n: Int) = (1..n).map { "$movzu-t-$it" }.toSet()

    /* ---------- Səviyyə keçidləri ---------- */

    // 10 bölmə, hər birində 10 junior çalışma; middle və senior mövzular ayrıca.
    private val bolmeler = (1..10).map { bolme("b$it", it - 1) }
    private val juniorMovzular = (1..10).map { movzu("b$it", 10, "junior") }
    private val middleMovzu = movzu("m", 10, "middle", sectionId = "yox-m")
    private val seniorMovzu = movzu("s", 10, "senior", sectionId = "yox-s")
    private val hamisi = juniorMovzular + middleMovzu + seniorMovzu

    @Test
    fun `yeni istifadeci Junior baslayir`() {
        val t = tereqqiHesabla(bolmeler, hamisi, emptySet(), emptySet())
        assertEquals(Level.JUNIOR, t.seviyye)
        assertEquals(Level.MIDDLE, t.novbetiSeviyye)
        assertEquals(0f, t.dersFaizi)
    }

    @Test
    fun `Middle ucun 80 faiz junior calisma VE 30 faiz ders lazimdir`() {
        // 8 bölmə tam: 80 junior çalışma (80 %) və 8/10 dərs (80 % ≥ 30 %).
        val hell = idler("b1", 10) + idler("b2", 10) + idler("b3", 10) +
            idler("b4", 10) + idler("b5", 10) + idler("b6", 10) +
            idler("b7", 10) + idler("b8", 10)
        val t = tereqqiHesabla(bolmeler, hamisi, hell, emptySet())
        assertEquals(80, t.say(Level.JUNIOR).hell)
        assertEquals(8, t.tamamlananDers)
        assertEquals(Level.MIDDLE, t.seviyye)
    }

    @Test
    fun `yalniz calisma faizi odenirse seviyye qalxmir`() {
        // 80 junior çalışma həll edilib, amma heç bir bölmə TAM deyil (hər birində 8/10).
        val hell = (1..10).flatMap { b -> (1..8).map { "b$b-t-$it" } }.toSet()
        val t = tereqqiHesabla(bolmeler, hamisi, hell, emptySet())
        assertTrue(t.say(Level.JUNIOR).faiz >= SeviyyeQaydasi.CALISMA_HEDEFI)
        assertEquals(0, t.tamamlananDers)
        assertEquals(Level.JUNIOR, t.seviyye)
    }

    @Test
    fun `yalniz ders faizi odenirse seviyye qalxmir`() {
        // 3 bölmə tam (30 %), amma cəmi 30 junior çalışma (30 % < 80 %).
        val hell = idler("b1", 10) + idler("b2", 10) + idler("b3", 10)
        val t = tereqqiHesabla(bolmeler, hamisi, hell, emptySet())
        assertEquals(3, t.tamamlananDers)
        assertEquals(Level.JUNIOR, t.seviyye)
    }

    @Test
    fun `Senior ucun 80 faiz middle calisma ve 60 faiz ders lazimdir`() {
        // 6 bölmə tam (60 % dərs) + 8 middle (80 %). Junior payı Senior şərtinə daxil deyil.
        val hell = (1..6).flatMap { idler("b$it", 10) }.toSet() +
            (1..8).map { "m-t-$it" }
        val t = tereqqiHesabla(bolmeler, hamisi, hell, emptySet())
        assertEquals(Level.SENIOR, t.seviyye)
        assertEquals(null, t.novbetiSeviyye)
    }

    @Test
    fun `Senior serti ders faizi 60-dan azdirsa Middle-de qalir`() {
        // 5 bölmə tam + 5 bölmədə 6/10 = 80 junior (80 %), 50 % dərs, 8 middle —
        // Middle açılır (≥ 30 % dərs), Senior yox (< 60 %).
        val hell = (1..5).flatMap { idler("b$it", 10) }.toSet() +
            (6..10).flatMap { b -> (1..6).map { "b$b-t-$it" } } +
            (1..8).map { "m-t-$it" }
        val t = tereqqiHesabla(bolmeler, hamisi, hell, emptySet())
        assertEquals(80, t.say(Level.JUNIOR).hell)
        assertEquals(5, t.tamamlananDers)
        assertEquals(Level.MIDDLE, t.seviyye)
    }

    @Test
    fun `seviyyeler ardicildir - middle calismalar tek basina Senior acmir`() {
        // Bütün middle həll edilib, amma junior və dərslər boş → Junior.
        val hell = idler("m", 10)
        val t = tereqqiHesabla(bolmeler, hamisi, hell, emptySet())
        assertEquals(Level.JUNIOR, t.seviyye)
    }

    /* ---------- Sərhəd halları ---------- */

    @Test
    fun `bos bank cokmur - Junior, faiz 0`() {
        val t = tereqqiHesabla(emptyList(), emptyList(), emptySet(), emptySet())
        assertEquals(Level.JUNIOR, t.seviyye)
        assertEquals(0f, t.dersFaizi)
        assertEquals(0, t.umumiDers)
        assertEquals(0f, t.say(Level.JUNIOR).faiz)
        assertEquals(0f, t.bolme("yoxdur").faiz)
    }

    @Test
    fun `bolmeler var, calisma yoxdursa ders faizi 0-dir ve seviyye qalxmir`() {
        val t = tereqqiHesabla(bolmeler, emptyList(), emptySet(), setOf("b1", "b2", "b3", "b4", "b5"))
        // 5 bölmə əl ilə oxunub → 50 % dərs, amma junior çalışma umumi = 0 → açılmır.
        assertEquals(5, t.tamamlananDers)
        assertEquals(Level.JUNIOR, t.seviyye)
    }

    @Test
    fun `calismasiz bolme yalniz oxunanlar ile tamamlanir`() {
        val giris = bolme("giris")
        val t1 = tereqqiHesabla(listOf(giris), emptyList(), emptySet(), emptySet())
        val b1 = t1.bolme("giris")
        assertTrue(b1.calismasiz)
        assertFalse(b1.tamamlandi)
        assertEquals(0f, b1.faiz)

        val t2 = tereqqiHesabla(listOf(giris), emptyList(), emptySet(), setOf("giris"))
        val b2 = t2.bolme("giris")
        assertTrue(b2.tamamlandi)
        assertEquals(1f, b2.faiz)
        assertEquals(1, t2.tamamlananDers)
    }

    @Test
    fun `calismali bolmede Oxudum tesdiqi kifayet etmir`() {
        val t = tereqqiHesabla(listOf(bolme("b1")), listOf(movzu("b1", 4, "junior")), emptySet(), setOf("b1"))
        val b = t.bolme("b1")
        assertFalse(b.calismasiz)
        assertFalse(b.tamamlandi)
        assertEquals(0f, b.faiz)
    }

    @Test
    fun `bolme faizi hell edilen calismalarin payidir`() {
        val t = tereqqiHesabla(listOf(bolme("b1")), listOf(movzu("b1", 4, "junior")), setOf("b1-t-1"), emptySet())
        assertEquals(0.25f, t.bolme("b1").faiz)
        assertEquals(1, t.bolme("b1").hell)
        assertEquals(4, t.bolme("b1").umumi)
    }

    @Test
    fun `nezeri ve praktiki calismalar birlikde sayilir`() {
        val m = ExerciseTopic(
            id = "b1", title = "b1",
            nezeri = listOf(nezeri("n1", "junior")),
            praktiki = listOf(praktiki("p1", "junior"), praktiki("p2", "senior"))
        )
        val t = tereqqiHesabla(listOf(bolme("b1")), listOf(m), setOf("n1", "p2"), emptySet())
        assertEquals(3, t.bolme("b1").umumi)
        assertEquals(2, t.bolme("b1").hell)
        assertEquals(SeviyyeSayi(1, 2), t.say(Level.JUNIOR))
        assertEquals(SeviyyeSayi(1, 1), t.say(Level.SENIOR))
    }

    @Test
    fun `namelum level Junior sayilir`() {
        val m = movzu("b1", 3, "expert")
        val t = tereqqiHesabla(listOf(bolme("b1")), listOf(m), setOf("b1-t-1"), emptySet())
        assertEquals(SeviyyeSayi(1, 3), t.say(Level.JUNIOR))
        assertEquals(SeviyyeSayi(0, 0), t.say(Level.MIDDLE))
        assertEquals(Level.JUNIOR, Level.from("expert"))
    }

    @Test
    fun `sectionId verilmeyende movzunun oz id-si bolmeye baglanir`() {
        // ExerciseTopic.sectionId defoltu = id (Models.kt); burada o qıfıllanır.
        val m = ExerciseTopic(id = "b1", title = "b1", nezeri = listOf(nezeri("x", "junior")))
        assertEquals("b1", m.sectionId)
        val t = tereqqiHesabla(listOf(bolme("b1")), listOf(m), emptySet(), emptySet())
        assertEquals(1, t.bolme("b1").umumi)
    }

    @Test
    fun `sectionId ferqli olanda bolme hemin id ile tapilir`() {
        val m = movzu("scope-funksiyalari", 2, "junior", sectionId = "scope")
        val t = tereqqiHesabla(listOf(bolme("scope")), listOf(m), emptySet(), emptySet())
        assertEquals(2, t.bolme("scope").umumi)
        assertEquals(0, t.bolme("scope-funksiyalari").umumi)
    }

    @Test
    fun `bank id-si olmayan hell edilmis id-ler sayilmir`() {
        val t = tereqqiHesabla(listOf(bolme("b1")), listOf(movzu("b1", 2, "junior")), setOf("kohne-id"), emptySet())
        assertEquals(0, t.bolme("b1").hell)
        assertEquals(0, t.say(Level.JUNIOR).hell)
    }

    @Test
    fun `seviyye qaydasi sabitleri`() {
        assertEquals(Level.JUNIOR, SeviyyeQaydasi.onSeviyye(Level.MIDDLE))
        assertEquals(Level.MIDDLE, SeviyyeQaydasi.onSeviyye(Level.SENIOR))
        assertEquals(0f, SeviyyeQaydasi.dersHedefi(Level.JUNIOR))
        assertEquals(0.30f, SeviyyeQaydasi.dersHedefi(Level.MIDDLE))
        assertEquals(0.60f, SeviyyeQaydasi.dersHedefi(Level.SENIOR))
    }
}
