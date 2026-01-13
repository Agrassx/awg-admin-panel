package org.awgadmin.components

import mui.icons.material.Add
import mui.icons.material.Refresh
import mui.icons.material.Security
import mui.material.AppBar
import mui.material.AppBarPosition
import mui.material.IconButton
import mui.material.IconButtonColor
import mui.material.Toolbar
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import web.cssom.FlexGrow
import web.cssom.number

external interface HeaderProps : Props {
    var onAddClick: () -> Unit
    var onRefreshClick: () -> Unit
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

            IconButton {
                color = IconButtonColor.inherit
                onClick = { props.onRefreshClick() }
                Refresh {}
            }

            IconButton {
                color = IconButtonColor.primary
                onClick = { props.onAddClick() }
                Add {}
            }
        }
    }
}
