package az.kotlinaz.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/* ============================================================
   Sayt məzmununun tətbiqdəki qarşılığı.
   JSON aktivləri tools/build-content.js tərəfindən yaradılır.
   ============================================================ */

/** Sətir daxili mətn parçası: adi mətn, qalın, kod, keçid və s. */
@Serializable
data class Span(
    val k: String,
    val v: String = "",
    val href: String? = null
) {
    companion object {
        const val TEXT = "t"
        const val BOLD = "b"
        const val ITALIC = "i"
        const val CODE = "code"
        const val LINK = "link"
        const val KBD = "kbd"
        const val GRAD = "grad"
        const val BREAK = "br"
    }
}

@Serializable
@JsonClassDiscriminator("t")
sealed interface Block

@Serializable @SerialName("h2")
data class HeadingH2(val text: String) : Block

@Serializable @SerialName("h3")
data class HeadingH3(val text: String) : Block

@Serializable @SerialName("h4")
data class HeadingH4(val text: String) : Block

@Serializable @SerialName("h5")
data class HeadingH5(val text: String) : Block

@Serializable @SerialName("p")
data class Paragraph(
    val spans: List<Span> = emptyList(),
    val lead: Boolean = false
) : Block

@Serializable @SerialName("code")
data class CodeBlock(
    val title: String = "",
    val lang: String = "kotlin",
    val code: String,
    val output: String? = null
) : Block

@Serializable @SerialName("callout")
data class Callout(
    val kind: String = "info",
    val ico: String = "",
    val title: String = "",
    val blocks: List<Block> = emptyList()
) : Block

@Serializable @SerialName("cards")
data class CardGrid(
    val cols: Int = 2,
    val items: List<CardItem> = emptyList()
) : Block

@Serializable
data class CardItem(
    val ico: String = "",
    val title: String = "",
    val blocks: List<Block> = emptyList()
)

@Serializable @SerialName("table")
data class TableBlock(
    val head: List<List<List<Span>>> = emptyList(),
    val rows: List<List<List<Span>>> = emptyList()
) : Block

@Serializable @SerialName("list")
data class ListBlock(
    val ordered: Boolean = false,
    val items: List<List<Span>> = emptyList()
) : Block

@Serializable @SerialName("timeline")
data class Timeline(val items: List<TimelineItem> = emptyList()) : Block

@Serializable
data class TimelineItem(
    val year: String = "",
    val title: String = "",
    val desc: List<Span> = emptyList()
)

@Serializable @SerialName("tabs")
data class TabsBlock(val tabs: List<TabItem> = emptyList()) : Block

@Serializable
data class TabItem(
    val label: String = "",
    val blocks: List<Block> = emptyList()
)

@Serializable @SerialName("hero")
data class Hero(
    val badge: String = "",
    val title: List<Span> = emptyList(),
    val sub: String = "",
    val stats: List<HeroStat> = emptyList()
) : Block

@Serializable
data class HeroStat(val num: String = "", val lbl: String = "")

/** Nativ interaktiv nümayiş yerləşdiricisi. */
@Serializable @SerialName("demo")
data class DemoBlock(val demo: String) : Block

@Serializable @SerialName("quiz")
data object QuizBlock : Block

@Serializable @SerialName("playground")
data object PlaygroundBlock : Block

@Serializable @SerialName("hr")
data object Divider : Block

/* ---------- Bölmələr ---------- */

@Serializable
data class Section(
    val id: String,
    val index: Int,
    val title: String,
    val heading: String = "",
    val group: String = "",
    val kicker: String = "",
    val special: String? = null,
    val blocks: List<Block> = emptyList()
)

@Serializable
data class Content(
    val version: String = "1.0.0",
    val sections: List<Section> = emptyList()
)

/* ---------- Çalışmalar ---------- */

@Serializable
data class TheoryExercise(
    val id: String,
    val no: Int,
    val level: String,
    val q: String,
    val code: String? = null,
    val opts: List<String> = emptyList(),
    val a: Int,
    val exp: String
)

@Serializable
data class PracticeExercise(
    val id: String,
    val no: Int,
    val level: String,
    val tapsiriq: String,
    val starter: String,
    val gozlenilen: String,
    val ipucu: String? = null,
    val hell: String
)

@Serializable
data class ExerciseTopic(
    val id: String,
    val title: String,
    val sectionId: String = id,
    val nezeri: List<TheoryExercise> = emptyList(),
    val praktiki: List<PracticeExercise> = emptyList()
)

@Serializable
data class ExerciseBank(val topics: List<ExerciseTopic> = emptyList())

/* ---------- Bilik testi ---------- */

@Serializable
data class QuizQuestion(
    val id: String,
    val q: String,
    val code: String? = null,
    val opts: List<String> = emptyList(),
    val a: Int,
    val exp: String
)

@Serializable
data class QuizBank(val questions: List<QuizQuestion> = emptyList())

/* ---------- Kod meydanı nümunələri ---------- */

@Serializable
data class PlaygroundPreset(val id: String, val label: String, val code: String)

@Serializable
data class PlaygroundData(val presets: List<PlaygroundPreset> = emptyList())

/* ---------- Axtarış indeksi ---------- */

@Serializable
data class SearchDoc(
    val id: String,
    val title: String,
    val group: String = "",
    val text: String = ""
)

@Serializable
data class SearchIndex(val docs: List<SearchDoc> = emptyList())

/* ---------- Səviyyə ---------- */

enum class Level(val id: String, val label: String) {
    JUNIOR("junior", "Junior"),
    MIDDLE("middle", "Middle"),
    SENIOR("senior", "Senior");

    companion object {
        fun from(id: String): Level = entries.firstOrNull { it.id == id } ?: JUNIOR
    }
}
