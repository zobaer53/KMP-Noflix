# NoFlix: iOS port plan (Phase 9)

_Audited and spiked 2026-11-14 against `origin/dev` at `6589851` (branch `feat/a-ios-migration`). This is the detailed plan for "Phase 9: iOS" of [kmp-migration-plan.md](kmp-migration-plan.md). [kmp-android-plan.md](kmp-android-plan.md) is the format model. Every library claim below was **checked by a spike**: iOS targets were added temporarily, `:shared` was compiled and linked for `iosSimulatorArm64`, the common tests ran on the simulator, and a throw-away XcodeGen app showed Home with live TMDB data on an iPhone 17 simulator. The spike was then reverted. Its findings are in "Spike results", and its verified code is in Appendix A._

## Goal

Ship NoFlix on iPhone as a **sideloaded/personal build with full in-app playback**, from the same `:shared` module that Android uses. At the end:

- `:shared` also targets `iosArm64` and `iosSimulatorArm64` and builds a static `Shared.framework`. Every `expect` has an iOS `actual`.
- `iosApp/` is a SwiftUI app shell (XcodeGen spec + generated project) that hosts the shared Compose UI (`MainViewController`), starts Koin with the TMDB key from a gitignored `Secrets.xcconfig`, and plays titles in a `WKWebView` player that injects the same `StreamPageStyle` scripts as Android.
- Android ships **exactly as it does today**: same APK contents (no new `.so`), same behavior, same screenshots.

### Non-goals

