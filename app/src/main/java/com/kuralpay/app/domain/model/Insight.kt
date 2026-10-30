package com.kuralpay.app.domain.model

data class Insight(
    val text: String,
    val type: InsightType,
    val timestamp: Long
)

enum class InsightType {
    DAILY_TREND,
    WEEKLY_TREND,
    GROWTH_INDICATOR,
    PEAK_HOUR,
    SOURCE_PERFORMANCE
}
