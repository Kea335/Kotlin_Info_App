package az.kotlinaz.app.ui.components

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/*
   Bu üç funksiya blok renderinin ən dərin qatına lazımdır (məsələn cədvəl
   hücrəsindəki keçid, kart içindəki kod bloku). Onları hər composable-a
   parametr kimi ötürmək əvəzinə CompositionLocal ilə paylaşırıq —
   dəyərləri AppRoot təyin edir. Defolt boş funksiyadır: preview və testdə
   çökmə olmasın deyə.
*/

/** Qısa bildiriş göstərmək üçün — saytdakı `toast()` funksiyasının qarşılığı. */
val LocalBildiris: ProvidableCompositionLocal<(String) -> Unit> =
    staticCompositionLocalOf { {} }

/** Bölmə keçidlərini (`#null-safety`) idarə edir. */
val LocalBolmeyeKec: ProvidableCompositionLocal<(String) -> Unit> =
    staticCompositionLocalOf { {} }

/** Kodu online kod meydanına göndərir. */
val LocalMeydanaGonder: ProvidableCompositionLocal<(String) -> Unit> =
    staticCompositionLocalOf { {} }
