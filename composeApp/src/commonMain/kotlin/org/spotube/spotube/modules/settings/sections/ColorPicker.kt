package org.spotube.spotube.modules.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.spotube.millennium.composables.MillenniumText
import org.spotube.spotube.core.ui.theming.AppTheme

private val pkPresets = listOf(
    0x3340F0, 0xE0457B, 0xE67E22, 0xA3E635, 0x22C55E, 0x06B6D4, 0x8B5CF6, 0x0A0C1F
)

private fun pkParse(s: String): Int? {
    val h = s.trim().removePrefix("#")
    if (h.length != 6) return null
    return h.toIntOrNull(16)
}

private fun pkColorOf(rgb: Int): Color =
    Color(((rgb shr 16) and 255) / 255f, ((rgb shr 8) and 255) / 255f, (rgb and 255) / 255f)

private fun pkRgb(c: Color): Int =
    ((c.red * 255f).roundToInt() shl 16) or
            ((c.green * 255f).roundToInt() shl 8) or
            (c.blue * 255f).roundToInt()

private fun pkHex(c: Color): String =
    "#" + pkRgb(c).toString(16).padStart(6, '0').uppercase()

private fun pkHsv(h: Float, s: Float, v: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f
    val c = v * s
    val x = c * (1f - abs((hh / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when ((hh / 60f).toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

private fun pkRgbToHsv(r: Float, g: Float, b: Float): FloatArray {
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val d = mx - mn
    val h = when {
        d == 0f -> 0f
        mx == r -> 60f * (((g - b) / d) % 6f)
        mx == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    val hh = if (h < 0f) h + 360f else h
    val s = if (mx == 0f) 0f else d / mx
    return floatArrayOf(hh, s, mx)
}

/** Sélecteur de couleur : carré saturation/luminosité, barre de teinte et couleurs prêtes. */
@Composable
internal fun ColorPickerField(label: String, value: String, onValueChange: (String) -> Unit) {
    val onChange by rememberUpdatedState(onValueChange)
    val initial = remember {
        val c = pkColorOf(pkParse(value) ?: 0x3340F0)
        pkRgbToHsv(c.red, c.green, c.blue)
    }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var sat by remember { mutableFloatStateOf(initial[1]) }
    var bri by remember { mutableFloatStateOf(initial[2]) }
    var expanded by remember { mutableStateOf(false) }

    fun emit() {
        onChange(pkHex(pkHsv(hue, sat, bri)))
    }

    // Quand la valeur change de l'extérieur (Surprends-moi, Réinitialiser), on suit.
    LaunchedEffect(value) {
        val rgb = pkParse(value)
        if (rgb != null && pkRgb(pkHsv(hue, sat, bri)) != rgb) {
            val c = pkColorOf(rgb)
            val h = pkRgbToHsv(c.red, c.green, c.blue)
            hue = h[0]
            sat = h[1]
            bri = h[2]
        }
    }

    val shown = pkHsv(hue, sat, bri)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shown)
                    .border(1.dp, AppTheme.colors.outlineVariant, RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                MillenniumText(label, color = AppTheme.colors.onSurface)
                MillenniumText(pkHex(shown), color = AppTheme.colors.onSurfaceVariant)
            }
            MillenniumText(
                if (expanded) "Fermer" else "Choisir",
                color = AppTheme.colors.primary
            )
        }

        if (expanded) {
            // Carré saturation (gauche -> droite) / luminosité (bas -> haut)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .drawBehind {
                        drawRect(Brush.horizontalGradient(listOf(Color.White, pkHsv(hue, 1f, 1f))))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(sat * size.width, (1f - bri) * size.height),
                            style = Stroke(2.dp.toPx())
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { o ->
                            sat = (o.x / size.width).coerceIn(0f, 1f)
                            bri = 1f - (o.y / size.height).coerceIn(0f, 1f)
                            emit()
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { o ->
                                sat = (o.x / size.width).coerceIn(0f, 1f)
                                bri = 1f - (o.y / size.height).coerceIn(0f, 1f)
                                emit()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                bri = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                emit()
                            }
                        )
                    }
            )

            // Barre de teinte
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .drawBehind {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { pkHsv(it, 1f, 1f) }
                            )
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(hue / 360f * size.width, size.height / 2f),
                            style = Stroke(2.dp.toPx())
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { o ->
                            hue = (o.x / size.width).coerceIn(0f, 1f) * 360f
                            emit()
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { o ->
                                hue = (o.x / size.width).coerceIn(0f, 1f) * 360f
                                emit()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                hue = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                                emit()
                            }
                        )
                    }
            )

            // Couleurs prêtes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pkPresets.forEach { rgb ->
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(pkColorOf(rgb))
                            .border(1.dp, AppTheme.colors.outlineVariant, CircleShape)
                            .clickable {
                                val c = pkColorOf(rgb)
                                val h = pkRgbToHsv(c.red, c.green, c.blue)
                                hue = h[0]
                                sat = h[1]
                                bri = h[2]
                                emit()
                            }
                    )
                }
            }
        }
    }
}