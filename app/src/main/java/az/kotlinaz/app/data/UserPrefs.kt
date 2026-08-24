package az.kotlinaz.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kotlinaz")

/** Tərəqqi və tənzimləmələr — hamısı cihazda saxlanılır. */
class UserPrefs(private val context: Context) {

    private object Acar {
        val TEMA = stringPreferencesKey("tema")
        val OXUNAN = stringSetPreferencesKey("oxunan_bolmeler")
        val HELL = stringSetPreferencesKey("hell_edilmis")
        val QUIZ_REKORD = intPreferencesKey("quiz_rekord")
        val SON_BOLME = stringPreferencesKey("son_bolme")
        val SRIFT = intPreferencesKey("srift_olcusu")
    }

    val tema: Flow<ThemeMode> = context.dataStore.data.map {
        ThemeMode.from(it[Acar.TEMA])
    }

    val oxunanBolmeler: Flow<Set<String>> = context.dataStore.data.map {
        it[Acar.OXUNAN] ?: emptySet()
    }

    val hellEdilmis: Flow<Set<String>> = context.dataStore.data.map {
        it[Acar.HELL] ?: emptySet()
    }

    val quizRekord: Flow<Int> = context.dataStore.data.map {
        it[Acar.QUIZ_REKORD] ?: 0
    }

    val sonBolme: Flow<String?> = context.dataStore.data.map {
        it[Acar.SON_BOLME]
    }

    /** Kod bloklarının şrift ölçüsü — 0 kiçik, 1 normal, 2 böyük. */
    val sriftOlcusu: Flow<Int> = context.dataStore.data.map {
        it[Acar.SRIFT] ?: 1
    }

    suspend fun temaSec(mode: ThemeMode) {
        context.dataStore.edit { it[Acar.TEMA] = mode.id }
    }

    suspend fun sriftSec(olcu: Int) {
        context.dataStore.edit { it[Acar.SRIFT] = olcu.coerceIn(0, 2) }
    }

    suspend fun bolmeniOxunmusIsaretle(id: String) {
        context.dataStore.edit { p ->
            p[Acar.OXUNAN] = (p[Acar.OXUNAN] ?: emptySet()) + id
            p[Acar.SON_BOLME] = id
        }
    }

    suspend fun sonBolmeniYaz(id: String) {
        context.dataStore.edit { it[Acar.SON_BOLME] = id }
    }

    suspend fun calismaniHellIsaretle(id: String) {
        context.dataStore.edit { p ->
            p[Acar.HELL] = (p[Acar.HELL] ?: emptySet()) + id
        }
    }

    suspend fun quizNeticesiniYaz(bal: Int) {
        context.dataStore.edit { p ->
            val kohne = p[Acar.QUIZ_REKORD] ?: 0
            if (bal > kohne) p[Acar.QUIZ_REKORD] = bal
        }
    }

    suspend fun oxumaTereqqisiniSifirla() {
        context.dataStore.edit { it.remove(Acar.OXUNAN) }
    }

    suspend fun calismaTereqqisiniSifirla() {
        context.dataStore.edit { it.remove(Acar.HELL) }
    }

    suspend fun hamisiniSifirla() {
        context.dataStore.edit { p ->
            p.remove(Acar.OXUNAN)
            p.remove(Acar.HELL)
            p.remove(Acar.QUIZ_REKORD)
            p.remove(Acar.SON_BOLME)
        }
    }
}

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "Sistem"),
    LIGHT("light", "İşıqlı"),
    DARK("dark", "Qaranlıq");

    companion object {
        fun from(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
