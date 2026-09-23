package az.kotlinaz.app.ui.playground

import az.kotlinaz.app.ui.components.SoldanSaga
import androidx.compose.ui.res.stringResource
import az.kotlinaz.app.R
import az.kotlinaz.app.ui.xetaMetni
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.kotlinaz.app.data.model.PlaygroundPreset
import az.kotlinaz.app.ui.components.LocalBildiris
import az.kotlinaz.app.ui.components.kopyala
import az.kotlinaz.app.ui.components.paylas
import az.kotlinaz.app.ui.demos.DemoChip
import az.kotlinaz.app.ui.editor.KodRedaktoru
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.KotlinBrush
import az.kotlinaz.app.ui.AppViewModel

/**
 * Kod meydanı — tətbiqin YEGANƏ internet tələb edən hissəsi.
 * Kod JetBrains-in rəsmi kompilyator xidmətində işlədilir; saytdakı
 * Kotlin Playground da məhz bu xidməti çağırır.
 *
 * Vəziyyət və icra [PlaygroundViewModel]-dədir — bu Composable yalnız
 * `ui`-ni render edir və hadisə göndərir.
 */
@Composable
fun PlaygroundScreen(
    vm: PlaygroundViewModel,
    appVm: AppViewModel,
    modifier: Modifier = Modifier
) {
    val presets by appVm.presets.collectAsStateWithLifecycle()
    val kompilyator by appVm.kompilyator.collectAsStateWithLifecycle()
    val meydanKodu by appVm.meydanKodu.collectAsStateWithLifecycle()
    PlaygroundContent(
        vm = vm,
        presets = presets,
        kompilyatorVersiyasi = kompilyator.versiya,
        xariciKod = meydanKodu,
        modifier = modifier,
        onXariciKodAlindi = { appVm.meydanKodunuTemizle() }
    )
}

