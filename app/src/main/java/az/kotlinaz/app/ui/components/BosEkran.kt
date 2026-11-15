package az.kotlinaz.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import az.kotlinaz.app.ui.theme.KAz

/** «Bölmə tapılmadı» kimi boş hal — ünvandakı id məzmunda yoxdursa. */
@Composable
fun BosEkran(metn: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(metn, color = KAz.colors.textFaint)
    }
}
