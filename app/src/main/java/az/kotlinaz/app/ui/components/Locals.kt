package az.kotlinaz.app.ui.components

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** Qısa bildiriş göstərmək üçün — saytdakı `toast()` funksiyasının qarşılığı. */
val LocalBildiris: ProvidableCompositionLocal<(String) -> Unit> =
    staticCompositionLocalOf { {} }

/** Bölmə keçidlərini (`#null-safety`) idarə edir. */
val LocalBolmeyeKec: ProvidableCompositionLocal<(String) -> Unit> =
    staticCompositionLocalOf { {} }

/** Kodu online kod meydanına göndərir. */
val LocalMeydanaGonder: ProvidableCompositionLocal<(String) -> Unit> =
    staticCompositionLocalOf { {} }
