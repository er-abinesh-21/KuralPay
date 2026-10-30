package com.kuralpay.app.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

internal val WidgetBg = Color(0xFF261A66)
internal val WidgetText = ColorProvider(Color(0xFFFFFFFF))
internal val WidgetMuted = ColorProvider(Color(0xFFC2BFD4))
internal val WidgetLime = ColorProvider(Color(0xFFEF5F18))

class SmallPaymentWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val total = WidgetDataProvider.getTodayTotal(context)
            Column(
                modifier = GlanceModifier.fillMaxSize().cornerRadius(20.dp).background(WidgetBg).padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY",
                    style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, color = WidgetMuted),
                    modifier = GlanceModifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "₹${widgetInr(total)}",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = WidgetLime)
                )
            }
        }
    }
}

class SmallPaymentWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallPaymentWidget()
}
