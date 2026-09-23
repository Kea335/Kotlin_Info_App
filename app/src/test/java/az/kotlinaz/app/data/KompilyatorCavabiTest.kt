package az.kotlinaz.app.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Kompilyator xidmətinin cavab parseri — `kompilyatorCavabiniOxu` və
 * `cixisiAyir`. Nümunələr api.kotlinlang.org-un real cavablarının
 * formasındadır; şəbəkə yoxdur.
 */
class KompilyatorCavabiTest {

    /* ---------- kompilyatorCavabiniOxu ---------- */

    @Test
    fun `ugurlu icra - outStream acilir`() {
        val n = kompilyatorCavabiniOxu("""{"errors":{"File.kt":[]},"exception":null,"text":"<outStream>Salam\n</outStream>"}""")
        assertEquals(RunResult.Ok("Salam\n"), n)
    }

    @Test
    fun `kompilyasiya xetasi - setir 1-den sayilir`() {
        val n = kompilyatorCavabiniOxu(
            """
            {"errors":{"File.kt":[
              {"interval":{"start":{"line":2,"ch":4},"end":{"line":2,"ch":9}},
               "message":"Unresolved reference: prntln","severity":"ERROR","className":"red_wavy_line"}
            ]},"text":""}
            """
        )
        assertEquals(RunResult.CompileError(listOf("Sətir 3: Unresolved reference: prntln")), n)
    }

    @Test
    fun `xeberdarliq kompilyasiyani sindirmir`() {
        val n = kompilyatorCavabiniOxu(
            """
            {"errors":{"File.kt":[
              {"interval":{"start":{"line":0,"ch":4}},"message":"Variable 'x' is never used","severity":"WARNING"}
            ]},"text":"<outStream>ok</outStream>"}
            """
        )
        assertEquals(RunResult.Ok("ok"), n)
    }

    @Test
    fun `yalniz ERROR saxlanilir, severity boyuk-kicik herfe hessas deyil`() {
        val n = kompilyatorCavabiniOxu(
            """
            {"errors":{"File.kt":[
              {"message":"w","severity":"WARNING"},
              {"interval":{"start":{"line":0}},"message":"e1","severity":"error"},
              {"message":"e2","severity":"ERROR"}
            ]}}
            """
        )
        // Intervalsız xəta sətir nömrəsiz göstərilir.
        assertEquals(RunResult.CompileError(listOf("Sətir 1: e1", "e2")), n)
    }

    @Test
    fun `bir nece fayldaki xetalar birlesdirilir`() {
        val n = kompilyatorCavabiniOxu(
            """{"errors":{"A.kt":[{"message":"a","severity":"ERROR"}],"B.kt":[{"message":"b","severity":"ERROR"}]}}"""
        )
        assertEquals(RunResult.CompileError(listOf("a", "b")), n)
    }

    @Test
    fun `istisna - cixis ve tam ad mesajla`() {
        val n = kompilyatorCavabiniOxu(
            """
            {"errors":{"File.kt":[]},
             "exception":{"message":"/ by zero","fullName":"java.lang.ArithmeticException","stackTrace":[],"cause":null},
             "text":"<outStream>evvel\n</outStream>"}
            """
        )
        assertEquals(RunResult.Crashed("evvel\n", "java.lang.ArithmeticException: / by zero"), n)
    }

    @Test
    fun `istisna - adsiz ve mesajsiz`() {
        val n = kompilyatorCavabiniOxu("""{"exception":{"fullName":"","message":null},"text":""}""")
        assertEquals(RunResult.Crashed("", "Exception"), n)
    }

    @Test
    fun `kompilyasiya xetasi istisnadan ustundur`() {
        val n = kompilyatorCavabiniOxu(
            """{"errors":{"File.kt":[{"message":"e","severity":"ERROR"}]},"exception":{"fullName":"X"}}"""
        )
        assertIs<RunResult.CompileError>(n)
    }

    @Test
    fun `namelum saheler nezere alinmir`() {
        // Server yeni sahə əlavə etsə tətbiq sınmamalıdır.
        val n = kompilyatorCavabiniOxu("""{"text":"<outStream>1</outStream>","jvmByteCode":null,"yeni":{"a":1}}""")
        assertEquals(RunResult.Ok("1"), n)
    }

    @Test
    fun `bos obyekt - bos cixis`() {
        assertEquals(RunResult.Ok(""), kompilyatorCavabiniOxu("{}"))
    }

    @Test
    fun `JSON olmayan cavab - Failed`() {
        val n = kompilyatorCavabiniOxu("<html>502 Bad Gateway</html>")
        assertIs<RunResult.Failed>(n)
        assertTrue(n.message.startsWith("Cavab oxunmadı"), n.message)
    }

    /* ---------- cixisiAyir ---------- */

    @Test
    fun `bos metn - bos`() {
        assertEquals("", cixisiAyir(""))
        assertEquals("", cixisiAyir("  \n"))
    }

    @Test
    fun `etiketsiz metn oldugu kimi qalir`() {
        assertEquals("xam cixis", cixisiAyir("xam cixis"))
    }

    @Test
    fun `bir nece outStream birlesdirilir`() {
        assertEquals("a\nb\n", cixisiAyir("<outStream>a\n</outStream><outStream>b\n</outStream>"))
    }

    @Test
    fun `errStream outStream-dan sonra gelir`() {
        // Sıra etiket növünə görədir, cavabdakı yerə görə yox.
        assertEquals("out\nerr\n", cixisiAyir("<errStream>err\n</errStream><outStream>out\n</outStream>"))
    }

    @Test
    fun `cox setirli cixis qorunur`() {
        assertEquals("1\n2\n\n3", cixisiAyir("<outStream>1\n2\n\n3</outStream>"))
    }

    @Test
    fun `HTML entity acilmir`() {
        // Xidmət mətni qaçırmır — `&lt;` istifadəçinin həqiqi çıxışıdır.
        assertEquals("a & b <tag> &lt;", cixisiAyir("<outStream>a & b <tag> &lt;</outStream>"))
    }
}
