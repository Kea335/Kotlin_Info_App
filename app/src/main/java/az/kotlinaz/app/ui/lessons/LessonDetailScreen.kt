package az.kotlinaz.app.ui.lessons

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import az.kotlinaz.app.data.oyrenme.BookmarkTipi
import az.kotlinaz.app.ui.isareler.IsareViewModel
import az.kotlinaz.app.ui.isareler.IsareDuymesi
import az.kotlinaz.app.ui.isareler.QeydDuymesi
import az.kotlinaz.app.ui.isareler.QeydKarti
import az.kotlinaz.app.ui.isareler.QeydDialoqu
import az.kotlinaz.app.ui.isareler.KodIsaresi
import az.kotlinaz.app.ui.isareler.LocalKodIsaresi
import az.kotlinaz.app.ui.isareler.ustBlokIndeksi
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import az.kotlinaz.app.R
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.kotlinaz.app.data.BolmeTereqqisi
import az.kotlinaz.app.data.model.IdliBlok
import az.kotlinaz.app.data.model.Section
import az.kotlinaz.app.ui.AppViewModel
import az.kotlinaz.app.ui.components.BlockView
import az.kotlinaz.app.ui.components.BosEkran
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush

/**
 * Bir bölmənin tam məzmunu.
 * Bloklar `LazyColumn` ilə göstərilir — 32 bölmənin ən uzunu belə
 * rəvan sürüşür, çünki yalnız görünən bloklar qurulur.
 */
@Composable
fun LessonDetailScreen(
    vm: LessonsViewModel,
    appVm: AppViewModel,
    /** İşarə və qeydlər (T5.3). */
    isareVm: IsareViewModel,
    /** Ünvandan gələn bölmə id-si. */
    id: String,
    modifier: Modifier = Modifier,
    /** İşarələnmiş kod bloku — dərs ona sürüşür (T5.3). */
    blok: String? = null,
    onNavigate: (String) -> Unit,
    onQuiz: () -> Unit,
    onPlayground: () -> Unit,
    onExercises: () -> Unit,
    onMovzu: (String) -> Unit
) {
    val sections by vm.sections.collectAsStateWithLifecycle()
    val topics by appVm.topics.collectAsStateWithLifecycle()
    val tereqqi by appVm.tereqqi.collectAsStateWithLifecycle()

    val bolme = sections.firstOrNull { it.id == id }
    if (bolme == null) {
        BosEkran(stringResource(R.string.ders_bolme_yoxdur), modifier)
        return
    }

    // Dərs açılan kimi «son bölmə» yazılır — «Davam et» kartı yarımçıq
    // qoyulan yerə qaytarsın deyə.
    LaunchedEffect(bolme.id) { vm.sonBolmeniYaz(bolme.id) }

    val isareliler by isareVm.isareliIdler.collectAsStateWithLifecycle()
    val qeydler by isareVm.qeydler.collectAsStateWithLifecycle()
    var qeydAcik by rememberSaveable { mutableStateOf(false) }

    // Kod kartlarındakı işarə düyməsi blok renderinin dərinliyindədir — CompositionLocal ilə.
    CompositionLocalProvider(
        LocalKodIsaresi provides KodIsaresi(isareliler) { isareVm.isareDeyis(it, BookmarkTipi.KOD) }
    ) {
    LessonDetailContent(
        section = bolme,
        tereqqi = tereqqi.bolme(bolme.id),
        // Bölməyə bağlı çalışma mövzusu; JSON-da `sectionId` sahəsi ilə
        // göstərilir, olmayan bölmələrdə null qalır.
        movzuId = topics.firstOrNull { it.sectionId == bolme.id }?.id,
        // `index` content.json-da 0-dan başlayır və siyahıdakı mövqe ilə
        // eynidir, ona görə qonşular birbaşa götürülür. Kənarlarda getOrNull
        // null qaytarır — düymə sönük qalır.
        evvelki = sections.getOrNull(bolme.index - 1),
        novbeti = sections.getOrNull(bolme.index + 1),
        modifier = modifier,
        onNavigate = onNavigate,
        onOxundu = { vm.bolmeniOxunmusIsaretle(it) },
        onQuiz = onQuiz,
        onPlayground = onPlayground,
        onExercises = onExercises,
        onMovzu = onMovzu,
        blok = blok,
        isareli = bolme.id in isareliler,
        qeyd = qeydler[bolme.id],
        onIsare = { isareVm.isareDeyis(bolme.id, BookmarkTipi.BOLME) },
        onQeyd = { qeydAcik = true }
    )
    }

    if (qeydAcik) {
        QeydDialoqu(
            ilkin = qeydler[bolme.id].orEmpty(),
            onSaxla = { isareVm.qeydYaz(bolme.id, it) },
            onSil = { isareVm.qeydYaz(bolme.id, "") },
            onBagla = { qeydAcik = false }
        )
    }
}

