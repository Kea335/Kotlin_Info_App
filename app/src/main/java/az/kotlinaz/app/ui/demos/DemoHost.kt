package az.kotlinaz.app.ui.demos

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Saytdakı interaktiv nümayişlərin nativ qarşılıqları.
 * Hamısı tam oflayn işləyir — məntiq js/demos.js faylından portlanıb.
 */
@Composable
fun DemoHost(id: String, modifier: Modifier = Modifier) {
    when (id) {
        "tip-cixarisi" -> TypeInferDemo(modifier)
        "null-safety" -> NullSafetyDemo(modifier)
        "kolleksiyalar" -> CollectionsDemo(modifier)
        "when" -> WhenDemo(modifier)
        "scope" -> ScopeDemo(modifier)
        "coroutines" -> CoroutinesDemo(modifier)
        else -> Unit
    }
}

/* ============================================================
   Sadə sarılan sıra — çiplər üçün
   ============================================================ */

@Composable
private fun FlowChips(
    modifier: Modifier = Modifier,
    aralik: Dp = 7.dp,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val bosluq = aralik.roundToPx()
        // Valideyn eni məhdudlaşdırmasa (məsələn üfüqi sürüşən sıra),
        // constraints.maxWidth sonsuzdur və layout() ilə ölçü kimi verilə bilməz.
        // Belə halda çiplər tək sətirdə qalır.
        val maxEn = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val yerlesdirilmis = measurables.map { it.measure(constraints.copy(minWidth = 0)) }

        var x = 0
        var y = 0
        var setirHundurluyu = 0
        val movqeler = ArrayList<Pair<Int, Int>>(yerlesdirilmis.size)

        yerlesdirilmis.forEach { p ->
            if (x > 0 && x + p.width > maxEn) {
                x = 0
                y += setirHundurluyu + bosluq
                setirHundurluyu = 0
            }
            movqeler.add(x to y)
            x += p.width + bosluq
            setirHundurluyu = max(setirHundurluyu, p.height)
        }

        // Sonsuz enli halda faktiki tutulan eni veririk.
        val netEn = if (constraints.hasBoundedWidth) maxEn else (x - bosluq).coerceAtLeast(0)
        layout(netEn, y + setirHundurluyu) {
            yerlesdirilmis.forEachIndexed { i, p ->
                p.placeRelative(movqeler[i].first, movqeler[i].second)
            }
        }
    }
}

/* ============================================================
   1. Tip çıxarışı laboratoriyası
   ============================================================ */

private data class TipNeticesi(val tip: String, val qeyd: String)

private val R_LONG = Regex("^-?\\d+[lL]$")
private val R_INT = Regex("^-?\\d+$")
private val R_FLOAT = Regex("^-?\\d*\\.\\d+[fF]$")
private val R_DOUBLE = Regex("^-?\\d*\\.\\d+$")
private val R_BOOL = Regex("^(true|false)$")
private val R_CHAR = Regex("^'.'$")
private val R_STRING = Regex("^\".*\"$")
private val R_LAMBDA = Regex("^\\{.*\\}$")

