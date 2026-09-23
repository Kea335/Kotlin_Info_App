package az.kotlinaz.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Bütün tətbiq üçün TƏK DataStore nüsxəsi. `by preferencesDataStore` genişlənmə
// xassəsi olduğu üçün fayl səviyyəsində elan olunmalıdır — eyni adla ikinci dəfə
// yaradılsa çalışma vaxtında istisna atılır. Yalnız AppContainer oxuyur.
internal val Context.kotlinazDataStore: DataStore<Preferences> by preferencesDataStore(name = "kotlinaz")

/**
 * Bərpa oluna bilən keşlər (kompilyator versiyaları) — ayrıca faylda, çünki
 * yedəklənmir: `res/xml/backup_rules.xml` və `data_extraction_rules.xml` bu
 * faylı (`datastore/kotlinaz_kes.preferences_pb`) çıxarır. Tərəqqi və
 * tənzimləmələr isə yedəklənir — yeni telefonda itmir.
 */
internal val Context.kesDataStore: DataStore<Preferences> by preferencesDataStore(name = "kotlinaz_kes")

/**
 * Tərəqqi və tənzimləmələr — hamısı cihazda saxlanılır.
 *
 * `Context` yox, hazır [DataStore] alır: testdə yaddaşdakı fake store ilə
 * işləyir, tətbiqdə isə AppContainer [kotlinazDataStore]-u ötürür.
 */
