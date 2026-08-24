package az.kotlinaz.app.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import az.kotlinaz.app.data.model.Span
import az.kotlinaz.app.ui.theme.KAz

/**
 * Sayt mətnindəki sətir daxili parçaları (qalın, kod, keçid və s.)
 * Compose `AnnotatedString`-inə çevirir.
 */
@Composable
fun spansToAnnotated(
    spans: List<Span>,
    onLink: (String) -> Unit = {}
): AnnotatedString {
    val c = KAz.colors
    val kodStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 0.88.em,
        color = c.accent,
        background = c.accentSoft
    )
    val kbdStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 0.85.em,
        color = c.textDim,
        background = c.bgSunken
    )
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = c.accent, textDecoration = TextDecoration.Underline)
    )

    return buildAnnotatedString {
        spans.forEach { s ->
            when (s.k) {
                Span.BREAK -> append('\n')

                Span.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s.v) }

                Span.ITALIC -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(s.v) }

                Span.CODE -> withStyle(kodStyle) { append(s.v) }

                Span.KBD -> withStyle(kbdStyle) { append(" " + s.v + " ") }

                // Saytdakı qradient mətn — mobil oxunaqlığı üçün vurğu rəngi
                Span.GRAD -> withStyle(
                    SpanStyle(color = c.accent, fontWeight = FontWeight.Bold)
                ) { append(s.v) }

                Span.LINK -> {
                    val href = s.href.orEmpty()
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = href,
                            styles = linkStyles
                        ) { onLink(href) }
                    ) { append(s.v) }
                }

                else -> append(s.v)
            }
        }
    }
}

/** Paraqraf — sayt mətnini olduğu kimi göstərir. */
@Composable
fun SpansText(
    spans: List<Span>,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    lineHeight: TextUnit = TextUnit.Unspecified,
    onLink: (String) -> Unit = {}
) {
    Text(
        text = spansToAnnotated(spans, onLink),
        modifier = modifier,
        style = if (lineHeight != TextUnit.Unspecified) style.copy(lineHeight = lineHeight) else style
    )
}

/** Spanları düz mətnə çevirir — axtarış və paylaşma üçün. */
fun spansToPlain(spans: List<Span>): String =
    spans.joinToString("") { if (it.k == Span.BREAK) "\n" else it.v }
