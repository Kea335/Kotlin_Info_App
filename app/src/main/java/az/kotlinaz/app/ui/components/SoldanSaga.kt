package az.kotlinaz.app.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Kod, konsol çıxışı və redaktor RTL dillərdə (ərəb, fars) də soldan-sağa
 * qalır (T3.6) — yoxsa mötərizələr, operatorlar və girinti tərs görünür.
 *
 * İki şey məcburi LTR olur: layout istiqaməti (sürüşmə soldan başlayır,
 * elementlər soldan düzülür) və mətn istiqaməti — ərəbcə şərhlə başlayan
 * kod sətri də RTL abzasa çevrilmir. `Text` stili `LocalTextStyle`-dan alır;
 * `BasicTextField` almır, ona öz `textStyle`-ında `TextDirection.Ltr` verilir.
 */
@Composable
fun SoldanSaga(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalTextStyle provides LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
        content = content
    )
}

/**
 * Mətn daxilindəki kod parçasını (`val x = 5`) ətrafdakı RTL mətndən təcrid
 * edir: LRI … PDI (Unicode BiDi isolate). Onsuz ərəb cümləsinin içindəki
 * `x = 5` hissələri yer dəyişir. Görünməyən simvollardır; LTR dillərdə
 * lazım deyil, ona görə yalnız [rtl] olanda əlavə olunur.
 */
fun ltrTecrid(metn: String, rtl: Boolean): String =
    if (rtl) "\u2066$metn\u2069" else metn