/** Saf render — bütün vəziyyət parametrlə gəlir. */
@Composable
private fun LessonDetailContent(
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
    onMovzu: (String) -> Unit,
    blok: String?,
    isareli: Boolean,
    qeyd: String?,
    onIsare: () -> Unit,
    onQeyd: () -> Unit
) {
    val c = KAz.colors
    val listState = rememberLazyListState()

    // İşarədən gələndə kod blokuna sürüşülür — bir dəfə (döndərmədə təkrarlanmır).
    var surusdu by rememberSaveable(section.id, blok) { mutableStateOf(false) }
    LaunchedEffect(section.id, blok) {
        if (blok != null && !surusdu) {
            val i = ustBlokIndeksi(section.blocks, blok)
            // 0-cı element başlıqdır.
            if (i >= 0) listState.scrollToItem(i + 1)
            surusdu = true
        }
    }

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
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = section.heading.ifBlank { section.title },
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.text,
                        modifier = Modifier.weight(1f)
                    )
                    QeydDuymesi(qeydVar = qeyd != null, onClick = onQeyd)
                    IsareDuymesi(isareli = isareli, onClick = onIsare)
                }
                if (qeyd != null) {
                    Spacer(Modifier.height(8.dp))
                    QeydKarti(qeyd, onRedakte = onQeyd)
                }
            }
        }

        // Hər blok ayrıca element kimi verilir — LazyColumn yalnız ekranda
        // görünənləri qurur. Ən uzun bölmədə 50+ blok var; hamısını birdən
        // qurmaq açılışı gözəçarpan dərəcədə ləngidərdi.
        // Açar blokun sabit id-sidir (T3.1); id-siz bloklar (hr, quiz,
        // playground, demo) və köhnə aktivlər üçün mövqe işlədilir.
        itemsIndexed(
            items = section.blocks,
            key = { index, blok -> (blok as? IdliBlok)?.id?.ifBlank { null } ?: "b$index" }
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
                        text = stringResource(R.string.ders_calismalara_kec),
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
                    yon = stringResource(R.string.ortaq_evvelki),
                    basliq = evvelki?.title,
                    geri = true,
                    modifier = Modifier.weight(1f)
                ) { evvelki?.let { onNavigate(it.id) } }

                NavDuymesi(
                    yon = stringResource(R.string.ortaq_novbeti),
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
                    text = stringResource(if (bitib) R.string.ders_tamamlanib_basliq else R.string.ders_menimse_basliq),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (bitib) c.ok else c.text
                )
                Text(
                    text = when {
                        tereqqi.calismasiz && bitib -> stringResource(R.string.ders_oz_tesdiq)
                        tereqqi.calismasiz -> stringResource(R.string.ders_calismasiz)
                        else -> pluralStringResource(R.plurals.ders_calisma_hell, tereqqi.umumi, tereqqi.hell, tereqqi.umumi)
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
                    text = stringResource(R.string.ortaq_faiz, (tereqqi.faiz * 100).toInt()),
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
            text = stringResource(
                if (tereqqi.calismasiz) R.string.ders_calismasiz_izah else R.string.ders_calismali_izah
            ),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
            color = c.textDim
        )

        Spacer(Modifier.height(11.dp))

        if (tereqqi.calismasiz) {
            if (!bitib) {
                Text(
                    text = stringResource(R.string.ders_oxudum),
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
                    text = stringResource(
                        if (bitib) R.string.ders_calismalara_qayit else R.string.ders_calismalarina_kec
                    ),
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
