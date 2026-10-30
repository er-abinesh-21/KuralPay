package com.kuralpay.app.premium

import com.kuralpay.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend fun isPremium(): Boolean = settingsRepository.premiumEnabled.first()

    suspend fun getApiKey(): String = settingsRepository.groqApiKey.first()

    val premiumFeatures = listOf(
        PremiumFeature("AI Assistant", "Voice-activated payment queries with Groq AI"),
        PremiumFeature("Smart Insights", "AI-generated daily trends and analytics"),
        PremiumFeature("Voice Summaries", "Scheduled daily collection summaries"),
        PremiumFeature("Advanced Analytics", "Detailed payment source breakdowns"),
        PremiumFeature("Adaptive Learning", "Self-improving notification parser"),
        PremiumFeature("Multi-Device Sync", "Sync across all your devices"),
        PremiumFeature("Cloud Backup", "Secure cloud backup of all transactions"),
        PremiumFeature("Business Reports", "Comprehensive business intelligence reports")
    )
}

data class PremiumFeature(
    val name: String,
    val description: String
)
