# NoFlix: Android KMP migration plan (Android only)

_Audited 2026-10-15 against the code in this folder. This is the detailed, Android-only part of [kmp-migration-plan.md](kmp-migration-plan.md), which covers Android TV, desktop, web and iOS later. Library versions are given as minimums or major lines. Check the latest stable release before each phase, and settle every **verify** item with a short spike before relying on it._

## Goal

Move the Android app onto a Kotlin Multiplatform structure **without users noticing anything**. At the end:

- All domain, data, ViewModel, navigation and screen code lives in a new `:shared` KMP module built on multiplatform libraries (Koin, Ktor, Room KMP, DataStore KMP, Paging, Coil 3, Compose Multiplatform).
- `:app` holds only what is truly Android: the activities, the WebView playback path with `:ad-filter`, `TrailerActivity`, the launcher icon, splash and manifest, and the Android DI module (API key, `Context`).
- The APK still ships through the existing release pipeline, from the same module, with the same application id and signing key.

### Non-goals

- No new platforms ship. Android TV, desktop, web and iOS are later phases of the overall plan.
- No new features or visual changes.
- `:ad-filter` and `:adblock-client` stay as they are.

### Success criteria (checked at the end of every phase)

| Check | How | Pass |
| --- | --- | --- |
| Functional | `.maestro/run.sh` (flows 01–07) on the shared Pixel 6a (Android 17, 4 KB pages). 16 KB page support is covered by the static alignment check below. | All pass, no logcat crash |
| Data survives update | `.maestro/upgrade.sh <last public APK>` (`1_add_wishlist` → install over → `2_check_wishlist`) | Wish list intact, no Room crash |
| Content language survives update | Set the device to a non-English locale before the old APK's first start, then install over and open Home | Titles still in that language |
| Unit tests | `./gradlew :app:testDebugUnitTest` (later `:shared:allTests`) | All pass |
| 16 KB alignment | `zipalign -c -P 16 -v 4` + `llvm-objdump -p` on every 64-bit `.so`, as in CI | Aligned |
| APK size | Compare with the baseline | ≤ +10% (target: smaller once Retrofit and Hilt are gone) |
| Cold start | `adb shell am start -W -n com.zobaer53.zedmovies/.zedMoviesActivity`, median of 10 | ≤ baseline + 10% |
| Visual | Screenshots of home, see-all, search + filter sheet, details (movie and TV), wish list | Matches the baseline |

## Baseline (do this first, before any change)

1. Build the current `master` as release and as debug. Record APK size, `.so` list, cold start (median of 10) and `./gradlew :app:dependencies --configuration releaseRuntimeClasspath > doc/local/deps-baseline.txt`.
2. Save the **last public release APK** (from GitHub Releases) as `doc/local/baseline.apk`. Every upgrade test installs over it.
3. Take the baseline screenshots on the shared Pixel 6a (Android 17, 4 KB pages, en-US, dark theme) and save them in `doc/local/screens-baseline/`. 16 KB page support is covered by the static alignment check, not on this device.
4. Run `.maestro/run.sh` and `.maestro/upgrade.sh doc/local/baseline.apk` and record that both pass.

`doc/local/` is for files that aren't committed. Check that it's ignored.

## Branching and release strategy

This plan runs in the **`zobaer53/KMP-Noflix`** repository. It has no release workflow yet, so merging doesn't publish anything. The rules below keep it shippable anyway: the original NoFlix release pipeline comes back here once the migration lands.

- **Branches:** `main` = stable and always releasable. `dev` = integration: every PR goes into `dev`. `main` only receives `dev`, after a set of work packages passes all success criteria on a real device. Feature branches are `feat/a-…` (stream A) and `feat/b-…` (stream B), one per work package (see "Parallel streams").
- **Checks:** `.github/workflows/pr-checks.yml` runs unit tests, the debug build and the KMP guard on every PR, and uploads the debug APK as an artifact. Install that artifact over the baseline APK on a real phone before merging anything that touches data (A1–A5).
- **Merge one work package at a time**, only after its gates pass. Then a regression can be pinned to one package.
- **Version line:** when A4 (Phase 3) merges, bump `versionName` in `app/build.gradle` to `3.1.0`, so bug reports show whether the KMP data layer is in use.
- **Rollback:** no package changes the DB schema (it stays at version 3) or the DB/DataStore file locations. Reverting a merge commit on `dev` is safe, and a build from the reverted `dev`, installed over the newer one, still reads the data.
- **Commit hygiene:** don't name the streaming site in commits, file names or identifiers. Files under `app/` are tracked normally in this repository (no `git add -f`).

## Parallel streams

Two Claude Code sessions work in parallel, each in its own **git worktree**. Setup, ownership and conflict rules are in `CLAUDE.md` → "Parallel streams". In short:

- **Stream A (core)** works in the main checkout `NoFlix_KMP/`. It owns data, domain, DI, Gradle, CI and scripts, and builds the shared core first.
- **Stream B (UI)** works in the worktree `NoFlix_KMP-b/`. It owns `ui/`, `navigation/`, resources, Maestro and UI tests, and builds the UI on top of that core.

Both branch from `dev`, never from each other's unmerged branches, and share one emulator through the device lock.

### Work packages

Each package is one branch and one PR into `dev`. **Exceptions** are the only lines a package may change in files the other stream owns. Every other edit stays inside the package's own stream's folders.

