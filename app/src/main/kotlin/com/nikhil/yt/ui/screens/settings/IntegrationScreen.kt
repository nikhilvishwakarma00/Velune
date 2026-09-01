/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.nikhil.yt.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.nikhil.yt.LocalPlayerAwareWindowInsets
import com.nikhil.yt.R
import com.nikhil.yt.ui.component.PreferenceGroupTitle
import com.nikhil.yt.constants.ListenBrainzEnabledKey
import com.nikhil.yt.constants.ListenBrainzTokenKey
import com.nikhil.yt.ui.component.EditTextPreference
import com.nikhil.yt.ui.component.IconButton
import com.nikhil.yt.ui.component.InfoLabel
import com.nikhil.yt.ui.component.PreferenceEntry
import com.nikhil.yt.ui.component.SwitchPreference
import com.nikhil.yt.ui.component.TextFieldDialog
import com.nikhil.yt.ui.utils.backToMain
import com.nikhil.yt.utils.rememberPreference
import com.nikhil.yt.constants.DeezerArlKey
import com.nikhil.yt.constants.DeezerQualityKey
import com.nikhil.yt.constants.EnableDeezerKey
import com.nikhil.yt.ui.component.ListPreference
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.nikhil.yt.deezer.Deezer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (listenBrainzEnabled, onListenBrainzEnabledChange) = rememberPreference(ListenBrainzEnabledKey, false)
    val (listenBrainzToken, onListenBrainzTokenChange) = rememberPreference(ListenBrainzTokenKey, "")
    val (enableDeezer, onEnableDeezerChange) = rememberPreference(EnableDeezerKey, false)
    val (deezerArl, onDeezerArlChange) = rememberPreference(DeezerArlKey, "")
    val (deezerQuality, onDeezerQualityChange) = rememberPreference(DeezerQualityKey, "MP3_128")

    var showListenBrainzTokenEditor = remember { mutableStateOf(false) }
    val showImportSpotifyDialog = remember { mutableStateOf(false) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        PreferenceGroupTitle(
            title = "Integrazione Discord",
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.discord_integration)) },
            icon = { Icon(painterResource(R.drawable.discord), null) },
            onClick = {
                navController.navigate("settings/discord")
            },
        )

        PreferenceGroupTitle(
            title = "Deezer",
        )
        SwitchPreference(
            title = { Text("Deezer downloads") },
            description = "Download current song from Deezer at 128kbps (requires USA IP)",
            icon = { Icon(painterResource(R.drawable.download), null) },
            checked = enableDeezer,
            onCheckedChange = onEnableDeezerChange,
        )
        if (enableDeezer) {
            EditTextPreference(
                title = { Text("Deezer ARL token") },
                value = deezerArl,
                onValueChange = onDeezerArlChange,
                singleLine = false,
                isInputValid = { it.isNotEmpty() },
                icon = { Icon(painterResource(R.drawable.token), null) },
            )
            ListPreference(
                title = { Text("Deezer download quality") },
                icon = { Icon(painterResource(R.drawable.tune), null) },
                selectedValue = deezerQuality,
                values = listOf("MP3_128", "MP3_320", "FLAC"),
                valueText = { it },
                onValueSelected = onDeezerQualityChange,
            )
            InfoLabel(text = "Get ARL: login to deezer.com from USA IP → F12 → Application → Cookies → arl → copy value")

            val coroutineScope = rememberCoroutineScope()

            FilledTonalButton(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        Deezer.setLogDir(context.cacheDir)
                        Deezer.setArl(deezerArl)
                        val result = Deezer.login()
                        val msg = result.fold(
                            onSuccess = { "Deezer ARL: valid ✓" },
                            onFailure = { "Deezer ARL: invalid - ${it.message}" }
                        )
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Test Deezer connection")
            }

            FilledTonalButton(
                onClick = {
                    val tmpDir = context.cacheDir.resolve("deezer_tmp")
                    if (tmpDir.exists()) {
                        tmpDir.deleteRecursively()
                        Toast.makeText(context, "Deezer cache cleared", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No Deezer cache to clear", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Clear Deezer download cache")
            }
        }

        PreferenceGroupTitle(
            title = "Spotify",
        )
        PreferenceEntry(
            title = { Text("Spotify Importer") },
            icon = { Icon(painterResource(R.drawable.music_note), null) },
            onClick = { showImportSpotifyDialog.value = true }
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.scrobbling),
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.lastfm_integration)) },
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = {
                navController.navigate("settings/lastfm")
            },
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.listenbrainz_scrobbling)) },
            description = stringResource(R.string.listenbrainz_scrobbling_description),
            icon = { Icon(painterResource(R.drawable.token), null) },
            checked = listenBrainzEnabled,
            onCheckedChange = onListenBrainzEnabledChange,
        )
        PreferenceEntry(
            title = { Text(if (listenBrainzToken.isBlank()) stringResource(R.string.set_listenbrainz_token) else stringResource(R.string.edit_listenbrainz_token)) },
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = { showListenBrainzTokenEditor.value = true },
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )

    if (showListenBrainzTokenEditor.value) {
        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(listenBrainzToken),
            onDone = { data ->
                onListenBrainzTokenChange(data)
                showListenBrainzTokenEditor.value = false
            },
            onDismiss = { showListenBrainzTokenEditor.value = false },
            singleLine = true,
            maxLines = 1,
            isInputValid = {
                it.isNotEmpty()
            },
            extraContent = {
                InfoLabel(text = stringResource(R.string.listenbrainz_scrobbling_description))
            }
        )
    }

    if (showImportSpotifyDialog.value) {
        com.nikhil.yt.ui.component.SpotifyImportDialog(
            isVisible = showImportSpotifyDialog.value,
            navController = navController,
            onDismiss = { showImportSpotifyDialog.value = false }
        )
    }
}
