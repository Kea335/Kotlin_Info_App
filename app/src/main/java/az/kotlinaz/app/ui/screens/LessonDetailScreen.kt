package az.kotlinaz.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.model.Section
import az.kotlinaz.app.ui.components.BlockView
import az.kotlinaz.app.ui.theme.KAz

/**
 * Bir bölmənin tam məzmunu.
 * Bloklar `LazyColumn` ilə göstərilir — 32 bölmənin ən uzunu belə
 * rəvan sürüşür, çünki yalnız görünən bloklar qurulur.
 */
@Composable
fun LessonDetailScreen(
    section: Section,
    evvelki: Section?,
    novbeti: Section?,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    onOxundu: (String) -> Unit,
    onQuiz: () -> Unit,
    onPlayground: () -> Unit,
    onExercises: () -> Unit
) {
    val c = KAz.colors
    val listState = rememberLazyListState()

    // Sona yaxınlaşanda bölmə oxunmuş sayılır
    val sonaCatdi by remember(section.id) {
        derivedStateOf {
            val son = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val umumi = listState.layoutInfo.totalItemsCount
            umumi > 0 && son >= umumi - 2
        }
    }

    LaunchedEffect(section.id, sonaCatdi) {
        if (sonaCatdi) onOxundu(section.id)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item(key = "basliq") {
            Column(Modifier.padding(bottom = 4.dp)) {
                if (section.kicker.isNotBlank()) {
                    Text(
                        text = section.kicker.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.accent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = section.heading.ifBlank { section.title },
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.text
                )
            }
        }

        itemsIndexed(
            items = section.blocks,
            key = { index, _ -> "b$index" }
        ) { _, block ->
            BlockView(
                block = block,
                onQuiz = onQuiz,
                onPlayground = onPlayground
            )
        }

        // Çalışmalar bölməsi üçün birbaşa keçid
        if (section.special == "exercises") {
            item(key = "calisma-kecid") {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.accentSoft)
                        .clickable(onClick = onExercises)
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Çalışmalara keç — 25 mövzu, 1250 tapşırıq",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.accent,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = c.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        item(key = "sehife-nav") {
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                NavDuymesi(
                    yon = "Əvvəlki",
                    basliq = evvelki?.title,
                    geri = true,
                    modifier = Modifier.weight(1f)
                ) { evvelki?.let { onNavigate(it.id) } }

                NavDuymesi(
                    yon = "Növbəti",
                    basliq = novbeti?.title,
                    geri = false,
                    modifier = Modifier.weight(1f)
                ) { novbeti?.let { onNavigate(it.id) } }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun NavDuymesi(
    yon: String,
    basliq: String?,
    geri: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = KAz.colors
    val aktiv = basliq != null

    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .then(if (aktiv) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (geri) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = if (aktiv) c.textFaint else c.border,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = yon,
                style = MaterialTheme.typography.labelSmall,
                color = if (aktiv) c.textFaint else c.border,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (!geri) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = if (aktiv) c.textFaint else c.border,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = basliq ?: "—",
            style = MaterialTheme.typography.labelLarge,
            color = if (aktiv) c.text else c.textFaint,
            maxLines = 2
        )
    }
}
