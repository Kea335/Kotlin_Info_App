package az.kotlinaz.app.ui.statistika

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.kotlinaz.app.R
import az.kotlinaz.app.data.GunSutunu
import az.kotlinaz.app.data.SeviyyeSayi
import az.kotlinaz.app.data.Statistika
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.statistikaHesabla
import az.kotlinaz.app.ui.AppViewModel
import az.kotlinaz.app.ui.components.YuklenmeGostericisi
import az.kotlinaz.app.ui.etiket
import az.kotlinaz.app.ui.lessons.SeviyyeKarti
import az.kotlinaz.app.ui.quiz.QuizRejimi
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.seviyyeRengi
import java.text.DateFormat
import java.util.Date

/**
 * Statistika (T5.4): səviyyə, sayğaclar, son 30 günün fəallığı, seriya və
 * quiz tarixçəsi. Tərəqqi AppViewModel-dən, tarixçə Room-dan; hesablama saf
 * `statistikaHesabla`-dadır.
 */
@Composable
fun StatistikaScreen(
    vm: StatistikaViewModel,
    appVm: AppViewModel,
    modifier: Modifier = Modifier
) {
    val tereqqi by appVm.tereqqi.collectAsStateWithLifecycle()
    val hazir by appVm.movzularHazir.collectAsStateWithLifecycle()
    val turlar by vm.turlar.collectAsStateWithLifecycle()
    val fealiyyet by vm.fealiyyet.collectAsStateWithLifecycle()

    val t = turlar
    val f = fealiyyet
    if (!hazir || t == null || f == null) {
        YuklenmeGostericisi(modifier)
        return
    }
    val stat = remember(tereqqi, t, f, vm.bugun) { statistikaHesabla(tereqqi, t, f, vm.bugun) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "seviyye") { SeviyyeKarti(tereqqi) }
        item(key = "sayqaclar") { Sayqaclar(stat) }
        item(key = "fealiyyet") { Fealiyyet(stat) }
        item(key = "quiz") { QuizTarixcesi(stat) }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun Kart(basliq: String, content: @Composable ColumnScope.() -> Unit) {
    val c = KAz.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = basliq, style = MaterialTheme.typography.titleSmall, color = c.text)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun Setir(acar: String, deyer: String, reng: androidx.compose.ui.graphics.Color? = null) {
    val c = KAz.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(acar, style = MaterialTheme.typography.bodyMedium, color = c.textDim, modifier = Modifier.weight(1f))
        Text(deyer, style = MaterialTheme.typography.labelLarge, color = reng ?: c.text, fontWeight = FontWeight.SemiBold)
    }
}

private fun faiz(s: SeviyyeSayi): Int = (s.faiz * 100).toInt()

@Composable
private fun Sayqaclar(stat: Statistika) {
    Kart(stringResource(R.string.stat_sayqaclar)) {
        Setir(
            stringResource(R.string.stat_dersler),
            stringResource(R.string.ortaq_say_bolu, stat.dersler.hell, stat.dersler.umumi)
        )
        Setir(
            stringResource(R.string.stat_calismalar),
            stringResource(R.string.ortaq_say_bolu, stat.calismalar.hell, stat.calismalar.umumi)
        )
        Level.entries.forEach { lv ->
            val s = stat.seviyyeUzre[lv] ?: SeviyyeSayi(0, 0)
            Setir(lv.etiket(), stringResource(R.string.ortaq_faiz, faiz(s)), seviyyeRengi(lv))
        }
    }
}

@Composable
private fun Fealiyyet(stat: Statistika) {
    val c = KAz.colors
    val cemi = stat.son30Gun.sumOf { it.say }
    val fealGun = stat.son30Gun.count { it.say > 0 }
    val xulase = stringResource(
        R.string.stat_fealiyyet_xulase,
        pluralStringResource(R.plurals.stat_calisma_sayi, cemi, cemi),
        pluralStringResource(R.plurals.stat_gun_sayi, fealGun, fealGun)
    )
    Kart(stringResource(R.string.stat_fealiyyet)) {
        Text(xulase, style = MaterialTheme.typography.bodySmall, color = c.textDim)
        Spacer(Modifier.height(12.dp))
        SutunQrafiki(stat.son30Gun, xulase)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Seriya(
                ikon = { Icon(Icons.Outlined.LocalFireDepartment, null, tint = c.warn, modifier = Modifier.size(20.dp)) },
                etiket = stringResource(R.string.stat_seriya),
                deyer = pluralStringResource(R.plurals.stat_gun_sayi, stat.streak, stat.streak),
                modifier = Modifier.weight(1f)
            )
            Seriya(
                ikon = { Icon(Icons.Filled.EmojiEvents, null, tint = c.accent, modifier = Modifier.size(20.dp)) },
                etiket = stringResource(R.string.stat_en_uzun_seriya),
                deyer = pluralStringResource(R.plurals.stat_gun_sayi, stat.enUzunStreak, stat.enUzunStreak),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 30 sütun — kitabxanasız, Canvas-da. Boş gün nazik əsas xətt kimi görünür
 * (hamısı sıfır olsa da qrafik «sınıq» görünmür). Bu gün vurğulanır. Rənglər
 * temadandır — işıqlı və qaranlıqda oxunur. TalkBack üçün xülasə.
 */
@Composable
private fun SutunQrafiki(gunler: List<GunSutunu>, tesvir: String) {
    val c = KAz.colors
    val maks = (gunler.maxOfOrNull { it.say } ?: 0).coerceAtLeast(1)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .semantics { contentDescription = tesvir }
    ) {
        val n = gunler.size.coerceAtLeast(1)
        val aralig = 3.dp.toPx()
        val en = (size.width - aralig * (n - 1)) / n
        val minH = 2.dp.toPx()
        gunler.forEachIndexed { i, g ->
            val h = if (g.say == 0) minH else (size.height * g.say / maks).coerceAtLeast(minH * 2)
            val reng = when {
                g.say == 0 -> c.border
                i == gunler.lastIndex -> c.accent
                else -> c.accent.copy(alpha = 0.55f)
            }
            drawRoundRect(
                color = reng,
                topLeft = Offset(i * (en + aralig), size.height - h),
                size = Size(en, h),
                cornerRadius = CornerRadius(en / 3, en / 3)
            )
        }
    }
}

@Composable
private fun Seriya(ikon: @Composable () -> Unit, etiket: String, deyer: String, modifier: Modifier) {
    val c = KAz.colors
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.bgSunken)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ikon()
        Spacer(Modifier.width(9.dp))
        Column {
            Text(etiket, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
            Text(deyer, style = MaterialTheme.typography.titleSmall, color = c.text)
        }
    }
}

@Composable
private fun QuizTarixcesi(stat: Statistika) {
    val c = KAz.colors
    val locale = LocalConfiguration.current.locales[0]
    Kart(stringResource(R.string.stat_quiz_tarixcesi)) {
        if (stat.quizTarixcesi.isEmpty()) {
            Text(
                stringResource(R.string.stat_quiz_bos),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                color = c.textDim
            )
            return@Kart
        }
        stat.quizTarixcesi.forEach { s ->
            val rejim = QuizRejimi.entries.firstOrNull { it.id == s.tur.rejim }
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        rejim?.etiket() ?: s.tur.rejim,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.text
                    )
                    Text(
                        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(s.tur.tarix)),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint
                    )
                }
                if (s.rekorddur) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = stringResource(R.string.stat_rekord),
                        tint = c.accent,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    stringResource(R.string.ortaq_say_bolu, s.tur.bal, s.tur.umumi),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (s.rekorddur) c.accent else c.text,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
