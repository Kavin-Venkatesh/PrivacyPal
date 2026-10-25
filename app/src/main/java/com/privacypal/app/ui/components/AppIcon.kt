package com.privacypal.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.privacypal.app.data.scanner.AppIconLoader

/** Provided by MainActivity. The default loads nothing, so previews and screenshots show the placeholder. */
val LocalAppIconLoader = staticCompositionLocalOf<AppIconLoader> { AppIconLoader { null } }

/** The app's launcher icon, loaded lazily; a generic icon until it loads or if the app has none. */
@Composable
fun AppIcon(packageName: String, contentDescription: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val loader = LocalAppIconLoader.current
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName, loader) {
        value = loader.load(packageName)
    }
    val current = icon
    if (current != null) {
        Image(bitmap = current, contentDescription = contentDescription, modifier = modifier.size(size))
    } else {
        Icon(
            imageVector = Icons.Outlined.Android,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(size),
        )
    }
}
