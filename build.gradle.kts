plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("multiplatform") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
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
