package az.kotlinaz.app.data

import org.junit.Test
import kotlin.test.assertEquals

/**
 * `neticeniNormallasdir` — saytdakı `normallasdir()` ilə hərfi-hərfinə eyni
 * olmalıdır; burada o qayda qıfıllanır.
 */
class NormallasdirTest {

    @Test
    fun `CRLF setir sonlari LF-e cevrilir`() {
        assertEquals("a\nb", neticeniNormallasdir("a\r\nb"))
    }

    @Test
    fun `sagdaki bosluqlar atilir`() {
        assertEquals("a\nb", neticeniNormallasdir("a   \nb  "))
    }

    @Test
    fun `soldaki bosluqlar qorunur`() {
        // Yalnız sağ tərəf kəsilir — girinti çıxışın hissəsidir.
        assertEquals("  a\n b", neticeniNormallasdir("  a\n b"))
    }

    @Test
    fun `kenar bos setirler atilir, ortadaki qorunur`() {
        assertEquals("a\n\nb", neticeniNormallasdir("\n\na\n\nb\n\n"))
    }

    @Test
    fun `null bos setir verir`() {
        assertEquals("", neticeniNormallasdir(null))
    }

    @Test
    fun `bos metn bos qalir`() {
        assertEquals("", neticeniNormallasdir(""))
    }

    @Test
    fun `yalniz bosluqdan ibaret metn bos olur`() {
        assertEquals("", neticeniNormallasdir("   \n\t\n  "))
    }

    @Test
    fun `tab da sag bosluq sayilir`() {
        assertEquals("a\nb", neticeniNormallasdir("a\t\nb\t"))
    }

    @Test
    fun `iki defe tetbiq neticeni deyismir`() {
        val bir = neticeniNormallasdir("\r\n x  \r\n\r\n y \r\n")
        assertEquals(bir, neticeniNormallasdir(bir))
    }
}
