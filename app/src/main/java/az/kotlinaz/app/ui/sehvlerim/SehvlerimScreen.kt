package az.kotlinaz.app.ui.sehvlerim

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.kotlinaz.app.R
import az.kotlinaz.app.ui.components.YuklenmeGostericisi
import az.kotlinaz.app.ui.demos.DemoCode
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush
import java.text.DateFormat
import java.util.Date

/**
 * «Səhvlərim» (T5.2) — səhv cavab verilən suallar, ən təzəsi əvvəl. Sətrə
 * toxunanda variantlar və izah açılır; yuxarıdakı düymə yalnız bu suallardan
 * test başladır.
 */
@Composable
fun SehvlerimScreen(
    vm: SehvlerimViewModel,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val setirler by vm.setirler.collectAsStateWithLifecycle()
    when (val s = setirler) {
        null -> YuklenmeGostericisi(modifier)
        else -> if (s.isEmpty()) SehvYoxdur(modifier) else SehvSiyahisi(s, onTest, modifier)
    }
}

/** Boş hal — «Səhvlərim» və səhv testi eyni mətni göstərir. */
@Composable
fun SehvYoxdur(modifier: Modifier = Modifier) {
    val c = KAz.colors
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = c.ok, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.sehvlerim_bos),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = c.textDim,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SehvSiyahisi(setirler: List<SehvSetri>, onTest: () -> Unit, modifier: Modifier) {
    val c = KAz.colors
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item(key = "basliq") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.bgElev)
                    .border(1.dp, c.border, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = pluralStringResource(R.plurals.sehvlerim_sayi, setirler.size, setirler.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sehvlerim_izah),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = c.textDim
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.sehvlerim_test),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(KotlinBrush)
                        .clickable(onClick = onTest)
                        .padding(vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
        items(setirler, key = { it.sual.id }) { SehvKarti(it) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SehvKarti(s: SehvSetri) {
    val c = KAz.colors
    var acik by rememberSaveable(s.sual.id) { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val tarix = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(s.sonTarix))

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(13.dp))
            .clickable { acik = !acik }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = s.sual.q,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = c.text,
                maxLines = if (acik) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                if (acik) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = c.textFaint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.sehvlerim_meta,
                s.movzu ?: stringResource(R.string.basliq_bilik_testi),
                pluralStringResource(R.plurals.sehvlerim_defe, s.say, s.say),
                tarix
            ),
            style = MaterialTheme.typography.labelSmall,
            color = c.textFaint
        )

        if (acik) {
            if (!s.sual.code.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                DemoCode(s.sual.code)
            }
            Spacer(Modifier.height(10.dp))
            s.sual.opts.forEachIndexed { i, variant ->
                val duzgun = i == s.sual.a
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (duzgun) c.ok.copy(alpha = 0.10f) else c.bgSunken)
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ('A' + i).toString(),
                        modifier = Modifier.width(20.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (duzgun) c.ok else c.textFaint,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = variant,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.text
                    )
                    if (duzgun) Icon(Icons.Filled.Check, null, tint = c.ok, modifier = Modifier.size(16.dp))
                }
            }
            if (s.sual.exp.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = s.sual.exp,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = c.textDim
                )
            }
        }
    }
}
