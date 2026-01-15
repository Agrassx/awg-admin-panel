package org.awgadmin.components

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mui.material.Alert
import mui.material.Box
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Card
import mui.material.CardContent
import mui.material.FormControlVariant
import mui.material.InputAdornment
import mui.material.InputAdornmentPosition
import mui.material.TextField
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.sx
import org.awgadmin.api.ApiClient
import react.FC
import react.Props
import react.dom.html.ReactHTML.form
import react.useRef
import react.useState
import web.cssom.AlignItems
import web.cssom.Display
import web.cssom.FlexDirection
import web.cssom.JustifyContent
import web.cssom.pct
import web.cssom.px
import web.cssom.vh
import web.html.HTMLInputElement

private val scope = MainScope()

external interface LoginPageProps : Props {
    var onLoginSuccess: (username: String) -> Unit
}

val LoginPage = FC<LoginPageProps> { props ->
    val usernameRef = useRef<HTMLInputElement>(null)
    val passwordRef = useRef<HTMLInputElement>(null)
    var loading by useState(false)
    var error by useState<String?>(null)
    var showPassword by useState(false)

    fun handleLogin() {
        val username = usernameRef.current?.value ?: ""
        val password = passwordRef.current?.value ?: ""

        if (username.isBlank() || password.isBlank()) {
            error = "Please enter username and password"
            return
        }

        loading = true
        error = null

        scope.launch {
            try {
                val response = ApiClient.login(username, password)
                if (response.success && response.username != null) {
                    props.onLoginSuccess(response.username)
                } else {
                    error = response.message ?: "Login failed"
                    loading = false
                }
            } catch (e: Exception) {
                error = "Connection error: ${e.message}"
                loading = false
            }
        }
    }

    Box {
        sx {
            minHeight = 100.vh
            display = Display.flex
            alignItems = AlignItems.center
            justifyContent = JustifyContent.center
            background = "linear-gradient(135deg, #0D1117 0%, #161B22 50%, #1F2937 100%)".asDynamic()
        }

        Card {
            sx {
                width = 400.px
                maxWidth = 90.pct
                backgroundColor = "rgba(22, 27, 34, 0.95)".asDynamic()
                backdropFilter = "blur(10px)".asDynamic()
                border = "1px solid rgba(124, 77, 255, 0.3)".asDynamic()
            }

            CardContent {
                sx {
                    padding = 32.px
                }

                Box {
                    sx {
                        display = Display.flex
                        flexDirection = FlexDirection.column
                        alignItems = AlignItems.center
                        marginBottom = 32.px
                    }

                    // Logo/Title
                    Typography {
                        variant = TypographyVariant.h4
                        sx {
                            fontWeight = "bold".asDynamic()
                            background = "linear-gradient(90deg, #7C4DFF, #00E5FF)".asDynamic()
                            asDynamic().WebkitBackgroundClip = "text"
                            asDynamic().WebkitTextFillColor = "transparent"
                            marginBottom = 8.px
                        }
                        +"AWG Admin"
                    }

                    Typography {
                        sx {
                            color = "text.secondary".asDynamic()
                        }
                        +"Sign in to manage VPN clients"
                    }
                }

                form {
                    onSubmit = { e ->
                        e.preventDefault()
                        handleLogin()
                    }

                    TextField {
                        fullWidth = true
                        label = react.ReactNode("Username")
                        name = "username"
                        variant = FormControlVariant.outlined
                        autoFocus = true
                        autoComplete = "username"
                        asDynamic().inputRef = usernameRef
                        sx {
                            marginBottom = 16.px
                        }
                    }

                    TextField {
                        fullWidth = true
                        label = react.ReactNode("Password")
                        name = "password"
                        type = if (showPassword) web.html.InputType.text else web.html.InputType.password
                        variant = FormControlVariant.outlined
                        autoComplete = "current-password"
                        asDynamic().inputRef = passwordRef
                        sx {
                            marginBottom = 24.px
                        }
                    }

                    error?.let { err ->
                        Alert {
                            asDynamic().severity = "error"
                            sx {
                                marginBottom = 16.px
                            }
                            +err
                        }
                    }

                    Button {
                        fullWidth = true
                        variant = ButtonVariant.contained
                        disabled = loading
                        type = web.html.ButtonType.submit
                        sx {
                            height = 48.px
                            fontSize = 16.px
                        }
                        +(if (loading) "Signing in..." else "Sign In")
                    }
                }

                Box {
                    sx {
                        marginTop = 24.px
                        padding = 16.px
                        backgroundColor = "rgba(124, 77, 255, 0.1)".asDynamic()
                        borderRadius = 8.px
                    }

                    Typography {
                        variant = TypographyVariant.body2
                        sx {
                            color = "text.secondary".asDynamic()
                            textAlign = "center".asDynamic()
                        }
                        +"First run? Check the server logs for the generated admin password."
                    }
                }
            }
        }
    }
}