private fun tipCixar(xam: String): TipNeticesi {
    val v = xam.trim()
    if (v.isEmpty()) return TipNeticesi("?", "Yuxarıya bir dəyər yazın və ya nümunələrdən seçin.")
    if (R_LONG.matches(v)) return TipNeticesi("Long", "L sonluğu Long bildirir (64 bit).")
    if (R_INT.matches(v)) {
        val n = v.toLongOrNull() ?: 0L
        return if (n > Int.MAX_VALUE || n < Int.MIN_VALUE) {
            TipNeticesi("Long", "Dəyər Int hüdudundan böyükdür, ona görə avtomatik Long seçildi.")
        } else {
            TipNeticesi("Int", "Tam rəqəmlər defolt olaraq Int-dir (32 bit).")
        }
    }
    if (R_FLOAT.matches(v)) return TipNeticesi("Float", "f sonluğu Float bildirir (32 bit onluq).")
    if (R_DOUBLE.matches(v)) return TipNeticesi("Double", "Onluq ədədlər defolt olaraq Double-dir (64 bit).")
    if (R_BOOL.matches(v)) return TipNeticesi("Boolean", "Məntiqi dəyər: yalnız true və ya false ola bilər.")
    if (R_CHAR.matches(v)) return TipNeticesi("Char", "Tək dırnaq Char bildirir, iki dırnaq isə String.")
    if (R_STRING.matches(v)) return TipNeticesi("String", "İki dırnaq arasındakı mətn String-dir.")
    if (v == "null") {
        return TipNeticesi(
            "Nothing?",
            "Tək başına null-un tipi Nothing?-dir. Adətən açıq tip yazılır: val x: String? = null"
        )
    }
    if (v.startsWith("listOf(")) {
        val inner = v.removePrefix("listOf(").removeSuffix(")").trim()
        if (inner.isEmpty()) {
            return TipNeticesi(
                "List<Nothing>",
                "Boş siyahıda element tipi bilinmir — açıq yazın: listOf<Int>()"
            )
        }
        val ilk = inner.split(",").first().trim()
        val t = when {
            R_INT.matches(ilk) -> "Int"
            R_STRING.matches(ilk) -> "String"
            else -> "Any"
        }
        return TipNeticesi("List<$t>", "listOf() dəyişməz (read-only) siyahı qaytarır.")
    }
    if (v.startsWith("mutableListOf(")) {
        return TipNeticesi("MutableList<Int>", "mutableListOf() əlavə və silmə əməliyyatlarına icazə verir.")
    }
    if (v.startsWith("mapOf(")) return TipNeticesi("Map<K, V>", "mapOf(\"a\" to 1) forması Map<String, Int> verir.")
    if (v.startsWith("setOf(")) return TipNeticesi("Set<Int>", "Set təkrarlanan elementləri saxlamır.")
    if (v.startsWith("arrayOf(")) return TipNeticesi("Array<Int>", "Array sabit ölçülüdür; adətən List daha rahatdır.")
    if (R_LAMBDA.matches(v)) {
        return TipNeticesi("() -> Unit", "Süslü mötərizə lambda-dır; parametrsiz olsa tipi () -> Unit olur.")
    }
    return TipNeticesi("String", "Tanınmayan ifadə — yuxarıdakı nümunələrdən birini seçin.")
}

private val TI_NUMUNELER = listOf(
    "42", "42L", "3.14", "3.14f", "true", "'A'", "\"Kotlin\"", "null",
    "listOf(1, 2, 3)", "10000000000"
)

@Composable
private fun TypeInferDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    var giris by remember { mutableStateOf("42") }
    val netice = remember(giris) { tipCixar(giris) }

    DemoCard(
        basliq = "Tip çıxarışı laboratoriyası",
        tesvir = "Kotlin val x = ... yazanda sağ tərəfə baxıb tipi özü təyin edir.",
        modifier = modifier
    ) {
        FlowChips {
            TI_NUMUNELER.forEach { n ->
                DemoChip(etiket = n, aktiv = giris == n) { giris = n }
            }
        }
        Spacer(Modifier.height(11.dp))
        DemoField(value = giris, onValueChange = { giris = it }, placeholder = "42")
        Spacer(Modifier.height(11.dp))

        val gosterilen = giris.ifBlank { "…" }
        DemoCode(
            "val x = $gosterilen\n\n// kompilyatorun gördüyü:\nval x: ${netice.tip} = $gosterilen"
        )
        Spacer(Modifier.height(11.dp))

        DemoScreen(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.textFaint)) { append("Çıxarılan tip: ") }
                withStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Bold)) { append(netice.tip) }
            }
        )
        Spacer(Modifier.height(11.dp))
        DemoNote(netice.qeyd)
    }
}

/* ============================================================
   2. Null-safety laboratoriyası
   ============================================================ */

private enum class NsKind { OK, COMPILE, CRASH }
private data class NsNetice(val kind: NsKind, val msg: String, val note: String)

private data class NsOp(
    val id: String,
    val label: String,
    val run: (Boolean) -> NsNetice
)

