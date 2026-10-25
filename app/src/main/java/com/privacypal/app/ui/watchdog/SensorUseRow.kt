package com.privacypal.app.ui.watchdog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.privacypal.app.R
import com.privacypal.app.domain.watchdog.Confidence
import com.privacypal.app.domain.watchdog.Sensor
import com.privacypal.app.domain.watchdog.SensorUse
import com.privacypal.app.ui.components.AppIcon
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

/**
 * One microphone or camera use: who (the likely app, never certain), what, when and for how long,
 * and whether the screen was off.
 *
 * @param appLabel the likely app's name, or null to show the package name.
 */
@Composable
fun SensorUseRow(
    use: SensorUse,
    appLabel: String?,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val packageName = use.likelyApp?.packageName
    val colors = MaterialTheme.colorScheme
    val background = if (highlighted) colors.secondaryContainer else colors.surface
    val onBackground = if (highlighted) colors.onSecondaryContainer else colors.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (packageName != null) {
            AppIcon(packageName = packageName, contentDescription = appLabel ?: packageName)
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = use.sensor.icon,
                    contentDescription = stringResource(use.sensor.labelRes),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = whoText(use, appLabel),
                style = MaterialTheme.typography.bodyLarge,
                color = onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.use_summary,
                    stringResource(use.sensor.labelRes),
                    timeText(use.startedAtMs),
                    durationText(use),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = onBackground,
            )
            confidenceText(use)?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = onBackground)
            }
            if (!use.screenOn) ScreenOffLabel()
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = use.sensor.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ScreenOffLabel() {
    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.ScreenLockPortrait,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.use_screen_off),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun whoText(use: SensorUse, appLabel: String?): String {
    val likely = use.likelyApp ?: return stringResource(R.string.use_checking)
    return when {
        likely.packageName != null -> appLabel ?: likely.packageName
        likely.inForeground == false -> stringResource(R.string.use_background_app)
        else -> stringResource(R.string.use_unknown_app)
    }
}

@Composable
private fun confidenceText(use: SensorUse): String? {
    val likely = use.likelyApp ?: return null
    return when {
        likely.confidence == Confidence.HIGH -> stringResource(R.string.use_confidence_high)
        likely.confidence == Confidence.MEDIUM -> stringResource(R.string.use_confidence_medium)
        likely.confidence == Confidence.LOW -> stringResource(R.string.use_confidence_low)
        likely.inForeground == false -> stringResource(R.string.use_no_app_on_screen)
        else -> stringResource(R.string.use_no_access)
    }
}

@Composable
private fun timeText(ms: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(ms, locale) { DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(ms)) }
}

@Composable
private fun durationText(use: SensorUse): String {
    val endedAt = use.endedAtMs
    return when {
        endedAt != null -> durationText(endedAt - use.startedAtMs)
        use.interrupted -> stringResource(R.string.use_interrupted)
        else -> stringResource(R.string.use_in_progress)
    }
}

@Composable
fun durationText(ms: Long): String {
    val seconds = ((ms + 500) / 1_000).toInt().coerceAtLeast(1)
    return if (seconds < 60) {
        pluralStringResource(R.plurals.duration_seconds, seconds, seconds)
    } else {
        pluralStringResource(R.plurals.duration_minutes, seconds / 60, seconds / 60)
    }
}

/** "Today", "Yesterday" or the full date. */
@Composable
fun dayText(day: LocalDate, today: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return when (day) {
        today -> stringResource(R.string.activity_today)

        today.minusDays(1) -> stringResource(R.string.activity_yesterday)

        else -> remember(day, locale) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(day)
        }
    }
}

val Sensor.icon: ImageVector
    get() = when (this) {
        Sensor.MICROPHONE -> Icons.Outlined.Mic
        Sensor.CAMERA -> Icons.Outlined.PhotoCamera
    }

val Sensor.labelRes: Int
    get() = when (this) {
        Sensor.MICROPHONE -> R.string.activity_filter_mic
        Sensor.CAMERA -> R.string.activity_filter_camera
    }
