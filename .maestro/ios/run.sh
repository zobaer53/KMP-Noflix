#!/usr/bin/env bash
# Builds (unless --no-build), installs NoFlix on the iOS simulator and runs the iOS Maestro flows, then checks
# the log and the crash reports (doc/kmp-ios-plan.md, I4). The iOS counterpart of ../run.sh.
# Usage: .maestro/ios/run.sh [--no-build] [flow.yaml ...]
# Environment:
#   IOS_SIMULATOR_UDID   simulator to use (default: "NoFlix iPhone 17", created for this project; never use
#                        another project's simulator)
#   NOFLIX_BUILD_LOCK    path to a lock wrapper such as build-lock.sh. When set, the whole script re-runs under it
#                        (xcodebuild, Gradle and the simulator session are serialized with other agents' builds).
#                        The lock isn't re-entrant: never call it from inside the Xcode build phase.
# Results (screenshots, logs) land in build/maestro-ios.
set -euo pipefail
cd "$(dirname "$0")/../.."
if [ -n "${NOFLIX_BUILD_LOCK:-}" ] && [ -z "${NOFLIX_LOCKED:-}" ]; then
  NOFLIX_LOCKED=1 exec "$NOFLIX_BUILD_LOCK" "$0" "$@"
fi

U=${IOS_SIMULATOR_UDID:-2E6CF691-424D-4045-B5AF-DEB4C78D2C9F}
BUNDLE_ID=com.zobaer53.zedmovies
PROC=NoFlix
APP=iosApp/build/Build/Products/Debug-iphonesimulator/NoFlix.app
OUT=build/maestro-ios

if [ "${1:-}" = "--no-build" ]; then
  shift
else
  if [ -x scripts/ios-secrets.sh ]; then scripts/ios-secrets.sh; fi
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
    -destination "id=$U" -derivedDataPath iosApp/build -quiet build
fi
[ -d "$APP" ] || { echo "ios run: $APP not found (build first)"; exit 1; }

xcrun simctl boot "$U" 2>/dev/null || true
xcrun simctl bootstatus "$U" -b > /dev/null
# Clean install: an update over an older build keeps the Room database and DataStore in Documents/.
xcrun simctl uninstall "$U" "$BUNDLE_ID" 2>/dev/null || true
xcrun simctl install "$U" "$APP"

rm -rf "$OUT"; mkdir -p "$OUT"
# Stream the app's log for the whole run: it holds the NoFlixPlayer: lines (blocked pop-ups and navigations)
# and any Kotlin crash message.
xcrun simctl spawn "$U" log stream --level debug --style compact \
  --predicate "process == \"$PROC\" OR eventMessage CONTAINS \"NoFlixPlayer\"" > "$OUT/app.log" 2>&1 &
log_pid=$!
trap 'kill "$log_pid" 2>/dev/null || true' EXIT
marker=$(mktemp); touch "$marker"

status=0
if [ $# -gt 0 ]; then
  maestro --device "$U" test --test-output-dir "$OUT" "$@" || status=$?
else
  maestro --device "$U" test --test-output-dir "$OUT" .maestro/ios || status=$?
fi
sleep 2
kill "$log_pid" 2>/dev/null || true

if grep -q "Uncaught Kotlin exception" "$OUT/app.log"; then
  echo "CRASH: Uncaught Kotlin exception in the app log:"; grep -m5 -A10 "Uncaught Kotlin exception" "$OUT/app.log"
  status=1
fi
# A native crash leaves a report that is newer than the marker file.
for ips in "$HOME"/Library/Logs/DiagnosticReports/"$PROC"-*.ips; do
  [ -e "$ips" ] && [ "$ips" -nt "$marker" ] && { echo "CRASH report: $ips"; status=1; }
done
rm -f "$marker"
echo "NoFlixPlayer log lines: $(grep -c 'NoFlixPlayer' "$OUT/app.log" || true)" \
  "(blocked popups: $(grep -c 'NoFlixPlayer: blocked popup' "$OUT/app.log" || true)," \
  "cancelled navigations: $(grep -c 'NoFlixPlayer: cancelled navigation' "$OUT/app.log" || true))"
[ $status -eq 0 ] && echo "iOS Maestro suite: OK" || echo "iOS Maestro suite: FAILED (results in $OUT)"
exit $status
