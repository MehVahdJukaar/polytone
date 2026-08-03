#!/usr/bin/env bash
# Builds a Polytone dev resource pack into a distributable zip, named by the version
# string in its pack.mcmeta description (e.g. "§8v1.5" -> sunbathing-v1.5.zip).
# pack.mcmeta ends up at the zip root; dev cruft (.git/.idea) is excluded.
#
# Usage: ./build.sh [pack-name]      (default: sunbathing)
set -euo pipefail

PACK="${1:-sunbathing}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACK_DIR="$HERE/$PACK"
OUT_DIR="$HERE/builds"
mkdir -p "$OUT_DIR"

if [[ ! -f "$PACK_DIR/pack.mcmeta" ]]; then
  echo "No pack.mcmeta found at $PACK_DIR" >&2
  exit 1
fi

# Pull the version (e.g. "v1.5" -> "1.5") from the description; fall back if absent.
VERSION="$(grep -oE 'v[0-9]+(\.[0-9]+)+' "$PACK_DIR/pack.mcmeta" | head -n1 | tr -d 'v')"
VERSION="${VERSION:-unversioned}"

ZIP="$OUT_DIR/${PACK}-v${VERSION}.zip"
rm -f "$ZIP"

( cd "$PACK_DIR" && zip -rq -X "$ZIP" . -x '.git/*' -x '.idea/*' -x '.gitignore' )

echo "Built $ZIP ($(du -h "$ZIP" | cut -f1))"
