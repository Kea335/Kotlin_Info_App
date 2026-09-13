package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.SearchDoc
import java.util.Locale

/**
 * Oflayn tam mətn axtarışı. Saf sinifdir — Context və Compose asılılığı yoxdur,
 * ona görə birbaşa unit testdən çağırıla bilər.
 *
 * DÜZƏLİŞ: kiçik hərfə çevirmə DİL ÜZRƏ aparılır. Kotlin-in parametrsiz
 * `lowercase()`-i Locale.ROOT işlədir və Azərbaycan əlifbası üçün yanlışdır:
 *
 *     "İstifadəçi".lowercase()              → "i̇stifadəçi"  (i + U+0307, 11 simvol)
 *     "İstifadəçi".lowercase(Locale("az"))  → "istifadəçi"   (10 simvol)
 *
 * İstifadəçi klaviaturada adi `i` yazır, ROOT ilə qurulmuş indeksdə isə
 * `i` + birləşən nöqtə var — ona görə «İstisnalar», «İdiomlar» kimi `İ` ilə
 * başlayan heç bir söz tapılmırdı. Bundan əlavə, ROOT-da `İ` iki simvola
 * çevrildiyi üçün tapılan mövqe orijinal mətndəkindən sürüşürdü və parça
 * yanlış yerdən kəsilirdi.
 *
 * Amma təkcə locale kifayət etmir: az/tr qaydası ilə `I` → `ı` olur, məzmun
 * isə Azərbaycan mətni ilə ingilis/Kotlin identifikatorlarının qarışığıdır —
 * `"Int".lowercase(az)` = `"ınt"`, yəni «int» sorğusu `Int`, `IDE`, `API`,
 * `IOException`-ı tapmazdı. Ona görə kiçiltmədən sonra `ı` → `i` birləşdirilir:
 * həm `İstisnalar` → `istisnalar`, həm `Int` → `int`, həm də istifadəçinin
 * `ı` əvəzinə `i` yazması (və əksinə) nəticəni dəyişmir.
 *
 * `dilTeqi` hələlik sabit «az»-dır; dil seçimi gələndə seçilmiş dildən gələcək.
 */
class AxtarisMotoru(docs: List<SearchDoc>, dilTeqi: String = "az") {

    private val locale: Locale = Locale.forLanguageTag(dilTeqi)

    // Hər iki addım simvol sayını saxlayır (İ→i, I→ı, ı→i — hamısı 1:1), ona
    // görə kiçik mətndə tapılan mövqe orijinal mətnə birbaşa tətbiq oluna bilir.
    private fun kicilt(s: String): String = s.lowercase(locale).replace('ı', 'i')

    // Axtarış indeksi ~120 KB mətndir. Kiçik hərfli surət bir dəfə, sinif
    // yaranarkən hazırlanır; `axtar()` yalnız hazır sətirlərdə axtarır — hər
    // hərf vuruşunda 32 sənədi yenidən kiçiltmək lazım gəlmir.
    private data class Sened(
        val doc: SearchDoc,
        val basliqKicik: String,
        val metnKicik: String
    )

    private val indeks: List<Sened> =
        docs.map { Sened(it, kicilt(it.title), kicilt(it.text)) }

    fun axtar(sorgu: String): List<SearchNetice> {
        val q = kicilt(sorgu.trim())
        // Bir hərflik sorğu bütün sənədləri qaytarardı — mənasızdır.
        if (q.length < 2) return emptyList()

        return indeks.mapNotNull { (doc, basliqKicik, metnKicik) ->
            val basliqda = basliqKicik.contains(q)
            val yer = metnKicik.indexOf(q)
            // Nə başlıqda, nə mətndə varsa — bu sənəd nəticəyə düşmür.
            if (!basliqda && yer < 0) return@mapNotNull null

            // Tapılan yerin ətrafından parça kəsilir: 60 simvol öncə, 90 sonra.
            val parca = if (yer >= 0) {
                val bas = (yer - 60).coerceAtLeast(0)
                val son = (yer + q.length + 90).coerceAtMost(doc.text.length)
                buildString {
                    if (bas > 0) append("…")
                    append(doc.text.substring(bas, son).trim())
                    if (son < doc.text.length) append("…")
                }
            } else {
                doc.text.take(140)
            }

            SearchNetice(
                id = doc.id,
                title = doc.title,
                group = doc.group,
                parca = parca,
                // Başlıqda tapılanlar 0 bal alır və yuxarı qalxır; mətndə
                // tapılanlar 1. sortedBy sabitdir, ona görə bərabər ballılar
                // sənəd sırasını (yəni dərs sırasını) saxlayır.
                bal = if (basliqda) 0 else 1
            )
        }.sortedBy { it.bal }
    }
}

/** Bir axtarış nəticəsi — ekranda göstərilən sətir. */
data class SearchNetice(
    val id: String,
    val title: String,
    val group: String,
    val parca: String,
    val bal: Int
)
