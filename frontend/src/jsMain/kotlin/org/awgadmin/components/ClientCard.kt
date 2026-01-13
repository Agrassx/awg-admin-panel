package org.awgadmin.components

import mui.icons.material.Delete
import mui.icons.material.PowerSettingsNew
import mui.icons.material.QrCode
import mui.material.Box
import mui.material.Card
import mui.material.CardActions
import mui.material.CardContent
import mui.material.Chip
import mui.material.ChipColor
import mui.material.IconButton
import mui.material.IconButtonColor
import mui.material.Size
import mui.material.Typography
import mui.system.sx
import org.awgadmin.api.ClientDto
import react.FC
import react.Props
import web.cssom.AlignItems
import web.cssom.Display
import web.cssom.FlexDirection
import web.cssom.JustifyContent
import web.cssom.px

external interface ClientCardProps : Props {
    var client: ClientDto
    var onToggle: () -> Unit
    var onDelete: () -> Unit
    var onShowConfig: () -> Unit
}

val ClientCard = FC<ClientCardProps> { props ->
    val client = props.client

    Card {
        sx {
            width = 320.px
            background = "rgba(22, 27, 34, 0.8)".asDynamic()
            backdropFilter = "blur(10px)".asDynamic()
            border = "1px solid rgba(255,255,255,0.1)".asDynamic()
        }

        CardContent {
            Box {
                sx {
                    display = Display.flex
                    alignItems = AlignItems.center
                    justifyContent = JustifyContent.spaceBetween
                    marginBottom = 16.px
                }

                Typography {
                    variant = "h6".asDynamic()
                    +client.name
                }

                Chip {
                    size = Size.small
                    color = if (client.isOnline) ChipColor.success else ChipColor.default
                    label = react.ReactNode(if (client.isOnline) "Online" else "Offline")
                }
            }

            Box {
                sx {
                    display = Display.flex
                    flexDirection = FlexDirection.column
                    gap = 8.px
                }

                InfoRow {
                    label = "IP"
                    value = client.ipAddress
                }

                InfoRow {
                    label = "Status"
                    value = if (client.isEnabled) "Enabled" else "Disabled"
                }

                client.expiresAt?.let {
                    InfoRow {
                        label = "Expires"
                        value = it.substringBefore("T")
                    }
                }

                if (client.transferRx > 0 || client.transferTx > 0) {
                    InfoRow {
                        label = "Traffic"
                        value = "↓${formatBytes(client.transferRx)} ↑${formatBytes(client.transferTx)}"
                    }
                }
            }
        }

        CardActions {
            sx {
                justifyContent = JustifyContent.flexEnd
                borderTop = "1px solid rgba(255,255,255,0.05)".asDynamic()
            }

            IconButton {
                color = IconButtonColor.primary
                onClick = { props.onShowConfig() }
                title = "Show QR Code"
                QrCode {}
            }

            IconButton {
                color = if (client.isEnabled) IconButtonColor.warning else IconButtonColor.success
                onClick = { props.onToggle() }
                title = if (client.isEnabled) "Disable" else "Enable"
                PowerSettingsNew {}
            }

            IconButton {
                color = IconButtonColor.error
                onClick = { props.onDelete() }
                title = "Delete"
                Delete {}
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
