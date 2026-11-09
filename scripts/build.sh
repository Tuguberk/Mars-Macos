#!/bin/bash
# Build build/Mars.jar from the Java source files.
# The .class files in the repo come from upstream. This script does not use them.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}"

stage="build/stage"
/bin/rm -rf "$stage" build/Mars.jar
mkdir -p "$stage"

find Mars.java mars -name '*.java' > build/sources.txt
"${java_bin}javac" --release 11 -encoding UTF-8 -nowarn -Xlint:none \
  -cp "lib/*" -d "$stage" @build/sources.txt

# Put the libraries inside Mars.jar, so one jar is enough to run the app.
for library in lib/*.jar; do
  unzip -qo "$library" -d "$stage" -x 'META-INF/MANIFEST.MF' 'META-INF/LICENSE*' '*module-info.class'
done
rsync -a lib/licenses "$stage/"

# Copy the same files as upstream CreateMarsJar.bat, but not the old classes.
rsync -a --exclude='*.class' Mars.java docs help images mars "$stage/"
cp PseudoOps.txt Config.properties Syscall.properties Settings.properties \
  MARSlicense.txt mainclass.txt MipsXRayOpcode.xml registerDatapath.xml \
  controlDatapath.xml ALUcontrolDatapath.xml "$stage/"

# FlatLaf has special classes for new Java versions, so the jar must be Multi-Release.
printf 'Main-Class: Mars\nClass-Path: .\nMulti-Release: true\n' > build/manifest.txt
"${java_bin}jar" cfm build/Mars.jar build/manifest.txt -C "$stage" .
echo "Built build/Mars.jar"
