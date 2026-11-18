// Compose Web shell (Phase 8): a Kotlin/Wasm browser app hosting the shared UI.
@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
plugins {
    id("org.jetbrains.kotlin.multiplatform") // no version: KGP is on the root buildscript classpath
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// The API key is inlined at build time like in the APK/desktop app. A production deployment should
// replace this with a proxy that adds the key server-side (see the migration plan's hosting section).
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
val webApiKey = (System.getenv("TMDB_API_KEY") ?: localKey ?: "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

val generateWebSecrets = tasks.register("generateWebSecrets") {
    val key = webApiKey
    val outputDir = layout.buildDirectory.dir("generated/sources/secrets/kotlin/wasmJsMain")
    inputs.property("apiKeyPresent", key.isNotEmpty())
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/zobaer53/zedmovies/webapp/WebSecrets.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package com.zobaer53.zedmovies.webapp\n\n" +
                "internal const val WEB_API_KEY: String = \"$key\"\n"
        )
    }
}

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        wasmJsMain {
            kotlin.srcDir(generateWebSecrets.map { it.outputs.files.singleFile })
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(libs.kotlinx.browser)
            }
        }
    }
}

tasks.named("compileKotlinWasmJs") { dependsOn(generateWebSecrets) }
