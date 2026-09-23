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
    @Serializable data class Ders(val id: String)
    @Serializable object Calismalar
    @Serializable data class Calisma(val id: String)
    @Serializable object Quiz
    @Serializable object Meydan
    @Serializable object Axtaris
    @Serializable object Tenzimleme
    /** «Səhvlərim» (T5.2). */
    @Serializable object Sehvlerim
    /** Yalnız səhv suallardan test — QuizScreen [QuizRejimi.SEHVLER] ilə. */
    @Serializable object SehvTesti
}
