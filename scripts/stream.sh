#!/usr/bin/env bash
# Parallel work streams on one repository with git worktrees (see "Parallel streams" in CLAUDE.md).
#
#   scripts/stream.sh init <stream> [device]                      make this checkout a stream (A, B, …)
#   scripts/stream.sh worktree <path> <branch> <stream> [device]  add another checkout for a second stream
#   scripts/stream.sh start <branch>                              new branch from the latest origin/dev
#   scripts/stream.sh sync                                        merge origin/dev into this branch (never rebase)
#   scripts/stream.sh check [--maestro]                           KMP guard, unit tests + debug build (+ Maestro)
#   scripts/stream.sh test-merge [--maestro]                      sync, then check: run before a PR is merged
#   scripts/stream.sh pr <title> <body-file> [--no-auto]          push, open a PR into dev, turn on auto-merge
#   scripts/stream.sh with-device -- <command…>                   run a command while holding this stream's device
#   scripts/stream.sh status                                      worktrees, streams and device locks
#
# <device> is an adb serial (default emulator-5554). Both streams may use the same one; every run that
# touches a device takes a lock first (scripts/lib/device-lock.sh).
set -euo pipefail

BASE=dev
DEFAULT_DEVICE=emulator-5554
root=$(git rev-parse --show-toplevel)
main=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
cd "$root"
. scripts/lib/device-lock.sh

die() { echo "stream: $*" >&2; exit 1; }

# origin/dev once it exists on GitHub, the local dev branch before the first push.
base_ref() {
  if git fetch -q origin "$BASE" 2> /dev/null; then
    echo "origin/$BASE"
  elif git rev-parse -q --verify "refs/heads/$BASE" > /dev/null; then
    echo "$BASE"
  else
    die "no '$BASE' branch on origin or locally"
  fi
}

# $1 checkout dir, $2 stream name, $3 device serial
setup_checkout() {
  [[ "$2" =~ ^[A-Z]$ ]] || die "stream name must be one capital letter (A, B, …), got '$2'"
  cat > "$1/.stream.env" << EOF
# Written by scripts/stream.sh. Local to this worktree, not committed.
STREAM=$2
# Device for .maestro/run.sh and .maestro/upgrade.sh. Streams may share one; each run takes a lock.
export ANDROID_SERIAL="\${ANDROID_SERIAL:-$3}"
EOF
  # Shared by every worktree: blocks force pushes and direct pushes to main/dev.
  git config core.hooksPath scripts/git-hooks
  if [ "$1" != "$main" ]; then
    # Local files git doesn't carry: the TMDB key and SDK path, and the shared local notes/baseline APKs.
    [ -f "$main/local.properties" ] && cp "$main/local.properties" "$1/local.properties"
    mkdir -p "$main/doc/local"
    rm -rf "$1/doc/local" && ln -s "$main/doc/local" "$1/doc/local"
  fi
  echo "stream $2 in $1 · device ${3}"
}

stream_of() { sed -n 's/^STREAM=//p' "$1/.stream.env" 2> /dev/null || true; }

