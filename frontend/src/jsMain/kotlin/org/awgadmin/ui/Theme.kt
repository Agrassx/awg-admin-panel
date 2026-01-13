package org.awgadmin.ui

import js.objects.jso
import mui.material.styles.createTheme
import web.cssom.px

val appTheme = createTheme(
    jso {
        palette = jso {
            mode = "dark".asDynamic()
            primary = jso {
                main = "#7C4DFF"
            }
            secondary = jso {
                main = "#00E5FF"
            }
            background = jso {
                default = "#0D1117"
                paper = "#161B22"
            }
        }
        typography = jso {
            fontFamily = "'JetBrains Mono', 'Fira Code', monospace"
        }
        shape = jso {
            borderRadius = 12.px
        }
    }
)
