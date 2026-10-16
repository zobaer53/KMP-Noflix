# NoFlix: Kotlin Multiplatform migration plan

_Audited 2026-10-14 against the code before the migration. Status (2026-10-16): the Android part (phases 0–4 and the switch to `AndroidSQLiteDriver`) is merged into `dev`. The hardening clean-up (A6) is in progress. Phases 5–9 haven't started. Follows JetBrains' guide [Migrating a Jetpack Compose app to Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform/migrate-from-android.html). Library versions are given as minimums or major lines only. Before each phase, check the latest stable release, and check items marked **verify** with a short spike._

> **Android-only detail:** phases 0–4 for Android (plus a hardening phase) are expanded step by step, with code, gates and risks, in [kmp-android-plan.md](kmp-android-plan.md). That document is the working plan for the Android migration. This one covers all platforms.

## Goals and priority

| Priority | Platform | Ships as |
| --- | --- | --- |
| **1 (now)** | **Android phone/tablet** | Same `NoFlix.apk`, same release pipeline. Every phase has to keep it working and shippable. |
| 2 | **Android TV / Google TV** | **The same APK**: TV launcher entry, D-pad UI, TV playback mode |
| 3 | **Desktop** (macOS, Windows, Linux) | Compose Desktop installers (`.dmg`, `.msi`, `.deb`) attached to the same GitHub release |
| 4 | **Web** | Compose for Web (Kotlin/Wasm) static site |
| 5 (later) | **iOS** | Xcode app, distribution to be decided |

## Verdict

**All five are possible.** Browsing, search, filters, details and the wish list can be written once in shared code. **Ad-free playback can't be shared.** Each platform has a different way of playing the stream and blocking ads, and on web nothing can block ads inside another site's player.

### What each platform gets

| Feature | Android | Android TV | Desktop | Web | iOS |
| --- | --- | --- | --- | --- | --- |
| Home, see all, search, filters, details | ✅ shared | ✅ shared, D-pad layout | ✅ shared, wide layout | ✅ shared, wide layout | ✅ shared |
| Wish list + content language (persisted) | ✅ Room + DataStore | ✅ same | ✅ Room + DataStore (JVM) | ✅ browser storage (no Room, see Phase 8) | ✅ Room + DataStore |
| Offline cache of lists/details | ✅ Room | ✅ | ✅ | ❌ network only | ✅ |
| Trailers | ✅ in-app player (today) | ✅ YouTube TV app, in-app fallback | ✅ browser (v1) / embedded (v2) | ✅ YouTube iframe in page | ✅ YouTube app (v1) / in-app (v2) |
| Playback | ✅ WebView + native ad-block (today) | ✅ same WebView + TV remote mode | ⚠️ v1: opens the system browser (no built-in ad-block). v2: embedded Chromium with blocking, **after a codec spike** | ⚠️ opens the stream page in a new tab. **The app can't block ads there.** | ⚠️ WKWebView + content-blocker rules. **App Store unlikely** |

### Shareability of the code before the migration

| Area | Shareable? | Effort |
| --- | --- | --- |
| `domain/` (models, repository interfaces, 17 use cases) | **Yes, almost as is.** It imports only `javax.inject` and `androidx.paging.PagingData`. | Low |
| `data/` (TMDB, Room, DataStore, Paging, mappers) | **Yes**, once Retrofit/OkHttp → Ktor and a few `android.*`/`java.*` calls are removed. Web needs a cache-less variant (Phase 8). | Medium |
| `di/` (Hilt) | **Rewrite** in Koin | Low–medium |
| `ui/` Compose screens, design system, `navigation/` | **Yes, through Compose Multiplatform.** The move is mostly mechanical (`R.*` → `Res.*`). TV, desktop and web also need a large-screen + focus layout, which this app doesn't have yet. | Medium (+ medium for large screens) |
| `ui/webview/`, `:ad-filter`, `:adblock-client` | **Android + Android TV only** (Android `WebView`, JNI, WorkManager) | Per-platform work |
| `ui/trailer/` (android-youtube-player) | **Android only** | Low per platform |

Before the migration: about 14,900 lines of Kotlin, and roughly 85% was expected to move to `commonMain`. After Phase 4, about 13,700 of the 14,550 lines in the main source sets of `:app` and `:shared` (about 94%) are in `shared/src/commonMain`. The ad-block modules are not counted.

### Why this project is a good candidate

