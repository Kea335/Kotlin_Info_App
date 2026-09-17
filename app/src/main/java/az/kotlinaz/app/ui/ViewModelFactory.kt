package az.kotlinaz.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import az.kotlinaz.app.KotlinAzApp
import az.kotlinaz.app.data.AppContainer
import az.kotlinaz.app.ui.lessons.LessonsViewModel
import az.kotlinaz.app.ui.quiz.QuizViewModel
import az.kotlinaz.app.ui.search.SearchViewModel
import az.kotlinaz.app.ui.settings.SettingsViewModel
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
            AppViewModel(c.contentRepository, c.userPrefs, c.compiler)
        }
        initializer {
            PlaygroundViewModel(konteyner.compiler, createSavedStateHandle())
        }
        initializer {
            val c = konteyner
            ExerciseViewModel(c.compiler, c.userPrefs, createSavedStateHandle())
        }
        initializer {
            val c = konteyner
            QuizViewModel(c.contentRepository, c.userPrefs, createSavedStateHandle())
        }
        initializer {
            val c = konteyner
            LessonsViewModel(c.contentRepository, c.userPrefs)
        }
        initializer {
            SearchViewModel(konteyner.contentRepository, createSavedStateHandle())
        }
        initializer {
            SettingsViewModel(konteyner.userPrefs)
        }
    }
}
