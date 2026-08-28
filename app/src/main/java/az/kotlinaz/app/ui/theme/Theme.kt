package az.kotlinaz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.data.model.Level

/* ============================================================
   Rənglər saytın css/style.css faylından birbaşa götürülüb.
   ============================================================ */

val KPurple = Color(0xFF7F52FF)
val KMagenta = Color(0xFFC711E1)
val KOrange = Color(0xFFE44857)

/**
 * Saytdakı `--k-grad` qradienti — Kotlin loqosunun üç rəngi.
 * `get()` ilə hər müraciətdə yenidən qurulur, çünki `linearGradient` ölçüyə
 * uyğunlaşan Brush qaytarır və onu qlobal `val` kimi paylaşmaq düzgün deyil.
 */
val KotlinBrush: Brush
    get() = Brush.linearGradient(listOf(KPurple, KMagenta, KOrange))

/**
 * Tətbiqin öz rəng dəsti. Material3-ün `ColorScheme`-i saytın palitrasını
 * tam ifadə etmir (sintaksis rəngləri, iki fərqli «batıq» fon və s.),
 * ona görə paralel dəst saxlanılır və `KAz.colors` ilə oxunur.
 *
 * `@Immutable` Compose-a dəyişməzliyə söz verir — rənglər dəyişmədikcə
 * bu dəsti oxuyan composable-lar yenidən qurulmur.
 */
@Immutable
data class KotlinAzColors(
    val bg: Color,
    val bgElev: Color,
    val bgSunken: Color,
    val bgCode: Color,
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val border: Color,
    val borderStrong: Color,
    val accent: Color,
    val accentSoft: Color,
    val ok: Color,
    val warn: Color,
    val err: Color,
    // Sintaksis
    val synKey: Color,
    val synStr: Color,
    val synNum: Color,
    val synCom: Color,
    val synFn: Color,
    val synType: Color,
    val synAnn: Color,
    val synPunc: Color
)

private val AciqRengler = KotlinAzColors(
    bg = Color(0xFFF7F7FB),
    bgElev = Color(0xFFFFFFFF),
    bgSunken = Color(0xFFEFEFF6),
    bgCode = Color(0xFFF3F1FA),
    text = Color(0xFF16161D),
    textDim = Color(0xFF5A5A6E),
    textFaint = Color(0xFF8A8A9E),
    border = Color(0xFFE2E2EC),
    borderStrong = Color(0xFFCFCFDE),
    accent = Color(0xFF6B3FE8),
    accentSoft = Color(0x1A7F52FF),
    ok = Color(0xFF178A5A),
    warn = Color(0xFFB26A00),
    err = Color(0xFFC7373F),
    synKey = Color(0xFF9C27B0),
    synStr = Color(0xFF1E7A4C),
    synNum = Color(0xFFB45309),
    synCom = Color(0xFF8A8A9E),
    synFn = Color(0xFF2563C9),
    synType = Color(0xFFC2410C),
    synAnn = Color(0xFF857A00),
    synPunc = Color(0xFF55556A)
)

private val QaranliqRengler = KotlinAzColors(
    bg = Color(0xFF0B0B12),
    bgElev = Color(0xFF14141F),
    bgSunken = Color(0xFF101019),
    bgCode = Color(0xFF16161F),
    text = Color(0xFFECECF4),
    textDim = Color(0xFFA2A2B8),
    textFaint = Color(0xFF6E6E88),
    border = Color(0xFF26263A),
    borderStrong = Color(0xFF35354E),
    accent = Color(0xFFA78BFA),
    accentSoft = Color(0x24A78BFA),
    ok = Color(0xFF4ADE80),
    warn = Color(0xFFFBBF24),
    err = Color(0xFFFB7185),
    synKey = Color(0xFFC792EA),
    synStr = Color(0xFF7EE787),
    synNum = Color(0xFFF7A76C),
    synCom = Color(0xFF6E6E88),
    synFn = Color(0xFF79C0FF),
    synType = Color(0xFFFFB86C),
    synAnn = Color(0xFFE3D26F),
    synPunc = Color(0xFF9A9AB4)
)

// `staticCompositionLocalOf` — dəyər nadir hallarda dəyişir (yalnız tema
// dəyişəndə). Belə halda `compositionLocalOf`-dan fərqli olaraq oxu yerləri
// izlənmir, əvəzində bütün alt ağac yenidən qurulur: bu daha ucuzdur.
val LocalKotlinAzColors: ProvidableCompositionLocal<KotlinAzColors> =
    staticCompositionLocalOf { AciqRengler }

/**
 * Yazı ölçüsü katsayısı — istifadəçi tənzimləməsi («Yazı şrifti»).
 * Həm adi mətnə (tipoqrafiya), həm də kod bloklarına eyni nisbətdə təsir edir.
 */
val LocalCodeScale: ProvidableCompositionLocal<Float> = staticCompositionLocalOf { 1f }

/** Redaktorda söz tamamlama zolağı açıqdırmı — tənzimləmələrdən idarə olunur. */
val LocalKodTamamlama: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }

