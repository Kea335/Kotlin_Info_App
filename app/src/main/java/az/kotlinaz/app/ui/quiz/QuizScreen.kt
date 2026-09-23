package az.kotlinaz.app.ui.quiz

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import az.kotlinaz.app.R
import az.kotlinaz.app.ui.etiket
import az.kotlinaz.app.ui.izah
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.kotlinaz.app.data.model.QuizQuestion
import az.kotlinaz.app.ui.components.YuklenmeGostericisi
import az.kotlinaz.app.ui.demos.DemoCode
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush
import az.kotlinaz.app.ui.theme.seviyyeRengi

/**
 * Bilik testi — səviyyə üzrə bölünmüş, hər dəfə təsadüfi sıra ilə.
 * Tam oflayn işləyir.
 *
 * Turun vəziyyəti [QuizViewModel]-dədir (proses ölümündən sağ çıxır); bu
 * Composable yalnız render edir və hadisə göndərir. Rejim və hovuz
 * qurulması da oradadır — `ui/quiz/QuizHovuzu.kt`.
 */
@Composable
fun QuizScreen(
    vm: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val hovuzlar by vm.hovuzlar.collectAsStateWithLifecycle()
    val rejim by vm.rejim.collectAsStateWithLifecycle()
    val rekordlar by vm.rekordlar.collectAsStateWithLifecycle()

    // Hovuzlar məzmundan qurulur; boş xəritə = hələ oxunmayıb (T2.6).
    if (hovuzlar.isEmpty()) {
        YuklenmeGostericisi(modifier)
        return
    }

    val secilen = rejim
    if (secilen == null) {
        RejimSecimi(
            hovuzlar = hovuzlar,
            rekordlar = rekordlar,
            modifier = modifier,
            onSec = { vm.rejimSec(it) }
        )
    } else {
        QuizTuru(
            vm = vm,
            rejim = secilen,
            rekord = rekordlar[secilen.id] ?: 0,
            modifier = modifier
        )
    }
}

/* ============================================================
   Rejim seçimi
   ============================================================ */

