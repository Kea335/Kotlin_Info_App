package az.kotlinaz.app.ui.editor

/* ============================================================
   Redaktorun söz tamamlama lüğəti.

   Xarici indeksləşdirmə və ya dil serveri yoxdur — sadə prefiks
   axtarışıdır: «pri» yazanda print, println; «va» yazanda
   val, var, vararg təklif olunur. Lüğət tətbiqin içindədir,
   ona görə tam oflayn işləyir.
   ============================================================ */

/** Təklifin növü — zolaqda rəng və sıralama üçün. */
enum class TamamlamaNovu { ACAR, TIP, FUNKSIYA, SABLON }

/**
 * Bir təklif.
 *
 * @param anahtar yazılan prefiksin tutuşdurulduğu söz (println)
 * @param etiket  zolaqda görünən mətn
 * @param metn    redaktora daxil edilən mətn
 * @param kursorGeri daxil edildikdən sonra kursor sondan neçə simvol geri
 *                   çəkilsin — mötərizəli funksiyada 1, yəni mötərizənin içi
 */
data class Tamamlama(
    val anahtar: String,
    val etiket: String,
    val metn: String,
    val kursorGeri: Int = 0,
    val nov: TamamlamaNovu = TamamlamaNovu.FUNKSIYA
)

private fun acar(s: String) = Tamamlama(s, s, s, 0, TamamlamaNovu.ACAR)
private fun tip(s: String) = Tamamlama(s, s, s, 0, TamamlamaNovu.TIP)

/** Mötərizəli funksiya — kursor mötərizənin içinə qoyulur. */
private fun fn(s: String) = Tamamlama(s, s + "()", s + "()", 1, TamamlamaNovu.FUNKSIYA)

/** Lambda qəbul edən funksiya — kursor fiqurlu mötərizənin içində. */
private fun lam(s: String) = Tamamlama(s, s + " { }", s + " { }", 2, TamamlamaNovu.FUNKSIYA)

/** Xassə kimi işlənən üzvlər — mötərizəsiz. */
private fun xas(s: String) = Tamamlama(s, s, s, 0, TamamlamaNovu.FUNKSIYA)

private val ACAR_SOZLER = listOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
    "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
    "true", "try", "typealias", "val", "var", "when", "while", "by", "catch",
    "constructor", "finally", "get", "import", "init", "set", "where", "abstract",
    "annotation", "companion", "const", "crossinline", "data", "enum", "external",
    "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open",
    "operator", "out", "override", "private", "protected", "public", "reified",
    "sealed", "suspend", "tailrec", "value", "vararg"
).map(::acar)

private val TIPLER = listOf(
    "Int", "Long", "Short", "Byte", "Float", "Double", "Boolean", "Char", "String",
    "Any", "Unit", "Nothing", "Array", "List", "MutableList", "Set", "MutableSet",
    "Map", "MutableMap", "Pair", "Triple", "Sequence", "Iterable", "Collection",
    "Comparable", "Throwable", "Exception", "RuntimeException", "IllegalArgumentException",
    "IllegalStateException", "NumberFormatException", "Result", "IntArray", "LongArray",
    "DoubleArray", "CharArray", "BooleanArray", "ArrayList", "HashMap", "HashSet",
    "LinkedHashMap", "StringBuilder", "Regex", "Number", "Enum", "Function",
    "CharSequence", "Iterator", "Random", "Math"
).map(::tip)

