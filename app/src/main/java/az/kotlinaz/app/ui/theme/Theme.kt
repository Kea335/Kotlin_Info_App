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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.data.ThemeMode

/* ============================================================
   Rənglər saytın css/style.css faylından birbaşa götürülüb.
   ============================================================ */

val KPurple = Color(0xFF7F52FF)
val KMagenta = Color(0xFFC711E1)
val KOrange = Color(0xFFE44857)

/** Saytdakı `--k-grad` qradienti. */
val KotlinBrush: Brush
    get() = Brush.linearGradient(listOf(KPurple, KMagenta, KOrange))

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

val LocalKotlinAzColors: ProvidableCompositionLocal<KotlinAzColors> =
    staticCompositionLocalOf { AciqRengler }

/** Kod bloklarının şrift ölçüsü — istifadəçi tənzimləməsi. */
val LocalCodeScale: ProvidableCompositionLocal<Float> = staticCompositionLocalOf { 1f }

object KAz {
    val colors: KotlinAzColors
        @Composable @ReadOnlyComposable get() = LocalKotlinAzColors.current

    val codeScale: Float
        @Composable @ReadOnlyComposable get() = LocalCodeScale.current
}

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

private val Tipoqrafiya = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.5.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun KotlinAzTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    codeScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val rengler = if (dark) QaranliqRengler else AciqRengler

    CompositionLocalProvider(
        LocalKotlinAzColors provides rengler,
        LocalCodeScale provides codeScale
    ) {
        MaterialTheme(
            colorScheme = if (dark) m3Dark(rengler) else m3Light(rengler),
            typography = Tipoqrafiya,
            content = content
        )
    }
}
