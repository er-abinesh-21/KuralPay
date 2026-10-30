package com.kuralpay.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.kuralpay.app.domain.model.PaymentSource

/** Brand palette: White, Orange, Scarlet Bikini. */
val BrandWhite = Color(0xFFFFFFFF)
val BrandOrange = Color(0xFFEF5F18)
val BrandIndigo = Color(0xFF261A66)

@Immutable
data class KpColors(
    val ink: Color,
    val deck: Color,
    val deck2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    /** Indigo: structure, selection, primary buttons. */
    val royal: Color,
    val royalDeep: Color,
    /** Orange as a fill (buttons, bars, the wave); always paired with [onLime]. */
    val lime: Color,
    val onLime: Color,
    /** Accent for text and thin strokes; a deeper orange on white so small text stays readable. */
    val accent: Color,
    /** Money received. */
    val good: Color,
    val warn: Color,
    val onRoyal: Color,
    val isDark: Boolean
)

/**
 * Only the three brand colours carry meaning. Surfaces and hairlines are Scarlet Bikini
 * mixed with White (8%, 14%, 22%) so every neutral stays inside the palette.
 */
val DarkKpColors = KpColors(
    ink = BrandIndigo,
    deck = Color(0xFF372C72),
    deck2 = Color(0xFF443A7B),
    line = Color(0xFF564C88),
    text = BrandWhite,
    muted = Color(0xFFC2BFD4),
    royal = BrandOrange,
    royalDeep = BrandIndigo,
    lime = BrandOrange,
    onLime = BrandWhite,
    accent = BrandOrange,
    good = BrandOrange,
    warn = BrandOrange,
    onRoyal = BrandWhite,
    isDark = true
)

val LightKpColors = KpColors(
    ink = Color(0xFFF6F6F9),
    deck = BrandWhite,
    deck2 = Color(0xFFEEEDF4),
    line = Color(0xFFE1DFEA),
    text = BrandIndigo,
    muted = Color(0xFF675E94),
    royal = BrandIndigo,
    royalDeep = BrandIndigo,
    lime = BrandOrange,
    onLime = BrandWhite,
    accent = BrandOrange,
    good = BrandOrange,
    warn = BrandOrange,
    onRoyal = BrandWhite,
    isDark = false
)

val LocalKpColors = staticCompositionLocalOf { DarkKpColors }

/** The one colour outside the palette: the payment-received tick. */
val SuccessGreen = Color(0xFF22C55E)

fun PaymentSource.brandColor(): Color = when (this) {
    PaymentSource.GOOGLE_PAY -> Color(0xFF4285F4)
    PaymentSource.PHONEPE -> Color(0xFF5F259F)
    PaymentSource.PAYTM -> Color(0xFF00A4DB)
    PaymentSource.AMAZON_PAY -> Color(0xFFE88B00)
    PaymentSource.BHIM -> Color(0xFF0E7F3B)
    PaymentSource.BANK_TRANSFER -> Color(0xFF55657F)
    PaymentSource.MERCHANT_APP -> Color(0xFF8A5A44)
    PaymentSource.UNKNOWN -> Color(0xFF6B6680)
}

fun PaymentSource.monogram(): String = when (this) {
    PaymentSource.GOOGLE_PAY -> "G"
    PaymentSource.PHONEPE -> "Pe"
    PaymentSource.PAYTM -> "P"
    PaymentSource.AMAZON_PAY -> "a"
    PaymentSource.BHIM -> "B"
    PaymentSource.BANK_TRANSFER -> "₹"
    PaymentSource.MERCHANT_APP -> "M"
    PaymentSource.UNKNOWN -> "?"
}
