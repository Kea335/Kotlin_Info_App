package az.kotlinaz.app.data.oyrenme

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Yaddaşda işləyən [OyrenmeRepository] — ViewModel testləri üçün. Qaydalar
 * real implementasiya ilə eynidir ([sehvYenile]); saat idarə olunur.
 */
class FakeOyrenmeRepository(var indi: Long = 1_000L) : OyrenmeRepository {

    private val sehv = MutableStateFlow<Map<String, SehvCavab>>(emptyMap())
    private val bm = MutableStateFlow<List<Bookmark>>(emptyList())
    private val qd = MutableStateFlow<Map<String, Qeyd>>(emptyMap())
    private val turlar = MutableStateFlow<List<QuizTuru>>(emptyList())
    private val gunler = MutableStateFlow<Map<String, Int>>(emptyMap())

    override val sehvler: Flow<List<SehvCavab>> =
        sehv.map { m -> m.values.filterNot { it.menimsenilib }.sortedByDescending { it.sonTarix } }
    override val butunSehvler: Flow<List<SehvCavab>> =
        sehv.map { m -> m.values.sortedByDescending { it.sonTarix } }

    override suspend fun cavabYaz(sualId: String, duzgun: Boolean) {
        val yeni = sehvYenile(sehv.value[sualId], duzgun, indi) ?: return
        sehv.value = sehv.value + (sualId to yeni.copy(sualId = sualId))
    }

    override val bookmarklar: Flow<List<Bookmark>> = bm.map { l -> l.sortedByDescending { it.tarix } }

    override suspend fun bookmarkDeyis(hedefId: String, tip: BookmarkTipi) {
        bm.value = if (bm.value.any { it.hedefId == hedefId }) bm.value.filterNot { it.hedefId == hedefId }
        else bm.value + Bookmark(hedefId, tip, indi)
    }

    override val qeydler: Flow<List<Qeyd>> = qd.map { m -> m.values.sortedByDescending { it.yenilenib } }

    override suspend fun qeydYaz(hedefId: String, metn: String) {
        qd.value = if (metn.isBlank()) qd.value - hedefId else qd.value + (hedefId to Qeyd(hedefId, metn, indi))
    }

    override val quizTurlari: Flow<List<QuizTuru>> = turlar.map { l -> l.sortedByDescending { it.tarix } }

    override suspend fun quizTuruYaz(rejim: String, bal: Int, umumi: Int) {
        turlar.value = turlar.value + QuizTuru(rejim, bal, umumi, indi)
    }

    override val fealiyyet: Flow<List<GunlukFealiyyet>> =
        gunler.map { m -> m.entries.sortedBy { it.key }.map { GunlukFealiyyet(it.key, it.value) } }

    override suspend fun hellQeydEt() {
        val g = gunTarixi(indi)
        gunler.value = gunler.value + (g to (gunler.value[g] ?: 0) + 1)
    }
}
