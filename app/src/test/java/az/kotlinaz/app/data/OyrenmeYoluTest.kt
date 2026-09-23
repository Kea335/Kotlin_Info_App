package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.Content
import az.kotlinaz.app.data.model.ExerciseBank
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Öyrənmə yolu (T5.5) real məzmunla: qraf aktivlərlə sinxrondur, dövr yoxdur,
 * tövsiyə sıra ilə irəliləyir, kilid yalnız xəbərdarlıqdır.
 */
class OyrenmeYoluTest {

    private fun resurs(ad: String) =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(ad)).bufferedReader().use { it.readText() }

    private val sections = AktivJson.decodeFromString<Content>(resurs("content/az/content.json")).sections
    private val topics = AktivJson.decodeFromString<ExerciseBank>(resurs("content/az/exercises.json")).topics

    private fun tereqqi(hell: Set<String>) = tereqqiHesabla(sections, topics, hell, emptySet())

    /** Bölmənin bütün çalışmaları — onu tamamlamaq üçün. */
    private fun calismalari(vararg bolme: String): Set<String> =
        topics.filter { it.sectionId in bolme }.flatMap { t -> t.nezeri.map { it.id } + t.praktiki.map { it.id } }.toSet()

    @Test
    fun `path json content json ile sinxrondur`() {
        // Unit test modul qovluğundan işə düşür.
        val path = Json.parseToJsonElement(File("../tools/path.json").readText()).jsonObject["requires"]!!.jsonObject
        val qraf = path.mapValues { (_, v) -> v.jsonArray.map { it.jsonPrimitive.content } }
        val aktivde = sections.filter { it.requires.isNotEmpty() }.associate { it.id to it.requires }
        assertEquals(qraf, aktivde, "tools/path.json dəyişib, amma build-content.js işlədilməyib")
    }

    @Test
    fun `butun asililiqlar movcud bolmelerdir`() {
        val idler = sections.map { it.id }.toSet()
        sections.forEach { s -> s.requires.forEach { assertTrue(it in idler, "${s.id} → $it yoxdur") } }
    }

    @Test
    fun `qrafda dovr yoxdur`() {
        val qraf = sections.associate { it.id to it.requires }
        val hal = HashMap<String, Int>()
        fun gez(id: String, zencir: List<String>) {
            when (hal[id]) {
                2 -> return
                1 -> error("dövr: " + (zencir + id).joinToString(" → "))
            }
            hal[id] = 1
            qraf[id].orEmpty().forEach { gez(it, zencir + id) }
            hal[id] = 2
        }
        qraf.keys.forEach { gez(it, emptyList()) }
    }

    @Test
    fun `yeni istifadeciye sintaksis tovsiye olunur, ilk calismasi ile`() {
        val t = assertNotNull(novbetiAddim(sections, tereqqi(emptySet()), topics, emptySet()))
        assertEquals("sintaksis", t.bolme.id)
        val movzu = topics.first { it.sectionId == "sintaksis" }
        assertEquals(movzu.id, t.movzuId)
        assertEquals(movzu.nezeri.first().id, t.calismaId)
        assertEquals(movzu.nezeri.size + movzu.praktiki.size, t.qalanCalisma)
    }

    @Test
    fun `bolme tamamlananda tovsiye novbetiye kecir`() {
        val hell = calismalari("sintaksis")
        assertEquals("deyisenler", novbetiAddim(sections, tereqqi(hell), topics, hell)?.bolme?.id)
    }

    @Test
    fun `yarimciq bolmede ilk hell edilmemis calisma tovsiye olunur`() {
        val movzu = topics.first { it.sectionId == "sintaksis" }
        val hell = setOf(movzu.nezeri[0].id, movzu.nezeri[1].id)
        val t = novbetiAddim(sections, tereqqi(hell), topics, hell)!!
        assertEquals(movzu.nezeri[2].id, t.calismaId)
    }

    @Test
    fun `kilidli bolme - catismayan asililiq, amma aciq qalan tovsiye`() {
        val t = tereqqi(emptySet())
        val generics = sections.first { it.id == "generics" }
        assertEquals(YolHali.KILIDLI, yolHali(generics, t))
        assertEquals(listOf("xususi-sinifler"), catismayanAsililiqlar(generics, t))
        // Asılılıqsız bölmə heç vaxt kilidli deyil.
        assertEquals(YolHali.ACIQ, yolHali(sections.first { it.id == "giris" }, t))
    }

    @Test
    fun `sirasiz tamamlanan bolme tovsiyeni pozmur`() {
        // İstifadəçi əvvəlcə coroutines-i bitirib — tövsiyə yenə sintaksisdir.
        val hell = calismalari("coroutines")
        val t = tereqqi(hell)
        assertEquals(YolHali.TAMAMLANIB, yolHali(sections.first { it.id == "coroutines" }, t))
        assertEquals("sintaksis", novbetiAddim(sections, t, topics, hell)?.bolme?.id)
    }

    @Test
    fun `butun yol tamamdirsa tovsiye yoxdur`() {
        val hell = topics.flatMap { t -> t.nezeri.map { it.id } + t.praktiki.map { it.id } }.toSet()
        assertNull(novbetiAddim(sections, tereqqi(hell), topics, hell))
    }
}