class UserPrefs(
    private val dataStore: DataStore<Preferences>,
    /** Yedəklənməyən keşlər — tətbiqdə [kesDataStore]; testdə defolt olaraq eyni store. */
    private val kesStore: DataStore<Preferences> = dataStore
) {

    // Disk açarları. Adlar dəyişsə istifadəçinin tərəqqisi itər — sabit qalmalıdır.
    private object Acar {
        val TEMA = stringPreferencesKey("tema")
        /** Tətbiq dili — [AppDil.teq]; açar yoxdursa sistem dili (T3.3). */
        val DIL = stringPreferencesKey("dil")
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
        /**
         * Kompilyator versiyalarının keşi — `api.kotlinlang.org/versions`
         * cavabı, [KompilyatorVersiyalari.keseYaz] formatında. Tərəqqi deyil,
         * ona görə sıfırlamalarda toxunulmur. [kesStore]-dadır (yedəklənmir).
         */
        val KOMPILYATOR_VERSIYALARI = stringPreferencesKey("compiler_versions")
        /** Keşin yazıldığı an, epoch ms — 24 saatdan köhnədirsə yenilənir. */
        val KOMPILYATOR_VERSIYALARI_VAXTI = longPreferencesKey("compiler_versions_ts")
        /**
         * Kod meydanındakı son kod — tətbiq bağlanıb açılanda itməsin. Tərəqqi
         * deyil, sıfırlamalarda toxunulmur.
         */
        val MEYDAN_KODU = stringPreferencesKey("meydan_kodu")
        /** Həmin kodun başladığı nümunə; yoxdursa boş redaktordan. */
        val MEYDAN_PRESETI = stringPreferencesKey("meydan_preseti")
    }

    val tema: Flow<ThemeMode> = dataStore.data.map {
        ThemeMode.from(it[Acar.TEMA])
    }

    /**
     * Seçilmiş dil; null = «Sistem dili». Naməlum teq (məsələn dil siyahıdan
     * çıxarılıbsa) da sistem dili sayılır — tətbiq çökmür.
     */
    val dil: Flow<AppDil?> = dataStore.data.map {
        AppDil.from(it[Acar.DIL])
    }

    val oxunanBolmeler: Flow<Set<String>> = dataStore.data.map {
        it[Acar.OXUNAN] ?: emptySet()
    }

    val hellEdilmis: Flow<Set<String>> = dataStore.data.map {
        it[Acar.HELL] ?: emptySet()
    }

    /** Rejim id-si → rekord. Açar yoxdursa boş xəritə. */
    val quizRekordlari: Flow<Map<String, Int>> = dataStore.data.map {
        xeriteAc(it[Acar.QUIZ_REKORDLARI])
    }

    val sonBolme: Flow<String?> = dataStore.data.map {
        it[Acar.SON_BOLME]
    }

    /** Yazı ölçüsü — 0 kiçik, 1 normal, 2 böyük. Bütün mətnə təsir edir. */
    val sriftOlcusu: Flow<Int> = dataStore.data.map {
        it[Acar.SRIFT] ?: 1
    }

    /** Redaktorda söz tamamlama — defolt açıqdır. */
    val kodTamamlama: Flow<Boolean> = dataStore.data.map {
        it[Acar.TAMAMLAMA] ?: true
    }

    suspend fun temaSec(mode: ThemeMode) {
        dataStore.edit { it[Acar.TEMA] = mode.id }
    }

    /** null = sistem dili: açar silinir ki, «sistem» ilə «az» seçimi qarışmasın. */
    suspend fun dilSec(dil: AppDil?) {
        dataStore.edit {
            if (dil == null) it.remove(Acar.DIL) else it[Acar.DIL] = dil.teq
        }
    }

    suspend fun sriftSec(olcu: Int) {
        // coerceIn: diapazondan kənar dəyər yazılsa MainActivity-dəki `when`
        // onu «normal» kimi qəbul edərdi — mənbədə saxlamaq daha təmizdir.
        dataStore.edit { it[Acar.SRIFT] = olcu.coerceIn(0, 2) }
    }

    suspend fun tamamlamaSec(acıq: Boolean) {
        dataStore.edit { it[Acar.TAMAMLAMA] = acıq }
    }

    suspend fun bolmeniOxunmusIsaretle(id: String) {
        // Tək `edit` blokunda iki açar — atomik yazılır.
        dataStore.edit { p ->
            p[Acar.OXUNAN] = (p[Acar.OXUNAN] ?: emptySet()) + id
            p[Acar.SON_BOLME] = id
        }
    }

    /** Dərs açılanda çağırılır — bölmə oxunmuş sayılmasa da mövqe yadda qalır. */
    suspend fun sonBolmeniYaz(id: String) {
        dataStore.edit { it[Acar.SON_BOLME] = id }
    }

    suspend fun calismaniHellIsaretle(id: String) {
        dataStore.edit { p ->
            p[Acar.HELL] = (p[Acar.HELL] ?: emptySet()) + id
        }
    }

    /** Rejimin rekordu yalnız köhnəni ötəndə yenilənir. */
    suspend fun quizNeticesiniYaz(rejim: String, bal: Int) {
        dataStore.edit { p ->
            val xerite = xeriteAc(p[Acar.QUIZ_REKORDLARI])
            if (bal > (xerite[rejim] ?: 0)) {
                p[Acar.QUIZ_REKORDLARI] = xeriteYaz(xerite + (rejim to bal))
            }
        }
    }

    suspend fun oxumaTereqqisiniSifirla() {
        dataStore.edit { it.remove(Acar.OXUNAN) }
    }

    suspend fun calismaTereqqisiniSifirla() {
        dataStore.edit { it.remove(Acar.HELL) }
    }

    /**
     * Kompilyator versiyalarının keşi — bir dəfəlik oxunuş, axın deyil:
     * [KotlinCompiler] onu yalnız sorğudan əvvəl soruşur. Açar yoxdursa və ya
     * sətir tam zədəlidirsə null; disk oxunmasa da null — keşsiz qalmaq
     * çökməkdən yaxşıdır.
     */
    suspend fun kompilyatorKesi(): KompilyatorKesi? = try {
        val p = kesStore.data.first()
        val versiyalar = KompilyatorVersiyalari.kesdenOxu(p[Acar.KOMPILYATOR_VERSIYALARI])
        val vaxt = p[Acar.KOMPILYATOR_VERSIYALARI_VAXTI]
        if (versiyalar.isEmpty() || vaxt == null) null else KompilyatorKesi(versiyalar, vaxt)
    } catch (_: Exception) {
        null
    }

    suspend fun kompilyatorKesiniYaz(versiyalar: List<KompilyatorVersiyasi>, vaxt: Long) {
        kesStore.edit { p ->
            p[Acar.KOMPILYATOR_VERSIYALARI] = KompilyatorVersiyalari.keseYaz(versiyalar)
            p[Acar.KOMPILYATOR_VERSIYALARI_VAXTI] = vaxt
        }
        // Köhnə versiyalar keşi əsas faylda saxlayırdı — ilk yeni yazıda
        // oradan silinir ki, yedəyə düşməsin.
        if (kesStore !== dataStore) {
            dataStore.edit { p ->
                p.remove(Acar.KOMPILYATOR_VERSIYALARI)
                p.remove(Acar.KOMPILYATOR_VERSIYALARI_VAXTI)
            }
        }
    }

    /** Meydanın saxlanmış kodu; yoxdursa və ya disk oxunmasa null. */
    suspend fun meydanQaralamasi(): MeydanQaralamasi? = try {
        val p = dataStore.data.first()
        p[Acar.MEYDAN_KODU]?.let { MeydanQaralamasi(it, p[Acar.MEYDAN_PRESETI]) }
    } catch (_: Exception) {
        null
    }

    suspend fun meydanQaralamasiniYaz(q: MeydanQaralamasi) {
        dataStore.edit { p ->
            p[Acar.MEYDAN_KODU] = q.kod
            if (q.preset == null) p.remove(Acar.MEYDAN_PRESETI) else p[Acar.MEYDAN_PRESETI] = q.preset
        }
    }

    /** Tərəqqinin hamısı silinir; tema, dil və şrift ölçüsü toxunulmadan qalır. */
    suspend fun hamisiniSifirla() {
        dataStore.edit { p ->
            p.remove(Acar.OXUNAN)
            p.remove(Acar.HELL)
            p.remove(Acar.KOHNE_QUIZ_REKORD)
            p.remove(Acar.QUIZ_REKORDLARI)
            p.remove(Acar.SON_BOLME)
        }
    }
}

/** Kod meydanının diskdəki halı: redaktordakı kod və seçili nümunə (null = boş redaktor). */
data class MeydanQaralamasi(val kod: String, val preset: String?)

/** Diskdəki kompilyator versiyaları və yazıldığı an (epoch ms). */
data class KompilyatorKesi(val versiyalar: List<KompilyatorVersiyasi>, val vaxt: Long)

/* ---------- «açar=dəyər;…» sətir formatı ---------- */

internal fun xeriteAc(xam: String?): Map<String, Int> {
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

internal fun xeriteYaz(xerite: Map<String, Int>): String =
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
