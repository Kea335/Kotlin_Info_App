package az.kotlinaz.app.ui

import az.kotlinaz.app.data.AppDil
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.FakeContentRepository
import az.kotlinaz.app.data.KotlinCompiler
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.YaddasDataStore
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [AppViewModel] fake asılılıqlarla (T2.1): `Context` yoxdur, aktiv yoxdur —
 * ViewModel yalnız [ContentRepository] interfeysini və yaddaşdakı
 * [UserPrefs]-i görür.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun sifirla() = Dispatchers.resetMain()

    private fun vm(
        repo: ContentRepository,
        prefs: UserPrefs = UserPrefs(YaddasDataStore()),
        sistemDilleri: () -> List<String> = { emptyList() }
    ) = AppViewModel(
        repo, prefs,
        KotlinCompiler(prefs, sebekeVar = { false }, io = dispatcher),
        sistemDilleri = sistemDilleri
    )

    @Test
    fun `fake repository ile bolmeler yuklenir ve hazir olur`() = runTest(dispatcher) {
        val repo = FakeContentRepository(
            bolmeler = listOf(Section(id = "intro", index = 0, title = "Giriş")),
            movzular = listOf(ExerciseTopic(id = "intro", title = "Giriş"))
        )
        val vm = vm(repo)

        assertTrue(vm.hazir.value)
        assertTrue(vm.movzularHazir.value)
        assertNull(vm.yukleneXetasi.value)
        assertEquals(listOf("intro"), vm.sections.value.map { it.id })
        assertNotNull(vm.bolme("intro"))
        assertNotNull(vm.movzu("intro"))
        assertNull(vm.bolme("yoxdur"))
    }

    @Test
    fun `yuklenme xetasi hazir bayragini bloklamir`() = runTest(dispatcher) {
        val vm = vm(FakeContentRepository(xeta = IllegalStateException("aktiv pozulub")))

        // `finally` qaydası: xəta olsa da fırlanğıc bitməlidir, səbəb görünməlidir.
        assertTrue(vm.hazir.value)
        assertEquals("aktiv pozulub", vm.yukleneXetasi.value)
        assertTrue(vm.sections.value.isEmpty())
    }

    @Test
    fun `ikinci merhelenin xetasi da yukleneXetasi-ye yazilir, ekran acilir`() = runTest(dispatcher) {
        val vm = vm(
            FakeContentRepository(
                bolmeler = listOf(Section(id = "intro", index = 0, title = "Giriş")),
                calismaXetasi = IllegalStateException("exercises.json pozulub")
            )
        )

        // Dərslər ekranı açılır (content.json sağlamdır), xəta isə görünür.
        assertTrue(vm.hazir.value)
        assertTrue(vm.movzularHazir.value)
        assertEquals(listOf("intro"), vm.sections.value.map { it.id })
        assertEquals("exercises.json pozulub", vm.yukleneXetasi.value)
        assertTrue(vm.topics.value.isEmpty())
    }

    @Test
    fun `tereqqi prefs-deki hell edilmis coxlugundan hesablanir`() = runTest(dispatcher) {
        val prefs = UserPrefs(YaddasDataStore())
        val vm = vm(FakeContentRepository(), prefs)

        assertTrue(vm.hellEdilmis.value.isEmpty())
        prefs.calismaniHellIsaretle("intro-t-1")
        assertEquals(setOf("intro-t-1"), vm.hellEdilmis.value)

        prefs.hamisiniSifirla()
        assertTrue(vm.hellEdilmis.value.isEmpty())
    }

    /* ---------- Dil (T3.3) ---------- */

    // Eyni id, fərqli başlıq — tərcümə overlay-i belə işləyir (T3.1/T3.2).
    private val azBolmeler = listOf(
        Section(id = "intro", index = 0, title = "Giriş"),
        Section(id = "vars", index = 1, title = "Dəyişənlər")
    )
    private val enBolmeler = listOf(
        Section(id = "intro", index = 0, title = "Intro"),
        Section(id = "vars", index = 1, title = "Variables")
    )
    private fun ikiDilliRepo() = FakeContentRepository(
        movzular = listOf(ExerciseTopic(id = "intro", title = "Giriş")),
        diller = listOf("az", "en"),
        dilUzreBolmeler = mapOf("az" to azBolmeler, "en" to enBolmeler)
    )
    /** Azərbaycan dilli cihaz — «Sistem dili» seçimi bazaya çözülsün. */
    private val azCihaz: () -> List<String> = { listOf("az-AZ") }

    @Test
    fun `dil deyisende mezmun yeniden oxunur, tereqqi eyni qalir`() = runTest(dispatcher) {
        val prefs = UserPrefs(YaddasDataStore())
        val repo = ikiDilliRepo()
        val vm = vm(repo, prefs, azCihaz)
        prefs.calismaniHellIsaretle("intro-t-1")
        prefs.bolmeniOxunmusIsaretle("vars")
        val evvelkiTereqqi = vm.tereqqi.value
        assertEquals("az", vm.mezmunDili.value)
        assertEquals(listOf("Giriş", "Dəyişənlər"), vm.sections.value.map { it.title })

        prefs.dilSec(AppDil.EN)

        assertEquals("en", vm.mezmunDili.value)
        assertEquals("en", repo.dil.value)
        assertEquals(listOf("Intro", "Variables"), vm.sections.value.map { it.title })
        assertEquals(listOf("intro", "vars"), vm.sections.value.map { it.id })
        // Tərəqqi id-lərə bağlıdır — dil dəyişəndə toxunulmur.
        assertEquals(evvelkiTereqqi, vm.tereqqi.value)
        assertEquals(setOf("intro-t-1"), vm.hellEdilmis.value)
        assertTrue(vm.hazir.value)
        assertNull(vm.yukleneXetasi.value)
    }

    @Test
    fun `tercumesi olmayan dil secilende en, o da yoxdursa az qalir`() = runTest(dispatcher) {
        val prefs = UserPrefs(YaddasDataStore())
        val repo = ikiDilliRepo()
        val vm = vm(repo, prefs, azCihaz)
        val evvel = repo.oxunmaSayi

        prefs.dilSec(AppDil.TR)
        assertEquals("en", vm.mezmunDili.value)
        assertEquals(listOf("Intro", "Variables"), vm.sections.value.map { it.title })

        // Eyni məzmun dilinə çözülən başqa seçim — yenidən oxunmur.
        val enOxunma = repo.oxunmaSayi
        prefs.dilSec(AppDil.DE)
        assertEquals("en", vm.mezmunDili.value)
        assertEquals(enOxunma, repo.oxunmaSayi)
        assertTrue(repo.oxunmaSayi > evvel)

        // Yalnız baza qurulubsa hər seçim bazaya düşür.
        val tekDilli = FakeContentRepository(bolmeler = azBolmeler)
        val vm2 = vm(tekDilli, prefs, azCihaz)
        assertEquals("az", vm2.mezmunDili.value)
        assertEquals("Giriş", vm2.sections.value.first().title)
    }

    @Test
    fun `sistem dili secimi cihazin siyahisindan cozulur ve yenilene bilir`() = runTest(dispatcher) {
        val repo = ikiDilliRepo()
        var cihaz = listOf("tr-TR", "en-US")
        val vm = vm(repo, sistemDilleri = { cihaz })
        assertEquals("en", vm.mezmunDili.value)

        // Cihazın dili dəyişdi (Activity yenidən yaranır) — seçim hələ «sistem»dir.
        cihaz = listOf("az-AZ")
        vm.sistemDiliniYenile()
        assertEquals("az", vm.mezmunDili.value)
        assertEquals("Giriş", vm.sections.value.first().title)
    }

    @Test
    fun `meydana gonderilen kod oxunub temizlenir`() = runTest(dispatcher) {
        val vm = vm(FakeContentRepository())
        assertNull(vm.meydanKodu.value)
        vm.meydanaGonder("println(1)")
        assertEquals("println(1)", vm.meydanKodu.value)
        vm.meydanKodunuTemizle()
        assertNull(vm.meydanKodu.value)
    }
}
