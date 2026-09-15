package az.kotlinaz.app.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Redaktorun söz tamamlaması: prefiks tapma, təklif sıralaması, tətbiq. */
class TamamlamaTest {

    /* ---------- prefiksArali ---------- */

    @Test
    fun `setir basindaki soz`() {
        assertEquals(0 until 3, prefiksArali("pri", 3))
    }

    @Test
    fun `bosluqdan sonraki soz`() {
        val m = "val x = pri"
        assertEquals(8 until 11, prefiksArali(m, m.length))
    }

    @Test
    fun `yeni setirden sonraki soz`() {
        val m = "fun main() {\n    pri"
        assertEquals(m.length - 3 until m.length, prefiksArali(m, m.length))
    }

    @Test
    fun `reqemle baslayan parca soz deyil`() {
        assertNull(prefiksArali("12ab", 4))
        assertNull(prefiksArali("x = 42", 6))
    }

    @Test
    fun `alt xett soz hissesidir`() {
        assertEquals(0 until 4, prefiksArali("_abc", 4))
    }

    @Test
    fun `kursor bosluqdan sonradirsa null`() {
        assertNull(prefiksArali("val ", 4))
        assertNull(prefiksArali("a.", 2))
    }

    @Test
    fun `kursor 0-da ve ya metnden kenarda null`() {
        assertNull(prefiksArali("abc", 0))
        assertNull(prefiksArali("abc", 4))
        assertNull(prefiksArali("", 0))
    }

    @Test
    fun `kursor sozun ortasindadirsa yalniz sol hisse goturulur`() {
        assertEquals(0 until 2, prefiksArali("print", 2))
    }

    /* ---------- tekliflerTap ---------- */

    @Test
    fun `pri ucun print ve println var, print onde`() {
        val t = tekliflerTap("pri", 3).map { it.anahtar }
        assertTrue("print" in t)
        assertTrue("println" in t)
        assertTrue(t.indexOf("print") < t.indexOf("println"))
    }

    @Test
    fun `va ucun val ve var vararg-dan qabaqdir`() {
        val t = tekliflerTap("va", 2).map { it.anahtar }
        assertTrue(t.indexOf("val") < t.indexOf("vararg"))
        assertTrue(t.indexOf("var") < t.indexOf("vararg"))
    }

    @Test
    fun `boyuk-kicik herf tam uygunluq onde`() {
        // «Str» → String (tam uyğun) startsWith-dən qabaq gəlir.
        val t = tekliflerTap("Str", 3).map { it.anahtar }
        assertEquals("String", t.first())
    }

    @Test
    fun `boyuk-kicik herf ferqi teklifi kesmir`() {
        assertTrue(tekliflerTap("PRINTL", 6).any { it.anahtar == "println" })
    }

    @Test
    fun `metn setri icinde teklif verilmir`() {
        val m = "println(\"pri"
        assertTrue(tekliflerTap(m, m.length).isEmpty())
    }

    @Test
    fun `serh icinde teklif verilmir`() {
        val m = "// pri"
        assertTrue(tekliflerTap(m, m.length).isEmpty())
    }

    @Test
    fun `bagli metnden sonra teklif verilir`() {
        val m = "val s = \"a\"; pri"
        assertTrue(tekliflerTap(m, m.length).isNotEmpty())
    }

    @Test
    fun `qacirilmis dirnaq metni bitirmir`() {
        val m = "val s = \"a\\\"b pri"
        assertTrue(tekliflerTap(m, m.length).isEmpty())
    }

    @Test
    fun `evvelki setirdeki serh cari setri tesir etmir`() {
        val m = "// serh\npri"
        assertTrue(tekliflerTap(m, m.length).isNotEmpty())
    }

    @Test
    fun `prefiks yoxdursa bos siyahi`() {
        assertTrue(tekliflerTap("val ", 4).isEmpty())
        assertTrue(tekliflerTap("", 0).isEmpty())
    }

    @Test
    fun `yazilanin eynisi elavesiz teklif kimi gosterilmir`() {
        // «val» yazılıb — «val» açar sözü təklifdə yer tutmur, «value»/«vararg» qalır.
        val t = tekliflerTap("val", 3)
        assertTrue(t.none { it.anahtar == "val" && it.metn == "val" })
        assertTrue(t.any { it.anahtar == "value" })
    }

    @Test
    fun `main ucun sablon teklif olunur`() {
        val t = tekliflerTap("ma", 2)
        val sablon = t.first { it.anahtar == "main" }
        assertEquals(TamamlamaNovu.SABLON, sablon.nov)
        assertEquals("fun main() {\n    \n}", sablon.metn)
    }

    @Test
    fun `en cox 12 teklif`() {
        // Bir hərflik prefiks onlarla söz tutur — siyahı kəsilməlidir.
        for (p in listOf("a", "c", "s", "t", "i")) {
            assertTrue(tekliflerTap(p, 1).size <= 12, "«$p» üçün ${tekliflerTap(p, 1).size} təklif")
        }
        assertEquals(12, tekliflerTap("s", 1).size)
    }

    /* ---------- tamamlamaniTetbiqEt ---------- */

    private fun deyer(metn: String, kursor: Int = metn.length) = TextFieldValue(metn, TextRange(kursor))

    @Test
    fun `moterizeli funksiyada kursor moterizenin icine dusur`() {
        val teklif = tekliflerTap("printl", 6).first { it.anahtar == "println" }
        val n = tamamlamaniTetbiqEt(deyer("    printl"), teklif)
        assertEquals("    println()", n.text)
        assertEquals("    println(".length, n.selection.start)
        assertTrue(n.selection.collapsed)
    }

    @Test
    fun `sablonda kursor govdenin icine dusur`() {
        val teklif = tekliflerTap("mai", 3).first { it.anahtar == "main" }
        val n = tamamlamaniTetbiqEt(deyer("mai"), teklif)
        assertEquals("fun main() {\n    \n}", n.text)
        assertEquals("fun main() {\n    ".length, n.selection.start)
    }

    @Test
    fun `lambda funksiyasinda kursor fiqurlu moterizenin icindedir`() {
        val teklif = tekliflerTap("forE", 4).first { it.anahtar == "forEach" }
        val n = tamamlamaniTetbiqEt(deyer("xs.forE"), teklif)
        assertEquals("xs.forEach { }", n.text)
        // kursorGeri = 2 → kursor «{»-dən dərhal sonra, boşluqdan əvvəl.
        assertEquals("xs.forEach {".length, n.selection.start)
    }

    @Test
    fun `kursordan sagdaki metn qorunur`() {
        val teklif = tekliflerTap("pri", 3).first { it.anahtar == "print" }
        val m = "pri // qeyd"
        val n = tamamlamaniTetbiqEt(deyer(m, 3), teklif)
        assertEquals("print() // qeyd", n.text)
        assertEquals("print(".length, n.selection.start)
    }

    @Test
    fun `prefiks yoxdursa deyer olduğu kimi qalir`() {
        val teklif = Tamamlama("x", "x", "x()", 1)
        val d = deyer("val ", 4)
        assertEquals(d, tamamlamaniTetbiqEt(d, teklif))
    }
}
