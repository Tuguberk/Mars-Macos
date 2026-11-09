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
  -d "$stage" @build/sources.txt

# Copy the same files as upstream CreateMarsJar.bat, but not the old classes.
rsync -a --exclude='*.class' Mars.java docs help images mars "$stage/"
cp PseudoOps.txt Config.properties Syscall.properties Settings.properties \
  MARSlicense.txt mainclass.txt MipsXRayOpcode.xml registerDatapath.xml \
  controlDatapath.xml ALUcontrolDatapath.xml "$stage/"

"${java_bin}jar" cfm build/Mars.jar mainclass.txt -C "$stage" .
echo "Built build/Mars.jar"
