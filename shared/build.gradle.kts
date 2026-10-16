// Domain, data and the data DI modules, shared through Kotlin Multiplatform (doc/kmp-android-plan.md, Phase 3).
// The jvm() target is never shipped: it makes the compiler reject Android APIs in commonMain and runs the
// Room/DataStore tests on the host without a device.
plugins {
    id("org.jetbrains.kotlin.multiplatform") // no version: KGP is on the root buildscript classpath
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    // The design system and its resources (Phase 4, B5).
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    android {
        namespace = "com.zobaer53.zedmovies.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = 26
        // Compose resources ship as assets, and AGP's KMP library only packages assets with this on.
        androidResources { enable = true }
    }
    jvm()
    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            // In :shared's public API (domain models, use cases, the Koin modules), so :app sees them too.
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            api(libs.kotlinx.coroutines.core)
            api(libs.androidx.paging.common)
            api(libs.kotlinx.datetime)
            // Public API: :app's activity, WebView screens and screenshot tests use Compose directly.
            api(libs.jetbrains.compose.runtime)
            api(libs.jetbrains.compose.foundation)
            api(libs.jetbrains.compose.ui)
            api(libs.jetbrains.compose.material3)
            api(libs.jetbrains.compose.components.resources)
            api(libs.androidx.paging.compose) // LazyPagingItems in the shared components' signatures

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.paging)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io.core)
            implementation(libs.jetbrains.compose.material.ripple) // RippleAlpha for the custom ripple
            implementation(libs.coil.compose)
            // Coil's network fetcher; its Ktor engine is the OkHttp one already in androidMain/jvmMain.
            implementation(libs.coil.network.ktor3)
            // Screens (B6): navigation, ViewModels with SavedStateHandle, Koin injection.
            implementation(libs.jetbrains.androidx.navigation.compose)
            implementation(libs.jetbrains.androidx.lifecycle.runtime.compose)
            implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.androidx.lifecycle.viewmodel.savedstate)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.core.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core) // WindowCompat in SystemBarsAppearance's actual
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            // AndroidSQLiteDriver: Room on the platform SQLite (never the bundled one in the APK).
            implementation(libs.androidx.sqlite.framework)
            // Android's Context.preferencesDataStoreFile: the same file as before the move.
            implementation(libs.androidx.datastore.preferences)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        jvmTest.dependencies {
            implementation(libs.koin.test)
            implementation(libs.androidx.room.testing)
            implementation(libs.jetbrains.compose.ui.test)
            implementation(compose.desktop.currentOs) // skiko runtime for this host (macOS arm64, linux-x64 on CI)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}

compose.resources {
    packageOfResClass = "com.zobaer53.zedmovies.resources"
    publicResClass = false // all UI is in :shared since B6; :app's code and tests never use Res
}

room {
    // Moved from app/schemas: 3.json must stay byte-for-byte identical (no accidental entity change).
    schemaDirectory("$projectDir/schemas")
}
