package az.kotlinaz.app.ui

import kotlinx.serialization.Serializable

/**
 * Naviqasiya ünvanları — tip-təhlükəsiz route-lar (T2.7).
 *
 * Hər ekran bir `@Serializable` tipdir; arqumentli ekranlar `data class`.
 * NavHost route-u sinifdən özü qurur, arqument isə `entry.toRoute<Ders>()`
 * ilə tipli oxunur — səhv açar adı və ya çatışmayan arqument artıq
 * kompilyasiya vaxtında görünür, string literal qalmır.
 */
object Yol {
    @Serializable object Dersler
    /** [blok] — işarələnmiş kod bloku (T5.3); verilibsə dərs ona sürüşür. */
    @Serializable data class Ders(val id: String, val blok: String? = null)
    @Serializable object Calismalar
    /** [hedef] — işarədən açılan konkret çalışma (T5.3). */
    @Serializable data class Calisma(val id: String, val hedef: String? = null)
    @Serializable object Quiz
    @Serializable object Meydan
    @Serializable object Axtaris
    @Serializable object Tenzimleme
    /** «Səhvlərim» (T5.2). */
    @Serializable object Sehvlerim
    /** Yalnız səhv suallardan test — QuizScreen [QuizRejimi.SEHVLER] ilə. */
    @Serializable object SehvTesti
    /** «İşarələr və qeydlər» (T5.3). */
    @Serializable object Isareler
    /** Statistika (T5.4). */
    @Serializable object Statistika
}
