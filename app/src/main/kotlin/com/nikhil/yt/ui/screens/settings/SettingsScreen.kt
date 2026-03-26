/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.ui.screens.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import com.nikhil.yt.App.Companion.forgetAccount
import com.nikhil.yt.BuildConfig
import com.nikhil.yt.LocalPlayerAwareWindowInsets
import com.nikhil.yt.R
import com.nikhil.yt.constants.InnerTubeCookieKey
import com.nikhil.yt.innertube.utils.parseCookieString
import com.nikhil.yt.ui.component.IconButton
import com.nikhil.yt.ui.component.TopSearch
import com.nikhil.yt.ui.utils.backToMain
import com.nikhil.yt.utils.rememberPreference
import com.nikhil.yt.viewmodels.HomeViewModel
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.layout.ContentScale

data class SettingsQuickAction(
    val icon: Painter,
    val label: String,
    val onClick: () -> Unit,
    val accentColor: Color,
)

data class SettingsCategory(
    val title: String,
    val items: List<PremiumSettingsItem>,
)

data class PremiumSettingsItem(
    val icon: Painter,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val showUpdateIndicator: Boolean = false,
    val accentColor: Color = Color.Unspecified,
    val keywords: List<String> = emptyList(),
    val onClick: () -> Unit,
)

data class SettingsIntegrationAction(
    val icon: Painter,
    val label: String,
    val onClick: () -> Unit,
    val accentColor: Color,
)

private fun filterQuickActions(
    actions: List<SettingsQuickAction>,
    query: String,
): List<SettingsQuickAction> {
    if (query.isBlank()) return actions
    return actions.filter { it.label.contains(query, ignoreCase = true) }
}

private fun filterSettingsCategories(
    categories: List<SettingsCategory>,
    query: String,
): List<SettingsCategory> {
    if (query.isBlank()) return categories
    return categories.mapNotNull { category ->
        if (category.title.contains(query, ignoreCase = true)) {
            category
        } else {
            val filteredItems = category.items.filter { item ->
                matchesSearchQuery(item, query)
            }
            if (filteredItems.isEmpty()) null else category.copy(items = filteredItems)
        }
    }
}

private fun matchesSearchQuery(
    item: PremiumSettingsItem,
    query: String,
): Boolean {
    if (item.title.contains(query, ignoreCase = true)) return true
    if (item.subtitle?.contains(query, ignoreCase = true) == true) return true
    if (item.badge?.contains(query, ignoreCase = true) == true) return true
    return item.keywords.any { keyword ->
        keyword.contains(query, ignoreCase = true) ||
                query.contains(keyword, ignoreCase = true)
    }
}

private fun filterInternalSettingsItems(
    items: List<PremiumSettingsItem>,
    query: String,
): List<PremiumSettingsItem> {
    if (query.isBlank()) return emptyList()
    return items.filter { item -> matchesSearchQuery(item, query) }
}

