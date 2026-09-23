package az.kotlinaz.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.AxtarisMotoru
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.SearchNetice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Oflayn axtarış (T2.5). İndeks məzmun dili üzrə burada qurulur; sorğu
 * [SavedStateHandle]-dadır, nəticələr sorğu dəyişəndə hesablanır.
 *
 * Axtarışın özü [AxtarisMotoru]-dadır (saf sinif, testdən çağırıla bilir).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    repo: ContentRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    val sorgu: StateFlow<String> = state.getStateFlow(SORGU, "")

    // Sənədlər oxunana qədər null — nəticə boşdur. Dil dəyişəndə (T3.3) indeks
    // həmin dilin search.json-undan və həmin dilin kiçiltmə qaydası ilə
    // yenidən qurulur: `İ`/`ı` düzəlişi (T0.1) yalnız az/tr üçün lazımdır.
    private val motor: StateFlow<AxtarisMotoru?> =
        repo.dil.mapLatest { teq -> AxtarisMotoru(repo.searchIndex().docs, dilTeqi = teq) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val neticeler: StateFlow<List<SearchNetice>> =
        combine(motor, sorgu) { m, s -> m?.axtar(s).orEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun sorguDeyisdi(metn: String) { state[SORGU] = metn }

    private companion object {
        const val SORGU = "sorgu"
    }
}
