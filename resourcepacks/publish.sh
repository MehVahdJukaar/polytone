#!/usr/bin/env bash
#
# Package a dev resource pack into a releasable .zip under resourcepacks/builds/.
#
#   ./publish.sh                 # builds sunbathing, version auto-read from pack.mcmeta
#   ./publish.sh sunbathing      # same, explicit pack
#   ./publish.sh sunbathing 1.3  # override the version tag
#
# The archive has pack.mcmeta at its ROOT (what Minecraft expects) and includes the
# overlay directories (e.g. reversed_z/). Dev/OS junk is excluded.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACK="${1:-sunbathing}"
PACK_DIR="$SCRIPT_DIR/$PACK"
BUILD_DIR="$SCRIPT_DIR/builds"

[ -d "$PACK_DIR" ]            || { echo "error: no such pack directory: $PACK_DIR" >&2; exit 1; }
[ -f "$PACK_DIR/pack.mcmeta" ] || { echo "error: missing pack.mcmeta in $PACK_DIR" >&2; exit 1; }
command -v zip >/dev/null      || { echo "error: 'zip' is not installed" >&2; exit 1; }

# version: 2nd arg, else the first "vX.Y" token found in pack.mcmeta, else "dev"
VERSION="${2:-}"
if [ -z "$VERSION" ]; then
  VERSION="$(grep -oE 'v[0-9]+(\.[0-9]+)+' "$PACK_DIR/pack.mcmeta" | head -1 || true)"
fi
VERSION="${VERSION#v}"
VERSION="${VERSION:-dev}"

mkdir -p "$BUILD_DIR"
OUT="$BUILD_DIR/${PACK}-v${VERSION}.zip"
rm -f "$OUT"

# zip the CONTENTS of the pack dir at the archive root; drop OS/editor junk.
( cd "$PACK_DIR" && zip -r -X -q "$OUT" . \
    -x '*.DS_Store' -x '__MACOSX*' -x '*/Thumbs.db' -x '*.swp' -x '*/.git/*' )

echo "built $OUT ($(du -h "$OUT" | cut -f1))"
unzip -l "$OUT" | grep -q ' pack.mcmeta$' && echo "ok: pack.mcmeta is at archive root" \
    || { echo "error: pack.mcmeta is NOT at archive root" >&2; exit 1; }
