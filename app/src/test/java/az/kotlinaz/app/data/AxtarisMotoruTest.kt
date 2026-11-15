package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.SearchDoc
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** `AxtarisMotoru` — T0.1-dəki İ/ı düzəlişi və dil üzrə normallaşdırma (T3.5) burada qıfıllanır. */
class AxtarisMotoruTest {

    private val docs = listOf(
        SearchDoc(id = "giris", title = "Giriş", group = "Başlanğıc", text = "Kotlin JetBrains tərəfindən yaradılıb."),
        SearchDoc(id = "istisnalar", title = "İstisnalar (exceptions)", group = "Əsas", text = "try və catch blokları ilə xəta tutulur."),
        SearchDoc(id = "idiomlar", title = "İdiomlar və best practice", group = "Əlavə", text = "Qısa və oxunaqlı kod yazmaq üçün."),
        SearchDoc(id = "tipler", title = "Tiplər", group = "Əsas", text = "Int, Long, Double və String əsas tiplərdir. IDE kömək edir."),
        SearchDoc(id = "metn", title = "Mətn", group = "Əsas", text = "Bu bölmədə istisna hallar da izah olunur, işıq sürəti kimi.")
    )

    private val motor = AxtarisMotoru(docs, dilTeqi = "az")

    /* ---------- İ/ı reqressiya ---------- */

    @Test
    fun `kicik i ile yazilan sorgu boyuk I-li basliqlari tapir`() {
        val n = motor.axtar("istisna")
        assertTrue(n.any { it.id == "istisnalar" }, "«istisna» → «İstisnalar» tapılmalıdır")
    }

    @Test
    fun `idiom sorgusu Idiomlar basligini tapir`() {
        assertEquals("idiomlar", motor.axtar("idiom").single().id)
    }

    @Test
    fun `boyuk herfli sorgu da isleyir`() {
        val ids = motor.axtar("İSTİSNA").map { it.id }
        assertTrue("istisnalar" in ids)
    }

    @Test
    fun `isiq ve ısıq eyni neticeni verir`() {
        assertEquals(motor.axtar("işıq").map { it.id }, motor.axtar("ışıq").map { it.id })
        assertEquals(listOf("metn"), motor.axtar("işıq").map { it.id })
    }

    @Test
    fun `kod terminleri boyuk I ile tapilir - Int, IDE`() {
        // az qaydası ilə "Int" → "ınt" olardı; ı→i birləşdirməsi onu «int» ilə tapır.
        assertTrue(motor.axtar("int").any { it.id == "tipler" })
        assertTrue(motor.axtar("ide").any { it.id == "tipler" })
    }

    /* ---------- Sıralama ---------- */

    @Test
    fun `basliqda tapilan metnde tapilandan onde gelir`() {
        // «istisna»: başlıqda «İstisnalar» (bal 0), mətndə «Mətn» bölməsi (bal 1).
        val n = motor.axtar("istisna")
        assertEquals(listOf("istisnalar", "metn"), n.map { it.id })
        assertEquals(0, n[0].bal)
        assertEquals(1, n[1].bal)
    }

    @Test
    fun `beraber ballilar sened sirasini saxlayir`() {
        // «və» hər üç «Əsas» sənədin mətnində var — sıra docs sırasıdır.
        val ids = motor.axtar("və").filter { it.bal == 1 }.map { it.id }
        assertEquals(ids, docs.map { it.id }.filter { it in ids })
    }

    /* ---------- Sərhəd halları ---------- */

    @Test
    fun `bir herflik sorgu bos neticedir`() {
        assertTrue(motor.axtar("a").isEmpty())
        assertTrue(motor.axtar(" a ").isEmpty())
    }

    @Test
    fun `bos sorgu bos neticedir`() {
        assertTrue(motor.axtar("").isEmpty())
        assertTrue(motor.axtar("   ").isEmpty())
    }

    @Test
    fun `tapilmayan sorgu bos neticedir`() {
        assertTrue(motor.axtar("zzzzqq").isEmpty())
    }

    @Test
    fun `sorgunun kenar bosluqlari nezere alinmir`() {
        assertEquals(motor.axtar("istisna").map { it.id }, motor.axtar("  istisna  ").map { it.id })
    }

    @Test
    fun `bos indeks cokmur`() {
        assertTrue(AxtarisMotoru(emptyList()).axtar("kotlin").isEmpty())
    }

    /* ---------- Parça (snippet) ---------- */

    @Test
    fun `parca tapilan sozun etrafindan kesilir`() {
        val uzun = "a".repeat(100) + " HƏDƏF " + "b".repeat(200)
        val m = AxtarisMotoru(listOf(SearchDoc("u", "Uzun", "", uzun)))
        val p = m.axtar("hədəf").single().parca
        assertTrue(p.startsWith("…"), "əvvəl kəsilib: …")
        assertTrue(p.endsWith("…"), "sonra kəsilib: …")
        assertTrue("HƏDƏF" in p)
        // 60 öncə + söz + 90 sonra + iki «…» — bundan uzun ola bilməz.
        assertTrue(p.length <= 60 + "HƏDƏF".length + 90 + 2)
    }

