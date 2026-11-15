package az.kotlinaz.app.ui.statistika

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kotlinaz.app.data.oyrenme.GunlukFealiyyet
import az.kotlinaz.app.data.oyrenme.OyrenmeRepository
import az.kotlinaz.app.data.oyrenme.QuizTuru
import az.kotlinaz.app.data.oyrenme.gunTarixi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Statistika ekranının Room datası (T5.4). Tərəqqi (dərslər, çalışmalar,
 * səviyyə) AppViewModel-dədir; hesablamanın özü saf `statistikaHesabla`-dır.
 */
class StatistikaViewModel(
    oyrenme: OyrenmeRepository,
    saat: () -> Long = System::currentTimeMillis
) : ViewModel() {

    /** Yerli gün — ekran açılanda bir dəfə; gecə yarısı açıq qalsa növbəti açılışda yenilənir. */
    val bugun: String = gunTarixi(saat())

    /** null = hələ oxunmayıb. */
    val turlar: StateFlow<List<QuizTuru>?> =
        oyrenme.quizTurlari.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val fealiyyet: StateFlow<List<GunlukFealiyyet>?> =
        oyrenme.fealiyyet.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
