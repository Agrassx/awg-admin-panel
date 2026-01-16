package org.awgadmin.components

import mui.icons.material.Add
import mui.icons.material.Group
import mui.icons.material.Logout
import mui.icons.material.Person
import mui.icons.material.Refresh
import mui.icons.material.Security
import mui.icons.material.Settings
import mui.material.AppBar
import mui.material.AppBarPosition
import mui.material.Box
import mui.material.Chip
import mui.material.ChipVariant
import mui.material.IconButton
import mui.material.IconButtonColor
import mui.material.Tab
import mui.material.Tabs
import mui.material.TabsTextColor
import mui.material.Toolbar
import mui.material.Tooltip
import mui.material.Typography
import mui.system.sx
import org.awgadmin.ui.AppTab
import react.FC
import react.Props
import react.ReactNode
import react.create
import web.cssom.AlignItems
import web.cssom.Display
import web.cssom.FlexGrow
import web.cssom.JustifyContent
import web.cssom.number
import web.cssom.px

external interface HeaderProps : Props {
    var username: String?
    var onAddClick: () -> Unit
    var onRefreshClick: () -> Unit
    var onLogoutClick: (() -> Unit)?
    var currentTab: AppTab
    var onTabChange: (AppTab) -> Unit
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
            sx {
                display = Display.flex
                justifyContent = JustifyContent.spaceBetween
                gap = 24.px
            }

            // Left group: Logo + Title
            Box {
                sx {
                    display = Display.flex
                    alignItems = AlignItems.center
                }
                Security {
                    sx {
                        marginRight = 12.px
                        color = "primary.main".asDynamic()
                        fontSize = 28.px
                    }
                }
                Typography {
                    variant = "h5".asDynamic()
                    sx {
                        fontWeight = "bold".asDynamic()
                    }
                    +"AWG Admin"
                }
            }

            // Center group: Navigation tabs
            Box {
                sx {
                    flexGrow = number(1.0)
                    display = Display.flex
                    justifyContent = JustifyContent.center
                }
                Tabs {
                    value = props.currentTab.ordinal
                    onChange = { _, newValue ->
                        props.onTabChange(AppTab.entries[newValue as Int])
                    }
                    textColor = TabsTextColor.inherit
                    sx {
                        asDynamic()["& .MuiTab-root"] = js("{color: 'rgba(255,255,255,0.7)'}")
                        asDynamic()["& .Mui-selected"] = js("{color: '#7C4DFF'}")
                        asDynamic()["& .MuiTabs-indicator"] = js("{backgroundColor: '#7C4DFF'}")
                    }

                    Tab {
                        icon = Group.create()
                        iconPosition = mui.material.IconPosition.start
                        label = ReactNode("Clients")
                    }
                    Tab {
                        icon = Settings.create()
                        iconPosition = mui.material.IconPosition.start
                        label = ReactNode("Settings")
                    }
                    Tab {
                        icon = Person.create()
                        iconPosition = mui.material.IconPosition.start
                        label = ReactNode("Profile")
                    }
                }
            }

            // Right group: Actions + User chip + Logout
            Box {
                sx {
                    display = Display.flex
                    alignItems = AlignItems.center
                    gap = 8.px
                }

                // Show Add/Refresh only on Clients tab
                if (props.currentTab == AppTab.CLIENTS) {
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
                }

                // User chip
                props.username?.let { username ->
                    Chip {
                        label = ReactNode(username)
                        variant = ChipVariant.outlined
                        sx {
                            borderColor = "rgba(124, 77, 255, 0.5)".asDynamic()
                            color = "text.secondary".asDynamic()
                        }
                    }
                }

                // Logout
                props.onLogoutClick?.let { logout ->
                    Tooltip {
                        title = ReactNode("Logout")
                        IconButton {
                            color = IconButtonColor.inherit
                            onClick = { logout() }
                            Logout {}
                        }
                    }
                }
            }
        }
    }
}