private val FUNKSIYALAR = listOf(
    // Giriş / çıxış
    "print", "println", "readLine", "readln", "readlnOrNull",
    // Kolleksiya qurucuları
    "listOf", "listOfNotNull", "mutableListOf", "arrayListOf", "setOf", "mutableSetOf",
    "mapOf", "mutableMapOf", "arrayOf", "intArrayOf", "doubleArrayOf", "booleanArrayOf",
    "emptyList", "emptyMap", "emptySet", "buildString", "buildList", "buildMap",
    // Çevirmə
    "toInt", "toIntOrNull", "toLong", "toDouble", "toDoubleOrNull", "toFloat", "toChar",
    "toString", "toList", "toSet", "toMap", "toMutableList", "toTypedArray", "toBoolean",
    // Mətn
    "substring", "split", "trim", "trimStart", "trimEnd", "trimIndent", "trimMargin",
    "replace", "replaceFirst", "contains", "startsWith", "endsWith", "indexOf",
    "lastIndexOf", "uppercase", "lowercase", "padStart", "padEnd",
    "joinToString", "format", "isBlank", "isNotBlank", "isEmpty", "isNotEmpty",
    "isNullOrEmpty", "isNullOrBlank", "removePrefix", "removeSuffix", "lines",
    // Kolleksiya əməliyyatları
    "add", "addAll", "remove", "removeAt", "removeAll", "clear", "put", "getOrNull",
    "getOrDefault", "getOrElse", "getOrPut", "elementAt", "containsKey", "containsValue",
    "distinct", "distinctBy", "reversed", "take", "takeLast", "drop", "dropLast",
    "chunked", "windowed", "zip", "unzip", "plus", "minus", "subList", "slice",
    "sorted", "sortedDescending", "sortedBy", "sortedByDescending", "shuffled",
    "sum", "sumOf", "average", "count", "maxOrNull", "minOrNull", "maxOf", "minOf",
    "maxByOrNull", "minByOrNull", "withIndex", "firstOrNull", "lastOrNull",
    "single", "singleOrNull", "reduce", "fold", "flatten", "associate",
    "associateBy", "associateWith",
    // Digər
    "require", "requireNotNull", "check", "checkNotNull", "error", "TODO",
    "coerceIn", "coerceAtLeast", "coerceAtMost", "abs", "sqrt", "roundToInt",
    "until", "downTo", "step", "compareTo", "copy",
    "launch", "async", "await", "delay", "runBlocking"
).map(::fn)

/** Lambda qəbul edənlər — fiqurlu mötərizə ilə daxil olunur. */
private val LAMBDALILAR = listOf(
    "forEach", "forEachIndexed", "map", "mapIndexed", "mapNotNull", "mapValues",
    "filter", "filterNot", "filterNotNull", "filterIsInstance", "flatMap",
    "any", "all", "none", "find", "groupBy", "partition", "sortedWith", "onEach",
    "takeWhile", "dropWhile", "let", "run", "apply", "also", "takeIf", "takeUnless",
    "with", "repeat", "lazy"
).map(::lam)

/** Mötərizəsiz üzvlər. */
private val XASSELER = listOf(
    "size", "length", "key", "value", "keys", "values", "entries", "it",
    "first", "second", "third", "last", "lastIndex", "indices", "name", "ordinal"
).map(::xas)

private val SABLONLAR = listOf(
    Tamamlama(
        anahtar = "main",
        etiket = "fun main()",
        metn = "fun main() {\n    \n}",
        kursorGeri = 2,
        nov = TamamlamaNovu.SABLON
    ),
    Tamamlama(
        anahtar = "fun",
        etiket = "fun ad() {…}",
        metn = "fun ad() {\n    \n}",
        kursorGeri = 2,
        nov = TamamlamaNovu.SABLON
    ),
    Tamamlama(
        anahtar = "for",
        etiket = "for (…) {…}",
        metn = "for (i in 0 until 10) {\n    \n}",
        kursorGeri = 2,
        nov = TamamlamaNovu.SABLON
    ),
    Tamamlama(
        anahtar = "if",
        etiket = "if (…) {…}",
        metn = "if () {\n    \n}",
        kursorGeri = 10,
        nov = TamamlamaNovu.SABLON
    ),
    Tamamlama(
        anahtar = "when",
        etiket = "when (…) {…}",
        metn = "when () {\n    \n}",
        kursorGeri = 10,
        nov = TamamlamaNovu.SABLON
    ),
    Tamamlama(
        anahtar = "while",
        etiket = "while (…) {…}",
        metn = "while () {\n    \n}",
        kursorGeri = 10,
        nov = TamamlamaNovu.SABLON
    ),
    Tamamlama(
        anahtar = "try",
        etiket = "try/catch",
        metn = "try {\n    \n} catch (e: Exception) {\n    \n}",
        kursorGeri = 32,
        nov = TamamlamaNovu.SABLON
    ),
    Tamamlama(
        anahtar = "class",
        etiket = "class Ad {…}",
        metn = "class Ad {\n    \n}",
        kursorGeri = 2,
        nov = TamamlamaNovu.SABLON
    ),
    Tamamlama(
        anahtar = "data",
        etiket = "data class",
        metn = "data class Ad(val x: Int)",
        kursorGeri = 0,
        nov = TamamlamaNovu.SABLON
    )
)