- No App Store or TestFlight build, no device signing (there is no Apple developer team). Simulator builds only. Device signing is the optional last step under "Open questions".
- No iPad layout, no landscape main UI (iPhone, portrait-locked; only the player rotates).
- No state restoration after iOS kills the app in the background (Android's process-death check has no iOS equivalent here).
- No full AdGuard filter conversion for WebKit (v2), no in-app YouTube player (v2), no `iosX64` (Intel simulators).
- No change to `:ad-filter`/`:adblock-client` (Android-only, JNI).

### Decisions (made, recorded here; don't re-ask)

| # | Decision | Reason |
| --- | --- | --- |
| D1 | **Distribution: sideload/personal build with full playback.** Not an App Store browse-only app. | App Review guideline **5.2** (intellectual property; 5.2.3 covers apps that stream third-party content without authorization) would reject in-app playback from a third-party streaming site. A browse-only store app would drop the app's main feature. |
| D2 | **Minimum iOS 16.0**, iPhone only (`TARGETED_DEVICE_FAMILY = 1`), main UI portrait-locked. | The player's rotation needs `UIWindowScene.requestGeometryUpdate` and `setNeedsUpdateOfSupportedInterfaceOrientations` (both iOS 16). The spike linked fine at 16.0 (one data-only linker warning, see Risks). |
| D3 | **Targets `iosArm64` + `iosSimulatorArm64` only.** No `iosX64`. | Apple silicon only: the Mac and GitHub's `macos-*` arm64 runners. `iosX64` would double native compile time for nothing. |
| D4 | **Static framework `Shared`** linked through `embedAndSignAppleFrameworkForXcode`. No CocoaPods, no SwiftPM export. | Verified in the spike: the Compose resources land in the app bundle (`compose-resources/`), and no pod setup is needed. |
| D5 | **Xcode project from XcodeGen** (`iosApp/project.yml`, XcodeGen 2.46.0 is installed). The generated `iosApp.xcodeproj` (with the shared scheme) is **committed too**. | It's reproducible without the Xcode GUI and diffable. Committing the output means `xcodebuild` and Xcode work without XcodeGen installed. The I4 CI job checks that it doesn't drift. |
| D6 | **iOS bundle id = `com.zobaer53.zedmovies`** (the Android application id), display name "NoFlix", `PRODUCT_NAME = NoFlix`. | The ids are per platform, so they can't collide. The Maestro flows (`appId: com.zobaer53.zedmovies`) then work on iOS unchanged. |
| D7 | **Player in Swift** (`WKWebView`, UIKit), **decisions in Kotlin common** (URL classification, navigation policy, the scripts), **trailers in Kotlin** (`UIApplication.openURL`). | WebKit delegates and orientation overrides are idiomatic and short in Swift, and Sonnet agents know them well. The decisions that need tests (which URL is allowed, which page is a watch page) are pure Kotlin, testable on the JVM and the iOS simulator. Trailers need no UI, so no Swift. |
| D8 | **`WKContentRuleList` in v1 as a small bundled list** (≤ 100 rules, third-party ad/pop-under hosts only). The full AdGuard conversion is deferred to v2. | Pop-up and navigation blocking (D9) stop redirects and new windows, but not in-page ad requests (banners and VAST calls inside the player iframe). WebKit has no `shouldInterceptRequest`: a content rule list is the only network-level block. A small list is about 30 lines of Swift, compiles once and is cached by identifier, and needs no CI tooling. Keeping it short and third-party-only keeps the risk of breaking the player low. The AdGuard conversion (SafariConverterLib, its license, the 150k-rule cap, a CI conversion step) is real work, so it's v2. |
| D9 | **Pop-ups and navigation:** `createWebViewWith` returns `nil`, `javaScriptCanOpenWindowsAutomatically = false`, top-level navigations off the stream site are cancelled, non-web schemes (`itms-apps:`, `intent:`, …) are cancelled in every frame. Landscape on `/watch/`. | This is what the brief asked for, and it catches most visible ads with little code. |
| D10 | **Trailers v1: the YouTube app, else Safari:** `openURL(https://www.youtube.com/watch?v=<key>, [.universalLinksOnly: true])`, then a plain `openURL` if that fails. In-app IFrame (`loadHTMLString` with a base URL) is v2, optional. | Needs no `LSApplicationQueriesSchemes` and no `canOpenURL`. On the simulator (no YouTube app) it opens Safari. |
| D11 | **API key:** `local.properties` `API_KEY` → `scripts/ios-secrets.sh` → gitignored `iosApp/Secrets.xcconfig` (`TMDB_API_KEY = …`) → `#include?` from the committed `iosApp/Config.xcconfig` → `Info.plist` `TMDBApiKey = $(TMDB_API_KEY)` → Swift reads it → `IosKoinKt.startKoinIos(apiKey:)` → `zedMoviesApiKeyProvider`. CI writes the file from the `TMDB_API_KEY` secret. | `#include?` (optional include) lets a fresh checkout build without the file. The key is then empty and TMDB returns 401, which the app shows as an error state (once fix F2 is in, see below). |
| D12 | **Room DB and DataStore in the app's Documents directory:** `Documents/zedMovies.db`, `Documents/preferences.preferences_pb`. No `UIFileSharingEnabled`, so the files are invisible in the Files app. | Room's KMP sample uses Documents. The files are backed up, so the wish list survives a device restore. Same file names as Android. |
| D13 | **Move the stream constants and scripts to `commonMain`** (`ui/playback/StreamPage.kt`). `:app` keeps its `android.net.Uri` helpers and only changes imports. | The scripts break whenever the site's DOM changes. With one copy, one fix covers both platforms. A Robolectric test in `:app` proves the Android script stays byte-identical. |
| D14 | **Android unchanged.** No behavior change, no new native library in the APK (`sqlite-bundled` only in `iosMain`), `verifyRoborazziDebug` unchanged. | Brief requirement. The three common fixes (F1–F3) don't change Android's runtime behavior (see each). |
| D15 | **Integration on the local branch `feat/a-ios-migration`**, with **one PR into `dev` at the end** (after the Opus test pass), auto-merge as usual. Packages merge locally with `git merge --no-ff`. | That's one CI run and one review instead of five. The packages are designed to merge without conflicts (disjoint files). |

### Success criteria

All checks run on the simulator **`NoFlix iPhone 17`** (iPhone 17, iOS 26.5, UDID `2E6CF691-424D-4045-B5AF-DEB4C78D2C9F`, created for this project during the spike; the machine has no plain "iPhone 17" device, only devices named for other projects). Use `name=NoFlix iPhone 17` or the UDID, never another project's simulator.

| # | Check | How | Pass |
| --- | --- | --- | --- |
| S1 | App builds | `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=NoFlix iPhone 17' -derivedDataPath iosApp/build build` | `BUILD SUCCEEDED` |
| S2 | Launches, Home loads TMDB data | `xcrun simctl install` + `launch --console-pty`, screenshot after 20 s | "Upcoming Movies" pager and "Top Rated" rows with posters, no `Uncaught Kotlin exception` in the console |
| S3 | See-all paging | Home → "See all" → scroll down 3 screens | More pages load (the list grows past 20 items), no duplicates at page joins, back button returns Home |
| S4 | Search + filters | Search "Batman", switch tabs; open filters, set rating and year, apply; "More filters" loads TMDB choices; "Clear all" | Results change with each filter; chips show localized numbers; sheet's Cancel label shown |
| S5 | Details | Open a movie and a TV show | Backdrop, rating, genres, cast; Trailer chip when TMDB has one |
| S6 | Wish list persisted | Add 1 movie + 1 TV show, `simctl terminate`, relaunch | Both still in the wish list |
| S7 | Content language persisted | First launch with `-AppleLanguages "(de)"` (fresh install), then relaunch without it | A details page never opened before loads in German both times |
| S8 | Trailer opens | Details → Trailer | Safari opens `youtube.com/watch?v=…` (no YouTube app on the simulator) |
| S9 | Playback opens, pop-ups blocked | Details → Play | Full-screen web player, landscape on `/watch/`; `NoFlixPlayer:` log lines show blocked pop-ups/navigations; the app stays in the player (never ends up in Safari or the App Store); close returns portrait details |
| S10 | Common tests on iOS | `./gradlew :shared:iosSimulatorArm64Test` | All pass (45 existing + the new ones) |
| S11 | Android regression | `:shared:jvmTest :app:testDebugUnitTest :app:assembleDebug`, `scripts/kmp-guard.sh`, `:app:verifyRoborazziDebug` | All green; `unzip -l app-debug.apk '*.so'` list identical to `dev` |
| S12 | Android device (optional) | `scripts/stream.sh with-device -- .maestro/run.sh` on the Pixel 6a, only if it's free | 7/7 flows |

## Spike results (2026-11-14)

The commands ran under the build lock. Timings are on this Mac (M-series, 10 cores, 16 GB):

| Step | Result |
| --- | --- |
| Add `iosArm64()`/`iosSimulatorArm64()` + static framework + `ktor-client-darwin` 3.6.0 + `sqlite-bundled` 2.6.2 in `iosMain` + `kspIos*` Room | **All dependencies resolve at the pinned versions. No version bump is needed.** Kotlin/Native 2.4.21 was already in `~/.konan`. |
| `:shared:compileKotlinIosSimulatorArm64` | First run 3 min 25 s (incl. downloads + KSP). **One commonMain error:** `GenreMapper.kt:12/15 Unresolved reference 'JvmName'` → fix F1 |
| `:shared:linkDebugFrameworkIosSimulatorArm64` | 1 min 44 s, `Shared.framework` 291 MB (debug), header 25,490 lines; `shared/schemas/3.json` unchanged |
| Swift names in the header | `MainViewControllerKt.MainViewController(host:)`, `IosKoinKt.startKoinIos(apiKey:)`, protocol `IosPlaybackHost`; a Kotlin `fun presentPlayer(url: String, title: String)` became `presentPlayer(url:title:)` |
| XcodeGen app + `embedAndSignAppleFrameworkForXcode` | Builds in ~11 s with an up-to-date framework. App 88 MB (debug). `compose-resources/` is in the bundle. Linker warning: `libicu.icudtl_dat.o` built for iOS-simulator 18.5 (ICU **data** object; see Risks). |
| First launch | **Crash:** `ClassCastException: ZedMoviesResult.Failure.HttpError cannot be cast to ZedMoviesResult.Success` in `MovieRepositoryImpl … networkBoundResource` → fix F2 (the trigger was an empty key, see F4) |
| Second launch | **Crash:** `SerializationException: Serializer for class 'Genre' is not found` (Room `ListConverter`) → fix F3 |
| Third launch | **Home shows live TMDB data**: pager, "29 July, 2026" (NSDateFormatter), ratings "9.1" (NSNumberFormatter), posters (Coil + Ktor Darwin), Montserrat font, bottom bar above the home indicator. Room wrote and re-read the lists; DataStore served the content language. |
| `:shared:iosSimulatorArm64Test` | **Compile error** on 4 test names: `Name contains illegal characters: ","` → fix F4. After the rename, **45/45 tests pass** on the simulator. |
| `:shared:jvmTest` + Android compile with F1–F3 | Green |

### Fixes found by the spike (all in I0)

| Id | File | Change | Android impact |
| --- | --- | --- | --- |
| F1 | `shared/src/commonMain/…/data/mapper/GenreMapper.kt` | Add `import kotlin.jvm.JvmName` (on Native the annotation isn't auto-imported) | None (same annotation) |
| F2 | `shared/src/commonMain/…/data/common/result/Operators.kt` | In `isLoading`/`isSuccess`/`isFailure`: `returns() implies …` → **`returns(true) implies …`**. The old contract told the compiler that the type check holds even when the function returns `false`. In `networkBoundResource`, after `response.isSuccess()` returned `false`, the `isFailure()` branch smart-cast `response` to `Success & Failure`. Kotlin/Native emits a downcast to `Success` and crashes on every network failure (offline, 401, 5xx). The JVM only emits the cast it needs, so Android survives by luck. | None at runtime (same bytecode casts on the JVM). It removes a latent wrong smart cast. |
| F3 | `shared/src/commonMain/…/data/database/model/common/Genre.kt` | Add `@Serializable` to `enum class Genre`. On the JVM, kotlinx-serialization builds an enum serializer by reflection; Native can't. | None: an enum without `@SerialName` encodes by `name`, which is what the JVM fallback wrote, so stored rows (`["ACTION",…]`) stay readable. A `ListConverterTest` pins the format (I1). |
| F4 | `shared/src/commonTest/…/FilterOptionsMapperTest.kt:24`, `SearchFiltersMapperTest.kt:115,151`, `MovieServiceTest.kt:33` | Remove the commas from the backtick test names (Native rejects `, . : ; / < > [ ]` in names). New names: `certifications of the region mildest first`, `runtime origin country and excluded genres apply to both types`, `TV network status and type apply to TV only`, `discover sends the filters language page and api key` | Test names only |

Also found: `local.properties` writes the key as ` API_KEY =<key>` (a leading space and a space before `=`). Java `Properties` trims both, so a `grep '^API_KEY='` finds nothing. `scripts/ios-secrets.sh` must parse like `Properties`: `sed -nE 's/^[[:space:]]*API_KEY[[:space:]]*[=:][[:space:]]*(.*[^[:space:]])[[:space:]]*$/\1/p'`.

## Audit: what is in the way

### `expect` declarations that need an iOS `actual`

| `expect` (commonMain) | Android `actual` | iOS `actual` (file in `shared/src/iosMain/kotlin/com/zobaer53/zedmovies/…`) | Notes |
| --- | --- | --- | --- |
| `data/database/ZedMoviesDatabase.kt:63` `expect object zedMoviesDatabaseConstructor` | KSP-generated | KSP-generated by `kspIosArm64`/`kspIosSimulatorArm64` | Verified |
| `data/network/util/HttpClient.kt:34` `internal expect fun httpEngine()` | `OkHttp.create()` | `data/network/util/HttpEngine.ios.kt`: `Darwin.create()` | Needs `ktor-client-darwin` |
| `di/KoinModules.kt:123` `expect val platformModule` | Room on `getDatabasePath`, `AndroidSQLiteDriver`, `preferencesDataStoreFile` | `di/PlatformModule.ios.kt`: `Room.databaseBuilder<zedMoviesDatabase>(name = "<Documents>/zedMovies.db").setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)`; `PreferenceDataStoreFactory.createWithPath { "<Documents>/preferences.preferences_pb".toPath() }` (okio comes in transitively); `IosDeviceLocaleProvider`, `IosRegionProvider` | `Dispatchers.IO` needs `import kotlinx.coroutines.IO` on Native |
| `ui/SystemBarsAppearance.kt:19` `@Composable expect fun rememberSystemBarsAppearance()` | `WindowCompat` insets controller | `ui/SystemBarsAppearance.ios.kt`: picks `UIStatusBarStyleDarkContent`/`LightContent` from the luminance, served through `ComposeUIViewControllerDelegate.preferredStatusBarStyle` + `setNeedsStatusBarAppearanceUpdate()` | CMP 1.12.2's `ui-uikit` klib has `ComposeUIViewControllerConfiguration`, `ComposeUIViewControllerDelegate`, `preferredStatusBarStyle` and `LocalUIViewController` (checked in the klib). If the delegate is deprecated, use its replacement. A no-op fallback is acceptable: the `.default` style follows the system theme, as `zedMoviesTheme` does. |
| `ui/smallcomponent/mapper/ReleaseDateMapper.kt:15` `internal expect fun LocalDate.formatLocalized(pattern)` | `java.time` `DateTimeFormatter.ofPattern` | `NSDateFormatter`, `dateFormat = pattern`, `locale = currentLocale`, **Gregorian calendar** for both `dateFromComponents` and the formatter (Java always formats in ISO; with the user's calendar set to Japanese or Buddhist, the current calendar would shift the year) | Pattern `d MMMM, yyyy` means the same in LDML and `java.time` |
| `ui/util/PlatformUi.kt:9` `internal expect fun Modifier.testTagsAsResourceId()` | semantics `testTagsAsResourceId = true` | `= this` | CMP on iOS exposes `testTag` as the accessibility identifier, which Maestro's `id:` uses |
| `ui/util/PlatformUi.kt:13` `@Composable internal expect fun cancelLabel()` | `android.R.string.cancel` | UIKit's own localized string: `NSBundle.bundleWithIdentifier("com.apple.UIKit")?.localizedStringForKey("Cancel", "Cancel", null) ?: "Cancel"` | |
| `ui/util/PlatformFormat.kt:13` `isoLanguageCodes()` | `Locale.getISOLanguages()` | `NSLocale.ISOLanguageCodes` **filtered to 2 letters** (iOS also lists 3-letter ISO 639-2 codes) | The caller sorts by display name (`FiltersSheet.kt:430`) |
| `:16` `isoCountryCodes()` | `Locale.getISOCountries()` | `NSLocale.ISOCountryCodes` | |
| `:19` `displayLanguageName(code)` | `getDisplayLanguage(default).capitalized()` | `currentLocale.localizedStringForLanguageCode(code) ?: code`, then `capitalized()` | |
| `:22` `displayCountryName(code)` | `getDisplayCountry`, else the code | `currentLocale.localizedStringForCountryCode(code) ?: code` | |
| `:25` `String.capitalized()` | `titlecase(Locale.getDefault())` | First character uppercased with `NSString.uppercaseStringWithLocale(currentLocale)` (Turkish i → İ like Android) | |
| `:27` `formatRating(Double)` | `String.format(locale, "%.1f")` (HALF_UP) | `NSNumberFormatter`, 1 fraction digit, no grouping, **`roundingMode = NSNumberFormatterRoundHalfUp`** (the default is half-even: 7.25 would give 7.2, while Android gives 7.3) | |
| `:29` `formatCount(Int)` | `NumberFormat.getIntegerInstance()` | `NSNumberFormatter` decimal style, 0 fraction digits | |
| `:35` `formatInteger(Int)` | `String.format(locale, "%d")` (localized digits, no grouping) | `NSNumberFormatter`, `usesGroupingSeparator = false` | |

Non-`expect` platform interfaces: `data/platform/DeviceLocaleProvider` (Android: the configuration's first locale) and `RegionProvider` (`Locale.getDefault().country`). iOS: `NSLocale.preferredLanguages.first` → `NSLocale(localeIdentifier = it).languageCode`. Use the **system** list, not `NSBundle.preferredLocalizations`, which only knows the app's own localizations ("en"). Region: `NSLocale.currentLocale.countryCode ?: ""`. Hebrew comes out as `he` on iOS, while Android reports `iw`. TMDB accepts both.

### Dependencies (pinned in `gradle/libs.versions.toml`) on iOS

All resolved for `iosSimulatorArm64` in the spike (`kmpPartiallyResolvedDependenciesChecker` passed, the framework linked). **No bumps.**

| Library | Version | iOS | Action |
| --- | --- | --- | --- |
| Kotlin / K/N | 2.4.21 | ✅ (prebuilt in `~/.konan`) | — |
| Compose Multiplatform (runtime, foundation, ui, resources, ripple) / material3 | 1.12.2 / 1.9.0 | ✅ | — |
| JetBrains navigation-compose / lifecycle | 2.10.0-rc01 / 2.11.0 | ✅ | — |
| Koin BOM (core, compose, compose-viewmodel, core-viewmodel) | 4.2.2 | ✅ (`koin-android` stays in androidMain) | — |
| Ktor core, content-negotiation, serialization-json | 3.6.0 | ✅ | — |
| **Ktor Darwin engine** | 3.6.0 | ✅ | **new catalog entry** `ktor-client-darwin`, in `iosMain` |
| Room runtime + room-paging + KSP compiler | 2.8.5 | ✅ | `add("kspIosArm64", …)`, `add("kspIosSimulatorArm64", …)` |
| **androidx.sqlite `sqlite-bundled`** | 2.6.2 | ✅ (static, in the klib; no `-lsqlite3`) | **`iosMain` only.** It must never reach `androidMain` (16 KB rule) |
| DataStore preferences-core | 1.2.1 | ✅ (`createWithPath`, okio transitive) | — |
| Paging common / paging-compose | 3.5.3 | ✅ | — |
| kotlinx coroutines / serialization / datetime / io | 1.11.0 / 1.11.0 / 0.8.0 / 0.9.1 | ✅ (`toNSDateComponents()` available) | — |
| Coil compose + network-ktor3 | 3.6.3 | ✅ (uses the Darwin engine found on the classpath; posters loaded) | — |
| `ktor-client-okhttp`, `koin-android`, `androidx.core`, `sqlite-framework`, `datastore-preferences` | — | Android/JVM only | stay in androidMain/jvmMain |

### commonMain code that compiles on the JVM but not on Native

Grep of `shared/src/commonMain` for `java.`, `synchronized`, `@Volatile`, `String.format`, `::class.java`, `@Jvm*`, `System.`, `Thread`, `ConcurrentHashMap`, `Dispatchers.IO`, `runBlocking` found only:

- `@JvmName` ×2 in `GenreMapper.kt` (F1). No other `@Jvm*`, no `synchronized`, no `String.format`, no reflection, no `Dispatchers.IO` in commonMain (`Dispatchers.Main.immediate` in `SearchViewModel.kt:159` is fine on iOS).
- Runtime-only problems that the compiler can't see: F2 (contracts) and F3 (reflective enum serializer). Other `Json` uses are on `@Serializable` classes (`Credits`, `SearchFilters`) or on `List<Int>`/`List<String>`. Fine.
- DAOs: every method is `suspend`, `Flow` or `PagingSource`, so none is blocking (which Native wouldn't allow).

### `:app` code iOS needs (D13)

`app/src/main/java/com/zobaer53/zedmovies/ui/webview/StreamUrl.kt` (`STREAM_BASE_URL`, `buildStreamUrl`) and `StreamPageStyle.kt` (`STREAM_DOCUMENT_START_SCRIPT`, `STREAM_PAGE_CSS`, `STREAM_PAGE_SCRIPT`, plus `isStreamSiteUrl`/`isStreamWatchUrl` on `android.net.Uri`). The script embeds the CSS with `org.json.JSONObject.quote`. Common code uses `kotlinx.serialization.json.JsonPrimitive(css).toString()`, which gives the same output for printable ASCII without `<` (`JSONObject.quote` additionally escapes `</` and some Unicode ranges). The CSS meets that today. A test pins it, and the Robolectric test pins byte equality on Android.

### Other items

| Item | Finding |
| --- | --- |
| Ktor Darwin | Works (spike). `NSURLSession`. ATS: TMDB, `image.tmdb.org` and the stream site are HTTPS, so no ATS exception is needed. |
| Coil | Works with `coil-network-ktor3` + Darwin. No code change. |
| Safe area | `zedMoviesApp` already uses `WindowInsets.safeDrawing` and the screens apply insets. The host must lay the Compose view out full-screen (`.ignoresSafeArea()`). The spike showed the bottom bar above the home indicator and the content below the Dynamic Island. |
| `CADisableMinimumFrameDurationOnPhone` | CMP's strict plist check requires it in `Info.plist` (`true`) |
| Back navigation | Details and List have on-screen back buttons (`ui/smallcomponent/BackButton.kt`). Tabs use the bottom bar. iOS has no system back. CMP's edge-swipe back is a bonus. |
| KMP guard | `scripts/kmp-guard.sh` only checks `import android/java/javax` in commonMain. iOS adds a new trap: test names with `, . : ; / < > [ ]` (F4). I4 extends the guard so the Linux CI job catches it too. |
| Linux CI host | `pr-checks.yml` (ubuntu) will configure `:shared` with iOS targets it can't build. KGP disables them on Linux, and `:shared:jvmTest`/`:app:*` don't depend on them. `kotlin.native.ignoreDisabledTargets=true` silences the warning. **Unverified on Linux locally**: see Risks R6. |

## Target layout

### Gradle (I0)

`shared/build.gradle.kts` (additions only):

```kotlin
kotlin {
    android { … }          // unchanged
    jvm()
    // iOS (doc/kmp-ios-plan.md): Apple silicon only, no iosX64. Static framework for embedAndSignAppleFrameworkForXcode.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    jvmToolchain(17)
    sourceSets {
        …
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.androidx.sqlite.bundled) // Room's driver on iOS (no system SQLite driver for KMP)
        }
        …
    }
}
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
```

Catalog: `ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }` between `ktor-client-core` and `ktor-client-mock`. Change the comment above `androidx-sqlite-bundled` to "JVM tests and iOS only. Never in the APK …" (comment line only). `gradle.properties`: add `kotlin.native.ignoreDisabledTargets=true` with a one-line comment (Linux CI).

### `shared/src/iosMain` (end state)

```
shared/src/iosMain/kotlin/com/zobaer53/zedmovies/
  data/network/util/HttpEngine.ios.kt          Darwin engine                                   (I0, final)
  data/platform/IosPlatformProviders.kt        IosDeviceLocaleProvider, IosRegionProvider       (I0 → I1)
  di/PlatformModule.ios.kt                     Room + DataStore in Documents, providers        (I0 → I1)
  ui/SystemBarsAppearance.ios.kt               status bar style + configureStatusBar() hook   (I0 stub → I1)
  ui/smallcomponent/mapper/ReleaseDateMapper.ios.kt                                            (I0 → I1)
  ui/util/PlatformFormat.ios.kt                                                                (I0 → I1)
  ui/util/PlatformUi.ios.kt                    testTagsAsResourceId = this, cancelLabel        (I0 → I1)
  ios/IosPlaybackHost.kt                       the Swift seam (interface + request)            (I0, frozen contract)
  ios/IosKoin.kt                               startKoinIos(apiKey)                            (I0, final)
  ios/MainViewController.kt                    MainViewController(host)                        (I0, final)
  ios/IosMediaLauncher.kt                      MediaLauncher for iOS                           (I0 stub → I3)
shared/src/iosTest/kotlin/com/zobaer53/zedmovies/…                                            (I1, I3: separate files)
```

"I0 → I1" means I0 lands the spike's working version (Appendix A) and I1 refines it. Only one package edits a file after I0.

### `iosApp/` (I2, except `Playback/`)

```
iosApp/
  project.yml                         XcodeGen spec (source of truth)
  iosApp.xcodeproj/                   generated + committed (incl. xcshareddata/xcschemes/iosApp.xcscheme)
  Config.xcconfig                     committed: TMDB_API_KEY = (empty default) then #include? "Secrets.xcconfig"
  Secrets.xcconfig                    gitignored, written by scripts/ios-secrets.sh
  iosApp/
    NoFlixApp.swift                   @main SwiftUI App, @UIApplicationDelegateAdaptor, starts Koin
    AppDelegate.swift                 supportedInterfaceOrientationsFor → OrientationLock.mask
    ComposeRootView.swift             UIViewControllerRepresentable → MainViewControllerKt.MainViewController(host:)
    Info.plist                        generated by XcodeGen from project.yml `info.properties`
    Assets.xcassets/                  AppIcon.appiconset (1024 px, opaque), LaunchBackground.colorset, LaunchLogo.imageset
    Playback/                         I0 stubs, then owned by I3
      PlayerPresenter.swift           IosPlaybackHost implementation (I0 stub)
      OrientationLock.swift           orientation mask + geometry update (I0, frozen API)
      PlayerViewController.swift      WKWebView player (I3)
      BlockRules.json                 WKContentRuleList source (I3)
```

`project.yml` essentials (verified in the spike unless marked):

- `options.deploymentTarget.iOS: "16.0"`. `configFiles: {Debug: Config.xcconfig, Release: Config.xcconfig}`.
- Target `iosApp`, `type: application`, `platform: iOS`, `sources: [iosApp]` (whole folder, so I3's new files need **no** `project.yml` edit).
- Settings: `PRODUCT_NAME: NoFlix`, `PRODUCT_BUNDLE_IDENTIFIER: com.zobaer53.zedmovies`, `TARGETED_DEVICE_FAMILY: "1"`, `SWIFT_VERSION: "5.0"` (Swift 6 strict concurrency would reject calls into the non-`Sendable` Kotlin types), `ENABLE_USER_SCRIPT_SANDBOXING: NO` (the Gradle phase writes outside the sandbox), `FRAMEWORK_SEARCH_PATHS: $(inherited) $(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`, `OTHER_LDFLAGS: $(inherited) -framework Shared`, `CODE_SIGNING_ALLOWED: NO` (simulator only, no team), `GENERATE_INFOPLIST_FILE: NO`, `ASSETCATALOG_COMPILER_APPICON_NAME: AppIcon`.
- `preBuildScripts`: name "Compile Kotlin framework", `basedOnDependencyAnalysis: false`, script `cd "$SRCROOT/.." && ./gradlew :shared:embedAndSignAppleFrameworkForXcode`. **Never** put `build-lock.sh` inside this phase: the outer `xcodebuild` already holds the lock, and the lock isn't re-entrant.
- `info.properties`: `CFBundleDisplayName: NoFlix`, `CADisableMinimumFrameDurationOnPhone: true`, `TMDBApiKey: $(TMDB_API_KEY)`, `UILaunchScreen: {UIColorName: LaunchBackground, UIImageName: LaunchLogo}`, `UISupportedInterfaceOrientations: [Portrait, LandscapeLeft, LandscapeRight]` (all three: the app delegate narrows it at runtime), `UIRequiresFullScreen: true`, `UIApplicationSceneManifest: {UIApplicationSupportsMultipleScenes: false}`, `ITSAppUsesNonExemptEncryption: false`.
- `schemes.iosApp`: build target `iosApp`, run/test/archive Debug/Release (generates the shared scheme).

App icon: I2 adds to `tools/branding/generate.py` the line `render.full(1024).convert("RGB").save(f"{ROOT}/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png", optimize=True)` (opaque, square: iOS applies its own mask), plus `LaunchLogo` (`render.masked(240, 0.22)` at @1x/@2x/@3x, or one 720 px PNG marked `universal`). `LaunchBackground` = the Android splash background color from `app/src/main/res/values*/` (look it up, don't guess). Run `generate.py` as its docstring says and commit the PNGs.

## Seams (exact contracts; I0 writes them, nobody changes their signatures)

### Kotlin → Swift: playback host

`shared/src/iosMain/kotlin/com/zobaer53/zedmovies/ios/IosPlaybackHost.kt` (I0):

```kotlin
package com.zobaer53.zedmovies.ios

/** Implemented in Swift by iosApp/iosApp/Playback/PlayerPresenter.swift. Called on the main thread. */
interface IosPlaybackHost {
    /** Presents the full-screen web player for [request] over the current screen. */
    fun presentPlayer(request: IosPlayerRequest)
}

/** Everything the Swift player needs. IosMediaLauncher builds it from a PlayRequest. */
class IosPlayerRequest(
    /** buildStreamUrl(tmdbId, type). */
    val url: String,
    /** "<name> <year>" for the player's accessibility label. */
    val title: String,
    /** STREAM_DOCUMENT_START_SCRIPT: WKUserScript at .atDocumentStart, main frame only. */
    val documentStartScript: String,
    /** STREAM_PAGE_SCRIPT: WKUserScript at .atDocumentEnd, main frame only, and re-run on didFinish (idempotent). */
    val pageScript: String,
)
```

Swift sees `protocol IosPlaybackHost { func presentPlayer(request: IosPlayerRequest) }` and `IosPlayerRequest(url:title:documentStartScript:pageScript:)` with `String` properties.

### Kotlin entry points called from Swift (I0)

```kotlin
// ios/MainViewController.kt   → Swift: MainViewControllerKt.MainViewController(host:)
fun MainViewController(host: IosPlaybackHost): UIViewController =
    ComposeUIViewController(configure = { configureStatusBar() }) {
        val launcher = remember(host) { IosMediaLauncher(host) }
        CompositionLocalProvider(LocalMediaLauncher provides launcher) { zedMoviesApp() }
    }

// ios/IosKoin.kt              → Swift: IosKoinKt.startKoinIos(apiKey:)
// Starts Koin once with the API key, viewModelModule and sharedModules (as zedMoviesApplication does on Android).
fun startKoinIos(apiKey: String)
```

`configureStatusBar()` is `internal fun ComposeUIViewControllerConfiguration.configureStatusBar()` in `ui/SystemBarsAppearance.ios.kt`: a no-op in I0, implemented in I1. `IosMediaLauncher` is `internal class IosMediaLauncher(private val host: IosPlaybackHost) : MediaLauncher` in `ios/IosMediaLauncher.kt`: in I0 `play` does nothing and `playTrailer` does nothing, and I3 implements both. (Don't name the entry `initKoin`: Objective-C export turns names starting with `init` into `doInit…`.)

### Swift stubs that I2 compiles against (I0 writes, I3 replaces the body)

`iosApp/iosApp/Playback/PlayerPresenter.swift`:

```swift
import UIKit
import Shared

/// The app's one IosPlaybackHost: presents the web player. App code creates it with PlayerPresenter() and
/// passes it to MainViewControllerKt.MainViewController(host:). I3 implements it; keep this signature.
final class PlayerPresenter: IosPlaybackHost {
    init() {}
    func presentPlayer(request: IosPlayerRequest) {
        NSLog("NoFlixPlayer: presentPlayer %@ (stub)", request.url)
    }
}
```

`iosApp/iosApp/Playback/OrientationLock.swift` (final in I0; I3 may add helpers but must keep these members):

```swift
import UIKit

/// The orientations the app allows right now. AppDelegate returns `mask` from
/// application(_:supportedInterfaceOrientationsFor:). Portrait everywhere except the player.
enum OrientationLock {
    static var mask: UIInterfaceOrientationMask = .portrait

    /// Sets `mask` and rotates now (iOS 16 API).
    static func apply(_ newMask: UIInterfaceOrientationMask, from viewController: UIViewController) {
        mask = newMask
        viewController.setNeedsUpdateOfSupportedInterfaceOrientations()
        viewController.view.window?.windowScene?.requestGeometryUpdate(.iOS(interfaceOrientations: newMask)) { error in
            NSLog("NoFlixPlayer: geometry update failed: %@", error.localizedDescription)
        }
    }
}
```

I2's `AppDelegate`: `func application(_:supportedInterfaceOrientationsFor:) -> UIInterfaceOrientationMask { OrientationLock.mask }`. I2's `ComposeRootView` holds one `PlayerPresenter()` and passes it to `MainViewController(host:)`.

### Common playback API (I3, `shared/src/commonMain/kotlin/com/zobaer53/zedmovies/ui/playback/StreamPage.kt`)

These names are used by `:app` (imports), by `IosMediaLauncher` and by Swift (`StreamPageKt.…`):

```kotlin
package com.zobaer53.zedmovies.ui.playback

const val STREAM_BASE_URL = "https://…"                       // moved verbatim from :app StreamUrl.kt
fun buildStreamUrl(tmdbId: Int, type: String): String           // moved verbatim
const val STREAM_DOCUMENT_START_SCRIPT = "…"                    // moved verbatim
const val STREAM_PAGE_CSS = """…"""                             // moved verbatim, now public (parity test)
val STREAM_PAGE_SCRIPT: String                                  // same text; CSS quoted with JsonPrimitive(STREAM_PAGE_CSS).toString()

/** True if [url] is on the stream site (its host or a subdomain, dot-bounded). */
fun isStreamSitePage(url: String?): Boolean
/** True if [url] is a stream-site page whose path starts with /watch/ (the landscape player page). */
fun isStreamWatchPage(url: String?): Boolean
/**
 * The WKWebView navigation policy. Main frame (or a new window: pass isMainFrame = true): allow only http(s)
 * on the stream site, plus about:blank. Sub-frames (the cross-origin player iframe): allow http, https,
 * about, data, blob. Cancel every other scheme (itms-apps:, intent:, market:, custom app schemes) everywhere.
 */
fun isStreamNavigationAllowed(url: String?, isMainFrame: Boolean): Boolean
/** https://www.youtube.com/watch?v=<url-encoded key>. */
fun youtubeWatchUrl(youtubeKey: String): String
```

The URL parsing uses Ktor's `parseUrl` (already in commonMain). The Android `isStreamSiteUrl`/`isStreamWatchUrl` stay in `:app` on `android.net.Uri` (D14: no Android behavior change). The names differ on purpose: no ambiguity in `:app`'s package.

## Playback architecture on iOS

```
Compose Play button ─► LocalMediaLauncher (IosMediaLauncher, Kotlin)
    play(PlayRequest) ─► IosPlayerRequest(buildStreamUrl, scripts) ─► IosPlaybackHost.presentPlayer   [Swift]
         PlayerPresenter finds the top view controller of the key window scene and presents
         PlayerViewController (modalPresentationStyle = .fullScreen)
    playTrailer(key) ─► UIApplication.openURL(youtubeWatchUrl(key), universalLinksOnly) ─► else openURL (Safari)
```

`PlayerViewController` (I3, Swift):

- `WKWebViewConfiguration`: `allowsInlineMediaPlayback = true`, `mediaTypesRequiringUserActionForPlayback = []`, `preferences.javaScriptCanOpenWindowsAutomatically = false`, `preferences.isElementFullscreenEnabled = true` (iOS 16), the two `WKUserScript`s from the request (main frame only), and the compiled `WKContentRuleList` when available.
- Rule list: `WKContentRuleListStore.default().lookUpContentRuleList(forIdentifier: "noflix-block-v<N>")`, else `compileContentRuleList(forIdentifier:encodedContentRuleList:)` from the bundled `BlockRules.json`. Load the page only after lookup/compile finishes (async, ~ms when cached). If compile fails, log and load without it. The launch argument `-NoFlixDisableBlockRules YES` (`UserDefaults`) skips it for diagnosis. List content: `{"trigger": {"url-filter": "^https?://([^/]+\\.)?<ad-domain>[:/]", "load-type": ["third-party"]}, "action": {"type": "block"}}`, ≤ 100 entries. Seed the list from the Android debug log (`NoFlixAdBlock: blocked …` hosts on a watch page, via `scripts/stream.sh with-device` if the Pixel 6a is free), else from well-known ad and pop-under networks. **Never** list the stream site's host or the player iframe's host. Bump `<N>` on every edit.
- `WKUIDelegate.webView(_:createWebViewWith:for:windowFeatures:)` → `nil` (log `NoFlixPlayer: blocked popup <host>`). JS `alert`/`confirm`/`prompt` panels: complete immediately without UI (ad dialogs).
- `WKNavigationDelegate.decidePolicyFor navigationAction`: `StreamPageKt.isStreamNavigationAllowed(url:isMainFrame:)` with `isMainFrame = navigationAction.targetFrame?.isMainFrame ?? true`. Cancel → log `NoFlixPlayer: cancelled navigation <host>`.
- Orientation: KVO on `webView.url` (the site is a SPA: `pushState` changes don't trigger `didFinish`). If `isStreamWatchPage(url)`, `OrientationLock.apply(.landscape, from: self)`, else `.portrait`. On dismiss, `.portrait`. `supportedInterfaceOrientations` returns `OrientationLock.mask`; `prefersStatusBarHidden` and `prefersHomeIndicatorAutoHidden` are `true`.
- Re-run `pageScript` in `didFinish` (idempotent, as Android does in `onPageFinished`).
- Close: a small close button (top leading, inside the safe area, semi-transparent, accessibility id `player_close`), plus `allowsBackForwardNavigationGestures = true`. The close button always dismisses the player, and the edge swipe handles in-page history.
- Debug: `webView.isInspectable = true` in DEBUG (`if #available(iOS 16.4, *)`) for Safari Web Inspector.
- Logs use `NSLog("NoFlixPlayer: …")` so the tester can count them: `xcrun simctl spawn booted log stream --level debug --predicate 'eventMessage CONTAINS "NoFlixPlayer"'`.

## Work packages

### Overview

Wave 1 is **I0 alone** (small, mostly the verified spike code). It merges into `feat/a-ios-migration`. Wave 2 is **I1–I4 in parallel**, each in its own worktree branched from that merge commit. Wave 2 packages can merge in any order. The suggested order I1 → I3 → I2 → I4 lets the later packages run their checks on more integrated code.

| ID | Branch | Owns (may create/edit; nothing else) | Depends on | Scope |
| --- | --- | --- | --- | --- |
| **I0** | `feat/a-ios-i0-foundation` | `shared/build.gradle.kts`; `gradle/libs.versions.toml` (one new entry + one comment line); `gradle.properties` (one line); F1–F4 files; all of `shared/src/iosMain/**` (creates); `iosApp/iosApp/Playback/PlayerPresenter.swift` + `OrientationLock.swift` (creates); the I0 row in this plan | — | iOS targets, framework, KSP, Darwin + bundled SQLite, the four spike fixes, working actuals from Appendix A, the frozen seams, the Swift stubs |
| **I1** | `feat/a-ios-i1-actuals` | `shared/src/iosMain/kotlin/com/zobaer53/zedmovies/{data,di,ui}/**` (edit the I0 files; may add files there); `shared/src/iosTest/kotlin/com/zobaer53/zedmovies/{data,di,ui}/**` (creates); `shared/src/commonTest/kotlin/com/zobaer53/zedmovies/data/database/converter/ListConverterTest.kt` + `data/network/common/NetworkBoundResourceTest.kt` (creates); the I1 row | I0 | Locale-correct formatting, status bar, Cancel label, iOS tests (Room round trip, DataStore, Koin graph, formatting), tests for F2/F3 |
| **I2** | `feat/a-ios-i2-xcode` | `iosApp/**` **except** `iosApp/iosApp/Playback/**`; `scripts/ios-secrets.sh` (creates); `tools/branding/generate.py` (two additions) + the generated iOS PNGs; root `.gitignore` (iOS lines appended under a `# iOS` comment); the I2 row | I0 | XcodeGen spec + committed project + shared scheme, SwiftUI shell, Koin start, secrets chain, icon, launch screen, orientation delegate |
| **I3** | `feat/a-ios-i3-playback` | `shared/src/commonMain/kotlin/com/zobaer53/zedmovies/ui/playback/StreamPage.kt` (creates); `shared/src/commonTest/kotlin/com/zobaer53/zedmovies/ui/playback/StreamPageTest.kt` (creates); `shared/src/iosMain/kotlin/com/zobaer53/zedmovies/ios/IosMediaLauncher.kt` (replaces the stub); `shared/src/iosTest/kotlin/com/zobaer53/zedmovies/ios/IosMediaLauncherTest.kt` (creates); `iosApp/iosApp/Playback/**`; **exceptions in `:app`:** delete `app/src/main/java/com/zobaer53/zedmovies/ui/webview/StreamUrl.kt`, in `StreamPageStyle.kt` delete the moved constants and keep only the two `Uri` helpers (+ one import), import lines only in `WebView.kt`, `ContentBlockingWebViewClient.kt`, `WebViewActivity.kt`; create `app/src/test/java/com/zobaer53/zedmovies/ui/webview/StreamPageScriptParityTest.kt`; the I3 row | I0 | Shared scripts + policy, iOS launcher (play + trailers), the Swift WKWebView player, the content rule list |
| **I4** | `feat/a-ios-i4-ci-docs` | `.github/workflows/ios-checks.yml` (creates); `scripts/kmp-guard.sh`; `.maestro/ios/**` (creates); `CLAUDE.md` (an iOS section + the build commands, new lines only); `README.md` (one iOS paragraph); the I4 row + the Phase 9 progress line in `kmp-migration-plan.md` | I0 | macOS CI job, guard for Native-illegal test names, iOS Maestro flows, docs |

Stream note: this wave runs on stream A's branch, but I1 writes under `shared/src/iosMain/…/ui`, I3 touches `:app`'s `ui/webview`, and I4 creates `.maestro/ios`. Those are stream B's folders, and the paths above are this wave's listed exceptions. Stream B makes no edits under `shared/src/iosMain`, `shared/src/iosTest`, `iosApp/`, `.maestro/ios` or `ui/playback` while the wave runs.

### I0: Foundation (merge first, keep it small)

Deliverables:

1. Gradle, catalog and `gradle.properties` exactly as in "Target layout → Gradle".
2. F1–F4 exactly as in the fixes table.
3. `shared/src/iosMain/**`: the Appendix A files (verified in the spike) plus the seams: `ios/IosPlaybackHost.kt`, `ios/IosKoin.kt` (`startKoinIos`), `ios/MainViewController.kt` (with `configure = { configureStatusBar() }`), `ios/IosMediaLauncher.kt` (stub), and in `ui/SystemBarsAppearance.ios.kt` the no-op `internal fun ComposeUIViewControllerConfiguration.configureStatusBar() {}`.
4. `iosApp/iosApp/Playback/PlayerPresenter.swift` and `OrientationLock.swift` exactly as in "Seams".

Verification (each command under the build lock, see "Resource rules"):

```bash
L=/private/tmp/claude-501/-Volumes-external-AndroidStudioDownload-StudioProjects-kmp-NoFlix-KMP/250da03a-7879-49e7-8985-536f7d0039de/scratchpad/build-lock.sh
$L ./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64
$L ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
$L ./gradlew :shared:iosSimulatorArm64Test                      # 45/45
xcrun --sdk iphonesimulator swiftc -typecheck -target arm64-apple-ios16.0-simulator \
  -F shared/build/bin/iosSimulatorArm64/debugFramework iosApp/iosApp/Playback/*.swift   # if the toolchain refuses, note it: I2's xcodebuild covers it
$L ./gradlew :shared:jvmTest :app:testDebugUnitTest :app:assembleDebug
scripts/kmp-guard.sh
git diff --exit-code shared/schemas                              # Room schemas untouched
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep '\.so$' | awk '{print $4}' | sort > /tmp/so-after.txt   # compare with dev: identical
```

Done when every command passes, the `.so` list equals `dev`'s, and `Shared.h` contains `IosPlaybackHost`, `IosPlayerRequest`, `startKoinIos(apiKey:)` and `MainViewController(host:)`. Optional, if the orchestrator allows pushing: push the branch and `gh workflow run pr-checks.yml --ref feat/a-ios-i0-foundation` to prove the Linux job (R6).

### I1: iOS platform actuals and iOS tests

Deliverables:

1. Refine the I0 actuals as the audit table says: Gregorian calendar in `formatLocalized`; `NSNumberFormatterRoundHalfUp` in `formatRating`; locale-aware `capitalized()`; 2-letter filter in `isoLanguageCodes`; UIKit `cancelLabel()`; `IosDeviceLocaleProvider` from `NSLocale.preferredLanguages`. Reuse one `NSNumberFormatter` per function only if it's created on the main thread, otherwise create one per call (the formatters aren't thread-safe and these are called during composition).
2. `ui/SystemBarsAppearance.ios.kt`: `configureStatusBar()` installs a `ComposeUIViewControllerDelegate` whose `preferredStatusBarStyle` returns a module-level `var` that `setIconsFor(color)` sets (`luminance() > 0.5` → `UIStatusBarStyleDarkContent`, else `UIStatusBarStyleLightContent`). Then call `setNeedsStatusBarAppearanceUpdate()` on `LocalUIViewController.current`, captured in `rememberSystemBarsAppearance()`. If the API is deprecated, use the documented replacement. If neither works, keep a no-op and record it in the I1 row.
3. `PlatformModule.ios.kt`: keep the paths of D12. Create the Documents folder if needed (`URLForDirectory(create = true)`). Document the choice in a comment.
4. Tests (`shared/src/iosTest/…`):
   - `di/IosPlatformModuleTest.kt`: start a `koinApplication` with `platformModule` + `dataModules` + a fake key module, resolve `zedMoviesDatabase`, `DataStore<Preferences>`, `MovieRepository`, `DeviceLocaleProvider`. Insert a `WishlistEntity` through `WishlistDao` and read it back (bundled SQLite on iOS). Write a preference, close, re-create the DataStore on the same path and read it (persistence). Delete the test files afterwards.
   - `ui/util/PlatformFormatIosTest.kt`: `formatRating(7.25)` ends with `3`; `formatInteger(2020)` has no separator; `formatCount(12345)` has a grouping separator; `isoLanguageCodes()` are all 2 letters and contain `en` and `de`; `displayCountryName("ZZ")` isn't empty; `LocalDate(2026, 7, 29).formatLocalized("d MMMM, yyyy")` contains `29` and `2026`.
   - In `commonTest` (JVM + iOS): `data/database/converter/ListConverterTest.kt` (`genreListToString(listOf(Genre.ACTION)) == "[\"ACTION\"]"` and the round trip: pins F3's format), `data/network/common/NetworkBoundResourceTest.kt` (a failing `fetch` emits `Loading` then `Failure` with the cached value and doesn't throw: pins F2).

Verification:

```bash
$L ./gradlew :shared:iosSimulatorArm64Test :shared:jvmTest
$L ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
scripts/kmp-guard.sh
# If I2 has merged into your base (it won't have unless you sync): the simulator smoke S2 + S4 + S6 + S7.
```

Done when the tests pass on both targets and no file outside I1's list changed.

### I2: Xcode project, Swift shell, secrets, icon, launch screen

Deliverables:

1. `iosApp/project.yml` as in "Target layout". Generate with `cd iosApp && xcodegen generate` and commit the result. Re-running `xcodegen generate` must leave `git status` clean (deterministic output).
2. `iosApp/Config.xcconfig`: `TMDB_API_KEY =` then `#include? "Secrets.xcconfig"`.
3. `scripts/ios-secrets.sh`: writes `iosApp/Secrets.xcconfig` from `$TMDB_API_KEY` if set, else from `local.properties` (parse as in "Spike results"). `chmod +x`. Writes `TMDB_API_KEY = <key>` and never prints the key. Exits 0 with a warning if no key is found (the build still works).
4. Swift: `NoFlixApp.swift` (`@main`, `@UIApplicationDelegateAdaptor(AppDelegate.self)`; `init()` reads `Bundle.main.object(forInfoDictionaryKey: "TMDBApiKey")` and calls `IosKoinKt.startKoinIos(apiKey:)`; `WindowGroup { ComposeRootView().ignoresSafeArea() }`), `AppDelegate.swift`, `ComposeRootView.swift` (holds one `PlayerPresenter()`).
5. `Assets.xcassets`: `AppIcon` (1024, opaque), `LaunchBackground`, `LaunchLogo`, all generated by `tools/branding/generate.py` (two additions; run it as its docstring says).
6. `.gitignore` (append under `# iOS`): `iosApp/Secrets.xcconfig`, `xcuserdata/`, `*.xcuserstate`, `iosApp/build/` (already covered by `build/`, but explicit).

Verification:

```bash
scripts/ios-secrets.sh
(cd iosApp && xcodegen generate) && git status --porcelain iosApp   # only intended files
$L xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=NoFlix iPhone 17' -derivedDataPath iosApp/build build
U=2E6CF691-424D-4045-B5AF-DEB4C78D2C9F
$L sh -c "xcrun simctl boot $U 2>/dev/null; xcrun simctl bootstatus $U -b >/dev/null; \
  xcrun simctl install $U iosApp/build/Build/Products/Debug-iphonesimulator/NoFlix.app && \
  xcrun simctl launch --terminate-running-process $U com.zobaer53.zedmovies && sleep 20 && \
  xcrun simctl io $U screenshot /tmp/i2-home.png"
```

Done when S1 and S2 pass (Home with data, icon on the springboard, launch screen shown, portrait-locked: rotating the simulator with `Cmd+→` doesn't rotate Home), and a fresh clone without `Secrets.xcconfig` still builds. Play only logs the stub line until I3 merges.

### I3: Playback and trailers

Deliverables:

1. `StreamPage.kt` with exactly the API in "Seams → Common playback API". Move the constants **verbatim** (the script text must stay byte-identical).
2. `:app` exceptions as listed. Run `:app:assembleDebug` and the parity test: `StreamPageScriptParityTest` (`@RunWith(RobolectricTestRunner::class)`, real `org.json`) asserts `STREAM_PAGE_SCRIPT.contains("s.textContent = " + JSONObject.quote(STREAM_PAGE_CSS) + ";")`, so Android injects the same bytes as before.
3. `StreamPageTest` (commonTest): `buildStreamUrl` for movie/tv/" TV "; `isStreamSitePage` (site, subdomain, look-alike host `evil<site-host>` → false, null → false); `isStreamWatchPage`; `isStreamNavigationAllowed` (main frame: site ✓, other host ✗, `about:blank` ✓, `itms-apps://` ✗; sub-frame: other https host ✓, `data:` ✓, `itms-apps://` ✗); `youtubeWatchUrl` encodes `&`; CSS is printable ASCII + `\n` with no `<` (so `JsonPrimitive` ≡ `JSONObject.quote`).
4. `IosMediaLauncher.kt`: `play` builds `IosPlayerRequest(buildStreamUrl(…), title, STREAM_DOCUMENT_START_SCRIPT, STREAM_PAGE_SCRIPT)` and calls `host.presentPlayer`. `playTrailer` follows D10 with `UIApplication.sharedApplication.openURL(url, options = mapOf(UIApplicationOpenURLOptionUniversalLinksOnly to true)) { opened -> if (!opened) openURL(url, emptyMap<Any?, Any?>(), null) }`. `IosMediaLauncherTest` (iosTest) checks `play` with a fake host.
5. `iosApp/iosApp/Playback/`: `PlayerPresenter.swift` (real), `PlayerViewController.swift`, `BlockRules.json`, per "Playback architecture on iOS".

Verification:

```bash
$L ./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test :app:testDebugUnitTest :app:assembleDebug
scripts/kmp-guard.sh
$L ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
xcrun --sdk iphonesimulator swiftc -typecheck -target arm64-apple-ios16.0-simulator \
  -F shared/build/bin/iosSimulatorArm64/debugFramework iosApp/iosApp/Playback/*.swift
python3 -m json.tool iosApp/iosApp/Playback/BlockRules.json > /dev/null
```

Before I2 merges, there is no app to run. After the integration merge the tester runs S8 and S9. If I3 merges after I2, the I3 implementer runs S8/S9 itself on the integrated branch (sync = `git merge feat/a-ios-migration`). Done when the commands pass and `:app`'s diff is only the deletion, the moved constants and import lines.

### I4: CI, guard, Maestro on iOS, docs

Deliverables:

1. `.github/workflows/ios-checks.yml`: `on: pull_request` (dev, main), `push` (dev), `workflow_dispatch`, with `paths: [shared/**, iosApp/**, gradle/**, '*.gradle*', gradle.properties, scripts/ios-secrets.sh, .github/workflows/ios-checks.yml]`. One job `ios` on `macos-26` (Xcode 26; select it with `sudo xcode-select -s` if the image's default differs; check the runner image list). Steps: checkout, setup-java 17, setup-gradle, `scripts/ios-secrets.sh` (env `TMDB_API_KEY` from secrets; an empty key is fine), `./gradlew :shared:iosSimulatorArm64Test`, `brew install xcodegen && (cd iosApp && xcodegen generate) && git diff --exit-code iosApp` (drift check), and `xcodebuild … -destination 'generic/platform=iOS Simulator' build` **only if `iosApp/project.yml` exists** (`if: hashFiles('iosApp/project.yml') != ''`, so the job is green before I2 merges). **Not** a required check. `pr-checks.yml` stays unchanged.
2. `scripts/kmp-guard.sh`: also fail on backtick function names with `, . : ; / < > [ ]` in `shared/src/common*`. Keep the existing rule and message, and add a second message for the new rule.
3. `.maestro/ios/flows/`: copies of 01, 02, 03, 04, 07 adapted for iOS (`back` → tap the on-screen back button or an edge swipe `swipe: {start: "2%,50%", end: "80%,50%"}`; no `16 KB` assertion; keyboard `hideKeyboard` may need a tap outside). Plus `05_play.yaml` (Play opens a view with `player_close`; then tap it) and `06_trailer.yaml` (asserts Safari came to the front via `assertVisible` on "youtube"; optional). `.maestro/ios/run.sh`: runs them on the `NoFlix iPhone 17` UDID with `maestro --device <UDID> test .maestro/ios/flows`, under the build lock. They're verified only after integration (the tester runs them).
4. `CLAUDE.md`: an "iOS" subsection under Build & run (the commands of this plan), `iosApp/` and the seams in Architecture, the simulator name, XcodeGen, `Secrets.xcconfig`. `README.md`: one paragraph (simulator build only, sideload). Phase 9 progress line in `kmp-migration-plan.md`.

Verification:

```bash
scripts/kmp-guard.sh                                    # passes on the tree (F4 is in I0)
printf 'class T { @Test fun `a, b`() {} }\n' > shared/src/commonTest/kotlin/GuardProbe.kt && ! scripts/kmp-guard.sh; rm shared/src/commonTest/kotlin/GuardProbe.kt
ruby -ryaml -e 'YAML.load_file(".github/workflows/ios-checks.yml")'    # syntax (or actionlint if installed)
for f in .maestro/ios/flows/*.yaml; do maestro check-syntax "$f" 2>/dev/null || ruby -ryaml -e "YAML.load_stream(File.read('$f'))"; done
```

Done when the guard catches the probe, the YAML parses, and docs name only files that exist in this plan.

### Integration (orchestrator)

1. Worktrees: `git worktree add --no-track -b <branch> ../NoFlix_KMP-i<N> feat/a-ios-migration` and `cp local.properties ../NoFlix_KMP-i<N>/`. Don't run `scripts/stream.sh worktree` (it allows one worktree per stream).
2. Merge I0 into `feat/a-ios-migration` (`git merge --no-ff`), then create the I1–I4 worktrees from the new head.
3. Merge I1, I3, I2, I4 (`--no-ff`). Disjoint files mean no conflicts are expected. If one appears, stop and fix the ownership, never by rebasing.
4. On the integrated branch, run I2's xcodebuild once (under the lock), then hand over to the Opus tester (see Test plan).
5. Afterwards: `git worktree remove ../NoFlix_KMP-i<N>` for each, then `scripts/stream.sh test-merge`, then `scripts/stream.sh pr "iOS port (simulator build)" <body>` with auto-merge (memory "always auto-merge"; note "merged without human review" if so).

## Resource rules for parallel agents (16 GB RAM)

- **Every** Gradle, Kotlin/Native and `xcodebuild` command, and every simulator session (boot, install, launch, screenshot, Maestro), runs under the shared lock: `/private/tmp/claude-501/-Volumes-external-AndroidStudioDownload-StudioProjects-kmp-NoFlix-KMP/250da03a-7879-49e7-8985-536f7d0039de/scratchpad/build-lock.sh <cmd…>`. It serializes across all worktrees (it waits 15 s between checks and frees a lock held by a dead PID). Wrap the whole command, never a sub-step, and never call the lock from inside the Xcode build phase (not re-entrant: deadlock). Plain file edits, `grep`, `xcodegen generate`, `swiftc -typecheck` and `scripts/kmp-guard.sh` don't need the lock.
- Keep `org.gradle.jvmargs=-Xmx4g` (the spike linked the framework within it). Don't raise it. Don't add `--no-daemon`: one daemon is reused across worktrees between locked runs.
- Leave `kotlin.native.cacheKind` at its default (static caches speed up debug links). Don't set `kotlin.native.disableCompilerDaemon`.
- Don't run `clean` or delete `~/.konan`/`~/.gradle` unless a build is provably corrupt. The build cache (`org.gradle.caching=true`) shares compile outputs between worktrees.
- Expect per worktree: first iOS compile ~3.5 min, debug framework link ~2 min, `iosSimulatorArm64Test` link + run ~1–2 min, and about 2–3 GB under `shared/build`. Free disk: ~45 GB system, ~61 GB external.
- One booted simulator at a time: only `NoFlix iPhone 17`. Shut it down at the end of your session (`xcrun simctl shutdown 2E6CF691-424D-4045-B5AF-DEB4C78D2C9F`). Don't boot other projects' simulators (SpeakPro, PE, Hopcrumb, VisaVerse, NA-Stream-*). Don't start Android emulators.
- The Pixel 6a is shared with other projects: Android device work only through `scripts/stream.sh with-device`, and only if it's free.
- At the end of your package: `$L ./gradlew --stop`.

## Test plan (for the final Opus tester)

### Unit tests

| Suite | Command | Expect |
| --- | --- | --- |
| commonTest on iOS | `$L ./gradlew :shared:iosSimulatorArm64Test` | 45 existing + `ListConverterTest`, `NetworkBoundResourceTest`, `StreamPageTest` |
| iosTest | same task | `IosPlatformModuleTest`, `PlatformFormatIosTest`, `IosMediaLauncherTest` |
| commonTest + jvmTest | `$L ./gradlew :shared:jvmTest` | all, incl. the new common tests |
| Android | `$L ./gradlew :app:testDebugUnitTest :app:assembleDebug` + `scripts/kmp-guard.sh` | green, incl. `StreamPageScriptParityTest` |
| Screenshots | `$L ./gradlew :app:verifyRoborazziDebug` | 12/12 unchanged |
| APK `.so` list | `unzip -l app-debug.apk '*.so'` vs a `dev` build | identical (no `libsqliteJni`/bundled SQLite) |

### Simulator smoke checklist (S1–S9)

```bash
U=2E6CF691-424D-4045-B5AF-DEB4C78D2C9F; APP=iosApp/build/Build/Products/Debug-iphonesimulator/NoFlix.app
$L xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination "id=$U" -derivedDataPath iosApp/build build
xcrun simctl boot $U; xcrun simctl bootstatus $U -b
xcrun simctl uninstall $U com.zobaer53.zedmovies; xcrun simctl install $U "$APP"
xcrun simctl launch --console-pty --terminate-running-process $U com.zobaer53.zedmovies > /tmp/ios-console.log 2>&1 &
xcrun simctl io $U screenshot /tmp/ios-home.png                        # S2
xcrun simctl spawn $U log stream --level debug --predicate 'eventMessage CONTAINS "NoFlixPlayer"'   # S9
xcrun simctl launch $U com.zobaer53.zedmovies -AppleLanguages "(de)"   # S7, on a fresh install
xcrun simctl terminate $U com.zobaer53.zedmovies                       # S6 relaunch check
grep -c "Uncaught Kotlin exception" /tmp/ios-console.log               # must be 0
```

Also: airplane-mode equivalent. Run with an empty key (`TMDB_API_KEY=` → rebuild): Home must show the error state and **not crash** (proves F2 end to end). Rotate during playback (`/watch/` → landscape) and after closing (portrait). Check on the iOS **18.5** runtime too (installed; create a temporary iPhone device on it and delete it afterwards), since iOS 16/17 runtimes aren't installed (R2).

### Maestro on the iOS simulator

Maestro 2.12.0 is installed (`~/.maestro/bin/maestro`) and drives iOS simulators. With D6, `appId: com.zobaer53.zedmovies` matches. The Android flows **as they are** use `back`, which is Android-only, so I4 provides `.maestro/ios/flows` (01, 02, 03, 04, 07 adapted; 05/06 iOS-specific) and `.maestro/ios/run.sh`. Run them under the lock on `NoFlix iPhone 17`. If Maestro's iOS driver fails to install on Xcode 26.6, record that and fall back to the manual checklist (not a blocker).

### Android regression gates

Required: `:shared:jvmTest`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `scripts/kmp-guard.sh`, `:app:verifyRoborazziDebug`. **Optional:** `scripts/stream.sh with-device -- .maestro/run.sh` on the Pixel 6a. It may be busy with other projects: check the lock and the foreground app first (memory "Pixel 6a shared"). If it's busy, skip it and record that in the Progress row.

## Risks and mitigations

| # | Risk | Impact | Mitigation |
| --- | --- | --- | --- |
| R1 | More Native-only runtime failures like F2/F3 in screens the spike didn't open (details, search, filters, paging, wish list) | Crash on iOS | The tester covers every screen (S3–S7). Run `:shared:iosSimulatorArm64Test` in CI. Grep for `Json.*<enum>` and contracts before merging. The console check for `Uncaught Kotlin exception`. |
| R2 | iOS 16/17 can't be tested here (only the 18.5 and 26.5 runtimes are installed). The linker warns that `libicu.icudtl_dat.o` (Compose's ICU data) was built for iOS 18.5. | A crash on old iOS | The warning is about a data-only object, so it's probably harmless. Smoke-test on 18.5. If a device on iOS 16/17 matters, download that runtime (~7 GB) as a separate step, or raise the minimum to 17/18 and record why. |
| R3 | The stream site's DOM or flow differs on WebKit (pop-under scripts, `window.open` loops, player iframe needs) | Player blank or ads leak | D9 + D8, logs for each block, `-NoFlixDisableBlockRules`. The shared script means one fix covers both platforms. |
| R4 | The content rule list blocks something the player needs | No video | Third-party ad hosts only, ≤ 100 rules, versioned identifier, kill switch; test S9 with and without the list |
| R5 | `ComposeUIViewControllerDelegate` is deprecated or behaves differently in 1.12.2 | Wrong status bar icon color on Details | No-op fallback is acceptable (the system default matches the theme). Record it in the I1 row. |
| R6 | `pr-checks.yml` on ubuntu configures `:shared` with iOS targets for the first time | The required `build` check fails, blocking merges | `kotlin.native.ignoreDisabledTargets=true`; optional I0 `workflow_dispatch` run; the integrated PR runs it before merge, and the fix is local to `shared/build.gradle.kts` |
| R7 | RAM pressure from parallel agents (K/N + Gradle + Xcode + simulator) | Swap, killed builds, flaky results | The lock for every build and simulator session, one simulator, `--stop` at the end |
| R8 | XcodeGen output not deterministic | CI drift check flaps | Commit after one `generate` and check that a second `generate` leaves the tree clean (I2 done criterion) |
| R9 | GitHub macOS runner image lacks Xcode 26 or the simulator SDK | The iOS job fails | Pin the image, `xcode-select` the version explicitly, `generic/platform=iOS Simulator` destination. Not a required check. |
| R10 | Sideloading onto a real device needs a signing identity, and there is no Apple developer team | No device build yet | Out of scope (simulator only). See open question Q1. |
| R11 | Moving the scripts changes Android's injected JS | Android playback styling breaks | Verbatim move + `StreamPageScriptParityTest` with real `org.json` |

## Open questions (with recommended answers)

| # | Question | Recommendation |
| --- | --- | --- |
| Q1 | Device builds: when and how? | Later and optional. A free personal team in Xcode gives 7-day profiles on your own device (enough to try it). A paid account plus AltStore/SideStore gives longer installs. It's then a `DEVELOPMENT_TEAM` setting and `CODE_SIGNING_ALLOWED: YES` in `project.yml`. Nothing else changes. |
| Q2 | Full AdGuard lists on iOS (v2)? | Only if S9 shows ads that D8/D9 miss. Then convert in CI with AdGuard's SafariConverterLib (check its license against Apache-2.0) and download the JSON at runtime. |
| Q3 | In-app trailers (v2)? | Skip unless the user asks. YouTube app/Safari works and avoids error 153 handling. |
| Q4 | `MARKETING_VERSION` for iOS? | `1.0.0` until a release pipeline exists. Align with Android's `versionName` then. |
| Q5 | iPad? | No. iPhone only; the adaptive-UI phase (Phase 5) comes first. |
| Q6 | Release (Kotlin release framework, archive)? | Not in this plan (simulator debug only). `linkReleaseFrameworkIosArm64` comes with Q1. |

## Progress

Each package edits only its own row (in its first commit: "In progress").

| ID | Status | Branch / merge | Gates |
| --- | --- | --- | --- |
| Plan | Done (2026-11-14): audit + spike (iOS compile/link/tests/app launch), fixes F1–F4 found | `feat/a-ios-migration` | Spike reverted; Home loaded on `NoFlix iPhone 17` |
| I0 | Done (2026-11-14): iOS targets + static `Shared` framework, KSP, Darwin + bundled SQLite (iosMain only), F1–F4, Appendix A actuals, frozen seams, Swift stubs | `feat/a-ios-i0-foundation` (committed on the orchestrator's checkout) | iOS compile (sim + device) OK; `linkDebugFrameworkIosSimulatorArm64` OK (`Shared.h` has `IosPlaybackHost`, `IosPlayerRequest`, `startKoinIos(apiKey:)`, `MainViewController(host:)`); `iosSimulatorArm64Test` 45/45; `swiftc -typecheck` of `Playback/*.swift` OK; `:shared:jvmTest :app:testDebugUnitTest :app:assembleDebug` green; `kmp-guard.sh` OK; `shared/schemas` unchanged; APK `.so` list identical to a build without the I0 changes (12 files) |
| I1 | Implemented (2026-11-14), merged into `feat/a-ios-migration`; integration test pending | `feat/a-ios-i1` | `iosSimulatorArm64Test` 66/66, `:shared:jvmTest` 70/70, framework link OK, `kmp-guard.sh` OK, schemas unchanged. Status bar uses the deprecated `ComposeUIViewControllerDelegate` (`@Suppress`); icon flip not yet seen on the simulator |
| I2 | Implemented (2026-11-14), merged into `feat/a-ios-migration`; integration test pending | `feat/a-ios-i2` | `xcodegen generate` deterministic (no diff after regenerate); `xcodebuild` simulator build OK; installs and launches, Home shows TMDB data; `kmp-guard.sh` OK. No `createIntermediateGroups` (random group ids); version 1.0.0 (1) |
| I3 | Implemented (2026-11-14), merged into `feat/a-ios-migration`; integration test pending | `feat/a-ios-i3` | `iosSimulatorArm64Test` 60/60, `:shared:jvmTest` 75/75, `:app:testDebugUnitTest` 14/14 incl. `StreamPageScriptParityTest`, `:app:assembleDebug` OK, Swift typechecks, `BlockRules.json` (64 hosts) compiles with `WKContentRuleListStore`. Deviation: `org.json` escapes every `/`, so the shared quoting does too (byte parity) |
| I4 | Implemented (2026-11-14), merged into `feat/a-ios-migration`; integration test pending | `feat/a-ios-i4` | `kmp-guard.sh` OK and catches the probe; `ios-checks.yml` parses (not run on a macOS runner yet); `maestro check-syntax` OK for 7 iOS flows; docs updated |
| Test | Not started (Opus tester, after integration) | `feat/a-ios-migration` | S1–S12 |

## Appendix A: verified spike code for I0

These files compiled for `iosSimulatorArm64`, linked, and ran on the simulator (Home loaded). I0 lands them as they are (plus the seams). I1 then refines them.

`data/network/util/HttpEngine.ios.kt`

```kotlin
package com.zobaer53.zedmovies.data.network.util

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun httpEngine(): HttpClientEngine = Darwin.create()
```

`data/platform/IosPlatformProviders.kt`

```kotlin
package com.zobaer53.zedmovies.data.platform

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages

internal class IosRegionProvider : RegionProvider {
    override fun region(): String = NSLocale.currentLocale.countryCode ?: ""
}

internal class IosDeviceLocaleProvider : DeviceLocaleProvider {
    override fun language(): String {
        val first = NSLocale.preferredLanguages.firstOrNull() as? String ?: return NSLocale.currentLocale.languageCode
        return NSLocale(localeIdentifier = first).languageCode
    }
}
```

`di/PlatformModule.ios.kt`

```kotlin
package com.zobaer53.zedmovies.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.zobaer53.zedmovies.data.database.zedMoviesDatabase
import com.zobaer53.zedmovies.data.platform.DeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.IosDeviceLocaleProvider
import com.zobaer53.zedmovies.data.platform.IosRegionProvider
import com.zobaer53.zedmovies.data.platform.RegionProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private fun documentsDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    return requireNotNull(url?.path)
}

actual val platformModule: Module = module {
    single {
        Room.databaseBuilder<zedMoviesDatabase>(name = documentsDirectory() + "/" + zedMoviesDatabase.zedMovies_DATABASE)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(produceFile = { (documentsDirectory() + "/preferences.preferences_pb").toPath() })
    }
    single<DeviceLocaleProvider> { IosDeviceLocaleProvider() }
    single<RegionProvider> { IosRegionProvider() }
}
```

`ui/SystemBarsAppearance.ios.kt` (I0 adds the no-op `configureStatusBar()` hook)

```kotlin
package com.zobaer53.zedmovies.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private object IosSystemBarsAppearance : SystemBarsAppearance {
    override fun setIconsFor(color: Color) = Unit
}

@Composable
actual fun rememberSystemBarsAppearance(): SystemBarsAppearance = IosSystemBarsAppearance
```

`ui/smallcomponent/mapper/ReleaseDateMapper.ios.kt` (I1 switches to a Gregorian calendar)

```kotlin
package com.zobaer53.zedmovies.ui.smallcomponent.mapper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

internal actual fun LocalDate.formatLocalized(pattern: String): String {
    val date = NSCalendar.currentCalendar.dateFromComponents(toNSDateComponents()) ?: return toString()
    val formatter = NSDateFormatter()
    formatter.locale = NSLocale.currentLocale
    formatter.dateFormat = pattern
    return formatter.stringFromDate(date)
}
```

`ui/util/PlatformFormat.ios.kt` (I1 adds half-up rounding and locale-aware capitalization)

```kotlin
package com.zobaer53.zedmovies.ui.util

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale
import platform.Foundation.ISOCountryCodes
import platform.Foundation.ISOLanguageCodes
import platform.Foundation.localizedStringForCountryCode
import platform.Foundation.localizedStringForLanguageCode

internal actual fun isoLanguageCodes(): List<String> = NSLocale.ISOLanguageCodes.filterIsInstance<String>().filter { it.length == 2 }

internal actual fun isoCountryCodes(): List<String> = NSLocale.ISOCountryCodes.filterIsInstance<String>()

internal actual fun displayLanguageName(isoCode: String): String =
    (NSLocale.currentLocale.localizedStringForLanguageCode(isoCode) ?: isoCode).capitalized()

internal actual fun displayCountryName(isoCode: String): String =
    NSLocale.currentLocale.localizedStringForCountryCode(isoCode) ?: isoCode

internal actual fun String.capitalized(): String = replaceFirstChar { it.titlecase() }

internal actual fun formatRating(rating: Double): String {
    val f = NSNumberFormatter()
    f.locale = NSLocale.currentLocale
    f.minimumFractionDigits = 1u
    f.maximumFractionDigits = 1u
    f.usesGroupingSeparator = false
    return f.stringFromNumber(NSNumber(rating)) ?: rating.toString()
}

internal actual fun formatCount(value: Int): String {
    val f = NSNumberFormatter()
    f.locale = NSLocale.currentLocale
    f.numberStyle = NSNumberFormatterDecimalStyle
    f.maximumFractionDigits = 0u
    return f.stringFromNumber(NSNumber(value)) ?: value.toString()
}

internal actual fun formatInteger(value: Int): String {
    val f = NSNumberFormatter()
    f.locale = NSLocale.currentLocale
    f.usesGroupingSeparator = false
    return f.stringFromNumber(NSNumber(value)) ?: value.toString()
}
```

`ui/util/PlatformUi.ios.kt` (I1 switches to UIKit's localized Cancel)

```kotlin
package com.zobaer53.zedmovies.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal actual fun Modifier.testTagsAsResourceId(): Modifier = this

@Composable
internal actual fun cancelLabel(): String = "Cancel"
```

`ios/IosKoin.kt` (as spiked; I0 formats it properly)

```kotlin
package com.zobaer53.zedmovies.ios

import com.zobaer53.zedmovies.data.network.api.zedMoviesApiKeyProvider
import com.zobaer53.zedmovies.di.sharedModules
import com.zobaer53.zedmovies.di.viewModelModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun startKoinIos(apiKey: String) {
    val iosAppModule = module {
        single<zedMoviesApiKeyProvider> { object : zedMoviesApiKeyProvider { override val apiKey = apiKey } }
    }
    startKoin { modules(listOf(iosAppModule, viewModelModule) + sharedModules) }
}
```

The spike's `project.yml` (in the scratchpad, not committed) differed from the I2 spec only by absolute paths, an inline `Secrets.xcconfig` and the default product name.
