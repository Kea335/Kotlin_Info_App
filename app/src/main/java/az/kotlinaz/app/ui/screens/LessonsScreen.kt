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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.SeviyyeQaydasi
import az.kotlinaz.app.data.Tereqqi
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.model.Section
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush
import az.kotlinaz.app.ui.theme.seviyyeRengi

/**
 * Bölmələrin siyahısı — saytdakı yan paneldəki naviqasiyanın qarşılığı.
 * Qruplara bölünür; bölmənin tamamlanması ona bağlı çalışmalardan gəlir.
 */
@Composable
fun LessonsScreen(
    sections: List<Section>,
    tereqqi: Tereqqi,
    sonBolme: String?,
    modifier: Modifier = Modifier,
    onSection: (String) -> Unit
) {
    val c = KAz.colors
    // groupBy sıranı saxlayır (LinkedHashMap), ona görə qruplar saytdakı
    // ardıcıllıqla düzülür: Başlanğıc → Dilin əsasları → … → Yekun.
    val qruplar = remember(sections) { sections.groupBy { it.group } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item(key = "seviyye") {
            SeviyyeKarti(tereqqi)
            Spacer(Modifier.height(9.dp))
        }

        item(key = "tereqqi") {
            TereqqiKarti(
                tamamlanan = tereqqi.tamamlananDers,
                umumi = sections.size,
                sonBolme = sonBolme?.let { id -> sections.firstOrNull { it.id == id } },
                onDavam = onSection
            )
            Spacer(Modifier.height(10.dp))
        }

        qruplar.forEach { (qrup, siyahi) ->
            item(key = "qrup-$qrup") {
                Text(
                    text = qrup.uppercase(),
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            items(siyahi, key = { it.id }) { bolme ->
                val bt = tereqqi.bolme(bolme.id)
                BolmeSetri(
                    bolme = bolme,
                    faiz = bt.faiz,
                    tamamlandi = bt.tamamlandi,
                    onClick = { onSection(bolme.id) }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

/* ============================================================
   Səviyyə
   ============================================================ */

/**
 * İstifadəçinin səviyyəsi və növbətiyə nə qədər qaldığı.
 *
 * Hər kəs Junior başlayır. Növbəti səviyyə iki şərt birlikdə ödənəndə açılır —
 * kart hər ikisini ayrıca göstərir ki, nəyin çatmadığı aydın olsun.
 */
@Composable
private fun SeviyyeKarti(tereqqi: Tereqqi) {
    val c = KAz.colors
    val lv = tereqqi.seviyye
    val reng = seviyyeRengi(lv)
    val novbeti = tereqqi.novbetiSeviyye

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
                    text = "Səviyyən",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = lv.label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(reng.copy(alpha = 0.14f))
                        .border(1.dp, reng.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = reng,
                    fontWeight = FontWeight.Bold
                )
            }
            if (novbeti != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Növbəti",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint
                    )
                    Text(
                        text = novbeti.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = seviyyeRengi(novbeti)
                    )
                }
            }
        }

        if (novbeti == null) {
            Spacer(Modifier.height(11.dp))
            Text(
                text = "Ən yüksək səviyyədəsən — bütün çalışma bankı sənin ixtiyarındadır.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                color = c.textDim
            )
            return@Column
        }

        val onSeviyye = SeviyyeQaydasi.onSeviyye(novbeti)
        val calismaSayi = tereqqi.say(onSeviyye)
        val dersHedefi = SeviyyeQaydasi.dersHedefi(novbeti)
        val lazimDers = kotlin.math.ceil(tereqqi.umumiDers * dersHedefi).toInt()

        Spacer(Modifier.height(13.dp))

        Text(
            text = "${novbeti.label} olmaq üçün",
            style = MaterialTheme.typography.labelMedium,
            color = c.text,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(9.dp))

        SertSetri(
            etiket = "${onSeviyye.label} çalışmaları",
            indiki = calismaSayi.hell,
            hedef = kotlin.math.ceil(calismaSayi.umumi * SeviyyeQaydasi.CALISMA_HEDEFI).toInt(),
            faiz = calismaSayi.faiz,
            hedefFaiz = SeviyyeQaydasi.CALISMA_HEDEFI,
            reng = seviyyeRengi(onSeviyye)
        )

        Spacer(Modifier.height(10.dp))

        SertSetri(
            etiket = "Tamamlanmış dərslər",
            indiki = tereqqi.tamamlananDers,
            hedef = lazimDers,
            faiz = tereqqi.dersFaizi,
            hedefFaiz = dersHedefi,
            reng = KAz.colors.accent
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Dərs yalnız ona bağlı bütün çalışmalar düzgün həll ediləndə " +
                "tamamlanır — sürətlə sürüşdürmək saymır.",
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
            color = c.textFaint
        )
    }
}

/**
 * Bir səviyyə şərti: cari say, tələb olunan say və zolaq.
 *
 * Zolaq TƏLƏBƏ görə doldurulur (nisbət `faiz / hedefFaiz`), ona görə tam
 * dolduğu an şərt ödənmiş olur — istifadəçi 80%-i gözlə axtarmalı olmur.
 */
@Composable
private fun SertSetri(
    etiket: String,
    indiki: Int,
    hedef: Int,
    faiz: Float,
    hedefFaiz: Float,
    reng: Color
) {
    val c = KAz.colors
    val doluluq = if (hedefFaiz <= 0f) 1f else (faiz / hedefFaiz).coerceIn(0f, 1f)
    val odenib = doluluq >= 1f

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = etiket,
                style = MaterialTheme.typography.labelSmall,
                color = c.textDim,
                modifier = Modifier.weight(1f)
            )
            if (odenib) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = c.ok,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = "$indiki / $hedef",
                style = MaterialTheme.typography.labelSmall,
                color = if (odenib) c.ok else c.textFaint,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(5.dp))
        Zolaq(doluluq, duzFirca(if (odenib) c.ok else reng))
    }
}

