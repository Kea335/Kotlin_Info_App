package az.kotlinaz.app.ui.screens

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.model.QuizQuestion
import az.kotlinaz.app.ui.demos.DemoCode
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush
import az.kotlinaz.app.ui.theme.seviyyeRengi

/** Bir turdakı sual sayı. Hovuz daha kiçikdirsə, olduğu qədər sual verilir. */
const val QUIZ_SUAL_SAYI = 15

/**
 * Testin rejimi.
 *
 * Səviyyə rejimləri sualları çalışma bankının nəzəri hissəsindən götürür —
 * orada hər sualın `level` sahəsi var. «Qarışıq» isə həm bankı, həm də
 * quiz.json-dakı ümumi sualları birləşdirir.
 */
enum class QuizRejimi(val id: String, val label: String, val seviyye: Level?) {
    JUNIOR("junior", "Junior", Level.JUNIOR),
    MIDDLE("middle", "Middle", Level.MIDDLE),
    SENIOR("senior", "Senior", Level.SENIOR),
    QARISIQ("qarisiq", "Qarışıq", null);

    val izah: String
        get() = when (this) {
            JUNIOR -> "Sintaksis, dəyişənlər, şərtlər, dövrlər — başlanğıc suallar."
            MIDDLE -> "Kolleksiyalar, lambda, OYP, null təhlükəsizliyi."
            SENIOR -> "Generics, korutinlər, delegatlar, incə detallar."
            QARISIQ -> "Bütün səviyyələrdən təsadüfi seçim — əsl imtahan."
        }
}

/**
 * Bilik testi — səviyyə üzrə bölünmüş, hər dəfə təsadüfi sıra ilə.
 * Tam oflayn işləyir.
 */
@Composable
fun QuizScreen(
    questions: List<QuizQuestion>,
    topics: List<ExerciseTopic>,
    rekordlar: Map<String, Int>,
    modifier: Modifier = Modifier,
    onBitdi: (String, Int) -> Unit
) {
    // Rejim üzrə sual hovuzları — məzmun dəyişməyincə bir dəfə qurulur.
    val hovuzlar = remember(questions, topics) { hovuzlariQur(questions, topics) }

    // null = rejim seçimi ekranı.
    var rejim by remember { mutableStateOf<QuizRejimi?>(null) }

    val secilen = rejim
    if (secilen == null) {
        RejimSecimi(
            hovuzlar = hovuzlar,
            rekordlar = rekordlar,
            modifier = modifier,
            onSec = { rejim = it }
        )
    } else {
        QuizTuru(
            rejim = secilen,
            hovuz = hovuzlar[secilen].orEmpty(),
            rekord = rekordlar[secilen.id] ?: 0,
            modifier = modifier,
            onBitdi = { bal -> onBitdi(secilen.id, bal) },
            onRejimiDeyis = { rejim = null }
        )
    }
}

/* ============================================================
   Sual hovuzları
   ============================================================ */

