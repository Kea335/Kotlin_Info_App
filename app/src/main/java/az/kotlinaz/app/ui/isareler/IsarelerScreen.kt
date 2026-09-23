package az.kotlinaz.app.ui.isareler

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.kotlinaz.app.R
import az.kotlinaz.app.data.oyrenme.BookmarkTipi
import az.kotlinaz.app.ui.components.YuklenmeGostericisi
import az.kotlinaz.app.ui.demos.DemoChip
import az.kotlinaz.app.ui.theme.KAz

/**
 * «İşarələr və qeydlər» (T5.3): iki tab — işarələnənlər (tip filtri ilə) və
 * qeydlər. Sətrə toxunanda hədəfə keçilir; qeyd redaktə və silinə bilir.
 */
@Composable
fun IsarelerScreen(
    vm: IsarelerViewModel,
    onKecid: (Kecid) -> Unit,
    modifier: Modifier = Modifier
) {
    val isareler by vm.isareler.collectAsStateWithLifecycle()
    val qeydler by vm.qeydler.collectAsStateWithLifecycle()
    // 0 — işarələr, 1 — qeydlər; döndərmədə qalsın.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // null = hamısı.
    var filtr by rememberSaveable { mutableStateOf<BookmarkTipi?>(null) }
    var redakte by rememberSaveable { mutableStateOf<String?>(null) }

    val i = isareler
    val q = qeydler
    if (i == null || q == null) {
        YuklenmeGostericisi(modifier)
        return
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Tab(stringResource(R.string.isareler_tab_isareler, i.size), tab == 0, Modifier.weight(1f)) { tab = 0 }
            Tab(stringResource(R.string.isareler_tab_qeydler, q.size), tab == 1, Modifier.weight(1f)) { tab = 1 }
        }

        if (tab == 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                DemoChip(etiket = stringResource(R.string.calisma_hamisi), aktiv = filtr == null) { filtr = null }
                BookmarkTipi.entries.forEach { t ->
                    DemoChip(etiket = tipEtiketi(t), aktiv = filtr == t) { filtr = t }
                }
            }
            val gorunen = i.filter { filtr == null || it.hedef.tip == filtr }
            if (gorunen.isEmpty()) {
                BosHal(stringResource(R.string.isareler_bos))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gorunen, key = { it.id }) { s ->
                        HedefSetri(s.hedef, onClick = { onKecid(s.hedef.kecid) }) {
                            IsareDuymesi(isareli = true) { vm.isareniSil(s.id, s.hedef.tip) }
                        }
                    }
                }
            }
        } else {
            if (q.isEmpty()) {
                BosHal(stringResource(R.string.qeydler_bos))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(q, key = { it.id }) { s ->
                        Column {
                            HedefSetri(s.hedef, onClick = { onKecid(s.hedef.kecid) })
                            Spacer(Modifier.height(5.dp))
                            QeydKarti(s.metn) { redakte = s.id }
                        }
                    }
                }
            }
        }
    }

    val r = redakte
    if (r != null) {
        QeydDialoqu(
            ilkin = q.firstOrNull { it.id == r }?.metn.orEmpty(),
            onSaxla = { vm.qeydYaz(r, it) },
            onSil = { vm.qeydYaz(r, "") },
            onBagla = { redakte = null }
        )
    }
}

@Composable
fun tipEtiketi(t: BookmarkTipi): String = stringResource(
    when (t) {
        BookmarkTipi.BOLME -> R.string.isare_tip_bolme
        BookmarkTipi.CALISMA -> R.string.isare_tip_calisma
        BookmarkTipi.KOD -> R.string.isare_tip_kod
    }
)

@Composable
private fun Tab(etiket: String, aktiv: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = KAz.colors
    Text(
        text = etiket,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (aktiv) c.accentSoft else c.bgSunken)
            .border(1.dp, if (aktiv) c.accent.copy(alpha = 0.5f) else c.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        style = MaterialTheme.typography.labelLarge,
        color = if (aktiv) c.accent else c.textDim,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun HedefSetri(
    hedef: Hedef,
    onClick: () -> Unit,
    sag: (@Composable () -> Unit)? = null
) {
    val c = KAz.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bgElev)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 13.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (hedef.tip) {
                BookmarkTipi.BOLME -> Icons.AutoMirrored.Outlined.MenuBook
                BookmarkTipi.CALISMA -> Icons.Outlined.EditNote
                BookmarkTipi.KOD -> Icons.Outlined.Code
            },
            contentDescription = tipEtiketi(hedef.tip),
            tint = c.accent,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = hedef.basliq,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = c.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (hedef.yer.isNotBlank()) {
                Text(
                    text = hedef.yer,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (sag != null) sag() else Spacer(Modifier.width(9.dp))
    }
}

@Composable
private fun BosHal(metn: String) {
    val c = KAz.colors
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Bookmark, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                text = metn,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = c.textDim,
                textAlign = TextAlign.Center
            )
        }
    }
}
