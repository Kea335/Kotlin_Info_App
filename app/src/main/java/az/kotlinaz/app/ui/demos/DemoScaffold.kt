package az.kotlinaz.app.ui.demos

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.ui.highlight.rememberHighlighted
import az.kotlinaz.app.ui.theme.KAz

/* ============================================================
   İnteraktiv nümayişlər üçün ortaq görünüş elementləri.
   Saytdakı .demo, .chip, .demo-screen siniflərinin qarşılığı.
   ============================================================ */

@Composable
fun DemoCard(
    basliq: String,
    tesvir: String,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val c = KAz.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.bgElev)
            .border(1.dp, c.borderStrong, RoundedCornerShape(16.dp))
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "İnteraktiv",
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(c.accentSoft)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = c.accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = basliq,
                style = MaterialTheme.typography.titleSmall,
                color = c.text
            )
        }
        if (tesvir.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = tesvir,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                color = c.textDim
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** Saytdakı `.chip` düyməsi. */
@Composable
fun DemoChip(
    etiket: String,
    aktiv: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = KAz.colors
    Text(
        text = etiket,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (aktiv) c.accentSoft else c.bgSunken)
            .border(
                1.dp,
                if (aktiv) c.accent.copy(alpha = 0.55f) else c.border,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = if (aktiv) c.accent else c.textDim,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1
    )
}

/** Çipləri sətirlərə bölüb göstərir (FlowRow əvəzi — sadə və proqnozlu). */
@Composable
fun ChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) { content() }
}

/** Konsol görünüşü — saytdakı `.demo-screen`. */
@Composable
fun DemoScreen(
    metn: AnnotatedString,
    modifier: Modifier = Modifier,
    basliq: String? = null
) {
    val c = KAz.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(c.bgCode)
            .border(1.dp, c.border, RoundedCornerShape(11.dp))
            .padding(12.dp)
    ) {
        if (basliq != null) {
            Text(
                text = basliq,
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(5.dp))
        }
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Text(
                text = metn,
                fontFamily = FontFamily.Monospace,
                fontSize = (13 * KAz.codeScale).sp,
                lineHeight = (20 * KAz.codeScale).sp,
                softWrap = false
            )
        }
    }
}

@Composable
fun DemoScreen(metn: String, modifier: Modifier = Modifier, basliq: String? = null) =
    DemoScreen(AnnotatedString(metn), modifier, basliq)

/** Rənglənmiş kod paneli — demo daxilində. */
@Composable
fun DemoCode(kod: String, modifier: Modifier = Modifier) {
    val c = KAz.colors
    val rengli = rememberHighlighted(kod)
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(c.bgCode)
            .border(1.dp, c.border, RoundedCornerShape(11.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = rengli,
            modifier = Modifier.padding(12.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = (12.5f * KAz.codeScale).sp,
            lineHeight = (20 * KAz.codeScale).sp,
            color = c.text,
            softWrap = false
        )
    }
}

/** Mətn sahəsi — saytdakı `.field`. */
@Composable
fun DemoField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    val c = KAz.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.bgSunken)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                color = c.textFaint,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalTextStyle.current.copy(
                color = c.text,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(c.accent),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Açar-dəyər sətri — demo izahlarında. */
@Composable
fun DemoFact(acar: String, deyer: String, modifier: Modifier = Modifier) {
    val c = KAz.colors
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = acar,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.labelSmall,
            color = c.textFaint
        )
        Text(
            text = deyer,
            style = MaterialTheme.typography.labelMedium,
            color = c.text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** İzah mətni — «Nə baş verdi?» blokları. */
@Composable
fun DemoNote(metn: String, basliq: String? = null, modifier: Modifier = Modifier) {
    val c = KAz.colors
    Column(modifier.fillMaxWidth()) {
        if (basliq != null) {
            Text(
                text = basliq,
                style = MaterialTheme.typography.labelMedium,
                color = c.text,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            text = metn,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
            color = c.textDim
        )
    }
}

/** Açar (switch) sətri. */
@Composable
fun DemoToggleRow(
    etiket: String,
    checked: Boolean,
    sagMetn: String? = null,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    val c = KAz.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.bgSunken)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(24.dp)
        )
        Spacer(Modifier.width(11.dp))
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodySmall,
            color = c.textDim,
            modifier = Modifier.weight(1f)
        )
        if (sagMetn != null) {
            Text(
                text = sagMetn,
                style = MaterialTheme.typography.labelMedium,
                color = c.accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Zəncir addımı — kolleksiya və when demolarında. */
@Composable
fun ChainStep(
    sol: String,
    sag: String,
    aktiv: Boolean,
    modifier: Modifier = Modifier,
    sagReng: Color? = null
) {
    val c = KAz.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(if (aktiv) c.accentSoft else c.bgSunken.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sol,
            style = MaterialTheme.typography.labelSmall,
            color = if (aktiv) c.accent else c.textFaint,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = sag,
            style = MaterialTheme.typography.labelSmall,
            color = sagReng ?: if (aktiv) c.text else c.textDim,
            fontFamily = FontFamily.Monospace
        )
    }
}
