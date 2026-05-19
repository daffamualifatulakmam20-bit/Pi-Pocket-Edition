package pi.pocket.edition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import pi.pocket.edition.data.PrefsManager
import pi.pocket.edition.ui.navigation.NavGraph
import pi.pocket.edition.ui.theme.PiPocketTheme

class MainActivity : ComponentActivity() {

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefsManager = PrefsManager(this)

        setContent {
            val isDarkTheme by prefsManager.isDarkTheme.collectAsState(initial = true)
            val isSetupComplete by prefsManager.isSetupComplete.collectAsState(initial = false)

            PiPocketTheme(darkTheme = isDarkTheme) {
                NavGraph(
                    isSetupComplete = isSetupComplete,
                    prefsManager = prefsManager
                )
            }
        }
    }
}
