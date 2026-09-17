package az.kotlinaz.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.data.UserPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tənzimləmələr (T2.5): tema, şrift, tamamlama, rekordlar və sıfırlamalar.
 * Hamısı DataStore-dadır — bu ViewModel yalnız oxuyub yazır.
 *
 * `MainActivity` də tema/şrift/tamamlama üçün bunu işlədir (Activity-yə
 * bağlı nüsxə); tənzimləmə ekranı naviqasiya girişinə bağlı öz nüsxəsini
 * alır — ikisi eyni DataStore axınını oxuduğundan fərq görünmür.
 */
class SettingsViewModel(private val prefs: UserPrefs) : ViewModel() {

    val tema: StateFlow<ThemeMode> =
        prefs.tema.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** Yazı ölçüsü — 0 kiçik, 1 normal, 2 böyük. */
    val sriftOlcusu: StateFlow<Int> =
        prefs.sriftOlcusu.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val kodTamamlama: StateFlow<Boolean> =
        prefs.kodTamamlama.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Bilik testinin rejim üzrə rekordları: `"junior"` → bal. */
    val quizRekordlari: StateFlow<Map<String, Int>> =
        prefs.quizRekordlari.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun temaSec(mode: ThemeMode) = viewModelScope.launch { prefs.temaSec(mode) }

    fun sriftSec(olcu: Int) = viewModelScope.launch { prefs.sriftSec(olcu) }

    fun tamamlamaSec(acıq: Boolean) = viewModelScope.launch { prefs.tamamlamaSec(acıq) }

    fun oxumaTereqqisiniSifirla() = viewModelScope.launch { prefs.oxumaTereqqisiniSifirla() }

    fun calismaTereqqisiniSifirla() = viewModelScope.launch { prefs.calismaTereqqisiniSifirla() }

    fun hamisiniSifirla() = viewModelScope.launch { prefs.hamisiniSifirla() }
}
