package az.kotlinaz.app.ui.demos

import androidx.compose.ui.semantics.Role
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import az.kotlinaz.app.R
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
import androidx.compose.runtime.saveable.rememberSaveable
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

/** [tip] Kotlin tipidir (tərcümə olunmur), [qeyd] izahın resursu. */
private data class TipNeticesi(val tip: String, @param:StringRes val qeyd: Int)

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
    if (v.isEmpty()) return TipNeticesi("?", R.string.demo_tip_bos)
    if (R_LONG.matches(v)) return TipNeticesi("Long", R.string.demo_tip_long_l)
    if (R_INT.matches(v)) {
        val n = v.toLongOrNull() ?: 0L
        return if (n > Int.MAX_VALUE || n < Int.MIN_VALUE) {
            TipNeticesi("Long", R.string.demo_tip_long_boyuk)
        } else {
            TipNeticesi("Int", R.string.demo_tip_int)
        }
    }
    if (R_FLOAT.matches(v)) return TipNeticesi("Float", R.string.demo_tip_float)
    if (R_DOUBLE.matches(v)) return TipNeticesi("Double", R.string.demo_tip_double)
    if (R_BOOL.matches(v)) return TipNeticesi("Boolean", R.string.demo_tip_boolean)
    if (R_CHAR.matches(v)) return TipNeticesi("Char", R.string.demo_tip_char)
    if (R_STRING.matches(v)) return TipNeticesi("String", R.string.demo_tip_string)
    if (v == "null") {
        return TipNeticesi(
            "Nothing?",
            R.string.demo_tip_null
        )
    }
    if (v.startsWith("listOf(")) {
        val inner = v.removePrefix("listOf(").removeSuffix(")").trim()
        if (inner.isEmpty()) {
            return TipNeticesi(
                "List<Nothing>",
                R.string.demo_tip_bos_siyahi
            )
        }
        val ilk = inner.split(",").first().trim()
        val t = when {
            R_INT.matches(ilk) -> "Int"
            R_STRING.matches(ilk) -> "String"
            else -> "Any"
        }
        return TipNeticesi("List<$t>", R.string.demo_tip_list)
    }
    if (v.startsWith("mutableListOf(")) {
        return TipNeticesi("MutableList<Int>", R.string.demo_tip_mutable_list)
    }
    if (v.startsWith("mapOf(")) return TipNeticesi("Map<K, V>", R.string.demo_tip_map)
    if (v.startsWith("setOf(")) return TipNeticesi("Set<Int>", R.string.demo_tip_set)
    if (v.startsWith("arrayOf(")) return TipNeticesi("Array<Int>", R.string.demo_tip_array)
    if (R_LAMBDA.matches(v)) {
        return TipNeticesi("() -> Unit", R.string.demo_tip_lambda)
    }
    return TipNeticesi("String", R.string.demo_tip_taninmayan)
}

private val TI_NUMUNELER = listOf(
    "42", "42L", "3.14", "3.14f", "true", "'A'", "\"Kotlin\"", "null",
    "listOf(1, 2, 3)", "10000000000"
)

@Composable
private fun TypeInferDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    var giris by rememberSaveable { mutableStateOf("42") }
    val netice = remember(giris) { tipCixar(giris) }

    DemoCard(
        basliq = stringResource(R.string.demo_tip_basliq),
        tesvir = stringResource(R.string.demo_tip_tesvir),
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
            "val x = $gosterilen\n\n// ${stringResource(R.string.demo_tip_kod_serhi)}\nval x: ${netice.tip} = $gosterilen"
        )
        Spacer(Modifier.height(11.dp))

        DemoScreen(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.textFaint)) { append(stringResource(R.string.demo_tip_cixarilan)) }
                withStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Bold)) { append(netice.tip) }
            }
        )
        Spacer(Modifier.height(11.dp))
        DemoNote(stringResource(netice.qeyd))
    }
}

/* ============================================================
   2. Null-safety laboratoriyası
   ============================================================ */

