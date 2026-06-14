/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.ui.menu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nikhil.yt.R
import com.nikhil.yt.db.entities.Playlist
import com.nikhil.yt.services.ExportPlaylistService
import com.nikhil.yt.ui.component.DefaultDialog

/**
 * A dialog that displays an encoded export string for the given [playlist].
 * The user can copy it to clipboard or share it via the Android share sheet.
 *
 * Call-site example (inside PlaylistMenu):
 * ```
 * if (showExportDialog) {
 *     ExportPlaylistDialog(
 *         playlist = playlist,
 *         songIds = songs.map { it.id },
 *         onDismiss = { showExportDialog = false },
 *     )
 * }
 * ```
 */
@Composable
fun ExportPlaylistDialog(
    playlist: Playlist,
    songIds: List<String>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val encoded = remember(playlist.id, songIds) {
        ExportPlaylistService.encode(playlist, songIds)
    }

    DefaultDialog(
        onDismiss = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.share),
                contentDescription = null,
            )
        },
        title = { Text(text = stringResource(R.string.export_playlist)) },
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.export_playlist_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (encoded != null) {
                    Text(
                        text = encoded,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        maxLines = 4,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.export_playlist_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }

            if (encoded != null) {
                TextButton(
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(playlist.playlist.name, encoded)
                        )
                        Toast.makeText(
                            context,
                            context.getString(R.string.copied),
                            Toast.LENGTH_SHORT,
                        ).show()
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(R.string.copy))
                }

                TextButton(
                    onClick = {
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, encoded)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(R.string.share))
                }
            }
        },
    )
}
