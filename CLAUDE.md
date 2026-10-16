# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

NoFlix (Gradle root project `zeDMovies`, package `com.zobaer53.zedmovies`) is an Android app that browses movies/TV shows from The Movie DB and plays them ad-free in a WebView pointed at a third-party streaming site. The Android app module (`:app`: activities, WebView playback, trailer player, Android DI) sits on top of a Kotlin Multiplatform module (`:shared`: domain, data, DI, ViewModels, navigation and the Compose Multiplatform UI). Two vendored ad-blocking library modules complete the project (`:ad-filter`, `:adblock-client`, originally Edsuns/AdblockAndroid).

## Build & run

```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:installDebug         # install on connected device/emulator
./gradlew :app:assembleRelease      # signed release APK (needs signing keys in local.properties)
./gradlew :app:testDebugUnitTest    # JVM unit tests (also renders the screenshot tests, without comparing)
./gradlew :shared:jvmTest           # :shared's tests on its JVM target (commonTest + jvmTest: data layer, Room DAOs + migration on the bundled SQLite, Koin/ViewModel graphs, screen smoke test)
scripts/kmp-guard.sh                # no Android/JVM APIs in shared/src/commonMain (CI runs it too)
./gradlew :app:verifyRoborazziDebug   # screenshot tests: compare the screens with app/src/test/screenshots/
./gradlew :app:recordRoborazziDebug   # re-record those reference images after an intended UI change
./gradlew :ad-filter:connectedDebugAndroidTest :adblock-client:connectedDebugAndroidTest   # the ad-block modules' instrumented tests (device required; :app has none)
./gradlew :ad-filter:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.edsuns.adfilter.RuleIteratorTest  # single test class
.maestro/run.sh [--no-build] [flow.yaml ...]   # Maestro UI suite on the connected device (see below)
.maestro/upgrade.sh [--content-language] old.apk [new.apk]   # install-over test: data (and the content language) must survive an update
.maestro/baseline.sh [--name NAME]            # sizes, cold start, screenshots, suite + upgrade into doc/local/; any NAME but baseline is compared with it
```

There is no lint/format config beyond Android defaults. `:shared:jvmTest` covers the search filters (`SearchFiltersMapperTest`, `FilterOptionsMapperTest`, `SearchFiltersMatcherTest`), `TmdbPagingSourceTest`, `MovieService` and `safeGet` (Ktor `MockEngine`), the Room DAOs and migration, `UserMessageMapper`, and the Koin and ViewModel graphs (`SharedKoinGraphTest`, `ViewModelGraphTest`). `:app:testDebugUnitTest` has `KoinGraphTest` and the screenshot tests; `:shared:jvmTest` also renders every screen on Compose Desktop (`ui/ScreensSmokeTest`: texts and test tags, no pixels). Screenshot tests (`app/src/test/…/ui/screenshot/`, Roborazzi + Robolectric on SDK 35) render each screen's stateless composable (public in `:shared` for these tests) with fake state in the dark theme and compare it with the committed PNGs. The references are recorded on macOS, and CI only renders them (text rendering differs between operating systems), so run `verifyRoborazziDebug` locally; the vendored ad-block modules have `ChecksumTest` (`:ad-filter:testDebugUnitTest`, not run by CI) and the upstream instrumented `RuleIteratorTest` and `AdBlockClientTest`. The real regression check is the **Maestro suite** in `.maestro/flows/` (home, see-all paging + pull-to-refresh, search + tabs, details + wish list, playback, trailer, search filters). `run.sh` clean-installs the debug APK, runs the flows (a full run then also kills the app in the background on Search with a filter and checks the restored task still shows it), and fails on logcat crashes, a lost Search state, a non-landscape player, any 64-bit `.so` that isn't 16 KB aligned, fewer than 6 installed filter lists, or the injected element-hiding script never finishing (the last checks rely on debug-only logs: `NoFlixAdBlock: blocked …` and the ad-filter JS `console.log`s). Maestro clears logcat at the start of every flow, so `run.sh` streams logcat to a file for the whole run instead of dumping it at the end; with several devices attached, set `ANDROID_SERIAL` (both scripts pass it to Maestro as `--device`). Start emulators with `-gpu host`: under memory pressure the emulator silently falls back to software rendering and the app then ANRs. Start the API 37 (Android 17) emulator with `-gpu host -feature GuestAngle`: plain `-gpu host` on a Mac only offers OpenGL ES 3.0, Chromium 145 asks for an ES 3.1 context (`EGL_BAD_CONFIG: no ES 3.1 support`) and its GPU process aborts (`FATAL: gl_version_info.cc … VERSION =`) on YouTube and the stream player.

