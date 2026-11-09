#!/bin/bash
# Make build/package/Mars-Macos.app and a .dmg file. The app has its own Java.
# The app is not signed with an Apple Developer ID.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

version="4.6.0"
arch="$(uname -m)"

if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21)"
  export JAVA_HOME
fi

scripts/build.sh

out="build/package"
/bin/rm -rf "$out"
mkdir -p "$out/input" "$out/icon/Mars-Macos.iconset" "$out/dmg"
cp build/Mars.jar "$out/input/"

for size in 16 32 128 256 512; do
  double=$((size * 2))
  sips -z "$size" "$size" packaging/macos/icon-1024.png \
    --out "$out/icon/Mars-Macos.iconset/icon_${size}x${size}.png" > /dev/null
  sips -z "$double" "$double" packaging/macos/icon-1024.png \
    --out "$out/icon/Mars-Macos.iconset/icon_${size}x${size}@2x.png" > /dev/null
done
iconutil -c icns "$out/icon/Mars-Macos.iconset" -o "$out/icon/Mars-Macos.icns"

"$JAVA_HOME/bin/jpackage" \
  --type app-image \
  --name Mars-Macos \
  --app-version "$version" \
  --input "$out/input" \
  --main-jar Mars.jar \
  --main-class Mars \
  --icon "$out/icon/Mars-Macos.icns" \
  --resource-dir packaging/macos \
  --mac-package-identifier io.github.tuguberk.marsmacos \
  --mac-package-name Mars-Macos \
  --add-modules java.base,java.desktop,java.prefs \
  --jlink-options "--strip-debug --no-man-pages --no-header-files" \
  --dest "$out"

cp -R "$out/Mars-Macos.app" "$out/dmg/"
ln -s /Applications "$out/dmg/Applications"
cp MARSlicense.txt "$out/dmg/LICENSE.txt"

dmg="$out/Mars-Macos-$version-$arch.dmg"
hdiutil create -quiet -volname "Mars-Macos" -srcfolder "$out/dmg" -ov -format UDZO "$dmg"
echo "Built $dmg"
