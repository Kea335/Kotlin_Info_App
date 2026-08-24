package az.kotlinaz.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.KotlinCompiler
import az.kotlinaz.app.data.RunResult
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.model.PracticeExercise
import az.kotlinaz.app.data.model.TheoryExercise
import az.kotlinaz.app.data.neticeniNormallasdir
import az.kotlinaz.app.ui.components.LocalBildiris
import az.kotlinaz.app.ui.components.kopyala
import az.kotlinaz.app.ui.demos.DemoCode
import az.kotlinaz.app.ui.highlight.rememberKotlinTransformation
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush
import kotlinx.coroutines.launch

private enum class Rejim(val etiket: String) { NEZERI("Nəzəri"), PRAKTIKI("Praktiki") }

/**
 * Çalışma mühərriki — saytdakı js/practice.js faylının qarşılığı.
 *
 * Nəzəri çalışmalar tam oflayn işləyir. Praktiki çalışmalarda tapşırıq,
 * başlanğıc kod, ipucu, gözlənilən nəticə və model həll də oflayndır;
 * yalnız «Yoxla» düyməsi kodu real kompilyatorda işlətmək üçün
 * internet tələb edir.
 */
@Composable
fun ExercisePlayerScreen(
    topic: ExerciseTopic,
    hellEdilmis: Set<String>,
    compiler: KotlinCompiler,
    modifier: Modifier = Modifier,
    onHellIsaretle: (String) -> Unit
) {
    val c = KAz.colors
    var rejim by remember(topic.id) { mutableStateOf(Rejim.NEZERI) }
    var seviyye by remember(topic.id) { mutableStateOf<Level?>(null) }
    var index by remember(topic.id) { mutableIntStateOf(0) }

    val nezeriSiyahi = remember(topic.id, seviyye) {
        topic.nezeri.filter { seviyye == null || it.level == seviyye?.id }
    }
    val praktikiSiyahi = remember(topic.id, seviyye) {
        topic.praktiki.filter { seviyye == null || it.level == seviyye?.id }
    }
    val say = if (rejim == Rejim.NEZERI) nezeriSiyahi.size else praktikiSiyahi.size
    val cariIndex = index.coerceIn(0, (say - 1).coerceAtLeast(0))

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item(key = "basliq") {
            Column {
                // Rejim seçimi
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Rejim.entries.forEach { r ->
                        val aktiv = r == rejim
                        Text(
                            text = r.etiket,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (aktiv) c.accentSoft else c.bgSunken)
                                .border(
                                    1.dp,
                                    if (aktiv) c.accent.copy(alpha = 0.5f) else c.border,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { rejim = r; index = 0 }
                                .padding(vertical = 9.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (aktiv) c.accent else c.textDim,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(9.dp))

                // Səviyyə filtri
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SeviyyeCipi("Hamısı", seviyye == null) { seviyye = null; index = 0 }
                    Level.entries.forEach { lv ->
                        SeviyyeCipi(lv.label, seviyye == lv) { seviyye = lv; index = 0 }
                    }
                }

                Spacer(Modifier.height(11.dp))

                if (say > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Çalışma ${cariIndex + 1} / $say",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.textDim,
                            modifier = Modifier.weight(1f)
                        )
                        val cariId = if (rejim == Rejim.NEZERI) {
                            nezeriSiyahi.getOrNull(cariIndex)?.id
                        } else {
                            praktikiSiyahi.getOrNull(cariIndex)?.id
                        }
                        if (cariId != null && hellEdilmis.contains(cariId)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = c.ok,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Həll edilib",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.ok
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(c.bgSunken)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((cariIndex + 1).toFloat() / say)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(KotlinBrush)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
            }
        }

        if (say == 0) {
            item(key = "bos") {
                Text(
                    text = "Bu səviyyədə çalışma yoxdur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textFaint,
                    modifier = Modifier.padding(vertical = 30.dp)
                )
            }
        } else if (rejim == Rejim.NEZERI) {
            val calisma = nezeriSiyahi[cariIndex]
            item(key = "nezeri-${calisma.id}") {
                NezeriKart(
                    calisma = calisma,
                    hellOlunub = hellEdilmis.contains(calisma.id),
                    onDuzgun = { onHellIsaretle(calisma.id) }
                )
            }
        } else {
            val calisma = praktikiSiyahi[cariIndex]
            item(key = "praktiki-${calisma.id}") {
                PraktikiKart(
                    calisma = calisma,
                    compiler = compiler,
                    hellOlunub = hellEdilmis.contains(calisma.id),
                    onDuzgun = { onHellIsaretle(calisma.id) }
                )
            }
        }

        item(key = "nav") {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                AddimDuymesi(
                    etiket = "Əvvəlki",
                    ikon = Icons.AutoMirrored.Outlined.ArrowBack,
                    aktiv = cariIndex > 0,
                    modifier = Modifier.weight(1f)
                ) { if (cariIndex > 0) index = cariIndex - 1 }

                AddimDuymesi(
                    etiket = "Növbəti",
                    ikon = Icons.AutoMirrored.Outlined.ArrowForward,
                    aktiv = cariIndex < say - 1,
                    solda = false,
                    modifier = Modifier.weight(1f)
                ) { if (cariIndex < say - 1) index = cariIndex + 1 }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/* ============================================================
   Nəzəri çalışma — çoxvariantlı, tam oflayn
   ============================================================ */

@Composable
private fun NezeriKart(
    calisma: TheoryExercise,
    hellOlunub: Boolean,
    onDuzgun: () -> Unit
) {
    val c = KAz.colors
    var secim by remember(calisma.id) { mutableStateOf<Int?>(null) }

    Column {
        SeviyyeNisani(calisma.level)
        Spacer(Modifier.height(9.dp))

        Text(
            text = calisma.q,
            style = MaterialTheme.typography.titleMedium.copy(lineHeight = 25.sp),
            color = c.text
        )

        if (!calisma.code.isNullOrBlank()) {
            Spacer(Modifier.height(11.dp))
            DemoCode(calisma.code)
        }

        Spacer(Modifier.height(13.dp))

        calisma.opts.forEachIndexed { i, variant ->
            val secilib = secim == i
            val duzgundur = i == calisma.a
            val cavabVerilib = secim != null

            val cerceve = when {
                !cavabVerilib -> c.border
                duzgundur -> c.ok
                secilib -> c.err
                else -> c.border
            }
            val fon = when {
                !cavabVerilib -> c.bgElev
                duzgundur -> c.ok.copy(alpha = 0.10f)
                secilib -> c.err.copy(alpha = 0.10f)
                else -> c.bgElev
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(fon)
                    .border(1.dp, cerceve, RoundedCornerShape(11.dp))
                    .clickable(enabled = !cavabVerilib) {
                        secim = i
                        if (i == calisma.a) onDuzgun()
                    }
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ('A' + i).toString(),
                    modifier = Modifier.width(22.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (cavabVerilib && duzgundur) c.ok else c.textFaint,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = variant,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.text,
                    modifier = Modifier.weight(1f)
                )
                if (cavabVerilib && duzgundur) {
                    Icon(Icons.Filled.Check, null, tint = c.ok, modifier = Modifier.size(17.dp))
                } else if (cavabVerilib && secilib) {
                    Icon(Icons.Filled.Close, null, tint = c.err, modifier = Modifier.size(17.dp))
                }
            }
        }

        if (secim != null) {
            Spacer(Modifier.height(4.dp))
            val duzgun = secim == calisma.a
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background((if (duzgun) c.ok else c.warn).copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        (if (duzgun) c.ok else c.warn).copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(13.dp)
            ) {
                Text(
                    text = if (duzgun) "Düzdür" else "Düzgün cavab: ${('A' + calisma.a)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (duzgun) c.ok else c.warn
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = calisma.exp,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = c.textDim
                )
            }
        }
    }
}

/* ============================================================
   Praktiki çalışma — kod yazma
   ============================================================ */

private sealed interface YoxlamaVeziyyeti {
    data object Bos : YoxlamaVeziyyeti
    data object Isleyir : YoxlamaVeziyyeti
    data class Duzgun(val cixis: String) : YoxlamaVeziyyeti
    data class Sehv(val gozlenilen: String, val alinan: String) : YoxlamaVeziyyeti
    data class Xeta(val basliq: String, val metn: String) : YoxlamaVeziyyeti
    data object Oflayn : YoxlamaVeziyyeti
}

@Composable
private fun PraktikiKart(
    calisma: PracticeExercise,
    compiler: KotlinCompiler,
    hellOlunub: Boolean,
    onDuzgun: () -> Unit
) {
    val c = KAz.colors
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val bildir = LocalBildiris.current

    var kod by remember(calisma.id) { mutableStateOf(calisma.starter) }
    var veziyyet by remember(calisma.id) { mutableStateOf<YoxlamaVeziyyeti>(YoxlamaVeziyyeti.Bos) }
    var ipucuGorunur by remember(calisma.id) { mutableStateOf(false) }
    var hellGorunur by remember(calisma.id) { mutableStateOf(false) }

    Column {
        SeviyyeNisani(calisma.level)
        Spacer(Modifier.height(9.dp))

        Text(
            text = calisma.tapsiriq,
            style = MaterialTheme.typography.titleMedium.copy(lineHeight = 25.sp),
            color = c.text
        )

        Spacer(Modifier.height(12.dp))

        // Gözlənilən nəticə — oflayn mövcuddur
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(c.bgSunken)
                .padding(12.dp)
        ) {
            Text(
                text = "Gözlənilən nəticə",
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Text(
                    text = calisma.gozlenilen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = c.ok,
                    softWrap = false
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Redaktor
        KodRedaktoru(kod = kod, onChange = { kod = it })

        Spacer(Modifier.height(10.dp))

        // Düymələr
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = if (veziyyet is YoxlamaVeziyyeti.Isleyir) "Yoxlanılır…" else "Yoxla",
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(KotlinBrush)
                    .clickable(enabled = veziyyet !is YoxlamaVeziyyeti.Isleyir) {
                        veziyyet = YoxlamaVeziyyeti.Isleyir
                        scope.launch {
                            veziyyet = when (val r = compiler.isle(kod)) {
                                is RunResult.Ok -> {
                                    val alinan = neticeniNormallasdir(r.output)
                                    val gozlenilen = neticeniNormallasdir(calisma.gozlenilen)
                                    if (alinan == gozlenilen) {
                                        onDuzgun()
                                        YoxlamaVeziyyeti.Duzgun(alinan)
                                    } else {
                                        YoxlamaVeziyyeti.Sehv(gozlenilen, alinan.ifBlank { "(boş)" })
                                    }
                                }
                                is RunResult.CompileError ->
                                    YoxlamaVeziyyeti.Xeta(
                                        "Kompilyasiya xətası",
                                        r.errors.joinToString("\n")
                                    )
                                is RunResult.Crashed ->
                                    YoxlamaVeziyyeti.Xeta("Çalışma vaxtı xətası", r.exception)
                                RunResult.Offline -> YoxlamaVeziyyeti.Oflayn
                                is RunResult.Failed ->
                                    YoxlamaVeziyyeti.Xeta("Bağlantı problemi", r.message)
                            }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )

            KicikDuyme("İpucu") { ipucuGorunur = true }
            KicikDuyme("Model həll") { hellGorunur = true; kod = calisma.hell }
            KicikDuyme("Sıfırla") {
                kod = calisma.starter
                veziyyet = YoxlamaVeziyyeti.Bos
                hellGorunur = false
            }
        }

        Spacer(Modifier.height(7.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            KicikDuyme("Kodu kopyala") {
                kopyala(context, kod)
                bildir("Kod kopyalandı")
            }
            if (!hellOlunub) {
                KicikDuyme("Həll etdim") {
                    onDuzgun()
                    bildir("Çalışma həll edilmiş kimi işarələndi")
                }
            }
        }

        if (ipucuGorunur && !calisma.ipucu.isNullOrBlank()) {
            Spacer(Modifier.height(11.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(c.warn.copy(alpha = 0.08f))
                    .border(1.dp, c.warn.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "İpucu",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.warn,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = calisma.ipucu,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = c.textDim
                )
            }
        }

        if (hellGorunur) {
            Spacer(Modifier.height(11.dp))
            Text(
                text = "Model həll redaktora yükləndi",
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint
            )
        }

        Spacer(Modifier.height(12.dp))
        YoxlamaNeticesi(veziyyet)
    }
}

@Composable
private fun YoxlamaNeticesi(veziyyet: YoxlamaVeziyyeti) {
    val c = KAz.colors
    when (veziyyet) {
        YoxlamaVeziyyeti.Bos -> Unit

        YoxlamaVeziyyeti.Isleyir -> Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(c.bgSunken)
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = c.accent
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Kod JetBrains serverində işlədilir — adətən 3–10 saniyə.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textDim
            )
        }

        is YoxlamaVeziyyeti.Duzgun -> NeticePaneli(
            basliq = "Düzdür! Nəticə tam uyğundur.",
            reng = c.ok
        ) {
            KonsolMetni(veziyyet.cixis, c.ok)
        }

        is YoxlamaVeziyyeti.Sehv -> NeticePaneli(
            basliq = "Nəticə uyğun gəlmədi",
            reng = c.err
        ) {
            Text(
                text = "Gözlənilən",
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint
            )
            KonsolMetni(veziyyet.gozlenilen, c.ok)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sənin nəticən",
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint
            )
            KonsolMetni(veziyyet.alinan, c.err)
        }

        is YoxlamaVeziyyeti.Xeta -> NeticePaneli(
            basliq = veziyyet.basliq,
            reng = c.err
        ) {
            KonsolMetni(veziyyet.metn, c.err)
        }

        YoxlamaVeziyyeti.Oflayn -> NeticePaneli(
            basliq = "İnternet yoxdur",
            reng = c.warn
        ) {
            Text(
                text = "Kodun işlədilməsi üçün bağlantı lazımdır — Kotlin kompilyatoru " +
                    "JetBrains serverlərindədir. Bağlantısız da işləyə bilərsən: " +
                    "«Model həll» düyməsi ilə düzgün həlli aç və öz kodunla müqayisə et, " +
                    "sonra «Həll etdim» ilə işarələ.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = c.textDim
            )
        }
    }
}

@Composable
private fun NeticePaneli(
    basliq: String,
    reng: Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(reng.copy(alpha = 0.08f))
            .border(1.dp, reng.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(13.dp)
    ) {
        Text(
            text = basliq,
            style = MaterialTheme.typography.labelLarge,
            color = reng
        )
        Spacer(Modifier.height(7.dp))
        content()
    }
}

@Composable
private fun KonsolMetni(metn: String, reng: Color) {
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Text(
            text = metn,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
            color = reng,
            softWrap = false
        )
    }
}

/* ============================================================
   Kod redaktoru
   ============================================================ */

@Composable
private fun KodRedaktoru(kod: String, onChange: (String) -> Unit) {
    val c = KAz.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bgCode)
            .border(1.dp, c.borderStrong, RoundedCornerShape(12.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.bgSunken)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Main.kt",
                style = MaterialTheme.typography.labelSmall,
                color = c.textDim,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${kod.lines().size} sətir",
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint
            )
        }

        BasicTextField(
            value = kod,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp)
                .padding(12.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = (13 * KAz.codeScale).sp,
                lineHeight = (21 * KAz.codeScale).sp,
                color = c.text
            ),
            visualTransformation = rememberKotlinTransformation(),
            cursorBrush = SolidColor(c.accent)
        )
    }
}

