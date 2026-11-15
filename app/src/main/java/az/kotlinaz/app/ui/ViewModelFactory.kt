package az.kotlinaz.app.ui

import az.kotlinaz.app.ui.statistika.StatistikaViewModel
import az.kotlinaz.app.ui.isareler.IsareViewModel
import az.kotlinaz.app.ui.isareler.IsarelerViewModel
import az.kotlinaz.app.ui.sehvlerim.SehvlerimViewModel
import kotlinx.coroutines.flow.map
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import az.kotlinaz.app.BuildConfig
import az.kotlinaz.app.KotlinAzApp
import az.kotlinaz.app.data.AppContainer
import az.kotlinaz.app.ui.lessons.LessonsViewModel
import az.kotlinaz.app.ui.quiz.QuizViewModel
import az.kotlinaz.app.ui.search.SearchViewModel
import az.kotlinaz.app.ui.settings.SettingsViewModel
import az.kotlinaz.app.ui.settings.appCompatDiliTetbiqEt
import az.kotlinaz.app.ui.exercises.ExerciseViewModel
import az.kotlinaz.app.ui.playground.PlaygroundViewModel

/** `CreationExtras`-dan tətbiqin asılılıq qrafına çıxış. */
val CreationExtras.konteyner: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KotlinAzApp).container

/**
 * Bütün ViewModel-lərin factory-si — hər biri üçün bir `initializer`.
 * Composable-da: `viewModel(factory = AppViewModelFactory.Factory)`.
 *
 * Ekran ViewModel-ləri `SavedStateHandle` alır: naviqasiya girişinə bağlı
 * olduqlarından `createSavedStateHandle()` həmin girişin arqumentlərini və
 * saxlanmış vəziyyətini gətirir.
 */
object AppViewModelFactory {
    val Factory = viewModelFactory {
        initializer {
            val c = konteyner
            AppViewModel(c.contentRepository, c.userPrefs, c.compiler, c.sistemDilleri)
        }
        initializer {
            PlaygroundViewModel(konteyner.compiler, createSavedStateHandle(), konteyner.userPrefs)
        }
        initializer {
            val c = konteyner
            ExerciseViewModel(c.compiler, c.userPrefs, c.oyrenme, createSavedStateHandle())
        }
        initializer {
            val c = konteyner
            QuizViewModel(c.contentRepository, c.userPrefs, c.oyrenme, createSavedStateHandle())
        }
        initializer {
            val c = konteyner
            LessonsViewModel(c.contentRepository, c.userPrefs)
        }
        initializer {
            SearchViewModel(konteyner.contentRepository, createSavedStateHandle())
        }
        initializer {
            val c = konteyner
            SettingsViewModel(
                c.userPrefs,
                // Dil seçimi DataStore-dan əlavə AppCompat-a da yazılır (T3.3).
                dilTetbiqEt = ::appCompatDiliTetbiqEt,
                // Siyahıda yalnız qurulmuş (tam tərcümə olunmuş) dillər; debug-da hamısı (T4.7).
                movcudDiller = c.contentRepository::diller,
                hamisiGorunsun = BuildConfig.DEBUG,
                sehvSayi = c.oyrenme.sehvler.map { it.size }
            )
        }
        initializer {
            val c = konteyner
            SehvlerimViewModel(c.contentRepository, c.oyrenme)
        }
        initializer {
            IsareViewModel(konteyner.oyrenme)
        }
        initializer {
            StatistikaViewModel(konteyner.oyrenme)
        }
        initializer {
            val c = konteyner
            IsarelerViewModel(c.contentRepository, c.oyrenme)
        }
    }
}
