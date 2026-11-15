package az.kotlinaz.app.data.oyrenme

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/* ============================================================
   Öyrənmə tarixçəsi — Room (T5.1).

   DataStore id çoxluqları (oxunmuş, həll edilmiş) və bir neçə sayğac üçün
   qalır; burada yalnız zaman və say daşıyan tarixçə var: səhv cavablar,
   bookmark, qeydlər, quiz turları, günlük fəallıq. Miqrasiya yoxdur —
   DataStore datası yerindədir.

   Bütün oxunuşlar Flow, yazılar suspend — əsas sapda sorğu yoxdur.
   Fayl `databases/kotlinaz.db` Android yedəyinə düşür (backup_rules.xml).
   ============================================================ */

@Entity(tableName = "sehv_cavab")
data class SehvCavabEntity(
    /** Quiz sualının id-si: `quiz-01` (quiz.json) və ya `q-<nəzəri id>` (çalışma bankı). */
    @PrimaryKey val sualId: String,
    /** Neçə dəfə səhv cavab verilib — heç vaxt azalmır. */
    val say: Int,
    /** Sonuncu səhvdən bəri ardıcıl düzgün cavab; 2-yə çatanda mənimsənilib (T5.2). */
    val ardicilDuzgun: Int,
    /** Son cavabın vaxtı, epoch ms. */
    val sonTarix: Long
)

@Entity(tableName = "bookmark")
data class BookmarkEntity(
    /** Bölmə, çalışma və ya blok id-si (T3.1) — dildən asılı deyil. */
    @PrimaryKey val hedefId: String,
    /** `bolme` | `calisma` | `kod`. */
    val tip: String,
    val tarix: Long
)

@Entity(tableName = "qeyd")
data class QeydEntity(
    @PrimaryKey val hedefId: String,
    val metn: String,
    val yenilenib: Long
)

@Entity(tableName = "quiz_turu")
data class QuizTuruEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rejim: String,
    val bal: Int,
    val umumi: Int,
    val tarix: Long
)

@Entity(tableName = "gunluk_fealiyyet")
data class GunlukFealiyyetEntity(
    /** Yerli tarix, ISO: `2026-11-02`. */
    @PrimaryKey val tarix: String,
    val hellSayi: Int
)

@Dao
interface SehvDao {
    /** Hələ mənimsənilməmiş səhvlər, ən təzəsi əvvəl. */
    @Query("SELECT * FROM sehv_cavab WHERE ardicilDuzgun < :hedd ORDER BY sonTarix DESC")
    fun aktivler(hedd: Int): Flow<List<SehvCavabEntity>>

    @Query("SELECT * FROM sehv_cavab ORDER BY sonTarix DESC")
    fun hamisi(): Flow<List<SehvCavabEntity>>

    @Query("SELECT * FROM sehv_cavab WHERE sualId = :sualId")
    suspend fun tap(sualId: String): SehvCavabEntity?

    @Upsert
    suspend fun yaz(e: SehvCavabEntity)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmark ORDER BY tarix DESC")
    fun hamisi(): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) > 0 FROM bookmark WHERE hedefId = :hedefId")
    suspend fun varmi(hedefId: String): Boolean

    @Upsert
    suspend fun yaz(e: BookmarkEntity)

    @Query("DELETE FROM bookmark WHERE hedefId = :hedefId")
    suspend fun sil(hedefId: String)
}

@Dao
interface QeydDao {
    @Query("SELECT * FROM qeyd ORDER BY yenilenib DESC")
    fun hamisi(): Flow<List<QeydEntity>>

    @Upsert
    suspend fun yaz(e: QeydEntity)

    @Query("DELETE FROM qeyd WHERE hedefId = :hedefId")
    suspend fun sil(hedefId: String)
}

@Dao
interface QuizTuruDao {
    @Query("SELECT * FROM quiz_turu ORDER BY tarix DESC")
    fun hamisi(): Flow<List<QuizTuruEntity>>

    @Insert
    suspend fun yaz(e: QuizTuruEntity)
}

@Dao
interface FealiyyetDao {
    @Query("SELECT * FROM gunluk_fealiyyet ORDER BY tarix")
    fun hamisi(): Flow<List<GunlukFealiyyetEntity>>

    // minSdk 24-ün SQLite-ı (3.9) `ON CONFLICT DO UPDATE` bilmir — iki
    // addım bir tranzaksiyada: sətir yoxdursa 0 ilə yaranır, sonra artırılır.
    @Query("INSERT OR IGNORE INTO gunluk_fealiyyet(tarix, hellSayi) VALUES(:tarix, 0)")
    suspend fun yarat(tarix: String)

    @Query("UPDATE gunluk_fealiyyet SET hellSayi = hellSayi + 1 WHERE tarix = :tarix")
    suspend fun artir(tarix: String)

    @Transaction
    suspend fun birArtir(tarix: String) {
        yarat(tarix)
        artir(tarix)
    }
}

@Database(
    entities = [
        SehvCavabEntity::class,
        BookmarkEntity::class,
        QeydEntity::class,
        QuizTuruEntity::class,
        GunlukFealiyyetEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class KotlinAzDatabase : RoomDatabase() {
    abstract fun sehvDao(): SehvDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun qeydDao(): QeydDao
    abstract fun quizTuruDao(): QuizTuruDao
    abstract fun fealiyyetDao(): FealiyyetDao

    companion object {
        const val FAYL = "kotlinaz.db"
    }
}
