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

    // Security
    implementation(libs.bcrypt)

    // Logging
    implementation(libs.logback)

    // Testing
    testImplementation(kotlin("test"))
}

// Copy frontend build output to backend resources
tasks.processResources {
    dependsOn(":frontend:jsBrowserProductionWebpack")
    
    from(project(":frontend").layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable")) {
        into("static")
    }
    from(project(":frontend").layout.buildDirectory.dir("processedResources/js/main")) {
        into("static")
    }
}

tasks.jar {
    archiveBaseName.set("awg-admin")
    archiveVersion.set("")
    
    manifest {
        attributes["Main-Class"] = "org.awgadmin.ApplicationKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        // Exclude signature files from signed JARs to avoid SecurityException
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/MANIFEST.MF")
    }
}
