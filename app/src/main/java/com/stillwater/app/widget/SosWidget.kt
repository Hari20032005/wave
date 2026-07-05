package com.stillwater.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.stillwater.app.MainActivity

/**
 * One-tap SOS from the home screen. Deliberately quiet: deep-water surface,
 * seafoam text, no badge, no counter — it should feel like a hand rail, not
 * an app fighting for attention.
 */
class SosWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val intent = Intent(context, MainActivity::class.java)
                    .setAction(MainActivity.ACTION_OPEN_SOS)
                    .putExtra(MainActivity.EXTRA_ENTRY_POINT, "WIDGET")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF161F1B)))
                        .cornerRadius(24.dp)
                        .clickable(actionStartActivity(intent)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = GlanceModifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "I'm feeling an urge",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF8FB8A5)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                        Text(
                            text = "Stillwater is here",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF9DACA2)),
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

class SosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SosWidget()
}
