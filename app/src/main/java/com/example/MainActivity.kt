package com.example

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AboutScreen
import com.example.ui.ApiKeyGuideScreen
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.LocalScreen
import com.example.ui.LogScreen
import com.example.ui.PrivacyScreen
import com.example.ui.RightDrawerMenu
import com.example.ui.SettingsScreen
import com.example.ui.VideoScreen
import com.example.ui.persona.PersonaScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppShortcuts

class MainActivity : ComponentActivity() {

    private var pendingLiveMode = false
    private var pendingOpenSettings = false

    private val pipState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppShortcuts.install(applicationContext)
        handleIntent(intent)

        setContent {
            val inPip by pipState
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(
                        inPip = inPip,
                        startLiveMode = pendingLiveMode,
                        startSettings = pendingOpenSettings,
                        consumeFlags = {
                            pendingLiveMode = false
                            pendingOpenSettings = false
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        recreate()
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra("start_live_mode", false)) pendingLiveMode = true
        if (intent.getBooleanExtra("open_settings", false)) pendingOpenSettings = true
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        AppState.isInPip = isInPictureInPictureMode
        pipState.value = isInPictureInPictureMode
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        AppState.isInPip = isInPictureInPictureMode
        pipState.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (AppState.isVideoFullscreen && AppState.hasActiveVideo) {
            PipHelper.enter(this, 16, 9)
        } else if (AppState.isChatActive) {
            PipHelper.enter(this, 3, 4)
        }
    }
}

@Composable
private fun MainContent(
    inPip: Boolean = false,
    startLiveMode: Boolean = false,
    startSettings: Boolean = false,
    consumeFlags: () -> Unit = {}
) {
    val viewModel: ChatViewModel = viewModel()

    var currentTab by rememberSaveable {
        mutableStateOf(if (startSettings) "settings" else "chat")
    }
    var showLog by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showApiKeyGuide by rememberSaveable { mutableStateOf(false) }
    var showRightMenu by remember { mutableStateOf(false) }
    var liveRequest by remember { mutableStateOf(startLiveMode) }

    LaunchedEffect(Unit) { consumeFlags() }

    LaunchedEffect(currentTab, showLog, showAbout, showApiKeyGuide, inPip) {
        AppState.isChatActive = currentTab == "chat" && !showLog && !showAbout &&
            !showApiKeyGuide && !inPip
    }

    LaunchedEffect(inPip) {
        if (inPip) showRightMenu = false
    }

    fun navigate(route: String) {
        when (route) {
            "log" -> {
                showLog = true; showAbout = false; showApiKeyGuide = false
            }
            "about" -> {
                showAbout = true; showLog = false; showApiKeyGuide = false
            }
            "apikey_guide" -> {
                showApiKeyGuide = true; showLog = false; showAbout = false
            }
            else -> {
                showLog = false; showAbout = false; showApiKeyGuide = false
                currentTab = route
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            showLog && !inPip -> LogScreen(
                currentRoute = "log",
                onNavigate = { navigate(it) },
                onNavigateBack = { showLog = false }
            )
            showAbout && !inPip -> AboutScreen(
                currentRoute = "about",
                onNavigate = { navigate(it) },
                onBack = { showAbout = false },
                onOpenApiKeyGuide = { navigate("apikey_guide") }
            )
            showApiKeyGuide && !inPip -> ApiKeyGuideScreen(
                currentRoute = "apikey_guide",
                onNavigate = { navigate(it) },
                onBack = { showApiKeyGuide = false }
            )
            else -> {
                when (currentTab) {
                    "chat" -> ChatScreen(
                        viewModel = viewModel,
                        currentRoute = "chat",
                        onNavigate = { navigate(it) },
                        onOpenMenu = { showRightMenu = true },
                        requestLiveMode = liveRequest,
                        onLiveModeConsumed = { liveRequest = false },
                        forcePipLayout = inPip
                    )
                    "video" -> VideoScreen(viewModel, "video", { navigate(it) })
                    "local" -> LocalScreen(viewModel, "local", { navigate(it) })
                    "privacy" -> PrivacyScreen(
                        currentRoute = "privacy",
                        onNavigate = { navigate(it) }
                    )
                    "settings" -> SettingsScreen(viewModel, "settings", { navigate(it) })
                    "persona" -> PersonaScreen(
                        onBack = { navigate("chat") },
                        onPersonaSelected = { p ->
                            viewModel.settings.systemPrompt = p.systemPrompt
                            viewModel.settings.activePersonaId = p.id
                            viewModel.settings.ttsVoiceName = p.voiceName
                            viewModel.settings.ttsRate = p.ttsRate
                            viewModel.settings.ttsPitch = p.ttsPitch
                        }
                    )
                }
            }
        }

        if (!inPip) {
            RightDrawerMenu(
                isOpen = showRightMenu,
                onClose = { showRightMenu = false },
                onItemClick = { item ->
                    when (item.id) {
                        "persona" -> navigate("persona")
                        "log" -> navigate("log")
                        "settings" -> navigate("settings")
                        "about" -> navigate("about")
                        "share" -> { /* TODO */ }
                    }
                }
            )
        }
    }
}