/**
 * Bütün lüğət — proses ərzində bir dəfə qurulur.
 *
 * `distinctBy` eyni anahtarın həm funksiya, həm lambda siyahısında olduğu
 * halları (məsələn `repeat`) təmizləyir. Açar isə NÖVLƏ birlikdə götürülür:
 * `class` üçün həm şablon (`class Ad {…}`), həm də quru açar söz qalsın deyə.
 * Sıra siyahıların ardıcıllığını saxlayır, ona görə şablonlar öndə çıxır.
 */
private val LUGET: List<Tamamlama> =
    (SABLONLAR + ACAR_SOZLER + FUNKSIYALAR + LAMBDALILAR + TIPLER + XASSELER)
        .distinctBy { it.anahtar to it.nov }

/** Eyni anda göstərilən maksimum təklif sayı. */
private const val MAKS_TEKLIF = 12

private fun Char.sozHissesi() = this == '_' || this.isLetterOrDigit()

/**
 * Kursorun solundakı yarımçıq sözün aralığı.
 * Söz yoxdursa (boşluq, durğu işarəsi, sətir başı) null qaytarır.
 */
fun prefiksArali(metn: String, kursor: Int): IntRange? {
    if (kursor <= 0 || kursor > metn.length) return null
    var bas = kursor
    while (bas > 0 && metn[bas - 1].sozHissesi()) bas--
    if (bas == kursor) return null
    // Rəqəmlə başlayan parça söz deyil — 12ab üçün təklif verilmir.
    if (metn[bas].isDigit()) return null
    return bas until kursor
}

/**
 * Kursor mətn sətrinin və ya şərhin içindədirmi?
 *
 * Tam leksik analiz əvəzinə yalnız CARİ SƏTİR yoxlanılır — hər vuruşda bütün
 * sənədi gəzmək lazım gəlmir. Çoxsətirli mətn bu yoxlamadan yayına bilər,
 * amma səhvin bahası sadəcə artıq təklifdir.
 */
private fun metnVeYaSerhIcinde(metn: String, kursor: Int): Boolean {
    val axtarisBaslangici = (kursor - 1).coerceAtLeast(0)
    val setirBasi = if (metn.isEmpty()) 0 else {
        val n = metn.lastIndexOf('\n', axtarisBaslangici)
        if (n < 0) 0 else n + 1
    }
    var i = setirBasi
    var dirnaq = false
    var apostrof = false
    while (i < kursor) {
        val ch = metn[i]
        when {
            ch == '\\' -> i++ // qaçırılmış simvol — növbətini atla
            ch == '"' && !apostrof -> dirnaq = !dirnaq
            ch == '\'' && !dirnaq -> apostrof = !apostrof
            ch == '/' && !dirnaq && !apostrof &&
                i + 1 < kursor && metn[i + 1] == '/' -> return true
        }
        i++
    }
    return dirnaq || apostrof
}

/**
 * Kursorun solundakı prefiksə uyğun təkliflər.
 *
 * Sıralama: əvvəlcə hərf-hərf üst-üstə düşənlər, sonra qısa sözlər. Beləcə
 * «pri» yazanda print öndə çıxır, «va» yazanda val/var vararg-dan qabaqda olur.
 */
fun tekliflerTap(metn: String, kursor: Int): List<Tamamlama> {
    val aralig = prefiksArali(metn, kursor) ?: return emptyList()
    val prefiks = metn.substring(aralig.first, aralig.last + 1)
    if (prefiks.isEmpty()) return emptyList()
    if (metnVeYaSerhIcinde(metn, aralig.first)) return emptyList()

    return LUGET.asSequence()
        .filter { it.anahtar.startsWith(prefiks, ignoreCase = true) }
        // Yazılanın eynisi və əlavəsiz təklifdirsə zolaqda yer tutmasın.
        .filterNot { it.anahtar == prefiks && it.metn == prefiks }
        .sortedWith(
            compareBy(
                { !it.anahtar.startsWith(prefiks) }, // böyük/kiçik hərf tam uyğun
                { it.anahtar.length },
                { it.anahtar }
            )
        )
        .take(MAKS_TEKLIF)
        .toList()
}
