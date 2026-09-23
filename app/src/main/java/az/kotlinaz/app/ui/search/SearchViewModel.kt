package az.kotlinaz.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.AxtarisMotoru
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.SearchNetice
import az.kotlinaz.app.data.axtarisSenedleri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Oflayn axtarış (T2.5). İndeks məzmun dili üzrə burada qurulur; sorğu
 * [SavedStateHandle]-dadır, nəticələr sorğu dəyişəndə hesablanır.
 *
 * Axtarışın özü [AxtarisMotoru]-dadır (saf sinif, testdən çağırıla bilir).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    repo: ContentRepository,
    private val state: SavedStateHandle,
    /** İndeks bu dispatcher-də qurulur — UI sapı bloklanmır; testdə test dispatcher-i. */
    hesab: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    val sorgu: StateFlow<String> = state.getStateFlow(SORGU, "")

    // İndeks qurulana qədər null — nəticə boşdur. İndeks göstərilən məzmunun
    // özündən qurulur (T3.5): dil dəyişəndə (T3.3) həmin dilin bölmələrindən və
    // həmin dilin normallaşdırma qaydası ilə yenidən — köhnə dilin nəticəsi qalmır.
    private val motor: StateFlow<AxtarisMotoru?> =
        repo.dil.mapLatest { teq ->
            val bolmeler = repo.sections()
            withContext(hesab) { AxtarisMotoru(axtarisSenedleri(bolmeler), dilTeqi = teq) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val neticeler: StateFlow<List<SearchNetice>> =
        combine(motor, sorgu) { m, s -> m?.axtar(s).orEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun sorguDeyisdi(metn: String) { state[SORGU] = metn }

    private companion object {
        const val SORGU = "sorgu"
    }
}
