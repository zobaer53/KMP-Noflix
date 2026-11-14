#!/usr/bin/env bash
# Writes iosApp/Secrets.xcconfig (gitignored) with the TMDB key, for iosApp/Config.xcconfig to include.
# Source of the key: $TMDB_API_KEY if set, else API_KEY in local.properties (parsed like java.util.Properties:
# local.properties may contain " API_KEY =<key>"). Never prints the key. Without a key it warns and exits 0:
# the build still works, and TMDB answers 401 (the app shows an error state).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/iosApp/Secrets.xcconfig"

key="${TMDB_API_KEY:-}"
source="TMDB_API_KEY"
if [ -z "$key" ] && [ -f "$ROOT/local.properties" ]; then
  key="$(sed -nE 's/^[[:space:]]*API_KEY[[:space:]]*[=:][[:space:]]*(.*[^[:space:]])[[:space:]]*$/\1/p' "$ROOT/local.properties" | head -n 1)"
  source="local.properties"
fi

if [ -z "$key" ]; then
  echo "ios-secrets: no TMDB key (set TMDB_API_KEY or API_KEY in local.properties); the app will build but TMDB will answer 401" >&2
  : > "$OUT"
  exit 0
fi

printf 'TMDB_API_KEY = %s\n' "$key" > "$OUT"
echo "ios-secrets: wrote iosApp/Secrets.xcconfig from $source"
