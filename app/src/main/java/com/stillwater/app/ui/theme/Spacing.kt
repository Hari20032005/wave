package com.stillwater.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 8-pt spacing grid. Generous whitespace is a design-psychology requirement,
 * not decoration — density reads as urgency.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val huge = 64.dp

    /** Minimum touch target anywhere in the app. */
    val minTouchTarget = 48.dp

    /**
     * Touch target on SOS/crisis screens — during an urge, motor precision
     * and executive function are impaired (Design Psychology #5).
     */
    val sosTouchTarget = 72.dp

    /** Screen edge padding. */
    val screenEdge = lg
}
