#!/bin/bash
# Run tests/programs/*.asm and compare the result with tests/expected.
#   scripts/test.sh            test build/Mars.jar
#   scripts/test.sh --update   make new expected files with the official MARS 4.5 jar
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

baseline_url="https://dpetersanderson.github.io/Mars4_5.jar"
baseline_sha="ac340b676ba2b62246b9df77e62f81ad4447bcfd329ab539716bcd09950b7096"
max_steps=2000000

if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
java_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}java"

# On Linux, Java prints a message the first time it makes this folder.
mkdir -p "$HOME/.java/.userPrefs"

work="$(mktemp -d)"
trap '/bin/rm -rf "$work"' EXIT

# Run one program in an empty folder, so file tests do not touch the repo.
run_program() {
  local jar="$1" name="$2" run_dir out input
  run_dir="$(cd "$work" && mkdir -p "run/$name" && cd "run/$name" && pwd -P)"
  cp "tests/programs/$name.asm" "$run_dir/"
  input=/dev/null
  [[ -f "tests/programs/$name.in" ]] && input="$root/tests/programs/$name.in"
  out="$work/out"
  local code=0
  (cd "$run_dir" && "$java_cmd" -Djava.awt.headless=true -jar "$jar" nc ae2 se3 "$max_steps" "$name.asm" \
    < "$input" > "$out.stdout" 2> "$out.stderr") || code=$?
  {
    cat "$out.stdout"
    echo "--- stderr ---"
    cat "$out.stderr"
    echo "--- exit code: $code ---"
  } | tr -d '\r' | sed "s#$run_dir#<dir>#g"
}

programs=()
for file in tests/programs/*.asm; do
  programs+=("$(basename "$file" .asm)")
done

if [[ "${1:-}" == "--update" ]]; then
  jar="$root/build/baseline/Mars4_5.jar"
  if [[ ! -f "$jar" ]]; then
    mkdir -p build/baseline
    curl -fsSL --max-time 120 -o "$jar" "$baseline_url"
  fi
  echo "$baseline_sha  $jar" | shasum -a 256 -c -
  mkdir -p tests/expected
  for name in "${programs[@]}"; do
    run_program "$jar" "$name" > "tests/expected/$name.txt"
    echo "updated $name"
  done
  exit 0
fi

jar="$root/build/Mars.jar"
failed=0
for name in "${programs[@]}"; do
  run_program "$jar" "$name" > "$work/actual.txt"
  if diff -u "tests/expected/$name.txt" "$work/actual.txt" > "$work/diff.txt"; then
    echo "ok    $name"
  else
    echo "FAIL  $name"
    cat "$work/diff.txt"
    failed=$((failed + 1))
  fi
done

echo "${#programs[@]} tests, $failed failed"
[[ "$failed" -eq 0 ]]
