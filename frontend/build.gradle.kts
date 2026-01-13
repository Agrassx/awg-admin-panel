plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
            runTask {
                mainOutputFileName.set("frontend.js")
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                // Kotlin Wrappers (BOM for version alignment)
                implementation(project.dependencies.enforcedPlatform(libs.kotlin.wrappers.bom))
                implementation(libs.kotlin.react)
                implementation(libs.kotlin.react.dom)
                implementation(libs.kotlin.emotion)
                implementation(libs.kotlin.mui.material)
                implementation(libs.kotlin.mui.icons)

                // Kotlinx
                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.serialization)

                // Ktor Client
                implementation(libs.bundles.ktor.client)
            }
        }
    }
}
