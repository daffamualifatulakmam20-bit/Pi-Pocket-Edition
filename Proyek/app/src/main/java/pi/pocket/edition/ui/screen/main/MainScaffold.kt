package pi.pocket.edition.ui.screen.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import pi.pocket.edition.data.PrefsManager
import pi.pocket.edition.ui.screen.network.NetworkScreen

enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    ADMIN("Admin", Icons.Filled.Language, Icons.Outlined.Language),
    NETWORK("Network", Icons.Filled.CellTower, Icons.Outlined.CellTower)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    prefsManager: PrefsManager,
    onNavigateToSettings: () -> Unit,
    onNavigateToPassword: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pi-Pocket Edition",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            MainTab.HOME -> DashboardScreen(
                modifier = Modifier.padding(padding),
                prefsManager = prefsManager
            )
            MainTab.ADMIN -> PiholeAdminScreen(
                modifier = Modifier.padding(padding)
            )
            MainTab.NETWORK -> NetworkScreen(
                modifier = Modifier.padding(padding),
                prefsManager = prefsManager
            )
        }
    }
}
