package az.kotlinaz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.kotlinaz.app.ui.AppRoot
import az.kotlinaz.app.ui.AppViewModel
import az.kotlinaz.app.ui.theme.KotlinAzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val vm: AppViewModel = viewModel()
            val tema by vm.tema.collectAsStateWithLifecycle()
            val srift by vm.sriftOlcusu.collectAsStateWithLifecycle()

            KotlinAzTheme(
                mode = tema,
                codeScale = when (srift) {
                    0 -> 0.88f
                    2 -> 1.15f
                    else -> 1f
                }
            ) {
                AppRoot(vm)
            }
        }
    }
}
