package org.awgadmin.ui

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mui.material.Box
import mui.material.CircularProgress
import mui.material.Container
import mui.material.CssBaseline
import mui.material.Typography
import mui.material.styles.ThemeProvider
import mui.system.sx
import org.awgadmin.api.ApiClient
import org.awgadmin.api.ClientDto
import org.awgadmin.components.AddClientDialog
import org.awgadmin.components.ChangePasswordDialog
import org.awgadmin.components.ClientCard
import org.awgadmin.components.ClientConfigDialog
import org.awgadmin.components.Header
import org.awgadmin.components.LoginPage
import react.FC
import react.Props
import react.useEffectOnce
import react.useState
import web.cssom.AlignItems
import web.cssom.Display
import web.cssom.FlexWrap
import web.cssom.JustifyContent
import web.cssom.pct
import web.cssom.px
import web.cssom.vh

private val scope = MainScope()

/**
 * Authentication state.
 */
sealed class AuthState {
    data object Loading : AuthState()
    data object NotAuthenticated : AuthState()
    data class Authenticated(val username: String) : AuthState()
}

val App = FC<Props> {
    var authState by useState<AuthState>(AuthState.Loading)
    var clients by useState<List<ClientDto>>(emptyList())
    var loading by useState(true)
    var error by useState<String?>(null)
    var showAddDialog by useState(false)
    var showChangePasswordDialog by useState(false)
    var configDialogClient by useState<ClientDto?>(null)

    fun loadClients() {
        scope.launch {
            try {
                clients = ApiClient.getClients()
                loading = false
                error = null
            } catch (e: Exception) {
                // Check if it's an auth error
                if (e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true) {
                    authState = AuthState.NotAuthenticated
                } else {
                    error = e.message
                }
                loading = false
            }
        }
    }

    fun checkAuth() {
        scope.launch {
            val response = ApiClient.checkAuth()
            authState = if (response?.success == true && response.username != null) {
                AuthState.Authenticated(response.username)
            } else {
                AuthState.NotAuthenticated
            }
        }
    }

    fun handleLogout() {
        scope.launch {
            try {
                ApiClient.logout()
            } catch (e: Exception) {
                // Ignore logout errors
            }
            authState = AuthState.NotAuthenticated
            clients = emptyList()
        }
    }

    useEffectOnce {
        checkAuth()
    }

    // Load clients when authenticated
    react.useEffect(authState) {
        if (authState is AuthState.Authenticated) {
            loading = true
            loadClients()
        }
    }

    ThemeProvider {
        theme = appTheme

        CssBaseline {}

        when (val state = authState) {
            is AuthState.Loading -> {
                // Loading spinner while checking auth
                Box {
                    sx {
                        minHeight = 100.vh
                        display = Display.flex
                        alignItems = AlignItems.center
                        justifyContent = JustifyContent.center
                        background = "linear-gradient(135deg, #0D1117 0%, #161B22 50%, #1F2937 100%)".asDynamic()
                    }
                    CircularProgress {}
                }
            }

            is AuthState.NotAuthenticated -> {
                LoginPage {
                    onLoginSuccess = { username ->
                        authState = AuthState.Authenticated(username)
                    }
                }
            }

            is AuthState.Authenticated -> {
                Box {
                    sx {
                        minHeight = 100.pct
                        background = "linear-gradient(135deg, #0D1117 0%, #161B22 50%, #1F2937 100%)".asDynamic()
                    }

                    Header {
                        username = state.username
                        onAddClick = { showAddDialog = true }
                        onRefreshClick = { loadClients() }
                        onChangePasswordClick = { showChangePasswordDialog = true }
                        onLogoutClick = { handleLogout() }
                    }

                    Container {
                        maxWidth = "lg"
                        sx {
                            paddingTop = 32.px
                            paddingBottom = 32.px
                        }

                        if (loading) {
                            Box {
                                sx {
                                    display = Display.flex
                                    justifyContent = JustifyContent.center
                                    padding = 48.px
                                }
                                CircularProgress {}
                            }
                        }

                        error?.let { err ->
                            Typography {
                                sx { color = "error.main".asDynamic() }
                                +"Error: $err"
                            }
                        }

                        if (!loading) {
                            Box {
                                sx {
                                    display = Display.flex
                                    flexWrap = FlexWrap.wrap
                                    gap = 16.px
                                }

                                clients.forEach { client ->
                                    ClientCard {
                                        this.client = client
                                        onToggle = {
                                            scope.launch {
                                                ApiClient.toggleClient(client.id)
                                                loadClients()
                                            }
                                        }
                                        onDelete = {
                                            scope.launch {
                                                ApiClient.deleteClient(client.id)
                                                loadClients()
                                            }
                                        }
                                        onShowConfig = {
                                            configDialogClient = client
                                        }
                                    }
                                }

                                if (clients.isEmpty()) {
                                    Box {
                                        sx {
                                            width = 100.pct
                                            padding = 48.px
                                        }
                                        Typography {
                                            sx {
                                                textAlign = "center".asDynamic()
                                                color = "text.secondary".asDynamic()
                                            }
                                            +"No clients yet. Click + to add one."
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAddDialog) {
                    AddClientDialog {
                        onClose = { showAddDialog = false }
                        onCreated = {
                            showAddDialog = false
                            loadClients()
                        }
                    }
                }

                if (showChangePasswordDialog) {
                    ChangePasswordDialog {
                        onClose = { showChangePasswordDialog = false }
                        onSuccess = { showChangePasswordDialog = false }
                    }
                }

                configDialogClient?.let { client ->
                    ClientConfigDialog {
                        this.client = client
                        onClose = { configDialogClient = null }
                    }
                }
            }
        }
    }
}
