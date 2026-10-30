package com.kuralpay.app.voice.tts

import com.kuralpay.app.domain.model.VoiceProfile

object VoiceProfileMapper {

    fun getAnnouncementText(profile: VoiceProfile, amount: Double, sender: String, source: String): String {
        // A bare UPI ID ("*9795@ptyes") is noise when spoken aloud; only real names are read out.
        val name = sender.trim().takeUnless { it.isEmpty() || it.equals("Unknown", ignoreCase = true) || '@' in it }
        return when (profile) {
            VoiceProfile.SHOP -> "Payment received. ${amountToWords(amount)} rupees."
            VoiceProfile.PROFESSIONAL -> "Transaction successful. ₹${formatAmount(amount)} credited."
            VoiceProfile.FRIENDLY ->
                if (name != null) "You have received ₹${formatAmount(amount)} from $name."
                else "You have received ₹${formatAmount(amount)}."
            VoiceProfile.TAMIL -> "ரூபாய் ${formatAmount(amount)} பெறப்பட்டது."
            VoiceProfile.TAMIL_FRIENDLY ->
                if (name != null) "$name அவர்களிடமிருந்து ரூபாய் ${formatAmount(amount)} பெறப்பட்டது."
                else "ரூபாய் ${formatAmount(amount)} பெறப்பட்டது."
        }
    }

    fun getSummaryText(profile: VoiceProfile, total: Double, count: Int): String {
        return when (profile) {
            VoiceProfile.SHOP -> "Today's collection is ${amountToWords(total)} rupees from $count transactions."
            VoiceProfile.PROFESSIONAL -> "Daily summary. ₹${formatAmount(total)} from $count transactions."
            VoiceProfile.FRIENDLY -> "You have received ₹${formatAmount(total)} from $count transactions today."
            VoiceProfile.TAMIL -> "இன்றைய வசூல் ரூபாய் ${formatAmount(total)}. மொத்தம் $count பரிவர்த்தனைகள்."
            VoiceProfile.TAMIL_FRIENDLY -> "இன்று $count பேரிடமிருந்து மொத்தம் ரூபாய் ${formatAmount(total)} பெறப்பட்டது."
        }
    }

    fun getSourceInsightText(profile: VoiceProfile, source: String, percentage: Int): String {
        return when (profile) {
            VoiceProfile.SHOP -> "Most payments received through $source. $percentage percent."
            VoiceProfile.PROFESSIONAL -> "Primary payment source: $source at $percentage%."
            VoiceProfile.FRIENDLY -> "Most of your payments came through $source."
            VoiceProfile.TAMIL -> "பெரும்பாலான கொடுப்பனவுகள் $source மூலம் பெறப்பட்டன."
            VoiceProfile.TAMIL_FRIENDLY -> "பெரும்பாலான பணம் $source மூலம் பெறப்பட்டது."
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            String.format("%.2f", amount)
        }
    }

    private fun amountToWords(amount: Double): String {
        val whole = amount.toLong()
        return when {
            whole < 1000 -> whole.toString()
            whole < 100000 -> "${whole / 1000} thousand ${whole % 1000}"
            else -> "${whole / 100000} lakh ${whole % 100000 / 1000} thousand"
        }
    }
}
