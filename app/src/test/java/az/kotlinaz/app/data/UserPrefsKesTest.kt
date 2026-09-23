package az.kotlinaz.app.data

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Kompilyator keşi ayrıca, yedəklənməyən store-dadır (Mərhələ 5); tərəqqi
 * və meydanın kodu əsas (yedəklənən) store-da qalır.
 */
class UserPrefsKesTest {

    private val kohneVersiyalar = stringPreferencesKey("compiler_versions")
    private val kohneVaxt = longPreferencesKey("compiler_versions_ts")

    @Test
    fun `kes ayrica store-a yazilir, kohne acarlar esas fayldan silinir`() = runTest {
        val esas = YaddasDataStore(preferencesOf(kohneVersiyalar to "2.3.0", kohneVaxt to 1L))
        val kes = YaddasDataStore()
        val prefs = UserPrefs(esas, kesStore = kes)

        prefs.kompilyatorKesiniYaz(listOf(KompilyatorVersiyasi("2.4.10", latestStable = true)), 1_000L)

        val e = esas.data.first()
        assertFalse(kohneVersiyalar in e)
        assertFalse(kohneVaxt in e)
        assertTrue(kohneVersiyalar in kes.data.first())
        assertEquals(KompilyatorKesi(listOf(KompilyatorVersiyasi("2.4.10", true)), 1_000L), prefs.kompilyatorKesi())
    }

    @Test
    fun `kes esas fayldan oxunmur`() = runTest {
        // Köhnə versiyadan qalan keş yeni store-a köçürülmür — sadəcə yenidən yüklənir.
        val esas = YaddasDataStore(preferencesOf(kohneVersiyalar to "2.3.0", kohneVaxt to 1L))
        assertNull(UserPrefs(esas, kesStore = YaddasDataStore()).kompilyatorKesi())
    }

    @Test
    fun `tereqqi ve meydan kodu esas store-dadir`() = runTest {
        val esas = YaddasDataStore()
        val kes = YaddasDataStore()
        val prefs = UserPrefs(esas, kesStore = kes)

        prefs.calismaniHellIsaretle("c1")
        prefs.meydanQaralamasiniYaz(MeydanQaralamasi("println(1)", null))

        assertEquals(0, kes.yazmaSayi)
        assertEquals(setOf("c1"), prefs.hellEdilmis.first())
        assertEquals(MeydanQaralamasi("println(1)", null), prefs.meydanQaralamasi())
    }
}
