package az.kotlinaz.app.data

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Dil modeli və məzmun dilinin çözülməsi (T3.3) — saf funksiyalar, Android yoxdur. */
class AppDilTest {

    /* ---------- AppDil ---------- */

    @Test
    fun `15 dil, teqler unikaldir, oz adlari bosh deyil`() {
        assertEquals(15, AppDil.entries.size)
        assertEquals(AppDil.entries.size, AppDil.entries.map { it.teq }.toSet().size)
        assertTrue(AppDil.entries.all { it.ozAdi.isNotBlank() })
        assertEquals(setOf(AppDil.AR, AppDil.FA), AppDil.entries.filter { it.rtl }.toSet())
    }

    @Test
    fun `from yalniz tam teqi tanıyir`() {
        assertEquals(AppDil.PT_BR, AppDil.from("pt-BR"))
        assertNull(AppDil.from("pt"))
        assertNull(AppDil.from("en-US"))
        assertNull(AppDil.from(null))
        assertNull(AppDil.from(""))
    }

    @Test
    fun `yaxin dil koduna gore de tapir`() {
        assertEquals(AppDil.EN, AppDil.yaxin("en-US"))
        assertEquals(AppDil.PT_BR, AppDil.yaxin("pt-PT"))
        assertEquals(AppDil.ZH_CN, AppDil.yaxin("zh-Hans-CN"))
        assertEquals(AppDil.HI, AppDil.yaxin("hi-IN"))
        assertNull(AppDil.yaxin("xx"))
        assertNull(AppDil.yaxin(""))
        assertNull(AppDil.yaxin(null))
    }

    /**
     * `res/xml/locales_config.xml` (Android 13+ sistem menyusu) modelin eynisi
     * olmalıdır — biri yenilənib o biri unudulsa sistem menyusunda seçilən dil
     * tətbiqdə tanınmaz. Unit test modul qovluğundan işə düşür.
     */
    @Test
    fun `locales_config xml AppDil ile eynidir`() {
        val xml = File("src/main/res/xml/locales_config.xml").readText()
        val xmldeki = Regex("""android:name="([^"]+)"""").findAll(xml).map { it.groupValues[1] }.toList()
        assertEquals(AppDil.entries.map { it.teq }, xmldeki)
    }

    /* ---------- mezmunDiliSec ---------- */

    private val movcud = listOf("az", "en", "pt-BR")

    @Test
    fun `secim movcuddursa ozu goturulur`() {
        assertEquals("en", mezmunDiliSec("en", sistem = listOf("tr"), movcud = movcud))
        assertEquals("pt-BR", mezmunDiliSec("pt-BR", sistem = emptyList(), movcud = movcud))
    }

    @Test
    fun `secim movcud deyilse en, o da yoxdursa baza`() {
        assertEquals("en", mezmunDiliSec("tr", sistem = listOf("tr"), movcud = movcud))
        assertEquals("az", mezmunDiliSec("tr", sistem = listOf("tr"), movcud = listOf("az")))
    }

    @Test
    fun `sistem dili - siyahi ustunluk sirasi ile gezilir`() {
        assertEquals("en", mezmunDiliSec(null, sistem = listOf("tr-TR", "en-US"), movcud = movcud))
        assertEquals("az", mezmunDiliSec(null, sistem = listOf("az-AZ", "en"), movcud = movcud))
        // Heç biri yoxdur → en.
        assertEquals("en", mezmunDiliSec(null, sistem = listOf("de", "fr"), movcud = movcud))
        // Boş sistem siyahısı çökdürmür.
        assertEquals("en", mezmunDiliSec(null, sistem = emptyList(), movcud = movcud))
    }

    @Test
    fun `dil kodu regionsuz da uygun gelir`() {
        assertEquals("pt-BR", mezmunDiliSec("pt", sistem = emptyList(), movcud = movcud))
        assertEquals("pt-BR", mezmunDiliSec(null, sistem = listOf("pt-PT"), movcud = movcud))
        assertEquals("en", mezmunDiliSec(null, sistem = listOf("en-GB"), movcud = movcud))
    }
}