private enum class NsKind { OK, COMPILE, CRASH }
/**
 * [msg] konsolun göstərdiyidir (kod çıxışı və ya kompilyator mesajı —
 * tərcümə olunmur); [msgRes] verilibsə onun yerinə UI mətni; [note] izahdır.
 */
private data class NsNetice(
    val kind: NsKind,
    val msg: String,
    @param:StringRes val note: Int,
    @param:StringRes val msgRes: Int? = null
)

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
            R.string.demo_ns_direct
        )
    },
    NsOp("safe", "ad?.length") { isNull ->
        if (isNull) NsNetice(NsKind.OK, "null", R.string.demo_ns_safe_null)
        else NsNetice(NsKind.OK, "6", R.string.demo_ns_safe_dolu)
    },
    NsOp("bang", "ad!!.length") { isNull ->
        if (isNull) NsNetice(
            NsKind.CRASH,
            "Exception in thread \"main\"\nkotlin.KotlinNullPointerException\n\tat MainKt.main(Main.kt:4)",
            R.string.demo_ns_bang_null
        )
        else NsNetice(NsKind.OK, "6", R.string.demo_ns_bang_dolu)
    },
    NsOp("elvis", "ad?.length ?: 0") { isNull ->
        if (isNull) NsNetice(NsKind.OK, "0", R.string.demo_ns_elvis_null)
        else NsNetice(NsKind.OK, "6", R.string.demo_ns_elvis_dolu)
    },
    NsOp("let", "ad?.let { it.length }") { isNull ->
        if (isNull) NsNetice(NsKind.OK, "null", R.string.demo_ns_let_null)
        else NsNetice(NsKind.OK, "6", R.string.demo_ns_let_dolu)
    },
    NsOp("ifcheck", "if (ad != null) ad.length") { isNull ->
        if (isNull) NsNetice(NsKind.OK, "", R.string.demo_ns_if_null, msgRes = R.string.demo_ns_hec_ne)
        else NsNetice(NsKind.OK, "6", R.string.demo_ns_if_dolu)
    }
)

@Composable
private fun NullSafetyDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    var deyerVar by rememberSaveable { mutableStateOf(false) }
    var opId by rememberSaveable { mutableStateOf("direct") }

    val op = NS_OPS.firstOrNull { it.id == opId } ?: NS_OPS.first()
    val isNull = !deyerVar
    val res = remember(opId, isNull) { op.run(isNull) }

    DemoCard(
        basliq = stringResource(R.string.demo_ns_basliq),
        tesvir = stringResource(R.string.demo_ns_tesvir),
        modifier = modifier
    ) {
        DemoToggleRow(
            etiket = stringResource(R.string.demo_ns_deyer_var),
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

        val basliq = stringResource(
            when (res.kind) {
                NsKind.COMPILE -> R.string.demo_ns_kompilyasiya
                NsKind.CRASH -> R.string.demo_ns_cokme
                NsKind.OK -> R.string.demo_ns_netice
            }
        )
        val msg = res.msgRes?.let { stringResource(it) } ?: res.msg
        DemoScreen(
            basliq = basliq,
            metn = buildAnnotatedString {
                withStyle(
                    SpanStyle(color = if (res.kind == NsKind.OK) c.ok else c.err)
                ) { append(msg) }
            }
        )
        Spacer(Modifier.height(11.dp))
        DemoNote(stringResource(res.note), basliq = stringResource(R.string.demo_ns_ne_bas_verdi))
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
        basliq = stringResource(R.string.demo_zencir_basliq),
        tesvir = stringResource(R.string.demo_zencir_tesvir),
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
        DemoChip(etiket = stringResource(R.string.demo_zencir_temizle)) { zencir.clear() }
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
        val terminalSonra = stringResource(R.string.demo_zencir_terminal)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ChainStep("listOf(…)", colFmt(COL_BASE), aktiv = true)

            zencir.forEach { id ->
                val o = COL_OPS.firstOrNull { it.id == id } ?: return@forEach
                if (bitdi) {
                    ChainStep(
                        o.label,
                        terminalSonra,
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
                withStyle(SpanStyle(color = c.textFaint)) { append(stringResource(R.string.demo_zencir_netice)) }
                withStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Bold)) { append(colFmt(cari)) }
            }
        )
    }
}

