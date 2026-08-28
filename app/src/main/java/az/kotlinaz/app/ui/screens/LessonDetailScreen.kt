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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.BolmeTereqqisi
import az.kotlinaz.app.data.model.Section
import az.kotlinaz.app.ui.components.BlockView
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush

/**
 * Bir bölmənin tam məzmunu.
 * Bloklar `LazyColumn` ilə göstərilir — 32 bölmənin ən uzunu belə
 * rəvan sürüşür, çünki yalnız görünən bloklar qurulur.
 */
@Composable
fun LessonDetailScreen(
    section: Section,
    /** Bu bölmənin mənimsəmə vəziyyəti — çalışmalardan hesablanır. */
    tereqqi: BolmeTereqqisi,
    /** Bölməyə bağlı çalışma mövzusunun id-si; yoxdursa null. */
    movzuId: String?,
    evvelki: Section?,
    novbeti: Section?,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
    onOxundu: (String) -> Unit,
    onQuiz: () -> Unit,
    onPlayground: () -> Unit,
    onExercises: () -> Unit,
    onMovzu: (String) -> Unit
) {
    val c = KAz.colors
    val listState = rememberLazyListState()

    // DÜZƏLİŞ (davranış): əvvəllər siyahının sonu görünən kimi bölmə avtomatik
    // «oxunmuş» işarələnirdi. Mətni yuxarıdan aşağı sürətlə keçmək də bunu
    // işə salırdı — yəni heç nə oxumadan tərəqqi artırdı. Artıq belə deyil:
    // bölmə YALNIZ ona bağlı çalışmaların hamısı düzgün həll ediləndə
    // tamamlanmış sayılır (aşağıdakı «Mənimsəmə» kartına bax). Çalışması
    // olmayan bölmələrdə isə istifadəçinin öz təsdiqi tələb olunur.

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

        // Hər blok ayrıca element kimi verilir — LazyColumn yalnız ekranda
        // görünənləri qurur. Ən uzun bölmədə 50+ blok var; hamısını birdən
        // qurmaq açılışı gözəçarpan dərəcədə ləngidərdi.
        // Açar indeksdən qurulur, çünki blokların öz id-si yoxdur.
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

        // Yalnız «Çalışmalar» bölməsində görünən birbaşa keçid.
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

        item(key = "menimseme") {
            Spacer(Modifier.height(18.dp))
            MenimsemeKarti(
                tereqqi = tereqqi,
                movzuId = movzuId,
                onMovzu = onMovzu,
                onOxundu = { onOxundu(section.id) }
            )
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

/**
 * Dərsin mənimsəmə kartı.
 *
 * Bölmənin «oxunmuş» sayılması üçün TƏK yol budur: ona bağlı bütün nəzəri və
 * praktiki çalışmaların düzgün həll edilməsi. Kart nə qədər qaldığını göstərir
 * və birbaşa həmin mövzunun çalışmalarına aparır.
 */
@Composable
private fun MenimsemeKarti(
    tereqqi: BolmeTereqqisi,
    movzuId: String?,
    onMovzu: (String) -> Unit,
    onOxundu: () -> Unit
) {
    val c = KAz.colors
    val bitib = tereqqi.tamamlandi
    val cerceve = if (bitib) c.ok.copy(alpha = 0.45f) else c.border

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.bgElev)
            .border(1.dp, cerceve, RoundedCornerShape(14.dp))
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (bitib) "Bu dərs tamamlanıb" else "Bu dərsi mənimsəmək",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (bitib) c.ok else c.text
                )
                Text(
                    text = when {
                        tereqqi.calismasiz && bitib -> "Öz təsdiqinlə işarələnib"
                        tereqqi.calismasiz -> "Bu bölmənin çalışması yoxdur"
                        else -> "${tereqqi.hell} / ${tereqqi.umumi} çalışma həll edilib"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
            }
            if (bitib) {
                Box(
                    Modifier.size(24.dp).background(c.ok.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = c.ok,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else if (!tereqqi.calismasiz) {
                Text(
                    text = "${(tereqqi.faiz * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.accent,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (!tereqqi.calismasiz) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(c.bgSunken)
            ) {
                if (tereqqi.faiz > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth(tereqqi.faiz)
                            .height(6.dp)
                            .clip(CircleShape)
                            // Hər iki qol Brush olmalıdır — Modifier.background
                            // Color və Brush üçün ayrı imzalara malikdir.
                            .background(
                                if (bitib) Brush.linearGradient(listOf(c.ok, c.ok))
                                else KotlinBrush
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (tereqqi.calismasiz) {
                "Bu bölmə üçün çalışma bankında mövzu yoxdur. Oxuyub bitirdiyini " +
                    "özün təsdiqləyə bilərsən."
            } else {
                "Mətni sürüşdürmək kifayət etmir — dərs yalnız bütün çalışmalar " +
                    "düzgün həll ediləndə tamamlanmış sayılır."
            },
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
            color = c.textDim
        )

        Spacer(Modifier.height(11.dp))

        if (tereqqi.calismasiz) {
            if (!bitib) {
                Text(
                    text = "Oxudum, bitirdim",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.accentSoft)
                        .clickable(onClick = onOxundu)
                        .padding(vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.accent,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else if (movzuId != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (bitib) c.bgSunken else c.accentSoft)
                    .clickable { onMovzu(movzuId) }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (bitib) "Çalışmalara qayıt" else "Bu dərsin çalışmalarına keç",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (bitib) c.textDim else c.accent,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = if (bitib) c.textDim else c.accent,
                    modifier = Modifier.size(17.dp)
                )
            }
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
    // Kənar bölmələrdə qonşu yoxdur — düymə sönük və toxunulmaz olur.
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