| ID | Branch | Scope (plan sections) | Depends on (merged) | Exceptions (other stream's files) |
| --- | --- | --- | --- | --- |
| **A1** | `feat/a-core-cleanup` | 0.1 data rows (`"Success1"` logs incl. the `toString()` side effects, `DispatchersModule`, `zedMoviesVersionProvider`, Timber); 0.2 except `UserMessage`; 0.5 `LocalDateSerializerTest` | — | none |
| **A2** | `feat/a-koin` | Phase 1 (Hilt → Koin, `KoinGraphTest`) | A1, **B2** (B2 edits the same screen files) | 5 screens: the `hiltViewModel` import + default-parameter line. 5 ViewModels: `@HiltViewModel`/`@Inject` lines + imports. `ZedMoviesActivity.kt`: the `@AndroidEntryPoint` line. |
| **A3** | `feat/a-ktor` | Phase 2 (Retrofit/OkHttp → Ktor, Coil network, `SafeRequestTest`, `MovieServiceTest`) | A2 | none |
| **A4** | `feat/a-shared-data` | Phase 3 (`:shared` with android + jvm, domain + data + Koin data modules, Room/DataStore KMP, CI guard, JVM DAO/migration tests, `versionName` 3.1.0). `di/ViewModelModule.kt` stays in `:app` (it references `:app` ViewModels). | A3 | none (package names are unchanged, so UI imports compile as they are) |
| **A5** | `feat/a-sqlite-driver` | 3.3b (`AndroidSQLiteDriver`) | A4, verified on a real device | none |
| **A6** | `feat/a-hardening` | Phase 5 items 2–5 (catalog/leftover clean-up, optional R8 + Baseline Profile, docs) | B6 | none |
| **B1** | `feat/b-baseline` | Baseline section, automated as `.maestro/baseline.sh` (APK size, `.so` list, cold start ×10, dependency snapshot, screenshot flow) → results in `doc/local/` | — | none |
| **B2** | `feat/b-ui-cleanup` | 0.1 UI rows (`"error1"`/`"error2"` logs), `UserMessage` → `ui/` as a sealed type (incl. `IOException` → `kotlinx.io` in `UserMessageMapper`), 0.3 (`PlatformFormat`, `MediaLauncher`, icon drawables), 0.4 if decided, 0.5 `UserMessageMapperTest` | B1 | Delete `data/model/UserMessage.kt` (moved to `ui/`) |
| **B3** | `feat/b-screenshot-tests` | **Addition:** JVM screenshot tests (Roborazzi + Robolectric) of every screen's stateless composable with fake UI state. They automate the "Visual" success criterion for B5/B6 and later move to `:shared`. | B2 | none |
| **B4** | `feat/b-gate-checks` | **Addition:** automate the remaining success criteria. `upgrade.sh` also checks that the content language survives (non-English locale before the old APK's first start). A process-death check (`am kill` on Search with filters, then reopen) for A2's gate. A cold-start comparison against the B1 baseline. | B1 | none |
| **B5** | `feat/b-cmp-foundation` | 4.1 (Compose Multiplatform in `:shared`), 4.2 (resources), 4.3 steps 1–3 (design system, `ui/smallcomponent`) | **A4** | `shared/build.gradle.kts`: the compose plugin, the compose dependency lines and the `compose.resources {}` block only |
| **B6** | `feat/b-cmp-screens` | 4.3 steps 4–8, 4.4, 4.5 (screens, navigation, `SystemBarsAppearance`, `MediaLauncher` → commonMain, slim `zedMoviesActivity`, JVM UI smoke test) | B5 | Move `di/ViewModelModule.kt` into `:shared` |

### Waves

| Wave | Stream A | Stream B | Notes |
| --- | --- | --- | --- |
| 1 | A1 | B1 → B2 | No overlap: A edits data/domain/di, B edits ui/. Both touch the catalog in different groups. |
| 2 | A2 (starts once B2 has merged) | B3 | A2's screen edits are one-liners |
| 3 | A3 | B4 | |
| 4 | A4 | reviews A4 and runs the B3/B4 gates on its branch artifact | B makes no code changes in data/domain/di while files move into `:shared` |
| 5 | A5 | B5 | B5 needs `:shared` from A4 |
| 6 | A6 (after B6) | B6 | A6 removes catalog entries only once B6 no longer uses them |

## Audit summary (Android-relevant)

| Item | Count / location | Notes |
| --- | --- | --- |
| Kotlin under `app/src/main` | ≈14,900 lines | No Java |
| Classes with `@Inject constructor` | 41: 19 data, 17 use cases, 5 ViewModels | All constructor injection, which maps 1:1 to Koin's `factoryOf`/`viewModelOf` |
| Hilt modules | `DatabaseModule`, `DataStoreModule`, `NetworkModule`, `RepositoryModule` (ViewModel-scoped), `DispatchersModule` | **`DispatchersModule` has no consumers.** No class injects `@Dispatcher(...)`. |
| `zedMoviesVersionProvider` | Provided in `DatabaseModule` | **Unused.** Delete it. |
| Retrofit endpoints | 20 in 3 services (`MovieService`, `TvShowService`, `FilterOptionsService`) | `@QueryMap` used for discover |
| `android.util.Log` outside playback | 12 in data (`"Success1"`), 14 in UI (`"error1"`, `"error2"`) | `ZedMoviesResult.*.toString()` **logs as a side effect**. Remove all of them. |
| `R.*` references outside playback | ≈223 in 29 files | All format args are already positional (`%1$s`, `%1$d`), which Compose resources require |
| Strings | 138 in `values/`, 27 in `values-night/` | `values-night` looks like accidental duplicates (open decision) |
| `SavedStateHandle` | Details and List (nav args), Search (filters as a JSON string) | No `Parcelable`/`@Parcelize` anywhere, so it carries over to KMP unchanged |
| DB | `zedMovies.db`, version 3, `AutoMigration(2, 3)`, schemas `2.json` and `3.json` | **The file name must not change** |
| DataStore | `preferencesDataStore(name = "preferences")` → `files/datastore/preferences.preferences_pb` | **The path must not change** |
| Tests | `SearchFiltersMapperTest`, `SearchFiltersMatcherTest`, `FilterOptionsMapperTest`, `TmdbPagingSourceTest`, `ExampleUnitTest` | JUnit 4 asserts + coroutines-test |
| R8 | `minifyEnabled false` | No keep rules to maintain during the migration |

## Target module layout (Android-only end state)

```
app/                                   com.android.application (built-in Kotlin) – unchanged name and path
  src/main/java/com/zobaer53/zedmovies/
    ZedMoviesActivity.kt               enableEdgeToEdge, splash, setContent { zedMoviesApp(...) }
    ZedMoviesApplication.kt            startKoin { androidContext; modules(androidAppModule + sharedModules) }
    di/AndroidAppModule.kt             apiKeyProvider (BuildConfig), MediaLauncher
    playback/AndroidMediaLauncher.kt   starts WebViewActivity / TrailerActivity
    ui/webview/…                       unchanged
    ui/trailer/…                       unchanged
  src/main/res/                        mipmaps, splash theme, themes.xml, app_name, raw/ (if scraping kept)
shared/                                kotlin.multiplatform + com.android.kotlin.multiplatform.library
  src/commonMain/kotlin/com/zobaer53/zedmovies/
    domain/ data/ di/ navigation/ ui/  moved from :app, same package names
    platform/                          expect declarations (formatting, locale, system bars)
  src/commonMain/composeResources/     strings, drawables, fonts
  src/androidMain/kotlin/…/platform/   actuals + database/DataStore builders
  src/jvmMain/kotlin/…/platform/       actuals for the JVM guard/test target (never shipped)
  src/commonTest/                      unit tests (kotlin.test)
  src/jvmTest/                         Room DAO + migration tests on the JVM
  schemas/                             Room schemas (moved from app/schemas)
```

**Why a `jvm()` target in an Android-only plan?** With `android` as the only target, `commonMain` is compiled just like `androidMain`. `android.*` and `java.*` imports would compile there without any error, and the code would only look portable. A second target (the JVM is the cheapest) makes the compiler reject Android APIs in `commonMain`. It also gives fast host tests: Room DAO tests with an in-memory bundled SQLite, without a device. The `jvm` target is never packaged or shipped. It also becomes the desktop target later.

---

## Phase 0: Clean up inside `:app` (no new modules)

Small, behavior-neutral commits. They can ship as an ordinary patch release.

### 0.1 Remove debug logging and dead code

| Change | Files |
| --- | --- |
| Delete all `Log.i("Success1", …)`, including the logging inside the four `toString()` overrides (keep the `toString()` bodies) | `data/common/result/ZedMoviesResult.kt`, `data/network/common/NetworkBoundResource.kt`, `data/network/source/TvShowDetailsNetworkDataSource.kt`, `data/paging/TvShowRemoteMediator.kt` |
| Delete the `Log.i("error1"/"error2", …)` calls | `ui/home/HomeScreen.kt` (6), `ui/search/SearchScreen.kt` (6), `ui/details/DetailsScreen.kt`, `ui/home/HomeViewModel.kt` |
| Delete `DispatchersModule`, `zedMoviesDispatchers`/`Dispatcher` qualifiers (nothing injects them) | `data/common/dispatcher/**` |
| Delete `zedMoviesVersionProvider` and its `@Provides` | `data/database/util/ZedMoviesVersionProvider.kt`, `di/DatabaseModule.kt` |
| Remove `timber` from the catalog (unused) | `gradle/libs.versions.toml` |

Need a log afterwards? Add **Kermit** (`co.touchlab:kermit`) and use `Logger.withTag("NoFlix")`. Keep logs out of `toString()`.

### 0.2 Replace JVM/Android APIs in data and domain

| File | Today | Change to |
| --- | --- | --- |
| `data/mapper/SearchFiltersMapper.kt` | `java.time.LocalDate.now()`, `LocalDate.of(y, 1, 1)` | `kotlinx.datetime.LocalDate(y, 1, 1)`. Keep the `today` parameter, defaulting to `Clock.System.todayIn(TimeZone.currentSystemDefault())` |
| `data/network/serializer/LocalDateSerializer.kt` | `catch (ParseException)` (never thrown) | `catch (e: IllegalArgumentException)`. Check today's behavior with a test for `"2024-13-40"` first, then keep it (the exception becomes `SerializationException`). |
| `data/paging/MovieRemoteMediator.kt`, `TvShowRemoteMediator.kt`, `TmdbPagingSource.kt`, `ui/smallcomponent/mapper/UserMessageMapper.kt`, `TmdbPagingSourceTest` | `java.io.IOException` | `kotlinx.io.IOException` (add `org.jetbrains.kotlinx:kotlinx-io-core`). It's a typealias on the JVM, so OkHttp's exceptions still match. |
| `data/repository/FilterOptionsRepositoryImpl.kt` | `Locale.getDefault().country` | Inject `RegionProvider { fun region(): String }`. Android implementation: `Locale.getDefault().country` |
| `data/datastore/PreferencesDataStoreDataSource.kt` + `util/LocaleUtils.kt` | `@ApplicationContext Context`, `ConfigurationCompat` | Inject `DeviceLocaleProvider { fun language(): String }`. The Android implementation holds the `Context`. |
| `data/model/UserMessage.kt` | `@StringRes val messageResourceId: Int` | `sealed interface UserMessage { data object NoInternet; data object Unknown; … }`. `UserMessageMapper` maps it to a string in the UI. |

Put the two provider interfaces in `data/platform/` and their Android implementations in `di/` for now. They move to `androidMain` in Phase 3.

### 0.3 Isolate Android-only UI calls

| File | Today | Change to |
| --- | --- | --- |
| `ui/search/FiltersSheet.kt` | `java.time.Year.now()`, `java.util.Locale` | `currentYear()` + `displayLanguageName(code)` from `ui/util/PlatformFormat.kt` |
| `ui/search/MoreFilters.kt` | `java.text.NumberFormat` | `formatCount(n)` in the same file |
| `ui/smallcomponent/Rating.kt`, `ui/details/components/Trailer.kt` | `Intent` + `LocalContext.startActivity` | `LocalMediaLauncher.current.play(...)` / `.playTrailer(key)` |
| `ui/search/ActiveFilters.kt`, `MoreFilters.kt`, `SearchScreen.kt` | `Icons.Rounded.Close`, `KeyboardArrowUp/Down` | `ic_close.xml`, `ic_arrow_up.xml`, `ic_arrow_down.xml` vector drawables. Remove `material-icons-core`. |

`ui/util/PlatformFormat.kt` holds every formatting function that will need a different implementation per platform:

```kotlin
internal fun currentYear(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year
internal fun displayLanguageName(isoCode: String): String = Locale(isoCode).getDisplayLanguage(Locale.getDefault())
internal fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)
```

The `MediaLauncher` abstraction:

```kotlin
// ui/playback/MediaLauncher.kt (moves to commonMain in Phase 4)
data class PlayRequest(val tmdbId: Int, val type: String, val name: String, val year: String)

interface MediaLauncher {
    fun play(request: PlayRequest)
    fun playTrailer(youtubeKey: String)
}

val LocalMediaLauncher = staticCompositionLocalOf<MediaLauncher> { error("No MediaLauncher provided") }
```

```kotlin
// app: playback/AndroidMediaLauncher.kt – today's Intent code, moved, not rewritten
class AndroidMediaLauncher(private val context: Context) : MediaLauncher {
    override fun play(request: PlayRequest) = context.startActivity(
        Intent(context, WebViewActivity::class.java)
            .putExtra("movieName", request.name).putExtra("movieYear", request.year)
            .putExtra("type", request.type).putExtra("tmdbId", request.tmdbId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)   // only if context is not an Activity
    )
    override fun playTrailer(youtubeKey: String) = context.startActivity(TrailerActivity.intent(context, youtubeKey))
}
```

Provide it in `zedMoviesActivity` with `CompositionLocalProvider(LocalMediaLauncher provides AndroidMediaLauncher(this))`. That passes the **Activity** context, so no `NEW_TASK` flag is needed. Copy the exact extras from `Rating.kt` rather than retyping them.

### 0.4 Optional: remove the scraping fallback

The Jsoup path (`ui/webview/RunOn Background.kt`, `scrapeMovieData()`) and the Lottie animation (`raw/data.json`) only run when `tmdbId == 0`. Every item reaching the Play button comes from TMDB and has an id. Deleting them removes Jsoup and Lottie from the APK (**open decision**, recommended). If you keep them, they stay in `:app` untouched.

### 0.5 Unit tests added in Phase 0

- `LocalDateSerializerTest`: valid, empty and invalid dates.
- `UserMessageMapperTest` (JVM): `IOException` → `NoInternet`, anything else → `Unknown`.

**Gate:** all success criteria. `grep -rE '^import (android|java)\.' app/src/main/java/com/zobaer53/zedmovies/{data,domain}` returns nothing. The only `javax.inject` left is removed in Phase 1.

---

## Phase 1: Hilt → Koin

### 1.1 Dependencies

Catalog: `koin-bom`, `koin-core`, `koin-android`, `koin-compose`, `koin-compose-viewmodel`, `koin-test` (and `koin-test-junit4` if needed). Remove: `hilt-android`, `hilt-compiler`, `androidx-hilt-lifecycle-viewmodel-compose`, plugin `com.google.dagger.hilt.android`. **KSP stays** for Room.

### 1.2 Modules (mirror the Hilt graph 1:1)

Hilt scopes map to Koin like this:

| Hilt | Today | Koin |
| --- | --- | --- |
| `@Singleton @Provides` | `zedMoviesDatabase`, `DataStore<Preferences>`, `zedMoviesApi` | `single` |
| Unscoped `@Provides` | DAOs, services, `apiKeyProvider` | `factory` (DAO getters are cheap) or `single` for services |
| Unscoped `@Inject` classes | data sources, use cases, `PreferencesDataStoreDataSource`, transaction provider | `factoryOf(::X)` |
| `@ViewModelScoped @Binds` | 5 repositories | `factoryOf(::XImpl) bind X::class` (one per ViewModel, as today) |
| `@Singleton` class + unscoped `@Binds` | `FilterOptionsRepositoryImpl` | `singleOf(::FilterOptionsRepositoryImpl) bind FilterOptionsRepository::class` |
| `@HiltViewModel` | 5 ViewModels | `viewModelOf(::XViewModel)` |

```kotlin
// di/KoinModules.kt (moves to shared/commonMain in Phase 3, except androidModule)
val networkModule = module {
    single { zedMoviesApi(get()) }
    single { get<zedMoviesApi>().movieService }
    single { get<zedMoviesApi>().tvShowService }
    single { get<zedMoviesApi>().filterOptionsService }
}
val databaseModule = module {
    factory { get<zedMoviesDatabase>().movieDao }
    factory { get<zedMoviesDatabase>().tvShowDao }
    factory { get<zedMoviesDatabase>().movieRemoteKeyDao }
    factory { get<zedMoviesDatabase>().tvShowRemoteKeyDao }
    factory { get<zedMoviesDatabase>().movieDetailsDao }
    factory { get<zedMoviesDatabase>().tvShowDetailsDao }
    factory { get<zedMoviesDatabase>().wishlistDao }
    factoryOf(::zedMoviesDatabaseTransactionProvider)
}
val dataSourceModule = module {
    factoryOf(::MovieDatabaseDataSource); factoryOf(::TvShowDatabaseDataSource)
    factoryOf(::MovieDetailsDatabaseDataSource); factoryOf(::TvShowDetailsDatabaseDataSource)
    factoryOf(::WishlistDatabaseDataSource); factoryOf(::PreferencesDataStoreDataSource)
    factoryOf(::MovieNetworkDataSource); factoryOf(::TvShowNetworkDataSource)
    factoryOf(::MovieDetailsNetworkDataSource); factoryOf(::TvShowDetailsNetworkDataSource)
    factoryOf(::FilterOptionsNetworkDataSource)
}
val repositoryModule = module {
    factoryOf(::MovieRepositoryImpl) bind MovieRepository::class
    factoryOf(::TvShowRepositoryImpl) bind TvShowRepository::class
    factoryOf(::MovieDetailsRepositoryImpl) bind MovieDetailsRepository::class
    factoryOf(::TvShowDetailsRepositoryImpl) bind TvShowDetailsRepository::class
    factoryOf(::WishlistRepositoryImpl) bind WishlistRepository::class
    singleOf(::FilterOptionsRepositoryImpl) bind FilterOptionsRepository::class
}
val useCaseModule = module { /* factoryOf(::GetMoviesUseCase) … all 17 */ }
val viewModelModule = module {
    viewModelOf(::HomeViewModel); viewModelOf(::ListViewModel); viewModelOf(::SearchViewModel)
    viewModelOf(::DetailsViewModel); viewModelOf(::WishlistViewModel)
}

// :app only
val androidModule = module {
    single { Room.databaseBuilder(androidContext(), zedMoviesDatabase::class.java, zedMoviesDatabase.zedMovies_DATABASE).build() }
    single { androidContext().preferencesDataStore }          // same delegate as today, same file
    single<zedMoviesApiKeyProvider> { object : zedMoviesApiKeyProvider { override val apiKey = BuildConfig.API_KEY } }
    single<DeviceLocaleProvider> { AndroidDeviceLocaleProvider(androidContext()) }
    single<RegionProvider> { AndroidRegionProvider() }
}
```

### 1.3 Wiring

- `zedMoviesApplication`: remove `@HiltAndroidApp` and call `startKoin { androidLogger(Level.ERROR); androidContext(this@…); modules(androidModule, networkModule, …) }`.
- `zedMoviesActivity`: remove `@AndroidEntryPoint`.
- Five screens: replace `hiltViewModel()` → `koinViewModel()` (`org.koin.compose.viewmodel.koinViewModel`).
- `SavedStateHandle`: `viewModelOf` passes the `SavedStateHandle` from the `NavBackStackEntry`'s creation extras, so the nav arguments (`id`, `mediaType`, list type) and Search's saved filters keep working. **Verify** with Maestro flows 02, 04 and 07, and with process death: `adb shell am kill` while on Search with filters applied, then reopen.
- Remove every `@Inject`/`javax.inject` import (41 classes), then run `./gradlew clean` so stale Hilt sources don't linger.

### 1.4 Tests

```kotlin
class KoinGraphTest {
    @Test fun graphIsComplete() = module { includes(allModules) }
        .verify(extraTypes = listOf(Context::class, SavedStateHandle::class, CreationExtras::class))
}
```

**Gate:** all success criteria, plus the process-death check above. No `dagger`, `hilt` or `javax.inject` left in `:app`.

---

## Phase 2: Retrofit/OkHttp → Ktor

### 2.1 Dependencies

Add: `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-okhttp` (engine; reuses OkHttp, so connection behavior and timeouts stay as they are today), `ktor-client-mock` (tests), and `coil-network-ktor3`. Remove: `retrofit`, `retrofit-kotlinx-serialization`, `coil-network-okhttp`, and the direct `okhttp` dependency (the engine pulls it in).

### 2.2 Client

```kotlin
// data/network/util/HttpClient.kt
internal fun zedMoviesHttpClient(apiKeyProvider: zedMoviesApiKeyProvider, json: Json = defaultJson) =
    HttpClient(OkHttp) {                         // engine moves to androidMain in Phase 3
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        defaultRequest {
            url(Constants.API_URL)
            url.parameters.append(Constants.API_KEY_QUERY_PARAM, apiKeyProvider.requireApiKey())
        }
    }
```

### 2.3 Result mapping (replaces `ResultAdapterFactory`, `ResultCall`, `CallDelegate`)

```kotlin
// data/network/util/SafeRequest.kt
internal suspend inline fun <reified T> HttpClient.safeGet(
    path: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): ZedMoviesResult<T> = try {
    val response = get(path) { block() }
    val url = response.request.url.withoutApiKey()
    if (response.status.isSuccess()) {
        ZedMoviesResult.success(response.body<T>(), response.status.value, response.status.description, url)
    } else {
        ZedMoviesResult.failure(HttpException(statusCode = response.status.value, statusMessage = response.status.description, url = url))
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {                // IOException, SerializationException, … – same as ResultCall.onFailure
    ZedMoviesResult.failure(e)
}

private fun Url.withoutApiKey() = URLBuilder(this).apply { parameters.remove(Constants.API_KEY_QUERY_PARAM) }.buildString()
```

**Behavior to keep, case by case:**

| Case | Retrofit today | Ktor |
| --- | --- | --- |
| 2xx | `Success.HttpResponse(value, code, message, url)` | Same. `url` without `api_key`. `call.request()` was the request before the interceptor, so it never contained the key. |
| 4xx/5xx | `Failure.HttpError(HttpException(code, message, url))` | Same |
| No network / timeout | `Failure.Error(IOException)` → "no internet" message | Ktor/OkHttp throw `IOException` subclasses → same branch. **Verify** that `HttpRequestTimeoutException` (only thrown with the `HttpTimeout` plugin, which isn't installed) never occurs. |
| Bad JSON | `Failure.Error(SerializationException)` | Same |
| Coroutine cancelled | Propagated | Rethrown explicitly |

### 2.4 Services

The three interfaces become classes with **the same method signatures**, so the network data sources and tests don't change:

```kotlin
class MovieService internal constructor(private val client: HttpClient) {
    suspend fun getUpcoming(language: String, page: Int = DEFAULT_PAGE) =
        client.safeGet<MovieResponseDto>(UPCOMING_MOVIE) { parameter(LANGUAGE, language); parameter(PAGE, page) }

    suspend fun discover(filters: Map<String, String>, language: String, page: Int = DEFAULT_PAGE) =
        client.safeGet<MovieResponseDto>(DISCOVER_MOVIE) {
            filters.forEach { (key, value) -> parameter(key, value) }
            parameter(LANGUAGE, language); parameter(PAGE, page)
        }

    suspend fun getDetailsById(id: Int, language: String, includeVideoLanguage: String,
                               appendToResponse: String = DETAILS_APPEND_TO_RESPONSE) =
        client.safeGet<NetworkMovieDetails>(DETAILS_MOVIE.replace("{$ID}", id.toString())) {
            parameter(LANGUAGE, language); parameter(INCLUDE_VIDEO_LANGUAGE, includeVideoLanguage)
            parameter(APPEND_TO_RESPONSE, appendToResponse)
        }

    suspend fun search(query: String, language: String, page: Int = DEFAULT_PAGE, primaryReleaseYear: Int? = null) =
        client.safeGet<MovieResponseDto>(SEARCH_MOVIE) {
            parameter(QUERY, query); parameter(LANGUAGE, language); parameter(PAGE, page)
            primaryReleaseYear?.let { parameter(Discover.PRIMARY_RELEASE_YEAR, it) }   // Retrofit skipped nulls
        }
    // … the other 16 endpoints the same way
}
```

Watch for: Retrofit **dropped `null` query params**. Ktor's `parameter(key, null)` also skips them, but keep the explicit `?.let` for clarity. Paths in `Constants.Path` have no leading `/`. Keep it that way, so they resolve against `API_URL`'s `/3/`.

`zedMoviesApi` keeps one `HttpClient` (`single`) and exposes the three services as before.

### 2.5 Coil

Swap `coil-network-okhttp` → `coil-network-ktor3`. It registers itself through the service loader, as the OkHttp one does. If the app builds a custom `ImageLoader`, pass `KtorNetworkFetcherFactory()` explicitly. **Verify** that posters load on a clean install (Maestro 01 + screenshots).

### 2.6 Tests

- `SafeRequestTest` with `MockEngine`: 200 + body, 200 + bad JSON, 401, 404, `IOException`, cancellation, and the `api_key` stripped from `url`.
- `MovieServiceTest`: the discover `QueryMap` produces the expected query string, and the null year is omitted.
- `TmdbPagingSourceTest` stays unchanged.

**Gate:** all success criteria, the APK contains no `retrofit2` classes (`apkanalyzer dex packages`), and a manual airplane-mode check shows the "no internet" message on Home and Search.

---

## Phase 3: Create `:shared` and move domain + data

### 3.1 Gradle

**Catalog additions:**

```toml
[libraries]
androidx-sqlite-framework = { module = "androidx.sqlite:sqlite-framework", version.ref = "sqlite" }   # AndroidSQLiteDriver
androidx-sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }       # jvm tests only
androidx-paging-common = { module = "androidx.paging:paging-common", version.ref = "paging" }
androidx-datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "datastore" }
kotlinx-io-core = { … }
[plugins]
android-kotlin-multiplatform-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
```

Use the `sqlite` version that Room 2.8.5 itself depends on.

**`settings.gradle`:** `include ':shared'`.

**`shared/build.gradle.kts`:**

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")            // no version: KGP 2.4.21 is already on the root buildscript classpath
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
        withHostTest { }                                  // verify exact DSL for host (unit) tests in AGP 9
    }
    jvm()                                                 // guard + fast tests, never shipped
    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom)); implementation(libs.koin.core)
            implementation(libs.ktor.client.core); implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.androidx.room.runtime); implementation(libs.androidx.room.paging)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.coroutines.core); implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime); implementation(libs.kotlinx.io.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp); implementation(libs.koin.android)
            implementation(libs.androidx.sqlite.framework); implementation(libs.androidx.datastore.preferences)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.okhttp); implementation(libs.androidx.sqlite.bundled) }
        commonTest.dependencies {
            implementation(kotlin("test")); implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock); implementation(libs.koin.test)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}

