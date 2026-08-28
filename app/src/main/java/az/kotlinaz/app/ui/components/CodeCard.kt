package az.kotlinaz.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.ui.highlight.highlightOutput
import az.kotlinaz.app.ui.highlight.rememberHighlighted
import az.kotlinaz.app.ui.theme.KAz

/**
 * Saytdakı `.code-card` blokunun nativ qarşılığı.
 * Nəticə kartın içində saxlanılır — «İşlə» düyməsi oflayn işləyir,
 * çünki çıxış məzmunla birlikdə gəlir.
 */
@Composable
fun CodeCard(
    code: String,
    modifier: Modifier = Modifier,
    title: String = "",
    output: String? = null,
    onOpenInPlayground: ((String) -> Unit)? = null
) {
    val c = KAz.colors
    val context = LocalContext.current
    val bildir = LocalBildiris.current
    // Nəticə paneli əvvəlcə bağlıdır. `remember(code)` — kart yeni koda
    // təkrar işlədiləndə (LazyColumn elementləri təkrar istifadə olunur)
    // köhnə kartın açıq vəziyyəti yenisinə keçməsin deyə.
    var neticeGorunur by remember(code) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
            .background(c.bgCode, RoundedCornerShape(14.dp))
    ) {
        // ---- Başlıq zolağı ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.bgSunken, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(Color(0xFFFF5F57), Color(0xFFFEBC2E), Color(0xFF28C840)).forEach { nöqtə ->
                    Box(
                        Modifier
                            .size(9.dp)
                            .background(nöqtə.copy(alpha = 0.85f), CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = title.ifBlank { "Kotlin" },
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = c.textDim,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )

            // «İşlə» yalnız nəticəsi olan nümunələrdə görünür. Nəticə məzmunla
            // birlikdə gəlir, ona görə düymə oflayn da işləyir — heç nə icra olunmur,
            // sadəcə hazır çıxış açılır.
            if (output != null) {
                KodDuymesi(
                    ikon = Icons.Outlined.PlayArrow,
                    etiket = if (neticeGorunur) "Gizlət" else "İşlə",
                    vurgulu = true
                ) { neticeGorunur = !neticeGorunur }
                Spacer(Modifier.width(6.dp))
            }

            if (onOpenInPlayground != null) {
                // AutoMirrored variant: sağdan-sola dillərdə ikon da güzgülənir.
                KodDuymesi(ikon = Icons.AutoMirrored.Outlined.OpenInNew, etiket = "Meydan") {
                    onOpenInPlayground(code)
                }
                Spacer(Modifier.width(6.dp))
            }

            KodDuymesi(ikon = Icons.Outlined.ContentCopy, etiket = "Kopyala") {
                kopyala(context, code)
                // Android 13+ sistemin öz bildirişini göstərir — təkrarlamırıq
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    bildir("Kod kopyalandı")
                }
            }
        }

        // ---- Kod ----
        // Rəngləmə kod və tema dəyişməyincə keşdə qalır; uzun blokda
        // hər yenidən qurulmada təkrar hesablanmasın.
        val rengli = rememberHighlighted(code)
        Box(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = rengli,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = (13 * KAz.codeScale).sp,
                lineHeight = (21 * KAz.codeScale).sp,
                color = c.text,
                softWrap = false
            )
        }

        // ---- Nəticə ----
        AnimatedVisibility(
            visible = neticeGorunur && output != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.bgSunken)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Nəticə",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = c.textFaint,
                    fontWeight = FontWeight.SemiBold
                )
                // DÜZƏLİŞ: bu Column-un içindədir, ona görə width() heç bir
                // şaquli boşluq yaratmırdı — «Nəticə» yazısı çıxışa yapışırdı.
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Text(
                        text = highlightOutput(output.orEmpty(), c.ok, c.err),
                        fontFamily = FontFamily.Monospace,
                        fontSize = (12.5f * KAz.codeScale).sp,
                        lineHeight = (19 * KAz.codeScale).sp,
                        softWrap = false,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KodDuymesi(
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    etiket: String,
    vurgulu: Boolean = false,
    onClick: () -> Unit
) {
    val c = KAz.colors
    Row(
        modifier = Modifier
            .border(1.dp, if (vurgulu) c.accent.copy(alpha = 0.5f) else c.border, RoundedCornerShape(8.dp))
            .background(if (vurgulu) c.accentSoft else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = ikon,
            contentDescription = etiket,
            tint = if (vurgulu) c.accent else c.textDim,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = etiket,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = if (vurgulu) c.accent else c.textDim,
            fontWeight = FontWeight.SemiBold
        )
    }
}

internal fun kopyala(context: Context, metn: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("KotlinAZ", metn))
}
