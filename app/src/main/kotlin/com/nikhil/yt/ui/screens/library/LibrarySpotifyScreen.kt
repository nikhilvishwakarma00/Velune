package com.nikhil.yt.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nikhil.yt.R
import com.nikhil.yt.LocalPlayerAwareWindowInsets
import androidx.compose.ui.platform.LocalConfiguration
import com.nikhil.yt.ui.component.SpotifyImportDialog

data class SpotifyRecommendedPlaylist(
    val name: String,
    val description: String,
    val url: String
)

val recommendedPlaylists = listOf(
    SpotifyRecommendedPlaylist("Top 50 - Mondo", "La classifica giornaliera dei brani più ascoltati a livello globale.", "https://open.spotify.com/playlist/37i9dQZEVXbMDoHDwVN2tF"),
    SpotifyRecommendedPlaylist("Top 50 - Italia", "I 50 brani più ascoltati in Italia ogni giorno.", "https://open.spotify.com/playlist/37i9dQZEVXbIQnj7RRhdSX"),
    SpotifyRecommendedPlaylist("Top 50 - Spagna", "I brani del momento in Spagna, aggiornati quotidianamente.", "https://open.spotify.com/playlist/37i9dQZEVXbNFJfNWhZi8W"),
    SpotifyRecommendedPlaylist("Top 50 - Brasile", "Le hit più trasmesse e ascoltate in Brasile.", "https://open.spotify.com/playlist/37i9dQZEVXbMXbN3EUUhlg"),
    SpotifyRecommendedPlaylist("Top 50 - Germania", "I brani più ascoltati in Germania, aggiornati quotidianamente.", "https://open.spotify.com/playlist/37i9dQZEVXbJiZcmfTiPZ2"),
    SpotifyRecommendedPlaylist("Top 50 - Argentina", "Le migliori canzoni del momento in Argentina.", "https://open.spotify.com/playlist/37i9dQZEVXbMMy2roB9myp"),
    SpotifyRecommendedPlaylist("Today's Top Hits", "Il punto di riferimento globale per la musica pop e le hit del momento.", "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"),
    SpotifyRecommendedPlaylist("Viva Latino", "Il meglio del panorama reggaeton, pop latino e urban.", "https://open.spotify.com/playlist/37i9dQZF1DX10zKzsJ2jva"),
    SpotifyRecommendedPlaylist("Mega Hit Mix", "Un mix dinamico di successi mondiali, pop ed elettronici.", "https://open.spotify.com/playlist/37i9dQZF1DXbYM3nMM0oPk"),
    SpotifyRecommendedPlaylist("RapCaviar", "Il tempio dell'hip-hop internazionale con tutte le nuove uscite.", "https://open.spotify.com/playlist/37i9dQZF1DX0XUsuxWHRQd"),
    SpotifyRecommendedPlaylist("Rock Classics", "I grandi capolavori immortali della storia della musica rock.", "https://open.spotify.com/playlist/37i9dQZF1DWXRqbbSj53Jp"),
    SpotifyRecommendedPlaylist("Top 50 - UK", "I brani più ascoltati nel Regno Unito ogni giorno.", "https://open.spotify.com/playlist/37i9dQZEVXbLnolsZ8PSNw"),
    SpotifyRecommendedPlaylist("Lofi Beats", "Ritmi lofi e chill per rilassarsi, studiare o concentrarsi.", "https://open.spotify.com/playlist/37i9dQZF1DWWQRwui0ExPn")
)

@Composable
fun LibrarySpotifyScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedPlaylistUrl by remember { mutableStateOf("") }
    val playerAwarePadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) GridCells.Fixed(2) else GridCells.Fixed(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = playerAwarePadding.calculateTopPadding())
    ) {
        filterContent()
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Playlist Spotify Consigliate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Button(
                onClick = {
                    selectedPlaylistUrl = ""
                    showImportDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1DB954),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Aggiungi",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        LazyVerticalGrid(
            columns = columns,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = playerAwarePadding.calculateBottomPadding() + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(recommendedPlaylists) { playlist ->
                SpotifyCard(
                    playlist = playlist,
                    onClick = {
                        selectedPlaylistUrl = playlist.url
                        showImportDialog = true
                    }
                )
            }
        }
    }

    if (showImportDialog) {
        SpotifyImportDialog(
            isVisible = showImportDialog,
            initialUrl = selectedPlaylistUrl,
            navController = navController,
            onDismiss = { showImportDialog = false }
        )
    }
}

@Composable
private fun SpotifyCard(
    playlist: SpotifyRecommendedPlaylist,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1DB954).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_spotify),
                        contentDescription = "Spotify logo",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = playlist.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
