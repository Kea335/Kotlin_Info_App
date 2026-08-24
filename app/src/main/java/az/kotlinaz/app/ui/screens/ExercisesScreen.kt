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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush

/**
 * Çalışma mövzularının siyahısı.
 * Hər mövzuda 25 nəzəri + 25 praktiki çalışma var — hamısı oflayn.
 */
@Composable
fun ExercisesScreen(
    topics: List<ExerciseTopic>,
    hellEdilmis: Set<String>,
    modifier: Modifier = Modifier,
    onTopic: (String) -> Unit
) {
    val c = KAz.colors

    val umumiCalisma = topics.sumOf { it.nezeri.size + it.praktiki.size }
    val hellSayi = topics.sumOf { t ->
        (t.nezeri.count { hellEdilmis.contains(it.id) }) +
            (t.praktiki.count { hellEdilmis.contains(it.id) })
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            UmumiTereqqi(hellSayi, umumiCalisma)
            Spacer(Modifier.height(6.dp))
        }

        items(topics, key = { it.id }) { movzu ->
            MovzuKarti(
                movzu = movzu,
                hellEdilmis = hellEdilmis,
                onClick = { onTopic(movzu.id) }
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun UmumiTereqqi(hell: Int, umumi: Int) {
    val c = KAz.colors
    val faiz = if (umumi == 0) 0f else hell.toFloat() / umumi

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
                    text = "Çalışmalar bankı",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.text
                )
                Text(
                    text = "$hell / $umumi həll edilib",
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
    }
}

@Composable
private fun MovzuKarti(
    movzu: ExerciseTopic,
    hellEdilmis: Set<String>,
    onClick: () -> Unit
) {
    val c = KAz.colors
    val umumi = movzu.nezeri.size + movzu.praktiki.size
    val hell = movzu.nezeri.count { hellEdilmis.contains(it.id) } +
        movzu.praktiki.count { hellEdilmis.contains(it.id) }
    val faiz = if (umumi == 0) 0f else hell.toFloat() / umumi
    val bitib = hell == umumi && umumi > 0

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.bgElev)
            .border(
                1.dp,
                if (bitib) c.ok.copy(alpha = 0.45f) else c.border,
                RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = movzu.title,
                style = MaterialTheme.typography.titleSmall,
                color = c.text,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$hell/$umumi",
                style = MaterialTheme.typography.labelSmall,
                color = if (bitib) c.ok else c.textFaint,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(9.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(c.bgSunken)
        ) {
            if (faiz > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(faiz)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (bitib) c.ok else c.accent)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Etiket("${movzu.nezeri.size} nəzəri", c.accent)
            Etiket("${movzu.praktiki.size} praktiki", c.warn)
        }
    }
}

@Composable
private fun Etiket(metn: String, reng: androidx.compose.ui.graphics.Color) {
    Text(
        text = metn,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(reng.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = reng,
        fontSize = 10.5.sp
    )
}
