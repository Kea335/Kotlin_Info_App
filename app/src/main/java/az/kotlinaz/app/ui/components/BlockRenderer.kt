package az.kotlinaz.app.ui.components

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.model.Block
import az.kotlinaz.app.data.model.CardGrid
import az.kotlinaz.app.data.model.Callout
import az.kotlinaz.app.data.model.CodeBlock
import az.kotlinaz.app.data.model.DemoBlock
import az.kotlinaz.app.data.model.HeadingH2
import az.kotlinaz.app.data.model.HeadingH3
import az.kotlinaz.app.data.model.HeadingH4
import az.kotlinaz.app.data.model.HeadingH5
import az.kotlinaz.app.data.model.Hero
import az.kotlinaz.app.data.model.ListBlock
import az.kotlinaz.app.data.model.Paragraph
import az.kotlinaz.app.data.model.PlaygroundBlock
import az.kotlinaz.app.data.model.QuizBlock
import az.kotlinaz.app.data.model.Span
import az.kotlinaz.app.data.model.TableBlock
import az.kotlinaz.app.data.model.TabsBlock
import az.kotlinaz.app.data.model.Timeline
import az.kotlinaz.app.ui.demos.DemoHost
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush

/**
 * Sayt məzmununun bir blokunu nativ Compose ilə göstərir.
 * Hər blok tipi saytdakı CSS sinfinə uyğun gəlir.
 */
