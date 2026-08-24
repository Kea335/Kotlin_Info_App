package az.kotlinaz.app.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.model.Section
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush

/**
 * Bölmələrin siyahısı — saytdakı yan paneldəki naviqasiyanın qarşılığı.
 * Qruplara bölünür, oxunmuş bölmələr işarələnir.
 */
@Composable
fun LessonsScreen(
    sections: List<Section>,
    oxunanlar: Set<String>,
    sonBolme: String?,
    modifier: Modifier = Modifier,
    onSection: (String) -> Unit
) {
    val c = KAz.colors
    val qruplar = remember(sections) { sections.groupBy { it.group } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            TereqqiKarti(
                oxunan = sections.count { oxunanlar.contains(it.id) },
                umumi = sections.size,
                sonBolme = sonBolme?.let { id -> sections.firstOrNull { it.id == id } },
                onDavam = onSection
            )
            Spacer(Modifier.height(10.dp))
        }

        qruplar.forEach { (qrup, siyahi) ->
            item(key = "qrup-$qrup") {
                Text(
                    text = qrup.uppercase(),
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            items(siyahi, key = { it.id }) { bolme ->
                BolmeSetri(
                    bolme = bolme,
                    oxunub = oxunanlar.contains(bolme.id),
                    onClick = { onSection(bolme.id) }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun BolmeSetri(
    bolme: Section,
    oxunub: Boolean,
    onClick: () -> Unit
) {
    val c = KAz.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = bolme.index.toString().padStart(2, '0'),
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (oxunub) c.accent else c.textFaint,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(9.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = bolme.title,
                style = MaterialTheme.typography.titleSmall,
                color = c.text
            )
            if (bolme.special != null) {
                Text(
                    text = when (bolme.special) {
                        "playground" -> "İnternet tələb edir"
                        "exercises" -> "1250 çalışma"
                        "quiz" -> "15 sual"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
            }
        }

        if (oxunub) {
            Box(
                Modifier.size(20.dp).background(c.ok.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Oxunub",
                    tint = c.ok,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun TereqqiKarti(
    oxunan: Int,
    umumi: Int,
    sonBolme: Section?,
    onDavam: (String) -> Unit
) {
    val c = KAz.colors
    val faiz = if (umumi == 0) 0f else oxunan.toFloat() / umumi

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Oxuma tərəqqisi",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.text
                )
                Text(
                    text = "$oxunan / $umumi bölmə",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
            }
            Text(
                text = "${(faiz * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                color = c.accent,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.height(11.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(c.bgSunken)
        ) {
            if (faiz > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(faiz)
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(KotlinBrush)
                )
            }
        }

        if (sonBolme != null) {
            Spacer(Modifier.height(13.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.accentSoft)
                    .clickable { onDavam(sonBolme.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    tint = c.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Davam et",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint
                    )
                    Text(
                        text = sonBolme.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = c.accent
                    )
                }
            }
        }
    }
}
