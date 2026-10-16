// Domain, data and the data DI modules, shared through Kotlin Multiplatform (doc/kmp-android-plan.md, Phase 3).
// The jvm() target is never shipped: it makes the compiler reject Android APIs in commonMain and runs the
// Room/DataStore tests on the host without a device.
plugins {
    id("org.jetbrains.kotlin.multiplatform") // no version: KGP is on the root buildscript classpath
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    android {
        namespace = "com.zobaer53.zedmovies.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = 26
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

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.paging)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io.core)
        }
        androidMain.dependencies {
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
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}

room {
    // Moved from app/schemas: 3.json must stay byte-for-byte identical (no accidental entity change).
    schemaDirectory("$projectDir/schemas")
}