/** Qısa müraciət: `KAz.colors.accent`, `KAz.codeScale`. */
object KAz {
    val colors: KotlinAzColors
        @Composable @ReadOnlyComposable get() = LocalKotlinAzColors.current

    val codeScale: Float
        @Composable @ReadOnlyComposable get() = LocalCodeScale.current
}

/**
 * Səviyyə nişanlarının rəngi — çalışmalarda, dərslərdə, testdə və
 * tənzimləmələrdə eyni olsun deyə tək yerdə saxlanılır.
 */
@Composable
@ReadOnlyComposable
fun seviyyeRengi(lv: Level): Color = when (lv) {
    Level.JUNIOR -> KAz.colors.ok
    Level.MIDDLE -> KAz.colors.warn
    Level.SENIOR -> KAz.colors.err
}

// Material3 komponentləri (AlertDialog, Switch, Snackbar, NavigationBar…)
// öz sxemindən rəng götürür — onu da eyni palitradan doldururuq ki,
// hazır komponentlər saytın rəngləri ilə uyuşsun.
private fun m3Light(c: KotlinAzColors) = lightColorScheme(
    primary = c.accent,
    onPrimary = Color.White,
    primaryContainer = c.accentSoft,
    onPrimaryContainer = c.accent,
    secondary = KMagenta,
    onSecondary = Color.White,
    background = c.bg,
    onBackground = c.text,
    surface = c.bgElev,
    onSurface = c.text,
    surfaceVariant = c.bgSunken,
    onSurfaceVariant = c.textDim,
    outline = c.border,
    outlineVariant = c.border,
    error = c.err,
    onError = Color.White
)

private fun m3Dark(c: KotlinAzColors) = darkColorScheme(
    primary = c.accent,
    onPrimary = Color(0xFF1B1030),
    primaryContainer = c.accentSoft,
    onPrimaryContainer = c.accent,
    secondary = KMagenta,
    onSecondary = Color.White,
    background = c.bg,
    onBackground = c.text,
    surface = c.bgElev,
    onSurface = c.text,
    surfaceVariant = c.bgSunken,
    onSurfaceVariant = c.textDim,
    outline = c.border,
    outlineVariant = c.border,
    error = c.err,
    onError = Color(0xFF3B0A12)
)

/**
 * Tipoqrafiya ölçü katsayısı ilə qurulur.
 *
 * DÜZƏLİŞ: əvvəllər «şrift ölçüsü» tənzimləməsi YALNIZ kod bloklarına təsir
 * edirdi — düymələrə basanda ekranda demək olar heç nə dəyişmirdi. İndi
 * katsayı bütün mətn üslublarına vurulur, ona görə seçim bütün tətbiqdə
 * görünür.
 */
private fun tipoqrafiya(k: Float): Typography {
    val sans = FontFamily.SansSerif
    fun st(olcu: Float, setir: Float, ceki: FontWeight? = null) = TextStyle(
        fontFamily = sans,
        fontWeight = ceki,
        fontSize = (olcu * k).sp,
        lineHeight = (setir * k).sp
    )
    return Typography(
        displaySmall = st(30f, 38f, FontWeight.ExtraBold),
        headlineMedium = st(24f, 32f, FontWeight.Bold),
        headlineSmall = st(20f, 28f, FontWeight.Bold),
        titleLarge = st(18f, 25f, FontWeight.Bold),
        titleMedium = st(16f, 22f, FontWeight.SemiBold),
        titleSmall = st(14f, 20f, FontWeight.SemiBold),
        bodyLarge = st(16f, 26f),
        bodyMedium = st(14.5f, 23f),
        bodySmall = st(13f, 19f),
        labelLarge = st(14f, 20f, FontWeight.SemiBold),
        labelMedium = st(12.5f, 17f, FontWeight.Medium),
        labelSmall = st(11f, 15f, FontWeight.Medium)
    )
}

@Composable
fun KotlinAzTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    /** «Yazı şrifti» tənzimləməsi: 0.88 kiçik, 1.0 normal, 1.15 böyük. */
    yaziOlcusu: Float = 1f,
    kodTamamlama: Boolean = true,
    content: @Composable () -> Unit
) {
    // «Sistem» seçilibsə cihazın rejimi izlənir; digər hallarda istifadəçinin
    // seçimi cihaz tənzimləməsini üstələyir.
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val rengler = if (dark) QaranliqRengler else AciqRengler
    // Ölçü nadir hallarda dəyişir — hər yenidən qurulmada 12 TextStyle
    // yaratmağın mənası yoxdur.
    val tipo = remember(yaziOlcusu) { tipoqrafiya(yaziOlcusu) }

    CompositionLocalProvider(
        LocalKotlinAzColors provides rengler,
        LocalCodeScale provides yaziOlcusu,
        LocalKodTamamlama provides kodTamamlama
    ) {
        MaterialTheme(
            colorScheme = if (dark) m3Dark(rengler) else m3Light(rengler),
            typography = tipo,
            content = content
        )
    }
}
