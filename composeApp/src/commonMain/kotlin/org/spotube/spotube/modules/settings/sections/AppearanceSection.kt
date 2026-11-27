/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.modules.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogFooter
import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.millennium.composables.MillenniumText
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.components.SelectionSettingCard
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxColorSwatch
import org.spotube.spotube.resources.iconsax.IconsaxMagic
import spotube.composeapp.generated.resources.*

private const val DEFAULT_BG1 = 0xFF0A0C1FL
private const val DEFAULT_BG2 = 0xFF1D2AB9L
private const val DEFAULT_ACCENT = 0xFF3340F0L
private const val DEFAULT_DIRECTION = 45
private const val DEFAULT_INTENSITY = 73

internal fun LazyListScope.appearanceSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true) {
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_appearance)
    }
    settingsSectionCard(
        items = listOf(
            {
                SelectionSettingCard(
                    title = stringResource(Res.string.settings_theme_title),
                    subtitle = stringResource(
                        Res.string.settings_theme_subtitle_current,
                        settings.theme.displayLabel()
                    ),
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxColorSwatch, stringResource(Res.string.settings_theme_title))
                    },
                    selectedOption = settings.theme,
                    options = Theme.entries,
                    optionLabel = { it.displayLabel() },
                    onOptionSelected = { theme ->
                        settingsViewModel.updateSettings {
                            copy(theme = theme)
                        }
                    }
                )
            },
            {
                AppearanceSettingCard(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxMagic,
                            stringResource(Res.string.settings_appearance_title)
                        )
                    })
            })
    )
}

@Composable
private fun Theme.displayLabel(): String {
    return when (this) {
        Theme.LIGHT -> stringResource(Res.string.settings_theme_light)
        Theme.DARK -> stringResource(Res.string.settings_theme_dark)
        Theme.SYSTEM -> stringResource(Res.string.settings_theme_system)
    }
}

private fun String.parseHex(): Long? {
    val h = trim().removePrefix("#")
    if (h.length != 6) return null
    val v = h.toLongOrNull(16) ?: return null
    return 0xFF000000L or v
}

private fun Long.toHex(): String =
    "#" + (this and 0xFFFFFFL).toString(16).padStart(6, '0').uppercase()

private fun hsvColor(hue: Float, saturation: Float, value: Float): Long {
    val h = ((hue % 360f) + 360f) % 360f
    val c = value * saturation
    val hp = h / 60f
    val x = c * (1f - abs(hp % 2f - 1f))
    val m = value - c
    val rgb = when (hp.toInt()) {
        0 -> floatArrayOf(c, x, 0f)
        1 -> floatArrayOf(x, c, 0f)
        2 -> floatArrayOf(0f, c, x)
        3 -> floatArrayOf(0f, x, c)
        4 -> floatArrayOf(x, 0f, c)
        else -> floatArrayOf(c, 0f, x)
    }
    fun ch(f: Float): Long = ((f + m) * 255f).toInt().coerceIn(0, 255).toLong()
    return 0xFF000000L or (ch(rgb[0]) shl 16) or (ch(rgb[1]) shl 8) or ch(rgb[2])
}