cmd_init() {
  [ $# -ge 1 ] || die "usage: init <stream> [device]"
  setup_checkout "$root" "$1" "${2:-$DEFAULT_DEVICE}"
}

cmd_worktree() {
  [ $# -ge 3 ] || die "usage: worktree <path> <branch> <stream> [device]"
  local path=$1 branch=$2 stream=$3 device=${4:-$DEFAULT_DEVICE} base
  for wt in $(git worktree list --porcelain | sed -n 's/^worktree //p'); do
    [ "$(stream_of "$wt")" = "$stream" ] && die "stream $stream already lives in $wt"
  done
  base=$(base_ref)
  # --no-track: the branch must not track dev, or a plain `git push` would target dev.
  git worktree add --no-track -b "$branch" "$path" "$base"
  setup_checkout "$(cd "$path" && pwd)" "$stream" "$device"
  echo "next: open a Claude Code session in $(cd "$path" && pwd)"
}

cmd_start() {
  [ $# -eq 1 ] || die "usage: start <branch>"
  [ -z "$(git status --porcelain)" ] || die "commit or stash your changes first"
  local base
  base=$(base_ref)
  git switch --no-track -c "$1" "$base"
  echo "on $1 from $base. Push with: git push -u origin $1"
}

cmd_sync() {
  [ -z "$(git status --porcelain)" ] || die "commit or stash your changes first"
  local base
  base=$(base_ref)
  git merge --no-edit "$base" ||
    die "conflict with $base: resolve, commit and push. Never rebase a pushed branch."
}

cmd_check() {
  echo "== KMP guard"
  scripts/kmp-guard.sh || die "fix the KMP guard first"
  echo "== unit tests (:shared on the JVM, :app) + debug build"
  ./gradlew :shared:jvmTest :app:testDebugUnitTest :app:assembleDebug --console=plain -q
  if [ "${1:-}" = "--maestro" ]; then
    echo "== Maestro suite"
    .maestro/run.sh --no-build
  fi
  echo "checks passed"
}

cmd_test_merge() {
  cmd_sync
  cmd_check "$@"
  echo "merged the latest $BASE and the checks pass: push (scripts/stream.sh pr …) and let auto-merge merge it"
}

# Push this branch, open its PR into dev (or reuse the open one) and turn on auto-merge: GitHub merges it once
# the build check is green on a branch that is up to date with dev (the dev ruleset).
cmd_pr() {
  [ $# -ge 2 ] || die "usage: pr <title> <body-file> [--no-auto]"
  local title=$1 body=$2 auto=${3:-} branch number
  branch=$(git branch --show-current)
  case "$branch" in main | "$BASE") die "open PRs from a feature branch, not $branch" ;; esac
  [ -f "$body" ] || die "no PR body file: $body"
  [ -z "$(git status --porcelain)" ] || die "commit your changes first"
  git push -u origin "$branch"
  number=$(gh pr view "$branch" --json number,state -q 'select(.state == "OPEN") | .number' 2> /dev/null || true)
  if [ -z "$number" ]; then
    gh pr create --base "$BASE" --head "$branch" --title "$title" --body-file "$body"
    number=$(gh pr view "$branch" --json number -q .number)
  fi
  if [ "$auto" = "--no-auto" ]; then
    echo "PR #$number open without auto-merge (merge it by hand once approved)"
  else
    gh pr merge "$number" --auto --merge
    echo "PR #$number: auto-merge on; it merges once the build check passes on an up-to-date branch"
  fi
}

cmd_with_device() {
  [ "${1:-}" = "--" ] && shift
  [ $# -ge 1 ] || die "usage: with-device -- <command…>"
  [ -f .stream.env ] && . ./.stream.env
  device_lock_acquire "$(device_lock_id)"
  "$@"
}

cmd_status() {
  echo "== worktrees"
  git worktree list --porcelain | sed -n 's/^worktree //p' | while read -r wt; do
    printf '  stream %-2s %-40s %s\n' "$(stream_of "$wt")" "$(git -C "$wt" branch --show-current)" "$wt"
  done
  echo "== device locks"
  local locks
  locks="$(cd "$(git rev-parse --git-common-dir)" && pwd)/stream-locks"
  local found=
  for lock in "$locks"/*; do
    [ -d "$lock" ] || continue
    found=1
    pid=$(cat "$lock/pid" 2> /dev/null || true)
    state=$([ -n "$pid" ] && kill -0 "$pid" 2> /dev/null && echo "pid $pid" || echo "stale")
    echo "  $(basename "$lock"): $state · $(cat "$lock/owner" 2> /dev/null)"
  done
  [ -n "$found" ] || echo "  none"
}

case "${1:-}" in
  init) shift; cmd_init "$@" ;;
  worktree) shift; cmd_worktree "$@" ;;
  start) shift; cmd_start "$@" ;;
  sync) shift; cmd_sync ;;
  check) shift; cmd_check "$@" ;;
  test-merge) shift; cmd_test_merge "$@" ;;
  pr) shift; cmd_pr "$@" ;;
  with-device) shift; cmd_with_device "$@" ;;
  status) cmd_status ;;
  *) sed -n '2,16s/^# \{0,1\}//p' "$0"; exit 1 ;;
esac
