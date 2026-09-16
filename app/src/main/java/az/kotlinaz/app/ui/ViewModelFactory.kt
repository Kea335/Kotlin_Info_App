package az.kotlinaz.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import az.kotlinaz.app.KotlinAzApp
import az.kotlinaz.app.data.AppContainer

/** `CreationExtras`-dan tətbiqin asılılıq qrafına çıxış. */
val CreationExtras.konteyner: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KotlinAzApp).container

/**
 * Bütün ViewModel-lərin factory-si — hər biri üçün bir `initializer`.
 * Composable-da: `viewModel(factory = AppViewModelFactory.Factory)`.
 */
object AppViewModelFactory {
    val Factory = viewModelFactory {
        initializer {
            val c = konteyner
            AppViewModel(c.contentRepository, c.userPrefs, c.compiler)
        }
    }
}
