package org.awgadmin.components

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mui.material.Alert
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Dialog
import mui.material.DialogActions
import mui.material.DialogContent
import mui.material.DialogTitle
import mui.material.FormControlVariant
import mui.material.TextField
import mui.system.sx
import org.awgadmin.api.ApiClient
import react.FC
import react.Props
import react.dom.html.ReactHTML.form
import react.useRef
import react.useState
import web.cssom.px
import web.html.HTMLInputElement

private val scope = MainScope()

external interface ChangePasswordDialogProps : Props {
    var onClose: () -> Unit
    var onSuccess: () -> Unit
}

val ChangePasswordDialog = FC<ChangePasswordDialogProps> { props ->
    val newPasswordRef = useRef<HTMLInputElement>(null)
    val confirmPasswordRef = useRef<HTMLInputElement>(null)
    var loading by useState(false)
    var error by useState<String?>(null)
    var success by useState(false)

    fun handleSubmit() {
        val newPassword = newPasswordRef.current?.value ?: ""
        val confirmPassword = confirmPasswordRef.current?.value ?: ""

        if (newPassword.isBlank()) {
            error = "New password is required"
            return
        }

        if (newPassword.length < 12) {
            error = "Password must be at least 12 characters"
            return
        }

        if (newPassword != confirmPassword) {
            error = "Passwords do not match"
            return
        }

        loading = true
        error = null

        scope.launch {
            try {
                val response = ApiClient.changePassword(newPassword)
                if (response.success) {
                    success = true
                    // Auto close after success
                    kotlinx.coroutines.delay(1500)
                    props.onSuccess()
                } else {
                    error = response.message ?: "Failed to change password"
                    loading = false
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
                loading = false
            }
        }
    }

    Dialog {
        open = true
        onClose = { _, _ -> props.onClose() }

        DialogTitle {
            +"Change Password"
        }

        DialogContent {
            sx {
                minWidth = 400.px
                paddingTop = 16.px
            }

            if (success) {
                Alert {
                    asDynamic().severity = "success"
                    +"Password changed successfully!"
                }
            } else {
                form {
                    onSubmit = { e ->
                        e.preventDefault()
                        handleSubmit()
                    }

                    TextField {
                        fullWidth = true
                        label = react.ReactNode("New Password")
                        name = "newPassword"
                        type = web.html.InputType.password
                        variant = FormControlVariant.outlined
                        autoFocus = true
                        asDynamic().inputRef = newPasswordRef
                        asDynamic().helperText = "Minimum 12 characters"
                        sx {
                            marginTop = 8.px
                            marginBottom = 16.px
                        }
                    }

                    TextField {
                        fullWidth = true
                        label = react.ReactNode("Confirm Password")
                        name = "confirmPassword"
                        type = web.html.InputType.password
                        variant = FormControlVariant.outlined
                        asDynamic().inputRef = confirmPasswordRef
                    }
                }

                error?.let { err ->
                    Alert {
                        asDynamic().severity = "error"
                        sx {
                            marginTop = 16.px
                        }
                        +err
                    }
                }
            }
        }

        DialogActions {
            Button {
                onClick = { props.onClose() }
                disabled = loading
                +"Cancel"
            }

            if (!success) {
                Button {
                    variant = ButtonVariant.contained
                    disabled = loading
                    onClick = { handleSubmit() }
                    +(if (loading) "Changing..." else "Change Password")
                }
            }
        }
    }
}
