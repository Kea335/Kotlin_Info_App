package az.kotlinaz.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.math.pow
import kotlin.test.assertTrue

/**
 * Mətn rənglərinin kontrastı (T5.6) — WCAG AA, kiçik yazı: ≥ 4.5:1, bütün
 * fonlarda (əsas, kart, batıq, kod). Palitra dəyişəndə geriləmə buradan görünür.
 */
class KontrastTest {

    private fun kanal(c: Float): Double =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun parlaqliq(c: Color): Double =
        0.2126 * kanal(c.red) + 0.7152 * kanal(c.green) + 0.0722 * kanal(c.blue)

    private fun kontrast(a: Color, b: Color): Double {
        val (x, y) = listOf(parlaqliq(a), parlaqliq(b)).sortedDescending()
        return (x + 0.05) / (y + 0.05)
    }

    private fun yoxla(ad: String, r: KotlinAzColors) {
        val fonlar = mapOf("bg" to r.bg, "bgElev" to r.bgElev, "bgSunken" to r.bgSunken, "bgCode" to r.bgCode)
        val metnler = mapOf(
            "text" to r.text, "textDim" to r.textDim, "textFaint" to r.textFaint,
            "accent" to r.accent, "ok" to r.ok, "warn" to r.warn, "err" to r.err
        )
        metnler.forEach { (m, mr) ->
            fonlar.forEach { (f, fr) ->
                val k = kontrast(mr, fr)
                assertTrue(k >= 4.5, "$ad: $m / $f = %.2f < 4.5".format(k))
            }
        }
    }

    @Test
    fun `isiqli tema WCAG AA`() = yoxla("işıqlı", AciqRengler)

    @Test
    fun `qaranliq tema WCAG AA`() = yoxla("qaranlıq", QaranliqRengler)
}
