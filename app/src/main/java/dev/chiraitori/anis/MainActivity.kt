package dev.chiraitori.anis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chiraitori.anis.ui.MainApp
import dev.chiraitori.anis.ui.MainViewModel
import dev.chiraitori.anis.ui.theme.AnisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = application as AnisApplication
        val savedLang = app.settingsRepository.appLanguageFlow.value
        dev.chiraitori.anis.ui.i18n.I18n.applyLocale(this, savedLang)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()

            AnisTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainApp(viewModel = viewModel)
                }
            }
        }
    }
}