room { schemaDirectory("$projectDir/schemas") }
```

If Gradle reports that `org.jetbrains.kotlin.multiplatform` is "already on the classpath with an unknown version", the line above (without a version) is the fix. Never add `kotlin-android` to `:app`; `:app` keeps AGP's built-in Kotlin.

**`app/build.gradle`:** `implementation project(':shared')`. Remove what moved (`room-*`, `paging-runtime`, `datastore`, `ktor-*`, `kotlinx-datetime`, `koin-core`). Keep `koin-android`, `koin-compose(-viewmodel)` until Phase 4. `:app` keeps the `ksp` and `room` plugins only until the database has moved (step 3.3), then drops them.

### 3.2 Move order (compile after each step, one commit each)

1. `domain/model`, `domain/repository`, `domain/usecase` → `shared/src/commonMain/kotlin/com/zobaer53/zedmovies/domain/`.
2. `data/common/result`, `data/model`, `data/util`, `data/mapper` → commonMain.
3. `data/network/**` (models, serializers, services, data sources, `HttpClient` factory without the engine) → commonMain. `HttpClient(OkHttp)` becomes `HttpClient(httpEngine())`, with `expect fun httpEngine(): HttpClientEngine` and `actual` = `OkHttp.create()` in both `androidMain` and `jvmMain`.
4. `data/database/**` → commonMain (see 3.3).
5. `data/datastore/**` → commonMain (see 3.4).
6. `data/paging/**`, `data/network/common/NetworkBoundResource.kt`, `data/repository/**` → commonMain.
7. The Koin modules from Phase 1 → `shared/commonMain/di/`, except the platform bindings, which go to `expect val platformModule: Module` with `actual`s in `androidMain` and `jvmMain`.
8. Tests → `shared/src/commonTest` (JUnit asserts → `kotlin.test.assertEquals` and so on, `@Test` from `kotlin.test`).

Keep package names unchanged. `:app`'s imports then compile without edits.

### 3.3 Room KMP

```kotlin
// commonMain
@Database(entities = [...], version = 3, exportSchema = true, autoMigrations = [AutoMigration(from = 2, to = 3)])
@TypeConverters(ListConverter::class, LocalDateConverter::class, CreditsConverter::class)
@ConstructedBy(zedMoviesDatabaseConstructor::class)
abstract class zedMoviesDatabase : RoomDatabase() { /* DAOs unchanged */ }

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object zedMoviesDatabaseConstructor : RoomDatabaseConstructor<zedMoviesDatabase>

