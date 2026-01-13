package org.awgadmin.components

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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
import org.awgadmin.api.CreateClientRequest
import react.FC
import react.Props
import react.dom.html.ReactHTML.form
import react.useRef
import react.useState
import web.cssom.px
import web.html.HTMLInputElement

private val scope = MainScope()

external interface AddClientDialogProps : Props {
    var onClose: () -> Unit
    var onCreated: () -> Unit
}

val AddClientDialog = FC<AddClientDialogProps> { props ->
    val nameRef = useRef<HTMLInputElement>(null)
    val expiresAtRef = useRef<HTMLInputElement>(null)
    var loading by useState(false)
    var error by useState<String?>(null)

    fun handleCreate() {
        val nameValue = nameRef.current?.value ?: ""
        val expiresAtValue = expiresAtRef.current?.value ?: ""

        if (nameValue.isBlank()) {
            error = "Name is required"
            return
        }

        loading = true
        error = null
        scope.launch {
            try {
                ApiClient.createClient(
                    CreateClientRequest(
                        name = nameValue,
                        expiresAt = expiresAtValue.takeIf { it.isNotBlank() }?.let { "${it}T00:00:00Z" },
                    )
                )
                props.onCreated()
            } catch (e: Exception) {
                error = e.message
                loading = false
            }
        }
    }

    Dialog {
        open = true
        onClose = { _, _ -> props.onClose() }

        DialogTitle {
            +"Add New Client"
        }

        DialogContent {
            sx {
                minWidth = 400.px
                paddingTop = 16.px
            }

            form {
                onSubmit = { e ->
                    e.preventDefault()
                    handleCreate()
                }

                TextField {
                    fullWidth = true
                    label = react.ReactNode("Client Name")
                    name = "clientName"
                    variant = FormControlVariant.outlined
                    autoFocus = true
                    asDynamic().inputRef = nameRef
                    sx {
                        marginTop = 8.px
                        marginBottom = 16.px
                    }
                }

                TextField {
                    fullWidth = true
                    label = react.ReactNode("Expires At (optional)")
                    name = "expiresAt"
                    type = web.html.InputType.date
                    variant = FormControlVariant.outlined
                    asDynamic().inputRef = expiresAtRef
                    asDynamic().InputLabelProps = js.objects.jso<dynamic> {
                        shrink = true
                    }
                }
            }

            error?.let { err ->
                mui.material.Typography {
                    sx {
                        color = "error.main".asDynamic()
                        marginTop = 16.px
                    }
                    +err
                }
            }
        }

        DialogActions {
            Button {
                onClick = { props.onClose() }
                +"Cancel"
            }

            Button {
                variant = ButtonVariant.contained
                disabled = loading
                onClick = { handleCreate() }
                +(if (loading) "Creating..." else "Create")
            }
        }
    }
}
