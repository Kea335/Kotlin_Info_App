package az.kotlinaz.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.AxtarisMotoru
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.SearchNetice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Oflayn axtarış (T2.5). İndeks bir dəfə, burada qurulur; sorğu
 * [SavedStateHandle]-dadır, nəticələr sorğu dəyişəndə hesablanır.
 *
 * Axtarışın özü [AxtarisMotoru]-dadır (saf sinif, testdən çağırıla bilir).
 */
class SearchViewModel(
    repo: ContentRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    val sorgu: StateFlow<String> = state.getStateFlow(SORGU, "")

    // Sənədlər oxunana qədər null — nəticə boşdur.
    private val motor = MutableStateFlow<AxtarisMotoru?>(null)

    val neticeler: StateFlow<List<SearchNetice>> =
        combine(motor, sorgu) { m, s -> m?.axtar(s).orEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            // Dil teqi hələlik sabit «az»-dır; dil seçimi gələndə (T3.3)
            // seçilmiş dildən gələcək.
            motor.value = AxtarisMotoru(repo.searchIndex().docs, dilTeqi = "az")
        }
    }

    fun sorguDeyisdi(metn: String) { state[SORGU] = metn }

    private companion object {
        const val SORGU = "sorgu"
    }
}
