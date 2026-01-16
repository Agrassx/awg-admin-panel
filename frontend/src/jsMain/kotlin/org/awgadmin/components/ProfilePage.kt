package org.awgadmin.components

import js.objects.jso
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mui.icons.material.Lock
import mui.icons.material.Person
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.responsive
import mui.system.sx
import org.awgadmin.api.ApiClient
import react.FC
import react.Props
import react.ReactNode
import react.create
import react.useRef
import react.useState
import web.cssom.*
import web.html.HTMLInputElement

private val scope = MainScope()

external interface ProfilePageProps : Props {
    var username: String
    var onPasswordChanged: () -> Unit
}

val ProfilePage = FC<ProfilePageProps> { props ->
    var saving by useState(false)
    var snackbarMessage by useState<String?>(null)
    var snackbarError by useState(false)
    var showSuccess by useState(false)

    val currentPasswordRef = useRef<HTMLInputElement>(null)
    val newPasswordRef = useRef<HTMLInputElement>(null)
    val confirmPasswordRef = useRef<HTMLInputElement>(null)

    val handleChangePassword: () -> Unit = {
        val newPassword = newPasswordRef.current?.value ?: ""
        val confirmPassword = confirmPasswordRef.current?.value ?: ""

        when {
            newPassword.length < 12 -> {
                snackbarMessage = "Password must be at least 12 characters"
                snackbarError = true
            }
            newPassword != confirmPassword -> {
                snackbarMessage = "Passwords do not match"
                snackbarError = true
            }
            else -> {
                saving = true
                scope.launch {
                    try {
                        val result = ApiClient.changePassword(newPassword)
                        if (result.success) {
                            snackbarMessage = "Password changed successfully!"
                            snackbarError = false
                            showSuccess = true
                            // Clear fields
                            currentPasswordRef.current?.value = ""
                            newPasswordRef.current?.value = ""
                            confirmPasswordRef.current?.value = ""
                            props.onPasswordChanged()
                        } else {
                            snackbarMessage = result.message ?: "Failed to change password"
                            snackbarError = true
                        }
                    } catch (e: Exception) {
                        snackbarMessage = "Error: ${e.message}"
                        snackbarError = true
                    }
                    saving = false
                }
            }
        }
    }

    Box {
        sx {
            padding = 24.px
            maxWidth = 800.px
            margin = Auto.auto
        }

        // Profile Card
        Card {
            sx { marginBottom = 24.px }
            CardContent {
                Box {
                    sx {
                        display = Display.flex
                        alignItems = AlignItems.center
                        marginBottom = 24.px
                    }
                    Person {
                        sx { marginRight = 8.px }
                    }
                    Typography {
                        variant = TypographyVariant.h6
                        +"Profile"
                    }
                }

                Grid {
                    container = true
                    spacing = responsive(2)

                    Grid {
                        asDynamic().item = true
                        asDynamic().xs = 12

                        TextField {
                            fullWidth = true
                            label = ReactNode("Username")
                            value = props.username
                            asDynamic().InputProps = jso<dynamic> {
                                readOnly = true
                            }
                            helperText = ReactNode("Username cannot be changed")
                        }
                    }
                }
            }
        }

        // Change Password Card
        Card {
            CardContent {
                Box {
                    sx {
                        display = Display.flex
                        alignItems = AlignItems.center
                        marginBottom = 24.px
                    }
                    Lock {
                        sx { marginRight = 8.px }
                    }
                    Typography {
                        variant = TypographyVariant.h6
                        +"Change Password"
                    }
                }

                Grid {
                    container = true
                    spacing = responsive(2)

                    Grid {
                        asDynamic().item = true
                        asDynamic().xs = 12

                        TextField {
                            fullWidth = true
                            label = ReactNode("New Password")
                            type = web.html.InputType.password
                            asDynamic().inputRef = newPasswordRef
                            helperText = ReactNode("Minimum 12 characters")
                        }
                    }

                    Grid {
                        asDynamic().item = true
                        asDynamic().xs = 12

                        TextField {
                            fullWidth = true
                            label = ReactNode("Confirm Password")
                            type = web.html.InputType.password
                            asDynamic().inputRef = confirmPasswordRef
                        }
                    }

                    Grid {
                        asDynamic().item = true
                        asDynamic().xs = 12

                        Button {
                            variant = ButtonVariant.contained
                            disabled = saving
                            onClick = { handleChangePassword() }
                            sx { marginTop = 8.px }
                            +(if (saving) "Saving..." else "Change Password")
                        }
                    }
                }
            }
        }

        // Snackbar
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
