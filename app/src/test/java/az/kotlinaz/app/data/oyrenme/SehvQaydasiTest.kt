package az.kotlinaz.app.data.oyrenme

import org.junit.Test
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** «Səhvlərim»-in qaydası (T5.1/T5.2) — saf funksiya. */
class SehvQaydasiTest {

    @Test
    fun `ilk sehv - say 1, ardicil 0`() {
        assertEquals(SehvCavab("", 1, 0, 10), sehvYenile(null, duzgun = false, indi = 10))
    }

    @Test
    fun `tekrar sehv - say artir, ardicil sifirlanir`() {
        val s = SehvCavab("q1", 2, 1, 5)
        assertEquals(SehvCavab("q1", 3, 0, 20), sehvYenile(s, duzgun = false, indi = 20))
    }

    @Test
    fun `hec sehv olmayan sualin duzgun cavabi yazilmir`() {
        assertNull(sehvYenile(null, duzgun = true, indi = 1))
    }

    @Test
    fun `iki ardicil duzgun - menimsenilir, say qalir`() {
        val bir = sehvYenile(SehvCavab("q", 3, 0, 1), duzgun = true, indi = 2)!!
        assertFalse(bir.menimsenilib)
        val iki = sehvYenile(bir, duzgun = true, indi = 3)!!
        assertTrue(iki.menimsenilib)
        assertEquals(3, iki.say)
    }

    @Test
    fun `menimsenilmis sual yeni sehvle geri qayidir`() {
        val m = SehvCavab("q", 1, MENIMSEME_HEDDI, 1)
        val s = sehvYenile(m, duzgun = false, indi = 2)!!
        assertFalse(s.menimsenilib)
        assertEquals(2, s.say)
    }

    @Test
    fun `ardicil duzgun hedden yuxari qalxmir`() {
        val m = SehvCavab("q", 1, MENIMSEME_HEDDI, 1)
        assertEquals(MENIMSEME_HEDDI, sehvYenile(m, duzgun = true, indi = 2)!!.ardicilDuzgun)
    }

    @Test
    fun `gun tarixi yerli gunun ISO formasidir`() {
        val c = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 2, 23, 59, 0) }
        assertEquals("2026-11-02", gunTarixi(c.timeInMillis))
    }
}
