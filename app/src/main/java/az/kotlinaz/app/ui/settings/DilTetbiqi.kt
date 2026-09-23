package az.kotlinaz.app.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import az.kotlinaz.app.data.AppDil

/**
 * Seçilmiş dili tətbiqin UI resurslarına tətbiq edir (T3.3) — AppCompat-ın
 * per-app locale API-si. `null` = sistem dili (boş siyahı).
 *
 * Android 13+-da seçimi sistem özü saxlayır və Tənzimləmələr → Dillər
 * menyusunda göstərir; 13-dən aşağıda AppCompat `autoStoreLocales` ilə
 * (manifestdəki `AppLocalesMetadataHolderService`) öz yaddaşında saxlayır və
 * ilk `AppCompatActivity` yarananda tətbiq edir. Hər iki halda Activity
 * yenidən yaranır — Compose `stringResource`-ları yeni dildə oxunur.
 *
 * Ayrıca funksiyadır ki, [SettingsViewModel] unit testdə Android çərçivəsinə
 * toxunmasın — testdə yerinə boş lambda ötürülür.
 */
fun appCompatDiliTetbiqEt(dil: AppDil?) {
    AppCompatDelegate.setApplicationLocales(
        if (dil == null) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(dil.teq)
    )
}

/**
 * Sistemin hazırda tətbiq etdiyi dil — [AppDil.yaxin] ilə modelə çevrilir.
 * Boş siyahı (sistem dili) → null. 13+-da istifadəçi dili sistem
 * tənzimləmələrindən dəyişə bilər; `MainActivity` bunu DataStore ilə uzlaşdırır.
 */
fun appCompatCariDil(): AppDil? =
    AppDil.yaxin(AppCompatDelegate.getApplicationLocales().toLanguageTags().split(',').firstOrNull())
