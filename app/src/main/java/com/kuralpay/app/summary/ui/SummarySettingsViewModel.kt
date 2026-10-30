package com.kuralpay.app.summary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuralpay.app.data.repository.AnalyticsRepository
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.domain.model.SummarySlot
import com.kuralpay.app.summary.DailySummaryScheduler
import com.kuralpay.app.voice.tts.VoiceProfileMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummarySettingsState(
    val enabled: Map<SummarySlot, Boolean> = SummarySlot.entries.associateWith { true },
    val minutes: Map<SummarySlot, Int> = SummarySlot.entries.associateWith { it.defaultMinutes },
    val summaryDetails: Boolean = false
)

@HiltViewModel
class SummarySettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scheduler: DailySummaryScheduler,
    analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SummarySettingsState())
    val state: StateFlow<SummarySettingsState> = _state.asStateFlow()

    val previewText: StateFlow<String> = combine(
        settingsRepository.voiceProfile,
        analyticsRepository.getTodayStats()
    ) { profile, stats ->
        VoiceProfileMapper.getSummaryText(profile, stats.total, stats.count)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        SummarySlot.entries.forEach { slot ->
            viewModelScope.launch {
                settingsRepository.summaryEnabled(slot).collect { on ->
                    _state.update { it.copy(enabled = it.enabled + (slot to on)) }
                }
            }
            viewModelScope.launch {
                settingsRepository.summaryTime(slot).collect { m ->
                    _state.update { it.copy(minutes = it.minutes + (slot to m)) }
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.summaryDetails.collect { d -> _state.update { it.copy(summaryDetails = d) } }
        }
    }

    fun toggle(slot: SummarySlot) = viewModelScope.launch {
        val on = !(_state.value.enabled[slot] ?: true)
        when (slot) {
            SummarySlot.MORNING -> settingsRepository.setSummary9am(on)
            SummarySlot.AFTERNOON -> settingsRepository.setSummary1pm(on)
            SummarySlot.EVENING -> settingsRepository.setSummary6pm(on)
            SummarySlot.CLOSING -> settingsRepository.setSummaryClosing(on)
        }
        if (on) scheduler.schedule(slot, settingsRepository.summaryTime(slot).first(), replace = true)
        else scheduler.cancel(slot)
    }

    fun setTime(slot: SummarySlot, minutesOfDay: Int) = viewModelScope.launch {
        settingsRepository.setSummaryTime(slot, minutesOfDay)
        if (settingsRepository.summaryEnabled(slot).first()) {
            scheduler.schedule(slot, minutesOfDay, replace = true)
        }
    }

    fun toggleDetails() = viewModelScope.launch { settingsRepository.setSummaryDetails(!_state.value.summaryDetails) }
}
