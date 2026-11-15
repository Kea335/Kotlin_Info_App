package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.Section

/* ============================================================
   Öyrənmə yolu (T5.5) — «nəyi növbəti öyrənim?». Qraf
   tools/path.json-dadır, content.json-a bölmənin `requires`
   sahəsi kimi düşür. Saf funksiyalardır.

   Kilid sərt deyil: kilidli bölmə açılır, yalnız xəbərdarlıq
   göstərilir — tövsiyə istiqamət verir, qadağa qoymur.
   ============================================================ */

enum class YolHali { KILIDLI, ACIQ, TAMAMLANIB }

/** Bölmənin yoldakı halı: tamamlanıb; asılılıqları tamamdırsa açıq; qalanı kilidli. */
fun yolHali(bolme: Section, tereqqi: Tereqqi): YolHali = when {
    tereqqi.bolme(bolme.id).tamamlandi -> YolHali.TAMAMLANIB
    bolme.requires.all { tereqqi.bolme(it).tamamlandi } -> YolHali.ACIQ
    else -> YolHali.KILIDLI
}

/** Hələ tamamlanmamış asılılıqlar — kilidli bölmənin xəbərdarlığı üçün, qrafdakı sıra ilə. */
fun catismayanAsililiqlar(bolme: Section, tereqqi: Tereqqi): List<String> =
    bolme.requires.filterNot { tereqqi.bolme(it).tamamlandi }

/** Tövsiyə olunan addım: bölmə və onun ilk həll edilməmiş çalışması. */
data class Tovsiye(
    val bolme: Section,
    val movzuId: String?,
    /** Mövzunun ilk həll edilməmiş çalışması (əvvəl nəzəri, sonra praktiki). */
    val calismaId: String?,
    /** Bölmədə qalan həll edilməmiş çalışma sayı. */
    val qalanCalisma: Int
)

/**
 * Növbəti addım: dərs sırası ilə ilk açıq və tamamlanmamış yol bölməsi.
 * Yola qrafda keçən bölmələr daxildir (asılılığı olan və ya kiminsə asılılığı
 * olan); giriş, tarixçə, resurslar kimi asılılıqsız bölmələr tövsiyə olunmur.
 * Hamısı tamamdırsa null.
 */
fun novbetiAddim(
    sections: List<Section>,
    tereqqi: Tereqqi,
    topics: List<ExerciseTopic>,
    hellEdilmis: Set<String>
): Tovsiye? {
    val yolda = sections.flatMap { it.requires }.toSet() + sections.filter { it.requires.isNotEmpty() }.map { it.id }
    val bolme = sections
        .filter { it.id in yolda }
        .sortedBy { it.index }
        .firstOrNull { yolHali(it, tereqqi) == YolHali.ACIQ }
        ?: return null
    val movzu = topics.firstOrNull { it.sectionId == bolme.id }
    val ilk = movzu?.let { m -> (m.nezeri.map { it.id } + m.praktiki.map { it.id }).firstOrNull { it !in hellEdilmis } }
    val t = tereqqi.bolme(bolme.id)
    return Tovsiye(bolme, movzu?.id, ilk, (t.umumi - t.hell).coerceAtLeast(0))
}