@Composable
private fun RejimSecimi(
    hovuzlar: Map<QuizRejimi, List<QuizQuestion>>,
    rekordlar: Map<String, Int>,
    modifier: Modifier = Modifier,
    onSec: (QuizRejimi) -> Unit
) {
    val c = KAz.colors

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item(key = "basliq") {
            Column {
                Text(
                    text = stringResource(R.string.quiz_seviyye_sec),
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pluralStringResource(R.plurals.quiz_izah, QUIZ_SUAL_SAYI, QUIZ_SUAL_SAYI),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = c.textDim
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        QuizRejimi.entries.forEach { r ->
            item(key = r.id) {
                RejimKarti(
                    rejim = r,
                    hovuzOlcusu = hovuzlar[r]?.size ?: 0,
                    rekord = rekordlar[r.id] ?: 0,
                    onClick = { onSec(r) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun rejimRengi(rejim: QuizRejimi): Color =
    rejim.seviyye?.let { seviyyeRengi(it) } ?: KAz.colors.accent

@Composable
private fun RejimKarti(
    rejim: QuizRejimi,
    hovuzOlcusu: Int,
    rekord: Int,
    onClick: () -> Unit
) {
    val c = KAz.colors
    val reng = rejimRengi(rejim)
    val bos = hovuzOlcusu == 0

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.bgElev)
            .border(1.dp, reng.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .then(if (bos) Modifier else Modifier.clickable(onClick = onClick))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(reng))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = rejim.etiket(),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.text
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = rejim.izah(),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = c.textDim
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Nisan(pluralStringResource(R.plurals.quiz_hovuz_sayi, hovuzOlcusu, hovuzOlcusu), c.textFaint)
                if (rekord > 0) {
                    Nisan(stringResource(R.string.quiz_rekord_nisan, rekord, QUIZ_SUAL_SAYI), reng)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = if (bos) c.border else reng,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun Nisan(metn: String, reng: Color) {
    Text(
        text = metn,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(reng.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = reng
    )
}

/* ============================================================
   Turun özü
   ============================================================ */

@Composable
private fun QuizTuru(
    vm: QuizViewModel,
    rejim: QuizRejimi,
    rekord: Int,
    modifier: Modifier = Modifier
) {
    val c = KAz.colors

    val suallar by vm.suallar.collectAsStateWithLifecycle()
    val index by vm.index.collectAsStateWithLifecycle()
    val bal by vm.bal.collectAsStateWithLifecycle()
    val secim by vm.secim.collectAsStateWithLifecycle()
    val bitdi by vm.bitdi.collectAsStateWithLifecycle()

    val umumi = suallar.size

    // Sistemin «geri» jesti turdan çıxıb rejim seçiminə qaytarır — əks halda
    // istifadəçi Test bölməsindən tamam çıxmalı olurdu.
    BackHandler { vm.rejimiDeyis() }

    if (umumi == 0) return

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (bitdi) {
            item(key = "netice") {
                NeticeEkrani(
                    rejim = rejim,
                    bal = bal,
                    umumi = umumi,
                    rekord = rekord,
                    onYeniden = { vm.yenidenBasla() },
                    onRejimiDeyis = { vm.rejimiDeyis() }
                )
            }
            return@LazyColumn
        }

        val sual = suallar[index.coerceIn(0, umumi - 1)]

        item(key = "meta") {
            Text(
                text = stringResource(R.string.quiz_seviyyeni_deyis_geri),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { vm.rejimiDeyis() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Nisan(rejim.etiket(), rejimRengi(rejim))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.quiz_sual_sayqac, index + 1, umumi),
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textDim,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.quiz_bal, bal),
                    style = MaterialTheme.typography.labelMedium,
                    color = c.accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(c.bgSunken)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((index + 1).toFloat() / umumi)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(KotlinBrush)
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        item(key = "sual-${sual.id}") {
            Text(
                text = sual.q,
                style = MaterialTheme.typography.titleMedium.copy(lineHeight = 25.sp),
                color = c.text
            )

            if (!sual.code.isNullOrBlank()) {
                Spacer(Modifier.height(11.dp))
                DemoCode(sual.code)
            }

            Spacer(Modifier.height(14.dp))

            sual.opts.forEachIndexed { i, variant ->
                val secilib = secim == i
                val duzgundur = i == sual.a
                val cavabVerilib = secim != null

                val cerceve = when {
                    !cavabVerilib -> c.border
                    duzgundur -> c.ok
                    secilib -> c.err
                    else -> c.border
                }
                val fon = when {
                    !cavabVerilib -> c.bgElev
                    duzgundur -> c.ok.copy(alpha = 0.10f)
                    secilib -> c.err.copy(alpha = 0.10f)
                    else -> c.bgElev
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(fon)
                        .border(1.dp, cerceve, RoundedCornerShape(11.dp))
                        // Cavab verildikdən sonra variantlar kilidlənir —
                        // ikinci toxunuş balı təkrar artıra bilməsin.
                        .clickable(enabled = !cavabVerilib) { vm.cavabla(i) }
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ('A' + i).toString(),
                        modifier = Modifier.width(22.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (cavabVerilib && duzgundur) c.ok else c.textFaint,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = variant,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.text,
                        modifier = Modifier.weight(1f)
                    )
                    if (cavabVerilib && duzgundur) {
                        Icon(Icons.Filled.Check, null, tint = c.ok, modifier = Modifier.size(17.dp))
                    } else if (cavabVerilib && secilib) {
                        Icon(Icons.Filled.Close, null, tint = c.err, modifier = Modifier.size(17.dp))
                    }
                }
            }

            if (secim != null) {
                Spacer(Modifier.height(5.dp))
                val duzgun = secim == sual.a
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background((if (duzgun) c.ok else c.warn).copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            (if (duzgun) c.ok else c.warn).copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(13.dp)
                ) {
                    Text(
                        text = if (duzgun) stringResource(R.string.cavab_duzdur)
                        else stringResource(R.string.cavab_duzgun, ('A' + sual.a).toString()),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (duzgun) c.ok else c.warn
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = sual.exp,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = c.textDim
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(
                        if (index == umumi - 1) R.string.quiz_neticeye_bax else R.string.quiz_novbeti_sual
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(KotlinBrush)
                        .clickable { vm.novbeti() }
                        .padding(vertical = 13.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun NeticeEkrani(
    rejim: QuizRejimi,
    bal: Int,
    umumi: Int,
    rekord: Int,
    onYeniden: () -> Unit,
    onRejimiDeyis: () -> Unit
) {
    val c = KAz.colors
    // Tam ədəd bölməsi — faiz aşağıya yuvarlanır (14/15 → 93%).
    val faiz = if (umumi == 0) 0 else (bal * 100 / umumi)

    val rey = stringResource(
        when {
            faiz >= 90 -> R.string.quiz_rey_mukemmel
            faiz >= 70 -> R.string.quiz_rey_yaxsi
            faiz >= 50 -> R.string.quiz_rey_orta
            else -> R.string.quiz_rey_baslangic
        }
    )

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Nisan(rejim.etiket(), rejimRengi(rejim))
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.ortaq_faiz, faiz),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = (46 * KAz.codeScale).sp),
            color = c.accent,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = pluralStringResource(R.plurals.quiz_dogru_cavab, umumi, bal, umumi),
            style = MaterialTheme.typography.titleMedium,
            color = c.text
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = rey,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color = c.textDim,
            textAlign = TextAlign.Center
        )

        if (rekord > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.quiz_rekordun, rejim.etiket(), rekord, umumi),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.bgSunken)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = c.textDim
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.quiz_yeniden),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(KotlinBrush)
                .clickable(onClick = onYeniden)
                .padding(vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.quiz_seviyyeni_deyis),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(c.bgSunken)
                .border(1.dp, c.border, RoundedCornerShape(11.dp))
                .clickable(onClick = onRejimiDeyis)
                .padding(vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = c.textDim,
            textAlign = TextAlign.Center
        )
    }
}
