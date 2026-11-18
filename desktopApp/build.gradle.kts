// Compose Desktop shell (Phase 7a). It hosts the shared UI with the jvm target of :shared and
// opens playback in the system browser (no ad blocking inside the browser v1).
plugins {
    kotlin("jvm") // no version: KGP is on the root buildscript classpath
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// The TMDB key ships with the app, just like BuildConfig.API_KEY in the APK. Never commit it:
// local.properties is gitignored and CI uses the TMDB_API_KEY secret. Computed at configuration
// time so the task stays configuration-cache friendly.
val localPropertiesFile = rootProject.file("local.properties")
val localKey = if (localPropertiesFile.exists()) {
    localPropertiesFile.readLines()
        .firstOrNull { it.trimStart().startsWith("API_KEY=") }
        ?.substringAfter('=')
        ?.trim()
        ?.removeSurrounding("\"")
} else {
    null
}
val desktopApiKey = (System.getenv("TMDB_API_KEY") ?: localKey ?: "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

val generateDesktopSecrets = tasks.register("generateDesktopSecrets") {
    val key = desktopApiKey
    val outputDir = layout.buildDirectory.dir("generated/sources/secrets/kotlin/main")
    inputs.property("apiKeyPresent", key.isNotEmpty())
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/zobaer53/zedmovies/desktop/DesktopSecrets.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package com.zobaer53.zedmovies.desktop\n\n" +
                "internal const val DESKTOP_API_KEY: String = \"$key\"\n"
        )
    }
}

kotlin {
    jvmToolchain(17)
    sourceSets.main {
        kotlin.srcDir(generateDesktopSecrets.map { it.outputs.files.singleFile })
    }
}

tasks.named("compileKotlin") { dependsOn(generateDesktopSecrets) }

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.koin.core)
}

compose.desktop {
    application {
        mainClass = "com.zobaer53.zedmovies.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "NoFlix"
            packageVersion = "1.0.0"
            description = "Ad-free movies and TV series"
            vendor = "zeD"
        }
    }
}
