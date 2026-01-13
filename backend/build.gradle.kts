plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("org.awgadmin.ApplicationKt")
}

dependencies {
    // Ktor Server
    implementation(libs.bundles.ktor.server)

    // Database
    implementation(libs.bundles.exposed)
    implementation(libs.sqlite.jdbc)

    // Kotlinx
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.datetime)

    // Logging
    implementation(libs.logback)

    // Testing
    testImplementation(kotlin("test"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.awgadmin.ApplicationKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
