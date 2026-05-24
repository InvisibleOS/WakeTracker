package com.shivansh.waketracker.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Canonical green and red used throughout the dot matrix and detail views.
 * Defined once here so every call site stays in sync.
 */
val WakeGreen = Color(0xFF4CAF50)
val WakeRed = Color(0xFFF44336)

/**
 * Computes the exact dot color for a wake log entry based on how far into the
 * buffer window the user scanned.
 *
 * ### States
 * | Condition | Result |
 * |---|---|
 * | No scan (`actualScanTimeMs == null`) | [missedColor] |
 * | Scanned on time (`actual <= target`) | [onTimeColor] |
 * | Scanned after buffer expired (`actual >= target + buffer`) | [missedColor] |
 * | Scanned within buffer | Linear interpolation between [onTimeColor] and [missedColor] |
 *
 * @param targetTimeMs    Epoch millis of the intended wake time.
 * @param actualScanTimeMs Epoch millis of the NFC scan, or `null` if the user never scanned.
 * @param bufferDurationMs Duration of the grace window **in milliseconds**.
 * @param onTimeColor      Color representing a perfectly on-time scan.
 * @param missedColor      Color representing a fully missed / expired scan.
 *
 * @return The interpolated [Color] to render for this log entry.
 */
fun calculateWakeDotColor(
    targetTimeMs: Long,
    actualScanTimeMs: Long?,
    bufferDurationMs: Long,
    onTimeColor: Color = WakeGreen,
    missedColor: Color = WakeRed
): Color {
    // No scan at all → fully missed
    if (actualScanTimeMs == null) return missedColor

    // On time or early
    if (actualScanTimeMs <= targetTimeMs) return onTimeColor

    // Zero or negative buffer means there's no grace window — any lateness is a miss
    if (bufferDurationMs <= 0L) return missedColor

    // Past the buffer window → fully missed
    val lateness = actualScanTimeMs - targetTimeMs
    if (lateness >= bufferDurationMs) return missedColor

    // Within the buffer — interpolate
    val fraction = (lateness.toFloat() / bufferDurationMs.toFloat()).coerceIn(0f, 1f)
    return lerp(onTimeColor, missedColor, fraction)
}
