/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.nikhil.yt.ui.player

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.nikhil.yt.LocalDownloadUtil
import com.nikhil.yt.LocalPlayerConnection
import com.nikhil.yt.R
import com.nikhil.yt.constants.DarkModeKey
import com.nikhil.yt.constants.DisableBlurKey
import com.nikhil.yt.constants.PlayerBackgroundStyle
import com.nikhil.yt.constants.PlayerBackgroundStyleKey
import com.nikhil.yt.constants.PlayerButtonsStyle
import com.nikhil.yt.constants.ShowVUMeterKey
import com.nikhil.yt.constants.PlayerButtonsStyleKey
import com.nikhil.yt.constants.PlayerCustomBlurKey
import com.nikhil.yt.constants.PlayerCustomBrightnessKey
import com.nikhil.yt.constants.PlayerCustomContrastKey
import com.nikhil.yt.constants.PlayerCustomImageUriKey
import com.nikhil.yt.constants.PlayerDesignStyle
import com.nikhil.yt.constants.PlayerDesignStyleKey
import com.nikhil.yt.constants.QueuePeekHeight
import com.nikhil.yt.constants.SliderStyle
import com.nikhil.yt.constants.SliderStyleKey
import com.nikhil.yt.constants.UseNewMiniPlayerDesignKey
import com.nikhil.yt.extensions.metadata
import com.nikhil.yt.extensions.togglePlayPause
import com.nikhil.yt.innertube.toHighResThumbnail
import com.nikhil.yt.models.MediaMetadata
import com.nikhil.yt.ui.component.BigSeekBar
import com.nikhil.yt.ui.component.BottomSheet
import com.nikhil.yt.ui.component.VuMeter
import com.nikhil.yt.ui.component.BottomSheetState
import com.nikhil.yt.ui.component.LocalBottomSheetPageState
import com.nikhil.yt.ui.component.LocalMenuState
import com.nikhil.yt.ui.component.rememberBottomSheetState
import com.nikhil.yt.ui.menu.PlayerMenu
import com.nikhil.yt.ui.screens.settings.DarkMode
import com.nikhil.yt.ui.theme.PlayerColorExtractor
import com.nikhil.yt.ui.utils.ShowMediaInfo
import com.nikhil.yt.utils.rememberEnumPreference
import com.nikhil.yt.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current

    val bottomSheetPageState = LocalBottomSheetPageState.current

    val playerConnection = LocalPlayerConnection.current ?: return

    val playerDesignStyle by rememberEnumPreference(
        key = PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.V3
    )

    val (useNewMiniPlayerDesign) = rememberPreference(
        UseNewMiniPlayerDesignKey,
        defaultValue = true
    )

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.COLORING
    )

    val (playerCustomImageUri) = rememberPreference(PlayerCustomImageUriKey, "")
    val (playerCustomBlur) = rememberPreference(PlayerCustomBlurKey, 0f)
    val (playerCustomContrast) = rememberPreference(PlayerCustomContrastKey, 1f)
    val (playerCustomBrightness) = rememberPreference(PlayerCustomBrightnessKey, 1f)

    val (disableBlur) = rememberPreference(DisableBlurKey, true)
    val (showCodecOnPlayer) = rememberPreference(
        booleanPreferencesKey("show_codec_on_player"),
        false
    )

    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.SECONDARY
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }
    if (useNewMiniPlayerDesign) {
        if (useBlackBackground && state.value > state.collapsedBound) {
            val progress =
                ((state.value - state.collapsedBound) / (state.expandedBound - state.collapsedBound))
                    .coerceIn(0f, 1f)
            Color.Black.copy(alpha = progress)
        } else {
            val progress =
                ((state.value - state.collapsedBound) / (state.expandedBound - state.collapsedBound))
                    .coerceIn(0f, 1f)
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = progress)
        }
    } else {
        if (useBlackBackground) {
            lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentSongLiked = currentSong?.song?.liked == true
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val (thumbnailCornerRadius, _) = rememberPreference(
        key = com.nikhil.yt.constants.ThumbnailCornerRadiusKey,
        defaultValue = 16f
    )

    var showVUMeter by rememberPreference(ShowVUMeterKey, false)

    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.Circular)

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    val isLoading = playbackState == STATE_BUFFERING || sliderPosition != null

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    var previousThumbnailUrl by remember { mutableStateOf<String?>(null) }
    var previousGradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val defaultGradientColors =
        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id) {
        val currentThumbnail = mediaMetadata?.thumbnailUrl
        if (currentThumbnail != previousThumbnailUrl) {
            previousThumbnailUrl = currentThumbnail
            previousGradientColors = gradientColors
        }
    }

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.COLORING || playerBackground == PlayerBackgroundStyle.BLUR_GRADIENT || playerBackground == PlayerBackgroundStyle.GLOW || playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                } else {
                    val request = ImageRequest.Builder(context)
                        .data(currentMetadata.thumbnailUrl)
                        .size(
                            PlayerColorExtractor.Config.IMAGE_SIZE,
                            PlayerColorExtractor.Config.IMAGE_SIZE
                        )
                        .allowHardware(false)
                        .build()

                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            context.imageLoader.execute(request)
                        }
                    }.getOrNull()

                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                    .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                    .generate()
                            }

                            val extractedColors = PlayerColorExtractor.extractGradientColors(
                                palette = palette,
                                fallbackColor = fallbackColor
                            )

                            gradientColorsCache[currentMetadata.id] = extractedColors
                            gradientColors = extractedColors
                        } else {
                            gradientColors = defaultGradientColors
                        }
                    } else {
                        gradientColors = defaultGradientColors
                    }
                }
            } else {
                gradientColors = emptyList()
            }
        } else {
            gradientColors = emptyList()
        }
    }

    state.expandedBound / 3

    val TextBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.COLORING -> Color.White
            PlayerBackgroundStyle.BLUR_GRADIENT -> Color.White
            PlayerBackgroundStyle.GLOW -> Color.White
            PlayerBackgroundStyle.GLOW_ANIMATED -> Color.White
            PlayerBackgroundStyle.CUSTOM -> Color.White
        }

    val icBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            PlayerBackgroundStyle.GRADIENT -> Color.Black
            PlayerBackgroundStyle.COLORING -> Color.Black
            PlayerBackgroundStyle.BLUR_GRADIENT -> Color.Black
            PlayerBackgroundStyle.GLOW -> Color.Black
            PlayerBackgroundStyle.GLOW_ANIMATED -> Color.Black
            PlayerBackgroundStyle.CUSTOM -> Color.Black
        }

    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT -> Pair(TextBackgroundColor, icBackgroundColor)
        PlayerButtonsStyle.SECONDARY -> Pair(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
    }

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    OutlinedIconButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        },
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            },
        )
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    val dynamicQueuePeekHeight =
        if (showCodecOnPlayer) {
            88.dp
        } else {
            QueuePeekHeight
        }

    val dismissedBound =
        dynamicQueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = dismissedBound,
        expandedBound = state.expandedBound,
        collapsedBound = dismissedBound + 1.dp,
        initialAnchor = 1
    )

    val lyricsSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        expandedBound = state.expandedBound,
        collapsedBound = 0.dp,
        initialAnchor = 1
    )

    BackHandler(
        enabled =
            (!lyricsSheetState.isCollapsed && !lyricsSheetState.isDismissed) ||
                    (!queueSheetState.isCollapsed && !queueSheetState.isDismissed) ||
                    (!state.isCollapsed && !state.isDismissed)
    ) {
        when {
            !lyricsSheetState.isCollapsed && !lyricsSheetState.isDismissed -> lyricsSheetState.collapseSoft()
            !queueSheetState.isCollapsed && !queueSheetState.isDismissed -> queueSheetState.collapseSoft()
            !state.isCollapsed && !state.isDismissed -> state.collapseSoft()
        }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        backgroundColor = when (playerBackground) {
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> {
                val progress =
                    ((state.value - state.collapsedBound) / (state.expandedBound - state.collapsedBound))
                        .coerceIn(0f, 1f)

                val fadeProgress = if (progress < 0.2f) {
                    ((0.2f - progress) / 0.2f).coerceIn(0f, 1f)
                } else {
                    0f
                }

                MaterialTheme.colorScheme.surface.copy(alpha = 1f - fadeProgress)
            }

            else -> {
                val progress =
                    ((state.value - state.collapsedBound) / (state.expandedBound - state.collapsedBound))
                        .coerceIn(0f, 1f)

                val fadeProgress = if (progress < 0.2f) {
                    ((0.2f - progress) / 0.2f).coerceIn(0f, 1f)
                } else {
                    0f
                }

                if (useBlackBackground) {
                    Color.Black.copy(alpha = 1f - fadeProgress)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 1f - fadeProgress)
                }
            }
        },
        onDismiss = {
            playerConnection.service.stopAndClearPlayback()
        },
        collapsedContent = {
            MiniPlayer(
                position = position,
                duration = duration,
                pureBlack = pureBlack,
            )
        },
    ) {
        val onSliderValueChange: (Long) -> Unit = { sliderPosition = it }
        val onSliderValueChangeFinished: () -> Unit = {
            sliderPosition?.let {
                playerConnection.player.seekTo(it)
                position = it
            }
            sliderPosition = null
        }
        duration > 0L && duration != C.TIME_UNSET

        remember(queueWindows, currentWindowIndex) {
            queueWindows.getOrNull(currentWindowIndex + 1)?.mediaItem?.metadata
        }

        val enrichedMetadata = remember(mediaMetadata, currentSong) {
            val meta = mediaMetadata ?: return@remember null
            if (meta.album != null) return@remember meta
            val dbAlbum = currentSong?.album
            val dbAlbumId = currentSong?.song?.albumId
            when {
                dbAlbum != null -> meta.copy(
                    album = MediaMetadata.Album(id = dbAlbum.id, title = dbAlbum.title)
                )

                dbAlbumId != null -> meta.copy(
                    album = MediaMetadata.Album(
                        id = dbAlbumId,
                        title = currentSong?.song?.albumName.orEmpty()
                    )
                )

                else -> meta
            }
        }

        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            PlayerControlsContent(
                mediaMetadata = mediaMetadata,
                playerDesignStyle = playerDesignStyle,
                sliderStyle = sliderStyle,
                playbackState = playbackState,
                isPlaying = isPlaying,
                isLoading = isLoading,
                repeatMode = repeatMode,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                textButtonColor = textButtonColor,
                iconButtonColor = iconButtonColor,
                textBackgroundColor = TextBackgroundColor,
                icBackgroundColor = icBackgroundColor,
                sliderPosition = sliderPosition,
                position = position,
                duration = duration,
                playerConnection = playerConnection,
                navController = navController,
                state = state,
                menuState = menuState,
                bottomSheetPageState = bottomSheetPageState,
                clipboardManager = clipboardManager,
                context = context,
                onSliderValueChange = onSliderValueChange,
                onSliderValueChangeFinished = onSliderValueChangeFinished,
                showVUMeter = true
            )
        }

        if (!state.isCollapsed) {
            PlayerBackground(
                playerBackground = playerBackground,
                mediaMetadata = mediaMetadata,
                gradientColors = gradientColors,
                disableBlur = disableBlur,
                playerCustomImageUri = playerCustomImageUri,
                playerCustomBlur = playerCustomBlur,
                playerCustomContrast = playerCustomContrast,
                playerCustomBrightness = playerCustomBrightness
            )
        }