    @Test
    fun `metnin evvelinde tapilanda bas ucbucaq qoyulmur`() {
        val p = motor.axtar("kotlin").single { it.id == "giris" }.parca
        assertFalse(p.startsWith("…"))
        assertFalse(p.endsWith("…"), "qısa mətn tam sığır — son «…» olmamalıdır")
        assertEquals(docs[0].text, p)
    }

    @Test
    fun `yalniz basliqda tapilanda parca metnin evvelidir`() {
        val m = AxtarisMotoru(listOf(SearchDoc("x", "Başlıq Sözü", "", "m".repeat(300))))
        val p = m.axtar("başlıq").single().parca
        assertEquals("m".repeat(140), p)
    }

    @Test
    fun `I herfli metnde parca movqeyi surusmur`() {
        // ROOT locale ilə «İ» iki simvola çevrilirdi və indeks sürüşürdü;
        // az locale ilə parça məhz tapılan sözü əhatə edir.
        val m = AxtarisMotoru(listOf(SearchDoc("x", "X", "", "İİİİİ İİİİİ İstisna burada")))
        val p = m.axtar("istisna").single().parca
        assertTrue("İstisna burada" in p)
    }

    /* ---------- Dil üzrə normallaşdırma (T3.5) ---------- */

    private fun tek(dil: String, basliq: String, metn: String) =
        AxtarisMotoru(listOf(SearchDoc("x", basliq, "", metn)), dilTeqi = dil)

    @Test
    fun `azerbaycan I herfi tapilir`() {
        assertEquals(1, tek("az", "İstisnalar", "").axtar("istisna").size)
        assertEquals(1, tek("az", "Tiplər", "Int tipi").axtar("int").size)
    }

    @Test
    fun `turk dilinde I herfi tapilir`() {
        val m = tek("tr", "İstisnalar ve Int", "Işık hızı")
        assertEquals(1, m.axtar("istisna").size)
        assertEquals(1, m.axtar("int").size)
        // ı/i fərqi nəticəni dəyişmir — klaviaturada hansını yazsa da.
        assertEquals(1, m.axtar("işik").size)
        assertEquals(1, m.axtar("ışık").size)
        assertEquals(1, m.axtar("IŞIK").size)
    }

    @Test
    fun `ereb dilinde hereke fergi neticeye tesir etmir`() {
        // «kitabun» hərəkələrlə; sorğu hərəkəsiz — və əksinə.
        val hereke = "ك\u0650ت\u064Eاب\u064C"
        val m = tek("ar", "Başlıq", "هذا $hereke جديد")
        assertEquals(1, m.axtar("كتاب").size)
        assertEquals(1, tek("ar", "Başlıq", "هذا كتاب جديد").axtar(hereke).size)
        // Tatvil (ـ) və ərəb/fars hərf variantları da birləşir.
        assertEquals(1, tek("fa", "Başlıq", "كتـــاب يك").axtar("کتاب یک").size)
    }

    @Test
    fun `ereb parcasi orijinal mətndən, herekelerle kesilir`() {
        val hereke = "ك\u0650ت\u064Eاب\u064C"
        val metn = "ا".repeat(100) + " " + hereke + " " + "ب".repeat(200)
        val p = tek("ar", "B", metn).axtar("كتاب").single().parca
        assertTrue(hereke in p, "parça hərəkələri saxlamalıdır: " + p)
    }

    @Test
    fun `cince substring tapilir`() {
        val m = tek("zh-CN", "协程", "协程是轻量级线程，可以挂起。")
        assertEquals(1, m.axtar("轻量级").size)
        assertEquals(1, tek("ja", "コルーチン", "コルーチンは軽量なスレッドです").axtar("軽量").size)
        assertEquals(1, tek("ko", "코루틴", "코루틴은 가벼운 스레드입니다").axtar("가벼운").size)
    }

    @Test
    fun `alman dilinde umlaut tapilir`() {
        assertEquals(1, tek("de", "Über Kotlin", "").axtar("über").size)
        // Ayrılmış yazılış (U + birləşən iki nöqtə) NFKC ilə eyni sayılır.
        assertEquals(1, tek("de", "Titel", "U\u0308ber Kotlin").axtar("über").size)
    }

    @Test
    fun `genislenen simvolda parca movqeyi surusmur`() {
        // «ﬁ» (U+FB01) NFKC ilə iki simvola açılır — mövqe xəritəsi sürüşməni düzəldir.
        val metn = "\uFB01".repeat(80) + " HƏDƏF sonra"
        val p = tek("en", "X", metn).axtar("hədəf").single().parca
        assertTrue(p.contains("HƏDƏF sonra"), p)
    }
}
