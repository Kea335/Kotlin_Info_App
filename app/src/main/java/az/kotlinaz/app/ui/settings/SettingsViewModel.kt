package az.kotlinaz.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.AppDil
import az.kotlinaz.app.data.AssetContentRepository
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.gorunenDilleriSec
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tənzimləmələr (T2.5): tema, dil, şrift, tamamlama, rekordlar və sıfırlamalar.
 * Hamısı DataStore-dadır — bu ViewModel yalnız oxuyub yazır.
 *
 * `MainActivity` də tema/şrift/tamamlama üçün bunu işlədir (Activity-yə
 * bağlı nüsxə); tənzimləmə ekranı naviqasiya girişinə bağlı öz nüsxəsini
 * alır — ikisi eyni DataStore axınını oxuduğundan fərq görünmür.
 *
 * Dil seçimi (T3.3) iki yerə yazılır: DataStore (məzmun qovluğu və bu
 * ekranın seçimi üçün) və [dilTetbiqEt] (AppCompat — UI resursları üçün).
 */
class SettingsViewModel(
    private val prefs: UserPrefs,
    /** Seçimi sistemə tətbiq edir (`appCompatDiliTetbiqEt`); testdə boşdur. */
    private val dilTetbiqEt: (AppDil?) -> Unit = {},
    /** Qurulmuş məzmun dilləri — `ContentRepository.diller` (T4.7). */
    private val movcudDiller: suspend () -> List<String> = { listOf(AssetContentRepository.BAZA_LOCALE) },
    /** Debug build-də bütün dillər görünür — `BuildConfig.DEBUG`. */
    private val hamisiGorunsun: Boolean = false
) : ViewModel() {

    val tema: StateFlow<ThemeMode> =
        prefs.tema.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** Seçilmiş dil; null = «Sistem dili». */
    val dil: StateFlow<AppDil?> =
        prefs.dil.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Seçim siyahısındakı dillər (T4.7): release-də yalnız tam tərcümə
     * olunmuşlar, debug-da hamısı. Siyahı oxunmasa baza ilə davam edilir —
     * dil bölməsi boş qalmamalıdır.
     */
    val gorunenDiller: StateFlow<List<AppDil>> = combine(
        flow {
            emit(
                try {
                    movcudDiller()
                } catch (_: Exception) {
                    listOf(AssetContentRepository.BAZA_LOCALE)
                }
            )
        },
        prefs.dil
    ) { movcud, secim -> gorunenDilleriSec(movcud, secim, hamisiGorunsun) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            gorunenDilleriSec(listOf(AssetContentRepository.BAZA_LOCALE), null, hamisiGorunsun)
        )

    /** Yazı ölçüsü — 0 kiçik, 1 normal, 2 böyük. */
    val sriftOlcusu: StateFlow<Int> =
        prefs.sriftOlcusu.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val kodTamamlama: StateFlow<Boolean> =
        prefs.kodTamamlama.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Bilik testinin rejim üzrə rekordları: `"junior"` → bal. */
    val quizRekordlari: StateFlow<Map<String, Int>> =
        prefs.quizRekordlari.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun temaSec(mode: ThemeMode) = viewModelScope.launch { prefs.temaSec(mode) }

    /**
     * Dil seçimi. Əvvəl DataStore — `AppViewModel` məzmunu ondan yenidən
     * yükləyir; sonra AppCompat — Activity yenidən yaranır. Sıra vacibdir:
     * yenidən yaranan Activity DataStore-da artıq yeni dili görməlidir.
     */
    fun dilSec(dil: AppDil?) = viewModelScope.launch {
        prefs.dilSec(dil)
        dilTetbiqEt(dil)
    }

    /**
     * Android 13+-da istifadəçi dili sistem tənzimləmələrindən də dəyişə bilər —
     * onda AppCompat-ın gördüyü dil DataStore-dakından fərqlənir. Activity hər
     * yarananda çağırılır: fərq varsa sistemdəki əsas götürülür, DataStore
     * ona uyğunlaşdırılır (məzmun qovluğu da ardınca dəyişir).
     */
    fun sistemleUzlasdir(sistemdeki: AppDil?) = viewModelScope.launch {
        if (prefs.dil.first() != sistemdeki) prefs.dilSec(sistemdeki)
    }

    fun sriftSec(olcu: Int) = viewModelScope.launch { prefs.sriftSec(olcu) }

    fun tamamlamaSec(acıq: Boolean) = viewModelScope.launch { prefs.tamamlamaSec(acıq) }

    fun oxumaTereqqisiniSifirla() = viewModelScope.launch { prefs.oxumaTereqqisiniSifirla() }

    fun calismaTereqqisiniSifirla() = viewModelScope.launch { prefs.calismaTereqqisiniSifirla() }

    fun hamisiniSifirla() = viewModelScope.launch { prefs.hamisiniSifirla() }
}
