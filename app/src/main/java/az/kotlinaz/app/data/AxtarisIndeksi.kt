package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.Block
import az.kotlinaz.app.data.model.Callout
import az.kotlinaz.app.data.model.CardGrid
import az.kotlinaz.app.data.model.CodeBlock
import az.kotlinaz.app.data.model.HeadingH2
import az.kotlinaz.app.data.model.HeadingH3
import az.kotlinaz.app.data.model.HeadingH4
import az.kotlinaz.app.data.model.HeadingH5
import az.kotlinaz.app.data.model.Hero
import az.kotlinaz.app.data.model.ListBlock
import az.kotlinaz.app.data.model.Paragraph
import az.kotlinaz.app.data.model.SearchDoc
import az.kotlinaz.app.data.model.Section
import az.kotlinaz.app.data.model.Span
import az.kotlinaz.app.data.model.TableBlock
import az.kotlinaz.app.data.model.TabsBlock
import az.kotlinaz.app.data.model.Timeline

/* ============================================================
   Axtarış sənədləri yüklənmiş məzmundan qurulur (T3.5).

   Əvvəllər hər dil üçün ayrıca search.json (~120 KB) — eyni mətnin ikinci
   surəti — APK-ya qoyulurdu, «indeks hansı dildəndir?» sualı ilə birlikdə.
   İndi indeks həmişə göstərilən məzmunun özündən qurulur: yanlış dilin
   indeksi mümkün deyil. Qayda tools/content-lib.js-dəki `axtarisIndeksi`
   ilə eynidir — nəticələrin sırası və parçalar dəyişmir.
   ============================================================ */

/** Sənəd mətninin yuxarı həddi — çox uzun bölmədə axtarış yaddaşı şişməsin. */
private const val MAKS_METN = 24_000

/**
 * JS-in `\s` sinfi: Java-nın `\s`-indən fərqli olaraq Unicode boşluqlarını da
 * (qırılmayan boşluq, ` ` və s.) tutur — məzmunda bunlar var.
 */
private val BOSLUQ = Regex("[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+")

fun axtarisSenedleri(sections: List<Section>): List<SearchDoc> = sections.map { s ->
    val parcalar = mutableListOf(s.title, s.heading, s.group, s.kicker)
    bloklardanMetn(s.blocks, parcalar)
    val metn = parcalar.filter { it.isNotEmpty() }
        .joinToString(" ")
        .replace(BOSLUQ, " ")
        .trim(' ')
    SearchDoc(id = s.id, title = s.title, group = s.group, text = metn.take(MAKS_METN))
}

/** Paraqrafın düz mətni; sətir sonu (`br`) boşluq olur. */
private fun spanMetni(spans: List<Span>): String =
    spans.joinToString("") { if (it.k == Span.BREAK) " " else it.v }

/** Blokların mətni [topla]-ya, daxili bloklar (callout, kart, tab) da daxil. */
private fun bloklardanMetn(blocks: List<Block>, topla: MutableList<String>) {
    blocks.forEach { b ->
        when (b) {
            is HeadingH2 -> topla += b.text
            is HeadingH3 -> topla += b.text
            is HeadingH4 -> topla += b.text
            is HeadingH5 -> topla += b.text
            is Paragraph -> topla += spanMetni(b.spans)
            is CodeBlock -> { topla += b.title; topla += b.code }
            is Callout -> { topla += b.title; bloklardanMetn(b.blocks, topla) }
            is CardGrid -> b.items.forEach { topla += it.title; bloklardanMetn(it.blocks, topla) }
            is TableBlock -> (b.head + b.rows).forEach { setir -> setir.forEach { topla += spanMetni(it) } }
            is ListBlock -> b.items.forEach { topla += spanMetni(it) }
            is Timeline -> b.items.forEach { topla += it.year; topla += it.title; topla += spanMetni(it.desc) }
            is TabsBlock -> b.tabs.forEach { topla += it.label; bloklardanMetn(it.blocks, topla) }
            is Hero -> {
                topla += b.badge; topla += spanMetni(b.title); topla += b.sub
                b.stats.forEach { topla += it.num; topla += it.lbl }
            }
            // Nümayiş, quiz, meydan, xətt — mətnləri yoxdur.
            else -> Unit
        }
    }
}