Requirements:
- JDK 17+, Gradle 9.7.1, AGP 9.4.1 with **built-in Kotlin** (no `kotlin-android` plugin; never re-add it or `kotlin-kapt`), Kotlin 2.4.21 pinned through the root `buildscript` classpath, Compose compiler = the `org.jetbrains.kotlin.plugin.compose` plugin at the Kotlin version, KSP for Room (in `:shared`). `:shared` applies `org.jetbrains.kotlin.multiplatform` without a version (KGP is on the root classpath), `com.android.kotlin.multiplatform.library` and Compose Multiplatform (`org.jetbrains.compose`, version `composeMultiplatform`), which generates the Compose resources (`Res`). Its Compose libraries are the `jetbrains-compose-*` catalog entries (`composeMultiplatform`; Material 3 uses `composeMultiplatformMaterial3`), and on Android they resolve to the androidx artifacts. All versions live in `gradle/libs.versions.toml`, including `compileSdk` (37), `ndk` and `cmake`.
- compileSdk 37, targetSdk 37, minSdk 26 (Android 8.0). Android 7.x can't download the filter lists or play (missing TLS roots), and with 26 `java.time` (kotlinx-datetime) needs no core library desugaring.
- NDK r28c (`28.2.13676358`) + CMake 3.22.1 for `:adblock-client`'s native library. NDK r28+ is what makes the 64-bit `.so` 16 KB-page aligned. Every native library in the APK must stay 16 KB aligned (AndroidX ones too), or Android 15+ 16 KB devices run the app in compat mode with a warning.
- AGP 9 no longer aligns compile and runtime classpath versions (`android.dependency.useConstraints=false`), so declare the versions you actually ship (for example the `androidx.navigation` pin in `app/build.gradle`, which lifts :shared's JetBrains navigation to androidx 2.10.2) rather than relying on a transitive bump.
- `local.properties` must contain `API_KEY=<TMDB key>`; `app/build.gradle` reads it into `BuildConfig.API_KEY` and fails to configure without the file. Optional `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` enable release signing; without them the release build is unsigned.
- Repositories are only `google()`, `mavenCentral()` and `gradlePluginPortal()`. JCenter, the Aliyun mirror and JitPack are gone (Anko, Mezzanine and Edsuns HttpRequest were removed).

## Releases / CI

**This repository (`zobaer53/KMP-Noflix`, the KMP migration) has no release workflow yet.** Its only workflow is `.github/workflows/pr-checks.yml` (unit tests + debug build + the KMP guard on every PR into `dev`/`main` and every push to `dev`; needs the `TMDB_API_KEY` repository secret). Pushing to `main` does **not** publish anything here. The paragraph below describes the original NoFlix repository's release pipeline, which comes back once this migration ships.

`.github/workflows/release.yml` (original repo) runs on every push to `master` (except pushes that only touch `docs/`, `tools/` or Markdown): it computes the next version (latest `X.Y.Z` git tag with patch +1, or `versionName` from `app/build.gradle` if that is higher), passes `-PappVersionCode`/`-PappVersionName` to Gradle (versionCode = `X*10000 + Y*100 + Z`), verifies the APK is signed with the release cert (`CN=zeD, O=NoFlix`) and that it's 16 KB page-size aligned (`zipalign -P 16` + `llvm-objdump` on every 64-bit `.so`), and publishes a GitHub release with `NoFlix.apk`. To start a new minor/major line, bump `versionName` in `app/build.gradle`. Pushing to `master` therefore ships a public release.

Files under `app/` are tracked normally (the original repo's `/app/` ignore rule was dropped here, so `git add -f` is not needed).

The landing page website (https://zobaer53.github.io/NoFlix/) lives in another project, not in this repository. The README's images (logo, product card, architecture diagram, screenshots) and the repository's social preview are in `images/`.

Launcher icon, README logo and social preview are generated by `tools/branding/generate.py` (cairosvg + Pillow; see its docstring). The icon shapes are duplicated in `tools/branding/render.py` and `app/src/main/res/drawable/zed_movies_round_*.xml`, so change both together.

Do not name the streaming website in commit messages, release notes, or identifiers/file names — use neutral names like `STREAM_BASE_URL` / `StreamPageStyle.kt`. The URL itself stays in code.

## Parallel streams (KMP migration)

The Android KMP migration (`doc/kmp-android-plan.md`) runs as **two Claude Code sessions in parallel, one per git worktree** of this repository: two working folders on one `.git`, each with its own branch. Work is split by **folder ownership**, so the two streams rarely touch the same lines.

| | Stream A: core | Stream B: UI |
| --- | --- | --- |
| Folder | `NoFlix_KMP/` (main checkout) | `NoFlix_KMP-b/` (worktree) |
| Owns | `data/`, `domain/`, `di/`, `ZedMoviesApplication.kt`, Gradle files (`build.gradle`, `settings.gradle`, `gradle/`, `app/build.gradle`, `shared/build.gradle.kts`), `shared/src/*/…/{data,domain,di,platform}`, Room schemas, `.github/`, `scripts/` | `ui/`, `navigation/`, `ZedMoviesActivity.kt`, `app/src/main/res/`, `.maestro/`, UI tests, `shared/src/*/…/{ui,navigation}`, `composeResources/` |
| Work packages | A1–A6 in the plan's "Parallel streams" section | B1–B5 in the same section |

**Which stream am I?** Read `.stream.env` in the working folder (`STREAM=A` or `STREAM=B`). It also sets `ANDROID_SERIAL`. Only edit files your stream owns. When a work package needs a line in the other stream's files, the plan names that exception explicitly (for example, A2's one-line `hiltViewModel()` → `koinViewModel()` swaps in screens). Make no other edits there.

Rules that keep parallel PRs from conflicting:

- **Branch from `dev`, PR into `dev`.** `main` only receives `dev`. Start every package with `scripts/stream.sh start feat/<a|b>-<topic>`. **Never build on the other stream's unmerged branch.** If you need something it hasn't merged yet, wait for the merge.
- **Shared files** (`gradle/libs.versions.toml`, `app/build.gradle`, `CLAUDE.md`, the plan docs) are edited so that parallel PRs touch different lines:
  - Add catalog entries in their group, in alphabetical order. Never reorder or reformat existing lines.
  - In `app/build.gradle`, add dependencies next to their group.
  - In the plan, edit only your own work-package rows.
  - Strings go under the feature's own `<!-- feature -->` comment in `strings.xml`, never at the end of the file.
- **Claim before you start.** Set your package's row in the plan to "In progress" and push that row with your first commit.
- **On conflict, merge `origin/dev` into the branch (`scripts/stream.sh sync`). Never rebase or force-push a pushed branch.** The `pre-push` hook (`scripts/git-hooks/`, active in every worktree through `core.hooksPath`) blocks force pushes and direct pushes to `main`/`dev`. GitHub doesn't run PR checks on a conflicting head.
- **PRs merge themselves when green (auto-merge).** Before opening a PR, run `scripts/stream.sh test-merge [--maestro]`: it merges the latest `origin/dev`, then runs unit tests + debug build (+ the Maestro suite). Then push, open the PR into `dev` and turn on auto-merge in one step with `scripts/stream.sh pr "<title>" <body-file>`. GitHub merges it (merge commit, branch deleted) once the `build` check passes **on a branch that is up to date with `dev`**. That rule is the `dev` ruleset, which the repo admin can bypass. If the other stream merges first, the PR shows "out of date" and waits: run `scripts/stream.sh sync` (rerun the checks locally if `dev` touched your area) and push, and auto-merge fires after the new checks pass. Don't turn on auto-merge for a PR that still needs the user's decision or a real-device check (A1–A5 data packages: install the PR's debug APK artifact over the baseline APK first).
- **Devices are shared.** Both streams test on the **one device named in `.stream.env`** (`ANDROID_SERIAL`). That is currently a real Pixel 6a over USB (`2B171JEGR11436`, Android 17). **Don't start an emulator** unless the user asks for one: 16 GB of RAM doesn't fit an emulator plus two Gradle builds. `.maestro/run.sh` and `upgrade.sh` take a per-device lock in `$(git rev-parse --git-common-dir)/stream-locks/<serial>` (`mkdir` is atomic; a dead PID frees it), so a run waits while the other stream's run holds the device. Wrap any other command that installs or drives the app with `scripts/stream.sh with-device -- <cmd>`. Never call `maestro`/`adb install` directly, and always target the serial from `.stream.env` (other devices may be attached).
- **Avoid building both streams at the same moment** when you can: each Gradle daemon takes up to 4 GB (`org.gradle.jvmargs`).

Setup (once; both done by `scripts/stream.sh`):

```bash
scripts/stream.sh init A <adb-serial>                                  # main checkout = stream A
scripts/stream.sh worktree ../NoFlix_KMP-b feat/b-baseline B <adb-serial>   # second folder = stream B
scripts/stream.sh init <A|B> <adb-serial>                              # switch a stream to another device
scripts/stream.sh status                                                # worktrees, streams, device locks
```

`worktree` branches from `origin/dev` (or the local `dev` before the first push) with `--no-track`, writes `.stream.env`, copies the gitignored `local.properties` (TMDB key, SDK path), and symlinks `doc/local/` to the main checkout's copy. Both streams therefore share the baseline APKs and local notes.

## Architecture

Follows Google's recommended layered architecture (data → domain → ui) with Koin DI. Domain, data and the data DI modules live in the Kotlin Multiplatform module **`:shared`** (`shared/src/commonMain`, targets Android and a JVM target that is only used for host tests), under the same package names as before. Phase 4 (B5) also moved the UI models (`data/model`, which carry Compose `StringResource`s), the design system and `ui/smallcomponent` there, with the strings, icons and fonts in `shared/src/commonMain/composeResources` (`Res` in `com.zobaer53.zedmovies.resources`, internal to `:shared`). B6 moved the screens, ViewModels, navigation, the app shell (`zedMoviesApp`, `ZedMoviesAppState`) and `di/ViewModelModule.kt` too; their Android-only bits are `expect`/`actual` under `ui/` (`SystemBarsAppearance`, `ui/util/PlatformFormat.kt`, `ui/util/PlatformUi.kt`: `testTagsAsResourceId`, the system Cancel label; `ui/smallcomponent/mapper/ReleaseDateMapper.kt`: localized date format). `:app` keeps only the activities (`zedMoviesActivity`, `ui/webview`, `ui/trailer`), `zedMoviesApplication`, `playback/AndroidMediaLauncher.kt` and `di/AndroidAppModule.kt` (API key).

- **data/** (`:shared`) — Ktor (OkHttp engine, kotlinx-serialization) TMDB services in `data/network`, Room KMP DB in `data/database` (schemas in `shared/schemas/`), Preferences DataStore in `data/datastore`. Repositories (`data/repository/*Impl`) combine them:
  - Lists use Paging 3 `RemoteMediator`s (`data/paging`) that cache TMDB pages into Room with remote-key tables; search and filtered discover (Search tab filters, `doc/search-filters-plan.md`) use `TmdbPagingSource` (network only).
  - Details use `networkBoundResource` (`data/network/common/NetworkBoundResource.kt`): emit cached Room data, fetch, save, re-emit from Room.
  - Requests go through `HttpClient.safeGet` (`data/network/util/SafeRequest.kt`), which returns `ZedMoviesResult<T>` (success/failure/loading) instead of throwing.
  - Each layer has its own model types (`Network*` → `*Entity` → `domain/model/*Model` → `data/model`, the UI models), with mappers in `data/mapper`, `data/database/mapper`, and `ui/smallcomponent/mapper`.
- **domain/** (`:shared`) — repository interfaces and one-method use cases consumed by ViewModels.
- **di/** — Koin. `:shared`'s `sharedModules` = the data modules (`di/KoinModules.kt`) + `platformModule` (`expect`/`actual`: Room database and DataStore on the same files as before, locale/region providers). The app starts Koin in `zedMoviesApplication` with `androidAppModule` (API key from `BuildConfig.API_KEY`) and `viewModelModule` (in `:shared`, not part of `sharedModules`). `KoinGraphTest`, `SharedKoinGraphTest` and `ViewModelGraphTest` check the graph is complete.
- **navigation/** (`:shared`, top-level package, not under `ui/`) — JetBrains navigation-compose with string routes (`:app` pins androidx navigation 2.10.2). `ZedMoviesNavHost`, `TopLevelDestination` (bottom-bar tabs, rendered by `ui/component/ZedMoviesBottomBar.kt`) and the destination base interface.
- **ui/** — Compose Multiplatform in `:shared` (except `ui/webview` and `ui/trailer` in `:app`). `zedMoviesActivity` → `zedMoviesApp` (state in `ZedMoviesAppState`) → `ZedMoviesNavHost`. Each feature (`home`, `list`, `search`, `details`, `wishlist`) has `Screen`, `ViewModel`, `UiState`, an `Event` (except `list`), and a `navigation/*Destination.kt`. Shared design system lives in `ui/designsystem` (theme accessed via `zedMoviesTheme.colors/spacing/typography`); reusable widgets in `ui/smallcomponent`; both are in `:shared`.

Naming quirk: many classes/functions use a lowercase `zedMovies` prefix (`zedMoviesActivity`, `zedMoviesApplication`, `zedMoviesApi`, `zedMoviesTheme`) while their files are `ZedMovies*.kt`. The manifest references the lowercase names.

### Playback (WebView) path

Playback is separate from the Compose nav graph:

1. The "Play" button (`ui/smallcomponent/Rating.kt`, in `:shared`) calls `LocalMediaLauncher` (`ui/playback/MediaLauncher.kt`, commonMain); the app's `playback/AndroidMediaLauncher` starts `ui/webview/WebViewActivity` with extras `movieName`, `movieYear`, `type`, `tmdbId`.
2. `buildStreamUrl()` (`ui/webview/StreamUrl.kt`) builds `STREAM_BASE_URL/watch/<id>` (movies) or `/tv/<id>` (TV) from the `tmdbId`. The site is a client-rendered SPA keyed by TMDB id. Without a `tmdbId` the activity closes at once (the old Jsoup scraping fallback was removed).
3. `VideoWebView` (`ui/webview/WebView.kt`) hosts an Android `WebView` in Compose: installs AdGuard filter subscriptions on first run, wires `AdFilter` into `MyWebClient` (`ContentBlockingWebViewClient.kt`) for request blocking and cosmetic scripts, handles HTML5 full-screen via `onShowCustomView`, and locks orientation: landscape on `/watch/` pages and full-screen video, portrait everywhere else.
4. `StreamPageStyle.kt` injects CSS/JS into the site: a document-start script (sets the site's welcome-popup flag before its scripts run) and an idempotent page script that hides site chrome and stretches the player iframe on `/watch/` pages. Because the site is an SPA, it hooks `history.pushState`/`replaceState` and uses a `MutationObserver`. Selectors depend on the live site's DOM and break when it changes.

`WebViewActivity` declares `configChanges` for orientation/screen size in the manifest so rotation doesn't recreate it and reload the page.

### Trailers

The details screen shows a "Trailer" chip next to Play when TMDB has a YouTube trailer. The details request appends `videos` with `include_video_language=<content language>,en,null` (without it TMDB returns no videos for non-English content languages). `data/mapper/VideoMapper.kt` picks the key (trailers before teasers, content language before English, official first), and it's cached in Room as `trailer_key` (DB v3, AutoMigration 2→3). `ui/trailer/TrailerActivity` plays it with `com.pierfrancescosoffritti.androidyoutubeplayer:core`, which wraps YouTube's IFrame embed in a WebView and sets the origin YouTube requires (a bare embed WebView gets "Error 153"). It's a separate activity with `configChanges`, so it rotates without reloading while the main activity stays portrait; YouTube's full-screen button switches to landscape and hides the bars. Videos that can't be embedded open in the YouTube app or browser instead. On the API 37 emulator, playback needs `-feature GuestAngle` (see Build & run).

### Ad-blocking modules

- `:ad-filter` (`io.github.edsuns.adfilter`) — Kotlin API (`AdFilter.create/get`, `FilterViewModel`); downloads and installs filter lists with WorkManager (`DownloadWorker`, `InstallationWorker`) and injects element-hiding/scriptlet JS that `script/JsAssets.kt` reads from `src/main/assets/adfilter/` at runtime. `scriptlets.min.js` is generated from `scriptlets_src/` with `gulp`; `extended-css.min.js` is vendored from AdGuard ExtendedCss. The workers open the filter store with `BinaryDataStore.of(context)`; never create `AdFilter` from a worker: its init blocks on a WorkManager query queued behind that worker, deadlocks while holding the singleton lock, and the next `WebViewActivity` launch ANRs. Lists are tens of MB (processed up to ~27 MB), so they never go through the Java heap: downloads stream to the file, `Checksum.of(InputStream)` streams, and `AdBlockClient`'s `File` overloads read and write in native code (small heaps, e.g. 48 MB on old devices, ran out of memory).
- `:adblock-client` (`io.github.edsuns.adblockclient`) — JNI wrapper (`src/main/cpp/adblockclient-lib.cpp`) around Brave's C++ ad-block engine in `src/main/cpp/third-party/`. Native bugs here crash the app with SIGSEGV (e.g. strings passed to `parse()` must be NUL-terminated).

Both library modules take compileSdk from the catalog (37) and, like the app, hardcode minSdk 26 and Java 17 and have their own namespaces (AGP 9 requires unique package names). The vendored brave/ad-block C++ engine is deprecated upstream (successor: adblock-rust).

### System bars

`zedMoviesActivity` calls `enableEdgeToEdge()`, so bars are always transparent. `ui/SystemBarsAppearance.kt` (`:shared`; the Android `actual` does the work, the JVM one is a no-op) only picks light or dark bar icons from a color's luminance (`appState.setSystemBarsColor(color)`, e.g. transparent with white icons on the details backdrop). Screens must apply window insets themselves.
