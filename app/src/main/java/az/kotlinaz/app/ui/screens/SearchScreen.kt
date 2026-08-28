package az.kotlinaz.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.ui.SearchNetice
import az.kotlinaz.app.ui.theme.KAz

/** Oflayn tam mətn axtarışı — bütün 32 bölmə üzrə. */
@Composable
fun SearchScreen(
    axtar: (String) -> List<SearchNetice>,
    modifier: Modifier = Modifier,
    onSection: (String) -> Unit
) {
    val c = KAz.colors
    var sorgu by remember { mutableStateOf("") }
    val fokus = remember { FocusRequester() }

    // DÜZƏLİŞ: FocusRequester yaradılıb sahəyə bağlanmışdı, amma heç vaxt
    // requestFocus() çağırılmırdı — yəni axtarış ekranı açılanda kursor da,
    // klaviatura da gəlmirdi, istifadəçi əlavə bir dəfə toxunmalı olurdu.
    LaunchedEffect(Unit) { fokus.requestFocus() }

    // Nəticələr sorğu dəyişəndə yenidən hesablanır. Axtarış oflayn indeksdə,
    // əvvəlcədən kiçik hərfə salınmış mətndə gedir (bax: AppViewModel.axtar).
    val neticeler = remember(sorgu) { axtar(sorgu) }

    Column(modifier.fillMaxSize()) {
        // Axtarış sahəsi
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.bgSunken)
                .border(1.dp, c.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = c.textFaint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                // BasicTextField-in öz placeholder-i yoxdur — altda mətn kimi qoyulur.
                if (sorgu.isEmpty()) {
                    Text(
                        text = "Sənəddə axtar…",
                        color = c.textFaint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                BasicTextField(
                    value = sorgu,
                    onValueChange = { sorgu = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = c.text,
                        fontSize = (15 * KAz.codeScale).sp
                    ),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fokus)
                )
            }
            if (sorgu.isNotEmpty()) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Təmizlə",
                    tint = c.textFaint,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { sorgu = "" }
                )
            }
        }

        when {
            sorgu.trim().length < 2 -> Mesaj(
                "Axtarış üçün ən azı iki hərf yaz.\nBütün 32 bölmə oflayn axtarılır."
            )

            neticeler.isEmpty() -> Mesaj("«$sorgu» üzrə heç nə tapılmadı.")

            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "${neticeler.size} bölmədə tapıldı",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(neticeler, key = { it.id }) { n ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.bgElev)
                            .border(1.dp, c.border, RoundedCornerShape(12.dp))
                            .clickable { onSection(n.id) }
                            .padding(13.dp)
                    ) {
                        Text(
                            text = n.group.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textFaint,
                            fontSize = (10 * KAz.codeScale).sp
                        )
                        Text(
                            text = n.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.text
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = n.parca,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = c.textDim,
                            maxLines = 3
                        )
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun Mesaj(metn: String) {
    val c = KAz.colors
    Box(
        Modifier.fillMaxSize().padding(30.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = metn,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = c.textFaint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
