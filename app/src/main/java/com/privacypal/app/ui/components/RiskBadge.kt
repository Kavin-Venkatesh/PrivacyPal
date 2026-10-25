package com.privacypal.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.privacypal.app.domain.risk.RiskLevel
import com.privacypal.app.ui.risk.labelRes
import com.privacypal.app.ui.theme.privacyPalColors

/** Risk level as icon + text on the risk colour. Never relies on colour alone. */
@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    val (container, content) = riskColors(level)
    val label = stringResource(level.labelRes())
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        // Read once ("High risk"), not icon + text.
        modifier = modifier.clearAndSetSemantics { contentDescription = label },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = level.icon, contentDescription = label, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun riskColors(level: RiskLevel): Pair<Color, Color> {
    val colors = MaterialTheme.privacyPalColors
    return when (level) {
        RiskLevel.HIGH -> colors.riskHigh to colors.onRiskHigh
        RiskLevel.MEDIUM -> colors.riskMedium to colors.onRiskMedium
        RiskLevel.LOW -> colors.riskLow to colors.onRiskLow
        RiskLevel.NONE -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
    }
}

private val RiskLevel.icon: ImageVector
    get() = when (this) {
        RiskLevel.HIGH -> Icons.Outlined.ErrorOutline
        RiskLevel.MEDIUM -> Icons.Outlined.Info
        RiskLevel.LOW -> Icons.Outlined.CheckCircle
        RiskLevel.NONE -> Icons.Outlined.Shield
    }
