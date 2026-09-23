package az.kotlinaz.app.ui.isareler

import az.kotlinaz.app.data.model.Block
import az.kotlinaz.app.data.model.Callout
import az.kotlinaz.app.data.model.CardGrid
import az.kotlinaz.app.data.model.CodeBlock
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.IdliBlok
import az.kotlinaz.app.data.model.Section
import az.kotlinaz.app.data.model.TabsBlock
import az.kotlinaz.app.data.oyrenme.Bookmark
import az.kotlinaz.app.data.oyrenme.BookmarkTipi
import az.kotlinaz.app.data.oyrenme.Qeyd

/* ============================================================
   İşarə və qeydlərin hədəfləri (T5.3). Hədəf id-lə saxlanılır —
   bölmə, çalışma və ya kod bloku (T3.1-in sabit id-si); başlıq və
   keçid seçilmiş dilin məzmunundan qurulur. Saf funksiyalardır.
   ============================================================ */

/** Hədəfə keçid. */
sealed interface Kecid {
    /** Dərs; [blokId] verilibsə həmin kod blokuna sürüşülür. */
    data class Ders(val bolmeId: String, val blokId: String? = null) : Kecid

    /** Çalışma mövzusu, birbaşa həmin çalışma açıq. */
    data class Calisma(val movzuId: String, val calismaId: String) : Kecid
}

data class Hedef(
    val tip: BookmarkTipi,
    /** Bölmə adı, sualın/tapşırığın mətni və ya kod başlığı. */
    val basliq: String,
    /** Harada: qrup, mövzu və ya kodun olduğu dərs. */
    val yer: String,
    val kecid: Kecid
)

data class IsareSetri(val id: String, val hedef: Hedef)

data class QeydSetri(val id: String, val metn: String, val yenilenib: Long, val hedef: Hedef)

/** Bütün hədəflər id üzrə. İç-içə (callout, kart, tab) kod blokları da daxil. */
fun hedefXeritesi(sections: List<Section>, topics: List<ExerciseTopic>): Map<String, Hedef> {
    val m = HashMap<String, Hedef>()
    sections.forEach { s ->
        m[s.id] = Hedef(BookmarkTipi.BOLME, s.title, s.group, Kecid.Ders(s.id))
        kodBloklari(s.blocks).forEach { b ->
            if (b.id.isNotBlank()) {
                m[b.id] = Hedef(BookmarkTipi.KOD, b.title.ifBlank { ilkSetir(b.code) }, s.title, Kecid.Ders(s.id, b.id))
            }
        }
    }
    topics.forEach { t ->
        t.nezeri.forEach { m[it.id] = Hedef(BookmarkTipi.CALISMA, it.q, t.title, Kecid.Calisma(t.id, it.id)) }
        t.praktiki.forEach { m[it.id] = Hedef(BookmarkTipi.CALISMA, it.tapsiriq, t.title, Kecid.Calisma(t.id, it.id)) }
    }
    return m
}

/** İşarələr, ən təzəsi əvvəl; məzmunda olmayan hədəf səssizcə atlanır. */
fun isareSetirleri(isareler: List<Bookmark>, hedefler: Map<String, Hedef>): List<IsareSetri> =
    isareler.mapNotNull { b -> hedefler[b.hedefId]?.let { IsareSetri(b.hedefId, it) } }

/** Qeydlər, ən təzəsi əvvəl; hədəfi silinmiş qeyd atlanır. */
fun qeydSetirleri(qeydler: List<Qeyd>, hedefler: Map<String, Hedef>): List<QeydSetri> =
    qeydler.mapNotNull { q -> hedefler[q.hedefId]?.let { QeydSetri(q.hedefId, q.metn, q.yenilenib, it) } }

/** Kod blokunun hansı üst səviyyə blokun içində olduğu — dərs siyahısında sürüşmə üçün. */
fun ustBlokIndeksi(blocks: List<Block>, blokId: String): Int =
    blocks.indexOfFirst { b -> (b as? IdliBlok)?.id == blokId || kodBloklari(listOf(b)).any { it.id == blokId } }

private fun kodBloklari(blocks: List<Block>): List<CodeBlock> = blocks.flatMap { b ->
    when (b) {
        is CodeBlock -> listOf(b)
        is Callout -> kodBloklari(b.blocks)
        is CardGrid -> b.items.flatMap { kodBloklari(it.blocks) }
        is TabsBlock -> b.tabs.flatMap { kodBloklari(it.blocks) }
        else -> emptyList()
    }
}

private fun ilkSetir(kod: String): String = kod.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
