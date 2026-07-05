// FontVariation (variable-font weight axes) is still marked experimental.
@file:OptIn(ExperimentalTextApi::class)

package com.stillwater.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stillwater.app.R

/*
 * One calm font family: Figtree (variable, OFL), bundled — no downloadable
 * fonts, nothing leaves the device. Rounded-humanist, reads softly at large
 * sizes. Weights stop at SemiBold: heavy black headlines read as shouting.
 *
 * Scale discipline: 4 core sizes (34 display / 24 title / 17 body / 13 label)
 * plus one 20sp bridge. Body is 17sp on a 1.55 line height — bigger and airier
 * than the Material default, because crisis reading happens with impaired
 * executive function (Design Psychology #5).
 */
private val Figtree = FontFamily(
    Font(R.font.figtree, weight = FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.figtree, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.figtree, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.figtree, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

val StillwaterTypography = Typography(
    // Breathing screens, "urge surfed" moment: large, light, slow.
    displayLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    // Screen titles.
    headlineMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    // Section/card titles.
    titleMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    // Default reading text.
    bodyLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.1.sp,
    ),
    // Secondary/supporting text.
    bodyMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    // Buttons.
    labelLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    // Chips, captions, meta.
    labelMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp,
    ),
)
