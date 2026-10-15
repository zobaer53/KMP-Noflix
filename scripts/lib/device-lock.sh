# Device lock shared by every worktree of this repository (see "Parallel streams" in CLAUDE.md).
# Sourced by .maestro/run.sh, .maestro/upgrade.sh and scripts/stream.sh.
#
# Both streams test on the same emulator, and a Maestro run uninstalls and reinstalls the app, so
# only one run may use a device at a time. Each device gets a folder under <git common dir>/stream-locks,
# which every worktree sees. mkdir is atomic: whichever run creates the folder first gets the device,
# the other waits.

# The device this run will use: $ANDROID_SERIAL (set by .stream.env), or the only attached device.
device_lock_id() {
  if [ -n "${ANDROID_SERIAL:-}" ]; then echo "$ANDROID_SERIAL"; return; fi
  local devices
  devices=$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')
  if [ "$(printf '%s\n' "$devices" | grep -c .)" != 1 ]; then
    echo "device lock: set ANDROID_SERIAL (in .stream.env); attached: ${devices:-none}" >&2
    return 1
  fi
  echo "$devices"
}

device_lock_acquire() {
  local device=${1:?device id}
  # A parent process (scripts/stream.sh with-device) already holds this device.
  [ "${STREAM_DEVICE_LOCK_HELD:-}" = "$device" ] && return 0
  local locks lock holder announced=
  locks="$(cd "$(git rev-parse --git-common-dir)" && pwd)/stream-locks"
  mkdir -p "$locks"
  lock="$locks/$device"
  until mkdir "$lock" 2> /dev/null; do
    holder=$(cat "$lock/pid" 2> /dev/null || true)
    # A run that died without its trap leaves the lock behind: a dead PID, or no PID after a minute.
    if { [ -n "$holder" ] && ! kill -0 "$holder" 2> /dev/null; } ||
       { [ -z "$holder" ] && [ -n "$(find "$lock" -maxdepth 0 -mmin +1 2> /dev/null)" ]; }; then
      rm -rf "${lock:?}"
      continue
    fi
    if [ -z "$announced" ]; then
      echo "waiting for $device, held by: $(cat "$lock/owner" 2> /dev/null || echo unknown)" >&2
      announced=1
    fi
    sleep 5
  done
  echo $$ > "$lock/pid"
  echo "stream ${STREAM:-?} · $(git branch --show-current) · $(git rev-parse --show-toplevel)" > "$lock/owner"
  STREAM_DEVICE_LOCK=$lock
  export STREAM_DEVICE_LOCK_HELD=$device
  trap device_lock_release EXIT
  trap 'device_lock_release; exit 130' INT TERM
}

device_lock_release() {
  if [ -n "${STREAM_DEVICE_LOCK:-}" ] && [ "$(cat "$STREAM_DEVICE_LOCK/pid" 2> /dev/null)" = "$$" ]; then
    rm -rf "$STREAM_DEVICE_LOCK"
  fi
  STREAM_DEVICE_LOCK=
}