private val NS_OPS = listOf(
    NsOp("direct", "ad.length") {
        NsNetice(
            NsKind.COMPILE,
            "Only safe (?.) or non-null asserted (!!.) calls are allowed\non a nullable receiver of type String?",
            "Kotlin bu kodu işə salmağa belə imkan vermir. Xəta çalışma vaxtında yox, kompilyasiya vaxtında tutulur — yəni istifadəçi onu heç vaxt görməyəcək."
        )
    },
    NsOp("safe", "ad?.length") { isNull ->
        if (isNull) NsNetice(NsKind.OK, "null", "Safe call operatoru (?.): receiver null-dursa çağırış tamamilə atlanır və nəticə null olur. Çökmə baş vermir.")
        else NsNetice(NsKind.OK, "6", "Receiver null deyil, ona görə length adi qaydada hesablanır.")
    },
    NsOp("bang", "ad!!.length") { isNull ->
        if (isNull) NsNetice(
            NsKind.CRASH,
            "Exception in thread \"main\"\nkotlin.KotlinNullPointerException\n\tat MainKt.main(Main.kt:4)",
            "!! operatoru «mən zəmanət verirəm ki, null deyil» deməkdir. Səhv etsən — NPE. Mümkün qədər ondan qaçın."
        )
        else NsNetice(NsKind.OK, "6", "Bu dəfə dəyər null deyildi, ona görə işlədi. Amma !! yenə də risklidir — növbəti dəfə null ola bilər.")
    },
    NsOp("elvis", "ad?.length ?: 0") { isNull ->
        if (isNull) NsNetice(NsKind.OK, "0", "Elvis operatoru (?:) sol tərəf null olduqda sağ tərəfdəki ehtiyat dəyəri qaytarır.")
        else NsNetice(NsKind.OK, "6", "Sol tərəf null olmadığı üçün elvisin sağ tərəfi heç işləmir.")
    },
    NsOp("let", "ad?.let { it.length }") { isNull ->
        if (isNull) NsNetice(NsKind.OK, "null", "let bloku yalnız receiver null olmayanda işlənir. Null olanda bütün blok atlanır.")
        else NsNetice(NsKind.OK, "6", "Blok içində it artıq smart-cast olunmuş String tipidir — əlavə ? işarəsinə ehtiyac yoxdur.")
    },
    NsOp("ifcheck", "if (ad != null) ad.length") { isNull ->
        if (isNull) NsNetice(NsKind.OK, "(heç nə çap olunmadı)", "Şərti yoxlama false verdi, blok işlənmədi.")
        else NsNetice(NsKind.OK, "6", "SMART CAST: null yoxlamasından sonra kompilyator ad-ı avtomatik String kimi görür — ?. yazmağa ehtiyac qalmır.")
    }
)

@Composable
private fun NullSafetyDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    var deyerVar by remember { mutableStateOf(false) }
    var opId by remember { mutableStateOf("direct") }

    val op = NS_OPS.firstOrNull { it.id == opId } ?: NS_OPS.first()
    val isNull = !deyerVar
    val res = remember(opId, isNull) { op.run(isNull) }

    DemoCard(
        basliq = "Null-safety laboratoriyası",
        tesvir = "Dəyəri null / dolu et, sonra operatoru seç — nəticəni və izahı dərhal gör.",
        modifier = modifier
    ) {
        DemoToggleRow(
            etiket = "Dəyər var (söndürülübsə null)",
            checked = deyerVar,
            sagMetn = if (isNull) "ad = null" else "ad = \"Kotlin\"",
            onCheckedChange = { deyerVar = it }
        )
        Spacer(Modifier.height(11.dp))

        FlowChips {
            NS_OPS.forEach { o ->
                DemoChip(etiket = o.label, aktiv = o.id == opId) { opId = o.id }
            }
        }
        Spacer(Modifier.height(11.dp))

        DemoCode(
            "val ad: String? = " + (if (isNull) "null" else "\"Kotlin\"") + "\nprintln(" + op.label + ")"
        )
        Spacer(Modifier.height(11.dp))

        val basliq = when (res.kind) {
            NsKind.COMPILE -> "KOMPİLYASİYA XƏTASI"
            NsKind.CRASH -> "ÇALIŞMA VAXTI ÇÖKMƏSİ"
            NsKind.OK -> "NƏTİCƏ"
        }
        DemoScreen(
            basliq = basliq,
            metn = buildAnnotatedString {
                withStyle(
                    SpanStyle(color = if (res.kind == NsKind.OK) c.ok else c.err)
                ) { append(res.msg) }
            }
        )
        Spacer(Modifier.height(11.dp))
        DemoNote(res.note, basliq = "Nə baş verdi?")
    }
}

