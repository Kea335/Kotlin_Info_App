package az.kotlinaz.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Yaddaşda işləyən `DataStore<Preferences>` — testdə [UserPrefs] üçün.
 * Disk yoxdur, ona görə `preferencesDataStore` delegatına və `Context`-ə
 * ehtiyac qalmır; yazılanlar dərhal `data` axınında görünür.
 */
class YaddasDataStore(baslangic: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val veziyyet = MutableStateFlow(baslangic)

    override val data: Flow<Preferences> get() = veziyyet

    /** Neçə dəfə yazılıb — «yalnız bir dəfə yazılır» testləri üçün. */
    var yazmaSayi = 0
        private set

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        yazmaSayi++
        val yeni = transform(veziyyet.value)
        veziyyet.value = yeni
        return yeni
    }
}
