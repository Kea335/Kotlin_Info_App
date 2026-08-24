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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.ui.theme.KAz

@Composable
fun SettingsScreen(
    tema: ThemeMode,
    sriftOlcusu: Int,
    oxunanSayi: Int,
    bolmeSayi: Int,
    hellSayi: Int,
    calismaSayi: Int,
    quizRekord: Int,
    modifier: Modifier = Modifier,
    onTema: (ThemeMode) -> Unit,
    onSrift: (Int) -> Unit,
    onOxumaSifirla: () -> Unit,
    onCalismaSifirla: () -> Unit,
    onHamisiSifirla: () -> Unit
) {
    val c = KAz.colors
    var tesdiq by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Bolum("Görünüş") {
                Text(
                    text = "Tema",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ThemeMode.entries.forEach { m ->
                        Secim(
                            etiket = m.label,
                            aktiv = m == tema,
                            modifier = Modifier.weight(1f)
                        ) { onTema(m) }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Kod şrifti",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Kiçik", "Normal", "Böyük").forEachIndexed { i, ad ->
                        Secim(
                            etiket = ad,
                            aktiv = i == sriftOlcusu,
                            modifier = Modifier.weight(1f)
                        ) { onSrift(i) }
                    }
                }
            }
        }

        item {
            Bolum("Tərəqqi") {
                Setir("Oxunan bölmələr", "$oxunanSayi / $bolmeSayi")
                Setir("Həll edilən çalışmalar", "$hellSayi / $calismaSayi")
                Setir("Bilik testi rekordu", if (quizRekord > 0) "$quizRekord / 15" else "—")

                Spacer(Modifier.height(13.dp))

                Sifirla("Oxuma tərəqqisini sıfırla") { tesdiq = "oxuma" }
                Spacer(Modifier.height(7.dp))
                Sifirla("Çalışma tərəqqisini sıfırla") { tesdiq = "calisma" }
                Spacer(Modifier.height(7.dp))
                Sifirla("Hər şeyi sıfırla", tehlukeli = true) { tesdiq = "hamisi" }
            }
        }

        item {
            Bolum("Oflayn iş") {
                Text(
                    text = "Bütün dərslər, 144 kod nümunəsi (nəticələri ilə birlikdə), " +
                        "6 interaktiv nümayiş, 1250 çalışma və bilik testi tətbiqin " +
                        "içindədir — internet lazım deyil.\n\n" +
                        "Yalnız kod meydanı və praktiki çalışmalardakı «Yoxla» düyməsi " +
                        "bağlantı tələb edir, çünki kod real Kotlin kompilyatorunda " +
                        "işlədilir.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = c.textDim
                )
            }
        }

        item {
            Bolum("Mənbə") {
                Text(
                    text = "Məzmun KotlinAZ saytından götürülüb:\n" +
                        "hasanhome.tail0685ef.ts.net\n" +
                        "github.com/Kea335/Kotlin_Info_Web",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = c.textDim
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    text = "KotlinAZ · v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    // Təsdiq pəncərəsi
    if (tesdiq != null) {
        val (basliq, metn, emel) = when (tesdiq) {
            "oxuma" -> Triple(
                "Oxuma tərəqqisi silinsin?",
                "Oxunmuş bölmələrin işarəsi silinəcək. Məzmuna toxunulmur.",
                onOxumaSifirla
            )
            "calisma" -> Triple(
                "Çalışma tərəqqisi silinsin?",
                "Həll edilmiş çalışmaların işarəsi silinəcək.",
                onCalismaSifirla
            )
            else -> Triple(
                "Hər şey silinsin?",
                "Oxuma tərəqqisi, çalışmalar və test rekordu — hamısı sıfırlanacaq.",
                onHamisiSifirla
            )
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { tesdiq = null },
            title = { Text(basliq) },
            text = { Text(metn) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    emel()
                    tesdiq = null
                }) { Text("Sıfırla", color = c.err) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { tesdiq = null }) {
                    Text("İmtina")
                }
            },
            containerColor = c.bgElev,
            titleContentColor = c.text,
            textContentColor = c.textDim
        )
    }
}

@Composable
private fun Bolum(
    basliq: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val c = KAz.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(15.dp))
            .padding(15.dp)
    ) {
        Text(
            text = basliq,
            style = MaterialTheme.typography.titleSmall,
            color = c.text
        )
        Spacer(Modifier.height(11.dp))
        content()
    }
}

@Composable
private fun Secim(
    etiket: String,
    aktiv: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = KAz.colors
    Text(
        text = etiket,
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (aktiv) c.accentSoft else c.bgSunken)
            .border(
                1.dp,
                if (aktiv) c.accent.copy(alpha = 0.5f) else c.border,
                RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (aktiv) c.accent else c.textDim,
        fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Setir(acar: String, deyer: String) {
    val c = KAz.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = acar,
            style = MaterialTheme.typography.bodySmall,
            color = c.textDim,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = deyer,
            style = MaterialTheme.typography.labelMedium,
            color = c.text,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Sifirla(etiket: String, tehlukeli: Boolean = false, onClick: () -> Unit) {
    val c = KAz.colors
    val reng = if (tehlukeli) c.err else c.textDim
    Text(
        text = etiket,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(if (tehlukeli) c.err.copy(alpha = 0.07f) else c.bgSunken)
            .border(
                1.dp,
                if (tehlukeli) c.err.copy(alpha = 0.3f) else c.border,
                RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        style = MaterialTheme.typography.labelMedium,
        color = reng,
        textAlign = TextAlign.Center
    )
}
