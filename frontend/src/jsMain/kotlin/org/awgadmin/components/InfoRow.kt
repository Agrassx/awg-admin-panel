package org.awgadmin.components

import mui.material.Box
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import web.cssom.Display
import web.cssom.JustifyContent

external interface InfoRowProps : Props {
    var label: String
    var value: String
}

val InfoRow = FC<InfoRowProps> { props ->
    Box {
        sx {
            display = Display.flex
            justifyContent = JustifyContent.spaceBetween
        }

        Typography {
            variant = "body2".asDynamic()
            sx { color = "text.secondary".asDynamic() }
            +props.label
        }

        Typography {
            variant = "body2".asDynamic()
            sx { fontFamily = "monospace".asDynamic() }
            +props.value
        }
    }
}
