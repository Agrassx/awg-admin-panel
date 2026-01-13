package org.awgadmin

import org.awgadmin.ui.App
import react.create
import react.dom.client.createRoot
import web.dom.document

fun main() {
    val container = document.getElementById("root")
        ?: error("Root element not found")

    createRoot(container).render(App.create())
}
