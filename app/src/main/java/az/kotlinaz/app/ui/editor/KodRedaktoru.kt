package az.kotlinaz.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.ui.highlight.highlightKotlin
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinAzColors
import az.kotlinaz.app.ui.theme.LocalKodTamamlama

/**
 * Bundan uzun mətn rənglənmir.
 *
 * Rəngləmə hər vuruşda bütün sənədi gəzir; nəhəng yapışdırılmış fayl üçün bu
 * yazının gecikməsi kimi hiss olunur. Belə halda kod ağ-qara qalır, amma
 * redaktor rəvan işləyir — bu mübadilə şüurlu seçimdir.
 */
private const val MAKS_RENGLEME = 20_000

/**
 * Rəngləmə nəticəsini keşləyən çevirmə.
 *
 * `VisualTransformation.filter()` yalnız mətn dəyişəndə yox, HƏR yenidən
 * qurulmada çağırılır (kursor tərpənəndə, seçim dəyişəndə, valideyn
 * yenilənəndə…). Keşsiz variantda bu, eyni mətnin dəfələrlə rənglənməsi
 * demək idi. Burada sonuncu nəticə saxlanılır — mətn eynidirsə hazır
 * cavab qaytarılır.
 */
private class KeslenmisRengleme(private val colors: KotlinAzColors) : VisualTransformation {
    private var sonMetn: String? = null
    private var sonNetice: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val hazir = sonNetice
        if (hazir != null && sonMetn == text.text) return hazir

        // Rəngləmə simvolların sayını dəyişmir, ona görə mövqe uyğunluğu birbaşadır.
        val netice = if (text.length > MAKS_RENGLEME) {
            TransformedText(text, OffsetMapping.Identity)
        } else {
            TransformedText(highlightKotlin(text.text, colors), OffsetMapping.Identity)
        }
        sonMetn = text.text
        sonNetice = netice
        return netice
    }
}

@Composable
private fun rememberKeslenmisRengleme(): VisualTransformation {
    val colors = KAz.colors
    return remember(colors) { KeslenmisRengleme(colors) }
}

/**
 * Tətbiqin bütün redaktə oluna bilən kod sahələri — kod meydanı və praktiki
 * çalışma — bu komponenti işlədir.
 *
 * Vəziyyət `MutableState` kimi ötürülür, dəyər kimi yox. Səbəb sırf sürətdir:
 * `kod.value` YALNIZ bu funksiyanın içində oxunur, ona görə hər hərfdə yalnız
 * redaktor yenidən qurulur — valideyn ekran (düymələr, nəticə paneli, çiplər)
 * toxunulmaz qalır. Dəyər parametr kimi verilsəydi hər vuruş bütün ekranı
 * yenidən qurardı.
 */
@Composable
fun KodRedaktoru(
    kod: MutableState<TextFieldValue>,
    modifier: Modifier = Modifier,
    basliq: String = "Main.kt",
    minHundurluk: Dp = 200.dp,
    tamamlamaAcik: Boolean = LocalKodTamamlama.current,
    basliqSagi: (@Composable RowScope.() -> Unit)? = null
) {
    val c = KAz.colors
    val deyer = kod.value

    // Təkliflər yalnız kursor bir nöqtədə duranda (seçim yoxdursa) hesablanır.
    val kursor = if (deyer.selection.collapsed) deyer.selection.start else -1
    val teklifler = remember(deyer.text, kursor, tamamlamaAcik) {
        if (!tamamlamaAcik || kursor < 0) emptyList() else tekliflerTap(deyer.text, kursor)
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.bgCode)
            .border(1.dp, c.borderStrong, RoundedCornerShape(13.dp))
    ) {
        // ---- Başlıq zolağı ----
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.bgSunken)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = basliq,
                style = MaterialTheme.typography.labelMedium,
                color = c.textDim,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${deyer.text.count { it == '\n' } + 1} sətir",
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint
            )
            if (basliqSagi != null) {
                Spacer(Modifier.width(10.dp))
                basliqSagi()
            }
        }

        // ---- Təklif zolağı: kodun ÜSTÜNDƏ, redaktora yapışıq ----
        if (teklifler.isNotEmpty()) {
            TeklifZolagi(teklifler) { secilen ->
                // Cari dəyər lambdanın içində oxunur — düyməyə basılan andakı
                // mətn və kursor götürülsün deyə.
                kod.value = tamamlamaniTetbiqEt(kod.value, secilen)
            }
        }

        // ---- Kod sahəsi ----
        BasicTextField(
            value = deyer,
            onValueChange = { kod.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHundurluk)
                .padding(13.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = (13 * KAz.codeScale).sp,
                lineHeight = (21 * KAz.codeScale).sp,
                color = c.text
            ),
            // Avtomatik düzəliş və böyük hərfə keçid kod yazanda həm səhv
            // nəticə verir (`val` → `Val`), həm də IME-nin hər vuruşda əlavə
            // iş görməsinə səbəb olur. İkisi də söndürülür.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false
            ),
            visualTransformation = rememberKeslenmisRengleme(),
            cursorBrush = SolidColor(c.accent)
        )
    }
}

/** Seçilmiş təklifi yarımçıq sözün yerinə qoyur və kursoru yerləşdirir. */
internal fun tamamlamaniTetbiqEt(deyer: TextFieldValue, teklif: Tamamlama): TextFieldValue {
    val metn = deyer.text
    val kursor = deyer.selection.start
    val aralig = prefiksArali(metn, kursor) ?: return deyer

    val yeniMetn = metn.substring(0, aralig.first) + teklif.metn + metn.substring(kursor)
    val yeniKursor = (aralig.first + teklif.metn.length - teklif.kursorGeri)
        .coerceIn(0, yeniMetn.length)
    return TextFieldValue(yeniMetn, TextRange(yeniKursor))
}

@Composable
private fun TeklifZolagi(teklifler: List<Tamamlama>, onSec: (Tamamlama) -> Unit) {
    val c = KAz.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.bgElev)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        teklifler.forEach { t ->
            TeklifCipi(t) { onSec(t) }
        }
    }
}

@Composable
private fun TeklifCipi(teklif: Tamamlama, onClick: () -> Unit) {
    val c = KAz.colors
    val reng = when (teklif.nov) {
        TamamlamaNovu.ACAR -> c.synKey
        TamamlamaNovu.TIP -> c.synType
        TamamlamaNovu.FUNKSIYA -> c.synFn
        TamamlamaNovu.SABLON -> c.accent
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(reng.copy(alpha = 0.11f))
            .border(1.dp, reng.copy(alpha = 0.35f), RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(reng))
        Spacer(Modifier.width(6.dp))
        Text(
            text = teklif.etiket,
            style = MaterialTheme.typography.labelSmall,
            color = reng,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
