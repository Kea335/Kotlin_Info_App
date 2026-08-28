package az.kotlinaz.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.kotlinaz.app.data.ThemeMode
import az.kotlinaz.app.ui.AppRoot
import az.kotlinaz.app.ui.AppViewModel
import az.kotlinaz.app.ui.theme.KotlinAzTheme

/** Tətbiqin yeganə Activity-si — bütün ekranlar Compose naviqasiyası ilə onun içindədir. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Kənardan-kənara rejim: məzmun status və naviqasiya zolaqlarının
        // altına da uzanır. super.onCreate()-dən əvvəl çağırılır ki, pəncərə
        // ilk kadrdan düzgün qurulsun.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val vm: AppViewModel = viewModel()

            // Tema, şrift ölçüsü və tamamlama tənzimləməsi DataStore-dadır — cihazda qalır.
            val tema by vm.tema.collectAsStateWithLifecycle()
            val srift by vm.sriftOlcusu.collectAsStateWithLifecycle()
            val tamamlama by vm.kodTamamlama.collectAsStateWithLifecycle()

            // Tətbiqin ƏSL rejimi: «Sistem» seçilibsə cihazın rejimi, əks halda
            // istifadəçinin seçimi. Aşağıdakı sistem zolaqları da bunu izləməlidir.
            val qaranliq = when (tema) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // DÜZƏLİŞ: enableEdgeToEdge() defolt olaraq zolaq ikonlarının rəngini
            // CİHAZIN rejiminə görə seçir, tətbiqin seçiminə görə yox. Sistem işıqlı,
            // tətbiq isə qaranlıq olanda status zolağının ikonları qara fon üzərində
            // qara qalırdı — yəni görünmürdü. Rejim dəyişən kimi zolaq üslubunu
            // yenidən elan edirik; detectDarkMode həmişə tətbiqin öz rejimini qaytarır.
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { qaranliq },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { qaranliq }
                )
            }

            KotlinAzTheme(
                mode = tema,
                // Tənzimləmələrdəki «Yazı şrifti» seçimi: 0 kiçik, 1 normal, 2 böyük.
                // Katsayı bütün tipoqrafiyaya və kod bloklarına eyni nisbətdə düşür.
                yaziOlcusu = when (srift) {
                    0 -> 0.88f
                    2 -> 1.18f
                    else -> 1f
                },
                kodTamamlama = tamamlama
            ) {
                AppRoot(vm)
            }
        }
    }
}
