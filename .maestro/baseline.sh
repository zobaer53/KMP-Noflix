#!/usr/bin/env bash
# Baseline snapshot for the KMP migration (doc/kmp-android-plan.md → "Baseline" and "Success criteria"):
# APK sizes, native libraries (+ 16 KB alignment), the release runtime dependencies, cold start
# (median of 10) and the screenshots of .maestro/baseline/screens.yaml (dark theme, frozen status bar).
# Unless --skip-suites, it also runs the Maestro suite and, with doc/local/baseline.apk (the last public
# release), the install-over test. Results go to doc/local/ (not committed):
#   NAME.md  NAME.json  deps-NAME.txt  screens-NAME/  logs-NAME/
#
# The public release is signed with the release key and has a CI versionCode (30001 for 3.0.1), so the
# local release build gets that versionCode + 1 (-PappVersionCode, as CI does). The debug build can't be
# installed over it. Release signing needs the RELEASE_* keys in local.properties.
#
# Usage: .maestro/baseline.sh [--name NAME] [--no-build] [--skip-suites] [--runs N]
#   --name NAME    output name (default: baseline). Use another name to compare a branch with the baseline.
#   --no-build     reuse the APKs in app/build/outputs (and the existing deps-NAME.txt)
#   --skip-suites  skip .maestro/run.sh and .maestro/upgrade.sh
#   --runs N       cold starts to measure (default: 10)
set -euo pipefail
cd "$(dirname "$0")/.."
# A stream worktree's .stream.env sets ANDROID_SERIAL (see "Parallel streams" in CLAUDE.md).
[ -f .stream.env ] && . ./.stream.env
. scripts/lib/device-lock.sh

NAME=baseline BUILD=1 SUITES=1 RUNS=10
while [ $# -gt 0 ]; do
  case "$1" in
    --name) NAME=$2; shift 2 ;;
    --no-build) BUILD=; shift ;;
    --skip-suites) SUITES=; shift ;;
    --runs) RUNS=$2; shift 2 ;;
    *) echo "usage: $0 [--name NAME] [--no-build] [--skip-suites] [--runs N]" >&2; exit 2 ;;
  esac
done

PKG=com.zobaer53.zedmovies
ACTIVITY=$PKG/.zedMoviesActivity
LOCAL=doc/local
PUBLIC_APK=$LOCAL/baseline.apk
RELEASE_APK=app/build/outputs/apk/release/app-release.apk
DEBUG_APK=app/build/outputs/apk/debug/app-debug.apk
SCREENS=$LOCAL/screens-$NAME
LOGS=$LOCAL/logs-$NAME
mkdir -p "$LOCAL"

SDK=$(sed -n 's/^sdk.dir=//p' local.properties)
BUILD_TOOLS=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
OBJDUMP=$(ls "$SDK"/ndk/*/toolchains/llvm/prebuilt/*/bin/llvm-objdump 2>/dev/null | sort -V | tail -1)
badging() { "$BUILD_TOOLS/aapt2" dump badging "$1" | sed -n "s/.* $2='\([^']*\)'.*/\1/p" | head -1; }
size_of() { [ -f "$1" ] && wc -c < "$1" | tr -d ' ' || echo 0; }

# --- Build ---------------------------------------------------------------------------------------
if [ -n "$BUILD" ]; then
  version_args=()
  if [ -f "$PUBLIC_APK" ]; then
    version_args=("-PappVersionCode=$(( $(badging "$PUBLIC_APK" versionCode) + 1 ))")
  fi
  echo "== building release + debug ${version_args[*]:-}"
  ./gradlew :app:assembleRelease :app:assembleDebug ${version_args[@]+"${version_args[@]}"} --console=plain -q
  echo "== dependency snapshot"
  ./gradlew :app:dependencies --configuration releaseRuntimeClasspath --console=plain -q > "$LOCAL/deps-$NAME.txt"
fi
"$BUILD_TOOLS/apksigner" verify "$RELEASE_APK" 2>/dev/null ||
  { echo "$RELEASE_APK is not signed: add the RELEASE_* keys to local.properties" >&2; exit 1; }

