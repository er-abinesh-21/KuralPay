package com.kuralpay.app.domain.model

/** The four daily summary slots. Times are minutes after midnight; [tag] names the scheduled work. */
enum class SummarySlot(val tag: String, val defaultMinutes: Int) {
    MORNING("summary_9am", 9 * 60),
    AFTERNOON("summary_1pm", 13 * 60),
    EVENING("summary_6pm", 18 * 60),
    CLOSING("summary_closing", 20 * 60)
}
