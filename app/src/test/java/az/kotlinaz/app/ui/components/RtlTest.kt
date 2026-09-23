package az.kotlinaz.app.ui.components

import az.kotlinaz.app.data.AppDil
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** RTL dəstəyi (T3.6): manifest və inline kodun BiDi təcridi. */
class RtlTest {

    @Test
    fun `RTL dil varsa manifest supportsRtl true-dur`() {
        // Ərəb və ya fars seçiləndə UI güzgülənməlidir — false qalsa hər şey soldan-sağa qalır.
        val manifest = File("src/main/AndroidManifest.xml").readText()
        if (AppDil.entries.any { it.rtl }) {
            assertTrue("android:supportsRtl=\"true\"" in manifest, "manifestdə supportsRtl=\"true\" yoxdur")
        }
    }

    @Test
    fun `RTL-de kod parcasi LRI-PDI ile tecrid olunur`() {
        assertEquals("\u2066val x = 5\u2069", ltrTecrid("val x = 5", rtl = true))
    }

    @Test
    fun `LTR-de kod parcasi deyismir`() {
        assertEquals("val x = 5", ltrTecrid("val x = 5", rtl = false))
    }
}
