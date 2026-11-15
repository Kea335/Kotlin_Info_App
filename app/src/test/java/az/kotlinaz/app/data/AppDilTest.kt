package az.kotlinaz.app.data

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Dil modeli, məzmun dilinin çözülməsi (T3.3) və görünən dillər (T4.7) — saf funksiyalar, Android yoxdur. */
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

    private fun localeConfig(yol: String): List<String> =
        Regex("""android:name="([^"]+)"""").findAll(File(yol).readText()).map { it.groupValues[1] }.toList()

    /**
     * Debug-ın `locales_config.xml`-i (Android 13+ sistem menyusu) modelin
     * eynisi olmalıdır — biri yenilənib o biri unudulsa sistem menyusunda
     * seçilən dil tətbiqdə tanınmaz. Unit test modul qovluğundan işə düşür.
     */
    @Test
    fun `debug locales_config xml AppDil ile eynidir`() {
        assertEquals(AppDil.entries.map { it.teq }, localeConfig("src/debug/res/xml/locales_config.xml"))
    }

    /**
     * Release-in sistem menyusu (T4.7) məhz release məzmun dilləridir, AppDil
     * sırası ilə: yarımçıq dil sistem menyusundan da seçilə bilməməlidir.
     * Hər ikisini `tools/build-locale.js` yazır — uyğunsuzluq onun işlədilmədiyini göstərir.
     */
    @Test
    fun `release locales_config xml release dilleridir`() {
        val release = AktivJson.decodeFromString<LocaleSiyahisi>(
            File("src/main/assets/content/locales.json").readText()
        ).locales
        val gozlenilen = AppDil.entries.map { it.teq }.filter { it in release }
        assertEquals(gozlenilen, localeConfig("src/main/res/xml/locales_config.xml"))
        assertEquals(release.toSet(), gozlenilen.toSet(), "locales.json-da AppDil-də olmayan dil var")
    }

    /* ---------- gorunenDilleriSec (T4.7) ---------- */

    @Test
    fun `release-de yalniz movcud diller ve baza gorunur`() {
        assertEquals(listOf(AppDil.AZ), gorunenDilleriSec(listOf("az"), secim = null, hamisi = false))
        assertEquals(listOf(AppDil.AZ, AppDil.EN), gorunenDilleriSec(listOf("en", "az"), secim = null, hamisi = false))
        // locales.json oxunmayıb (boş) — baza yenə görünür.
        assertEquals(listOf(AppDil.AZ), gorunenDilleriSec(emptyList(), secim = null, hamisi = false))
    }

    @Test
    fun `secilmis dil siyahidan dussede gorunur`() {
        assertEquals(listOf(AppDil.AZ, AppDil.TR), gorunenDilleriSec(listOf("az"), secim = AppDil.TR, hamisi = false))
    }

    @Test
    fun `debug-da butun diller gorunur`() {
        assertEquals(AppDil.entries, gorunenDilleriSec(listOf("az"), secim = null, hamisi = true))
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
