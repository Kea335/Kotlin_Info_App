package az.kotlinaz.app.ui.highlight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinAzColors

/* ============================================================
   Kotlin üçün yüngül sintaksis rəngləyici.
   Saytdakı js/highlight.js faylının birbaşa portudur —
   xarici kitabxana yoxdur, tam oflayn işləyir.
   ============================================================ */

private val KEYWORDS = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
    "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
    "true", "try", "typealias", "typeof", "val", "var", "when", "while",
    "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally",
    "get", "import", "init", "param", "property", "receiver", "set", "setparam", "where",
    "abstract", "actual", "annotation", "companion", "const", "crossinline", "data",
    "enum", "expect", "external", "final", "infix", "inline", "inner", "internal",
    "lateinit", "noinline", "open", "operator", "out", "override", "private", "protected",
    "public", "reified", "sealed", "suspend", "tailrec", "value", "vararg"
)

private val BUILTIN_TYPES = setOf(
    "Int", "Long", "Short", "Byte", "Float", "Double", "Boolean", "Char", "String",
    "Any", "Unit", "Nothing", "Array", "List", "MutableList", "Set", "MutableSet",
    "Map", "MutableMap", "Pair", "Triple", "Sequence", "Iterable", "Collection",
    "Comparable", "Throwable", "Exception", "RuntimeException", "Result",
    "IntArray", "LongArray", "DoubleArray", "CharArray", "BooleanArray",
    "ArrayList", "HashMap", "HashSet", "LinkedHashMap", "StringBuilder", "Regex",
    "Flow", "StateFlow", "SharedFlow", "Job", "Deferred", "CoroutineScope",
    "Dispatchers", "Channel", "Mutex", "Number", "Enum", "Function"
)

// Punktuasiya və operator simvolları. Ardıcıl gələnlər tək parça kimi
// rənglənir — `?:`, `!!`, `->`, `==` bölünmür.
private const val PUNCT = "{}()[];,.:?!<>=+-*/%&|^~"

private fun Char.isIdentStart() = this == '_' || this.isLetter()
private fun Char.isIdentPart() = this == '_' || this.isLetterOrDigit()