# --- APK contents (no device needed) ---------------------------------------------------------------
release_size=$(size_of "$RELEASE_APK") debug_size=$(size_of "$DEBUG_APK") public_size=$(size_of "$PUBLIC_APK")
# "<bytes> <path> <aligned|NOT aligned|->" for every native library in the release APK.
so_list=$(
  libs=$(mktemp -d)
  unzip -q -o "$RELEASE_APK" 'lib/*' -d "$libs" 2>/dev/null || true
  for so in $(cd "$libs" && find lib -name '*.so' | sort); do
    align=-
    case "$so" in (lib/arm64-v8a/*|lib/x86_64/*)
      if [ -n "$OBJDUMP" ]; then
        "$OBJDUMP" -p "$libs/$so" | grep LOAD | grep -qv 'align 2\*\*1[4-9]' && align="NOT aligned" || align=aligned
      fi ;;
    esac
    echo "$(size_of "$libs/$so") $so $align"
  done
  rm -rf "$libs"
)

# --- Device --------------------------------------------------------------------------------------
ANDROID_SERIAL=$(device_lock_id); export ANDROID_SERIAL
device_lock_acquire "$ANDROID_SERIAL"
night_before=$(adb shell cmd uimode night | sed -n 's/^Night mode: *//p' | tr -d '\r')
restore_device() {
  adb shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1 || true
  adb shell cmd uimode night "${night_before:-auto}" >/dev/null 2>&1 || true
  device_lock_release
}
trap restore_device EXIT
trap 'restore_device; exit 130' INT TERM

prop() { adb shell getprop "$1" | tr -d '\r'; }
device="$(prop ro.product.model), API $(prop ro.build.version.sdk), $(adb shell getconf PAGE_SIZE | tr -d '\r') B pages, $(prop persist.sys.locale)"

# Prints "median min max all…" in ms for $RUNS cold starts of an APK. The first start (database and
# caches created, maybe the 16 KB dialog) isn't counted.
cold_start() {
  local times=() t i
  adb uninstall $PKG >/dev/null 2>&1 || true
  adb install "$1" >/dev/null
  adb shell am start -W -n "$ACTIVITY" >/dev/null; sleep 8
  for i in $(seq 1 "$RUNS"); do
    adb shell am force-stop $PKG; sleep 2
    t=$(adb shell am start -W -n "$ACTIVITY" | sed -n 's/^TotalTime: *//p' | tr -d '\r')
    times+=("$t"); sleep 3
  done
  adb shell am force-stop $PKG
  printf '%s\n' "${times[@]}" | sort -n | awk '{ v[NR] = $1 } END {
    m = NR % 2 ? v[(NR + 1) / 2] : (v[NR / 2] + v[NR / 2 + 1]) / 2
    printf "%d %d %d", m, v[1], v[NR]; for (i = 1; i <= NR; i++) printf " %d", v[i] }'
}
echo "== cold start: release build ($RUNS runs)"
read -r release_median release_min release_max release_all <<< "$(cold_start "$RELEASE_APK")"
public_median=- public_min=- public_max=- public_all=
if [ -f "$PUBLIC_APK" ]; then
  echo "== cold start: public release ($RUNS runs)"
  read -r public_median public_min public_max public_all <<< "$(cold_start "$PUBLIC_APK")"
fi

# Screenshots of the local release build in the dark theme, with a fixed status bar so they compare.
echo "== screenshots"
adb uninstall $PKG >/dev/null 2>&1 || true
adb install "$RELEASE_APK" >/dev/null
adb shell cmd uimode night yes >/dev/null
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
demo enter; demo clock -e hhmm 0941; demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile hide; demo notifications -e visible false
rm -rf "$SCREENS" "$LOGS"; mkdir -p "$SCREENS" "$LOGS"
screens_status=passed
maestro --device "$ANDROID_SERIAL" test --test-output-dir "$LOGS/screens" .maestro/baseline/screens.yaml ||
  screens_status=failed
find "$LOGS/screens" -name '[0-9][0-9]_*.png' -exec cp {} "$SCREENS/" \;
adb shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null
adb shell cmd uimode night "${night_before:-auto}" >/dev/null

suite_status=skipped upgrade_status=skipped
if [ -n "$SUITES" ]; then
  echo "== Maestro suite"
  # run.sh and upgrade.sh see STREAM_DEVICE_LOCK_HELD and reuse this run's lock.
  .maestro/run.sh --no-build > "$LOGS/run.txt" 2>&1 && suite_status=passed || suite_status=failed
  tail -3 "$LOGS/run.txt"
  if [ -f "$PUBLIC_APK" ]; then
    echo "== install over the public release"
    .maestro/upgrade.sh "$PUBLIC_APK" "$RELEASE_APK" > "$LOGS/upgrade.txt" 2>&1 &&
      upgrade_status=passed || upgrade_status=failed
    tail -1 "$LOGS/upgrade.txt"
  fi
fi

# --- Report --------------------------------------------------------------------------------------
commit=$(git rev-parse --short HEAD)$(git diff --quiet HEAD -- app ad-filter adblock-client || echo "+dirty")
mb() { awk -v b="$1" 'BEGIN { printf "%.2f MB", b / 1048576 }'; }
{
  echo "# Baseline: $NAME"
  echo
  echo "$(date '+%Y-%m-%d %H:%M') · branch \`$(git branch --show-current)\` at \`$commit\` · $device"
  echo
  echo "| APK | Version | Bytes | Size |"
  echo "| --- | --- | --- | --- |"
  echo "| Local release (\`$RELEASE_APK\`) | $(badging "$RELEASE_APK" versionName) ($(badging "$RELEASE_APK" versionCode)) | $release_size | $(mb "$release_size") |"
  echo "| Local debug | $(badging "$DEBUG_APK" versionName) ($(badging "$DEBUG_APK" versionCode)) | $debug_size | $(mb "$debug_size") |"
  [ -f "$PUBLIC_APK" ] &&
    echo "| Public release (\`$PUBLIC_APK\`) | $(badging "$PUBLIC_APK" versionName) ($(badging "$PUBLIC_APK" versionCode)) | $public_size | $(mb "$public_size") |"
  echo
  echo "Cold start (\`am start -W\`, TotalTime, $RUNS runs after one uncounted start):"
  echo
  echo "| APK | Median | Min | Max | All (ms, sorted) |"
  echo "| --- | --- | --- | --- | --- |"
  echo "| Local release | **$release_median ms** | $release_min | $release_max | $release_all |"
  [ -f "$PUBLIC_APK" ] && echo "| Public release | **$public_median ms** | $public_min | $public_max | $public_all |"
  echo
  echo "Native libraries in the local release APK:"
  echo
  echo "| Library | Bytes | 16 KB |"
  echo "| --- | --- | --- |"
  echo "$so_list" | awk 'NF { a = $3; for (i = 4; i <= NF; i++) a = a " " $i; printf "| `%s` | %s | %s |\n", $2, $1, a }'
  echo
  echo "| Check | Result |"
  echo "| --- | --- |"
  echo "| Screenshots (\`screens-$NAME/\`, dark theme) | $screens_status, $(ls "$SCREENS" | wc -l | tr -d ' ') files |"
  echo "| \`.maestro/run.sh\` | $suite_status |"
  echo "| \`.maestro/upgrade.sh $PUBLIC_APK $RELEASE_APK\` | $upgrade_status |"
  echo "| Dependency snapshot | \`deps-$NAME.txt\`, $(grep -c -- '--- ' "$LOCAL/deps-$NAME.txt" 2>/dev/null || echo 0) lines |"
} > "$LOCAL/$NAME.md"
{
  echo "{"
  echo "  \"name\": \"$NAME\", \"commit\": \"$commit\", \"device\": \"$device\","
  echo "  \"apkBytes\": { \"release\": $release_size, \"debug\": $debug_size, \"public\": $public_size },"
  echo "  \"coldStartMs\": { \"runs\": $RUNS, \"releaseMedian\": $release_median, \"publicMedian\": \"$public_median\" },"
  echo "  \"nativeLibs\": [$(echo "$so_list" | awk 'NF { printf "%s\"%s\"", n++ ? ", " : "", $2 }')],"
  echo "  \"screens\": \"$screens_status\", \"suite\": \"$suite_status\", \"upgrade\": \"$upgrade_status\""
  echo "}"
} > "$LOCAL/$NAME.json"
cat "$LOCAL/$NAME.md"
[ "$screens_status" = passed ] && [ "$suite_status" != failed ] && [ "$upgrade_status" != failed ]
