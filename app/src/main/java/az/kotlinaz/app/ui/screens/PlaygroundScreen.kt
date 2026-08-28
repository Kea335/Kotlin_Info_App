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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.KotlinCompiler
import az.kotlinaz.app.data.RunResult
import az.kotlinaz.app.data.model.PlaygroundPreset
import az.kotlinaz.app.ui.components.LocalBildiris
import az.kotlinaz.app.ui.components.kopyala
import az.kotlinaz.app.ui.demos.DemoChip
import az.kotlinaz.app.ui.editor.KodRedaktoru
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush
import kotlinx.coroutines.launch

// Ekranın nəticə panelinin vəziyyəti. Sealed interface seçilib ki, `when`
// bütün halları məcburi əhatə etsin — yeni hal əlavə olunanda kompilyator
// unudulmuş yerləri göstərir.
private sealed interface MeydanVeziyyeti {
    data object Bos : MeydanVeziyyeti
    data object Isleyir : MeydanVeziyyeti
    data class Cixis(val metn: String) : MeydanVeziyyeti
    data class Xetalar(val basliq: String, val metn: String) : MeydanVeziyyeti
    data object Oflayn : MeydanVeziyyeti
}

/** Boş redaktorun başlanğıc kodu — kursor gövdənin içində durur. */
private const val BOS_KOD = "fun main() {\n    \n}"

/**
 * Kod meydanı — tətbiqin YEGANƏ internet tələb edən hissəsi.
 * Kod JetBrains-in rəsmi kompilyator xidmətində işlədilir; saytdakı
 * Kotlin Playground da məhz bu xidməti çağırır.
 */
