package org.awgadmin.ui

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mui.material.Box
import mui.material.Container
import mui.material.CssBaseline
import mui.material.Typography
import mui.material.styles.ThemeProvider
import mui.system.sx
import org.awgadmin.api.ApiClient
import org.awgadmin.api.ClientDto
import org.awgadmin.components.AddClientDialog
import org.awgadmin.components.ClientCard
import org.awgadmin.components.ClientConfigDialog
import org.awgadmin.components.Header
import react.FC
import react.Props
import react.useEffectOnce
import react.useState
import web.cssom.Display
import web.cssom.FlexWrap
import web.cssom.pct
import web.cssom.px

private val scope = MainScope()

val App = FC<Props> {
    var clients by useState<List<ClientDto>>(emptyList())
    var loading by useState(true)
    var error by useState<String?>(null)
    var showAddDialog by useState(false)
    var configDialogClient by useState<ClientDto?>(null)

    fun loadClients() {
        scope.launch {
            try {
                clients = ApiClient.getClients()
                loading = false
                error = null
            } catch (e: Exception) {
                error = e.message
                loading = false
            }
        }
    }

    useEffectOnce {
        loadClients()
    }

    ThemeProvider {
        theme = appTheme

        CssBaseline {}

        Box {
            sx {
                minHeight = 100.pct
                background = "linear-gradient(135deg, #0D1117 0%, #161B22 50%, #1F2937 100%)".asDynamic()
            }

            Header {
                onAddClick = { showAddDialog = true }
                onRefreshClick = { loadClients() }
            }

            Container {
                maxWidth = "lg"
                sx {
                    paddingTop = 32.px
                    paddingBottom = 32.px
                }

                if (loading) {
                    Typography {
                        +"Loading..."
                    }
                }

                error?.let { err ->
                    Typography {
                        sx { color = "error.main".asDynamic() }
                        +"Error: $err"
                    }
                }

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

                    if (clients.isEmpty() && !loading) {
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

        if (showAddDialog) {
            AddClientDialog {
                onClose = { showAddDialog = false }
                onCreated = {
                    showAddDialog = false
                    loadClients()
                }
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
