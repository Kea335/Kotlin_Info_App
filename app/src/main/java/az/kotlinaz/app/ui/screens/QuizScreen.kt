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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import az.kotlinaz.app.data.model.QuizQuestion
import az.kotlinaz.app.ui.demos.DemoCode
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush

/**
 * Bilik testi — 15 sual, hər dəfə qarışıq sırada.
 * Tam oflayn işləyir.
 */
@Composable
fun QuizScreen(
    questions: List<QuizQuestion>,
    rekord: Int,
    modifier: Modifier = Modifier,
    onBitdi: (Int) -> Unit
) {
    val c = KAz.colors
    var tur by remember { mutableIntStateOf(0) }
    val siralama = remember(questions, tur) { questions.indices.shuffled() }

    var index by remember(tur) { mutableIntStateOf(0) }
    var bal by remember(tur) { mutableIntStateOf(0) }
    var secim by remember(tur, index) { mutableStateOf<Int?>(null) }
    var bitdi by remember(tur) { mutableStateOf(false) }

    val umumi = questions.size

    LaunchedEffect(bitdi) {
        if (bitdi) onBitdi(bal)
    }

    if (umumi == 0) return

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (bitdi) {
            item(key = "netice") {
                NeticeEkrani(bal = bal, umumi = umumi, rekord = rekord) { tur++ }
            }
            return@LazyColumn
        }

        val sual = questions[siralama[index]]

        item(key = "meta") {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
private fun NeticeEkrani(bal: Int, umumi: Int, rekord: Int, onYeniden: () -> Unit) {
    val c = KAz.colors
    val faiz = if (umumi == 0) 0 else (bal * 100 / umumi)

    val rey = when {
        faiz >= 90 -> "Mükəmməl! Kotlin-i çox yaxşı bilirsən."
        faiz >= 70 -> "Yaxşı nəticə! Bir neçə mövzunu təkrarlamaq kifayətdir."
        faiz >= 50 -> "Pis deyil. Null təhlükəsizliyi və kolleksiyalar bölmələrinə qayıt."
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
        Text(
            text = "$faiz%",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 46.sp),
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
                text = "Ən yaxşı nəticən: $rekord / $umumi",
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
                .clip(RoundedCornerShape(11.dp))
                .background(KotlinBrush)
                .clickable(onClick = onYeniden)
                .padding(horizontal = 26.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}
