package com.kuralpay.app.widgets

import android.content.Context
import java.util.Calendar

object WidgetDataProvider {

    fun getTodayTotal(context: Context): Double {
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        return prefs.getFloat("today_total", 0f).toDouble()
    }

    fun getTodayCount(context: Context): Int {
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        return prefs.getInt("today_count", 0)
    }

    fun getLastAmount(context: Context): Double {
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        return prefs.getFloat("last_amount", 0f).toDouble()
    }

    fun getLastSender(context: Context): String {
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        return prefs.getString("last_sender", "") ?: ""
    }

    fun updateWidgetData(context: Context, total: Double, count: Int, lastAmount: Double, lastSender: String) {
        context.getSharedPreferences("widget_data", Context.MODE_PRIVATE).edit().apply {
            putFloat("today_total", total.toFloat())
            putInt("today_count", count)
            putFloat("last_amount", lastAmount.toFloat())
            putString("last_sender", lastSender)
            apply()
        }
    }
}