private fun gradientBrush(c1: Color, c2: Color, directionDeg: Int, intensity: Int, size: Size): Brush {
    val target = lerp(c1, c2, intensity / 100f)
    val rad = directionDeg * PI / 180.0
    val dx = cos(rad).toFloat()
    val dy = sin(rad).toFloat()
    val half = max(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    return Brush.linearGradient(
        colors = listOf(c1, target),
        start = center - Offset(dx, dy) * half,
        end = center + Offset(dx, dy) * half
    )
}

@Composable
private fun AppearanceSettingCard(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    icon: (@Composable () -> Unit)? = null) {
    var isDialogOpen by remember { mutableStateOf(false) }

    SettingCardItem(
        title = stringResource(Res.string.settings_appearance_title),
        subtitle = "Fond en dégradé et couleur d'accent",
        icon = icon,
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 46.dp, height = 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .drawBehind {
                            drawRect(
                                gradientBrush(
                                    Color(settings.bgColor1), Color(settings.bgColor2),
                                    settings.bgDirection, settings.bgIntensity, size))
                        })
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(settings.accentColor)))
            }
        },
        onClick = { isDialogOpen = true })

    if (isDialogOpen) {
        var color1 by remember { mutableStateOf(settings.bgColor1.toHex()) }
        var color2 by remember { mutableStateOf(settings.bgColor2.toHex()) }
        var accent by remember { mutableStateOf(settings.accentColor.toHex()) }
        var direction by remember { mutableStateOf(settings.bgDirection) }
        var intensity by remember { mutableStateOf(settings.bgIntensity) }

        val p1 = Color(color1.parseHex() ?: settings.bgColor1)
        val p2 = Color(color2.parseHex() ?: settings.bgColor2)
        val pa = Color(accent.parseHex() ?: settings.accentColor)

        MillenniumDialog(visible = true, onDismissRequest = { isDialogOpen = false }) {
            MillenniumDialogHeader {
                MillenniumDialogTitle("Personnalise ton thème")
                MillenniumDialogClose()
            }
            MillenniumDialogBody {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .drawBehind {
                            drawRect(gradientBrush(p1, p2, direction, intensity, size))
                        },
                    contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(pa)
                            .padding(horizontal = 18.dp, vertical = 8.dp)) {
                        MillenniumText(
                            "Bouton",
                            color = if (pa.luminance() > 0.5f) Color(0xFF0A0C1F) else Color.White)
                    }
                }
                HexField("Couleur 1 (fond)", color1) { color1 = it }
                HexField("Couleur 2 (fond)", color2) { color2 = it }
                HexField("Couleur d'accent (boutons, barre active)", accent) { accent = it }
                MillenniumText(
                    "Direction du dégradé : $direction°",
                    color = AppTheme.colors.onSurfaceVariant)
                MillenniumSlider(
                    value = direction / 360f,
                    onValueChange = { direction = (it * 360f).toInt() },
                    label = "Direction du dégradé",
                    modifier = Modifier.fillMaxWidth())
                MillenniumText(
                    "Intensité de la couleur : $intensity%",
                    color = AppTheme.colors.onSurfaceVariant)
                MillenniumSlider(
                    value = intensity / 100f,
                    onValueChange = { intensity = (it * 100f).toInt() },
                    label = "Intensité de la couleur",
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MillenniumButton(
                        onClick = {
                            val hue = Random.nextFloat() * 360f
                            val hue2 = hue + Random.nextFloat() * 80f - 40f
                            color1 = hsvColor(hue, 0.7f, 0.12f).toHex()
                            color2 = hsvColor(hue2, 0.8f, 0.7f).toHex()
                            accent = hsvColor(hue2, 0.7f, 0.85f).toHex()
                            direction = Random.nextInt(0, 360)
                            intensity = Random.nextInt(55, 91)
                        },
                        style = MillenniumButtonStyle.Secondary) {
                        MillenniumText("Surprends-moi !")
                    }
                    MillenniumButton(
                        onClick = {
                            color1 = DEFAULT_BG1.toHex()
                            color2 = DEFAULT_BG2.toHex()
                            accent = DEFAULT_ACCENT.toHex()
                            direction = DEFAULT_DIRECTION
                            intensity = DEFAULT_INTENSITY
                        },
                        style = MillenniumButtonStyle.Secondary) {
                        MillenniumText("Réinitialiser")
                    }
                }
            }
            MillenniumDialogFooter {
                MillenniumButton(
                    onClick = { isDialogOpen = false },
                    style = MillenniumButtonStyle.Secondary) {
                    MillenniumText(stringResource(Res.string.common_cancel))
                }
                MillenniumButton(
                    onClick = {
                        val c1 = color1.parseHex()
                        val c2 = color2.parseHex()
                        val ca = accent.parseHex()
                        settingsViewModel.updateSettings {
                            copy(
                                bgColor1 = c1 ?: bgColor1,
                                bgColor2 = c2 ?: bgColor2,
                                accentColor = ca ?: accentColor,
                                bgDirection = direction,
                                bgIntensity = intensity)
                        }
                        isDialogOpen = false
                    }) {
                    MillenniumText(stringResource(Res.string.common_save))
                }
            }
        }
    }
}

@Composable
private fun HexField(label: String, value: String, onValueChange: (String) -> Unit) {
    ColorPickerField(label, value, onValueChange)
}