package az.kotlinaz.app.ui.isareler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.ContentRepository
import az.kotlinaz.app.data.oyrenme.BookmarkTipi
import az.kotlinaz.app.data.oyrenme.OyrenmeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Dərs və çalışma ekranlarındakı işarə düymələri və qeydlər (T5.3) —
 * yalnız id-lərlə işləyir, məzmun oxumur.
 */
class IsareViewModel(private val oyrenme: OyrenmeRepository) : ViewModel() {

    val isareliIdler: StateFlow<Set<String>> =
        oyrenme.bookmarklar.map { l -> l.mapTo(HashSet()) { it.hedefId } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** hədəf id → qeyd mətni. */
    val qeydler: StateFlow<Map<String, String>> =
        oyrenme.qeydler.map { l -> l.associate { it.hedefId to it.metn } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun isareDeyis(id: String, tip: BookmarkTipi) {
        viewModelScope.launch { oyrenme.bookmarkDeyis(id, tip) }
    }

    /** Boş mətn qeydi silir. */
    fun qeydYaz(id: String, metn: String) {
        viewModelScope.launch { oyrenme.qeydYaz(id, metn.trim()) }
    }
}

/**
 * «İşarələr və qeydlər» ekranı (T5.3). Başlıq və keçid seçilmiş dilin
 * məzmunundan; qeyd mətni istifadəçinindir, dil dəyişəndə dəyişmir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IsarelerViewModel(
    repo: ContentRepository,
    private val oyrenme: OyrenmeRepository
) : ViewModel() {

    // Məzmun oxunmasa boş xəritə — ekran çökmür, sətirlər sadəcə görünmür.
    private val hedefler = repo.dil.mapLatest {
        try {
            hedefXeritesi(repo.sections(), repo.exercises().topics)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyMap()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** null = hələ yüklənir. */
    val isareler: StateFlow<List<IsareSetri>?> =
        combine(hedefler, oyrenme.bookmarklar) { h, b -> h?.let { isareSetirleri(b, it) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val qeydler: StateFlow<List<QeydSetri>?> =
        combine(hedefler, oyrenme.qeydler) { h, q -> h?.let { qeydSetirleri(q, it) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun isareniSil(id: String, tip: BookmarkTipi) {
        viewModelScope.launch { oyrenme.bookmarkDeyis(id, tip) }
    }

    fun qeydYaz(id: String, metn: String) {
        viewModelScope.launch { oyrenme.qeydYaz(id, metn.trim()) }
    }
}