/* ============================================================
   4. when ifadəsi sınaqçısı
   ============================================================ */

// i18n-ignore-start — kod nümunəsinin literal-ları və onların çıxışı tərcümə
// olunmur (T4.0: kodda yalnız şərhlər); budaqlar və nəticələr eyni qalmalıdır.
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
    // Boş girişdə budaq yoxdur — UI «(boş giriş)» yazır.
    if (v.isEmpty()) return WhenNetice(null, "")

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
// i18n-ignore-end

@Composable
private fun WhenDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    var giris by rememberSaveable { mutableStateOf("7") }
    val netice = remember(giris) { whenQiymetlendir(giris) }

    DemoCard(
        basliq = stringResource(R.string.demo_when_basliq),
        tesvir = stringResource(R.string.demo_when_tesvir),
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
                ) { append(if (netice.branch == null) stringResource(R.string.demo_when_bos_giris) else netice.out) }
            }
        )
    }
}

/* ============================================================
   5. Scope funksiyaları müqayisəsi
   ============================================================ */

/**
 * [code] və [out] koddur — identifikatorlar və literal-lar tərcümə olunmur
 * (T4.0); koddakı `%1$s` yerinə tərcümə olunan şərh qoyulur ([serh]).
 */
private data class ScopeFn(
    val ad: String,
    val code: String,
    val ctx: String,
    @param:StringRes val ret: Int,
    @param:StringRes val use: Int,
    val out: String,
    @param:StringRes val serh: Int? = null
)

private val SCOPE_FNS = listOf(
    ScopeFn(
        "let",
        "val uzunluq = ad?.let {\n    println(it)      // it = \"Kotlin\"\n    it.length        // %1\$s\n}",
        "it", R.string.demo_scope_lambda_neticesi,
        R.string.demo_scope_let,
        "Kotlin\nuzunluq = 6",
        serh = R.string.demo_scope_son_setir
    ),
    ScopeFn(
        "run",
        "val uzunluq = ad.run {\n    println(this)    // this = \"Kotlin\"\n    length           // this.length\n}",
        "this", R.string.demo_scope_lambda_neticesi,
        R.string.demo_scope_run,
        "Kotlin\nuzunluq = 6"
    ),
    ScopeFn(
        "with",
        "val netice = with(ad) {\n    println(this)    // this = \"Kotlin\"\n    uppercase()\n}",
        "this", R.string.demo_scope_lambda_neticesi,
        R.string.demo_scope_with,
        "Kotlin\nnetice = KOTLIN"
    ),
    ScopeFn(
        "apply",
        "val istifadeci = Istifadeci().apply {\n    ad = \"Aysel\"     // this.ad\n    yas = 24\n}",
        "this", R.string.demo_scope_obyektin_ozu,
        R.string.demo_scope_apply,
        "istifadeci = Istifadeci(ad=Aysel, yas=24)"
    ),
    // i18n-ignore-start — kod və onun çıxışı (T4.0)
    ScopeFn(
        "also",
        "val siyahi = mutableListOf(1, 2)\n    .also { println(\"əvvəl: \$it\") }\n    .apply { add(3) }\n    .also { println(\"sonra: \$it\") }",
        "it", R.string.demo_scope_obyektin_ozu,
        R.string.demo_scope_also,
        "əvvəl: [1, 2]\nsonra: [1, 2, 3]"
    )
    // i18n-ignore-end
)

