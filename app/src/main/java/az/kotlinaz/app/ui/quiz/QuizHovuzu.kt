package az.kotlinaz.app.ui.quiz

import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.model.QuizQuestion

/** Bir turdakı sual sayı. Hovuz daha kiçikdirsə, olduğu qədər sual verilir. */
const val QUIZ_SUAL_SAYI = 15

/**
 * Testin rejimi.
 *
 * Səviyyə rejimləri sualları çalışma bankının nəzəri hissəsindən götürür —
 * orada hər sualın `level` sahəsi var. «Qarışıq» isə həm bankı, həm də
 * quiz.json-dakı ümumi sualları birləşdirir.
 */
enum class QuizRejimi(val id: String, val seviyye: Level?) {
    JUNIOR("junior", Level.JUNIOR),
    MIDDLE("middle", Level.MIDDLE),
    SENIOR("senior", Level.SENIOR),
    QARISIQ("qarisiq", null)
}

/** Rejim üzrə sual hovuzları. Saf funksiya — testdən birbaşa çağırılır (T1.1). */
internal fun hovuzlariQur(
    questions: List<QuizQuestion>,
    topics: List<ExerciseTopic>
): Map<QuizRejimi, List<QuizQuestion>> {
    // Bankdakı nəzəri çalışmalar səviyyəyə görə qruplaşdırılır.
    val seviyyeUzre = mutableMapOf<Level, MutableList<QuizQuestion>>()
    topics.forEach { t ->
        t.nezeri.forEach { e ->
            // Nəzəri çalışma testin sual formatına birbaşa uyğun gəlir; yalnız
            // id-yə önlük əlavə olunur ki, quiz.json-dakılarla qarışmasın.
            seviyyeUzre.getOrPut(Level.from(e.level)) { mutableListOf() }.add(
                QuizQuestion(
                    id = "q-${e.id}",
                    q = e.q,
                    code = e.code,
                    opts = e.opts,
                    a = e.a,
                    exp = e.exp
                )
            )
        }
    }

    return QuizRejimi.entries.associateWith { r ->
        when (val lv = r.seviyye) {
            // Qarışıq: quiz.json-dakı ümumi suallar + bütün bank.
            null -> questions + seviyyeUzre.values.flatten()
            else -> seviyyeUzre[lv].orEmpty()
        }
    }
}
