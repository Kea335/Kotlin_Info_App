package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.SearchDoc
import java.text.Normalizer
import java.util.Locale

/**
 * Oflayn tam mətn axtarışı. Saf sinifdir — Context və Compose asılılığı yoxdur,
 * ona görə birbaşa unit testdən çağırıla bilər.
 *
 * Həm sənəd, həm sorğu [normallasdir] ilə eyni qaydada normallaşdırılır —
 * qayda məzmunun dilindən asılıdır (T3.5):
 *
 *  - **az, tr** — kiçiltmə dil üzrə, sonra `ı` → `i`. Kotlin-in parametrsiz
 *    `lowercase()`-i ROOT işlədir: `"İstifadəçi"` → `"i̇stifadəçi"` (i + U+0307),
 *    istifadəçinin yazdığı adi `i` tapılmırdı (T0.1). Təkcə az qaydası da
 *    kifayət deyil: `"Int".lowercase(az)` = `"ınt"`, ona görə `ı` birləşdirilir
 *    — `İstisnalar`, `Int`, `IDE` hamısı `i` ilə tapılır.
 *  - **ar, fa** — hərəkələr və tatvil (uzatma xətti) silinir, `ي/ى` → `ی`, `ك` → `ک`,
 *    `أ/إ/آ` → `ا`: hərəkəli və hərəkəsiz yazılış, ərəb və fars klaviaturası
 *    eyni nəticəni verir.
 *  - **zh-CN, ja, ko** — sözlər boşluqla ayrılmır; tokenizasiya BİLƏRƏKDƏN
 *    yoxdur, substring (`contains`) axtarışı bu dillər üçün düzgündür. «Token
 *    əlavə edək» deyilsə — lazım deyil.
 *  - **qalanları** — NFKC + ROOT kiçiltmə (`Ü` → `ü`, ayrılmış `U+0308` də).
 *
 * Normallaşdırma simvol sayını dəyişə bilər (hərəkə silinir, `ﬁ` → `fi`),
 * ona görə hər normal simvolun orijinaldakı yeri saxlanılır: parça
 * (snippet) həmişə ORİJİNAL mətndən kəsilir — istifadəçi hərəkəsiz ərəb
 * mətni görmür.
 */
class AxtarisMotoru(docs: List<SearchDoc>, private val dilTeqi: String = "az") {

    // Axtarış indeksi ~120 KB mətndir. Normal surət bir dəfə, sinif yaranarkən
    // hazırlanır; `axtar()` yalnız hazır sətirlərdə axtarır — hər hərf
    // vuruşunda 32 sənədi yenidən normallaşdırmaq lazım gəlmir.
    private class Sened(val doc: SearchDoc, val basliq: String, val metn: NormalMetn)

    private val indeks: List<Sened> =
        docs.map { Sened(it, normallasdir(it.title, dilTeqi).metn, normallasdir(it.text, dilTeqi)) }

    fun axtar(sorgu: String): List<SearchNetice> {
        val q = normallasdir(sorgu.trim(), dilTeqi).metn
        // Bir hərflik sorğu bütün sənədləri qaytarardı — mənasızdır.
        if (q.length < 2) return emptyList()

        return indeks.mapNotNull { s ->
            val doc = s.doc
            val basliqda = s.basliq.contains(q)
            val yer = s.metn.metn.indexOf(q)
            // Nə başlıqda, nə mətndə varsa — bu sənəd nəticəyə düşmür.
            if (!basliqda && yer < 0) return@mapNotNull null

            // Tapılan yerin ətrafından parça kəsilir: 60 simvol öncə, 90 sonra —
            // orijinal mətndə, normal mövqelər orijinala çevrilərək.
            val parca = if (yer >= 0) {
                val tapilan = s.metn.orijinal(yer)
                val tapilanSon = s.metn.orijinal(yer + q.length)
                val bas = (tapilan - 60).coerceAtLeast(0)
                val son = (tapilanSon + 90).coerceAtMost(doc.text.length)
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

/* ---------- Dil üzrə normallaşdırma ---------- */

/** Normallaşdırılmış mətn və hər simvolunun orijinal mətndəki yeri. */
internal class NormalMetn(val metn: String, private val yerler: IntArray) {
    /** Normal mətndəki [i] mövqeyinin orijinaldakı qarşılığı; `i == metn.length` → orijinalın sonu. */
    fun orijinal(i: Int): Int = yerler[i]
}

// Ərəb/fars: tənvin, hərəkələr, şəddə, sukun (U+064B–U+0652) və tatvil (U+0640).
private val HEREKE = Regex("[\\u064B-\\u0652\\u0640]")

internal fun normallasdir(s: String, dilTeqi: String): NormalMetn {
    val qayda = qaydaSec(dilTeqi)
    val sb = StringBuilder(s.length)
    // NFKC bəzən çox genişləndirir (`ﷺ` → 18 simvol) — massiv lazım olduqca böyüyür.
    var yerler = IntArray(s.length + 1)
    var n = 0
    var i = 0
    while (i < s.length) {
        // Klaster: əsas simvol + ardınca gələn birləşən işarələr. NFKC onları
        // birlikdə görməlidir (`U` + `U+0308` → `Ü`), hərəkə də öz hərfi ilə
        // birlikdə silinir. Nəticənin hər simvolu klasterin başlanğıcına bağlanır.
        var j = i + Character.charCount(s.codePointAt(i))
        while (j < s.length && birlesendir(s.codePointAt(j))) j += Character.charCount(s.codePointAt(j))
        val c = s[i]
        val parca = if (j == i + 1 && c.code < 0x80) {
            // ASCII sürətli yolu — az/tr üçün də düzgündür: `I` → `ı` → `i`.
            c.lowercaseChar().toString()
        } else {
            qayda(s.substring(i, j))
        }
        if (n + parca.length >= yerler.size) yerler = yerler.copyOf(maxOf(yerler.size * 2, n + parca.length + 1))
        for (ch in parca) {
            sb.append(ch)
            yerler[n++] = i
        }
        i = j
    }
    yerler[n] = s.length
    return NormalMetn(sb.toString(), yerler.copyOf(n + 1))
}

private fun birlesendir(cp: Int): Boolean = when (Character.getType(cp).toByte()) {
    Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK -> true
    else -> false
}

private fun nfkc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFKC)

private fun qaydaSec(dilTeqi: String): (String) -> String {
    val dil = dilTeqi.substringBefore('-').lowercase(Locale.ROOT)
    if (dil == "az" || dil == "tr") {
        val locale = Locale.forLanguageTag(dil)
        return { s -> nfkc(s).lowercase(locale).replace('ı', 'i') }
    }
    if (dil == "ar" || dil == "fa") return ::erebNormal
    // CJK də bura düşür: NFKC tam enli latın/yarım enli kananı birləşdirir.
    return { s -> nfkc(s).lowercase(Locale.ROOT) }
}

private fun erebNormal(s: String): String =
    nfkc(s).replace(HEREKE, "")
        .replace('\u064A', '\u06CC') // ي → ی
        .replace('\u0649', '\u06CC') // ى → ی
        .replace('\u0643', '\u06A9') // ك → ک
        .replace('\u0623', '\u0627') // أ → ا
        .replace('\u0625', '\u0627') // إ → ا
        .replace('\u0622', '\u0627') // آ → ا
        .lowercase(Locale.ROOT)
