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

class LargePaymentWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val total = WidgetDataProvider.getTodayTotal(context)
            val count = WidgetDataProvider.getTodayCount(context)
            val lastAmount = WidgetDataProvider.getLastAmount(context)
            val lastSender = WidgetDataProvider.getLastSender(context)
            Column(
                modifier = GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(WidgetBg).padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(text = "TODAY", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, color = WidgetMuted))
                Text(
                    text = "₹${widgetInr(total)}",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, color = WidgetLime),
                    modifier = GlanceModifier.padding(vertical = 4.dp)
                )
                Row(modifier = GlanceModifier.padding(bottom = 10.dp)) {
                    Text(text = "$count ", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WidgetText))
                    Text(text = if (count == 1) "payment" else "payments", style = TextStyle(fontSize = 13.sp, color = WidgetMuted))
                }
                if (lastSender.isNotBlank()) {
                    Text(text = "LAST PAYMENT", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, color = WidgetMuted))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${widgetInr(lastAmount)}",
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = WidgetText)
                        )
                        Text(
                            text = lastSender,
                            style = TextStyle(fontSize = 13.sp, color = WidgetMuted),
                            modifier = GlanceModifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

class LargePaymentWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargePaymentWidget()
}
