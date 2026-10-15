#!/usr/bin/env bash
# Install-over test: OLD apk + wish list item, then the current debug build installed on top.
# Usage: .maestro/upgrade.sh [--content-language] path/to/old.apk [path/to/new.apk]
#   new defaults to the debug build. Over a public release it must be a release-signed build with a
#   higher versionCode (see .maestro/baseline.sh).
#   --content-language  also checks that the content language survives the update. The app stores it
#     from the system language on its first start, and adb can't change that language on a phone, so
#     the script asks for it and waits: German before the old APK starts, English again before the new
#     build starts. The new build must still load German content.
set -euo pipefail
cd "$(dirname "$0")/.."
# A stream worktree's .stream.env sets ANDROID_SERIAL (see "Parallel streams" in CLAUDE.md).
[ -f .stream.env ] && . ./.stream.env
. scripts/lib/device-lock.sh
LANGUAGE_CHECK=
if [ "${1:-}" = "--content-language" ]; then LANGUAGE_CHECK=1; shift; fi
ANDROID_SERIAL=$(device_lock_id); export ANDROID_SERIAL
device_lock_acquire "$ANDROID_SERIAL"
# adb reads ANDROID_SERIAL; Maestro gets --device.
MAESTRO=(maestro --device "$ANDROID_SERIAL")

# Waits up to 10 minutes for the phone's primary system language to be $1 ("de", "en"), asking the
# person at the phone to switch it in Settings.
wait_for_language() {
  local want=$1 name=$2 deadline=$(( $(date +%s) + 600 ))
  system_language() { adb shell getprop persist.sys.locale | tr -d '\r' | cut -d- -f1; }
  if [ "$(system_language)" != "$want" ]; then
    echo ">>> Set the phone's language to $name: Settings > System > Languages, make it the first one."
    until [ "$(system_language)" = "$want" ]; do
      [ "$(date +%s)" -gt $deadline ] && { echo "timed out waiting for the $name system language"; exit 1; }
      sleep 3
    done
    # Let the system finish applying the new configuration.
    sleep 5
  fi
  echo "system language: $(adb shell getprop persist.sys.locale | tr -d '\r')"
}

adb uninstall com.zobaer53.zedmovies >/dev/null 2>&1 || true
adb install "$1" >/dev/null
[ -n "$LANGUAGE_CHECK" ] && wait_for_language de "Deutsch (German)"
"${MAESTRO[@]}" test .maestro/upgrade/1_add_wishlist.yaml
[ -n "$LANGUAGE_CHECK" ] && wait_for_language en "English"
adb install -r "${2:-app/build/outputs/apk/debug/app-debug.apk}" >/dev/null
adb logcat -c
"${MAESTRO[@]}" test .maestro/upgrade/2_check_wishlist.yaml
if [ -n "$LANGUAGE_CHECK" ]; then
  "${MAESTRO[@]}" test .maestro/upgrade/3_check_content_language.yaml
  echo "content language kept: German content with an English system language"
fi
if adb logcat -d | grep -qE "FATAL EXCEPTION|IllegalStateException: .*(Room|migration)"; then
  echo "CRASH after upgrade:"; adb logcat -d | grep -A12 "FATAL EXCEPTION" | head -30; exit 1
fi
echo "UPGRADE CHECK PASSED"
