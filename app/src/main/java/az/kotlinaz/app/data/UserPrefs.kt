package az.kotlinaz.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Bütün tətbiq üçün TƏK DataStore nüsxəsi. `by preferencesDataStore` genişlənmə
// xassəsi olduğu üçün fayl səviyyəsində elan olunmalıdır — eyni adla ikinci dəfə
// yaradılsa çalışma vaxtında istisna atılır.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kotlinaz")

/** Tərəqqi və tənzimləmələr — hamısı cihazda saxlanılır. */
class UserPrefs(private val context: Context) {

    // Disk açarları. Adlar dəyişsə istifadəçinin tərəqqisi itər — sabit qalmalıdır.
    private object Acar {
        val TEMA = stringPreferencesKey("tema")
        /** Oxunmuş bölmə id-ləri. */
        val OXUNAN = stringSetPreferencesKey("oxunan_bolmeler")
        /** Həll edilmiş çalışma id-ləri (nəzəri və praktiki birlikdə). */
        val HELL = stringSetPreferencesKey("hell_edilmis")
        /** Köhnə vahid rekord açarı — yalnız sıfırlamada təmizlənir. */
        val KOHNE_QUIZ_REKORD = intPreferencesKey("quiz_rekord")
        /** «Davam et» kartının göstərdiyi son bölmə. */
        val SON_BOLME = stringPreferencesKey("son_bolme")
        val SRIFT = intPreferencesKey("srift_olcusu")
        /** Redaktorda söz tamamlama zolağı. */
        val TAMAMLAMA = booleanPreferencesKey("kod_tamamlama")
        /**
         * Bilik testinin rejim üzrə rekordları: `"junior=12;middle=9"` formatı.
         * Ayrı-ayrı açar əvəzinə tək sətir saxlanılır — rejim əlavə olunanda
         * disk sxemi dəyişmir.
         */
        val QUIZ_REKORDLARI = stringPreferencesKey("quiz_rekordlari")
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

    /** Rejim id-si → rekord. Açar yoxdursa boş xəritə. */
    val quizRekordlari: Flow<Map<String, Int>> = context.dataStore.data.map {
        xeriteAc(it[Acar.QUIZ_REKORDLARI])
    }

    val sonBolme: Flow<String?> = context.dataStore.data.map {
        it[Acar.SON_BOLME]
    }

    /** Yazı ölçüsü — 0 kiçik, 1 normal, 2 böyük. Bütün mətnə təsir edir. */
    val sriftOlcusu: Flow<Int> = context.dataStore.data.map {
        it[Acar.SRIFT] ?: 1
    }

    /** Redaktorda söz tamamlama — defolt açıqdır. */
    val kodTamamlama: Flow<Boolean> = context.dataStore.data.map {
        it[Acar.TAMAMLAMA] ?: true
    }

    suspend fun temaSec(mode: ThemeMode) {
        context.dataStore.edit { it[Acar.TEMA] = mode.id }
    }

    suspend fun sriftSec(olcu: Int) {
        // coerceIn: diapazondan kənar dəyər yazılsa MainActivity-dəki `when`
        // onu «normal» kimi qəbul edərdi — mənbədə saxlamaq daha təmizdir.
        context.dataStore.edit { it[Acar.SRIFT] = olcu.coerceIn(0, 2) }
    }

    suspend fun tamamlamaSec(acıq: Boolean) {
        context.dataStore.edit { it[Acar.TAMAMLAMA] = acıq }
    }

    suspend fun bolmeniOxunmusIsaretle(id: String) {
        // Tək `edit` blokunda iki açar — atomik yazılır.
        context.dataStore.edit { p ->
            p[Acar.OXUNAN] = (p[Acar.OXUNAN] ?: emptySet()) + id
            p[Acar.SON_BOLME] = id
        }
    }

    /** Dərs açılanda çağırılır — bölmə oxunmuş sayılmasa da mövqe yadda qalır. */
    suspend fun sonBolmeniYaz(id: String) {
        context.dataStore.edit { it[Acar.SON_BOLME] = id }
    }

    suspend fun calismaniHellIsaretle(id: String) {
        context.dataStore.edit { p ->
            p[Acar.HELL] = (p[Acar.HELL] ?: emptySet()) + id
        }
    }

    /** Rejimin rekordu yalnız köhnəni ötəndə yenilənir. */
    suspend fun quizNeticesiniYaz(rejim: String, bal: Int) {
        context.dataStore.edit { p ->
            val xerite = xeriteAc(p[Acar.QUIZ_REKORDLARI])
            if (bal > (xerite[rejim] ?: 0)) {
                p[Acar.QUIZ_REKORDLARI] = xeriteYaz(xerite + (rejim to bal))
            }
        }
    }

    suspend fun oxumaTereqqisiniSifirla() {
        context.dataStore.edit { it.remove(Acar.OXUNAN) }
    }

    suspend fun calismaTereqqisiniSifirla() {
        context.dataStore.edit { it.remove(Acar.HELL) }
    }

    /** Tərəqqinin hamısı silinir; tema və şrift ölçüsü toxunulmadan qalır. */
    suspend fun hamisiniSifirla() {
        context.dataStore.edit { p ->
            p.remove(Acar.OXUNAN)
            p.remove(Acar.HELL)
            p.remove(Acar.KOHNE_QUIZ_REKORD)
            p.remove(Acar.QUIZ_REKORDLARI)
            p.remove(Acar.SON_BOLME)
        }
    }
}

/* ---------- «açar=dəyər;…» sətir formatı ---------- */

private fun xeriteAc(xam: String?): Map<String, Int> {
    if (xam.isNullOrBlank()) return emptyMap()
    // Zədəli sətir (əl ilə redaktə, yarımçıq yazı) tətbiqi çökdürməməlidir —
    // oxunmayan cütlər sadəcə atılır.
    return xam.split(';').mapNotNull { cut ->
        val i = cut.indexOf('=')
        if (i <= 0) return@mapNotNull null
        val bal = cut.substring(i + 1).toIntOrNull() ?: return@mapNotNull null
        cut.substring(0, i) to bal
    }.toMap()
}

private fun xeriteYaz(xerite: Map<String, Int>): String =
    xerite.entries.joinToString(";") { "${it.key}=${it.value}" }

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "Sistem"),
    LIGHT("light", "İşıqlı"),
    DARK("dark", "Qaranlıq");

    companion object {
        /** Açar yoxdursa (ilk açılış) və ya naməlumdursa — sistem rejimi. */
        fun from(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
