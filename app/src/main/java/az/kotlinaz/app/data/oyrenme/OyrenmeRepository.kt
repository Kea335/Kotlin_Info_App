package az.kotlinaz.app.data.oyrenme

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ============================================================
   Öyrənmə tarixçəsinin repository sərhədi (T5.1). ViewModel-lər DAO-nu
   görmür — yalnız bu interfeysi; testdə yaddaşda işləyən fake ötürülür.
   ============================================================ */

/** Sual bu qədər ardıcıl düzgün cavablananda «Səhvlərim»-dən çıxır (T5.2). */
const val MENIMSEME_HEDDI = 2

data class SehvCavab(
    val sualId: String,
    val say: Int,
    val ardicilDuzgun: Int,
    val sonTarix: Long
) {
    val menimsenilib: Boolean get() = ardicilDuzgun >= MENIMSEME_HEDDI
}

enum class BookmarkTipi(val id: String) {
    BOLME("bolme"), CALISMA("calisma"), KOD("kod");

    companion object {
        fun from(id: String): BookmarkTipi = entries.firstOrNull { it.id == id } ?: BOLME
    }
}

data class Bookmark(val hedefId: String, val tip: BookmarkTipi, val tarix: Long)

data class Qeyd(val hedefId: String, val metn: String, val yenilenib: Long)

data class QuizTuru(val rejim: String, val bal: Int, val umumi: Int, val tarix: Long)

/** [tarix] — yerli gün, ISO (`2026-11-02`). */
data class GunlukFealiyyet(val tarix: String, val hellSayi: Int)

interface OyrenmeRepository {
    /** Hələ mənimsənilməmiş səhvlər, ən təzəsi əvvəl. */
    val sehvler: Flow<List<SehvCavab>>

    /** Bütün səhv tarixçəsi, mənimsənilənlər də — statistika üçün. */
    val butunSehvler: Flow<List<SehvCavab>>

    /** Quiz və ya nəzəri çalışmada verilən cavab; qayda [sehvYenile]-dədir. */
    suspend fun cavabYaz(sualId: String, duzgun: Boolean)

    val bookmarklar: Flow<List<Bookmark>>

    /** Varsa silir, yoxdursa əlavə edir. */
    suspend fun bookmarkDeyis(hedefId: String, tip: BookmarkTipi)

    val qeydler: Flow<List<Qeyd>>

    /** Boş mətn qeydi silir. */
    suspend fun qeydYaz(hedefId: String, metn: String)

    val quizTurlari: Flow<List<QuizTuru>>

    suspend fun quizTuruYaz(rejim: String, bal: Int, umumi: Int)

    /** Gün üzrə həll sayı, köhnədən yeniyə. */
    val fealiyyet: Flow<List<GunlukFealiyyet>>

    /** Bu gün bir çalışma həll olundu. */
    suspend fun hellQeydEt()
}

/**
 * Səhv cədvəlinin qaydası — saf funksiya, testdə birbaşa yoxlanılır (T5.2):
 *  - səhv cavab: `say++`, `ardicilDuzgun = 0` (mənimsənilmiş sual yenidən açılır);
 *  - düzgün cavab: `ardicilDuzgun++`, `say` dəyişmir; [MENIMSEME_HEDDI]-ə çatanda
 *    sual siyahıdan çıxır, sətir statistika üçün qalır;
 *  - heç səhv olmamış sualın düzgün cavabı yazılmır (null).
 */
fun sehvYenile(movcud: SehvCavab?, duzgun: Boolean, indi: Long): SehvCavab? = when {
    !duzgun -> SehvCavab(
        sualId = movcud?.sualId.orEmpty(),
        say = (movcud?.say ?: 0) + 1,
        ardicilDuzgun = 0,
        sonTarix = indi
    )
    movcud == null -> null
    else -> movcud.copy(
        ardicilDuzgun = (movcud.ardicilDuzgun + 1).coerceAtMost(MENIMSEME_HEDDI),
        sonTarix = indi
    )
}

/** Yerli günün ISO tarixi — `java.time` minSdk 24-də yoxdur. */
fun gunTarixi(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMs))

class RoomOyrenmeRepository(
    private val db: KotlinAzDatabase,
    private val saat: () -> Long = System::currentTimeMillis
) : OyrenmeRepository {

    private val sehvDao = db.sehvDao()
    private val bookmarkDao = db.bookmarkDao()
    private val qeydDao = db.qeydDao()
    private val quizDao = db.quizTuruDao()
    private val fealiyyetDao = db.fealiyyetDao()

    override val sehvler: Flow<List<SehvCavab>> =
        sehvDao.aktivler(MENIMSEME_HEDDI).map { l -> l.map { it.model() } }

    override val butunSehvler: Flow<List<SehvCavab>> =
        sehvDao.hamisi().map { l -> l.map { it.model() } }

    // Oxu-dəyiş-yaz tranzaksiyada: quiz və çalışma eyni sualı eyni anda yazsa say itməsin.
    override suspend fun cavabYaz(sualId: String, duzgun: Boolean) = db.withTransaction {
        val yeni = sehvYenile(sehvDao.tap(sualId)?.model(), duzgun, saat())
        if (yeni != null) sehvDao.yaz(yeni.copy(sualId = sualId).entity())
    }

    override val bookmarklar: Flow<List<Bookmark>> =
        bookmarkDao.hamisi().map { l -> l.map { Bookmark(it.hedefId, BookmarkTipi.from(it.tip), it.tarix) } }

    override suspend fun bookmarkDeyis(hedefId: String, tip: BookmarkTipi) = db.withTransaction {
        if (bookmarkDao.varmi(hedefId)) bookmarkDao.sil(hedefId)
        else bookmarkDao.yaz(BookmarkEntity(hedefId, tip.id, saat()))
    }

    override val qeydler: Flow<List<Qeyd>> =
        qeydDao.hamisi().map { l -> l.map { Qeyd(it.hedefId, it.metn, it.yenilenib) } }

    override suspend fun qeydYaz(hedefId: String, metn: String) {
        if (metn.isBlank()) qeydDao.sil(hedefId) else qeydDao.yaz(QeydEntity(hedefId, metn, saat()))
    }

    override val quizTurlari: Flow<List<QuizTuru>> =
        quizDao.hamisi().map { l -> l.map { QuizTuru(it.rejim, it.bal, it.umumi, it.tarix) } }

    override suspend fun quizTuruYaz(rejim: String, bal: Int, umumi: Int) =
        quizDao.yaz(QuizTuruEntity(rejim = rejim, bal = bal, umumi = umumi, tarix = saat()))

    override val fealiyyet: Flow<List<GunlukFealiyyet>> =
        fealiyyetDao.hamisi().map { l -> l.map { GunlukFealiyyet(it.tarix, it.hellSayi) } }

    override suspend fun hellQeydEt() = fealiyyetDao.birArtir(gunTarixi(saat()))
}

private fun SehvCavabEntity.model() = SehvCavab(sualId, say, ardicilDuzgun, sonTarix)
private fun SehvCavab.entity() = SehvCavabEntity(sualId, say, ardicilDuzgun, sonTarix)