// distance

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                if (playerDesignStyle == PlayerDesignStyle.V5) {
                    enrichedMetadata?.let { metadata ->
                        MetroPlayerContent(
                            mediaMetadata = metadata,
                            sliderPosition = sliderPosition,
                            positionMs = position,
                            durationMs = duration,
                            textColor = TextBackgroundColor,
                            liked = currentSongLiked,
                            playerConnection = playerConnection,
                            onToggleLike = playerConnection::toggleLike,
                            onExpandQueue = queueSheetState::expandSoft,
                            onMenuClick = {
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = metadata,
                                        navController = navController,
                                        playerBottomSheetState = state,
                                        onShowDetailsDialog = {
                                            bottomSheetPageState.show { ShowMediaInfo(metadata.id) }
                                        },
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            },
                            context = context,
                            bottomPadding = dynamicQueuePeekHeight

                        )
                    }

                } else {
                    Row(
                        modifier =
                            Modifier
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                .padding(bottom = queueSheetState.collapsedBound + 48.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f),
                        ) {
                            val screenWidth = LocalConfiguration.current.screenWidthDp
                            val thumbnailSize = (screenWidth * 0.4).dp
                            if (showVUMeter) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(16f / 9f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        VuMeter(
                                            modifier = Modifier.fillMaxSize(),
                                            isPlayerExpanded = state.isExpanded,
                                            cornerRadius = thumbnailCornerRadius,
                                            isWide = true
                                        )
                                        if (isLoading) {
                                            com.nikhil.yt.ui.component.VeluneLoader(
                                                size = 48.dp,
                                                color = TextBackgroundColor,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.4f))
                                                .clickable { showVUMeter = false },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.image),
                                                contentDescription = "Show artwork",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val currentMetadata = enrichedMetadata ?: mediaMetadata
                                    currentMetadata?.let { metadata ->
                                        PlayerTitleSection(
                                            mediaMetadata = metadata,
                                            textBackgroundColor = TextBackgroundColor,
                                            navController = navController,
                                            state = state,
                                            clipboardManager = clipboardManager,
                                            context = context
                                        )
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.size(thumbnailSize)) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Thumbnail(
                                                sliderPositionProvider = { sliderPosition },
                                                modifier = Modifier.fillMaxSize(),
                                                isPlayerExpanded = state.isExpanded
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.4f))
                                                    .clickable { showVUMeter = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.tune),
                                                    contentDescription = "Show VU meter",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val currentMetadata = enrichedMetadata ?: mediaMetadata
                                        currentMetadata?.let { metadata ->
                                            PlayerTitleSection(
                                                mediaMetadata = metadata,
                                                textBackgroundColor = TextBackgroundColor,
                                                navController = navController,
                                                state = state,
                                                clipboardManager = clipboardManager,
                                                context = context
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .windowInsetsPadding(
                                        WindowInsets.systemBars.only(
                                            WindowInsetsSides.Top
                                        )
                                    ),
                        ) {
                            Spacer(Modifier.weight(1f))

                            enrichedMetadata?.let {
                                controlsContent(it)
                            }

                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            else -> {
                if (playerDesignStyle == PlayerDesignStyle.V5) {
                    enrichedMetadata?.let { metadata ->
                        MetroPlayerContent(
                            mediaMetadata = metadata,
                            sliderPosition = sliderPosition,
                            positionMs = position,
                            durationMs = duration,
                            textColor = TextBackgroundColor,
                            liked = currentSongLiked,
                            playerConnection = playerConnection,
                            onToggleLike = playerConnection::toggleLike,
                            onExpandQueue = queueSheetState::expandSoft,
                            onMenuClick = {
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = metadata,
                                        navController = navController,
                                        playerBottomSheetState = state,
                                        onShowDetailsDialog = {
                                            bottomSheetPageState.show { ShowMediaInfo(metadata.id) }
                                        },
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            },
                            context = context,
                            bottomPadding = dynamicQueuePeekHeight

                        )
                    }

                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                .padding(bottom = queueSheetState.collapsedBound),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (showVUMeter) {
                                Box(
                                    modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxSize().statusBarsPadding()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.now_playing),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextBackgroundColor
                                            )
                                            val currentMetadata = enrichedMetadata ?: mediaMetadata
                                            val playingFrom = queueTitle ?: currentMetadata?.album?.title
                                            if (!playingFrom.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = playingFrom,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = TextBackgroundColor.copy(alpha = 0.8f),
                                                    maxLines = 1,
                                                    modifier = Modifier.basicMarquee(),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }

                                        val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
                                        val vuSize = screenWidth - (com.nikhil.yt.constants.PlayerHorizontalPadding * 2)

                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(vuSize)
                                                    .aspectRatio(16f / 9f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                VuMeter(
                                                    modifier = Modifier.fillMaxSize(),
                                                    isPlayerExpanded = state.isExpanded,
                                                    cornerRadius = thumbnailCornerRadius,
                                                    isWide = true
                                                )
                                                if (isLoading) {
                                                    com.nikhil.yt.ui.component.VeluneLoader(
                                                        size = 48.dp,
                                                        color = TextBackgroundColor,
                                                        modifier = Modifier.align(Alignment.Center)
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.4f))
                                                        .clickable { showVUMeter = false },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.image),
                                                        contentDescription = "Show artwork",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        val currentMetadata = enrichedMetadata ?: mediaMetadata
                                        currentMetadata?.let { metadata ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = com.nikhil.yt.constants.PlayerHorizontalPadding)
                                            ) {
                                                PlayerTitleSection(
                                                    mediaMetadata = metadata,
                                                    textBackgroundColor = TextBackgroundColor,
                                                    navController = navController,
                                                    state = state,
                                                    clipboardManager = clipboardManager,
                                                    context = context
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
                                        val vuSize = screenWidth - (com.nikhil.yt.constants.PlayerHorizontalPadding * 2)

                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(vuSize)
                                                    .aspectRatio(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Thumbnail(
                                                    sliderPositionProvider = { sliderPosition },
                                                    modifier = Modifier.fillMaxSize(),
                                                    isPlayerExpanded = state.isExpanded
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.4f))
                                                        .clickable { showVUMeter = true },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.tune),
                                                        contentDescription = "Show VU meter",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        val currentMetadata = enrichedMetadata ?: mediaMetadata
                                        currentMetadata?.let { metadata ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = com.nikhil.yt.constants.PlayerHorizontalPadding)
                                            ) {
                                                PlayerTitleSection(
                                                    mediaMetadata = metadata,
                                                    textBackgroundColor = TextBackgroundColor,
                                                    navController = navController,
                                                    state = state,
                                                    clipboardManager = clipboardManager,
                                                    context = context
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }

                        enrichedMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.height(30.dp))
                    }
                }
            }
        }

        val queueOnBackgroundColor =
            if (useBlackBackground) Color.White else MaterialTheme.colorScheme.onSurface
        val queueSurfaceColor =
            if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.surface

        val (_, _) = when (playerButtonsStyle) {
            PlayerButtonsStyle.DEFAULT -> Pair(queueOnBackgroundColor, queueSurfaceColor)
            PlayerButtonsStyle.SECONDARY -> Pair(
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.onSecondary
            )
        }

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            backgroundColor =
                if (useBlackBackground) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            onBackgroundColor = queueOnBackgroundColor,
            TextBackgroundColor = TextBackgroundColor,
            textButtonColor = textButtonColor,
            iconButtonColor = iconButtonColor,
            onShowLyrics = { lyricsSheetState.expandSoft() },
            pureBlack = pureBlack,
        )

        mediaMetadata?.let { metadata ->
            BottomSheet(
                state = lyricsSheetState,
                backgroundColor = Color.Unspecified,
                onDismiss = { /* Optional dismiss action */ },
                collapsedContent = {
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = lyricsSheetState.progress.coerceIn(0f, 1f)
                            )
                        )
                ) {
                    LyricsScreen(
                        mediaMetadata = metadata,
                        onBackClick = { lyricsSheetState.collapseSoft() },
                        navController = navController
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetroPlayerContent(
    mediaMetadata: MediaMetadata,
    sliderPosition: Long?,
    positionMs: Long,
    durationMs: Long,
    textColor: Color,
    liked: Boolean,
    playerConnection: com.nikhil.yt.playback.PlayerConnection,
    onToggleLike: () -> Unit,
    onExpandQueue: () -> Unit,
    onMenuClick: () -> Unit,
    context: Context,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val isLoading = playbackState == STATE_BUFFERING
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.systemBars.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.7f)
            )
            Text(
                text = mediaMetadata.album?.title ?: "Playing from Library",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            coil3.compose.AsyncImage(
                model = mediaMetadata.thumbnailUrl?.toHighResThumbnail(),
                contentDescription = "Album Art",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = com.nikhil.yt.constants.PlayerHorizontalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata.title ?: "Unknown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = mediaMetadata.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    onClick = {
                        val intent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND; type =
                            "text/plain"; putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                        )
                        }; context.startActivity(android.content.Intent.createChooser(intent, null))
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = "Share",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    onClick = onToggleLike,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
                            contentDescription = "Like",
                            tint = if (liked) MaterialTheme.colorScheme.error else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val displayPositionMs = sliderPosition ?: positionMs
            StyledPlaybackSlider(
                sliderStyle = SliderStyle.Wavy,
                value = (displayPositionMs.toFloat() / durationMs.coerceAtLeast(1L)).coerceIn(
                    0f,
                    1f
                ),
                valueRange = 0f..1f,
                onValueChange = { fraction -> playerConnection.player.seekTo((durationMs * fraction).toLong()) },
                onValueChangeFinished = {},
                activeColor = textColor,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .offset(y = (-8).dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = com.nikhil.yt.utils.makeTimeString(displayPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
                Text(
                    text = if (durationMs != C.TIME_UNSET) com.nikhil.yt.utils.makeTimeString(
                        durationMs
                    ) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val playInteractionSource =
                androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPlayPressed by playInteractionSource.collectIsPressedAsState()
            val sideButtonWidth by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isPlayPressed) 48.dp else 64.dp,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.6f,
                    stiffness = 400f
                ),
                label = "SideWidth"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = playerConnection::seekToPrevious,
                    shape = RoundedCornerShape(50),
                    color = textColor.copy(alpha = 0.08f),
                    modifier = Modifier
                        .width(sideButtonWidth)
                        .height(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            tint = textColor.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Surface(
                    onClick = {
                        if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                            playerConnection.player.seekTo(
                                0,
                                0
                            ); playerConnection.player.playWhenReady = true
                        } else playerConnection.player.togglePlayPause()
                    },
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    interactionSource = playInteractionSource,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isLoading) com.nikhil.yt.ui.component.VeluneLoader(size = 24.dp)
                        else {
                            Icon(
                                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPlaying) "Pause" else "Play",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black
                            )
                        }
                    }
                }
                Surface(
                    onClick = playerConnection::seekToNext,
                    shape = RoundedCornerShape(50),
                    color = textColor.copy(alpha = 0.08f),
                    modifier = Modifier
                        .width(sideButtonWidth)
                        .height(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            tint = textColor.copy(alpha = if (canSkipNext) 1f else 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val playerVolume by playerConnection.service.playerVolume.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    painter = painterResource(R.drawable.volume_off),
                    contentDescription = "Min volume",
                    modifier = Modifier.size(18.dp),
                    tint = textColor.copy(alpha = 0.6f)
                )

                BigSeekBar(
                    progressProvider = { playerVolume },
                    onProgressChange = { playerConnection.service.playerVolume.value = it },
                    color = textColor,
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .padding(horizontal = 12.dp)
                )

                Icon(
                    painter = painterResource(R.drawable.volume_up),
                    contentDescription = "Max volume",
                    modifier = Modifier.size(18.dp),
                    tint = textColor.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}














