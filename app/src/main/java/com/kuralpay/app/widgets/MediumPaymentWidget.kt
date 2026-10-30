package com.kuralpay.app.widgets

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.text.NumberFormat
import java.util.Locale

internal fun widgetInr(amount: Double): String =
    NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }.format(amount)

class MediumPaymentWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val total = WidgetDataProvider.getTodayTotal(context)
            val count = WidgetDataProvider.getTodayCount(context)
            Column(
                modifier = GlanceModifier.fillMaxSize().cornerRadius(22.dp).background(WidgetBg).padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "TODAY'S COLLECTION",
                    style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, color = WidgetMuted)
                )
                Text(
                    text = "₹${widgetInr(total)}",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, color = WidgetLime),
                    modifier = GlanceModifier.padding(vertical = 4.dp)
                )
                Row(modifier = GlanceModifier.padding(top = 6.dp)) {
                    Text(
                        text = "$count",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WidgetText),
                        modifier = GlanceModifier.padding(end = 6.dp)
                    )
                    Text(
                        text = if (count == 1) "payment" else "payments",
                        style = TextStyle(fontSize = 13.sp, color = WidgetMuted)
                    )
                }
            }
        }
    }
}

class MediumPaymentWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumPaymentWidget()
}