/** Saf render — məzmun parametrlə, redaktor və icra [PlaygroundViewModel]-dən. */
@Composable
private fun PlaygroundContent(
    vm: PlaygroundViewModel,
    presets: List<PlaygroundPreset>,
    /** Sorğularda işlədilən Kotlin versiyası — xəbərdarlıq zolağında göstərilir. */
    kompilyatorVersiyasi: String,
    xariciKod: String?,
    modifier: Modifier = Modifier,
    onXariciKodAlindi: () -> Unit
) {
    val c = KAz.colors
    val context = LocalContext.current
    val bildir = LocalBildiris.current

    // Redaktorun vəziyyəti ViewModel-dəki `MutableState`-dir və olduğu kimi
    // KodRedaktoru-ya verilir: hər hərf yalnız redaktoru yeniləyir, bu ekranın
    // qalan hissəsi (çiplər, düymələr, nəticə paneli) yenidən qurulmur.
    val kod = vm.kod
    val veziyyet by vm.ui.collectAsStateWithLifecycle()
    val seciliPreset by vm.seciliPreset.collectAsStateWithLifecycle()

    // Kod kartındakı «Meydan» düyməsindən gələn kod. Alındıqdan sonra
    // `onXariciKodAlindi()` onu AppViewModel-də təmizləyir — əks halda ekrana
    // hər qayıdışda istifadəçinin yazdığı kod yenidən əvəzlənərdi.
    LaunchedEffect(xariciKod) {
        if (xariciKod != null) {
            vm.xariciKodQoy(xariciKod)
            onXariciKodAlindi()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        item(key = "xeberdarliq") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(c.accentSoft)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).background(KotlinBrush, CircleShape))
                Spacer(Modifier.width(9.dp))
                Column {
                    Text(
                        text = stringResource(R.string.meydan_xeberdarliq),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = c.textDim
                    )
                    Spacer(Modifier.height(4.dp))
                    // İstifadəçi hansı versiyada yazdığını bilməlidir: məsələn
                    // `when` guard sintaksisi 2.1-də xəta verir, 2.4-də işləyir.
                    Text(
                        text = stringResource(R.string.meydan_kompilyator, kompilyatorVersiyasi),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint
                    )
                }
            }
            Spacer(Modifier.height(13.dp))
        }

        item(key = "numuneler") {
            Text(
                text = stringResource(R.string.meydan_baslangic),
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(7.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Boş redaktor ƏN QABAQDA: meydana gələn adam əvvəlcə öz kodunu
                // yazmaq istəyir, hazır nümunə ikinci addımdır.
                // Kursor dərhal `main` gövdəsinin içinə qoyulur —
                // istifadəçi çipdən sonra birbaşa yazmağa başlaya bilsin.
                DemoChip(etiket = stringResource(R.string.meydan_bos_redaktor), aktiv = seciliPreset == null) {
                    vm.presetSec(null, null)
                }
                presets.forEach { p ->
                    DemoChip(etiket = p.label, aktiv = p.id == seciliPreset) {
                        vm.presetSec(p.id, p.code)
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
        }

        item(key = "redaktor") {
            KodRedaktoru(
                kod = kod,
                minHundurluk = 210.dp,
                basliqSagi = {
                    val paylasBasliq = stringResource(R.string.meydan_paylas)
                    val paylasYoxdur = stringResource(R.string.meydan_paylas_tetbiq_yoxdur)
                    val kopyalandi = stringResource(R.string.ortaq_kod_kopyalandi)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = paylasBasliq,
                            tint = c.textDim,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    if (!paylas(context, kod.value.text, paylasBasliq)) {
                                        bildir(paylasYoxdur)
                                    }
                                }
                        )
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.ortaq_kopyala),
                            tint = c.textDim,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    kopyala(context, kod.value.text)
                                    bildir(kopyalandi)
                                }
                        )
                    }
                }
            )

            Spacer(Modifier.height(11.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // İcra gedərkən «İşlə» «Dayandır»-a çevrilir: eyni kodu paralel
                // iki dəfə göndərməyin mənası yoxdur, amma asılı qalmış (və ya
                // sonsuz dövrlü) sorğunu 60 s gözləmək də lazım deyil.
                val isleyir = veziyyet is MeydanVeziyyeti.Isleyir
                Text(
                    text = stringResource(if (isleyir) R.string.meydan_dayandir else R.string.meydan_isle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .then(if (isleyir) Modifier.background(c.err) else Modifier.background(KotlinBrush))
                        .clickable { if (isleyir) vm.dayandir() else vm.isle() }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )

                // Yalnız nəticə panelini bağlayır; redaktordakı koda toxunmur.
                // Kodu silmək üçün yuxarıdakı «Kod meydanı» çipi var.
                Text(
                    text = stringResource(R.string.meydan_temizle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(c.bgSunken)
                        .border(1.dp, c.border, RoundedCornerShape(11.dp))
                        .clickable { vm.temizle() }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textDim
                )
            }

            Spacer(Modifier.height(13.dp))
            MeydanNeticesi(veziyyet)
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun MeydanNeticesi(veziyyet: MeydanVeziyyeti) {
    val c = KAz.colors

    when (veziyyet) {
        MeydanVeziyyeti.Bos -> Unit

        MeydanVeziyyeti.Isleyir -> Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(c.bgSunken)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = c.accent
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.meydan_isleyir),
                style = MaterialTheme.typography.bodySmall,
                color = c.textDim
            )
        }

        is MeydanVeziyyeti.Cixis -> Panel(stringResource(R.string.ortaq_netice), c.ok) {
            KonsolMetn(veziyyet.metn.ifBlank { stringResource(R.string.meydan_cixis_yoxdur) }, c.ok)
        }

        is MeydanVeziyyeti.Xetalar -> Panel(stringResource(veziyyet.nov.basliqRes), c.err) {
            KonsolMetn(xetaMetni(veziyyet.setirler), c.err)
        }

        MeydanVeziyyeti.Dayandirildi -> Panel(stringResource(R.string.meydan_dayandirildi), c.textDim) {
            Text(
                text = stringResource(R.string.meydan_dayandirildi_izah),
                style = MaterialTheme.typography.bodySmall,
                color = c.textDim
            )
        }

        MeydanVeziyyeti.Oflayn -> Panel(stringResource(R.string.ortaq_internet_yoxdur), c.warn) {
            Text(
                text = stringResource(R.string.meydan_oflayn_izah),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = c.textDim
            )
        }
    }
}

@Composable
private fun Panel(
    basliq: String,
    reng: Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(reng.copy(alpha = 0.08f))
            .border(1.dp, reng.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(13.dp)
    ) {
        Text(
            text = basliq,
            style = MaterialTheme.typography.labelLarge,
            color = reng
        )
        Spacer(Modifier.height(7.dp))
        content()
    }
}

@Composable
private fun KonsolMetn(metn: String, reng: Color) = SoldanSaga {
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Text(
            text = metn,
            fontFamily = FontFamily.Monospace,
            fontSize = (12.5f * KAz.codeScale).sp,
            lineHeight = (19 * KAz.codeScale).sp,
            color = reng,
            softWrap = false
        )
    }
}
