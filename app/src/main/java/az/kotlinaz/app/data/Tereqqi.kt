package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.model.Section

/**
 * Bir bölmənin mənimsəmə vəziyyəti.
 *
 * Bölmə YALNIZ ona bağlı çalışmaların hamısı düzgün həll ediləndə tamamlanmış
 * sayılır — mətni sürüşdürmək kifayət etmir. Çalışması olmayan bölmələr
 * (giriş, tarixçə, resurslar…) üçün yeganə yol istifadəçinin öz təsdiqidir.
 */
data class BolmeTereqqisi(
    val bolmeId: String,
    /** Həll edilmiş çalışma sayı (nəzəri + praktiki). */
    val hell: Int,
    /** Bölməyə bağlı ümumi çalışma sayı. 0 — çalışması olmayan bölmə. */
    val umumi: Int,
    /** İstifadəçi «Oxudum» ilə təsdiqləyib (yalnız çalışmasız bölmələrdə mənalıdır). */
    val elIleOxunub: Boolean
) {
    /** Çalışması olmayan bölmə. */
    val calismasiz: Boolean get() = umumi == 0

    val faiz: Float
        get() = when {
            umumi > 0 -> hell.toFloat() / umumi
            elIleOxunub -> 1f
            else -> 0f
        }

    val tamamlandi: Boolean get() = if (umumi > 0) hell >= umumi else elIleOxunub
}

/** Bir səviyyədəki çalışmaların sayı və neçəsinin həll edildiyi. */
data class SeviyyeSayi(val hell: Int, val umumi: Int) {
    val faiz: Float get() = if (umumi == 0) 0f else hell.toFloat() / umumi
}

/**
 * Səviyyə qaydaları.
 *
 * Yeni istifadəçi həmişə Junior başlayır. Növbəti səviyyə iki şərtin
 * hər ikisi ödənəndə açılır: cari səviyyənin çalışmalarının 80%-i və
 * bölmələrin müəyyən hissəsinin tam mənimsənilməsi.
 */
object SeviyyeQaydasi {
    /** Səviyyənin öz çalışmalarından tələb olunan pay. */
    const val CALISMA_HEDEFI = 0.80f

    /** Middle üçün tamamlanmalı bölmə payı. */
    const val MIDDLE_DERS_HEDEFI = 0.30f

    /** Senior üçün tamamlanmalı bölmə payı. */
    const val SENIOR_DERS_HEDEFI = 0.60f

    /** [hedef] səviyyəyə keçmək üçün lazım olan bölmə payı. */
    fun dersHedefi(hedef: Level): Float = when (hedef) {
        Level.JUNIOR -> 0f
        Level.MIDDLE -> MIDDLE_DERS_HEDEFI
        Level.SENIOR -> SENIOR_DERS_HEDEFI
    }

    /** [hedef] səviyyəyə keçmək üçün çalışmaları sayılan səviyyə. */
    fun onSeviyye(hedef: Level): Level = when (hedef) {
        Level.JUNIOR -> Level.JUNIOR
        Level.MIDDLE -> Level.JUNIOR
        Level.SENIOR -> Level.MIDDLE
    }
}

/** Tətbiqin bütün tərəqqi hesablamaları — tək yerdə. */
data class Tereqqi(
    val bolmeler: Map<String, BolmeTereqqisi>,
    val seviyyeler: Map<Level, SeviyyeSayi>,
    val tamamlananDers: Int,
    val umumiDers: Int,
    val seviyye: Level
) {
    val dersFaizi: Float get() = if (umumiDers == 0) 0f else tamamlananDers.toFloat() / umumiDers

    fun bolme(id: String): BolmeTereqqisi =
        bolmeler[id] ?: BolmeTereqqisi(id, 0, 0, false)

    fun say(lv: Level): SeviyyeSayi = seviyyeler[lv] ?: SeviyyeSayi(0, 0)

    /** Cari səviyyədən sonrakı səviyyə; Senior-dursa null. */
    val novbetiSeviyye: Level?
        get() = when (seviyye) {
            Level.JUNIOR -> Level.MIDDLE
            Level.MIDDLE -> Level.SENIOR
            Level.SENIOR -> null
        }
}

/**
 * Bölmələri, çalışma mövzularını və həll edilmiş id-ləri birləşdirib
 * tam tərəqqi mənzərəsi qurur. Saf funksiyadır — UI-dan asılı deyil,
 * ona görə `remember` içində sərbəst çağırıla bilər.
 */
fun tereqqiHesabla(
    sections: List<Section>,
    topics: List<ExerciseTopic>,
    hellEdilmis: Set<String>,
    oxunanlar: Set<String>
): Tereqqi {
    // Bölmə id → mövzu. `sectionId` JSON-da yoxdursa mövzunun öz id-si götürülür
    // (Models.kt-dəki defolt dəyər), ona görə burada əlavə yoxlama lazım deyil.
    val movzuXeritesi = topics.associateBy { it.sectionId }

    val bolmeler = sections.associate { bolme ->
        val movzu = movzuXeritesi[bolme.id]
        val butun = movzu?.let { it.nezeri.map(::idAl) + it.praktiki.map(::idAl) } ?: emptyList()
        bolme.id to BolmeTereqqisi(
            bolmeId = bolme.id,
            hell = butun.count { hellEdilmis.contains(it) },
            umumi = butun.size,
            elIleOxunub = oxunanlar.contains(bolme.id)
        )
    }

    // Səviyyə üzrə saylar — bütün mövzuların nəzəri və praktiki çalışmaları.
    val seviyyeler = Level.entries.associateWith { lv ->
        var hell = 0
        var umumi = 0
        topics.forEach { t ->
            t.nezeri.forEach {
                if (Level.from(it.level) == lv) {
                    umumi++
                    if (hellEdilmis.contains(it.id)) hell++
                }
            }
            t.praktiki.forEach {
                if (Level.from(it.level) == lv) {
                    umumi++
                    if (hellEdilmis.contains(it.id)) hell++
                }
            }
        }
        SeviyyeSayi(hell, umumi)
    }

    val tamamlanan = bolmeler.values.count { it.tamamlandi }
    val dersFaizi = if (sections.isEmpty()) 0f else tamamlanan.toFloat() / sections.size

    fun acildi(hedef: Level): Boolean {
        val on = SeviyyeQaydasi.onSeviyye(hedef)
        val say = seviyyeler[on] ?: SeviyyeSayi(0, 0)
        return say.umumi > 0 &&
            say.faiz >= SeviyyeQaydasi.CALISMA_HEDEFI &&
            dersFaizi >= SeviyyeQaydasi.dersHedefi(hedef)
    }

    // Hər kəs Junior başlayır; səviyyələr ardıcıl açılır.
    val seviyye = when {
        acildi(Level.SENIOR) -> Level.SENIOR
        acildi(Level.MIDDLE) -> Level.MIDDLE
        else -> Level.JUNIOR
    }

    return Tereqqi(
        bolmeler = bolmeler,
        seviyyeler = seviyyeler,
        tamamlananDers = tamamlanan,
        umumiDers = sections.size,
        seviyye = seviyye
    )
}

private fun idAl(e: az.kotlinaz.app.data.model.TheoryExercise) = e.id
private fun idAl(e: az.kotlinaz.app.data.model.PracticeExercise) = e.id
