package az.kotlinaz.app.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** `xeriteAc` / `xeriteYaz` — DataStore-dakı «açar=dəyər;…» sətir formatı. */
class UserPrefsCodecTest {

    @Test
    fun `normal setir xeriteye acilir`() {
        assertEquals(mapOf("junior" to 12, "middle" to 9), xeriteAc("junior=12;middle=9"))
    }

    @Test
    fun `zedeli cutler atilir, saglam olan qalir`() {
        // "a=" — dəyər yoxdur; "b=x" — rəqəm deyil; "c=3" — sağlam.
        assertEquals(mapOf("c" to 3), xeriteAc("a=;b=x;c=3"))
    }

    @Test
    fun `beraberlik isaresi olmayan parca atilir`() {
        assertEquals(mapOf("k" to 1), xeriteAc("qarisiq;k=1"))
    }

    @Test
    fun `bos acar atilir`() {
        assertEquals(emptyMap(), xeriteAc("=5"))
    }

    @Test
    fun `null bos xerite verir`() {
        assertTrue(xeriteAc(null).isEmpty())
    }

    @Test
    fun `bos setir bos xerite verir`() {
        assertTrue(xeriteAc("").isEmpty())
        assertTrue(xeriteAc("   ").isEmpty())
    }

    @Test
    fun `menfi ve sifir deyerler oxunur`() {
        assertEquals(mapOf("a" to -1, "b" to 0), xeriteAc("a=-1;b=0"))
    }

    @Test
    fun `tekrar acarda sonuncu qalib gelir`() {
        assertEquals(mapOf("a" to 2), xeriteAc("a=1;a=2"))
    }

    @Test
    fun `yazma formati`() {
        assertEquals("junior=12;middle=9", xeriteYaz(linkedMapOf("junior" to 12, "middle" to 9)))
    }

    @Test
    fun `bos xerite bos setir verir`() {
        assertEquals("", xeriteYaz(emptyMap()))
    }

    @Test
    fun `gedis-gelis xeriteni saxlayir`() {
        val m = mapOf("junior" to 12, "middle" to 9, "senior" to 0, "qarisiq" to 15)
        assertEquals(m, xeriteAc(xeriteYaz(m)))
    }
}
