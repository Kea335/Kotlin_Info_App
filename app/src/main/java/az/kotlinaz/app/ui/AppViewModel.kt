package az.kotlinaz.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.AssetContentRepository
import az.kotlinaz.app.data.CompilerRepository
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.KompilyatorVeziyyeti
import az.kotlinaz.app.data.Tereqqi
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.mezmunDiliSec
import az.kotlinaz.app.data.tereqqiHesabla
import az.kotlinaz.app.data.model.ExerciseTopic
import az.kotlinaz.app.data.model.PlaygroundPreset
import az.kotlinaz.app.data.model.Section
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tətbiq səviyyəsində paylaşılan vəziyyət: aktiv yükləmə (`hazir`,
 * `yukleneXetasi`), məzmun siyahıları (başlıq və keçidlər üçün), tərəqqi
 * mənzərəsi və meydana ötürülən kod.
 *
 * Məzmun dili də burada çözülür (T3.3): `UserPrefs.dil` seçimi (və ya sistem
 * dilləri) mövcud qovluqlarla tutuşdurulur, nəticə [ContentRepository.dilDeyisdi]
 * ilə repository-yə verilir və aktivlər yenidən oxunur. Ekran ViewModel-ləri
 * `repo.dil`-i izləyib öz məzmununu özləri təzələyir.
 *
 * Ekran məsuliyyətləri öz ViewModel-lərindədir (T2.5): dərslər, çalışmalar,
 * test, meydan, axtarış, tənzimləmələr. Asılılıqlar konstruktorla gəlir
 * ([AppViewModelFactory] → `AppContainer`): `Context` yoxdur, testdə fake
 * repository ötürülür (T2.1).
 */
