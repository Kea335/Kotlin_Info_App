package az.kotlinaz.app.data

import az.kotlinaz.app.data.model.Level
import az.kotlinaz.app.data.oyrenme.GunlukFealiyyet
import az.kotlinaz.app.data.oyrenme.QuizTuru
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `statistikaHesabla` və seriya qaydaları (T5.4) — sabit tarixlə. */
class StatistikaTest {

    private val bugun = "2026-11-02"
    private fun g(iso: String) = isoGunNomresi(iso)!!

    private val tereqqi = Tereqqi(
        bolmeler = emptyMap(),
        seviyyeler = mapOf(
            Level.JUNIOR to SeviyyeSayi(8, 10),
            Level.MIDDLE to SeviyyeSayi(1, 10),
            Level.SENIOR to SeviyyeSayi(0, 5)
        ),
        tamamlananDers = 3,
        umumiDers = 32,
        seviyye = Level.JUNIOR
    )

    /* ---------- Seriya ---------- */

    @Test
    fun `streak bu gunle davam edir`() {
        assertEquals(3, streakHesabla(setOf(g("2026-10-31"), g("2026-11-01"), g("2026-11-02")), g(bugun)))
    }

    @Test
    fun `streak dunen fealiyyetle davam edir`() {
        // Bu gün hələ heç nə həll olunmayıb — seriya qırılmayıb.
        assertEquals(2, streakHesabla(setOf(g("2026-10-31"), g("2026-11-01")), g(bugun)))
    }

    @Test
    fun `streak iki gun bosluqda sifirlanir`() {
        assertEquals(0, streakHesabla(setOf(g("2026-10-30"), g("2026-10-31")), g(bugun)))
    }

    @Test
    fun `bos fealiyyet listi streak sifir verir`() {
        val s = statistikaHesabla(tereqqi, emptyList(), emptyList(), bugun)
        assertEquals(0, s.streak)
        assertEquals(0, s.enUzunStreak)
    }

    @Test
    fun `en uzun streak kecmisdeki zenciri tapir`() {
        val gunler = setOf(
            g("2026-09-01"), g("2026-09-02"), g("2026-09-03"), g("2026-09-04"),
            g("2026-10-10"), g("2026-11-02")
        )
        assertEquals(4, enUzunStreak(gunler))
        assertEquals(1, streakHesabla(gunler, g(bugun)))
    }

    @Test
    fun `ay ve il sərhədindən kecen seriya`() {
        val gunler = setOf(g("2025-12-30"), g("2025-12-31"), g("2026-01-01"), g("2026-01-02"))
        assertEquals(4, streakHesabla(gunler, g("2026-01-02")))
    }

    @Test
    fun `sifir hellli gun fealiyyet sayilmir`() {
        val s = statistikaHesabla(
            tereqqi, emptyList(),
            listOf(GunlukFealiyyet("2026-11-01", 0), GunlukFealiyyet("2026-11-02", 3)),
            bugun
        )
        assertEquals(1, s.streak)
    }

    /* ---------- Qrafik pəncərələri ---------- */

    @Test
    fun `pencereler bos gunleri sifirla doldurur, kohneden yeniye`() {
        val s = statistikaHesabla(tereqqi, emptyList(), listOf(GunlukFealiyyet("2026-10-30", 2)), bugun)
        assertEquals(30, s.son30Gun.size)
        assertEquals(7, s.son7Gun.size)
        assertEquals("2026-10-04", s.son30Gun.first().tarix)
        assertEquals("2026-11-02", s.son30Gun.last().tarix)
        assertEquals(2, s.son7Gun.single { it.tarix == "2026-10-30" }.say)
        assertEquals(2, s.son30Gun.sumOf { it.say })
    }

    @Test
    fun `bos datada qrafik hamisi sifir`() {
        val s = statistikaHesabla(tereqqi, emptyList(), emptyList(), bugun)
        assertTrue(s.son30Gun.all { it.say == 0 })
        assertEquals(30, s.son30Gun.size)
    }

    /* ---------- Sayğaclar və quiz ---------- */

    @Test
    fun `sayqaclar tereqqiden`() {
        val s = statistikaHesabla(tereqqi, emptyList(), emptyList(), bugun)
        assertEquals(SeviyyeSayi(3, 32), s.dersler)
        assertEquals(SeviyyeSayi(9, 25), s.calismalar)
        assertEquals(SeviyyeSayi(8, 10), s.seviyyeUzre[Level.JUNIOR])
    }

    @Test
    fun `quiz tarixcesi son 10 tur, rekord rejim uzre vurgulanir`() {
        // Ən təzəsi əvvəl (repo belə qaytarır): bal 12, 11, … 1.
        val turlar = (12 downTo 1).map { QuizTuru(if (it % 2 == 0) "junior" else "middle", it, 15, it.toLong()) }
        val s = statistikaHesabla(tereqqi, turlar, emptyList(), bugun)
        assertEquals(10, s.quizTarixcesi.size)
        // junior-un ən yüksəyi 12, middle-ın 11 — hər rejimdə bir rekord.
        assertEquals(listOf(12, 11), s.quizTarixcesi.filter { it.rekorddur }.map { it.tur.bal })

        // Rekord bütün tarixçə üzrədir: görünən 10 turdan kənarda qalan yüksək bal vurğunu götürür.
        val kohneRekord = turlar.drop(2) + QuizTuru("junior", 15, 15, 0)
        assertTrue(statistikaHesabla(tereqqi, kohneRekord, emptyList(), bugun).quizTarixcesi.none { it.rekorddur && it.tur.rejim == "junior" })
    }

    /* ---------- Tarix çevirmələri ---------- */

    @Test
    fun `iso gun nomresi gedis-qayidis`() {
        assertEquals(0L, isoGunNomresi("1970-01-01"))
        assertEquals("2024-02-29", gunNomresindenIso(isoGunNomresi("2024-02-29")!!))
        assertEquals(g("2024-03-01") - 1, g("2024-02-29"))
        listOf("2026-01-01", "2000-12-31", "1999-03-01", "2100-02-28").forEach {
            assertEquals(it, gunNomresindenIso(isoGunNomresi(it)!!))
        }
    }

    @Test
    fun `zedeli tarix null`() {
        assertNull(isoGunNomresi("2026/11/02"))
        assertNull(isoGunNomresi("2026-13-01"))
        assertNull(isoGunNomresi(""))
    }
}
