package com.kuralpay.app.domain.model

enum class VoiceProfile(
    val displayName: String,
    val pitch: Float,
    val speechRate: Float,
    val language: String
) {
    SHOP("Shop Mode", 1.0f, 0.8f, "en-IN"),
    PROFESSIONAL("Professional Mode", 1.0f, 1.0f, "en-IN"),
    FRIENDLY("Friendly Mode", 1.1f, 0.9f, "en-IN"),
    TAMIL("Tamil Mode", 1.0f, 0.9f, "ta-IN"),
    TAMIL_FRIENDLY("Tamil Friendly Mode", 1.05f, 0.9f, "ta-IN")
}
