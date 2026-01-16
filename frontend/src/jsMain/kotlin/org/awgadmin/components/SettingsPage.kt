package org.awgadmin.components

import js.objects.jso
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mui.icons.material.ContentCopy
import mui.icons.material.Dns
import mui.icons.material.Router
import mui.icons.material.Save
import mui.icons.material.Shield
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.responsive
import mui.system.sx
import org.awgadmin.api.ApiClient
import org.awgadmin.api.NetworkSettings
import org.awgadmin.api.ObfuscationSettings
import org.awgadmin.api.ServerSettings
import org.awgadmin.api.ServerStatus
import org.awgadmin.api.UpdateNetworkRequest
import org.awgadmin.api.UpdateObfuscationRequest
import react.FC
import react.Props
import react.ReactNode
import react.create
import react.useEffectOnce
import react.useRef
import react.useState
import web.cssom.*
import web.html.HTMLInputElement
import web.navigator.navigator

private val scope = MainScope()

val SettingsPage = FC<Props> {
    var serverSettings by useState<ServerSettings?>(null)
    var obfuscation by useState<ObfuscationSettings?>(null)
    var network by useState<NetworkSettings?>(null)
    var serverStatus by useState<ServerStatus?>(null)
    var loading by useState(true)
    var saving by useState(false)
    var snackbarMessage by useState<String?>(null)
    var snackbarError by useState(false)

    // Form refs
    val jcRef = useRef<HTMLInputElement>(null)
    val jminRef = useRef<HTMLInputElement>(null)
    val jmaxRef = useRef<HTMLInputElement>(null)
    val s1Ref = useRef<HTMLInputElement>(null)
    val s2Ref = useRef<HTMLInputElement>(null)
    val h1Ref = useRef<HTMLInputElement>(null)
    val h2Ref = useRef<HTMLInputElement>(null)
    val h3Ref = useRef<HTMLInputElement>(null)
    val h4Ref = useRef<HTMLInputElement>(null)
    val addressRef = useRef<HTMLInputElement>(null)
    val dnsRef = useRef<HTMLInputElement>(null)

    val loadSettings: () -> Unit = {
        scope.launch {
            loading = true
            try {
                val settings = ApiClient.getSettings()
                serverSettings = settings.server
                obfuscation = settings.obfuscation
                network = settings.network

                val status = ApiClient.getServerStatus()
                serverStatus = status
            } catch (e: Exception) {
                snackbarMessage = "Failed to load settings: ${e.message}"
                snackbarError = true
            }
            loading = false
        }
    }

    useEffectOnce {
        loadSettings()
    }

    val saveObfuscation: () -> Unit = {
        scope.launch {
            saving = true
            try {
                val request = UpdateObfuscationRequest(
                    jc = jcRef.current?.value?.toIntOrNull(),
                    jmin = jminRef.current?.value?.toIntOrNull(),
                    jmax = jmaxRef.current?.value?.toIntOrNull(),
                    s1 = s1Ref.current?.value?.toIntOrNull(),
                    s2 = s2Ref.current?.value?.toIntOrNull(),
                    h1 = h1Ref.current?.value?.toLongOrNull(),
                    h2 = h2Ref.current?.value?.toLongOrNull(),
                    h3 = h3Ref.current?.value?.toLongOrNull(),
                    h4 = h4Ref.current?.value?.toLongOrNull(),
                )
                val result = ApiClient.updateObfuscation(request)
                snackbarMessage = result.message
                snackbarError = false
                loadSettings()
            } catch (e: Exception) {
                snackbarMessage = "Failed to save: ${e.message}"
                snackbarError = true
            }
            saving = false
        }
    }

    val saveNetwork: () -> Unit = {
        scope.launch {
            saving = true
            try {
                val dnsValue = dnsRef.current?.value ?: ""
                val dnsList = dnsValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val request = UpdateNetworkRequest(
                    address = addressRef.current?.value,
                    dns = dnsList.takeIf { it.isNotEmpty() },
                )
                val result = ApiClient.updateNetwork(request)
                snackbarMessage = result.message
                snackbarError = false
                loadSettings()
            } catch (e: Exception) {
                snackbarMessage = "Failed to save: ${e.message}"
                snackbarError = true
            }
            saving = false
        }
    }

    val copyToClipboard: (String) -> Unit = { text ->
        scope.launch {
            navigator.clipboard.writeText(text)
            snackbarMessage = "Copied to clipboard"
            snackbarError = false
        }
    }

    Box {
        sx {
            padding = 24.px
            maxWidth = 1200.px
            margin = Auto.auto
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
        } else {
            // Server Status Card
            Card {
                sx { marginBottom = 24.px }
                CardContent {
                    Box {
                        sx {
                            display = Display.flex
                            alignItems = AlignItems.center
                            marginBottom = 16.px
                        }
                        Router {
                            sx { marginRight = 8.px }
                        }
                        Typography {
                            variant = TypographyVariant.h6
                            +"Server Status"
                        }
                    }

                    Grid {
                        container = true
                        spacing = responsive(2)

                        // Status
                        Grid {
                            asDynamic().item = true
                            asDynamic().xs = 12
                            asDynamic().sm = 6
                            asDynamic().md = 3

                            Paper {
                                sx {
                                    padding = 16.px
                                    textAlign = TextAlign.center
                                    backgroundColor = if (serverStatus?.isRunning == true)
                                        Color("rgba(76, 175, 80, 0.15)") else Color("rgba(244, 67, 54, 0.15)")
                                    border = if (serverStatus?.isRunning == true)
                                        "1px solid rgba(76, 175, 80, 0.3)".asDynamic() else "1px solid rgba(244, 67, 54, 0.3)".asDynamic()
                                }
                                Typography {
                                    variant = TypographyVariant.body2
                                    sx { color = "text.secondary".asDynamic() }
                                    +"Status"
                                }
                                Typography {
                                    variant = TypographyVariant.h6
                                    sx {
                                        color = if (serverStatus?.isRunning == true)
                                            Color("#4CAF50") else Color("#f44336")
                                    }
                                    +(if (serverStatus?.isRunning == true) "Running" else "Stopped")
                                }
                            }
                        }

                        // Peers
                        Grid {
                            asDynamic().item = true
                            asDynamic().xs = 12
                            asDynamic().sm = 6
                            asDynamic().md = 3

                            Paper {
                                sx {
                                    padding = 16.px
                                    textAlign = TextAlign.center
                                }
                                Typography {
                                    variant = TypographyVariant.body2
                                    sx { color = "text.secondary".asDynamic() }
                                    +"Active Peers"
                                }
                                Typography {
                                    variant = TypographyVariant.h6
                                    +"${serverStatus?.peersCount ?: 0}"
                                }
                            }
                        }

                        // RX
                        Grid {
                            asDynamic().item = true
                            asDynamic().xs = 12
                            asDynamic().sm = 6
                            asDynamic().md = 3

                            Paper {
                                sx {
                                    padding = 16.px
                                    textAlign = TextAlign.center
                                }
                                Typography {
                                    variant = TypographyVariant.body2
                                    sx { color = "text.secondary".asDynamic() }
                                    +"Total RX"
                                }
                                Typography {
                                    variant = TypographyVariant.h6
                                    +formatBytes(serverStatus?.transferRx ?: 0)
                                }
                            }
                        }

                        // TX
                        Grid {
                            asDynamic().item = true
                            asDynamic().xs = 12
                            asDynamic().sm = 6
                            asDynamic().md = 3

                            Paper {
                                sx {
                                    padding = 16.px
                                    textAlign = TextAlign.center
                                }
                                Typography {
                                    variant = TypographyVariant.body2
                                    sx { color = "text.secondary".asDynamic() }
                                    +"Total TX"
                                }
                                Typography {
                                    variant = TypographyVariant.h6
                                    +formatBytes(serverStatus?.transferTx ?: 0)
                                }
                            }
                        }
                    }
                }
            }

            // Server Info Card
            serverSettings?.let { server ->
                Card {
                    sx { marginBottom = 24.px }
                    CardContent {
                        Box {
                            sx {
                                display = Display.flex
                                alignItems = AlignItems.center
                                marginBottom = 16.px
                            }
                            Dns {
                                sx { marginRight = 8.px }
                            }
                            Typography {
                                variant = TypographyVariant.h6
                                +"Server Information"
                            }
                        }

                        Grid {
                            container = true
                            spacing = responsive(2)

                            // Interface
                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().md = 4

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("Interface")
                                    value = server.interfaceName
                                    asDynamic().InputProps = jso<dynamic> {
                                        readOnly = true
                                    }
                                }
                            }

                            // Endpoint
                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().md = 4

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("Endpoint")
                                    value = "${server.endpoint}:${server.listenPort}"
                                    asDynamic().InputProps = jso<dynamic> {
                                        readOnly = true
                                    }
                                }
                            }

                            // Public Key
                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().md = 4

                                Box {
                                    sx {
                                        display = Display.flex
                                        alignItems = AlignItems.center
                                        gap = 8.px
                                    }
                                    TextField {
                                        fullWidth = true
                                        label = ReactNode("Public Key")
                                        value = server.publicKey
                                        asDynamic().InputProps = jso<dynamic> {
                                            readOnly = true
                                        }
                                    }
                                    IconButton {
                                        size = Size.small
                                        onClick = { copyToClipboard(server.publicKey) }
                                        ContentCopy {}
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Obfuscation Settings Card
            obfuscation?.let { obf ->
                Card {
                    sx { marginBottom = 24.px }
                    CardContent {
                        Box {
                            sx {
                                display = Display.flex
                                alignItems = AlignItems.center
                                justifyContent = JustifyContent.spaceBetween
                                marginBottom = 16.px
                            }
                            Box {
                                sx {
                                    display = Display.flex
                                    alignItems = AlignItems.center
                                }
                                Shield {
                                    sx { marginRight = 8.px }
                                }
                                Typography {
                                    variant = TypographyVariant.h6
                                    +"AmneziaWG Obfuscation"
                                }
                            }
                            Button {
                                variant = ButtonVariant.contained
                                startIcon = Save.create()
                                disabled = saving
                                onClick = { saveObfuscation() }
                                +"Save"
                            }
                        }

                        Alert {
                            asDynamic().severity = "info"
                            sx { marginBottom = 16.px }
                            +"All clients must use the same obfuscation parameters. Restart required after changes."
                        }

                        Grid {
                            container = true
                            spacing = responsive(2)

                            // Jc
                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().sm = 6
                                asDynamic().md = 4

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("Jc (Junk packet count)")
                                    type = web.html.InputType.number
                                    defaultValue = obf.jc.toString()
                                    asDynamic().inputRef = jcRef
                                    helperText = ReactNode("Range: 1-128")
                                }
                            }

                            // Jmin
                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().sm = 6
                                asDynamic().md = 4

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("Jmin (Min junk size)")
                                    type = web.html.InputType.number
                                    defaultValue = obf.jmin.toString()
                                    asDynamic().inputRef = jminRef
                                    helperText = ReactNode("Range: 0-1280")
                                }
                            }

                            // Jmax
                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().sm = 6
                                asDynamic().md = 4

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("Jmax (Max junk size)")
                                    type = web.html.InputType.number
                                    defaultValue = obf.jmax.toString()
                                    asDynamic().inputRef = jmaxRef
                                    helperText = ReactNode("Range: 0-1280, >= Jmin")
                                }
                            }

                            // S1
                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().sm = 6
                                asDynamic().md = 6

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("S1 (Init packet part 1)")
                                    type = web.html.InputType.number
                                    defaultValue = obf.s1.toString()
                                    asDynamic().inputRef = s1Ref
                                    helperText = ReactNode("Range: 0-255")
                                }
                            }

                            // S2
                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().sm = 6
                                asDynamic().md = 6

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("S2 (Init packet part 2)")
                                    type = web.html.InputType.number
                                    defaultValue = obf.s2.toString()
                                    asDynamic().inputRef = s2Ref
                                    helperText = ReactNode("Range: 0-255")
                                }
                            }

                            // H1-H4
                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().sm = 6
                                asDynamic().md = 3

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("H1 (Header key)")
                                    defaultValue = obf.h1.toString()
                                    asDynamic().inputRef = h1Ref
                                }
                            }

                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().sm = 6
                                asDynamic().md = 3

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("H2 (Header key)")
                                    defaultValue = obf.h2.toString()
                                    asDynamic().inputRef = h2Ref
                                }
                            }

                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().sm = 6
                                asDynamic().md = 3

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("H3 (Header key)")
                                    defaultValue = obf.h3.toString()
                                    asDynamic().inputRef = h3Ref
                                }
                            }

                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().sm = 6
                                asDynamic().md = 3

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("H4 (Header key)")
                                    defaultValue = obf.h4.toString()
                                    asDynamic().inputRef = h4Ref
                                }
                            }
                        }
                    }
                }
            }

            // Network Settings Card
            network?.let { net ->
                Card {
                    CardContent {
                        Box {
                            sx {
                                display = Display.flex
                                alignItems = AlignItems.center
                                justifyContent = JustifyContent.spaceBetween
                                marginBottom = 16.px
                            }
                            Box {
                                sx {
                                    display = Display.flex
                                    alignItems = AlignItems.center
                                }
                                Dns {
                                    sx { marginRight = 8.px }
                                }
                                Typography {
                                    variant = TypographyVariant.h6
                                    +"Network Settings"
                                }
                            }
                            Button {
                                variant = ButtonVariant.contained
                                startIcon = Save.create()
                                disabled = saving
                                onClick = { saveNetwork() }
                                +"Save"
                            }
                        }

                        Grid {
                            container = true
                            spacing = responsive(2)

                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().md = 6

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("Server Address (CIDR)")
                                    defaultValue = net.address
                                    asDynamic().inputRef = addressRef
                                    helperText = ReactNode("e.g., 10.0.0.1/24")
                                }
                            }

                            Grid {
                                asDynamic().item = true
                                asDynamic().xs = 12
                                asDynamic().md = 6

                                TextField {
                                    fullWidth = true
                                    label = ReactNode("DNS Servers")
                                    defaultValue = net.dns.joinToString(", ")
                                    asDynamic().inputRef = dnsRef
                                    helperText = ReactNode("Comma-separated, e.g., 1.1.1.1, 8.8.8.8")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Snackbar for notifications
        snackbarMessage?.let { message ->
            Snackbar {
                open = true
                autoHideDuration = 4000
                onClose = { _, _ -> snackbarMessage = null }
                anchorOrigin = jso {
                    vertical = SnackbarOriginVertical.bottom
                    horizontal = SnackbarOriginHorizontal.center
                }

                Alert {
                    onClose = { snackbarMessage = null }
                    asDynamic().severity = if (snackbarError) "error" else "success"
                    +message
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.toInt()} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${mb.toInt()} MB"
    val gb = mb / 1024.0
    return "${(gb * 10).toInt() / 10.0} GB"
}