private fun hovuzlariQur(
    questions: List<QuizQuestion>,
    topics: List<ExerciseTopic>
): Map<QuizRejimi, List<QuizQuestion>> {
    // Bankdakı nəzəri çalışmalar səviyyəyə görə qruplaşdırılır.
    val seviyyeUzre = mutableMapOf<Level, MutableList<QuizQuestion>>()
    topics.forEach { t ->
        t.nezeri.forEach { e ->
            // Nəzəri çalışma testin sual formatına birbaşa uyğun gəlir; yalnız
            // id-yə önlük əlavə olunur ki, quiz.json-dakılarla qarışmasın.
            seviyyeUzre.getOrPut(Level.from(e.level)) { mutableListOf() }.add(
                QuizQuestion(
                    id = "q-${e.id}",
                    q = e.q,
                    code = e.code,
                    opts = e.opts,
                    a = e.a,
                    exp = e.exp
                )
            )
        }
    }

    return QuizRejimi.entries.associateWith { r ->
        when (val lv = r.seviyye) {
            // Qarışıq: quiz.json-dakı ümumi suallar + bütün bank.
            null -> questions + seviyyeUzre.values.flatten()
            else -> seviyyeUzre[lv].orEmpty()
        }
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
                    text = "Səviyyəni seç",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Hər turda $QUIZ_SUAL_SAYI sual təsadüfi seçilir — " +
                        "eyni rejimi təkrar oynasan suallar dəyişir.",
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
                    text = rejim.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = c.text
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = rejim.izah,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = c.textDim
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Nisan("$hovuzOlcusu sual", c.textFaint)
                if (rekord > 0) {
                    Nisan("rekord $rekord/$QUIZ_SUAL_SAYI", reng)
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
    rejim: QuizRejimi,
    hovuz: List<QuizQuestion>,
    rekord: Int,
    modifier: Modifier = Modifier,
    onBitdi: (Int) -> Unit,
    onRejimiDeyis: () -> Unit
) {
    val c = KAz.colors

    // `tur` — neçənci dəfə oynanılır. «Yenidən başla» onu artırır və bununla
    // aşağıdakı bütün `remember`-lər sıfırlanır: suallar yenidən seçilir, bal,
    // indeks və seçim təzələnir.
    var tur by remember(rejim) { mutableIntStateOf(0) }

    // Hovuzdan təsadüfi 15 sual — hər turda yeni dəst.
    val suallar = remember(hovuz, tur) { hovuz.shuffled().take(QUIZ_SUAL_SAYI) }

    var index by remember(rejim, tur) { mutableIntStateOf(0) }
    var bal by remember(rejim, tur) { mutableIntStateOf(0) }
    // Seçim həm turdan, həm indeksdən asılıdır: növbəti suala keçəndə sıfırlanır.
    var secim by remember(rejim, tur, index) { mutableStateOf<Int?>(null) }
    var bitdi by remember(rejim, tur) { mutableStateOf(false) }

    val umumi = suallar.size

    // Nəticə yalnız bir dəfə yazılır — `bitdi` false→true keçidində.
    LaunchedEffect(bitdi) {
        if (bitdi) onBitdi(bal)
    }

    // Sistemin «geri» jesti turdan çıxıb rejim seçiminə qaytarır — əks halda
    // istifadəçi Test bölməsindən tamam çıxmalı olurdu.
    BackHandler { onRejimiDeyis() }

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
                    onYeniden = { tur++ },
                    onRejimiDeyis = onRejimiDeyis
                )
            }
            return@LazyColumn
        }

        val sual = suallar[index.coerceIn(0, umumi - 1)]

        item(key = "meta") {
            Text(
                text = "‹  Səviyyəni dəyiş",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRejimiDeyis)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Nisan(rejim.label, rejimRengi(rejim))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sual ${index + 1} / $umumi",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textDim,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Bal: $bal",
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
                        .clickable(enabled = !cavabVerilib) {
                            secim = i
                            if (i == sual.a) bal++
                        }
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
                        text = if (duzgun) "Düzdür" else "Düzgün cavab: ${('A' + sual.a)}",
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
                    text = if (index == umumi - 1) "Nəticəyə bax" else "Növbəti sual",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(KotlinBrush)
                        .clickable {
                            if (index == umumi - 1) bitdi = true else index++
                        }
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

    val rey = when {
        faiz >= 90 -> "Mükəmməl! Bu səviyyəni yaxşı bilirsən."
        faiz >= 70 -> "Yaxşı nəticə! Bir neçə mövzunu təkrarlamaq kifayətdir."
        faiz >= 50 -> "Pis deyil. Səhv verdiyin mövzuların dərslərinə qayıt."
        else -> "Başlanğıc üçün normaldır — mövzuları yenidən oxu və təkrar sına."
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Nisan(rejim.label, rejimRengi(rejim))
        Spacer(Modifier.height(10.dp))
        Text(
            text = "$faiz%",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = (46 * KAz.codeScale).sp),
            color = c.accent,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "$bal / $umumi doğru cavab",
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
                text = "${rejim.label} rekordun: $rekord / $umumi",
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
            text = "Yenidən başla",
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
            text = "Səviyyəni dəyiş",
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
