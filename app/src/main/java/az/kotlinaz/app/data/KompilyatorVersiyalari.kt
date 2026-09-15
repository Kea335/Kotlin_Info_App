package az.kotlinaz.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `api.kotlinlang.org/versions` siyahısındakı bir versiya. */
data class KompilyatorVersiyasi(
    val versiya: String,
    /** Serverin «ən son stabil» kimi işarələdiyi versiya. */
    val latestStable: Boolean = false
)

/**
 * Kompilyator versiyasının seçimi və keş formatı. Saf obyektdir — Context,
 * şəbəkə və DataStore asılılığı yoxdur, ona görə birbaşa unit testdən
 * çağırıla bilər (T1.1). Şəbəkə və keş işi [KotlinCompiler]-dədir.
 *
 * DÜZƏLİŞ: əvvəllər siyahı əl ilə yazılmışdı — `2.1.20`, `2.1.0`, `2.0.20` —
 * və üçü də serverin elan etdiyi siyahıda YOX idi. Server naməlum versiyanı
 * səssizcə ən son stabilə yönləndirir (yoxlanılıb, 2026-09-14):
 *
 *     POST /api/2.1.20/compiler/run  →  println(KotlinVersion.CURRENT)  =  2.4.20
 *     POST /api/9.9.9/compiler/run   →  2.4.20   (uydurma versiya da işləyir!)
 *     POST /api/2.4.10/compiler/run  →  2.4.10   (mövcud versiya — yönləndirmə yoxdur)
 *
 * Yəni kod icrası yalnız bu yönləndirmə sayəsində işləyirdi; JetBrains onu
 * dayandıran kimi üç versiyanın üçü də rədd olunacaqdı. İndi siyahı
 * serverdən oxunur, seçim isə tətbiqin öz Kotlin versiyasına bağlanır.
 */
object KompilyatorVersiyalari {

    /**
     * Tətbiqin öz Kotlin versiyası — stdlib-dən oxunur, əl ilə yazılmır.
     * Dərslərdəki sintaksis bu versiya ilə yoxlanılıb, ona görə icra mühiti
     * də mümkün qədər eyni olmalıdır.
     */
    val TETBIQIN: String = KotlinVersion.CURRENT.toString()

    /**
     * Ehtiyat siyahı — `/versions` alınmayanda (oflayn keş də yoxdur, server
     * 500 qaytarır) işlədilir. Birincisi tətbiqin öz versiyasıdır; qalanları
     * serverin elan etdiyi siyahıdan götürülüb (2026-09-14: latestStable və
     * ondan əvvəlki minor). Server 400/404 qaytarsa növbətisi sınanır.
     */
    val EHTIYAT: List<String> = listOf(TETBIQIN, "2.4.20", "2.3.21").distinct()

    /** Bir sorğuda sınanacaq ən çox versiya sayı — hər sınaq 15 s gözləmə deməkdir. */
    private const val EN_COX_NAMIZED = 3

    // «major.minor.patch» — server siyahısında və keşdə başqa format qəbul edilmir.
    private val FORMAT = Regex("""^(\d+)\.(\d+)\.(\d+)$""")

    /**
     * Sorğuda işlədiləcək versiya:
     *  1. tətbiqin öz versiyası siyahıdadırsa — o;
     *  2. yoxdursa eyni major.minor-un ən yeni patch-i (patch buraxılışları
     *     dil xüsusiyyətini dəyişmir, dərslər onunla da işləyir);
     *  3. o da yoxdursa serverin `latestStable` işarələdiyi;
     *  4. işarə də yoxdursa siyahının birincisi (server ən yenini əvvəl yazır).
     * Boş siyahı üçün null.
     */
    fun sec(siyahi: List<KompilyatorVersiyasi>, tetbiqin: String = TETBIQIN): String? {
        if (siyahi.isEmpty()) return null
        siyahi.firstOrNull { it.versiya == tetbiqin }?.let { return it.versiya }

        val hedef = parcala(tetbiqin)
        if (hedef != null) {
            siyahi.mapNotNull { v -> parcala(v.versiya)?.let { p -> v to p } }
                .filter { (_, p) -> p.first == hedef.first && p.second == hedef.second }
                .maxByOrNull { (_, p) -> p.third }
                ?.let { (v, _) -> return v.versiya }
        }

        return (siyahi.firstOrNull { it.latestStable } ?: siyahi.first()).versiya
    }

    /**
     * Sınanma sırası ilə namizədlər. Siyahı məlumdursa: seçilmiş, sonra
     * `latestStable`, sonra qalanlar (ən çox [EN_COX_NAMIZED]). Siyahı
     * məlum deyilsə (null və ya boş) — [EHTIYAT].
     */
    fun namizedler(
        siyahi: List<KompilyatorVersiyasi>?,
        tetbiqin: String = TETBIQIN
    ): List<String> {
        val secilmis = siyahi?.let { sec(it, tetbiqin) } ?: return EHTIYAT
        val stabil = siyahi.firstOrNull { it.latestStable }?.versiya
        return (listOfNotNull(secilmis, stabil) + siyahi.map { it.versiya })
            .distinct()
            .take(EN_COX_NAMIZED)
    }

    /* ---------- Server cavabı ---------- */

    // Cavabda başqa sahələr də var (redirectCompletion, hasComposeAssets…) —
    // ignoreUnknownKeys onları ötürür, yeni sahə əlavə olunanda oxunuş sınmır.
    @Serializable
    private data class ApiVersion(
        val version: String = "",
        val latestStable: Boolean = false
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * `GET /versions` cavabı: `[{"version":"2.4.20","latestStable":true}, …]`.
     * Formatı pozulmuş girişlər atılır; oxunmayan cavab boş siyahı verir —
     * çağıran tərəf boş siyahını «məlum deyil» sayır.
     */
    fun cavabdanOxu(body: String): List<KompilyatorVersiyasi> = try {
        json.decodeFromString<List<ApiVersion>>(body)
            .filter { FORMAT.matches(it.version) }
            .map { KompilyatorVersiyasi(it.version, it.latestStable) }
    } catch (_: Exception) {
        emptyList()
    }

    /* ---------- «*2.4.20;2.4.10;…» keş formatı ---------- */

    // Ulduz `latestStable` işarəsidir. Tək sətir saxlanılır — UserPrefs-dəki
    // «açar=dəyər;…» rekord formatı ilə eyni prinsip: sxem dəyişmir.
    fun kesdenOxu(xam: String?): List<KompilyatorVersiyasi> {
        if (xam.isNullOrBlank()) return emptyList()
        // Zədəli sətir (yarımçıq yazı, əl ilə redaktə) tətbiqi çökdürməməlidir —
        // oxunmayan girişlər sadəcə atılır.
        return xam.split(';').mapNotNull { giris ->
            val stabil = giris.startsWith('*')
            val versiya = giris.removePrefix("*").trim()
            if (FORMAT.matches(versiya)) KompilyatorVersiyasi(versiya, stabil) else null
        }
    }

    fun keseYaz(siyahi: List<KompilyatorVersiyasi>): String =
        siyahi.joinToString(";") { (if (it.latestStable) "*" else "") + it.versiya }

    private fun parcala(versiya: String): Triple<Int, Int, Int>? {
        val m = FORMAT.matchEntire(versiya) ?: return null
        val (a, b, c) = m.destructured
        return Triple(a.toInt(), b.toInt(), c.toInt())
    }
}
