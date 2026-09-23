package az.kotlinaz.app.ui.settings

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import az.kotlinaz.app.R
import az.kotlinaz.app.ui.etiket
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.kotlinaz.app.BuildConfig
import az.kotlinaz.app.data.AppDil
import az.kotlinaz.app.data.KompilyatorVersiyalari
import az.kotlinaz.app.data.KompilyatorVeziyyeti
import az.kotlinaz.app.data.Tereqqi
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.ui.AppViewModel
import az.kotlinaz.app.ui.components.LocalBildiris
import az.kotlinaz.app.ui.quiz.QUIZ_SUAL_SAYI
import az.kotlinaz.app.ui.quiz.QuizRejimi
import az.kotlinaz.app.ui.theme.KAz
import az.kotlinaz.app.ui.theme.seviyyeRengi

/** Müəllifin GitHub səhifəsi. */
private const val GITHUB = "https://github.com/Kea335"

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    appVm: AppViewModel,
    modifier: Modifier = Modifier,
    onSehvlerim: () -> Unit = {},
    onIsareler: () -> Unit = {}
) {
    val bildir = LocalBildiris.current
    // Bildiriş mətnləri lambda-lardan əvvəl oxunur — onlar composable deyil.
    val oxumaSifirlandi = stringResource(R.string.ayarlar_oxuma_sifirlandi)
    val calismaSifirlandi = stringResource(R.string.ayarlar_calisma_sifirlandi)
    val hamisiSifirlandi = stringResource(R.string.ayarlar_hamisi_sifirlandi)
    val tema by vm.tema.collectAsStateWithLifecycle()
    val dil by vm.dil.collectAsStateWithLifecycle()
    val diller by vm.gorunenDiller.collectAsStateWithLifecycle()
    val mezmunDili by appVm.mezmunDili.collectAsStateWithLifecycle()
    val srift by vm.sriftOlcusu.collectAsStateWithLifecycle()
    val tamamlama by vm.kodTamamlama.collectAsStateWithLifecycle()
    val quizRekordlari by vm.quizRekordlari.collectAsStateWithLifecycle()
    val tereqqi by appVm.tereqqi.collectAsStateWithLifecycle()
    val kompilyator by appVm.kompilyator.collectAsStateWithLifecycle()
    val sehvSayi by vm.sehvSayi.collectAsStateWithLifecycle()

    SettingsContent(
        tema = tema,
        dil = dil,
        diller = diller,
        mezmunDili = mezmunDili,
        sriftOlcusu = srift,
        kodTamamlama = tamamlama,
        tereqqi = tereqqi,
        quizRekordlari = quizRekordlari,
        kompilyator = kompilyator,
        sehvSayi = sehvSayi,
        onSehvlerim = onSehvlerim,
        onIsareler = onIsareler,
        modifier = modifier,
        onTema = { vm.temaSec(it) },
        onDil = { vm.dilSec(it) },
        onSrift = { vm.sriftSec(it) },
        onTamamlama = { vm.tamamlamaSec(it) },
        onOxumaSifirla = {
            vm.oxumaTereqqisiniSifirla()
            bildir(oxumaSifirlandi)
        },
        onCalismaSifirla = {
            vm.calismaTereqqisiniSifirla()
            bildir(calismaSifirlandi)
        },
        onHamisiSifirla = {
            vm.hamisiniSifirla()
            bildir(hamisiSifirlandi)
        }
    )
}

