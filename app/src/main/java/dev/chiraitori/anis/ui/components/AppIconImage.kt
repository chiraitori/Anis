package dev.chiraitori.anis.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chiraitori.anis.ui.theme.shapes.ShapeCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconCache {
    private val memoryCache = LruCache<String, ImageBitmap>(300)

    fun get(packageName: String): ImageBitmap? = memoryCache.get(packageName)

    fun put(packageName: String, bitmap: ImageBitmap) {
        memoryCache.put(packageName, bitmap)
    }

    suspend fun loadIcon(context: Context, packageName: String): ImageBitmap? {
        val cached = get(packageName)
        if (cached != null) return cached

        return withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val drawable = pm.getApplicationIcon(packageName)
                val bitmap = safeDrawableToBitmap(drawable) ?: return@withContext null
                val imageBitmap = bitmap.asImageBitmap()
                put(packageName, imageBitmap)
                imageBitmap
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun safeDrawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceIn(48, 128) else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceIn(48, 128) else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * High-performance Android application icon renderer with butter-smooth 120Hz scrolling.
 */
@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val cachedBitmap = remember(packageName) { AppIconCache.get(packageName) }
    val imageBitmapState = remember(packageName) { mutableStateOf(cachedBitmap) }

    if (imageBitmapState.value == null) {
        LaunchedEffect(packageName) {
            val loaded = AppIconCache.loadIcon(context, packageName)
            if (loaded != null) {
                imageBitmapState.value = loaded
            }
        }
    }

    val bitmap = imageBitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(ShapeCache.corner14)
        )
    } else {
        Surface(
            shape = ShapeCache.corner14,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Android,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}
