package com.kuralpay.app.summary

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kuralpay.app.data.repository.AnalyticsRepository
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.domain.model.VoiceProfile
import com.kuralpay.app.voice.tts.PaymentTtsEngine
import com.kuralpay.app.voice.tts.VoiceProfileMapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val analyticsRepository: AnalyticsRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsEngine: PaymentTtsEngine
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val stats = analyticsRepository.getTodayStats().first()
        val profile = settingsRepository.voiceProfile.first()
        val showDetails = settingsRepository.summaryDetails.first()

        val tts = ttsEngine
        tts.init {
            val mainText = VoiceProfileMapper.getSummaryText(profile, stats.total, stats.count)
            tts.speak(mainText, profile) {
                if (showDetails) {
                    val topSource = stats.sourceDistribution.maxByOrNull { it.second }
                    if (topSource != null) {
                        val total = stats.sourceDistribution.sumOf { it.second }
                        val pct = if (total > 0) (topSource.second * 100 / total) else 0
                        tts.speak(
                            VoiceProfileMapper.getSourceInsightText(profile, topSource.first.displayName, pct),
                            profile
                        )
                    }
                }
            }
        }

        return Result.success()
    }
}
