package org.awgadmin.components

import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mui.icons.material.ContentCopy
import mui.icons.material.Download
import mui.material.Box
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Dialog
import mui.material.DialogActions
import mui.material.DialogContent
import mui.material.DialogTitle
import mui.material.IconButton
import mui.material.Typography
import mui.system.sx
import org.awgadmin.api.ApiClient
import org.awgadmin.api.ClientDto
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import react.FC
import react.Props
import react.useEffectOnce
import react.useState
import web.cssom.AlignItems
import web.cssom.Display
import web.cssom.JustifyContent
import web.cssom.pct
import web.cssom.px

private val scope = MainScope()

external interface ClientConfigDialogProps : Props {
    var client: ClientDto
    var onClose: () -> Unit
}

val ClientConfigDialog = FC<ClientConfigDialogProps> { props ->
    var config by useState<String?>(null)
    var loading by useState(true)

    useEffectOnce {
        scope.launch {
            try {
                config = ApiClient.getClientConfig(props.client.id)
            } catch (e: Exception) {
                config = "Error loading config: ${e.message}"
            }
            loading = false
        }
    }

    fun copyToClipboard() {
        config?.let {
            kotlinx.browser.window.navigator.clipboard.writeText(it)
        }
    }

    fun downloadConfig() {
        config?.let { content ->
            val blob = Blob(arrayOf(content), BlobPropertyBag(type = "text/plain"))
            val url = URL.createObjectURL(blob)
            val link = document.createElement("a") as HTMLAnchorElement
            link.href = url
            link.download = "${props.client.name}.conf"
            link.click()
            URL.revokeObjectURL(url)
        }
    }

    Dialog {
        open = true
        onClose = { _, _ -> props.onClose() }
        maxWidth = "md"
        fullWidth = true

        DialogTitle {
            Box {
                sx {
                    display = Display.flex
                    alignItems = AlignItems.center
                    justifyContent = JustifyContent.spaceBetween
                }

                Typography {
                    variant = "h6".asDynamic()
                    +"Config: ${props.client.name}"
                }

                Box {
                    IconButton {
                        onClick = { copyToClipboard() }
                        title = "Copy"
                        ContentCopy {}
                    }
                    IconButton {
                        onClick = { downloadConfig() }
                        title = "Download"
                        Download {}
                    }
                }
            }
        }

        DialogContent {
            if (loading) {
                Typography { +"Loading..." }
            } else {
                Box {
                    component = react.dom.html.ReactHTML.pre
                    sx {
                        background = "rgba(0,0,0,0.3)".asDynamic()
                        padding = 16.px
                        borderRadius = 8.px
                        overflow = "auto".asDynamic()
                        fontSize = 13.px
                        fontFamily = "'JetBrains Mono', monospace".asDynamic()
                        maxHeight = 400.px
                        width = 100.pct
                    }
                    +config.orEmpty()
                }
            }

            Typography {
                variant = "body2".asDynamic()
                sx {
                    marginTop = 16.px
                    color = "text.secondary".asDynamic()
                }
                +"Import this config into AmneziaVPN app to connect."
            }
        }

        DialogActions {
            Button {
                variant = ButtonVariant.contained
                onClick = { props.onClose() }
                +"Close"
            }
        }
    }
}
