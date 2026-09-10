package com.vlink.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vlink.app.core.vpn.V2RayVpnService
import com.vlink.app.data.db.AppDatabase
import com.vlink.app.data.model.ConfigProfile
import com.vlink.app.data.model.Subscription
import com.vlink.app.data.repository.VLinkRepository
import com.vlink.app.ui.screens.AddConfigScreen
import com.vlink.app.ui.screens.HomeScreen
import com.vlink.app.ui.screens.SubscriptionScreen
import com.vlink.app.ui.theme.VLinkTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = VLinkRepository(AppDatabase.get(applicationContext))

        setContent {
            VLinkTheme {
                val vm: VLinkViewModel = viewModel(factory = VLinkViewModel.factory(repository))
                val navController = rememberNavController()
                val configs by vm.configs.collectAsState()
                val subs by vm.subscriptions.collectAsState()

                // Android requires an explicit user-approval dialog before a
                // VpnService can start (VpnService.prepare()). This launcher
                // shows that system dialog; onActivityResult tells us if the
                // user granted it, and only then do we actually start the service.
                val vpnPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        vm.selectedConfigId?.let { startVpnService(it) }
                        vm.markConnected(true)
                    }
                }

                fun requestConnect() {
                    val prepareIntent = VpnService.prepare(this)
                    if (prepareIntent != null) {
                        vpnPermissionLauncher.launch(prepareIntent)
                    } else {
                        // Already granted previously.
                        vm.selectedConfigId?.let { startVpnService(it) }
                        vm.markConnected(true)
                    }
                }

                fun disconnect() {
                    stopVpnService()
                    vm.markConnected(false)
                }

                NavHost(navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            configs = configs,
                            selectedConfigId = vm.selectedConfigId,
                            isConnected = vm.isConnected,
                            onSelectConfig = { vm.select(it) },
                            onToggleConnect = {
                                if (vm.isConnected) disconnect() else requestConnect()
                            },
                            onPingOne = { vm.pingOne(it) },
                            onPingAll = { vm.pingAll() },
                            onAddManualConfig = { navController.navigate("add") },
                            onOpenSubscriptions = { navController.navigate("subs") },
                        )
                    }
                    composable("subs") {
                        SubscriptionScreen(
                            subscriptions = subs,
                            onAdd = { n, u -> vm.addSubscription(n, u) },
                            onRefresh = { vm.refreshSubscription(it) },
                            onRemove = { vm.removeSubscription(it) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("add") {
                        AddConfigScreen(
                            onLinkReady = { link ->
                                vm.addManualConfig(link)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    private fun startVpnService(configId: String) {
        val intent = Intent(this, V2RayVpnService::class.java)
            .putExtra(V2RayVpnService.EXTRA_CONFIG_ID, configId)
        startService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, V2RayVpnService::class.java).setAction(V2RayVpnService.ACTION_STOP)
        startService(intent)
    }
}

class VLinkViewModel(private val repository: VLinkRepository) : ViewModel() {

    val configs: StateFlow<List<ConfigProfile>> = MutableStateFlow(emptyList())
    val subscriptions: StateFlow<List<Subscription>> = MutableStateFlow(emptyList())

    var selectedConfigId by mutableStateOf<String?>(null)
        private set
    var isConnected by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            repository.observeConfigs().collect { (configs as MutableStateFlow).value = it }
        }
        viewModelScope.launch {
            repository.observeSubscriptions().collect { (subscriptions as MutableStateFlow).value = it }
        }
    }

    fun select(config: ConfigProfile) { selectedConfigId = config.id }

    /** Called by the Activity once the VpnService has actually been
     *  started/stopped, so UI state reflects reality rather than intent. */
    fun markConnected(value: Boolean) { isConnected = value }

    fun pingOne(config: ConfigProfile) = viewModelScope.launch { repository.pingConfig(config) }

    fun pingAll() = viewModelScope.launch { repository.pingAll(configs.value) }

    fun addManualConfig(link: String) = viewModelScope.launch { repository.addManualConfig(link) }

    fun addSubscription(name: String, url: String) = viewModelScope.launch {
        repository.addSubscription(name, url)
    }

    fun refreshSubscription(sub: Subscription) = viewModelScope.launch {
        repository.refreshSubscription(sub)
    }

    fun removeSubscription(sub: Subscription) = viewModelScope.launch {
        repository.removeSubscription(sub)
    }

    companion object {
        fun factory(repository: VLinkRepository) = viewModelFactory {
            initializer { VLinkViewModel(repository) }
        }
    }
}