/** Tək rəngdən Brush — `Zolaq` həm qradient, həm düz rəng qəbul etsin deyə. */
private fun duzFirca(reng: Color): Brush = Brush.linearGradient(listOf(reng, reng))

@Composable
private fun Zolaq(faiz: Float, firca: Brush, hundurluk: Int = 6) {
    val c = KAz.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(hundurluk.dp)
            .clip(CircleShape)
            .background(c.bgSunken)
    ) {
        if (faiz > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(faiz)
                    .height(hundurluk.dp)
                    .clip(CircleShape)
                    .background(firca)
            )
        }
    }
}

/* ============================================================
   Bölmə siyahısı
   ============================================================ */

@Composable
private fun BolmeSetri(
    bolme: Section,
    faiz: Float,
    tamamlandi: Boolean,
    onClick: () -> Unit
) {
    val c = KAz.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bgElev)
            .border(
                1.dp,
                if (tamamlandi) c.ok.copy(alpha = 0.4f) else c.border,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                // DÜZƏLİŞ: `index` 0-dan başlayır, ona görə ilk dərs «00» kimi
                // görünürdü. Nömrələmə istifadəçi üçün 01-dən başlamalıdır.
                text = (bolme.index + 1).toString().padStart(2, '0'),
                modifier = Modifier.width(28.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (tamamlandi) c.ok else c.textFaint,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.width(9.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = bolme.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = c.text
                )
                Text(
                    text = when (bolme.special) {
                        "playground" -> "İnternet tələb edir"
                        "exercises" -> "1250 çalışma"
                        "quiz" -> "Səviyyə üzrə test"
                        else -> if (faiz > 0f && !tamamlandi) {
                            "${(faiz * 100).toInt()}% mənimsənilib"
                        } else {
                            ""
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
            }

            if (tamamlandi) {
                Box(
                    Modifier.size(20.dp).background(c.ok.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Tamamlanıb",
                        tint = c.ok,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Yarımçıq bölmələrdə nazik zolaq — nə qədər qaldığı bir baxışda görünsün.
        if (faiz > 0f && !tamamlandi) {
            Spacer(Modifier.height(8.dp))
            Zolaq(faiz, KotlinBrush, hundurluk = 3)
        }
    }
}

@Composable
private fun TereqqiKarti(
    tamamlanan: Int,
    umumi: Int,
    sonBolme: Section?,
    onDavam: (String) -> Unit
) {
    val c = KAz.colors
    // Sıfıra bölünmə qorunması: məzmun hələ gəlməyibsə faiz 0 olur.
    val faiz = if (umumi == 0) 0f else tamamlanan.toFloat() / umumi

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
                    text = "Öyrənmə tərəqqisi",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.text
                )
                Text(
                    text = "$tamamlanan / $umumi bölmə tamamlanıb",
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

        Zolaq(faiz, KotlinBrush, hundurluk = 7)

        if (sonBolme != null) {
            Spacer(Modifier.height(13.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.accentSoft)
                    .clickable { onDavam(sonBolme.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    tint = c.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Davam et",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint
                    )
                    Text(
                        text = sonBolme.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = c.accent
                    )
                }
            }
        }
    }
}