@Composable
private fun ScopeDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    var secili by rememberSaveable { mutableStateOf("let") }
    val fn = SCOPE_FNS.firstOrNull { it.ad == secili } ?: SCOPE_FNS.first()

    DemoCard(
        basliq = stringResource(R.string.demo_scope_basliq),
        tesvir = stringResource(R.string.demo_scope_tesvir),
        modifier = modifier
    ) {
        FlowChips {
            SCOPE_FNS.forEach { f ->
                DemoChip(etiket = f.ad, aktiv = f.ad == secili) { secili = f.ad }
            }
        }
        Spacer(Modifier.height(11.dp))
        DemoCode(fn.serh?.let { fn.code.format(stringResource(it)) } ?: fn.code)
        Spacer(Modifier.height(11.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.bgSunken)
                .padding(11.dp)
        ) {
            DemoFact(stringResource(R.string.demo_scope_kontekst), fn.ctx)
            DemoFact(stringResource(R.string.demo_scope_qaytarir), stringResource(fn.ret))
        }
        Spacer(Modifier.height(11.dp))

        DemoScreen(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.ok)) { append(fn.out) }
            }
        )
        Spacer(Modifier.height(11.dp))
        DemoNote(stringResource(fn.use), basliq = stringResource(R.string.demo_scope_ne_vaxt))
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

// Şərhlər (%1$s, %2$s) tərcümə olunur, kod yox (T4.0).
private const val CORO_KOD_ARDICIL =
    "// %1\$s\n" +
        "suspend fun yukle() {\n" +
        "    val a = apiCagirisi()   // 1500 ms\n" +
        "    val b = bazaSorgusu()   // 1000 ms\n" +
        "    val c = faylOxuma()     // 800 ms\n" +
        "} // %2\$s"

private const val CORO_KOD_PARALEL =
    "// %1\$s\n" +
        "suspend fun yukle() = coroutineScope {\n" +
        "    val a = async { apiCagirisi() }\n" +
        "    val b = async { bazaSorgusu() }\n" +
        "    val c = async { faylOxuma() }\n" +
        "    listOf(a.await(), b.await(), c.await())\n" +
        "} // %2\$s"

@Composable
private fun CoroutinesDemo(modifier: Modifier = Modifier) {
    val c = KAz.colors
    var paralel by rememberSaveable { mutableStateOf(false) }
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
        basliq = stringResource(R.string.demo_coro_basliq),
        tesvir = stringResource(R.string.demo_coro_tesvir),
        modifier = modifier
    ) {
        FlowChips {
            DemoChip(etiket = stringResource(R.string.demo_coro_ardicil), aktiv = !paralel) {
                paralel = false; isleyir = false; kecen = 0f; bitmis = false
            }
            DemoChip(etiket = stringResource(R.string.demo_coro_paralel), aktiv = paralel) {
                paralel = true; isleyir = false; kecen = 0f; bitmis = false
            }
        }
        Spacer(Modifier.height(11.dp))
        DemoCode(
            if (paralel) {
                CORO_KOD_PARALEL.format(
                    stringResource(R.string.demo_coro_serh_paralel),
                    stringResource(R.string.demo_coro_serh_cemi, 1500)
                )
            } else {
                CORO_KOD_ARDICIL.format(
                    stringResource(R.string.demo_coro_serh_ardicil),
                    stringResource(R.string.demo_coro_serh_cemi, 3300)
                )
            }
        )
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
                            text = stringResource(R.string.demo_ms, gosterilenMs),
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
                text = stringResource(
                    if (isleyir) R.string.demo_coro_dayandir
                    else if (bitmis) R.string.demo_coro_yeniden
                    else R.string.demo_coro_basla
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(KotlinBrush)
                    .clickable(role = Role.Button) {
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
                text = stringResource(R.string.demo_ms, min(kecen, umumi.toFloat()).roundToInt()),
                style = MaterialTheme.typography.titleMedium,
                color = c.accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(11.dp))
        DemoNote(
            metn = stringResource(
                when {
                    bitmis && paralel -> R.string.demo_coro_paralel_bitdi
                    bitmis -> R.string.demo_coro_ardicil_bitdi
                    isleyir -> R.string.demo_coro_gedir
                    else -> R.string.demo_coro_baslayin
                }
            )
        )
    }
}
