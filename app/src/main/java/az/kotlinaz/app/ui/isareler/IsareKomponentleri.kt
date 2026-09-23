package az.kotlinaz.app.ui.isareler

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.kotlinaz.app.R
import az.kotlinaz.app.ui.theme.KAz

/**
 * Kod kartındakı işarə düyməsi üçün (T5.3). Kart blok renderinin dərinliyindədir —
 * dəyəri dərs ekranı təyin edir; null olanda (meydan, çalışma) düymə görünmür.
 */
data class KodIsaresi(val isareliler: Set<String>, val deyis: (String) -> Unit)

val LocalKodIsaresi = compositionLocalOf<KodIsaresi?> { null }

/** İşarə düyməsi — dolu ikon işarəlidir. */
@Composable
fun IsareDuymesi(isareli: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KAz.colors
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            if (isareli) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = stringResource(if (isareli) R.string.isare_sil else R.string.isare_elave),
            tint = if (isareli) c.accent else c.textDim,
            modifier = Modifier.size(21.dp)
        )
    }
}

/** Qeyd düyməsi — qeyd varsa vurğulu. */
@Composable
fun QeydDuymesi(qeydVar: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KAz.colors
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Outlined.EditNote,
            contentDescription = stringResource(if (qeydVar) R.string.qeyd_redakte else R.string.qeyd_elave),
            tint = if (qeydVar) c.accent else c.textDim,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Qeydin kartı: 4 sətir, uzundursa «Hamısını göstər». Toxunanda redaktə açılır.
 */
@Composable
fun QeydKarti(metn: String, modifier: Modifier = Modifier, onRedakte: () -> Unit) {
    val c = KAz.colors
    var tam by rememberSaveable(metn) { mutableStateOf(false) }
    var uzundur by rememberSaveable(metn) { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.warn.copy(alpha = 0.08f))
            .border(1.dp, c.warn.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .clickable(onClick = onRedakte)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.EditNote, contentDescription = null, tint = c.warn, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.qeyd_basliq),
                style = MaterialTheme.typography.labelMedium,
                color = c.warn
            )
        }
        Spacer(Modifier.padding(top = 4.dp))
        Text(
            text = metn,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color = c.text,
            maxLines = if (tam) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!tam) uzundur = it.hasVisualOverflow }
        )
        if (uzundur || tam) {
            Text(
                text = stringResource(if (tam) R.string.qeyd_qisalt else R.string.qeyd_hamisi),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { tam = !tam },
                style = MaterialTheme.typography.labelSmall,
                color = c.accent
            )
        }
    }
}

/**
 * Qeyd redaktoru. Mövcud qeydi silmək təsdiq soruşur (T5.3); boş saxlamaq
 * da qeydi silir, amma bu halda istifadəçi mətni özü silib — təsdiq lazım deyil.
 */
@Composable
fun QeydDialoqu(
    ilkin: String,
    onSaxla: (String) -> Unit,
    onSil: () -> Unit,
    onBagla: () -> Unit
) {
    val c = KAz.colors
    var metn by rememberSaveable { mutableStateOf(ilkin) }
    var silinsin by rememberSaveable { mutableStateOf(false) }

    if (silinsin) {
        AlertDialog(
            onDismissRequest = { silinsin = false },
            title = { Text(stringResource(R.string.qeyd_sil_basliq)) },
            text = { Text(stringResource(R.string.qeyd_sil_metn)) },
            confirmButton = {
                TextButton(onClick = { onSil(); onBagla() }) { Text(stringResource(R.string.qeyd_sil), color = c.err) }
            },
            dismissButton = { TextButton(onClick = { silinsin = false }) { Text(stringResource(R.string.ayarlar_imtina)) } },
            containerColor = c.bgElev,
            titleContentColor = c.text,
            textContentColor = c.textDim
        )
        return
    }

    AlertDialog(
        onDismissRequest = onBagla,
        title = { Text(stringResource(if (ilkin.isBlank()) R.string.qeyd_elave else R.string.qeyd_redakte)) },
        text = {
            Column {
                OutlinedTextField(
                    value = metn,
                    onValueChange = { metn = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    placeholder = { Text(stringResource(R.string.qeyd_ipucu)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent,
                        cursorColor = c.accent
                    )
                )
                Spacer(Modifier.padding(top = 6.dp))
                Text(
                    text = stringResource(R.string.qeyd_dil_izah),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaxla(metn); onBagla() }) { Text(stringResource(R.string.qeyd_saxla), color = c.accent) }
        },
        dismissButton = {
            Row {
                if (ilkin.isNotBlank()) {
                    TextButton(onClick = { silinsin = true }) { Text(stringResource(R.string.qeyd_sil), color = c.err) }
                }
                TextButton(onClick = onBagla) { Text(stringResource(R.string.ayarlar_imtina)) }
            }
        },
        containerColor = c.bgElev,
        titleContentColor = c.text,
        textContentColor = c.textDim
    )
}
