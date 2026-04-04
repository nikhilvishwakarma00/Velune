/*
 * Velune - by Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.nikhil.yt.LocalPlayerAwareWindowInsets
import com.nikhil.yt.LocalPlayerConnection
import com.nikhil.yt.R
import com.nikhil.yt.constants.MaxCanvasCacheSizeKey
import com.nikhil.yt.constants.MaxImageCacheSizeKey
import com.nikhil.yt.constants.MaxSongCacheSizeKey
import com.nikhil.yt.constants.SmartTrimmerKey
import com.nikhil.yt.extensions.directorySizeBytes
import com.nikhil.yt.extensions.tryOrNull
import com.nikhil.yt.ui.component.ActionPromptDialog
import com.nikhil.yt.ui.component.ListPreference
import com.nikhil.yt.ui.player.CanvasArtworkPlaybackCache
import com.nikhil.yt.ui.utils.backToMain
import com.nikhil.yt.ui.utils.formatFileSize
import com.nikhil.yt.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return
    
    val downloadCacheDir = remember { context.filesDir.resolve("download") }
    val playerCacheDir = remember { context.filesDir.resolve("exoplayer") }

    val coroutineScope = rememberCoroutineScope()
    val (smartTrimmer, onSmartTrimmerChange) = rememberPreference(
        key = SmartTrimmerKey,
        defaultValue = false
    )
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )
    val (maxCanvasCacheSize, onMaxCanvasCacheSizeChange) = rememberPreference(
        key = MaxCanvasCacheSizeKey,
        defaultValue = 256,
    )
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }
    var clearCanvasCacheDialog by remember { mutableStateOf(false) }

    var showSongCacheSizeDialog by remember { mutableStateOf(false) }
    var showImageCacheSizeDialog by remember { mutableStateOf(false) }
    var showCanvasCacheSizeDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember { mutableStateOf(imageDiskCache.size) }
    var playerCacheSize by remember { mutableStateOf(0L) }
    var downloadCacheSize by remember { mutableStateOf(0L) }
    var canvasCacheSize by remember { mutableStateOf(CanvasArtworkPlaybackCache.size()) }
    
    val imageCacheProgress by animateFloatAsState(
        targetValue = if (imageDiskCache.maxSize > 0) {
            (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f)
        } else 0f,
        label = "imageCacheProgress",
    )
    val maxSongCacheSizeBytes = if (maxSongCacheSize > 0) maxSongCacheSize * 1024 * 1024L else 0L
    val playerCacheProgress by animateFloatAsState(
        targetValue = if (maxSongCacheSizeBytes > 0) {
            (playerCacheSize.toFloat() / maxSongCacheSizeBytes).coerceIn(0f, 1f)
        } else 0f,
        label = "playerCacheProgress",
    )
    val canvasCacheProgress by animateFloatAsState(
        targetValue = if (maxCanvasCacheSize > 0) {
            (canvasCacheSize.toFloat() / maxCanvasCacheSize).coerceIn(0f, 1f)
        } else 0f,
        label = "canvasCacheProgress",
    )

    val isSmartTrimmerAvailable = maxImageCacheSize != 0 || maxSongCacheSize != 0
    LaunchedEffect(isSmartTrimmerAvailable) {
        if (!isSmartTrimmerAvailable && smartTrimmer) onSmartTrimmerChange(false)
    }

    LaunchedEffect(maxImageCacheSize) {
        if (maxImageCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                imageDiskCache.clear()
                com.nikhil.yt.utils.ArtworkStorage.clear(context)
            }
        }
    }
    LaunchedEffect(maxSongCacheSize) {
        if (maxSongCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                playerCache.keys.forEach { key ->
                    playerCache.removeResource(key)
                }
            }
        }
    }
    LaunchedEffect(maxCanvasCacheSize) {
        CanvasArtworkPlaybackCache.setMaxSize(maxCanvasCacheSize)
        if (maxCanvasCacheSize == 0) {
            CanvasArtworkPlaybackCache.clear()
        }
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache, playerCacheDir) {
        while (isActive) {
            delay(500)
            playerCacheSize =
                withContext(Dispatchers.IO) {
                    val cacheSpace = tryOrNull { playerCache.cacheSpace } ?: 0L
                    if (cacheSpace == 0L) playerCacheDir.directorySizeBytes() else cacheSpace
                }
        }
    }
    LaunchedEffect(downloadCache, downloadCacheDir) {
        while (isActive) {
            delay(500)
            downloadCacheSize =
                withContext(Dispatchers.IO) {
                    val cacheSpace = tryOrNull { downloadCache.cacheSpace } ?: 0L
                    if (cacheSpace == 0L) downloadCacheDir.directorySizeBytes() else cacheSpace
                }
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(500)
            canvasCacheSize = CanvasArtworkPlaybackCache.size()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.storage)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.smart_trimmer),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = stringResource(R.string.smart_trimmer_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, end = 16.dp)
                        )
                    }
                    Switch(
                        checked = smartTrimmer && isSmartTrimmerAvailable,
                        onCheckedChange = onSmartTrimmerChange,
                        enabled = isSmartTrimmerAvailable
                    )
                }
            }

            item {
                NewCacheCard(
                    icon = R.drawable.ic_download,
                    title = stringResource(R.string.downloaded_songs),
                    description = stringResource(R.string.size_used, formatFileSize(downloadCacheSize)),
                    progress = null,
                    showMaxCacheSize = false,
                    maxCacheSizeText = null,
                    clearActionText = stringResource(R.string.clear_all_downloads),
                    onClearClick = { clearDownloads = true }
                )
            }

            item {
                val songDesc = if (maxSongCacheSize == -1) {
                    stringResource(R.string.size_used, formatFileSize(playerCacheSize))
                } else {
                    "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}"
                }
                val maxCacheText = when (maxSongCacheSize) {
                    0 -> stringResource(R.string.disable)
                    -1 -> stringResource(R.string.unlimited)
                    else -> formatFileSize(maxSongCacheSize * 1024 * 1024L)
                }
                
                NewCacheCard(
                    icon = R.drawable.ic_music,
                    title = stringResource(R.string.song_cache),
                    description = songDesc,
                    progress = if (maxSongCacheSize > 0) playerCacheProgress else null,
                    showMaxCacheSize = true,
                    maxCacheSizeText = maxCacheText,
                    clearActionText = stringResource(R.string.clear_song_cache),
                    onClearClick = { clearCacheDialog = true },
                    onMaxCacheClick = { showSongCacheSizeDialog = true }
                )
            }

            item {
                val imgDesc = if (maxImageCacheSize > 0) {
                    "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
                } else {
                    stringResource(R.string.disable)
                }
                val imgMaxText = when (maxImageCacheSize) {
                    0 -> stringResource(R.string.disable)
                    else -> formatFileSize(maxImageCacheSize * 1024 * 1024L)
                }
                
                NewCacheCard(
                    icon = R.drawable.image,
                    title = stringResource(R.string.image_cache),
                    description = imgDesc,
                    progress = if (maxImageCacheSize > 0) imageCacheProgress else null,
                    showMaxCacheSize = true,
                    maxCacheSizeText = imgMaxText,
                    clearActionText = stringResource(R.string.clear_image_cache),
                    onClearClick = { clearImageCacheDialog = true },
                    onMaxCacheClick = { showImageCacheSizeDialog = true }
                )
            }

            item {
                val canvasDesc = if (maxCanvasCacheSize > 0) {
                    stringResource(
                        R.string.canvas_cache_usage,
                        stringResource(R.string.canvas_cache_items, canvasCacheSize),
                        stringResource(R.string.canvas_cache_items, maxCanvasCacheSize),
                    )
                } else {
                    stringResource(R.string.disable)
                }
                val canvasMaxText = when (maxCanvasCacheSize) {
                    0 -> stringResource(R.string.disable)
                    else -> stringResource(R.string.canvas_cache_items, maxCanvasCacheSize)
                }
                
                NewCacheCard(
                    icon = R.drawable.motion_photos_on,
                    title = stringResource(R.string.canvas_cache),
                    description = canvasDesc,
                    progress = if (maxCanvasCacheSize > 0) canvasCacheProgress else null,
                    showMaxCacheSize = true,
                    maxCacheSizeText = canvasMaxText,
                    clearActionText = stringResource(R.string.clear_canvas_cache),
                    onClearClick = { clearCanvasCacheDialog = true },
                    onMaxCacheClick = { showCanvasCacheSizeDialog = true }
                )
            }
        }

        if (clearDownloads) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_all_downloads),
                onDismiss = { clearDownloads = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        downloadCache.keys.forEach { key ->
                            downloadCache.removeResource(key)
                        }
                    }
                    clearDownloads = false
                },
                onCancel = { clearDownloads = false },
                content = { Text(text = stringResource(R.string.clear_downloads_dialog)) }
            )
        }

        if (clearCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_song_cache),
                onDismiss = { clearCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        playerCache.keys.forEach { key ->
                            playerCache.removeResource(key)
                        }
                    }
                    clearCacheDialog = false
                },
                onCancel = { clearCacheDialog = false },
                content = { Text(text = stringResource(R.string.clear_song_cache_dialog)) }
            )
        }

        if (clearImageCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_image_cache),
                onDismiss = { clearImageCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        imageDiskCache.clear()
                        com.nikhil.yt.utils.ArtworkStorage.clear(context)
                    }
                    clearImageCacheDialog = false
                },
                onCancel = { clearImageCacheDialog = false },
                content = { Text(text = stringResource(R.string.clear_image_cache_dialog)) }
            )
        }

        if (clearCanvasCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_canvas_cache),
                onDismiss = { clearCanvasCacheDialog = false },
                onConfirm = {
                    CanvasArtworkPlaybackCache.clear()
                    clearCanvasCacheDialog = false
                },
                onCancel = { clearCanvasCacheDialog = false },
                content = { Text(text = stringResource(R.string.clear_canvas_cache_dialog)) }
            )
        }
        
        // Settings Dialogs (since we removed ListPreference components, we re-implement the dialogs here)
        if (showSongCacheSizeDialog) {
            MaxCacheSizeDialog(
                title = stringResource(R.string.max_cache_size),
                selectedValue = maxSongCacheSize,
                values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
                valueToText = { 
                    when (it) {
                        0 -> stringResource(R.string.disable)
                        -1 -> stringResource(R.string.unlimited)
                        else -> formatFileSize(it * 1024 * 1024L)
                    }
                },
                onDismiss = { showSongCacheSizeDialog = false },
                onSelect = { 
                    onMaxSongCacheSizeChange(it)
                    showSongCacheSizeDialog = false 
                }
            )
        }
        
        if (showImageCacheSizeDialog) {
            MaxCacheSizeDialog(
                title = stringResource(R.string.max_cache_size),
                selectedValue = maxImageCacheSize,
                values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
                valueToText = { 
                    when (it) {
                        0 -> stringResource(R.string.disable)
                        else -> formatFileSize(it * 1024 * 1024L)
                    }
                },
                onDismiss = { showImageCacheSizeDialog = false },
                onSelect = { 
                    onMaxImageCacheSizeChange(it)
                    showImageCacheSizeDialog = false 
                }
            )
        }
        
        if (showCanvasCacheSizeDialog) {
            MaxCacheSizeDialog(
                title = stringResource(R.string.max_cache_size),
                selectedValue = maxCanvasCacheSize,
                values = listOf(0, 64, 128, 256, 512, 1024),
                valueToText = { 
                    when (it) {
                        0 -> stringResource(R.string.disable)
                        else -> stringResource(R.string.canvas_cache_items, it)
                    }
                },
                onDismiss = { showCanvasCacheSizeDialog = false },
                onSelect = { 
                    onMaxCanvasCacheSizeChange(it)
                    showCanvasCacheSizeDialog = false 
                }
            )
        }
    }
}

@Composable
private fun MaxCacheSizeDialog(
    title: String,
    selectedValue: Int,
    values: List<Int>,
    valueToText: @Composable (Int) -> String,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    com.nikhil.yt.ui.component.DefaultDialog(
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            values.forEach { value ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(value) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = value == selectedValue,
                        onClick = { onSelect(value) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = valueToText(value),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun NewCacheCard(
    icon: Int,
    title: String,
    description: String,
    progress: Float?,
    showMaxCacheSize: Boolean,
    maxCacheSizeText: String?,
    clearActionText: String,
    onClearClick: () -> Unit,
    onMaxCacheClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (progress != null) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showMaxCacheSize && maxCacheSizeText != null) {
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMaxCacheClick() }
            ) {
                Text(
                    text = "Max cache size",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = maxCacheSizeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(if (showMaxCacheSize) 16.dp else 20.dp))
        Text(
            text = clearActionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClearClick() }
        )
    }
}

