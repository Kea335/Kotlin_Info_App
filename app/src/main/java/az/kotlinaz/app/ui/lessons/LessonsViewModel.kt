package az.kotlinaz.app.ui.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.UserPrefs
import az.kotlinaz.app.data.model.Section
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Dərslər (T2.5): bölmə siyahısı, qruplar, «son bölmə» və «oxundu» yazısı.
 * Bölmə mənimsəməsi (`tereqqi`) burada yox, `AppViewModel`-dədir — dörd
 * ekran paylaşır.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LessonsViewModel(
    repo: ContentRepository,
    private val prefs: UserPrefs
) : ViewModel() {

    /**
     * Bölmələr cari məzmun dilində. `repo.dil` dəyişəndə yenidən oxunur
     * (T3.3); id-lər dildən asılı olmadığından açıq dərs yerində qalır.
     * Aktiv AppViewModel tərəfindən artıq oxunub keşlənib — çağırış anidir.
     */
    val sections: StateFlow<List<Section>> =
        repo.dil.mapLatest { repo.sections() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Qrup → bölmələr. `groupBy` sıranı saxlayır (LinkedHashMap), ona görə
     * qruplar saytdakı ardıcıllıqla düzülür: Başlanğıc → Dilin əsasları → … → Yekun.
     */
    val qruplar: StateFlow<Map<String, List<Section>>> =
        sections.map { it.groupBy { s -> s.group } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Sonuncu açılan bölmənin id-si — «Davam et» kartı üçün. */
    val sonBolme: StateFlow<String?> =
        prefs.sonBolme.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun bolme(id: String): Section? = sections.value.firstOrNull { it.id == id }

    /** Dərs açılanda çağırılır — bölmə oxunmuş sayılmasa da mövqe yadda qalır. */
    fun sonBolmeniYaz(id: String) = viewModelScope.launch { prefs.sonBolmeniYaz(id) }

    fun bolmeniOxunmusIsaretle(id: String) =
        viewModelScope.launch { prefs.bolmeniOxunmusIsaretle(id) }
}