internal fun RoomDatabase.Builder<zedMoviesDatabase>.buildZedMovies(): zedMoviesDatabase =
    setQueryCoroutineContext(Dispatchers.IO).build()
```

```kotlin
// androidMain – SAME FILE as today: <app data>/databases/zedMovies.db
internal fun androidDatabaseBuilder(context: Context) = Room.databaseBuilder<zedMoviesDatabase>(
    context = context.applicationContext,
    name = context.getDatabasePath(zedMoviesDatabase.zedMovies_DATABASE).absolutePath
)
```

Driver, in two separate releases:

- **3.3a (this phase):** don't call `setDriver` on Android. Room keeps today's `SupportSQLiteOpenHelper` path, so runtime behavior is identical. Only the code structure changes.
- **3.3b (separate small release after 3.3a is stable):** `setDriver(AndroidSQLiteDriver())`. It still uses the platform SQLite, so no new `.so` and the 16 KB check stays green. Run the upgrade test again.

Never use `BundledSQLiteDriver` in the APK. It adds a native library that would need its own 16 KB check, and Android gets nothing from it. On the JVM it's used for tests only.

Other Room changes:

- **Transactions:** `zedMoviesDatabaseTransactionProvider.runWithTransaction` → `database.useWriterConnection { it.immediateTransaction { block() } }`. `withTransaction` (`room-ktx`) is Android-only. Remove `room-ktx`.
- **Schemas:** move `app/schemas/com.zobaer53.zedmovies.data.database.zedMoviesDatabase/2.json`, `3.json` → `shared/schemas/…` (same subfolder). Diff the regenerated `3.json` after the first build. **It must be identical.** Any difference means the entities changed by accident.
- **TypeConverters:** `CreditsConverter` and `ListConverter` use kotlinx-serialization, and `LocalDateConverter` uses kotlinx-datetime. They work in common code unchanged.
- **Paging:** DAO `PagingSource` return types work through `room-paging` in common code (**verify** that `room-paging` 2.8.5 has a `jvm` artifact; if not, put the paging DAOs behind an Android/JVM-specific source set).

New JVM tests (`shared/src/jvmTest`):

- `WishlistDaoTest`: insert, query by media type, `isWishlisted`, delete (in-memory `BundledSQLiteDriver`).
- `MovieDaoPagingTest`: `getPagingByMediaType` returns pages in insertion order.
- `MigrationTest`: open a v2 database created from `2.json` and migrate it to v3 (`room-testing` `MigrationTestHelper` on the JVM, **verify** KMP support; otherwise keep this as an Android instrumented test in `:app`).

### 3.4 DataStore KMP

```kotlin
// commonMain
internal fun createPreferencesDataStore(path: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path().toPath() })