/** Saf render — bütün vəziyyət parametrlə gəlir. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(
    tema: ThemeMode,
    /** Seçilmiş dil; null = sistem dili. */
    dil: AppDil?,
    /** Siyahıda göstərilən dillər — release-də yalnız tam tərcümə olunmuşlar (T4.7). */
    diller: List<AppDil>,
    /** Məzmunun faktiki dili — seçilən dildə tərcümə yoxdursa fərqlənir. */
    mezmunDili: String,
    sriftOlcusu: Int,
    kodTamamlama: Boolean,
    tereqqi: Tereqqi,
    /** Bilik testinin rejim üzrə rekordları. */
    quizRekordlari: Map<String, Int>,
    /** Kod icrasında işlədilən Kotlin versiyası. */
    kompilyator: KompilyatorVeziyyeti,
    /** Mənimsənilməmiş səhv sualların sayı (T5.2). */
    sehvSayi: Int,
    onSehvlerim: () -> Unit,
    onIsareler: () -> Unit,
    modifier: Modifier = Modifier,
    onTema: (ThemeMode) -> Unit,
    onDil: (AppDil?) -> Unit,
    onSrift: (Int) -> Unit,
    onTamamlama: (Boolean) -> Unit,
    onOxumaSifirla: () -> Unit,
    onCalismaSifirla: () -> Unit,
    onHamisiSifirla: () -> Unit
) {
    val c = KAz.colors
    val context = LocalContext.current
    val bildir = LocalBildiris.current
    val brauzerYoxdur = stringResource(R.string.ortaq_brauzer_yoxdur)
    // Hansı sıfırlama təsdiqlənir: "oxuma" | "calisma" | "hamisi" | null (pəncərə bağlı).
    // Açıq dialoq döndərmədə itməsin deyə saveable (T2.4).
    var tesdiq by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Bolum(stringResource(R.string.ayarlar_gorunus)) {
                Text(
                    text = stringResource(R.string.ayarlar_tema),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ThemeMode.entries.forEach { m ->
                        Secim(
                            etiket = m.etiket(),
                            aktiv = m == tema,
                            modifier = Modifier.weight(1f)
                        ) { onTema(m) }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.ayarlar_srift),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(
                        R.string.ayarlar_srift_kicik,
                        R.string.ayarlar_srift_normal,
                        R.string.ayarlar_srift_boyuk
                    ).forEachIndexed { i, ad ->
                        Secim(
                            etiket = stringResource(ad),
                            aktiv = i == sriftOlcusu,
                            modifier = Modifier.weight(1f)
                        ) { onSrift(i) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.ayarlar_srift_izah),
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                    color = c.textFaint
                )
            }
        }

        item {
            Bolum(stringResource(R.string.ayarlar_dil)) {
                // Hər dil öz adı ilə — istifadəçi başa düşmədiyi dildəki siyahıda
                // öz dilini tapa bilsin. «Sistem dili» birincidir.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Secim(etiket = stringResource(R.string.ayarlar_sistem_dili), aktiv = dil == null, yigcam = true) { onDil(null) }
                    diller.forEach { d ->
                        Secim(etiket = d.ozAdi, aktiv = d == dil, yigcam = true) { onDil(d) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Seçilən dildə məzmun hələ yoxdursa dərslər hansı dildə gəlir —
                // istifadəçi «niyə dəyişmədi» deyə çaşmasın.
                val secilenTeq = dil?.teq
                val qeyd = if (secilenTeq != null && secilenTeq != mezmunDili) {
                    val adi = AppDil.from(mezmunDili)?.ozAdi ?: mezmunDili
                    stringResource(R.string.ayarlar_dil_mezmun_yoxdur, adi)
                } else {
                    stringResource(R.string.ayarlar_dil_izah)
                }
                Text(
                    text = qeyd,
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                    color = c.textFaint
                )
            }
        }

        item {
            Bolum(stringResource(R.string.ayarlar_redaktor)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.ayarlar_tamamlama),
                            style = MaterialTheme.typography.labelLarge,
                            color = c.text
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.ayarlar_tamamlama_izah),
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
            Bolum(stringResource(R.string.ayarlar_tereqqi)) {
                Setir(stringResource(R.string.ayarlar_seviyyen), tereqqi.seviyye.etiket(), vurgu = seviyyeRengi(tereqqi.seviyye))
                Setir(
                    stringResource(R.string.ayarlar_tamamlanan_dersler),
                    stringResource(R.string.ortaq_say_bolu, tereqqi.tamamlananDers, tereqqi.umumiDers)
                )
                Level.entries.forEach { lv ->
                    val say = tereqqi.say(lv)
                    Setir(
                        stringResource(R.string.ayarlar_seviyye_calismalar, lv.etiket()),
                        stringResource(R.string.ortaq_say_bolu, say.hell, say.umumi)
                    )
                }

                Spacer(Modifier.height(6.dp))
                Kecid(
                    ust = stringResource(R.string.sehvlerim_basliq),
                    etiket = pluralStringResource(R.plurals.sehvlerim_sayi, sehvSayi, sehvSayi),
                    onClick = onSehvlerim
                )
                Spacer(Modifier.height(7.dp))
                Kecid(
                    ust = stringResource(R.string.ayarlar_isareler_izah),
                    etiket = stringResource(R.string.isareler_basliq),
                    onClick = onIsareler
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.ayarlar_tereqqi_izah),
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                    color = c.textFaint
                )

                Spacer(Modifier.height(13.dp))

                Sifirla(stringResource(R.string.ayarlar_sifirla_oxuma)) { tesdiq = "oxuma" }
                Spacer(Modifier.height(7.dp))
                Sifirla(stringResource(R.string.ayarlar_sifirla_calisma)) { tesdiq = "calisma" }
                Spacer(Modifier.height(7.dp))
                Sifirla(stringResource(R.string.ayarlar_sifirla_hamisi), tehlukeli = true) { tesdiq = "hamisi" }
            }
        }

        item {
            Bolum(stringResource(R.string.ayarlar_rekordlar)) {
                val varmi = QuizRejimi.SECIM.any { (quizRekordlari[it.id] ?: 0) > 0 }
                if (!varmi) {
                    Text(
                        text = stringResource(R.string.ayarlar_rekord_yoxdur),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                        color = c.textDim
                    )
                } else {
                    QuizRejimi.SECIM.forEach { r ->
                        val rekord = quizRekordlari[r.id] ?: 0
                        Setir(
                            r.etiket(),
                            if (rekord > 0) stringResource(R.string.ortaq_say_bolu, rekord, QUIZ_SUAL_SAYI) else "—"
                        )
                    }
                }
            }
        }

        item {
            Bolum(stringResource(R.string.ayarlar_oflayn)) {
                Text(
                    text = stringResource(R.string.ayarlar_oflayn_izah),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = c.textDim
                )

                Spacer(Modifier.height(8.dp))
                Setir(
                    stringResource(R.string.ayarlar_kompilyator),
                    stringResource(R.string.ortaq_kotlin_versiya, kompilyator.versiya)
                )

                // Boz qeyd yalnız deyiləsi söz olanda: tətbiqin öz versiyası
                // serverdə yoxdursa, ya da siyahı hələ serverdən yoxlanılmayıbsa.
                val qeyd = when {
                    kompilyator.tetbiqdenFerqli ->
                        stringResource(
                            R.string.ayarlar_kompilyator_ferqli,
                            KompilyatorVersiyalari.TETBIQIN,
                            kompilyator.versiya
                        )
                    !kompilyator.serverden ->
                        stringResource(R.string.ayarlar_kompilyator_yoxlanmayib)
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
            Bolum(stringResource(R.string.ayarlar_mexfilik)) {
                // Play «Data safety» bəyanatı ilə eyni mətn (PRIVACY.md).
                Text(
                    text = stringResource(R.string.ayarlar_mexfilik_izah),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = c.textDim
                )
                Spacer(Modifier.height(10.dp))
                Kecid(
                    ust = stringResource(R.string.ayarlar_senad),
                    etiket = stringResource(R.string.ayarlar_mexfilik_siyaseti)
                ) {
                    kecidAc(context, MEXFILIK_URL, brauzerYoxdur, bildir)
                }
                Spacer(Modifier.height(7.dp))
                Kecid(
                    ust = stringResource(R.string.ayarlar_xeta_bildir_ust),
                    etiket = stringResource(R.string.ayarlar_xeta_bildir)
                ) {
                    kecidAc(context, xetaBildirisiUrl(cihazMelumati(dil, mezmunDili, kompilyator)), brauzerYoxdur, bildir)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.ayarlar_xeta_bildir_izah),
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                    color = c.textFaint
                )
            }
        }

        item {
            Bolum(stringResource(R.string.ayarlar_menbe)) {
                Text(
                    text = stringResource(R.string.ayarlar_menbe_izah),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = c.textDim
                )
                Spacer(Modifier.height(10.dp))
                Kecid(ust = "GitHub", etiket = "github.com/Kea335") {
                    kecidAc(context, GITHUB, brauzerYoxdur, bildir)
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    text = stringResource(R.string.ayarlar_versiya, BuildConfig.VERSION_NAME),
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
                R.string.ayarlar_tesdiq_oxuma_basliq,
                R.string.ayarlar_tesdiq_oxuma_metn,
                onOxumaSifirla
            )
            "calisma" -> Triple(
                R.string.ayarlar_tesdiq_calisma_basliq,
                R.string.ayarlar_tesdiq_calisma_metn,
                onCalismaSifirla
            )
            else -> Triple(
                R.string.ayarlar_tesdiq_hamisi_basliq,
                R.string.ayarlar_tesdiq_hamisi_metn,
                onHamisiSifirla
            )
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { tesdiq = null },
            title = { Text(stringResource(basliq)) },
            text = { Text(stringResource(metn)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    emel()
                    tesdiq = null
                }) { Text(stringResource(R.string.ortaq_sifirla), color = c.err) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { tesdiq = null }) {
                    Text(stringResource(R.string.ayarlar_imtina))
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
    /** Sıraya bərabər bölünmür, öz eninə yığılır (dil çipləri). */
    yigcam: Boolean = false,
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
            .padding(horizontal = if (yigcam) 12.dp else 0.dp, vertical = 9.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (aktiv) c.accent else c.textDim,
        fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center
    )
}

/** Brauzerdə açılan keçid sətri. */
@Composable
private fun Kecid(ust: String, etiket: String, onClick: () -> Unit) {
    val c = KAz.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.bgSunken)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = ust,
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint
            )
            Text(
                text = etiket,
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
}

/** Ünvanı brauzerdə açır; brauzer tapılmasa çökmək əvəzinə xəbər verir. */
private fun kecidAc(context: Context, url: String, brauzerYoxdur: String, bildir: (String) -> Unit) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        bildir(brauzerYoxdur)
    }
}

/** «Xəta bildir» hesabatı üçün cihaz məlumatı. */
private fun cihazMelumati(dil: AppDil?, mezmunDili: String, kompilyator: KompilyatorVeziyyeti) = CihazMelumati(
    tetbiq = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    android = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    cihaz = cihazAdi(Build.MANUFACTURER, Build.MODEL),
    dil = "${dil?.teq ?: "sistem"} → $mezmunDili",
    kompilyator = kompilyator.versiya
)

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
