package com.zobaer53.zedmovies.ui.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.zobaer53.zedmovies.resources.Res
import com.zobaer53.zedmovies.resources.montserrat_medium
import com.zobaer53.zedmovies.resources.montserrat_regular
import com.zobaer53.zedmovies.resources.montserrat_semibold
import org.jetbrains.compose.resources.Font

// Compose resources' Font() is @Composable, so zedMoviesTheme builds the family (no top-level val).
@Composable
internal fun rememberMontserrat(): FontFamily {
    val regular = Font(Res.font.montserrat_regular, FontWeight.Normal)
    val medium = Font(Res.font.montserrat_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.montserrat_semibold, FontWeight.SemiBold)
    return remember(regular, medium, semiBold) { FontFamily(regular, medium, semiBold) }
}

@Immutable
class zedMoviesTypography(fontFamily: FontFamily) {
    val regular = Regular(fontFamily)
    val medium = Medium(fontFamily)
    val semiBold = SemiBold(fontFamily)

    @Immutable
    data class Regular(
        val fontFamily: FontFamily,
        val default: TextStyle = defaultTextStyle(fontFamily),
        val h1: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = H1FontSize,
            lineHeight = H1LineHeight
        ),
        val h2: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = H2FontSize,
            lineHeight = H2LineHeight
        ),
        val h3: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = H3FontSize,
            lineHeight = H3LineHeight
        ),
        val h4: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = H4FontSize,
            lineHeight = H4LineHeight
        ),
        val h5: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = H5FontSize,
            lineHeight = H5LineHeight
        ),
        val h6: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = H6FontSize,
            lineHeight = H6LineHeight
        ),
        val h7: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = H7FontSize,
            lineHeight = H7LineHeight
        )
    )

    @Immutable
    data class Medium(
        val fontFamily: FontFamily,
        val default: TextStyle = defaultTextStyle(fontFamily),
        val h1: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = H1FontSize,
            lineHeight = H1LineHeight
        ),
        val h2: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = H2FontSize,
            lineHeight = H2LineHeight
        ),
        val h3: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = H3FontSize,
            lineHeight = H3LineHeight
        ),
        val h4: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = H4FontSize,
            lineHeight = H4LineHeight
        ),
        val h5: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = H5FontSize,
            lineHeight = H5LineHeight
        ),
        val h6: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = H6FontSize,
            lineHeight = H6LineHeight
        ),
        val h7: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = H7FontSize,
            lineHeight = H7LineHeight
        )
    )

    @Immutable
    data class SemiBold(
        val fontFamily: FontFamily,
        val default: TextStyle = defaultTextStyle(fontFamily),
        val h1: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = H1FontSize,
            lineHeight = H1LineHeight
        ),
        val h2: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = H2FontSize,
            lineHeight = H2LineHeight
        ),
        val h3: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = H3FontSize,
            lineHeight = H3LineHeight
        ),
        val h4: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = H4FontSize,
            lineHeight = H4LineHeight
        ),
        val h5: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = H5FontSize,
            lineHeight = H5LineHeight
        ),
        val h6: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = H6FontSize,
            lineHeight = H6LineHeight
        ),
        val h7: TextStyle = defaultTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = H7FontSize,
            lineHeight = H7LineHeight
        )
    )
}

private fun defaultTextStyle(
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = LetterSpacing
) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing
)

private fun defaultTextStyle(
    fontFamily: FontFamily,
    letterSpacing: TextUnit = LetterSpacing
) = TextStyle(
    fontFamily = fontFamily,
    letterSpacing = letterSpacing
)

/**
 * Material 3 text styles (TextField, tabs, snackbars, sheets) from the same family. Only family
 * and weight: the old top-level Typography val was initialised before the size vals below, so its
 * sizes, line heights and letter spacing were unspecified and fell back to the surrounding text
 * style. The screens (and the screenshot references) look like that, so keep it.
 */
internal fun materialTypography(typography: zedMoviesTypography) = with(typography) {
    Typography(
        displayLarge = semiBold.h1.familyAndWeight(),
        displayMedium = semiBold.h2.familyAndWeight(),
        displaySmall = semiBold.h3.familyAndWeight(),
        headlineLarge = medium.h1.familyAndWeight(),
        headlineMedium = medium.h2.familyAndWeight(),
        headlineSmall = medium.h3.familyAndWeight(),
        titleLarge = medium.h4.familyAndWeight(),
        titleMedium = medium.h5.familyAndWeight(),
        titleSmall = medium.h6.familyAndWeight(),
        bodyLarge = regular.h1.familyAndWeight(),
        bodyMedium = regular.h2.familyAndWeight(),
        bodySmall = regular.h3.familyAndWeight(),
        labelLarge = regular.h4.familyAndWeight(),
        labelMedium = regular.h5.familyAndWeight(),
        labelSmall = regular.h6.familyAndWeight(),
    )
}

private fun TextStyle.familyAndWeight() =
    TextStyle(fontFamily = fontFamily, fontWeight = fontWeight)

// Only seen outside zedMoviesTheme, which provides Montserrat; Font(Res…) needs a composition.
internal val LocalzedMoviesTypography =
    staticCompositionLocalOf { zedMoviesTypography(FontFamily.Default) }

private val H1FontSize = 28.sp
private val H1LineHeight = (34.13f).sp

private val H2FontSize = 24.sp
private val H2LineHeight = (29.26f).sp

private val H3FontSize = 18.sp
private val H3LineHeight = (21.94f).sp

private val H4FontSize = 16.sp
private val H4LineHeight = (19.5f).sp

private val H5FontSize = 14.sp
private val H5LineHeight = (17.07f).sp

private val H6FontSize = 12.sp
private val H6LineHeight = (14.63f).sp

private val H7FontSize = 10.sp
private val H7LineHeight = (12.19f).sp

private val LetterSpacing = (0.12f).sp
