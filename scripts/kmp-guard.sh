#!/usr/bin/env bash
# KMP guard: no Android/JVM APIs in shared/src/commonMain (doc/kmp-android-plan.md, 3.6).
# The jvm() target makes the compiler reject android.*, but not the JDK: with only Android and JVM
# targets, commonMain still sees java.*. So this also catches fully qualified java./javax. uses.
# androidx.room, androidx.paging, androidx.datastore and androidx.lifecycle are multiplatform: allowed.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
[ -d shared/src/commonMain ] || exit 0
if grep -rnE '^import (android|androidx\.core|java|javax)\.|(^|[^.[:alnum:]_])(java|javax)\.[a-z]+\.[A-Z]' \
    shared/src/commonMain; then
  echo "kmp-guard: Android/JVM APIs in shared/src/commonMain (see the KMP guard in CLAUDE.md)" >&2
  exit 1
fi