class AppViewModel(
    private val repo: ContentRepository,
    prefs: UserPrefs,
    private val compiler: CompilerRepository,
    /** Cihazın dil siyahısı, üstünlük sırası ilə — «Sistem dili» seçimi üçün. */
    private val sistemDilleri: () -> List<String> = { emptyList() }
) : ViewModel() {

    /** Aktiv kompilyator versiyası — Meydan zolağı və Tənzimləmələr üçün. İcra özü ekran ViewModel-lərindədir. */
    val kompilyator: StateFlow<KompilyatorVeziyyeti> = compiler.veziyyet

    /**
     * Cari məzmun dili — seçimdən çözülmüş, mövcud qovluğa uyğun teq.
     * Seçimlə fərqlənə bilər: «tr» seçilib, tərcümə yoxdursa «az» qalır.
     */
    val mezmunDili: StateFlow<String> = repo.dil

    // Sistem dili dəyişəndə Activity yenidən yaranır, seçim isə eyni qalır —
    // sayğac artırılır ki, `combine` sistem siyahısını təzədən oxusun.
    private val sistemYenilenmesi = MutableStateFlow(0)

    private val _sections = MutableStateFlow<List<Section>>(emptyList())
    val sections: StateFlow<List<Section>> = _sections.asStateFlow()

    private val _topics = MutableStateFlow<List<ExerciseTopic>>(emptyList())
    val topics: StateFlow<List<ExerciseTopic>> = _topics.asStateFlow()

    private val _presets = MutableStateFlow<List<PlaygroundPreset>>(emptyList())
    val presets: StateFlow<List<PlaygroundPreset>> = _presets.asStateFlow()

    // İlk mərhələ (content.json) oxunub bitəndə true olur — AppRoot ona qədər
    // fırlanğıc göstərir. Qalan aktivlər ekranı bloklamadan arxada gəlir (T2.6).
    private val _hazir = MutableStateFlow(false)
    val hazir: StateFlow<Boolean> = _hazir.asStateFlow()

    // İkinci mərhələ (çalışma bankı, meydan, axtarış) bitəndə true olur.
    // Çalışmalar ekranı ona qədər boş siyahı yox, fırlanğıc göstərir.
    private val _movzularHazir = MutableStateFlow(false)
    val movzularHazir: StateFlow<Boolean> = _movzularHazir.asStateFlow()

    // Aktiv oxunuşu sınarsa səbəbi burada qalır. Əvvəllər belə hal yalnız
    // sonsuz fırlanğıc kimi görünürdü — indi ekranda izahı çıxır.
    private val _yukleneXetasi = MutableStateFlow<String?>(null)
    val yukleneXetasi: StateFlow<String?> = _yukleneXetasi.asStateFlow()

    private val oxunanBolmeler: StateFlow<Set<String>> =
        prefs.oxunanBolmeler.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val hellEdilmis: StateFlow<Set<String>> =
        prefs.hellEdilmis.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /**
     * Tərəqqi mənzərəsi — bölmə mənimsəməsi və səviyyə.
     *
     * Dörd mənbədən yığılır, ona görə `combine`: məzmun gec gəlir, tərəqqi isə
     * istifadəçi çalışma həll etdikcə dəyişir. Nəticə tək yerdə hesablanır —
     * dərslər, çalışmalar və tənzimləmələr ekranları eyni rəqəmi görür.
     */
    val tereqqi: StateFlow<Tereqqi> = combine(
        sections, topics, hellEdilmis, oxunanBolmeler
    ) { bolmeler, movzular, hell, oxunan ->
        tereqqiHesabla(bolmeler, movzular, hell, oxunan)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        tereqqiHesabla(emptyList(), emptyList(), emptySet(), emptySet())
    )

    /** Kod meydanına göndərilən kod (kod kartındakı «Meydan» düyməsi). */
    private val _meydanKodu = MutableStateFlow<String?>(null)
    val meydanKodu: StateFlow<String?> = _meydanKodu.asStateFlow()

    init {
        // Kompilyator versiyasının keşi diskdən oxunur (şəbəkəsiz) — Tənzimləmələr
        // ilk açılışdan düzgün nömrəni göstərsin. Dilə bağlı deyil, bir dəfədir.
        viewModelScope.launch {
            try {
                compiler.hazirla()
            } catch (e: Exception) {
                _yukleneXetasi.value = e.message ?: e.javaClass.simpleName
            }
        }

        viewModelScope.launch {
            // Mövcud dillər bir dəfə oxunur; siyahı oxunmasa baza ilə davam
            // edilir — dil seçimi məzmunun özünü bloklamamalıdır.
            val movcud = try {
                repo.diller()
            } catch (_: Exception) {
                listOf(AssetContentRepository.BAZA_LOCALE)
            }
            combine(prefs.dil, sistemYenilenmesi) { secim, _ ->
                mezmunDiliSec(secim?.teq, sistemDilleri(), movcud)
            }
                .distinctUntilChanged()
                // collectLatest: dil ard-arda dəyişsə yarımçıq yükləmə ləğv
                // olunur — köhnə dilin siyahısı yeninin üstünə yazılmır.
                .collectLatest { teq ->
                    repo.dilDeyisdi(teq)
                    yukle()
                }
        }
    }

    /**
     * Aktivləri cari dildə oxuyur. İlk açılışda `hazir` bayrağını qaldırır;
     * dil dəyişəndə bayraqlar artıq qalxıb — köhnə məzmun yeni gələnə qədər
     * ekranda qalır, sonra yerində əvəzlənir (id-lər dildən asılı deyil, T3.1).
     */
    private suspend fun yukle() = coroutineScope {
        _yukleneXetasi.value = null

        // Mərhələ 1: yalnız ilk ekrana (Dərslər) lazım olan content.json.
        // 547 KB-lıq çalışma bankı istifadəçi «Çalışmalar»a toxunana qədər
        // gözləyə bilər — əvvəl beş aktiv ardıcıl oxunur, hamısı bitməyincə
        // fırlanğıc qalırdı (T2.6).
        launch {
            // DÜZƏLİŞ: əvvəl bu blokda try/catch yox idi. Hər hansı aktiv oxunmasa
            // (məsələn release qurulusunda R8 serializatoru silsə) korutin çökür,
            // `hazir` heç vaxt true olmur və tətbiq əbədi fırlanğıcda ilişirdi.
            // İndi səbəb `yukleneXetasi`-yə yazılır və istifadəçi onu görür.
            try {
                _sections.value = repo.sections()
            } catch (e: CancellationException) {
                // Dil dəyişdi — yeni yükləmə gəlir, bayraqlara toxunulmur.
                throw e
            } catch (e: Exception) {
                _yukleneXetasi.value = e.message ?: e.javaClass.simpleName
            }
            // Xəta olsa da ekran fırlanğıcdan çıxmalıdır.
            _hazir.value = true
        }

        // Mərhələ 2: qalan aktivlər paralel, ekranı bloklamadan. Repository-də
        // hər aktivin öz kilidi var, ona görə async-lər həqiqətən yanaşı gedir.
        launch {
            try {
                // coroutineScope: bir aktiv sınsa xəta buradaca tutulur,
                // viewModelScope-a yayılmır.
                coroutineScope {
                    val movzular = async { repo.exercises().topics }
                    val numuneler = async { repo.playground().presets }
                    // Axtarış indeksi aktiv deyil — SearchViewModel onu artıq
                    // yüklənmiş bölmələrdən özü qurur (T3.5).
                    _topics.value = movzular.await()
                    _presets.value = numuneler.await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _yukleneXetasi.value = e.message ?: e.javaClass.simpleName
            }
            _movzularHazir.value = true
        }
    }

    /**
     * Activity yenidən yarananda çağırılır: sistem dili dəyişibsə və seçim
     * «Sistem dili»dirsə məzmun qovluğu da dəyişməlidir. Dəyişməyibsə
     * `distinctUntilChanged` heç nə etmir.
     */
    fun sistemDiliniYenile() { sistemYenilenmesi.value++ }

    fun bolme(id: String): Section? = _sections.value.firstOrNull { it.id == id }

    fun movzu(id: String): ExerciseTopic? = _topics.value.firstOrNull { it.id == id }

    fun meydanaGonder(kod: String) { _meydanKodu.value = kod }

    fun meydanKodunuTemizle() { _meydanKodu.value = null }
}
