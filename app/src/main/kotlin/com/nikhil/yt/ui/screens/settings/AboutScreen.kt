/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.nikhil.yt.BuildConfig
import com.nikhil.yt.LocalPlayerAwareWindowInsets
import com.nikhil.yt.R
import com.nikhil.yt.ui.component.IconButton
import com.nikhil.yt.ui.utils.backToMain
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
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
                scrollBehavior = scrollBehavior,
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
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Title
                    Text(
                        text = "VELUNE",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )

                    Spacer(Modifier.height(16.dp))

                    // Version Badge
                    Row(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} • ${if (BuildConfig.DEBUG) "DEBUG" else "STABLE"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // --- DEVELOPER SECTION ---
            item {
                SectionTitle("DEVELOPER")
                Spacer(Modifier.height(8.dp))
                AboutItemCard(
                    iconUrl = "https://github.com/nikhilvishwakarma00.png",
                    title = "Nikhil",
                    subtitle = "App Developer",
                    onClick = { uriHandler.openUri("https://github.com/nikhilvishwakarma00") }
                )
                Spacer(Modifier.height(24.dp))
            }

            // --- INSPIRATION SECTION ---
            item {
                SectionTitle("INSPIRATION")
                Spacer(Modifier.height(8.dp))
                AboutItemCard(
                    iconUrl = "https://avatars.githubusercontent.com/u/107134739?v=4", 
                    title = "Archivetune   -by koiverse",
                    subtitle = "Base Framework",
                    onClick = { uriHandler.openUri("https://github.com/koiverse/ArchiveTune") }
                )
                Spacer(Modifier.height(8.dp))
                AboutItemCard(
                    iconUrl = "https://avatars.githubusercontent.com/u/80542861?v=4",
                    title = "MO AGAMY",
                    subtitle = "Metrolist Dev",
                    onClick = { uriHandler.openUri("https://github.com/mostafaalagamy") }
                )
                Spacer(Modifier.height(24.dp))
            }

            // --- COMMUNITY SECTION ---
            item {
                SectionTitle("COMMUNITY")
                Spacer(Modifier.height(8.dp))
                AboutItemCard(
                    iconRes = R.drawable.github,
                    title = "GitHub Repository",
                    subtitle = "View source code",
                    onClick = { uriHandler.openUri("https://github.com/nikhilvishwakarma00/Velune") }
                )
                Spacer(Modifier.height(24.dp))
            }

            // --- APP INFO SECTION ---
            item {
                SectionTitle("APP INFO")
                Spacer(Modifier.height(8.dp))
                val installDate = try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(packageInfo.firstInstallTime))
                } catch (e: Exception) {
                    "Unknown"
                }

                AboutItemCard(
                    iconRes = R.drawable.storage,
                    title = "Installed Date",
                    subtitle = installDate,
                    onClick = null
                )
                Spacer(Modifier.height(8.dp))
                AboutItemCard(
                    iconRes = R.drawable.info, 
                    title = "Version code",
                    subtitle = "${BuildConfig.VERSION_CODE}",
                    onClick = null
                )
                Spacer(Modifier.height(8.dp))
                AboutItemCard(
                    iconRes = R.drawable.security, 
                    title = "GNU General Public License v3.0",
                    subtitle = "GPL-3.0 • Free Open Source Software",
                    onClick = { uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html") }
                )
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFB0956E),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun AboutItemCard(
    iconUrl: String? = null,
    iconRes: Int? = null,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    val modifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        } else if (iconRes != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}