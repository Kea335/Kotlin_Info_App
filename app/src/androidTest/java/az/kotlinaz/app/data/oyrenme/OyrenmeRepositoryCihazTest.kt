package az.kotlinaz.app.data.oyrenme

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Room DAO-ları və [RoomOyrenmeRepository] real SQLite-da (in-memory) — T5.1.
 * JVM unit testində Room-un Android variantı işləmir, ona görə cihaz testidir;
 * CI-da emulyator job-u işlədir.
 */
@RunWith(AndroidJUnit4::class)
class OyrenmeRepositoryCihazTest {

    private lateinit var db: KotlinAzDatabase
    private var indi = 1_000L
    private lateinit var repo: RoomOyrenmeRepository

    @Before
    fun ac() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KotlinAzDatabase::class.java)
            .build()
        repo = RoomOyrenmeRepository(db) { indi }
    }

    @After
    fun bagla() = db.close()

    @Test
    fun sehv_say_artir_iki_duzgunden_sonra_siyahidan_cixir() = runTest {
        repo.cavabYaz("q1", duzgun = false)
        indi = 2_000L
        repo.cavabYaz("q1", duzgun = false)
        assertEquals(listOf(SehvCavab("q1", 2, 0, 2_000L)), repo.sehvler.first())

        repo.cavabYaz("q1", duzgun = true)
        assertEquals(1, repo.sehvler.first().size)
        repo.cavabYaz("q1", duzgun = true)
        assertTrue(repo.sehvler.first().isEmpty(), "mənimsənilmiş sual siyahıdan çıxmalıdır")
        // Sətir statistika üçün qalır.
        assertEquals(2, repo.butunSehvler.first().single().say)
    }

    @Test
    fun sehv_olmayan_sualin_duzgun_cavabi_yazilmir() = runTest {
        repo.cavabYaz("q2", duzgun = true)
        assertTrue(repo.butunSehvler.first().isEmpty())
    }

    @Test
    fun sehvler_en_tezesi_evvel() = runTest {
        repo.cavabYaz("a", false)
        indi = 5_000L
        repo.cavabYaz("b", false)
        assertEquals(listOf("b", "a"), repo.sehvler.first().map { it.sualId })
    }

    @Test
    fun bookmark_deyis_elave_edir_ve_silir() = runTest {
        repo.bookmarkDeyis("null-safety", BookmarkTipi.BOLME)
        assertEquals(listOf(Bookmark("null-safety", BookmarkTipi.BOLME, 1_000L)), repo.bookmarklar.first())
        repo.bookmarkDeyis("null-safety", BookmarkTipi.BOLME)
        assertTrue(repo.bookmarklar.first().isEmpty())
    }

    @Test
    fun bos_qeyd_silinir() = runTest {
        repo.qeydYaz("x", "Vacibdir")
        assertEquals("Vacibdir", repo.qeydler.first().single().metn)
        repo.qeydYaz("x", "  ")
        assertTrue(repo.qeydler.first().isEmpty())
    }

    @Test
    fun quiz_turlari_en_tezesi_evvel() = runTest {
        repo.quizTuruYaz("junior", 12, 15)
        indi = 9_000L
        repo.quizTuruYaz("senior", 7, 15)
        assertEquals(listOf("senior", "junior"), repo.quizTurlari.first().map { it.rejim })
    }

    @Test
    fun gunluk_fealiyyet_eyni_gunde_artir() = runTest {
        repo.hellQeydEt()
        repo.hellQeydEt()
        indi += 24L * 60 * 60 * 1000
        repo.hellQeydEt()
        assertEquals(listOf(2, 1), repo.fealiyyet.first().map { it.hellSayi })
    }
}
