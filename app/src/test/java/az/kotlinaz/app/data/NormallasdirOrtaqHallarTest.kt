package az.kotlinaz.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `tools/check-code.js` model həlləri JS-dəki qarşılıqla müqayisə edir (T4.6).
 * İki tətbiq ayrılsa skriptdə «ok» olan həll tətbiqdə «səhv» çıxar — ona görə
 * hər ikisi eyni hal faylından yoxlanılır. Unicode boşluqları (NBSP, U+3000)
 * xüsusilə: Kotlin-in `trimEnd` qaydası JS-in `trimEnd`-indən fərqlidir.
 */
class NormallasdirOrtaqHallarTest {

    @Test
    fun `ortaq hallar Kotlin qaydasi ile eynidir`() {
        val kok = Json.parseToJsonElement(File("../tools/normallasdir-hallar.json").readText())
        val hallar = kok.jsonObject["hallar"]!!.jsonArray
        assertTrue(hallar.size >= 10, "hal faylı boşdur")

        hallar.forEachIndexed { i, h ->
            val o = h.jsonObject
            val giris = o["giris"].let { if (it == null || it is JsonNull) null else it.jsonPrimitive.content }
            val netice = o["netice"]!!.jsonPrimitive.content
            assertEquals(netice, neticeniNormallasdir(giris), "hal #$i: ${giris?.map { "U+%04X".format(it.code) }}")
        }
    }
}