/* ============================================================
   3. Kolleksiya zənciri qurucusu
   ============================================================ */

private data class ColOp(
    val id: String,
    val label: String,
    val terminal: Boolean,
    val apply: (List<Int>) -> Any
)

private val COL_BASE = listOf(5, 3, 8, 3, 1, 9, 2, 7)

private val COL_OPS = listOf(
    ColOp("filter", ".filter { it > 3 }", false) { a -> a.filter { it > 3 } },
    ColOp("map", ".map { it * 2 }", false) { a -> a.map { it * 2 } },
    ColOp("sorted", ".sorted()", false) { a -> a.sorted() },
    ColOp("reversed", ".reversed()", false) { a -> a.reversed() },
    ColOp("distinct", ".distinct()", false) { a -> a.distinct() },
    ColOp("take", ".take(3)", false) { a -> a.take(3) },
    ColOp("drop", ".drop(2)", false) { a -> a.drop(2) },
    ColOp("sum", ".sum()", true) { a -> a.sum() },
    ColOp("average", ".average()", true) { a -> if (a.isEmpty()) "NaN" else a.average() },
    ColOp("max", ".maxOrNull()", true) { a -> a.maxOrNull() ?: "null" },
    ColOp("count", ".count()", true) { a -> a.size },
    ColOp("join", ".joinToString()", true) { a -> "\"" + a.joinToString(", ") + "\"" }
)

private fun colFmt(v: Any?): String = when (v) {
    is List<*> -> "[" + v.joinToString(", ") + "]"
    is Double -> String.format(java.util.Locale.US, "%.2f", v)
    else -> v.toString()
}

@Composable
private fun CollectionsDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    val zencir = remember { mutableStateListOf("filter", "map") }

    DemoCard(
        basliq = "Zəncir qurucusu",
        tesvir = "Əməliyyatları klikləyərək zəncir yığ — hər addımın aralıq nəticəsini gör.",
        modifier = modifier
    ) {
        FlowChips {
            COL_OPS.forEach { o ->
                DemoChip(etiket = o.label, aktiv = zencir.contains(o.id)) {
                    if (zencir.contains(o.id)) zencir.remove(o.id) else zencir.add(o.id)
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        DemoChip(etiket = "Zənciri təmizlə") { zencir.clear() }
        Spacer(Modifier.height(11.dp))

        val kod = buildString {
            append("val reqemler = listOf(").append(COL_BASE.joinToString(", ")).append(")\n\n")
            append("val netice = reqemler")
            zencir.forEach { id ->
                COL_OPS.firstOrNull { it.id == id }?.let { append("\n    ").append(it.label) }
            }
            append("\n\nprintln(netice)")
        }
        DemoCode(kod)
        Spacer(Modifier.height(11.dp))

        // Addım-addım aralıq nəticələr.
        // `Column` inline composable-dır, ona görə aşağıdakı blok elə burada,
        // sıra ilə icra olunur — `cari` DemoScreen-ə çatanda artıq son dəyərdir.
        var cari: Any = COL_BASE
        // Terminal əməliyyatdan (sum, count, …) sonra zəncir davam edə bilməz:
        // nəticə artıq siyahı deyil.
        var bitdi = false
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ChainStep("listOf(…)", colFmt(COL_BASE), aktiv = true)

            zencir.forEach { id ->
                val o = COL_OPS.firstOrNull { it.id == id } ?: return@forEach
                if (bitdi) {
                    ChainStep(
                        o.label,
                        "Terminal əməliyyatdan sonra zəncir davam edə bilməz",
                        aktiv = false,
                        sagReng = c.err
                    )
                    return@forEach
                }
                @Suppress("UNCHECKED_CAST")
                val siyahi = cari as? List<Int> ?: emptyList()
                cari = o.apply(siyahi)
                if (o.terminal) bitdi = true
                ChainStep(o.label, colFmt(cari), aktiv = true)
            }
        }

        Spacer(Modifier.height(11.dp))
        DemoScreen(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.textFaint)) { append("nəticə = ") }
                withStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Bold)) { append(colFmt(cari)) }
            }
        )
    }
}

