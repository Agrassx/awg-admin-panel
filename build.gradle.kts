plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt)
}

allprojects {
    group = "org.awgadmin"
    version = "1.0.0"
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    source.setFrom(
        "backend/src/main/kotlin",
        "frontend/src/jsMain/kotlin"
    )
}
