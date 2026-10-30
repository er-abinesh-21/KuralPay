package com.kuralpay.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kuralpay.app.R

val Lexend = FontFamily(
    Font(R.font.lexend_light, FontWeight.Light),
    Font(R.font.lexend_regular, FontWeight.Normal),
    Font(R.font.lexend_medium, FontWeight.Medium),
    Font(R.font.lexend_semibold, FontWeight.SemiBold),
    Font(R.font.lexend_bold, FontWeight.Bold)
)

val MuktaMalar = FontFamily(
    Font(R.font.mukta_malar_regular, FontWeight.Normal),
    Font(R.font.mukta_malar_medium, FontWeight.Medium),
    Font(R.font.mukta_malar_semibold, FontWeight.SemiBold),
    Font(R.font.mukta_malar_bold, FontWeight.Bold)
)

/** Figures in Lexend with tabular digits so amounts don't jitter while counting up. */
val AmountStyle = TextStyle(
    fontFamily = Lexend,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.03).em,
    fontFeatureSettings = "tnum"
)

val EyebrowStyle = TextStyle(
    fontFamily = Lexend,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 0.12.em
)

val Typography = Typography(
    displayLarge = AmountStyle.copy(fontSize = 52.sp, lineHeight = 56.sp),
    displayMedium = AmountStyle.copy(fontSize = 40.sp, lineHeight = 44.sp),
    displaySmall = AmountStyle.copy(fontSize = 32.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.02).em),
    titleLarge = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = MuktaMalar, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = MuktaMalar, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = MuktaMalar, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)