- 100% Kotlin and 100% Compose. No Java, no RxJava, no View-based UI outside the two playback activities.
- **Room 2.8.5, DataStore 1.2.1, Paging 3.5.3, Coil 3, kotlinx-serialization/-coroutines/-datetime and Material 3** already have multiplatform artifacts at the versions in `libs.versions.toml`.
- The layers are clean: data → domain → ui, with mappers between them. The domain layer depends on **repository interfaces**, so web can use different repository implementations without touching ViewModels.
- The API key from `BuildConfig` already sits behind an interface, `zedMoviesApiKeyProvider` (now in `:shared`, provided by `:app`'s `androidAppModule`). The unused `zedMoviesVersionProvider` was deleted in A1.
- Every Room DAO already returns `Flow` or `PagingSource`, or is `suspend`. Room requires this in common code.
- `adblock-client` doesn't set `abiFilters`, so the native library is built for `armeabi-v7a` as well. Many Android TV boxes run 32-bit userlands, so the ad blocker already works on them.

### Hard parts

1. **Playback differs on every platform.** See the matrix above and the phases below.
2. **AGP 9 constraint.** The KMP plugin can't be applied to a `com.android.application` module, so shared code goes into a new library module that uses `com.android.kotlin.multiplatform.library`. That plugin has no `buildConfig` and no build types.
3. **There is no focus or D-pad handling in the app today** (0 uses of `focusable`/`FocusRequester`/`onKeyEvent`). The UI is also portrait-phone only: `screenOrientation="portrait"` in the manifest, plus code in `WebView.kt` that forces portrait off the watch page. TV, desktop and web all need a landscape layout that works with keyboard and D-pad focus. Built once in shared code, it serves all three.
4. **Web can't use the Android caching design unchanged.** Room on the web needs an OPFS worker plus cross-origin isolation headers (COOP/COEP), and COEP blocks cross-origin iframes such as YouTube embeds. Web therefore goes network-only, with the wish list in browser storage (Phase 8).
5. **Distribution and exposure.** iOS: App Review will very likely reject in-app playback from a third-party site (guideline 5.2). Web: a public site that links to the stream site, hosted on GitHub Pages, is easier for rights holders to target than an APK. Decide where to host it (Phase 8).

## State before the migration (audit, 2026-10-14)

### Android-only APIs outside playback

Each of these needed a change before the file could move to `commonMain`, and phases 0–4 made all of them. Where the result differs from the table: the logs were deleted and Kermit wasn't added; on Android, DataStore uses `PreferenceDataStoreFactory.create` with `preferencesDataStoreFile("preferences")`; number formatting uses `expect` helpers in `ui/util/PlatformFormat.kt` (`formatInteger`, `formatCount`, `formatRating`) instead of `formatNumber`; the ripple uses both CMP `material-ripple` (`RippleAlpha`) and M3 `RippleConfiguration`.

| Where | Android/JVM API | Replacement |
| --- | --- | --- |
| `data/network/**` (`Retrofit.kt`, `ResultAdapterFactory`, `ResultCall`, `CallDelegate`, `ZedMoviesAuthInterceptor`, 3 `*Service` interfaces) | Retrofit 3, OkHttp 5, `java.lang.reflect` | **Ktor 3 client**. A `safeRequest {}` helper returns `ZedMoviesResult`. The auth interceptor becomes `defaultRequest { parameter("api_key", …) }`. |
| `di/*`, `DispatchersModule`, `javax.inject.*` in 19 data + 17 domain files | Hilt / Dagger | **Koin 4** modules. Remove `@Inject` annotations. |
| `data/datastore/PreferencesDataStoreDataSource.kt`, `util/LocaleUtils.kt`, `di/DataStoreModule.kt` | `Context`, `preferencesDataStore` delegate, `ConfigurationCompat` | `PreferenceDataStoreFactory.createWithPath` + `DeviceLocaleProvider` |
| `di/DatabaseModule.kt` | `Room.databaseBuilder(context, Class, name)` | `expect`/`actual` builder + `@ConstructedBy` |
| `data/database/util/ZedMoviesDatabaseTransactionProvider.kt` | `androidx.room.withTransaction` (Android-only) | `useWriterConnection { it.immediateTransaction { … } }` |
| `ZedMoviesResult.kt`, `NetworkBoundResource.kt`, `TvShowRemoteMediator.kt`, `TvShowDetailsNetworkDataSource.kt`, 4 UI files | `android.util.Log` (`"Success1"` debug leftovers) | Delete them. Use **Kermit** where you still want a log. |
| `data/model/UserMessage.kt`, `ui/smallcomponent/mapper/UserMessageMapper.kt` | `@StringRes Int` stored in a data-layer model | A sealed type in data, mapped to `StringResource` in the UI |
| `data/mapper/SearchFiltersMapper.kt` | `java.time.LocalDate.now()/of()` | kotlinx-datetime |
| `ui/search/FiltersSheet.kt` | `java.time.Year.now()`, `java.util.Locale` | kotlinx-datetime + an `expect` locale helper |
| `ui/search/MoreFilters.kt` | `java.text.NumberFormat` | `expect fun formatNumber(…)` |
| `data/repository/FilterOptionsRepositoryImpl.kt` | `Locale.getDefault().country` | A `RegionProvider`, injected |
| `data/network/serializer/LocalDateSerializer.kt` | `java.text.ParseException` | Catch `IllegalArgumentException`. kotlinx-datetime never throws `ParseException`, so today's catch is dead code. |
| Mediators, `TmdbPagingSource`, `UserMessageMapper`, tests | `java.io.IOException` | `kotlinx.io.IOException`. It's a typealias on the JVM and the base class of Ktor's network errors. |
| `navigation/TopLevelDestination.kt` | `@DrawableRes`/`@StringRes` + `R` | `DrawableResource`/`StringResource` + `Res` |
| `ui/SystemBarsAppearance.kt` | `Activity`, `Window`, `WindowCompat` | `expect` composable. A no-op on TV, desktop and web. |
| `ui/smallcomponent/Rating.kt` (Play), `ui/details/components/Trailer.kt` | `Intent`, `LocalContext`, `startActivity` | A **`MediaLauncher`** interface provided by each host app (see "Playback architecture") |
| `ui/search/*`, `SearchScreen.kt` | `material-icons-core` (`Close`, `KeyboardArrowUp/Down`) | Vector drawables in `composeResources` |
| `ui/designsystem/theme/Ripple.kt` | `androidx.compose.material.ripple.RippleAlpha` | CMP `material-ripple` (**verify**) or M3 `RippleConfiguration` |
| `ui/designsystem/theme/Type.kt` | `Font(R.font.montserrat_*)` | `Font(Res.font.montserrat_*)`. It's a `@Composable` call in CMP. |
| `ui/details/navigation`, `ui/list/navigation` | `SavedStateHandle`, `navArgument` | The same API from JetBrains' multiplatform navigation and lifecycle artifacts |

### Orientation and input (relevant for TV, desktop and web)

| Where | Today | Needed |
| --- | --- | --- |
| `AndroidManifest.xml`, `zedMoviesActivity` | `android:screenOrientation="portrait"` | Lock portrait at runtime on phones only. A manifest portrait lock also implies a required portrait screen feature, which TVs don't have. |
| `ui/webview/WebView.kt:46-49` | Landscape on `/watch/` and full screen, **portrait** everywhere else | Skip orientation changes on TV (and on large screens) |
| `ui/trailer/TrailerActivity.kt:141-151` | Sensor-landscape in full screen | Skip on TV |
| Whole UI | Bottom bar, vertical feeds, touch only, pull-to-refresh | Navigation rail or top tabs, visible focus, D-pad/keyboard navigation, refresh action |
| `ui/home/component/Pager.kt` | Touch-swiped hero pager | Left/right with the D-pad or arrow keys |
| `ui/search/FiltersSheet.kt` | `RangeSlider`/`Slider` in a `ModalBottomSheet` | Arrow-key support (**verify**). Otherwise stepper buttons on TV. |

### Resources

| Android `res/` | Target | Notes |
| --- | --- | --- |
| `values/strings.xml` (138 strings + plurals) | `shared/src/commonMain/composeResources/values/` | Keep `app_name` in `:app` as well (the manifest uses it) |
| `values-night/strings.xml` (27 strings) | `composeResources/values-dark` (B5) | They only change the casing of 5 strings in dark mode. Whether to delete them is still open. |
| `drawable/ic_*.xml`, `no_*_results.xml` | `composeResources/drawable` | Android vector XML works as is |
| `font/montserrat_*.ttf` | `composeResources/font` | |
| `raw/data.json` (Lottie) | Deleted | Removed with the scraping fallback in B2 |
| `mipmap-*`, `zed_movies_round_*`, `themes.xml`, splash | Stay in `:app` | Add a **TV banner** (320×180). Desktop needs `.icns`/`.ico`/`.png`, web needs a favicon and PWA icons, iOS needs `AppIcon.appiconset`. Generate all of them from `tools/branding/generate.py`. |

### Tests

Most unit tests now live in `:shared` and run on its JVM target (`./gradlew :shared:jvmTest`). `commonTest` (`kotlin.test`) has the search filter tests (`SearchFiltersMapperTest`, `SearchFiltersMatcherTest`, `FilterOptionsMapperTest`) and the `TmdbPagingSource`, `SafeRequest`, `MovieService` and `LocalDateSerializer` tests. `jvmTest` has the Room DAO and migration tests, the Koin graph tests (`SharedKoinGraphTest`, `ViewModelGraphTest`), `UserMessageMapperTest` and the screen smoke test. `:app` keeps `KoinGraphTest` and the Roborazzi screenshot tests. Maestro stays the main regression check on Android and covers Android TV too (D-pad keys). Desktop and web get Compose UI tests (`runComposeUiTest`) plus a manual smoke checklist.

## Target architecture

```
NoFlix_KMP/
├── app/                 Android phone + Android TV application (com.android.application) – stays ":app"
│   ├── zedMoviesActivity (+ LEANBACK_LAUNCHER), zedMoviesApplication (Koin start)
│   ├── ui/webview/      WebView playback + :ad-filter (phone and TV mode)
│   ├── ui/trailer/      TrailerActivity
│   └── res/             launcher icon, TV banner, splash, themes, app_name
├── shared/              KMP library: kotlin.multiplatform + com.android.kotlin.multiplatform.library
│   │                    + org.jetbrains.compose + kotlin.plugin.compose + ksp + room + serialization
│   ├── commonMain/      domain, repository interfaces, network, mappers, ViewModels, navigation, all UI,
│   │                    adaptive layouts, StreamPageStyle JS strings, buildStreamUrl()
│   ├── cachedMain/      Room DB, DataStore, RemoteMediators, cached repositories   (android, jvm, ios)
│   ├── webMain/         network-only repositories, browser-storage wish list        (wasmJs)
│   ├── androidMain/     platform actuals (DB path, DataStore path, locale, system bars, number format)
│   ├── jvmMain/         desktop actuals (app-data dir, Locale, NumberFormat)
│   ├── wasmJsMain/      web actuals (Intl, localStorage, Dispatchers)
│   ├── iosMain/         (later) iOS actuals + MainViewController()
│   ├── commonTest/      unit tests
│   ├── composeResources/
│   └── schemas/         Room schemas (moved from app/schemas)
├── desktopApp/          Compose Desktop application (main(), window, packaging)
├── webApp/              Kotlin/Wasm browser entry (index.html, ComposeViewport) – or a wasmJs executable in :shared
├── iosApp/              (later) Xcode project
├── ad-filter/           Android-only for now (possibly KMP later for desktop, Phase 7b)
└── adblock-client/      Android-only for now (JNI; could be built for desktop JNI, Phase 7b)
```

Why this layout:

- **Keep `:app` as the Android module, and ship phone and TV in one APK.** CI, `.maestro/run.sh`, `upgrade.sh` and `CLAUDE.md` all assume `app/`. One APK means one release and one signing key, and users install the same `NoFlix.apk` on a phone or a TV. A separate `:tvApp` only pays off if the TV UI ever diverges completely. The shared adaptive UI below avoids that.
- **One `:shared` module first.** Today's app is a single module. Split it into `:shared:data`/`:shared:ui` later if build times call for it.
- **Custom source-set hierarchy (not set up yet).** With only `android` and `jvm`, Room, DataStore and the RemoteMediators sit in `commonMain`. When Phase 8 adds `wasmJs`, move them into a `cachedMain` set shared by `android`, `jvm` and (later) `ios`, but **not** `wasmJs`. Web gets `webMain`. Both sit under `commonMain`, which holds the repository interfaces, network code, mappers and UI. Set this up with `applyDefaultHierarchyTemplate { common { group("cached") { withAndroidTarget(); withJvm(); group("ios") }; group("web") { withWasmJs() } } }`. **Verify** the exact `withAndroid…` matcher for the AGP 9 Android KMP plugin.
- **Add targets when their phase starts:** `android` + `jvm` in Phase 3 (JVM is cheap, runs tests fast, and is the desktop target), `wasmJs` in Phase 8, `iosArm64`/`iosSimulatorArm64` in Phase 9. That way you never wait on Kotlin/Native builds while Android is the focus.
- **New build scripts in Kotlin DSL** (`shared/build.gradle.kts`, `desktopApp/build.gradle.kts`). The existing Groovy scripts can stay.
- **Kotlin plugin wiring.** The root `buildscript` already puts KGP 2.4.21 on the classpath for AGP's built-in Kotlin. Apply `org.jetbrains.kotlin.multiplatform` in `:shared` without a version, or through an alias declared `apply false` at the root. If Gradle complains that the plugin is "already on the classpath with an unknown version", drop the version. `:app` keeps built-in Kotlin, and `kotlin-android` is never re-added.
- **Compose versions.** Once the UI is on Compose Multiplatform, the CMP plugin sets the Compose versions, and on Android they resolve to `androidx.compose.*`. Pick a CMP release whose Jetpack Compose baseline is at least BOM `2026.09.00`. Then remove the BOM from `:app`, or keep it pinned to the same versions. AGP 9 doesn't align versions (`android.dependency.useConstraints=false`), so check `./gradlew :app:dependencies`.

### Playback architecture

The shared UI only knows this interface (`ui/playback/MediaLauncher.kt` in `:shared`). Each host app provides its own implementation through `LocalMediaLauncher` (Android: `playback/AndroidMediaLauncher.kt` in `:app`). The interface has only `play()` and `playTrailer()` so far. `canPlay` below gets added with the first host that can't play:

```kotlin
interface MediaLauncher {
    val canPlay: Boolean
    fun play(request: PlayRequest)          // tmdbId, type, name, year
    fun playTrailer(youtubeKey: String)
}
```

| Host | `play()` | `playTrailer()` |
| --- | --- | --- |
| Android phone | `WebViewActivity` (today) | `TrailerActivity` (today) |
| Android TV | `WebViewActivity` in **TV mode** (Phase 6) | `vnd.youtube:<key>` intent to the YouTube TV app, falling back to `TrailerActivity` |
| Desktop | v1: `Desktop.browse(streamUrl)`. v2: embedded Chromium window (Phase 7b) | Open `youtube.com/watch?v=` in the browser (v1), or show it in the embedded window (v2) |
| Web | `window.open(streamUrl, "_blank", "noopener")` | YouTube IFrame embed in a dialog |
| iOS | `WKWebView` controller (Phase 9) | YouTube app/Safari (v1), embedded (v2) |

`buildStreamUrl()` and the `StreamPageStyle` JS strings move to `commonMain`. The Android WebView, desktop Chromium and iOS WKWebView all inject the same scripts, so a fix after a site DOM change applies everywhere.

### Adaptive UI (for TV, desktop, web and tablets)

The hosts provide two `CompositionLocal`s, and screens choose their layout from them:

- `LocalFormFactor`: `Phone`, `Tablet`, `Tv`, `Desktop`, `Web`. Android detects TV with `UiModeManager.currentModeType == UI_MODE_TYPE_TELEVISION`, and phone vs tablet from the window size class.
- `LocalInputMode`: `Touch`, `Directional` (D-pad or keyboard), `Pointer` (mouse). TV is always `Directional`. Desktop and web switch on the last input event.

Shared building blocks go in `ui/designsystem`, built only on **Compose foundation APIs** so they work on every target. `androidx.tv:tv-material` is Android-only, so it isn't used:

- `FocusableCard`: scales up and gets a border when focused, plus a hover state for pointers.
- `ZedNavigationRail` for TV, desktop and web next to the existing `ZedMoviesBottomBar` for phones.
- A focus-restoring `LazyRow`/`LazyVerticalGrid` wrapper (`focusRestorer`, initial `FocusRequester`).
- Overscan-safe padding on TV (about 48 dp horizontal, 27 dp vertical).
- Wide layouts: details shown as backdrop + side panel, feeds as multi-column grids, and the filter sheet as a side panel instead of a bottom sheet.

## Library mapping

| Today | KMP | Android | JVM (desktop) | wasmJs (web) | iOS |
| --- | --- | --- | --- | --- | --- |
| Hilt + `hilt-lifecycle-viewmodel-compose` | **Koin 4** (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`) | ✅ | ✅ | ✅ | ✅ |
| Retrofit 3 + OkHttp 5 | **Ktor 3 client** + content negotiation + kotlinx-json | OkHttp engine | OkHttp or CIO engine | Js engine (`fetch`) | Darwin engine |
| Coil 3 + `coil-network-okhttp` | Coil 3 + **`coil-network-ktor3`** | ✅ | ✅ | ✅ | ✅ |
| Room 2.8.5 | Room KMP (in `commonMain` for now; `cachedMain` once `wasmJs` is added) | `AndroidSQLiteDriver` (platform SQLite, no new `.so`) | `BundledSQLiteDriver` | ❌ not used (see Phase 8) | `BundledSQLiteDriver` |
| DataStore 1.2.1 | `datastore-preferences-core` (in `commonMain` for now; `cachedMain` once `wasmJs` is added) | ✅ | ✅ | ❌ → `multiplatform-settings` (localStorage) | ✅ |
| Paging 3.5.3 | `paging-common` + `paging-compose` | ✅ | ✅ | **verify** wasmJs artifacts. Otherwise a small common "load more" pager for web. | ✅ |
| `navigation-compose` | `org.jetbrains.androidx.navigation:navigation-compose` | ✅ | ✅ | ✅ (can bind to browser history) | ✅ |
| `lifecycle-*` | `org.jetbrains.androidx.lifecycle:*` | ✅ | ✅ | ✅ | ✅ |
| Compose BOM + Material 3 | Compose Multiplatform | ✅ | ✅ | ✅ | ✅ |
| kotlinx-datetime | same | ✅ | ✅ | ✅ (non-UTC zones need the js-joda timezone package, **verify** for wasm) | ✅ |
| `android.util.Log` / Timber | **Kermit** | ✅ | ✅ | ✅ | ✅ |
| `Dispatchers.IO` | `expect val ioDispatcher` | `IO` | `IO` | `Default` | `IO` |
| JUnit 4 | `kotlin.test` + coroutines-test | ✅ | ✅ | ✅ | ✅ |
| android-youtube-player, `androidx.webkit`, splashscreen, `:ad-filter` (Jsoup and Lottie were deleted in B2) | Stay in `:app` | ✅ | — | — | — |
| `BuildConfig.API_KEY` | Provided by each host through `zedMoviesApiKeyProvider` | `BuildConfig` | Gradle-generated constant from `local.properties` / CI secret | Build-time constant **or a proxy** (Phase 8) | `.xcconfig` → `Info.plist` |

Desktop also needs `kotlinx-coroutines-swing` on the classpath for `Dispatchers.Main`. `compose.desktop.currentOs` normally brings it in (**verify**).

## Phased plan

Rules for every phase:

- **Work on a branch from `dev`, never directly on `main` or `dev`.** This repository has no release workflow yet. In the original repository, a push to `master` publishes a public release.
- **The Android app stays shippable after every phase.** Gates: unit tests (`:shared:jvmTest` and `:app:testDebugUnitTest`, as in `pr-checks.yml`), the full `.maestro/run.sh`, and **`.maestro/upgrade.sh`** with the previous release APK. The upgrade test proves the wish list (Room) and content language (DataStore) survive the update.
- CI checks stay green (`pr-checks.yml`: unit tests, debug build, KMP guard). `.maestro/run.sh` checks the 16 KB alignment of every 64-bit `.so`. The release signature and `zipalign -P 16` checks come back with the release workflow.
- CI guard: `scripts/kmp-guard.sh`, run by `pr-checks.yml`, fails on `android.*`, `androidx.core.*`, `java.*` or `javax.*` imports in `shared/src/commonMain`, and on fully qualified `java.`/`javax.` names. It can't see `java.lang` names (`System`, `Thread`), so review those by hand. With only Android and JVM targets at first, nothing else would stop `java.*` imports from creeping into `commonMain` before web and iOS exist.
- Don't name the streaming site in commits, file names or identifiers.

### Phase 0: Android clean-up (no KMP yet)

Behavior-neutral changes inside `:app`. This phase can ship as a normal release.

1. Delete the `Log.i("Success1", …)` leftovers (data layer) and the `Log` calls in `HomeScreen`, `HomeViewModel`, `SearchScreen` and `DetailsScreen`. Add Kermit for any logs worth keeping.
2. `SearchFiltersMapper`, `FiltersSheet`: `java.time` → kotlinx-datetime. Keep the injectable `today` parameter, since tests use it.
3. `Locale` / `NumberFormat` → small `internal` functions in one file (`ui/util/PlatformFormat.kt`), so each becomes a single `expect`/`actual` later.
4. `FilterOptionsRepositoryImpl` → `RegionProvider`. `PreferencesDataStoreDataSource` → `DeviceLocaleProvider`. `Context` leaves the data layer.
5. `LocalDateSerializer`: catch `IllegalArgumentException`.
6. `java.io.IOException` → `kotlinx.io.IOException` (add `kotlinx-io-core`).
7. `UserMessage`: remove `@StringRes` from the data layer.
8. Add `MediaLauncher` and route the Play button (`Rating.kt`) and the trailer chip (`Trailer.kt`) through it. The Android implementation runs today's `Intent` code.
9. Replace the 3 `material-icons-core` icons with vector drawables and drop the dependency.
10. Delete the Jsoup/Lottie scraping fallback (done in B2: without a `tmdbId`, `WebViewActivity` closes at once).

**Done when:** Maestro passes, the upgrade test passes, and no `android.*`/`java.*` imports remain in `data/` or `domain/`.

### Phase 1: Hilt → Koin

1. Koin modules that mirror the Hilt ones: dispatchers, network, database, DataStore, repositories, use cases, ViewModels.
2. `@Singleton` → `single`. `@ViewModelScoped` repositories → `factory`. `FilterOptionsRepositoryImpl` → `single` (it caches for the session). Dispatcher qualifiers → `named(...)`.
3. `hiltViewModel()` → `koinViewModel()`. `SavedStateHandle` still gets injected.
4. `startKoin { androidContext(this); modules(...) }` in `zedMoviesApplication`. Remove `@HiltAndroidApp`/`@AndroidEntryPoint`, the Hilt plugin and its dependencies, then `./gradlew clean`.
5. Add a Koin `verify()` unit test so missing bindings fail in CI.

**Done when:** Maestro and the upgrade test pass, and the app has no `dagger`/`javax.inject` imports.

### Phase 2: Retrofit/OkHttp → Ktor

1. Configure the `HttpClient` with `ContentNegotiation(json(defaultJson))`, `defaultRequest { url(API_URL); parameter("api_key", …) }` and `expectSuccess = false`.
2. `HttpClient.safeGet(...)`: 2xx → `Success.HttpResponse`, non-2xx → `Failure.HttpError`, `IOException` → `Failure.Error`, and `CancellationException` is rethrown. Delete `ResultAdapterFactory`, `ResultCall` and `CallDelegate`.
3. Rewrite the 3 services as classes with the same method signatures. `@QueryMap` becomes a loop over `parameter(k, v)`.
4. Coil: `coil-network-okhttp` → `coil-network-ktor3`. Remove Retrofit.
5. Add Ktor `MockEngine` tests for every result branch.

**Done when:** Maestro passes (posters, paging, search, filters, details, trailer chip), and the APK has no Retrofit classes.

### Phase 3: `:shared` with domain + data (targets: android, jvm)

1. `shared/build.gradle.kts`: `kotlin { android { namespace = "com.zobaer53.zedmovies.shared"; compileSdk = 37; minSdk = 26 }; jvm(); jvmToolchain(17) }`. No custom hierarchy yet: it comes with `wasmJs` (Phase 8). Add `include ':shared'` and have `:app` depend on `project(':shared')`.
2. Move code bottom-up and compile after each step: `domain/` → `data/common`, `data/model`, `data/mapper` → `data/network` → `data/database` → `data/datastore` → `data/paging`, `data/repository` → Koin modules. Keep the package names so `:app` imports don't change. All of it went to `commonMain`, since both targets support Room and DataStore. Only the platform actuals sit in `androidMain`/`jvmMain`.
3. **Room:**
   - Add `@ConstructedBy(zedMoviesDatabaseConstructor::class)` + `expect object zedMoviesDatabaseConstructor : RoomDatabaseConstructor<zedMoviesDatabase>`.
   - KSP per target: `kspAndroid`, `kspJvm`, later `kspIosArm64`/`kspIosSimulatorArm64`. Don't use `kspCommonMainMetadata`.
   - Move `app/schemas/…/2.json`, `3.json` → `shared/schemas/`. Version stays **3**, and `AutoMigration(2, 3)` stays.
   - **The Android builder must open the same file:** `context.getDatabasePath("zedMovies.db").absolutePath`. If the name or path changes, users lose their wish list.
   - Drivers: Android → `AndroidSQLiteDriver` (no new `.so`, so the 16 KB check stays green). JVM → `BundledSQLiteDriver`. `setQueryCoroutineContext(ioDispatcher)` on both.
   - `zedMoviesDatabaseTransactionProvider` → `useWriterConnection { it.immediateTransaction { … } }`. Drop `room-ktx`.
4. **DataStore:** `PreferenceDataStoreFactory.create` as a Koin `single` in each `platformModule`.
   - Android path: `filesDir/datastore/preferences.preferences_pb`. That's exactly where today's `preferencesDataStore(name = "preferences")` delegate writes.
   - JVM path: the OS app-data dir (see Phase 7).
5. `expect`/`actual`: `platformModule` (DB builder, DataStore, `DeviceLocaleProvider`, `RegionProvider`) and the HTTP engine (`httpEngine()`). The number and locale formatting in `ui/util/PlatformFormat.kt` became `expect` functions in Phase 4. There is no `ioDispatcher` yet: the platform modules use `Dispatchers.IO`, and web will need the `expect`.
6. `:app` keeps an Android Koin module (`di/AndroidAppModule.kt`) for `apiKeyProvider` from `BuildConfig`.
7. Move the 7 data test classes to `commonTest` (`kotlin.test`), add JVM tests for the Room DAOs, the 2→3 migration and the Koin graph in `jvmTest`, and run `./gradlew :shared:jvmTest`.

**Done when:** `:app` contains only UI, navigation, activities and Android DI; `:shared:jvmTest` passes; Maestro passes; and the **upgrade test** shows the wish list and language intact.

### Phase 4: UI to Compose Multiplatform (Android still the only consumer)

1. Add `org.jetbrains.compose` + `kotlin.plugin.compose` to `:shared`, with foundation, material3, components.resources, ui.tooling.preview, the JetBrains navigation/lifecycle artifacts, paging-compose and coil-compose.
2. Resources → `composeResources`. Switch `stringResource(R.string.x)` → `stringResource(Res.string.x)`, and the same for `painterResource`/`pluralStringResource`. That's about 150 call sites in 30 files.
3. Move code in this order: design system → `ui/smallcomponent` → screens (**Wishlist → Home → List → Details → Search**) → `navigation/` and `ZedMoviesApp`/`ZedMoviesAppState`.
4. `SystemBarsAppearance` → a common interface plus `@Composable expect fun rememberSystemBarsAppearance()`. The host provides `MediaLauncher` through `LocalMediaLauncher`.
5. Keep every `testTag` and `testTagsAsResourceId`, since Maestro relies on them.
6. `zedMoviesActivity` shrinks to `enableEdgeToEdge()` + splash + `setContent { zedMoviesApp(...) }`.
7. Add a JVM Compose UI smoke test (`runComposeUiTest`) that renders each screen with fake repositories. It runs in seconds, and desktop and web reuse it later.

**Done when:** all Maestro flows pass unchanged, screenshots match (home, details, search + filter sheet, wish list), and the APK has grown by no more than a few hundred KB.

### Phase 5: Adaptive large-screen + focus UI (shared, prerequisite for TV, desktop and web)

Build this in `commonMain` once, test it on Android tablets and in a desktop window (the `jvm` target already exists, so a throwaway `main()` is enough before Phase 7).

1. Add `LocalFormFactor`/`LocalInputMode`, `FocusableCard`, `ZedNavigationRail`, and focus-restoring rows and grids (see "Adaptive UI").
2. **Home:** the hero pager accepts left/right keys and pauses auto-advance while focused. Feeds become focusable rows. Pull-to-refresh → a refresh action when `InputMode != Touch`.
3. **See all (List):** a grid with column count chosen by window width. Paging keeps loading as focus reaches the end.
4. **Search:** the text field takes initial focus, and pressing down moves into the results. The filter sheet becomes a side panel on wide screens. Check that `RangeSlider`/`Slider` respond to arrow keys (**verify**). If they don't, add +/− steppers.
5. **Details:** a wide layout (backdrop + panel), initial focus on **Play**, and focus order Play → Trailer → Wish list → cast row.
6. **Back handling:** the system back key and Esc go through navigation, and focus returns to the item that opened the screen.
7. Phones: no visual change. Maestro must still pass unchanged.

**Done when:** every screen can be used with only arrow keys + Enter + Esc in a desktop window and on an Android tablet with a keyboard, and the phone UI is unchanged.

### Phase 6: Android TV (same APK)

1. **Manifest:**
   - `<uses-feature android:name="android.software.leanback" android:required="false"/>` and `android.hardware.touchscreen` `required="false"`.
   - Add `LEANBACK_LAUNCHER` to `zedMoviesActivity`'s intent filter and `android:banner="@drawable/tv_banner"`.
   - **Remove `screenOrientation="portrait"`.** Lock portrait at runtime instead, only when `LocalFormFactor == Phone`.
2. **TV banner**: 320×180, generated by `tools/branding/generate.py`.
3. **TV detection** in `zedMoviesActivity` → `LocalFormFactor.Tv`, `LocalInputMode.Directional`. Phase 5 provides the layouts.
4. **WebView playback in TV mode** (`ui/webview`):
   - Skip all `requestedOrientation` changes (`WebView.kt:46-49`, `TrailerActivity`).
   - **Virtual cursor:** the D-pad moves a pointer drawn over the WebView, and OK dispatches a tap (`MotionEvent`) at that point. The site's player sits in a cross-origin iframe, so it can't be driven reliably by key focus. A cursor works whatever the DOM looks like.
   - Media keys (play/pause, rewind, fast-forward): dispatch the matching key events (space, left/right) to the WebView as an enhancement, after the cursor works.
   - Back: exit HTML5 full screen, then go back in WebView history, then close the activity.
   - Missing or outdated WebView on cheap boxes: check `WebViewCompat.getCurrentWebViewPackage()` and show a clear message.
5. **Trailers on TV:** `Intent(ACTION_VIEW, "vnd.youtube:<key>")`, which the YouTube TV app handles. Fall back to `TrailerActivity`.
6. **Low-memory boxes (1–2 GB):** cap Coil's memory cache and request images at display size.
7. **Search input:** the on-screen TV keyboard works with the Compose text field. Voice search is out of scope.
8. **Tests:** run on an Android TV / Google TV emulator (API 34+). Add `.maestro/flows/tv/` that drives the app with `pressKey: Remote Dpad …` keys: home → details → play (cursor + OK) → back. `run.sh`'s landscape-player check already holds on TV. Test on one real device (Chromecast with Google TV or similar), including a 32-bit box if available.
9. Update the README and `docs/` FAQ ("Does it work on Android TV?").

**Done when:** the app shows in the TV launcher with its banner, everything works with the remote alone, playback starts and pauses with the remote, ads are blocked like on the phone, and phone Maestro still passes.

### Phase 7: Desktop (macOS, Windows, Linux)

**7a. Desktop app with browser playback**

1. `desktopApp/build.gradle.kts`: `org.jetbrains.compose` + `kotlin.plugin.compose`, depending on `:shared` (jvm). `compose.desktop { application { mainClass = "…MainKt"; nativeDistributions { targetFormats(Dmg, Msi, Deb); packageName = "NoFlix"; packageVersion = <CI version> } } }`. MSI needs `MAJOR.MINOR.BUILD` with MAJOR > 0. `3.x.y` is fine.
2. `main()`: call `startKoin` with the desktop platform module, then `Window(title = "NoFlix", state = rememberWindowState(…)) { zedMoviesApp(mediaLauncher = DesktopMediaLauncher) }`. Set a minimum window size.
3. App-data dir: `~/Library/Application Support/NoFlix`, `%APPDATA%\NoFlix`, or `$XDG_DATA_HOME/noflix`. Room (`BundledSQLiteDriver`) and DataStore live there.
4. API key: a Gradle task generates a `DesktopSecrets.kt` from `local.properties` (CI: a secret). Like the APK, the key ships inside the client.
5. Pointer support: hover states on cards, mouse-wheel horizontal scrolling for rows (Shift+wheel or a custom modifier), and scrollbars on wide grids.
6. `DesktopMediaLauncher.play()` → `Desktop.getDesktop().browse(streamUrl)`. Trailers open in the browser too. Ads are blocked by whatever blocker the user has in their browser. Say so in the UI.
7. Icons from `generate.py` (`.icns`, `.ico`, `.png`).
8. **CI:** a matrix job (macos/windows/ubuntu runners) runs `:desktopApp:packageDistributionForCurrentOS` and attaches the installers to the **same** GitHub release as `NoFlix.apk`. Without paid signing: macOS shows a Gatekeeper warning (Apple notarization needs a developer account) and Windows shows SmartScreen. Document the workaround in the README, or budget for signing.

**7b. Embedded player with ad-blocking (optional, after a spike)**

1. **Spike first:** embed Chromium via JCEF/KCEF (for example `compose-webview-multiplatform`) and play 10 sample titles. **Risk:** many CEF builds ship without proprietary codecs (H.264/AAC), which HLS streams need. If the spike fails, stay on 7a.
2. If it passes, block requests in `CefResourceRequestHandler.onBeforeResourceLoad`, inject `StreamPageStyle` + `extended-css`/`scriptlets` via `executeJavaScript` on load, and block pop-ups in `CefLifeSpanHandler.onBeforePopup`.
3. Blocking engine options:
   - (a) Make `:ad-filter`'s rule download/storage KMP (Ktor + files, with WorkManager only in `androidMain`) and compile `adblock-client`'s C++ for desktop JNI (CMake per OS in the CI matrix).
   - (b) A simpler JVM matcher for the network rules.
   - Expect weeks, not days.
4. Note the extra download size: about 100–200 MB of Chromium on first run or bundled.

**Done when (7a):** installers for all 3 OSes come out of CI, the app browses, searches and keeps the wish list across restarts, and Play opens the stream in the browser.

### Phase 8: Web (Kotlin/Wasm)

1. Add the `wasmJs { browser() }` target and a `webApp` entry: `ComposeViewport(document.body!!) { zedMoviesApp(mediaLauncher = WebMediaLauncher) }`. Add `index.html` with a loading screen, because the first load downloads several MB of wasm and Skia.
2. **Data on the web (`webMain`)**, with no Room and no OPFS:
   - Lists and see-all use network-only repositories built on the existing `TmdbPagingSource` (Paging if its wasm artifacts exist, **verify**; otherwise a small common "load more" pager).
   - Details: network only. `networkBoundResource` emits the network result.
   - Wish list + content language: `multiplatform-settings` (localStorage) behind the existing `WishlistRepository` interface and a `PreferencesSource` interface. The wish list is small, so one JSON blob per media type is enough.
   - This keeps the page free of COOP/COEP headers, so the YouTube trailer iframe keeps working.
3. **TMDB from the browser:** TMDB's API allows cross-origin requests (**verify** with a fetch from the dev server). The API key would be visible in the page source. Recommended: a tiny proxy (for example a Cloudflare Worker) that adds the key and caches responses. This also helps rate limits. Acceptable for a first test: the key in the bundle, like in the APK.
4. `WebMediaLauncher`: Play → `window.open(streamUrl, "_blank", "noopener")`. The app can't block ads inside another site's tab, so say so in the UI and suggest a browser ad-blocker. Trailer → a YouTube IFrame embed in a dialog.
5. Navigation: bind the nav controller to browser history so the back button and deep links (`/details/movie/123`) work.
6. **Hosting decision:**
   - (a) A sub-path of the existing GitHub Pages site (`docs/app/`). Note that `docs/` deploys on every push to `master`, and pushes that only touch `docs/` skip the APK release.
   - (b) A separate host (Cloudflare Pages / Netlify) deployed by its own workflow.
   - Option (b) keeps the public, link-to-stream site away from the repo's GitHub Pages and makes the proxy easy. **Recommended: (b).**
7. Accessibility and SEO: Compose for Web renders to a canvas, so search engines don't index it. Keep `docs/index.html` as the SEO landing page and link to the web app from it.
8. Browser support: WasmGC is needed (current Chrome, Edge, Firefox and Safari 18.2+). Show a fallback message on older browsers.
9. Tests: `:shared:wasmJsBrowserTest` for the common unit tests, plus a manual smoke checklist in Chrome, Firefox and Safari.

**Done when:** the web app browses, searches and filters, keeps the wish list across reloads, plays trailers in the page, opens Play in a new tab, and deploys from CI.

### Phase 9: iOS (later)

**Decision gate first:** decide how iOS is distributed. App Review will very likely reject in-app playback from a third-party site. The options are a browse-only App Store app, or full playback distributed outside the App Store (sideloading, AltStore, EU alternative marketplaces).

1. **Shell.** Add `iosArm64()`/`iosSimulatorArm64()` with a static `Shared` framework, plus `kspIos*` for Room. Create `iosApp/` (SwiftUI `@main`) linked through the `embedAndSignAppleFrameworkForXcode` build phase, and commit the shared scheme. Add `MainViewController() = ComposeUIViewController { zedMoviesApp(mediaLauncher = IosMediaLauncher) }`, with `startKoin` called from Swift. API key: `Secrets.xcconfig` (gitignored) → `Info.plist`. Minimum iOS 16. Lock the main UI to portrait (iPhone). Add `AppIcon` to `generate.py`. Check the bottom bar against the safe area.
2. **Trailers v1:** open the YouTube app or Safari. **v2:** the IFrame API in `WKWebView` via `loadHTMLString(html, baseURL: https://<bundle-id>)`, which avoids error 153.
3. **Playback (optional, high risk):**
   - A `WKWebView` controller with `allowsInlineMediaPlayback`, injecting `StreamPageStyle` as `WKUserScript`.
   - **Pop-up/navigation blocking first:** `createWebViewWith` returns `nil`, and cross-origin top-level navigations are cancelled. That catches most visible ads with little code.
   - Then cosmetic scripts, then `WKContentRuleList` network rules. Convert the AdGuard lists in CI (for example with AdGuard's SafariConverterLib at build time; check its license against Apache-2.0) and download the JSON at runtime. Each list is capped at about 150k rules.
   - Landscape on `/watch/` via `requestGeometryUpdate` (iOS 16+).
4. **CI:** a macOS job runs `:shared:iosSimulatorArm64Test` + `xcodebuild … -sdk iphonesimulator build`. Release artifacts depend on the distribution decision.
5. **Maestro on iOS:** flows 01–04 and 07 can run against the simulator. CMP maps `testTag` to the accessibility identifier.

## CI overview (end state)

| Job | Trigger | Runs | Publishes |
| --- | --- | --- | --- |
| Android (release, to be restored from the original repository) | push to `master` | `:app` build, signature check, 16 KB check, unit tests | `NoFlix.apk` (phone + TV) |
| Shared tests | every push / PR | `:shared:jvmTest` (+ `wasmJsBrowserTest`, `iosSimulatorArm64Test` once added), Koin `verify()`, `commonMain` import guard | — |
| Desktop | push to `master` | matrix `packageDistributionForCurrentOS` | `.dmg`, `.msi`, `.deb` on the same release |
| Web | push to `master` | `wasmJsBrowserDistribution` | deploy to the chosen host |
| iOS (later) | every push | simulator build + tests | depends on the distribution decision |

All jobs share the version computed by the existing release workflow (passed as `-PappVersionName`). Update the `paths-ignore` rules so desktop- and web-only changes still trigger their jobs.

## Effort estimate (one developer, rough)

| Phase | Estimate | Cumulative result |
| --- | --- | --- |
| 0. Android clean-up | 2–3 days | Leaner Android app |
| 1. Hilt → Koin | 2–3 days | |
| 2. Retrofit → Ktor | 2–3 days | |
| 3. `:shared` domain + data (android, jvm) | 3–5 days | Android on shared data |
| 4. Compose Multiplatform UI | 5–8 days | Android fully on KMP |
| 5. Adaptive large-screen + focus UI | 5–8 days | Tablets improved; ready for TV, desktop and web |
| 6. Android TV | 4–7 days | **TV in the same APK** |
| 7a. Desktop (browser playback) | 3–5 days | **Desktop installers** |
| 7b. Desktop embedded player + ad-block | 2–6 weeks, only after the spike | |
| 8. Web | 5–8 days (+1–2 for a proxy) | **Web app** |
| 9. iOS shell + trailers | 4–6 days | iOS browse app |
| 9. iOS playback + ad-block | 1–3 weeks (very uncertain) | |

- Android on KMP (phases 0–4): **~3–4 weeks**.
- With TV (adding phases 5–6): **~5–7 weeks**.
- With desktop 7a and web: **~7–10 weeks**.
- iOS and the embedded desktop player are on top of that.

Phases 0–2 are worth doing even if no other platform ships: less code, no reflection-based networking, no Hilt, no Android types in the data layer.

## Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Room or DataStore path changes in Phase 3 | Android users lose their wish list and language | Hard-code today's paths. Run `.maestro/upgrade.sh` against the last public APK at the end of every phase. |
| Mixed Compose versions after CMP (AGP 9 doesn't align them) | `NoSuchMethodError` crashes | Match the CMP version to the BOM and inspect `:app:dependencies`. `:app` keeps the BOM only for debug tooling and tests, at the same versions. |
| `java.*` creeping into `commonMain` while only JVM targets exist | Surprise rewrite when web or iOS is added | The CI import guard |
| Missing Koin binding | Runtime crash | Koin `verify()` test, plus Maestro on every screen |
| Ktor error mapping differs from `ResultCall` | Wrong error states | `MockEngine` tests; manual airplane-mode check |
| New native libraries break 16 KB alignment | Compatibility warning on Android 15+ | `AndroidSQLiteDriver` on Android; the existing CI `llvm-objdump` check |
| Manifest portrait lock left in place | TVs (and Play, if ever used) treat the app as phone-only | Lock portrait at runtime, phones only (Phase 6) |
| Stream player unusable with a remote | TV playback fails | Virtual cursor (Phase 6); test on a real TV device |
| Old or missing WebView on cheap TV boxes | Blank player | `WebViewCompat` check + message |
| CEF builds without H.264/AAC | Embedded desktop player can't play | Spike before 7b; 7a (system browser) is the fallback |
| Unsigned desktop installers | Gatekeeper/SmartScreen warnings | README instructions, or a paid signing budget |
| Web: OPFS needs COOP/COEP, which blocks the YouTube iframe | Broken trailers or no persistence | No Room on web; browser storage for the wish list |
| Web: API key visible in the browser | Key abuse or rate limiting | A small proxy that adds the key and caches |
| Web: public site links to the stream site | Takedown risk for the host or the repo's Pages | Host separately from the repo's GitHub Pages; decide before launch |
| Paging wasm support missing | Web lists can't page | Verify early in Phase 8; fall back to a small common pager |
| iOS: App Review | iOS playback can't ship on the App Store | Decision gate before Phase 9 |
| Stream site DOM changes | Page scripts break on every platform | Shared JS in `commonMain`: one fix covers all |

## Open decisions

1. **Android TV:** the same APK (recommended) or a separate TV app?
2. **DI:** Koin (recommended, used by the official guide) or Metro (compile-time checks)? **Decided: Koin (A2).**
3. **Desktop playback:** system browser only (7a, recommended to start), or invest in the embedded Chromium player with ad-blocking (7b) after the codec spike?
4. **Web hosting:** a separate host with a key-adding proxy (recommended) or a sub-path of the existing GitHub Pages site?
5. **iOS distribution:** a browse-only App Store app, or full playback outside the App Store?
6. **Scraping fallback:** delete Jsoup + Lottie (recommended) or keep it Android-only? **Decided: deleted in B2.**
7. **`values-night/strings.xml`:** delete it, or keep it as `values-dark`? **Decided: moved to `values-dark` in B5** (deleting it can come later).
