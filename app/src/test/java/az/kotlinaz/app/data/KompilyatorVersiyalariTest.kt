package az.kotlinaz.app.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `KompilyatorVersiyalari` — T0.2-dəki seçim qaydası və keş formatı. */
class KompilyatorVersiyalariTest {

    private fun v(s: String, stabil: Boolean = false) = KompilyatorVersiyasi(s, stabil)

    // api.kotlinlang.org/versions cavabı, 2026-09-15.
    private val server = listOf(
        v("2.4.20", stabil = true), v("2.4.10"), v("2.4.0"), v("2.3.21"),
        v("2.2.21"), v("2.1.21"), v("2.0.21"), v("1.9.25")
    )

    /* ---------- sec ---------- */

    @Test
    fun `tetbiqin oz versiyasi siyahidadirsa o secilir`() {
        assertEquals("2.4.10", KompilyatorVersiyalari.sec(server, tetbiqin = "2.4.10"))
    }

    @Test
    fun `yoxdursa eyni major minor-un en yeni patch-i`() {
        // 2.4.15 yoxdur → 2.4.x-lərdən ən böyüyü: 2.4.20 (2.4.10 və 2.4.0 deyil).
        assertEquals("2.4.20", KompilyatorVersiyalari.sec(server, tetbiqin = "2.4.15"))
        assertEquals("2.3.21", KompilyatorVersiyalari.sec(server, tetbiqin = "2.3.0"))
    }

    @Test
    fun `major minor da yoxdursa latestStable`() {
        assertEquals("2.4.20", KompilyatorVersiyalari.sec(server, tetbiqin = "2.5.0"))
    }

    @Test
    fun `latestStable isaresi yoxdursa siyahinin birincisi`() {
        val isaresiz = listOf(v("2.1.21"), v("2.0.21"))
        assertEquals("2.1.21", KompilyatorVersiyalari.sec(isaresiz, tetbiqin = "9.9.9"))
    }

    @Test
    fun `bos siyahi null verir`() {
        assertNull(KompilyatorVersiyalari.sec(emptyList(), tetbiqin = "2.4.10"))
    }

    @Test
    fun `tetbiqin versiyasi formatsizdirsa latestStable-e dusur`() {
        assertEquals("2.4.20", KompilyatorVersiyalari.sec(server, tetbiqin = "2.4"))
    }

    /* ---------- namizedler ---------- */

    @Test
    fun `namizedler - secilmis, sonra stabil, sonra qalanlar, en cox 3`() {
        val n = KompilyatorVersiyalari.namizedler(server, tetbiqin = "2.4.10")
        assertEquals(listOf("2.4.10", "2.4.20", "2.4.0"), n)
    }

    @Test
    fun `namizedlerde tekrar yoxdur`() {
        // Seçilmiş = stabil → bir dəfə sayılır.
        val n = KompilyatorVersiyalari.namizedler(server, tetbiqin = "2.4.20")
        assertEquals(listOf("2.4.20", "2.4.10", "2.4.0"), n)
        assertEquals(n.distinct(), n)
    }

    @Test
    fun `siyahi melum deyilse ehtiyat siyahi`() {
        assertEquals(KompilyatorVersiyalari.EHTIYAT, KompilyatorVersiyalari.namizedler(null))
        assertEquals(KompilyatorVersiyalari.EHTIYAT, KompilyatorVersiyalari.namizedler(emptyList()))
    }

    @Test
    fun `ehtiyat siyahinin birincisi tetbiqin oz versiyasidir`() {
        assertEquals(KompilyatorVersiyalari.TETBIQIN, KompilyatorVersiyalari.EHTIYAT.first())
        assertTrue(KompilyatorVersiyalari.EHTIYAT.size in 1..3)
    }

    /* ---------- Server cavabı ---------- */

    @Test
    fun `server cavabi oxunur, namelum saheler oturulur`() {
        val body = """
            [
              { "version": "2.4.20", "latestStable": true, "redirectCompletion": true },
              { "version": "2.4.10", "redirectCompletion": true },
              { "version": "2.2.21", "latestCompletion": true }
            ]
        """.trimIndent()
        assertEquals(
            listOf(v("2.4.20", true), v("2.4.10"), v("2.2.21")),
            KompilyatorVersiyalari.cavabdanOxu(body)
        )
    }

    @Test
    fun `formatsiz versiya girisleri atilir`() {
        val body = """[{"version":"2.4.20"},{"version":"latest"},{"version":"2.4"},{"foo":1}]"""
        assertEquals(listOf(v("2.4.20")), KompilyatorVersiyalari.cavabdanOxu(body))
    }

    @Test
    fun `zedeli JSON bos siyahi verir`() {
        assertTrue(KompilyatorVersiyalari.cavabdanOxu("<html>502</html>").isEmpty())
        assertTrue(KompilyatorVersiyalari.cavabdanOxu("").isEmpty())
        assertTrue(KompilyatorVersiyalari.cavabdanOxu("{}").isEmpty())
    }

    /* ---------- Keş formatı ---------- */

    @Test
    fun `kes yazilisi - ulduz latestStable isaresidir`() {
        assertEquals("*2.4.20;2.4.10;2.3.21", KompilyatorVersiyalari.keseYaz(listOf(v("2.4.20", true), v("2.4.10"), v("2.3.21"))))
    }

    @Test
    fun `kes gedis-gelisi siyahini saxlayir`() {
        assertEquals(server, KompilyatorVersiyalari.kesdenOxu(KompilyatorVersiyalari.keseYaz(server)))
    }

    @Test
    fun `zedeli kes setri cokmur - oxunmayan girisler atilir`() {
        assertEquals(
            listOf(v("2.4.20", true), v("2.4.10")),
            KompilyatorVersiyalari.kesdenOxu("*2.4.20;;abc;2.4.10;*;2.4")
        )
    }

    @Test
    fun `bos ve null kes bos siyahidir`() {
        assertTrue(KompilyatorVersiyalari.kesdenOxu(null).isEmpty())
        assertTrue(KompilyatorVersiyalari.kesdenOxu("").isEmpty())
        assertTrue(KompilyatorVersiyalari.kesdenOxu("  ").isEmpty())
    }
}
