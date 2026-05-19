package pi.pocket.edition.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pi.pocket.edition.data.PrefsManager
import pi.pocket.edition.ui.screen.main.MainScaffold
import pi.pocket.edition.ui.screen.settings.PasswordScreen
import pi.pocket.edition.ui.screen.settings.SettingsScreen
import pi.pocket.edition.ui.screen.setup.ProgressScreen
import pi.pocket.edition.ui.screen.setup.SetupScreen
import pi.pocket.edition.ui.screen.setup.ThemeSelectionScreen

object Routes {
    const val THEME_SELECTION = "theme_selection"
    const val SETUP = "setup"
    const val PROGRESS = "progress"
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val PASSWORD = "password"
}

@Composable
fun NavGraph(
    isSetupComplete: Boolean,
    prefsManager: PrefsManager
) {
    val navController = rememberNavController()
    val isThemeSelected by prefsManager.isThemeSelected.collectAsState(initial = false)

    val startDestination = when {
        isSetupComplete -> Routes.MAIN
        isThemeSelected -> Routes.SETUP
        else -> Routes.THEME_SELECTION
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.THEME_SELECTION) {
            ThemeSelectionScreen(
                prefsManager = prefsManager,
                onContinue = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(Routes.THEME_SELECTION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SETUP) {
            SetupScreen(
                prefsManager = prefsManager,
                onStartSetup = { url, piholePass, sshPass ->
                    navController.navigate("${Routes.PROGRESS}/$url/$piholePass/$sshPass") {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable("${Routes.PROGRESS}/{url}/{piholePass}/{sshPass}") { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val piholePass = backStackEntry.arguments?.getString("piholePass") ?: ""
            val sshPass = backStackEntry.arguments?.getString("sshPass") ?: ""
            ProgressScreen(
                downloadUrl = url,
                piholePassword = piholePass,
                sshPassword = sshPass,
                prefsManager = prefsManager,
                onComplete = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScaffold(
                prefsManager = prefsManager,
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToPassword = {
                    navController.navigate(Routes.PASSWORD)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                prefsManager = prefsManager,
                onBack = { navController.popBackStack() },
                onNavigateToPassword = {
                    navController.navigate(Routes.PASSWORD)
                }
            )
        }

        composable(Routes.PASSWORD) {
            PasswordScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