// androidMain – SAME FILE as today's preferencesDataStore(name = "preferences")
single { createPreferencesDataStore { androidContext().preferencesDataStoreFile("preferences").absolutePath } }
```

Make it a Koin `single`. Two DataStores on one file throw at runtime. Remove the `Context.preferencesDataStore` delegate from `:app` in the same commit.

### 3.5 Platform actuals

| `expect` (commonMain) | `androidMain` | `jvmMain` |
| --- | --- | --- |
| `platformModule: Module` | DB builder, DataStore path, `DeviceLocaleProvider` (Context), `RegionProvider` | In-memory DB, temp-dir DataStore, `Locale` |
| `httpEngine(): HttpClientEngine` | `OkHttp.create()` | `OkHttp.create()` |

`:app` keeps `androidAppModule`: `apiKeyProvider` from `BuildConfig` and `MediaLauncher`.

### 3.6 CI guard

Add to the workflow (and to `.maestro/run.sh` as a pre-check):

```bash
! grep -rnE '^import (android|androidx\.core|java|javax)\.' shared/src/commonMain
```

`androidx.room`, `androidx.paging`, `androidx.datastore` and `androidx.lifecycle` are multiplatform, so they're allowed.

**Gate:**

- All success criteria. **The upgrade test is the critical one.** Also run it with a wish list of 10+ items, both movies and TV.
- `./gradlew :shared:allTests` passes.
- `shared/schemas/…/3.json` is unchanged.
- `:app` contains only `ui/`, `navigation/`, activities and the Android DI module.
- Bump `versionName` to `3.1.0` on merge.

---

## Phase 4: UI to Compose Multiplatform

### 4.1 Gradle

- Add `org.jetbrains.compose` (version = the Compose Multiplatform release) and `org.jetbrains.kotlin.plugin.compose` to `:shared`.
- `commonMain` dependencies: `compose.runtime`, `compose.foundation`, `compose.material3`, `compose.ui`, `compose.components.resources`, `compose.components.uiToolingPreview`, `org.jetbrains.androidx.navigation:navigation-compose`, `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` and `lifecycle-runtime-compose`, `androidx.paging:paging-compose`, `coil-compose`, `coil-network-ktor3`, `koin-compose`, `koin-compose-viewmodel`.
- `jvmMain`: `compose.desktop.currentOs` (for JVM UI tests only).

**Compose version alignment:** choose the CMP release whose Jetpack Compose baseline is **≥ the versions in BOM `2026.09.00`**. Then remove the BOM and the explicit `androidx.compose.*` / `navigation-compose` / `lifecycle-*-compose` dependencies from `:app`, and let `:shared` supply them. Compare `:app:dependencies --configuration releaseRuntimeClasspath` with the baseline: every `androidx.compose.*` artifact must resolve to a single version. AGP 9 doesn't align versions for you (`android.dependency.useConstraints=false`).

```kotlin
compose.resources {
    packageOfResClass = "com.zobaer53.zedmovies.resources"
    publicResClass = false
}
```

### 4.2 Resources

| From `app/src/main/res` | To `shared/src/commonMain/composeResources` |
| --- | --- |
| `values/strings.xml` (138 strings + 2 plurals) | `values/strings.xml`. Keep a copy of `app_name` in `:app` (manifest label). |
| `values-night/strings.xml` | Per the open decision: drop it, or move to `values-dark/strings.xml` |
| `drawable/ic_*.xml`, `no_search_results.xml`, `no_wishlist_results.xml`, the 3 new icons | `drawable/` |
| `font/montserrat_{regular,medium,semibold}.ttf` | `font/` |
| `mipmap-*`, `zed_movies_round_*`, `themes.xml`, `colors.xml`, `ids.xml`, `raw/` | **Stay** in `:app` |

**Verify** that the `\'` escapes (5 strings) render as `'`. Format args are already positional, so no changes are needed there.

API switch, done with find-and-replace one file at a time (≈223 refs, 29 files):

| Android | Compose Multiplatform |
| --- | --- |
| `androidx.compose.ui.res.stringResource(R.string.x, a)` | `org.jetbrains.compose.resources.stringResource(Res.string.x, a)` |
| `pluralStringResource(R.plurals.x, n, a)` | `pluralStringResource(Res.plurals.x, n, a)` |
| `painterResource(R.drawable.x)` | `painterResource(Res.drawable.x)` |
| `Font(R.font.x, FontWeight.Medium)` | `Font(Res.font.x, FontWeight.Medium)`. It's `@Composable`, so build `zedMoviesTypography` inside `zedMoviesTheme` with `remember`. |
| `@StringRes val textResourceId: Int` (`TopLevelDestination`, `GenreMapper`, …) | `val text: StringResource` |
| `@DrawableRes val iconResourceId: Int` | `val icon: DrawableResource` |

### 4.3 Move order (compile + run the app after each step)

1. `ui/designsystem/theme` (`Color`, `Shape`, `Spacing`, `Type`, `Ripple`, `Theme`). **Verify** `RippleAlpha` in CMP's `material-ripple`, or switch to M3 `RippleConfiguration`.
2. `ui/designsystem/component` (13 files).
3. `ui/smallcomponent/**` including mappers and `util/Paging.kt`.
4. Screens, smallest first, each with its `Event`/`UiState`/`ViewModel`/`navigation/*Destination.kt`: **Wishlist → Home → List → Details → Search**.
5. `navigation/` (`ZedMoviesNavHost`, `TopLevelDestination`, `zedMoviesNavigationDestination`) + `ui/component/ZedMoviesBottomBar.kt` + `ui/ZedMoviesApp.kt`, `ZedMoviesAppState.kt`.
6. `ui/SystemBarsAppearance.kt` → `expect fun SystemBarsAppearance(color: Color)` in commonMain. The `actual` in `androidMain` is today's code (`LocalView` → `Window` → `WindowCompat`), and the `jvmMain` `actual` is a no-op.
7. `ui/util/PlatformFormat.kt` → `expect` functions. Today's bodies become the `androidMain` actuals, and the `jvmMain` actuals reuse the same JDK calls.
8. `MediaLauncher` + `LocalMediaLauncher` → commonMain. `AndroidMediaLauncher` stays in `:app`.

Things that stay identical:

- **Navigation:** string routes and `navArgument` work as today. Type-safe routes are a later, optional change.
- **ViewModels:** `androidx.lifecycle.ViewModel`, `viewModelScope` and `SavedStateHandle` come from the JetBrains multiplatform artifacts and resolve to the same AndroidX classes on Android.
- **Paging:** `collectAsLazyPagingItems()` and `cachedIn(viewModelScope)` are unchanged.
- **Test tags:** every `testTag` and `Modifier.semantics { testTagsAsResourceId = true }` stays exactly where it is. Maestro flow 07 depends on them.
- **Previews:** `@Preview` from `org.jetbrains.compose.ui.tooling.preview` in commonMain (Android Studio renders it). Debug tooling (`ui-tooling`) stays as `debugImplementation` in `:app`.

### 4.4 `:app` after Phase 4

```kotlin
class zedMoviesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalMediaLauncher provides remember { AndroidMediaLauncher(this) }) {
                zedMoviesApp()
            }
        }
    }
}
```

`:app` dependencies shrink to: `:shared`, `:ad-filter`, `activity-compose`, `core-splashscreen`, `androidx.webkit`, `koin-android`, `youtube-player-core`, (Lottie/Jsoup if kept), and debug tooling.

### 4.5 Tests

- A JVM Compose UI smoke test (`runComposeUiTest` in `shared/jvmTest`) that renders each screen with fake repositories and asserts key texts and test tags. It runs in seconds and catches resource or `Res` mistakes before Maestro does.
- Maestro: all flows unchanged.
- Screenshots compared with the baseline: home, see-all, search + filter sheet (each filter section expanded), details (movie and TV, with and without the trailer chip), wish list (empty and filled), both tabs.

**Gate:** all success criteria, every `androidx.compose.*` on a single version, and APK growth within a few hundred KB.

---

## Phase 5: Clean-up and hardening

1. **Optional: switch to `AndroidSQLiteDriver`** (step 3.3b) if not done yet, as its own release.
2. **Delete leftovers:** `app/schemas` (moved), unused catalog entries (Retrofit, Hilt, Timber, `material-icons-core`, `room-ktx`), and `ExampleUnitTest`/`ExampleInstrumentedTest`.
3. **Optional: enable R8** (`minifyEnabled true`). This is a separate change with its own release. Add keep rules for kotlinx-serialization (`@Serializable` classes, `$$serializer`), Ktor and Koin (`viewModelOf` uses constructor references, which is R8-safe). Room needs no extra rules. Measure the APK size and run full Maestro.
4. **Optional: Baseline Profile** for startup, now that the startup path changed (Koin + CMP).
5. **Docs:** update `CLAUDE.md`:
   - Architecture: the `:shared` module, Koin, Ktor, Room KMP with `ksp<Target>`, Compose resources.
   - Build & run: `:shared:allTests`, the CI guard.
   - Requirements: the CMP version, and the note about applying the multiplatform plugin without a version.

   Update `doc/kmp-migration-plan.md` progress, and the README's tech list.

---

## Timeline (one developer)

| Phase | Commits (approx.) | Estimate | Ships as |
| --- | --- | --- | --- |
| Baseline | — | ½ day | — |
| 0. Clean-up | 6–8 | 2–3 days | 3.0.x patch release |
| 1. Koin | 4–5 | 2–3 days | 3.0.x |
| 2. Ktor | 4–6 | 2–3 days | 3.0.x |
| 3. `:shared` data | 8–10 | 3–5 days | **3.1.0** |
| 3b. `AndroidSQLiteDriver` | 1 | ½ day | 3.1.x |
| 4. CMP UI | 10–12 | 5–8 days | 3.1.x (or 3.2.0) |
| 5. Hardening | 3–6 | 1–3 days | 3.x |
| **Total** | | **~3–4 weeks** | |

## Progress

Claim a row ("In progress") in the first commit of the package's branch. Edit only your own stream's rows.

| ID | Stream | Status | PR | Gates |
| --- | --- | --- | --- | --- |
| Setup | — | Done (2026-10-15): repository, `main`/`dev`, worktrees, device lock, pre-push hook, PR checks | — | Lock tested: concurrent, stale, nested and killed runs |
| A1 | A | Not started | | |
| A2 | A | Not started | | |
| A3 | A | Not started | | |
| A4 | A | Not started | | |
| A5 | A | Not started | | |
| A6 | A | Not started | | |
| B1 | B | In progress | | |
| B2 | B | Not started | | |
| B3 | B | Not started | | |
| B4 | B | Not started | | |
| B5 | B | Not started | | |
| B6 | B | Not started | | |

## Risks (Android)

| Risk | Phase | Impact | Mitigation |
| --- | --- | --- | --- |
| DB file opened at a different path or name | 3 | Users lose their wish list | Builder uses `getDatabasePath("zedMovies.db")`. Upgrade test with 10+ items, plus a real-device install over the public release. |
| DataStore file path changes or a second instance | 3 | Content language resets, or a crash | `preferencesDataStoreFile("preferences")`; Koin `single`; remove the old delegate in the same commit |
| Regenerated Room schema differs | 3 | Room throws a migration error at startup | Diff `3.json`; `MigrationTest` |
| Transaction semantics change (`withTransaction` → `useWriterConnection`) | 3 | Duplicate or missing rows in see-all paging | RemoteMediator refresh test on the JVM, and Maestro flow 02 (paging + pull-to-refresh) |
| Missing Koin binding or `SavedStateHandle` not passed | 1 | Crash when opening a screen | `verify()` test; Maestro covers every screen; process-death check |
| Ktor error mapping differs | 2 | Wrong "no internet" or error states | `MockEngine` tests per branch; airplane-mode check |
| API key leaks into stored `url` or logs | 2 | Key exposure in bug reports | `withoutApiKey()`; no request logging in release |
| Two Compose versions on the runtime classpath | 4 | `NoSuchMethodError` crash | Single-version check of `releaseRuntimeClasspath`; remove the BOM from `:app` |
| Compose resources render strings differently (escapes, plurals) | 4 | Wrong text | Screenshot diff; check the 5 `\'` strings and both plurals |
| `android.*` creeping into `commonMain` | 3–4 | Hidden work later for TV, desktop and web | `jvm()` target + CI import guard |
| New native libraries | 3 | 16 KB warning on Android 15+ | No `sqlite-bundled` in the APK; the existing CI `llvm-objdump` check |
| A package merged to `dev` too early | all | A broken integration branch, and the other stream builds on it | PR checks, the debug APK artifact on a real device for data packages, one package per merge, `scripts/stream.sh test-merge` before every merge |
| Parallel PRs conflict | all | GitHub skips checks on a conflicting PR | Folder ownership + listed exceptions, `sync` (merge, never rebase), shared-file rules in `CLAUDE.md` |
| Both streams using the emulator at once | all | Flaky Maestro runs, wrong APK installed | Per-device lock in the git common dir (`run.sh`, `upgrade.sh`, `stream.sh with-device`) |

## Open decisions (Android)

1. **Scraping fallback** (Jsoup + Lottie): delete it in Phase 0 (recommended), or keep it in `:app`?
2. **`values-night/strings.xml`:** delete it, or move it to `values-dark`?
3. **Driver switch** (3.3b): do it right after Phase 3 (recommended), or leave Android on the compatibility path?
4. **R8:** enable it in Phase 5, or leave it for later?
5. **DI:** Koin (recommended) or Metro?
