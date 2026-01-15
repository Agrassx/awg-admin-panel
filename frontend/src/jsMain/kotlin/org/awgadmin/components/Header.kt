package org.awgadmin.components

import mui.icons.material.Add
import mui.icons.material.Key
import mui.icons.material.Logout
import mui.icons.material.Refresh
import mui.icons.material.Security
import mui.material.AppBar
import mui.material.AppBarPosition
import mui.material.Box
import mui.material.Chip
import mui.material.ChipVariant
import mui.material.IconButton
import mui.material.IconButtonColor
import mui.material.Toolbar
import mui.material.Tooltip
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import web.cssom.AlignItems
import web.cssom.Display
import web.cssom.FlexGrow
import web.cssom.number
import web.cssom.px

external interface HeaderProps : Props {
    var username: String?
    var onAddClick: () -> Unit
    var onRefreshClick: () -> Unit
    var onChangePasswordClick: (() -> Unit)?
    var onLogoutClick: (() -> Unit)?
}

val Header = FC<HeaderProps> { props ->
    AppBar {
        position = AppBarPosition.static
        sx {
            background = "transparent".asDynamic()
            boxShadow = "none".asDynamic()
            borderBottom = "1px solid rgba(255,255,255,0.1)".asDynamic()
        }

        Toolbar {
            Security {
                sx {
                    marginRight = 16.asDynamic()
                    color = "primary.main".asDynamic()
                }
            }

            Typography {
                variant = "h5".asDynamic()
                sx {
                    flexGrow = number(1.0)
                    fontWeight = "bold".asDynamic()
                }
                +"AWG Admin"
            }

            // User info
            props.username?.let { username ->
                Box {
                    sx {
                        display = Display.flex
                        alignItems = AlignItems.center
                        marginRight = 8.px
                    }

                    Chip {
                        label = ReactNode(username)
                        variant = ChipVariant.outlined
                        onClick = { props.onChangePasswordClick?.invoke() }
                        sx {
                            borderColor = "rgba(124, 77, 255, 0.5)".asDynamic()
                            color = "text.secondary".asDynamic()
                            cursor = "pointer".asDynamic()
                        }
                    }
                }
            }

            // Change password button
            props.onChangePasswordClick?.let { changePassword ->
                Tooltip {
                    title = ReactNode("Change Password")

                    IconButton {
                        color = IconButtonColor.inherit
                        onClick = { changePassword() }
                        Key {}
                    }
                }
            }

            Tooltip {
                title = ReactNode("Refresh")

                IconButton {
                    color = IconButtonColor.inherit
                    onClick = { props.onRefreshClick() }
                    Refresh {}
                }
            }

            Tooltip {
                title = ReactNode("Add Client")

                IconButton {
                    color = IconButtonColor.primary
                    onClick = { props.onAddClick() }
                    Add {}
                }
            }

            props.onLogoutClick?.let { logout ->
                Tooltip {
                    title = ReactNode("Logout")

                    IconButton {
                        color = IconButtonColor.inherit
                        onClick = { logout() }
                        sx {
                            marginLeft = 8.px
                        }
                        Logout {}
                    }
                }
            }
        }
    }
}