/* ============================================================
   4. when ifadəsi sınaqçısı
   ============================================================ */

private val WHEN_BRANCHES = listOf(
    "0 -> \"Sıfır\"",
    "1, 2, 3 -> \"Kiçik rəqəm: \$x\"",
    "in 4..10 -> \"4..10 aralığındadır\"",
    "is Int -> if (x < 0) \"Mənfi rəqəm\"",
    "is String -> \"Bu bir String-dir…\"",
    "else -> \"Böyük rəqəm: \$x\""
)

private val WHEN_NUMUNELER = listOf("0", "2", "7", "-5", "99", "Kotlin", "3.14")

private data class WhenNetice(val branch: Int?, val out: String)

private fun whenQiymetlendir(xam: String): WhenNetice {
    val v = xam.trim()
    if (v.isEmpty()) return WhenNetice(null, "(boş giriş)")

    val num = if (R_INT.matches(v)) v.toIntOrNull() else null
    if (num != null) {
        return when {
            num == 0 -> WhenNetice(0, "Sıfır")
            num in 1..3 -> WhenNetice(1, "Kiçik rəqəm: $num")
            num in 4..10 -> WhenNetice(2, "4..10 aralığındadır")
            num < 0 -> WhenNetice(3, "Mənfi rəqəm")
            else -> WhenNetice(5, "Böyük rəqəm: $num")
        }
    }
    // DÜZƏLİŞ: onluq ədəd əvvəllər 4-cü budağı — yəni `is String` budağını —
    // işıqlandırırdı, halbuki 3.14 String deyil. Həqiqi `when`-də Double heç bir
    // yuxarıdakı şərtə uymur və `else` budağına (indeks 5) düşür.
    if (Regex("^-?\\d+\\.\\d+$").matches(v)) return WhenNetice(5, "Böyük rəqəm: $v")
    return WhenNetice(4, "Bu bir String-dir: uzunluq ${v.length}")
}

@Composable
private fun WhenDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    var giris by remember { mutableStateOf("7") }
    val netice = remember(giris) { whenQiymetlendir(giris) }

    DemoCard(
        basliq = "when ifadəsi sınaqçısı",
        tesvir = "Dəyər yaz — hansı budağın seçildiyini canlı gör.",
        modifier = modifier
    ) {
        FlowChips {
            WHEN_NUMUNELER.forEach { n ->
                DemoChip(etiket = n, aktiv = giris == n) { giris = n }
            }
        }
        Spacer(Modifier.height(11.dp))
        DemoField(value = giris, onValueChange = { giris = it }, placeholder = "7")
        Spacer(Modifier.height(11.dp))

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            WHEN_BRANCHES.forEachIndexed { i, b ->
                val aktiv = netice.branch == i
                ChainStep(if (aktiv) "▶" else "  ", b, aktiv = aktiv)
            }
        }

        Spacer(Modifier.height(11.dp))
        DemoScreen(
            buildAnnotatedString {
                withStyle(
                    SpanStyle(color = if (netice.branch == null) c.textFaint else c.ok)
                ) { append(netice.out) }
            }
        )
    }
}

/* ============================================================
   5. Scope funksiyaları müqayisəsi
   ============================================================ */

private data class ScopeFn(
    val ad: String,
    val code: String,
    val ctx: String,
    val ret: String,
    val use: String,
    val out: String
)