@Composable
fun PlaygroundScreen(
    presets: List<PlaygroundPreset>,
    compiler: KotlinCompiler,
    xariciKod: String?,
    modifier: Modifier = Modifier,
    onXariciKodAlindi: () -> Unit
) {
    val c = KAz.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bildir = LocalBildiris.current

    // Redaktorun vəziyyəti `MutableState` kimi saxlanılır və olduğu kimi
    // KodRedaktoru-ya verilir: hər hərf yalnız redaktoru yeniləyir, bu ekranın
    // qalan hissəsi (çiplər, düymələr, nəticə paneli) yenidən qurulmur.
    val kod = remember { mutableStateOf(TextFieldValue(BOS_KOD, TextRange(BOS_KOD.length - 2))) }
    var veziyyet by remember { mutableStateOf<MeydanVeziyyeti>(MeydanVeziyyeti.Bos) }
    // null = boş redaktor («Kod meydanı» çipi seçilidir).
    var seciliPreset by remember { mutableStateOf<String?>(null) }

    fun kodQoy(yeni: String) {
        kod.value = TextFieldValue(yeni, TextRange(yeni.length))
        veziyyet = MeydanVeziyyeti.Bos
    }

    // Kod kartındakı «Meydan» düyməsindən gələn kod. Alındıqdan sonra
    // `onXariciKodAlindi()` onu ViewModel-də təmizləyir — əks halda ekrana
    // hər qayıdışda istifadəçinin yazdığı kod yenidən əvəzlənərdi.
    LaunchedEffect(xariciKod) {
        if (xariciKod != null) {
            kodQoy(xariciKod)
            seciliPreset = null
            onXariciKodAlindi()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        item(key = "xeberdarliq") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(c.accentSoft)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).background(KotlinBrush, CircleShape))
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "Bu bölmə internet tələb edir — kod JetBrains serverlərində " +
                        "kompilyasiya olunur. Tətbiqin qalan hissəsi tam oflayn işləyir.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = c.textDim
                )
            }
            Spacer(Modifier.height(13.dp))
        }

        item(key = "numuneler") {
            Text(
                text = "Başlanğıc",
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(7.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Boş redaktor ƏN QABAQDA: meydana gələn adam əvvəlcə öz kodunu
                // yazmaq istəyir, hazır nümunə ikinci addımdır.
                DemoChip(etiket = "Kod meydanı", aktiv = seciliPreset == null) {
                    // Kursor dərhal `main` gövdəsinin içinə qoyulur —
                    // istifadəçi çipdən sonra birbaşa yazmağa başlaya bilsin.
                    kod.value = TextFieldValue(BOS_KOD, TextRange(BOS_KOD.length - 2))
                    veziyyet = MeydanVeziyyeti.Bos
                    seciliPreset = null
                }
                presets.forEach { p ->
                    DemoChip(etiket = p.label, aktiv = p.id == seciliPreset) {
                        kodQoy(p.code)
                        seciliPreset = p.id
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
        }

        item(key = "redaktor") {
            KodRedaktoru(
                kod = kod,
                minHundurluk = 210.dp,
                basliqSagi = {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = "Kopyala",
                        tint = c.textDim,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                kopyala(context, kod.value.text)
                                bildir("Kod kopyalandı")
                            }
                    )
                }
            )

            Spacer(Modifier.height(11.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (veziyyet is MeydanVeziyyeti.Isleyir) "İşləyir…" else "▶  İşlə",
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(KotlinBrush)
                        // İcra gedərkən düymə kilidlənir — eyni kodu paralel
                        // iki dəfə serverə göndərməyin mənası yoxdur.
                        .clickable(enabled = veziyyet !is MeydanVeziyyeti.Isleyir) {
                            veziyyet = MeydanVeziyyeti.Isleyir
                            scope.launch {
                                // Kod yalnız BURADA oxunur — yazı zamanı deyil.
                                veziyyet = when (val r = compiler.isle(kod.value.text)) {
                                    is RunResult.Ok ->
                                        MeydanVeziyyeti.Cixis(
                                            r.output.ifBlank { "(çıxış yoxdur)" }
                                        )
                                    is RunResult.CompileError ->
                                        MeydanVeziyyeti.Xetalar(
                                            "Kompilyasiya xətası",
                                            r.errors.joinToString("\n")
                                        )
                                    is RunResult.Crashed ->
                                        MeydanVeziyyeti.Xetalar(
                                            "Çalışma vaxtı xətası",
                                            buildString {
                                                if (r.output.isNotBlank()) {
                                                    append(r.output).append('\n')
                                                }
                                                append(r.exception)
                                            }
                                        )
                                    RunResult.Offline -> MeydanVeziyyeti.Oflayn
                                    is RunResult.Failed ->
                                        MeydanVeziyyeti.Xetalar("Bağlantı problemi", r.message)
                                }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )

                // Yalnız nəticə panelini bağlayır; redaktordakı koda toxunmur.
                // Kodu silmək üçün yuxarıdakı «Kod meydanı» çipi var.
                Text(
                    text = "Təmizlə",
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(c.bgSunken)
                        .border(1.dp, c.border, RoundedCornerShape(11.dp))
                        .clickable { veziyyet = MeydanVeziyyeti.Bos }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textDim
                )
            }

            Spacer(Modifier.height(13.dp))
            MeydanNeticesi(veziyyet)
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun MeydanNeticesi(veziyyet: MeydanVeziyyeti) {
    val c = KAz.colors

    when (veziyyet) {
        MeydanVeziyyeti.Bos -> Unit

        MeydanVeziyyeti.Isleyir -> Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(c.bgSunken)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = c.accent
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Kod serverdə kompilyasiya olunur…",
                style = MaterialTheme.typography.bodySmall,
                color = c.textDim
            )
        }

        is MeydanVeziyyeti.Cixis -> Panel("Nəticə", c.ok) {
            KonsolMetn(veziyyet.metn, c.ok)
        }

        is MeydanVeziyyeti.Xetalar -> Panel(veziyyet.basliq, c.err) {
            KonsolMetn(veziyyet.metn, c.err)
        }

        MeydanVeziyyeti.Oflayn -> Panel("İnternet yoxdur", c.warn) {
            Text(
                text = "Kod meydanı Kotlin kompilyatorunu JetBrains serverlərində işlədir, " +
                    "ona görə bağlantı olmadan işləyə bilmir. Bağlantısız da öyrənməyə " +
                    "davam edə bilərsən — bütün dərslər, 144 kod nümunəsi (nəticələri ilə " +
                    "birlikdə), 1250 çalışma və bilik testi oflayn əlçatandır.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = c.textDim
            )
        }
    }
}

@Composable
private fun Panel(
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
private fun KonsolMetn(metn: String, reng: Color) {
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Text(
            text = metn,
            fontFamily = FontFamily.Monospace,
            fontSize = (12.5f * KAz.codeScale).sp,
            lineHeight = (19 * KAz.codeScale).sp,
            color = reng,
            softWrap = false
        )
    }
}
