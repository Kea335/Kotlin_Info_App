package az.kotlinaz.app.data

import java.util.Locale
import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.oyrenme.GunlukFealiyyet
import az.kotlinaz.app.data.oyrenme.QuizTuru

/* ============================================================
   Statistika (T5.4) — saf hesablama. `bugun` parametrdir ki,
   streak testləri sabit tarixlə yazılsın. Gün cihazın öz zona
   vaxtı ilə yazılır (gunTarixi), burada yalnız ISO sətirlər və
   gün nömrələri var — `java.time` minSdk 24-də yoxdur.
   ============================================================ */

/** Qrafikin bir sütunu: ISO gün və həll sayı. */
data class GunSutunu(val tarix: String, val say: Int)

/** Quiz tarixçəsinin sətri; [rekorddur] — həmin rejimdə ən yüksək bal. */
data class QuizTuruSetri(val tur: QuizTuru, val rekorddur: Boolean)

data class Statistika(
    val seviyye: Level,
    /** Tamamlanan / ümumi dərs. */
    val dersler: SeviyyeSayi,
    /** Həll edilən / ümumi çalışma. */
    val calismalar: SeviyyeSayi,
    val seviyyeUzre: Map<Level, SeviyyeSayi>,
    /** Son 10 tur, ən təzəsi əvvəl. */
    val quizTarixcesi: List<QuizTuruSetri>,
    /** Köhnədən yeniyə, boş günlər 0 ilə — qrafik həmişə 7 / 30 sütundur. */
    val son7Gun: List<GunSutunu>,
    val son30Gun: List<GunSutunu>,
    /** Ardıcıl fəal gün — bu gün və ya dünən fəallıq varsa davam edir. */
    val streak: Int,
    val enUzunStreak: Int
)

fun statistikaHesabla(
    tereqqi: Tereqqi,
    turlar: List<QuizTuru>,
    fealiyyet: List<GunlukFealiyyet>,
    bugun: String
): Statistika {
    val bugunNo = isoGunNomresi(bugun) ?: 0L
    // Gün nömrəsi → həll sayı; oxunmayan tarix atılır.
    val gunler = HashMap<Long, Int>()
    fealiyyet.forEach { f -> isoGunNomresi(f.tarix)?.let { gunler[it] = (gunler[it] ?: 0) + f.hellSayi } }
    val fealGunler = gunler.filterValues { it > 0 }.keys

    fun pencere(n: Int) = (n - 1 downTo 0).map { geri ->
        val no = bugunNo - geri
        GunSutunu(gunNomresindenIso(no), gunler[no] ?: 0)
    }

    val rekordlar = turlar.groupBy { it.rejim }.mapValues { (_, l) -> l.maxOf { it.bal } }
    val calismalar = tereqqi.seviyyeler.values.fold(SeviyyeSayi(0, 0)) { c, s ->
        SeviyyeSayi(c.hell + s.hell, c.umumi + s.umumi)
    }

    return Statistika(
        seviyye = tereqqi.seviyye,
        dersler = SeviyyeSayi(tereqqi.tamamlananDers, tereqqi.umumiDers),
        calismalar = calismalar,
        seviyyeUzre = Level.entries.associateWith { tereqqi.say(it) },
        quizTarixcesi = turlar.take(10).map { QuizTuruSetri(it, it.bal > 0 && it.bal == rekordlar[it.rejim]) },
        son7Gun = pencere(7),
        son30Gun = pencere(30),
        streak = streakHesabla(fealGunler, bugunNo),
        enUzunStreak = enUzunStreak(fealGunler)
    )
}

/**
 * Cari seriya: bu gün fəaldırsa bu gündən, deyilsə dünəndən geriyə ardıcıl
 * fəal günlər. İki gün boşluq (nə bu gün, nə dünən) — 0.
 */
fun streakHesabla(fealGunler: Set<Long>, bugun: Long): Int {
    var gun = when {
        bugun in fealGunler -> bugun
        bugun - 1 in fealGunler -> bugun - 1
        else -> return 0
    }
    var say = 0
    while (gun in fealGunler) { say++; gun-- }
    return say
}

/** Tarixçədəki ən uzun ardıcıl fəal günlər zənciri. */
fun enUzunStreak(fealGunler: Set<Long>): Int {
    var enUzun = 0
    fealGunler.forEach { g ->
        // Yalnız zəncirin başlanğıcından say — hər gün bir dəfə keçilir.
        if (g - 1 !in fealGunler) {
            var uz = 0
            while (g + uz in fealGunler) uz++
            enUzun = maxOf(enUzun, uz)
        }
    }
    return enUzun
}

/* ---------- ISO tarix ↔ gün nömrəsi (1970-01-01 = 0) ----------
   Howard Hinnant-ın «days from civil» alqoritmi: yalnız tam ədəd
   hesabı, qriqorian təqvimi, zona yoxdur. */

fun isoGunNomresi(iso: String): Long? {
    val p = iso.split('-')
    if (p.size != 3) return null
    val y0 = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val d = p[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return null
    val y = if (m <= 2) y0 - 1 else y0
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (m + if (m > 2) -3 else 9) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146_097L + doe - 719_468
}

fun gunNomresindenIso(gun: Long): String {
    val z = gun + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val y = yoe + era * 400 + if (m <= 2) 1 else 0
    // Locale.ROOT: ərəb/fars dilində də Avropa rəqəmləri — tarix açardır.
    return String.format(Locale.ROOT, "%04d-%02d-%02d", y, m, d)
}
