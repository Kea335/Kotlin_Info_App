package az.kotlinaz.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import az.kotlinaz.app.ui.theme.KAz

/** Ekranın məzmunu hələ oxunur — boş siyahı əvəzinə fırlanğıc (T2.6). */
@Composable
fun YuklenmeGostericisi(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = KAz.colors.accent)
    }
}