/** Kod mətnini rənglənmiş `AnnotatedString`-ə çevirir. */
fun highlightKotlin(src: String, c: KotlinAzColors): AnnotatedString = buildAnnotatedString {

    val comStyle = SpanStyle(color = c.synCom, fontStyle = FontStyle.Italic)
    val strStyle = SpanStyle(color = c.synStr)
    val numStyle = SpanStyle(color = c.synNum)
    val annStyle = SpanStyle(color = c.synAnn)
    val keyStyle = SpanStyle(color = c.synKey)
    val typStyle = SpanStyle(color = c.synType)
    val fnStyle = SpanStyle(color = c.synFn)
    val punStyle = SpanStyle(color = c.synPunc)

    fun add(style: SpanStyle?, text: String) {
        if (text.isEmpty()) return
        if (style == null) append(text) else withStyle(style) { append(text) }
    }

    // Əl ilə yazılmış leksik analiz: mətn əvvəldən sona bir dəfə gəzilir,
    // hər addımda ən uzun uyğun parça götürülüb rənglənir. Regex yığını
    // işlədilmir — uzun kod bloklarında bu üsul həm sürətli, həm proqnozludur.
    var i = 0
    val n = src.length

    while (i < n) {
        val ch = src[i]

        // --- Blok şərh /* ... */ (iç-içə dəstəklənir)
        if (ch == '/' && i + 1 < n && src[i + 1] == '*') {
            val start = i
            var depth = 1
            i += 2
            while (i < n && depth > 0) {
                when {
                    src[i] == '/' && i + 1 < n && src[i + 1] == '*' -> { depth++; i += 2 }
                    src[i] == '*' && i + 1 < n && src[i + 1] == '/' -> { depth--; i += 2 }
                    else -> i++
                }
            }
            add(comStyle, src.substring(start, i))
            continue
        }

        // --- Sətir şərhi // ...
        if (ch == '/' && i + 1 < n && src[i + 1] == '/') {
            val start = i
            while (i < n && src[i] != '\n') i++
            add(comStyle, src.substring(start, i))
            continue
        }

        // --- Üç dırnaqlı mətn (raw string) — içindəki hər şey mətndir
        if (ch == '"' && i + 2 < n && src[i + 1] == '"' && src[i + 2] == '"') {
            val start = i
            i += 3
            while (i < n && !(src[i] == '"' && i + 2 < n && src[i + 1] == '"' && src[i + 2] == '"')) i++
            i = minOf(i + 3, n)
            add(strStyle, src.substring(start, i))
            continue
        }

        // --- Adi mətn " ... "
        // Şablon ifadələri ($ad, ${…}) ayrıca rənglənir, ona görə mətnin
        // rənglənməmiş hissələri `buf`-da yığılır və şablona rast gələndə
        // yığılan hissə boşaldılır.
        if (ch == '"') {
            i++
            val buf = StringBuilder("\"")
            while (i < n && src[i] != '"') {
                // Qaçırılmış simvol: \\" mətni bitirmir.
                if (src[i] == '\\') {
                    buf.append(src[i])
                    if (i + 1 < n) buf.append(src[i + 1])
                    i += 2
                    continue
                }
                if (src[i] == '$') {
                    if (buf.isNotEmpty()) { add(strStyle, buf.toString()); buf.setLength(0) }
                    val ts = i
                    i++
                    // ${…} formasında mötərizələr sayılır (iç-içə ola bilər);
                    // $ad formasında isə sadəcə ad hissəsi götürülür.
                    if (i < n && src[i] == '{') {
                        var br = 1
                        i++
                        while (i < n && br > 0) {
                            if (src[i] == '{') br++
                            if (src[i] == '}') br--
                            i++
                        }
                    } else {
                        while (i < n && (src[i].isIdentPart() || src[i] == '.')) i++
                    }
                    add(fnStyle, src.substring(ts, i))
                    continue
                }
                buf.append(src[i])
                i++
            }
            if (i < n) { buf.append('"'); i++ }
            add(strStyle, buf.toString())
            continue
        }

        // --- Simvol 'a'
        if (ch == '\'') {
            val start = i
            i++
            while (i < n && src[i] != '\'') {
                if (src[i] == '\\') i++
                i++
            }
            i = minOf(i + 1, n)
            add(strStyle, src.substring(start, i))
            continue
        }

        // --- Annotasiya @Composable
        if (ch == '@' && i + 1 < n && src[i + 1].isIdentStart()) {
            val start = i
            i++
            while (i < n && (src[i].isIdentPart() || src[i] == '.')) i++
            add(annStyle, src.substring(start, i))
            continue
        }

        // --- Rəqəm: 42, 0xFF, 1_000_000, 3.14e-5, 42L, 1.5f
        if (ch.isDigit()) {
            val start = i
            while (i < n) {
                val d = src[i]
                val uygun = d.isDigit() || d in "abcdefABCDEFxXbBoO_.eE+-"
                if (!uygun) break
                // +/- yalnız eksponentin içində rəqəmin hissəsidir (1e-5);
                // əks halda operatordur və rəqəm burada bitir.
                if ((d == '+' || d == '-') && (i == 0 || src[i - 1] !in "eE")) break
                // Nöqtədən sonra rəqəm yoxdursa bu, üzv müraciətidir: 1.plus(2)
                if (d == '.' && (i + 1 >= n || !src[i + 1].isDigit())) break
                i++
            }
            while (i < n && src[i] in "fFlLuU") i++
            add(numStyle, src.substring(start, i))
            continue
        }

        // --- Ad / açar söz / tip / funksiya çağırışı
        if (ch.isIdentStart()) {
            val start = i
            while (i < n && src[i].isIdentPart()) i++
            val word = src.substring(start, i)

            // Növbəti mənalı simvola baxırıq: mötərizədirsə, bu ad funksiya
            // çağırışıdır. Aradakı boşluqlar atlanır — `println (x)` da tutulsun.
            var j = i
            while (j < n && (src[j] == ' ' || src[j] == '\t')) j++
            val nextCh = if (j < n) src[j] else ' '

            when {
                word in KEYWORDS -> add(keyStyle, word)
                // Böyük hərflə başlayan hər ad tip sayılır — sinif elanı da,
                // konstruktor çağırışı da eyni rəngdə görünsün.
                word in BUILTIN_TYPES || word.first().isUpperCase() -> add(typStyle, word)
                nextCh == '(' -> add(fnStyle, word)
                else -> add(null, word)
            }
            continue
        }

        // --- Punktuasiya / operatorlar
        if (ch in PUNCT) {
            val start = i
            while (i < n && src[i] in PUNCT) i++
            add(punStyle, src.substring(start, i))
            continue
        }

        // --- Digər (boşluq, sətir sonu)
        add(null, ch.toString())
        i++
    }
}

/**
 * Redaktor sahələri üçün rəngləmə.
 *
 * `highlightKotlin` yalnız üslub əlavə edir — simvolların sırası və sayı
 * dəyişmir, ona görə kursor mövqeləri birbaşa uyğun gəlir (Identity).
 */
// Redaktorlarda (kod meydanı, praktiki çalışma) işlədilir.
fun kotlinVisualTransformation(c: KotlinAzColors): VisualTransformation =
    VisualTransformation { text ->
        TransformedText(highlightKotlin(text.text, c), OffsetMapping.Identity)
    }

@Composable
fun rememberKotlinTransformation(): VisualTransformation {
    val colors = KAz.colors
    return remember(colors) { kotlinVisualTransformation(colors) }
}

/** Rəngləmə nəticəsi kod və tema dəyişməyincə yenidən hesablanmır. */
@Composable
fun rememberHighlighted(code: String): AnnotatedString {
    val colors = KAz.colors
    return remember(code, colors) { highlightKotlin(code, colors) }
}

/**
 * Konsol çıxışı üçün sadə rəngləmə — xəta sətirlərini fərqləndirir.
 * Yığın izinin («at MainKt.main») sətirləri də xəta sayılır.
 */
fun highlightOutput(text: String, ok: Color, err: Color): AnnotatedString = buildAnnotatedString {
    text.lineSequence().forEachIndexed { index, line ->
        if (index > 0) append('\n')
        val xetadir = line.startsWith("Exception") ||
            line.startsWith("java.lang.") ||
            line.startsWith("kotlin.") && line.contains("Exception") ||
            line.trimStart().startsWith("at ")
        withStyle(SpanStyle(color = if (xetadir) err else ok)) { append(line) }
    }
}
