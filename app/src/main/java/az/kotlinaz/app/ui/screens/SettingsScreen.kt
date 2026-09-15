package az.kotlinaz.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.BuildConfig
import az.kotlinaz.app.data.KompilyatorVersiyalari
import az.kotlinaz.app.data.KompilyatorVeziyyeti
import az.kotlinaz.app.data.Tereqqi
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.ui.components.LocalBildiris
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.seviyyeRengi

/** Müəllifin GitHub səhifəsi. */
private const val GITHUB = "https://github.com/Kea335"

@Composable
fun SettingsScreen(
    tema: ThemeMode,
    sriftOlcusu: Int,
    kodTamamlama: Boolean,
    tereqqi: Tereqqi,
    /** Bilik testinin rejim üzrə rekordları. */
    quizRekordlari: Map<String, Int>,
    /** Kod icrasında işlədilən Kotlin versiyası. */
    kompilyator: KompilyatorVeziyyeti,
    modifier: Modifier = Modifier,
    onTema: (ThemeMode) -> Unit,
    onSrift: (Int) -> Unit,
    onTamamlama: (Boolean) -> Unit,
    onOxumaSifirla: () -> Unit,
    onCalismaSifirla: () -> Unit,
    onHamisiSifirla: () -> Unit
) {
    val c = KAz.colors
    val context = LocalContext.current
    val bildir = LocalBildiris.current
    // Hansı sıfırlama təsdiqlənir: "oxuma" | "calisma" | "hamisi" | null (pəncərə bağlı).
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
                    text = "Yazı şrifti",
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
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Seçim bütün tətbiqə təsir edir — dərs mətni, düymələr " +
                        "və kod blokları birlikdə böyüyüb kiçilir.",
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                    color = c.textFaint
                )
            }
        }

        item {
            Bolum("Redaktor") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Kod tamamlama",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.text
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Kod yazarkən sözün üstündə təkliflər çıxır: «pri» " +
                                "yazanda print/println, «va» yazanda val/var. " +
                                "Kod meydanında və praktiki çalışmalarda işləyir.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = c.textDim
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = kodTamamlama,
                        onCheckedChange = onTamamlama,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = c.accent,
                            uncheckedTrackColor = c.bgSunken,
                            uncheckedBorderColor = c.border
                        )
                    )
                }
            }
        }

        item {
            Bolum("Tərəqqi") {
                Setir("Səviyyən", tereqqi.seviyye.label, vurgu = seviyyeRengi(tereqqi.seviyye))
                Setir(
                    "Tamamlanmış dərslər",
                    "${tereqqi.tamamlananDers} / ${tereqqi.umumiDers}"
                )
                Level.entries.forEach { lv ->
                    val say = tereqqi.say(lv)
                    Setir("${lv.label} çalışmalar", "${say.hell} / ${say.umumi}")
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Dərs yalnız ona bağlı çalışmaların hamısı düzgün həll " +
                        "ediləndə tamamlanmış sayılır.",
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                    color = c.textFaint
                )

                Spacer(Modifier.height(13.dp))

                Sifirla("Dərs təsdiqlərini sıfırla") { tesdiq = "oxuma" }
                Spacer(Modifier.height(7.dp))
                Sifirla("Çalışma tərəqqisini sıfırla") { tesdiq = "calisma" }
                Spacer(Modifier.height(7.dp))
                Sifirla("Hər şeyi sıfırla", tehlukeli = true) { tesdiq = "hamisi" }
            }
        }

        item {
            Bolum("Bilik testi rekordları") {
                val varmi = QuizRejimi.entries.any { (quizRekordlari[it.id] ?: 0) > 0 }
                if (!varmi) {
                    Text(
                        text = "Hələ test verməmisən. Test bölməsində Junior, Middle, " +
                            "Senior və Qarışıq rejimləri var.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                        color = c.textDim
                    )
                } else {
                    QuizRejimi.entries.forEach { r ->
                        val rekord = quizRekordlari[r.id] ?: 0
                        Setir(r.label, if (rekord > 0) "$rekord / $QUIZ_SUAL_SAYI" else "—")
                    }
                }
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

                Spacer(Modifier.height(8.dp))
                Setir("Kompilyator", "Kotlin ${kompilyator.versiya}")

                // Boz qeyd yalnız deyiləsi söz olanda: tətbiqin öz versiyası
                // serverdə yoxdursa, ya da siyahı hələ serverdən yoxlanılmayıbsa.
                val qeyd = when {
                    kompilyator.tetbiqdenFerqli ->
                        "Tətbiq Kotlin ${KompilyatorVersiyalari.TETBIQIN} ilə qurulub; " +
                            "server bu versiyanı təklif etmədiyi üçün kod " +
                            "${kompilyator.versiya} versiyasında icra olunur."
                    !kompilyator.serverden ->
                        "Versiya siyahısı ilk icrada serverdən yoxlanılır."
                    else -> null
                }
                if (qeyd != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = qeyd,
                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                        color = c.textFaint
                    )
                }
            }
        }

        item {
            Bolum("Mənbə") {
                Text(
                    text = "Məzmun KotlinAZ saytından götürülüb.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = c.textDim
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.bgSunken)
                        .border(1.dp, c.border, RoundedCornerShape(10.dp))
                        .clickable {
                            // Brauzer tapılmasa çökməmək üçün: xəbərdarlıq verilir.
                            val niyyet = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB))
                            try {
                                context.startActivity(niyyet)
                            } catch (_: ActivityNotFoundException) {
                                bildir("Brauzer tapılmadı")
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "GitHub",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textFaint
                        )
                        Text(
                            text = "github.com/Kea335",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.accent
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = c.accent,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    text = "KotlinAZ · v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    // Təsdiq pəncərəsi — sıfırlama geri qaytarıla bilmir, ona görə əvvəlcə soruşulur.
    if (tesdiq != null) {
        val (basliq, metn, emel) = when (tesdiq) {
            "oxuma" -> Triple(
                "Dərs təsdiqləri silinsin?",
                "Çalışması olmayan bölmələrdə verdiyin «Oxudum» təsdiqləri silinəcək. " +
                    "Çalışmalardan gələn mənimsəməyə toxunulmur.",
                onOxumaSifirla
            )
            "calisma" -> Triple(
                "Çalışma tərəqqisi silinsin?",
                "Həll edilmiş çalışmaların işarəsi silinəcək — dərslərin mənimsəməsi " +
                    "və səviyyən də sıfırlanacaq.",
                onCalismaSifirla
            )
            else -> Triple(
                "Hər şey silinsin?",
                "Dərs təsdiqləri, çalışmalar və test rekordları — hamısı sıfırlanacaq.",
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
private fun Setir(acar: String, deyer: String, vurgu: Color? = null) {
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
            color = vurgu ?: c.text,
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
