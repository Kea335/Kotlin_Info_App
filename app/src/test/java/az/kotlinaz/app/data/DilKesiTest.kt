package az.kotlinaz.app.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Aktiv keşi (T3.7): yaddaşda yalnız cari dilin məzmunu qalır — dil
 * dəyişdikcə keş böyümür, köhnə dilə qayıdanda yenidən oxunur.
 */
class DilKesiTest {

    private var dil = "az"
    private val yuklenen = mutableListOf<String>()
    private val kes = DilKesi({ dil }) { teq -> yuklenen += teq; "məzmun-$teq" }

    @Test
    fun `eyni dilde ikinci muraciet keshden gelir`() = runTest {
        val a = kes.al()
        assertSame(a, kes.al())
        assertEquals(listOf("az"), yuklenen)
    }

    @Test
    fun `5 dil deyisiminden sonra yalniz cari dil saxlanilir`() = runTest {
        listOf("en", "tr", "en", "ru", "az").forEach { dil = it; kes.al() }
        // Hər dəyişmədə yenidən oxunur — köhnə dilə qayıdanda da (keşdə bir yer var).
        assertEquals(listOf("en", "tr", "en", "ru", "az"), yuklenen)
        assertEquals("məzmun-az", kes.al())
        assertEquals(5, yuklenen.size)
    }

    @Test
    fun `eyni anda muracietler bir defe yuklenir`() = runTest {
        val yavas = DilKesi({ "az" }) { teq -> delay(100); yuklenen += teq; teq }
        List(5) { async { yavas.al() } }.awaitAll()
        assertEquals(listOf("az"), yuklenen)
    }
}
