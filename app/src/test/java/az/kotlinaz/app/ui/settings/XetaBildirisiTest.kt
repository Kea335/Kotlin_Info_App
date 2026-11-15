package az.kotlinaz.app.ui.settings

import org.junit.Test
import java.net.URI
import java.net.URLDecoder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** «Xəta bildir» ünvanı — GitHub-un yeni issue formu, cihaz məlumatı ilə. */
class XetaBildirisiTest {

    private val m = CihazMelumati(
        tetbiq = "1.0.0 (1)",
        android = "16 (API 37)",
        cihaz = "Google Pixel 9a",
        dil = "sistem → az",
        kompilyator = "2.4.10"
    )

    private fun parametrler(url: String): Map<String, String> =
        URI(url).rawQuery.split('&').associate { p ->
            val (a, d) = p.split('=', limit = 2)
            a to URLDecoder.decode(d, "UTF-8")
        }

    @Test
    fun `repo-nun yeni issue formuna aparir`() {
        val url = xetaBildirisiUrl(m)
        assertTrue(url.startsWith("https://github.com/Kea335/Kotlin_Info_App/issues/new?"), url)
        assertEquals("[Xəta] ", parametrler(url)["title"])
    }

    @Test
    fun `govdede cihaz ve versiya melumati var`() {
        val govde = parametrler(xetaBildirisiUrl(m)).getValue("body")
        listOf(
            "| Tətbiq | 1.0.0 (1) |",
            "| Android | 16 (API 37) |",
            "| Cihaz | Google Pixel 9a |",
            "| Dil | sistem → az |",
            "| Kompilyator | Kotlin 2.4.10 |"
        ).forEach { assertTrue(it in govde, "«$it» yoxdur:\n$govde") }
    }

    @Test
    fun `bosluq faizle kodlanir, xususi simvollar qacirilir`() {
        val url = xetaBildirisiUrl(m.copy(cihaz = "A&B=C #1"))
        val sorgu = URI(url).rawQuery
        // «+» GitHub-da başlıqda olduğu kimi qalardı; & və = parametrləri qırardı.
        assertFalse('+' in sorgu, sorgu)
        assertEquals(2, sorgu.count { it == '&' } + 1)
        assertTrue("| Cihaz | A&B=C #1 |" in parametrler(url).getValue("body"))
    }

    @Test
    fun `istehsalci model adinda varsa tekrarlanmir`() {
        assertEquals("Google Pixel 9a", cihazAdi("Google", "Pixel 9a"))
        assertEquals("OnePlus CPH2581", cihazAdi("OnePlus", "OnePlus CPH2581"))
        assertEquals("samsung SM-S918B", cihazAdi("samsung", "SM-S918B"))
    }

    @Test
    fun `mexfilik siyaseti repo-dadir`() {
        assertEquals("https://github.com/Kea335/Kotlin_Info_App/blob/main/PRIVACY.md", MEXFILIK_URL)
    }
}
