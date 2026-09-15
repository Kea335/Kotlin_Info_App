package az.kotlinaz.app.ui.highlight

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import az.kotlinaz.app.ui.theme.KotlinAzColors
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `highlightKotlin` — leksik parçalama. Hər sintaksis növü fərqli rənglə
 * verilir ki, parçanın hansı qaydaya düşdüyü rəngdən oxuna bilsin.
 */
class HighlightTest {

    private val KEY = Color(0xFF000001)
    private val STR = Color(0xFF000002)
    private val NUM = Color(0xFF000003)
    private val COM = Color(0xFF000004)
    private val FN = Color(0xFF000005)
    private val TYP = Color(0xFF000006)
    private val ANN = Color(0xFF000007)
    private val PUN = Color(0xFF000008)

    private val c = KotlinAzColors(
        bg = Color.White, bgElev = Color.White, bgSunken = Color.White, bgCode = Color.White,
        text = Color.Black, textDim = Color.Black, textFaint = Color.Black,
        border = Color.Black, borderStrong = Color.Black,
        accent = Color.Black, accentSoft = Color.Black,
        ok = Color.Black, warn = Color.Black, err = Color.Black,
        synKey = KEY, synStr = STR, synNum = NUM, synCom = COM,
        synFn = FN, synType = TYP, synAnn = ANN, synPunc = PUN
    )

    private fun rengle(src: String): AnnotatedString = highlightKotlin(src, c)

    /** Verilən rənglə rənglənmiş parçaların mətnləri, sıra ilə. */
    private fun parcalar(src: String, reng: Color): List<String> {
        val a = rengle(src)
        return a.spanStyles.filter { it.item.color == reng }.map { a.text.substring(it.start, it.end) }
    }

    /* ---------- Əsas şərt: uzunluq dəyişmir ---------- */

    @Test
    fun `renglenmis metnin uzunlugu girisle eynidir`() {
        val numuneler = listOf(
            "",
            "fun main() {\n    println(\"Salam, \$ad!\")\n}",
            "/* a /* b */ c */ val x = 0xFF_FFL",
            "\"\"\"raw \"quoted\" \$x\"\"\"",
            "val s = \"a\\\"b\"",
            "@Composable fun A() = 3.14e-5f + 'c' + '\\''",
            "x?.let { it } ?: return@run -1"
        )
        for (n in numuneler) {
            assertEquals(n, rengle(n).text, "mətn dəyişməməlidir: $n")
        }
    }

    @Test
    fun `parcalar bir-birini ortmur ve bosluq buraxmir`() {
        val src = "fun f(x: Int) = x * 2 // qeyd"
        val a = rengle(src)
        val sirali = a.spanStyles.sortedBy { it.start }
        for (i in 1 until sirali.size) {
            assertTrue(sirali[i].start >= sirali[i - 1].end, "parçalar üst-üstə düşməməlidir")
        }
    }

    /* ---------- Mətn ---------- */

    @Test
    fun `raw string tam bir parcadir`() {
        val src = "val s = \"\"\"a \"b\" // c\n d\"\"\" + 1"
        assertEquals(listOf("\"\"\"a \"b\" // c\n d\"\"\""), parcalar(src, STR))
        assertTrue(parcalar(src, COM).isEmpty(), "raw string içindəki // şərh deyil")
    }

    @Test
    fun `sablon ifadeleri ayrica renglenir`() {
        val src = "\"Salam \$ad, \${x.y} !\""
        assertEquals(listOf("\$ad", "\${x.y}"), parcalar(src, FN))
        assertEquals(listOf("\"Salam ", ", ", " !\""), parcalar(src, STR))
    }

    @Test
    fun `ic-ice fiqurlu moterize sablonda sayilir`() {
        val src = "\"\${xs.map { it * 2 }} son\""
        assertEquals(listOf("\${xs.map { it * 2 }}"), parcalar(src, FN))
    }

    @Test
    fun `qacirilmis dirnaq metni vaxtindan evvel bitirmir`() {
        val src = "\"a\\\"b\" + c"
        assertEquals(listOf("\"a\\\"b\""), parcalar(src, STR))
    }

    @Test
    fun `bagli olmayan metn setrin sonuna qeder metndir`() {
        val src = "\"acıq qaldı"
        assertEquals(listOf("\"acıq qaldı"), parcalar(src, STR))
    }

    @Test
    fun `simvol literali metn kimi renglenir`() {
        assertEquals(listOf("'a'", "'\\''"), parcalar("'a' + '\\''", STR))
    }

    /* ---------- Şərhlər ---------- */

    @Test
    fun `ic-ice blok serh duzgun baglanir`() {
        val src = "/* /* a */ b */ val x"
        assertEquals(listOf("/* /* a */ b */"), parcalar(src, COM))
        assertEquals(listOf("val"), parcalar(src, KEY))
    }

    @Test
    fun `setir serhi setrin sonunda bitir`() {
        val src = "val a = 1 // şərh\nval b = 2"
        assertEquals(listOf("// şərh"), parcalar(src, COM))
        assertEquals(listOf("val", "val"), parcalar(src, KEY))
    }

    @Test
    fun `baglanmamis blok serh sona qeder serhdir`() {
        val src = "/* açıq\nval x"
        assertEquals(listOf(src), parcalar(src, COM))
    }

    /* ---------- Sözlər ---------- */

    @Test
    fun `acar sozler, tipler ve funksiya cagirislari`() {
        val src = "fun say(x: Int): String = toStr(x)"
        assertEquals(listOf("fun"), parcalar(src, KEY))
        assertEquals(listOf("Int", "String"), parcalar(src, TYP))
        assertEquals(listOf("say", "toStr"), parcalar(src, FN))
    }

    @Test
    fun `boyuk herfle baslayan her ad tipdir`() {
        assertEquals(listOf("Qutu", "Qutu"), parcalar("class Qutu; val q = Qutu()", TYP))
    }

    @Test
    fun `bosluqdan sonraki moterize de funksiya sayilir`() {
        assertEquals(listOf("println"), parcalar("println (x)", FN))
    }

    @Test
    fun `annotasiya`() {
        assertEquals(listOf("@Composable", "@file.JvmName"), parcalar("@Composable @file.JvmName", ANN))
    }

    /* ---------- Rəqəmlər ---------- */

    @Test
    fun `reqem formalari`() {
        val src = "42 0xFF 1_000_000 3.14e-5 42L 1.5f"
        assertEquals(listOf("42", "0xFF", "1_000_000", "3.14e-5", "42L", "1.5f"), parcalar(src, NUM))
    }

    @Test
    fun `noqteden sonra reqem yoxdursa uzv muracietidir`() {
        assertEquals(listOf("1", "2"), parcalar("1.plus(2)", NUM))
        assertEquals(listOf("plus"), parcalar("1.plus(2)", FN))
    }

    @Test
    fun `menfi isare reqemin hissesi deyil`() {
        assertEquals(listOf("5", "3"), parcalar("5-3", NUM))
        assertEquals(listOf("-"), parcalar("5-3", PUN))
    }

    /* ---------- Punktuasiya ---------- */

    @Test
    fun `ardicil operatorlar tek parcadir`() {
        assertEquals(listOf("?:", "!!", "->", "=="), parcalar("a ?: b !! c -> d == e", PUN))
    }
}