private fun filterIntegrations(
    integrations: List<SettingsIntegrationAction>,
    query: String,
): List<SettingsIntegrationAction> {
    if (query.isBlank()) return integrations
    return integrations.filter { it.label.contains(query, ignoreCase = true) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val listState = rememberLazyListState()

    // Account state
    val viewModel: HomeViewModel = hiltViewModel()
    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    var isStorageGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isNotificationGranted by remember {
        mutableStateOf(
            notificationPermission == null ||
                    ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        isStorageGranted = result[storagePermission] == true || isStorageGranted
        if (notificationPermission != null) {
            isNotificationGranted = result[notificationPermission] == true || isNotificationGranted
        }
    }

    val shouldShowPermissionHint = !isStorageGranted || !isNotificationGranted
    val hasUpdate = false // disabled for v1.0.0

    var heroVisible by remember { mutableStateOf(false) }
    var bannerVisible by remember { mutableStateOf(false) }
    var quickActionsVisible by remember { mutableStateOf(false) }
    var integrationsVisible by remember { mutableStateOf(false) }
    var categoriesVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        heroVisible = true
        delay(60)
        bannerVisible = true
        delay(60)
        quickActionsVisible = true
        delay(70)
        integrationsVisible = true
        delay(70)
        categoriesVisible = true
    }

    val quickActions = listOf(
        SettingsQuickAction(
            icon = painterResource(R.drawable.palette),
            label = stringResource(R.string.appearance),
            onClick = { navController.navigate("settings/appearance") },
            accentColor = MaterialTheme.colorScheme.primary,
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.play),
            label = stringResource(R.string.player_and_audio),
            onClick = { navController.navigate("settings/player") },
            accentColor = MaterialTheme.colorScheme.tertiary,
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.storage),
            label = stringResource(R.string.storage),
            onClick = { navController.navigate("settings/storage") },
            accentColor = MaterialTheme.colorScheme.secondary,
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.security),
            label = stringResource(R.string.privacy),
            onClick = { navController.navigate("settings/privacy") },
            accentColor = MaterialTheme.colorScheme.error,
        ),
    )

    val integrationActions = listOf(
        SettingsIntegrationAction(
            icon = painterResource(R.drawable.discord),
            label = stringResource(R.string.discord),
            onClick = { navController.navigate("settings/discord") },
            accentColor = Color(0xFF5865F2),
        ),
        SettingsIntegrationAction(
            icon = painterResource(R.drawable.integration),
            label = stringResource(R.string.integration),
            onClick = { navController.navigate("settings/integration") },
            accentColor = MaterialTheme.colorScheme.secondary,
        ),
        SettingsIntegrationAction(
            icon = painterResource(R.drawable.fire),
            label = stringResource(R.string.music_together),
            onClick = { navController.navigate("settings/music_together") },
            accentColor = MaterialTheme.colorScheme.tertiary,
        ),
    )

    val resetSearch: () -> Unit = {
        isSearching = false
        query = TextFieldValue()
        focusManager.clearFocus()
    }

    val settingsCategories = buildList {
        add(
            SettingsCategory(
                title = stringResource(R.string.settings_section_ui),
                items = listOf(
                    PremiumSettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = stringResource(R.string.appearance),
                        subtitle = stringResource(R.string.dark_theme),
                        accentColor = MaterialTheme.colorScheme.primary,
                        keywords = listOf(
                            "theme",
                            "palette",
                            "material you",
                            "dynamic color",
                            "font",
                            "ui",
                        ),
                        onClick = { navController.navigate("settings/appearance") },
                    ),
                ),
            ),
        )

        add(
            SettingsCategory(
                title = stringResource(R.string.settings_section_player_content),
                items = listOf(
                    PremiumSettingsItem(
                        icon = painterResource(R.drawable.play),
                        title = stringResource(R.string.player_and_audio),
                        subtitle = stringResource(R.string.audio_quality),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf(
                            "audio",
                            "playback",
                            "volume",
                            "quality",
                            "equalizer",
                            "crossfade",
                        ),
                        onClick = { navController.navigate("settings/player") },
                    ),
                    PremiumSettingsItem(
                        icon = painterResource(R.drawable.language),
                        title = stringResource(R.string.content),
                        subtitle = stringResource(R.string.content_language),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        keywords = listOf(
                            "language",
                            "content",
                            "lyrics",
                            "translation",
                            "region",
                        ),
                        onClick = { navController.navigate("settings/content") },
                    ),
                    PremiumSettingsItem(
                        icon = painterResource(R.drawable.token),
                        title = stringResource(R.string.po_token_generation),
                        subtitle = stringResource(R.string.po_token_generation_subtitle),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf(
                            "po token",
                            "token",
                            "web client",
                            "visitor data",
                            "gvs",
                            "player",
                        ),
                        onClick = { navController.navigate("settings/po_token") },
                    ),
                ),
            ),
        )

        add(
            SettingsCategory(
                title = stringResource(R.string.settings_section_privacy),
                items = listOf(
                    PremiumSettingsItem(
                        icon = painterResource(R.drawable.security),
                        title = stringResource(R.string.privacy),
                        subtitle = stringResource(R.string.pause_listen_history),
                        accentColor = MaterialTheme.colorScheme.error,
                        keywords = listOf(
                            "privacy",
                            "history",
                            "tracking",
                            "security",
                            "permissions",
                        ),
                        onClick = { navController.navigate("settings/privacy") },
                    ),
                ),
            ),
        )

        add(
            SettingsCategory(
                title = stringResource(R.string.settings_section_storage),
                items = listOf(
                    PremiumSettingsItem(
                        icon = painterResource(R.drawable.storage),
                        title = stringResource(R.string.storage),
                        subtitle = stringResource(R.string.cache),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        keywords = listOf(
                            "storage",
                            "cache",
                            "offline",
                            "downloads",
                            "cleanup",
                        ),
                        onClick = { navController.navigate("settings/storage") },
                    ),
                    PremiumSettingsItem(
                        icon = painterResource(R.drawable.restore),
                        title = stringResource(R.string.backup_restore),
                        subtitle = stringResource(R.string.action_backup),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf(
                            "backup",
                            "restore",
                            "import",
                            "export",
                            "migration",
                        ),
                        onClick = { navController.navigate("settings/backup_restore") },
                    ),
                ),
            ),
        )

        add(
            SettingsCategory(
                title = stringResource(R.string.settings_section_system),
                items = buildList {
                    if (isAndroid12OrLater) {
                        add(
                            PremiumSettingsItem(
                                icon = painterResource(R.drawable.link),
                                title = stringResource(R.string.default_links),
                                subtitle = stringResource(R.string.open_supported_links),
                                accentColor = MaterialTheme.colorScheme.primary,
                                keywords = listOf(
                                    "links",
                                    "deeplink",
                                    "default",
                                    "supported links",
                                ),
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        when (e) {
                                            is ActivityNotFoundException,
                                            is SecurityException,
                                                -> {
                                                Toast.makeText(
                                                    context,
                                                    R.string.open_app_settings_error,
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                            else -> {
                                                Toast.makeText(
                                                    context,
                                                    R.string.open_app_settings_error,
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                        }
                                    }
                                },
                            ),
                        )
                    }
                    add(
                        PremiumSettingsItem(
                            icon = painterResource(R.drawable.experiment),
                            title = stringResource(R.string.experiment_settings),
                            subtitle = stringResource(R.string.misc),
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            keywords = listOf(
                                "experimental",
                                "debug",
                                "developer",
                                "labs",
                                "internal",
                            ),
                            onClick = { navController.navigate("settings/misc") },
                        ),
                    )

                    add(
                        PremiumSettingsItem(
                            icon = painterResource(R.drawable.info),
                            title = stringResource(R.string.about),
                            subtitle = "Velune",
                            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            keywords = listOf(
                                "about",
                                "app info",
                                "license",
                                "contributors",
                            ),
                            onClick = { navController.navigate("settings/about") },
                        ),
                    )
                },
            ),
        )
    }

    val internalSettingsItems = buildList {
        add(
            PremiumSettingsItem(
                icon = painterResource(R.drawable.palette),
                title = stringResource(R.string.theme_creator_title),
                subtitle = stringResource(R.string.theme_creator_subtitle),
                accentColor = MaterialTheme.colorScheme.primary,
                keywords = listOf(
                    "theme",
                    "creator",
                    "seed",
                    "material",
                    "palette",
                    "import",
                    "export",
                ),
                onClick = { navController.navigate("settings/appearance/theme_creator") },
            ),
        )
        add(
            PremiumSettingsItem(
                icon = painterResource(R.drawable.palette),
                title = stringResource(R.string.customize_colors),
                subtitle = stringResource(R.string.appearance),
                accentColor = MaterialTheme.colorScheme.primary,
                keywords = listOf(
                    "palette",
                    "color",
                    "accent",
                    "tone",
                    "dynamic color",
                ),
                onClick = { navController.navigate("settings/appearance/palette_picker") },
            ),
        )
        add(
            PremiumSettingsItem(
                icon = painterResource(R.drawable.image),
                title = stringResource(R.string.customize_background_title),
                subtitle = stringResource(R.string.appearance),
                accentColor = MaterialTheme.colorScheme.secondary,
                keywords = listOf(
                    "background",
                    "wallpaper",
                    "image",
                    "blur",
                    "gradient",
                ),
                onClick = { navController.navigate("customize_background") },
            ),
        )
        add(
            PremiumSettingsItem(
                icon = painterResource(R.drawable.discord),
                title = stringResource(R.string.discord_integration),
                subtitle = stringResource(R.string.integration),
                accentColor = Color(0xFF5865F2),
                keywords = listOf(
                    "discord",
                    "rpc",
                    "rich presence",
                    "status",
                    "activity",
                ),
                onClick = { navController.navigate("settings/discord") },
            ),
        )
        add(
            PremiumSettingsItem(
                icon = painterResource(R.drawable.security),
                title = stringResource(R.string.advanced_login),
                subtitle = stringResource(R.string.discord),
                accentColor = Color(0xFF5865F2),
                keywords = listOf(
                    "token",
                    "login",
                    "authentication",
                    "discord login",
                ),
                onClick = { navController.navigate("settings/discord/login") },
            ),
        )
        add(
            PremiumSettingsItem(
                icon = painterResource(R.drawable.experiment),
                title = stringResource(R.string.experimental_features),
                subtitle = stringResource(R.string.experimental_features_description),
                accentColor = MaterialTheme.colorScheme.tertiary,
                keywords = listOf(
                    "experimental",
                    "labs",
                    "advanced",
                    "discord experimental",
                    "internal",
                ),
                onClick = { navController.navigate("settings/discord/experimental") },
            ),
        )
        add(
            PremiumSettingsItem(
                icon = painterResource(R.drawable.integration),
                title = stringResource(R.string.lastfm_integration),
                subtitle = stringResource(R.string.integration),
                accentColor = MaterialTheme.colorScheme.secondary,
                keywords = listOf(
                    "lastfm",
                    "last.fm",
                    "scrobble",
                    "listening history",
                ),
                onClick = { navController.navigate("settings/lastfm") },
            ),
        )
        add(
            PremiumSettingsItem(
                icon = painterResource(R.drawable.fire),
                title = stringResource(R.string.music_together),
                subtitle = stringResource(R.string.integration),
                accentColor = MaterialTheme.colorScheme.tertiary,
                keywords = listOf(
                    "together",
                    "session",
                    "sync",
                    "party",
                    "join",
                    "host",
                ),
                onClick = { navController.navigate("settings/music_together") },
            ),
        )
    }

    val wrappedQuickActions = quickActions.map { action ->
        val originalOnClick = action.onClick
        action.copy(onClick = { resetSearch(); originalOnClick() })
    }

    val wrappedIntegrations = integrationActions.map { action ->
        val originalOnClick = action.onClick
        action.copy(onClick = { resetSearch(); originalOnClick() })
    }

    val wrappedCategories = settingsCategories.map { category ->
        category.copy(
            items = category.items.map { item ->
                val originalOnClick = item.onClick
                item.copy(onClick = { resetSearch(); originalOnClick() })
            }
        )
    }

    val wrappedInternalSettings = internalSettingsItems.map { item ->
        val originalOnClick = item.onClick
        item.copy(onClick = { resetSearch(); originalOnClick() })
    }

    val queryText = query.text.trim()
    val showSearchBar = isSearching || queryText.isNotBlank()

    val filteredQuickActions = filterQuickActions(wrappedQuickActions, queryText)
    val filteredIntegrations = filterIntegrations(wrappedIntegrations, queryText)
    val filteredCategories = filterSettingsCategories(wrappedCategories, queryText)
    val filteredInternalSettings = filterInternalSettingsItems(wrappedInternalSettings, queryText)

    val hasSearchResults by remember(
        filteredQuickActions,
        filteredCategories,
        filteredIntegrations,
        filteredInternalSettings,
    ) {
        derivedStateOf {
            filteredQuickActions.isNotEmpty() ||
                    filteredCategories.isNotEmpty() ||
                    filteredIntegrations.isNotEmpty() ||
                    filteredInternalSettings.isNotEmpty()
        }
    }

    val internalSettingsCategory = SettingsCategory(
        title = stringResource(R.string.internal_subcategory_settings),
        items = filteredInternalSettings,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (!showSearchBar) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    ),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item(key = "topSpacer") {
                    Spacer(
                        Modifier.windowInsetsPadding(
                            LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
                        )
                    )
                }

                item(key = "hero") {
                    AnimatedVisibility(
                        visible = heroVisible,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                                slideInVertically(
                                    initialOffsetY = { it / 5 },
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessLow,
                                        dampingRatio = 0.85f,
                                    ),
                                ),
                    ) {
                        SettingsHeroHeader(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 4.dp, bottom = 14.dp),
                        )
                    }
                }

                item(key = "account") {
                    AnimatedVisibility(
                        visible = heroVisible,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                                slideInVertically(
                                    initialOffsetY = { it / 5 },
                                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.85f),
                                ),
                    ) {
                        SettingsAccountCard(
                            isLoggedIn = isLoggedIn,
                            accountName = accountName,
                            accountImageUrl = accountImageUrl,
                            onAccountClick = {
                                if (isLoggedIn) navController.navigate("settings/account")
                                else navController.navigate("login")
                            },
                            onLogout = {
                                onInnerTubeCookieChange("")
                                forgetAccount(context)
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 14.dp),
                        )
                    }
                }

                if (queryText.isBlank()) {
                    item(key = "permission") {
                        AnimatedVisibility(
                            visible = bannerVisible && shouldShowPermissionHint,
                            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                                    expandVertically(spring(stiffness = Spring.StiffnessLow)) +
                                    slideInVertically(
                                        initialOffsetY = { -it / 4 },
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessLow,
                                            dampingRatio = 0.85f,
                                        ),
                                    ),
                            exit = fadeOut(tween(300)) + shrinkVertically(tween(300)),
                        ) {
                            PremiumPermissionCard(
                                onRequestPermission = {
                                    val toRequest = buildList {
                                        if (!isStorageGranted) add(storagePermission)
                                        if (!isNotificationGranted && notificationPermission != null) {
                                            add(notificationPermission)
                                        }
                                    }
                                    if (toRequest.isNotEmpty()) {
                                        permissionLauncher.launch(toRequest.toTypedArray())
                                    }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 14.dp),
                            )
                        }
                    }
                }




                if (queryText.isBlank() || filteredIntegrations.isNotEmpty()) {
                    item(key = "integrations") {
                        AnimatedVisibility(
                            visible = integrationsVisible,
                            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                                    slideInVertically(
                                        initialOffsetY = { it / 6 },
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessLow,
                                            dampingRatio = 0.85f,
                                        ),
                                    ),
                        ) {
                            val toShow = if (queryText.isBlank()) {
                                wrappedIntegrations
                            } else {
                                filteredIntegrations
                            }
                            SettingsIntegrationsRow(
                                integrations = toShow,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 12.dp),
                            )
                        }
                    }
                }

                if (queryText.isNotBlank() && !hasSearchResults) {
                    item(key = "empty") {
                        Spacer(modifier = Modifier.height(24.dp))
                        EmptyResultsCard(
                            title = stringResource(R.string.no_results_found),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    val categoriesToShow = if (queryText.isBlank()) {
                        wrappedCategories
                    } else {
                        filteredCategories
                    }

                    if (queryText.isNotBlank() && filteredInternalSettings.isNotEmpty()) {
                        item(key = "internalSearchResults") {
                            PremiumSettingsSection(
                                category = internalSettingsCategory,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 12.dp),
                            )
                        }
                    }

                    items(
                        count = categoriesToShow.size,
                        key = { categoriesToShow[it].title },
                    ) { index ->
                        val category = categoriesToShow[index]
                        AnimatedVisibility(
                            visible = categoriesVisible,
                            enter = fadeIn(tween(420, delayMillis = index * 60)) +
                                    slideInVertically(
                                        initialOffsetY = { it / 5 },
                                        animationSpec = tween(420, delayMillis = index * 60),
                                    ),
                        ) {
                            PremiumSettingsSection(
                                category = category,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 12.dp),
                            )
                        }
                    }
                }
            }
        }

        if (!showSearchBar) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isSearching = true },
                        onLongClick = {},
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ),
            )
        }

        AnimatedVisibility(
            visible = showSearchBar,
            enter = fadeIn(tween(durationMillis = 220)),
            exit = fadeOut(tween(durationMillis = 160)),
        ) {
            TopSearch(
                query = query,
                onQueryChange = { query = it },
                onSearch = { focusManager.clearFocus() },
                active = showSearchBar,
                onActiveChange = { active ->
                    if (active) {
                        isSearching = true
                    } else {
                        resetSearch()
                    }
                },
                placeholder = { Text(text = stringResource(R.string.search)) },
                leadingIcon = {
                    IconButton(
                        onClick = { resetSearch() },
                        onLongClick = {
                            if (queryText.isBlank()) {
                                navController.backToMain()
                            }
                        },
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                trailingIcon = {
                    Row {
                        if (query.text.isNotBlank()) {
                            IconButton(
                                onClick = { query = TextFieldValue() },
                                onLongClick = {},
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                },
                focusRequester = focusRequester,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            LocalPlayerAwareWindowInsets.current.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                            )
                        ),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    if (queryText.isNotBlank() && !hasSearchResults) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            EmptyResultsCard(
                                title = stringResource(R.string.no_results_found),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    } else {
                        if (filteredIntegrations.isNotEmpty()) {
                            item {
                                SettingsIntegrationsRow(
                                    integrations = filteredIntegrations,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 12.dp),
                                )
                            }
                        }

                        if (filteredInternalSettings.isNotEmpty()) {
                            item {
                                PremiumSettingsSection(
                                    category = internalSettingsCategory,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 12.dp),
                                )
                            }
                        }

                        items(filteredCategories.size) { index ->
                            val category = filteredCategories[index]
                            PremiumSettingsSection(
                                category = category,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeroHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.small_icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyResultsCard(
    title: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.search_try_different),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PremiumPermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.security),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.permissions_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.permissions_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onRequestPermission,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.allow),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsQuickActionsGrid(
    title: String,
    actions: List<SettingsQuickAction>,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.star),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            val rows = actions.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { rowActions ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        rowActions.forEach { action ->
                            SettingsQuickActionTile(
                                action = action,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowActions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsQuickActionTile(
    action: SettingsQuickAction,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tileScale",
    )
    val tileAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tileAlpha",
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .graphicsLayer { alpha = tileAlpha }
            .aspectRatio(1.45f),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = action.onClick,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            action.accentColor.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    ),
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            action.accentColor.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(0f, 0f),
                        radius = 500f,
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(action.accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = action.icon,
                        contentDescription = null,
                        tint = action.accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SettingsIntegrationsRow(
    integrations: List<SettingsIntegrationAction>,
    modifier: Modifier = Modifier,
) {
    if (integrations.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.integration),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Text(
                    text = stringResource(R.string.integrations),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    count = integrations.size,
                    key = { integrations[it].label },
                ) { index ->
                    IntegrationChip(action = integrations[index])
                }
            }
        }
    }
}

@Composable
private fun IntegrationChip(
    action: SettingsIntegrationAction,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "chipScale",
    )

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = action.onClick,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(action.accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = action.icon,
                    contentDescription = null,
                    tint = action.accentColor,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PremiumSettingsSection(
    category: SettingsCategory,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = category.title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                category.items.forEachIndexed { index, item ->
                    PremiumSettingsItemRow(
                        item = item,
                        showDivider = index < category.items.size - 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumSettingsItemRow(
    item: PremiumSettingsItem,
    showDivider: Boolean,
) {
    val effectiveAccent = if (item.accentColor.isSpecified) {
        item.accentColor
    } else {
        MaterialTheme.colorScheme.primary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.75f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "rowAlpha",
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = item.onClick,
                )
                .graphicsLayer { this.alpha = alpha }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (item.showUpdateIndicator) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                    effectiveAccent.copy(alpha = 0.16f),
                                ),
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    effectiveAccent.copy(alpha = 0.14f),
                                    effectiveAccent.copy(alpha = 0.08f),
                                ),
                            )
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (item.showUpdateIndicator) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(10.dp),
                            )
                        },
                    ) {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            tint = effectiveAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                } else {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = effectiveAccent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                item.subtitle?.let { subtitle ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.showUpdateIndicator) {
                            effectiveAccent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item.badge?.let { badge ->
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(text = badge) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = null,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp),
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp, end = 18.dp),
                thickness = 0.4.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
private fun SettingsAccountCard(
    isLoggedIn: Boolean,
    accountName: String,
    accountImageUrl: String?,
    onAccountClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColor by animateColorAsState(
        targetValue = if (isLoggedIn)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "accountCardColor",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onAccountClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLoggedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoggedIn && accountImageUrl != null) {
                    AsyncImage(
                        model = accountImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(CircleShape),
                    )
                } else {
                    Icon(
                        painter = painterResource(if (isLoggedIn) R.drawable.account else R.drawable.login),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isLoggedIn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) accountName else stringResource(R.string.login),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isLoggedIn) stringResource(R.string.account)
                    else stringResource(R.string.not_logged_in),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isLoggedIn) {
                FilledTonalButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.action_logout),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            } else {
                Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}