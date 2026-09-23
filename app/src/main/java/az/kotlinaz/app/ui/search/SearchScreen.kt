package az.kotlinaz.app.ui.search

import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import az.kotlinaz.app.R
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
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.kotlinaz.app.data.SearchNetice
import az.kotlinaz.app.ui.theme.KAz

/** Oflayn tam mətn axtarışı — bütün 32 bölmə üzrə. */
@Composable
fun SearchScreen(
    vm: SearchViewModel,
    modifier: Modifier = Modifier,
    onSection: (String) -> Unit
) {
    // Sorğu mətni ViewModel-in SavedStateHandle-ındadır — döndərmə və proses
    // ölümündən sağ çıxır (T2.4); nəticələr orada hesablanır.
    val sorgu by vm.sorgu.collectAsStateWithLifecycle()
    val neticeler by vm.neticeler.collectAsStateWithLifecycle()
    SearchContent(sorgu, neticeler, modifier, onSorgu = { vm.sorguDeyisdi(it) }, onSection = onSection)
}

/** Saf render — bütün vəziyyət parametrlə gəlir. */
@Composable
private fun SearchContent(
    sorgu: String,
    neticeler: List<SearchNetice>,
    modifier: Modifier = Modifier,
    onSorgu: (String) -> Unit,
    onSection: (String) -> Unit
) {
    val c = KAz.colors
    val fokus = remember { FocusRequester() }

    // DÜZƏLİŞ: FocusRequester yaradılıb sahəyə bağlanmışdı, amma heç vaxt
    // requestFocus() çağırılmırdı — yəni axtarış ekranı açılanda kursor da,
    // klaviatura da gəlmirdi, istifadəçi əlavə bir dəfə toxunmalı olurdu.
    LaunchedEffect(Unit) { fokus.requestFocus() }

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
                        text = stringResource(R.string.axtaris_ipucu),
                        color = c.textFaint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                BasicTextField(
                    value = sorgu,
                    onValueChange = onSorgu,
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
                    contentDescription = stringResource(R.string.axtaris_temizle),
                    tint = c.textFaint,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button) { onSorgu("") }
                        .size(18.dp)
                )
            }
        }

        when {
            sorgu.trim().length < 2 -> Mesaj(
                stringResource(R.string.axtaris_qisa)
            )

            neticeler.isEmpty() -> Mesaj(stringResource(R.string.axtaris_tapilmadi, sorgu))

            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = pluralStringResource(R.plurals.axtaris_netice_sayi, neticeler.size, neticeler.size),
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
                            .clickable(role = Role.Button) { onSection(n.id) }
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