@Composable
fun BlockView(
    block: Block,
    modifier: Modifier = Modifier,
    onQuiz: (() -> Unit)? = null,
    onPlayground: (() -> Unit)? = null
) {
    val c = KAz.colors
    val bolmeyeKec = LocalBolmeyeKec.current
    val meydanaGonder = LocalMeydanaGonder.current

    when (block) {
        is HeadingH2 -> Text(
            text = block.text,
            modifier = modifier.padding(top = 22.dp, bottom = 8.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = c.text
        )

        is HeadingH3 -> Text(
            text = block.text,
            modifier = modifier.padding(top = 20.dp, bottom = 6.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = c.text
        )

        is HeadingH4 -> Text(
            text = block.text,
            modifier = modifier.padding(top = 14.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            color = c.text
        )

        is HeadingH5 -> Text(
            text = block.text,
            modifier = modifier.padding(top = 12.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            color = c.textDim
        )

        is Paragraph -> SpansText(
            spans = block.spans,
            modifier = modifier.padding(vertical = 6.dp),
            style = if (block.lead) {
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 28.sp,
                    color = c.textDim
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(color = c.text)
            },
            onLink = bolmeyeKec
        )

        is CodeBlock -> CodeCard(
            code = block.code,
            modifier = modifier.padding(vertical = 10.dp),
            title = block.title,
            output = block.output,
            onOpenInPlayground = { meydanaGonder(it) }
        )

        is Callout -> CalloutView(block, modifier)

        is CardGrid -> CardGridView(block, modifier)

        is TableBlock -> TableView(block, modifier)

        is ListBlock -> ListView(block, modifier)

        is Timeline -> TimelineView(block, modifier)

        is TabsBlock -> TabsView(block, modifier)

        is Hero -> HeroView(block, modifier)

        is DemoBlock -> DemoHost(block.demo, modifier.padding(vertical = 10.dp))

        QuizBlock -> AksiyaKarti(
            ikon = "🎓",
            basliq = "Bilik testi",
            metn = "15 sual — sintaksisdən coroutine-lərə qədər. Suallar hər dəfə qarışıq sırada gəlir.",
            duyme = "Testi başlat",
            modifier = modifier
        ) { onQuiz?.invoke() }

        PlaygroundBlock -> AksiyaKarti(
            ikon = "▶",
            basliq = "Kod meydanı",
            metn = "Real Kotlin kompilyatoru. Bu bölmə internet tələb edir — tətbiqin qalan hissəsi oflayn işləyir.",
            duyme = "Meydanı aç",
            modifier = modifier
        ) { onPlayground?.invoke() }

        az.kotlinaz.app.data.model.Divider -> HorizontalDivider(
            modifier = modifier.padding(vertical = 18.dp),
            color = c.border
        )
    }
}

/** Bir neçə bloku ardıcıl göstərir (kart, callout və tab daxilində). */
@Composable
fun Blocks(
    blocks: List<Block>,
    modifier: Modifier = Modifier,
    onQuiz: (() -> Unit)? = null,
    onPlayground: (() -> Unit)? = null
) {
    Column(modifier) {
        blocks.forEach { BlockView(it, onQuiz = onQuiz, onPlayground = onPlayground) }
    }
}

/* ============================================================
   Callout — .callout tip|warn|info|danger
   ============================================================ */

@Composable
private fun CalloutView(block: Callout, modifier: Modifier = Modifier) {
    val c = KAz.colors
    val vurgu = when (block.kind) {
        "tip" -> c.ok
        "warn" -> c.warn
        "danger" -> c.err
        else -> c.accent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(vurgu.copy(alpha = 0.07f))
            .border(1.dp, vurgu.copy(alpha = 0.28f), RoundedCornerShape(13.dp))
            .padding(14.dp)
    ) {
        if (block.ico.isNotBlank()) {
            Text(text = block.ico, fontSize = 19.sp)
            Spacer(Modifier.width(11.dp))
        }
        Column(Modifier.weight(1f)) {
            if (block.title.isNotBlank()) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = vurgu,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
            }
            Blocks(block.blocks)
        }
    }
}

/* ============================================================
   Kart şəbəkəsi — .grid > .card
   Mobil ekranda kartlar alt-alta düzülür.
   ============================================================ */

@Composable
private fun CardGridView(block: CardGrid, modifier: Modifier = Modifier) {
    val c = KAz.colors
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        block.items.forEach { kart ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(c.bgElev)
                    .border(1.dp, c.border, RoundedCornerShape(13.dp))
                    .padding(14.dp)
            ) {
                if (kart.ico.isNotBlank()) {
                    Text(text = kart.ico, fontSize = 22.sp)
                    Spacer(Modifier.height(6.dp))
                }
                if (kart.title.isNotBlank()) {
                    Text(
                        text = kart.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = c.text
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Blocks(kart.blocks)
            }
        }
    }
}

/* ============================================================
   Cədvəl — üfüqi sürüşən, sütunları düzülmüş
   ============================================================ */

@Composable
private fun TableView(block: TableBlock, modifier: Modifier = Modifier) {
    val c = KAz.colors
    val sutunSayi = maxOf(
        block.head.firstOrNull()?.size ?: 0,
        block.rows.firstOrNull()?.size ?: 0
    ).coerceAtLeast(1)

    val sutunEni = when (sutunSayi) {
        1 -> 320.dp
        2 -> 180.dp
        3 -> 158.dp
        else -> 142.dp
    }

    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            block.head.forEach { setir ->
                Row(Modifier.background(c.bgSunken)) {
                    setir.forEachIndexed { i, hucre ->
                        TableCell(
                            spans = hucre,
                            width = if (i == 0 && sutunSayi > 1) sutunEni + 24.dp else sutunEni,
                            bold = true
                        )
                    }
                }
                HorizontalDivider(color = c.borderStrong)
            }

            block.rows.forEachIndexed { index, setir ->
                Row(
                    Modifier.background(
                        if (index % 2 == 1) c.bgSunken.copy(alpha = 0.45f) else Color.Transparent
                    )
                ) {
                    setir.forEachIndexed { i, hucre ->
                        TableCell(
                            spans = hucre,
                            width = if (i == 0 && sutunSayi > 1) sutunEni + 24.dp else sutunEni
                        )
                    }
                }
                if (index != block.rows.lastIndex) HorizontalDivider(color = c.border)
            }
        }
    }
}

@Composable
private fun TableCell(
    spans: List<Span>,
    width: androidx.compose.ui.unit.Dp,
    bold: Boolean = false
) {
    val c = KAz.colors
    SpansText(
        spans = spans,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        style = MaterialTheme.typography.bodySmall.copy(
            color = if (bold) c.text else c.textDim,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            lineHeight = 18.sp
        )
    )
}

/* ============================================================
   Siyahı — <ul> / <ol>
   ============================================================ */

@Composable
private fun ListView(block: ListBlock, modifier: Modifier = Modifier) {
    val c = KAz.colors
    Column(
        modifier = modifier.padding(vertical = 6.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        block.items.forEachIndexed { index, item ->
            Row {
                Text(
                    text = if (block.ordered) "${index + 1}." else "•",
                    modifier = Modifier.width(if (block.ordered) 24.dp else 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.accent,
                    fontWeight = FontWeight.Bold
                )
                SpansText(
                    spans = item,
                    style = MaterialTheme.typography.bodyMedium.copy(color = c.text),
                    onLink = LocalBolmeyeKec.current
                )
            }
        }
    }
}

/* ============================================================
   Xronologiya — .timeline
   ============================================================ */

@Composable
private fun TimelineView(block: Timeline, modifier: Modifier = Modifier) {
    val c = KAz.colors
    Column(modifier.padding(vertical = 8.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth()) {
                // Xətt və nöqtə
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(22.dp)
                ) {
                    Box(
                        Modifier
                            .size(11.dp)
                            .background(KotlinBrush, CircleShape)
                    )
                    if (index != block.items.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(if (item.desc.isEmpty()) 34.dp else 92.dp)
                                .background(c.border)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f).padding(bottom = 16.dp)) {
                    Text(
                        text = item.year,
                        style = MaterialTheme.typography.labelMedium,
                        color = c.accent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = c.text
                    )
                    if (item.desc.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        SpansText(
                            spans = item.desc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = c.textDim,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/* ============================================================
   Tablar — .tabs
   ============================================================ */

@Composable
private fun TabsView(block: TabsBlock, modifier: Modifier = Modifier) {
    val c = KAz.colors
    var secili by remember(block) { mutableIntStateOf(0) }

    Column(modifier.padding(vertical = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            block.tabs.forEachIndexed { index, tab ->
                val aktiv = index == secili
                Text(
                    text = tab.label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (aktiv) c.accentSoft else c.bgSunken)
                        .border(
                            1.dp,
                            if (aktiv) c.accent.copy(alpha = 0.45f) else c.border,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable { secili = index }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (aktiv) c.accent else c.textDim,
                    fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        block.tabs.getOrNull(secili)?.let { Blocks(it.blocks) }
    }
}

/* ============================================================
   Hero — yalnız giriş bölməsində
   ============================================================ */

@Composable
private fun HeroView(block: Hero, modifier: Modifier = Modifier) {
    val c = KAz.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(c.accentSoft)
            .padding(18.dp)
    ) {
        if (block.badge.isNotBlank()) {
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(c.bgElev)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).background(KotlinBrush, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = block.badge.removePrefix("• ").trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textDim
                )
            }
            Spacer(Modifier.height(13.dp))
        }

        SpansText(
            spans = block.title,
            style = MaterialTheme.typography.displaySmall.copy(color = c.text)
        )

        if (block.sub.isNotBlank()) {
            Spacer(Modifier.height(9.dp))
            Text(
                text = block.sub,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                color = c.textDim
            )
        }

        if (block.stats.isNotEmpty()) {
            Spacer(Modifier.height(15.dp))
            block.stats.chunked(2).forEach { cut ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    cut.forEach { stat ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(c.bgElev)
                                .padding(11.dp)
                        ) {
                            Text(
                                text = stat.num,
                                style = MaterialTheme.typography.titleMedium,
                                color = c.accent,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = stat.lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.textFaint
                            )
                        }
                    }
                    if (cut.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/* ============================================================
   Quiz / kod meydanı üçün keçid kartı
   ============================================================ */

@Composable
private fun AksiyaKarti(
    ikon: String,
    basliq: String,
    metn: String,
    duyme: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = KAz.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = ikon, fontSize = 20.sp)
            Spacer(Modifier.width(9.dp))
            Text(
                text = basliq,
                style = MaterialTheme.typography.titleMedium,
                color = c.text
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = metn,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color = c.textDim
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = duyme,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(KotlinBrush)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}