private val SCOPE_FNS = listOf(
    ScopeFn(
        "let",
        "val uzunluq = ad?.let {\n    println(it)      // it = \"Kotlin\"\n    it.length        // son sətir qaytarılır\n}",
        "it", "lambda nəticəsi",
        "Null olmayan dəyərlə iş görmək və onu başqa nəticəyə çevirmək üçün.",
        "Kotlin\nuzunluq = 6"
    ),
    ScopeFn(
        "run",
        "val uzunluq = ad.run {\n    println(this)    // this = \"Kotlin\"\n    length           // this.length\n}",
        "this", "lambda nəticəsi",
        "Obyekt konfiqurasiyası ilə nəticə hesablanmasını birləşdirmək üçün.",
        "Kotlin\nuzunluq = 6"
    ),
    ScopeFn(
        "with",
        "val netice = with(ad) {\n    println(this)    // this = \"Kotlin\"\n    uppercase()\n}",
        "this", "lambda nəticəsi",
        "Eyni obyekt üzərində bir neçə əməliyyatı bir yerdə qruplaşdırmaq üçün.",
        "Kotlin\nnetice = KOTLIN"
    ),
    ScopeFn(
        "apply",
        "val istifadeci = Istifadeci().apply {\n    ad = \"Aysel\"     // this.ad\n    yas = 24\n}",
        "this", "obyektin özü",
        "Obyekti qurmaq (builder üslubu) — ən çox işlənən scope funksiyasıdır.",
        "istifadeci = Istifadeci(ad=Aysel, yas=24)"
    ),
    ScopeFn(
        "also",
        "val siyahi = mutableListOf(1, 2)\n    .also { println(\"əvvəl: \$it\") }\n    .apply { add(3) }\n    .also { println(\"sonra: \$it\") }",
        "it", "obyektin özü",
        "Yan təsirlər üçün — loglama, yoxlama, debug. Zənciri pozmur.",
        "əvvəl: [1, 2]\nsonra: [1, 2, 3]"
    )
)

@Composable
private fun ScopeDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    var secili by remember { mutableStateOf("let") }
    val fn = SCOPE_FNS.firstOrNull { it.ad == secili } ?: SCOPE_FNS.first()

    DemoCard(
        basliq = "Scope funksiyaları müqayisəsi",
        tesvir = "Beş funksiyanın fərqi: kontekst obyekti nədir, nə qaytarır, harada işlədilir.",
        modifier = modifier
    ) {
        FlowChips {
            SCOPE_FNS.forEach { f ->
                DemoChip(etiket = f.ad, aktiv = f.ad == secili) { secili = f.ad }
            }
        }
        Spacer(Modifier.height(11.dp))
        DemoCode(fn.code)
        Spacer(Modifier.height(11.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.bgSunken)
                .padding(11.dp)
        ) {
            DemoFact("Kontekst", fn.ctx)
            DemoFact("Qaytarır", fn.ret)
        }
        Spacer(Modifier.height(11.dp))

        DemoScreen(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.ok)) { append(fn.out) }
            }
        )
        Spacer(Modifier.height(11.dp))
        DemoNote(fn.use, basliq = "Nə vaxt işlədilir?")
    }
}

/* ============================================================
   6. Coroutine vizualizatoru
   ============================================================ */

private data class CoroTask(val name: String, val dur: Int)

private val CORO_TASKS = listOf(
    CoroTask("apiCagirisi", 1500),
    CoroTask("bazaSorgusu", 1000),
    CoroTask("faylOxuma", 800)
)

// Animasiya real vaxtdan 1.6 dəfə sürətli gedir — 3300 ms-lik gözləmə
// ekranda darıxdırıcı olmasın deyə.
private const val CORO_SPEED = 1.6f

private const val CORO_KOD_ARDICIL =
    "// Ardıcıl — hər biri o birini gözləyir\n" +
        "suspend fun yukle() {\n" +
        "    val a = apiCagirisi()   // 1500 ms\n" +
        "    val b = bazaSorgusu()   // 1000 ms\n" +
        "    val c = faylOxuma()     // 800 ms\n" +
        "} // cəmi ~3300 ms"