/* ============================================================
   Kiçik ünsürlər
   ============================================================ */

@Composable
private fun SeviyyeNisani(level: String) {
    val c = KAz.colors
    val lv = Level.from(level)
    val reng = when (lv) {
        Level.JUNIOR -> c.ok
        Level.MIDDLE -> c.warn
        Level.SENIOR -> c.err
    }
    Text(
        text = lv.label,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(reng.copy(alpha = 0.13f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = reng,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SeviyyeCipi(etiket: String, aktiv: Boolean, onClick: () -> Unit) {
    val c = KAz.colors
    Text(
        text = etiket,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (aktiv) c.accentSoft else c.bgSunken)
            .border(
                1.dp,
                if (aktiv) c.accent.copy(alpha = 0.5f) else c.border,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = if (aktiv) c.accent else c.textDim,
        fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
private fun KicikDuyme(etiket: String, onClick: () -> Unit) {
    val c = KAz.colors
    Text(
        text = etiket,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(c.bgSunken)
            .border(1.dp, c.border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = c.textDim
    )
}

@Composable
private fun AddimDuymesi(
    etiket: String,
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    aktiv: Boolean,
    modifier: Modifier = Modifier,
    solda: Boolean = true,
    onClick: () -> Unit
) {
    val c = KAz.colors
    Row(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(11.dp))
            .then(if (aktiv) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (solda) {
            Icon(ikon, null, tint = if (aktiv) c.text else c.border, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = etiket,
            style = MaterialTheme.typography.labelLarge,
            color = if (aktiv) c.text else c.textFaint
        )
        if (!solda) {
            Spacer(Modifier.width(6.dp))
            Icon(ikon, null, tint = if (aktiv) c.text else c.border, modifier = Modifier.size(15.dp))
        }
    }
}