private const val CORO_KOD_PARALEL =
    "// Paralel — hamısı eyni anda başlayır\n" +
        "suspend fun yukle() = coroutineScope {\n" +
        "    val a = async { apiCagirisi() }\n" +
        "    val b = async { bazaSorgusu() }\n" +
        "    val c = async { faylOxuma() }\n" +
        "    listOf(a.await(), b.await(), c.await())\n" +
        "} // cəmi ~1500 ms"

@Composable
private fun CoroutinesDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    var paralel by remember { mutableStateOf(false) }
    var isleyir by remember { mutableStateOf(false) }
    var kecen by remember { mutableFloatStateOf(0f) }
    var bitmis by remember { mutableStateOf(false) }

    // Hər tapşırığın başlama anı: paraleldə hamısı 0-da, ardıcılda
    // əvvəlkilərin cəmindən sonra.
    val baslangiclar = remember(paralel) {
        if (paralel) CORO_TASKS.map { 0 }
        else {
            var acc = 0
            CORO_TASKS.map { t -> acc.also { acc += t.dur } }
        }
    }
    val umumi = remember(paralel) {
        if (paralel) CORO_TASKS.maxOf { it.dur } else CORO_TASKS.sumOf { it.dur }
    }

    // Animasiya dövrü — kadr saatı ilə. Açarlar `isleyir` və `paralel`-dir:
    // rejim dəyişəndə köhnə dövr ləğv olunur.
    LaunchedEffect(isleyir, paralel) {
        if (!isleyir) return@LaunchedEffect
        var baslangic = -1L
        while (true) {
            val indi = withInfiniteAnimationFrameMillis { it }
            if (baslangic < 0L) baslangic = indi
            val el = (indi - baslangic) * CORO_SPEED
            kecen = el
            if (el >= umumi) {
                kecen = umumi.toFloat()
                bitmis = true
                isleyir = false
                break
            }
        }
    }

    DemoCard(
        basliq = "Coroutine vizualizatoru",
        tesvir = "Eyni üç tapşırıq — ardıcıl və paralel icrada nə qədər çəkir?",
        modifier = modifier
    ) {
        FlowChips {
            DemoChip(etiket = "Ardıcıl", aktiv = !paralel) {
                paralel = false; isleyir = false; kecen = 0f; bitmis = false
            }
            DemoChip(etiket = "Paralel (async)", aktiv = paralel) {
                paralel = true; isleyir = false; kecen = 0f; bitmis = false
            }
        }
        Spacer(Modifier.height(11.dp))
        DemoCode(if (paralel) CORO_KOD_PARALEL else CORO_KOD_ARDICIL)
        Spacer(Modifier.height(13.dp))

        // Zolaqlar
        CORO_TASKS.forEachIndexed { i, t ->
            val yerli = kecen - baslangiclar[i]
            val pct = min(1f, max(0f, yerli / t.dur))
            val gosterilenMs = min(max(yerli, 0f), t.dur.toFloat()).roundToInt()

            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t.name,
                    modifier = Modifier.width(96.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textDim,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.bgSunken)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(pct)
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (pct >= 1f) c.ok.copy(alpha = 0.85f) else c.accent)
                    )
                    if (pct > 0.18f) {
                        Text(
                            text = "$gosterilenMs ms",
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 7.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(11.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isleyir) "Dayandır" else if (bitmis) "Yenidən" else "Başla",
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(KotlinBrush)
                    .clickable {
                        if (isleyir) {
                            isleyir = false
                            kecen = 0f
                            bitmis = false
                        } else {
                            kecen = 0f
                            bitmis = false
                            isleyir = true
                        }
                    }
                    .padding(horizontal = 17.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${min(kecen, umumi.toFloat()).roundToInt()} ms",
                style = MaterialTheme.typography.titleMedium,
                color = c.accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(11.dp))
        DemoNote(
            metn = when {
                bitmis && paralel -> "Paralel: 1500 ms — yalnız ən uzun tapşırıq qədər çəkdi. 2.2 dəfə sürətli!"
                bitmis -> "Ardıcıl: 3300 ms — hər tapşırıq öz növbəsini gözlədi."
                isleyir -> "İcra gedir…"
                else -> "Başla düyməsini basın."
            }
        )
    }
}
