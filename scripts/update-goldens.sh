#!/usr/bin/env bash
# update-goldens.sh — Regenerate golden files, then report what needs review.
#
# Regeneration records whatever the run produced. A function that fails
# verification has its failure written into the goldens and passes from then on,
# so the diff this prints is the only place that distinguishes the two.
#
# Usage:
#   ./scripts/update-goldens.sh                 # every test
#   ./scripts/update-goldens.sh Max_of_two      # one test
#   ./scripts/update-goldens.sh max_of_two      # same, case-insensitive

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$(cd "$SCRIPT_DIR/.." && pwd)"

PATTERN="${1:-}"

# GenerateTestsKt capitalizes the first letter of a testData file's stem to
# form the JUnit method name (max_function.kt -> testMax_function), and
# Gradle's --tests filter is case-sensitive, so a pattern taken verbatim from
# the filename would match nothing.
gradle_filter() {
    local p="$1"
    if [[ "$p" == test* ]]; then
        printf '%s' "$p"
    else
        printf '%s' "${p^}"
    fi
}

args=(--no-daemon -q -Pkotlin.test.update.test.data=true)
if [[ -n "$PATTERN" ]]; then
    args+=(--tests "*$(gradle_filter "$PATTERN")*")
fi

# assertEqualsToFile writes the file and then fails, so these are expected to
# exit non-zero. A pattern naming a test in one module leaves the other with no
# matching tests, which fails the same way.
./gradlew :formver.compiler-plugin:test "${args[@]}" 2>&1 || true
./gradlew :formver.compiler-plugin:locality:test "${args[@]}" 2>&1 || true

changed() {
    git status --porcelain -- "$@" | sed -E 's/^.{3}//'
}

report() {
    local header="$1"; shift
    local files
    files="$(changed "$@")"
    [[ -z "$files" ]] && return
    echo
    echo "$header"
    echo "$files" | sed 's/^/  /'
}

echo
echo "=== golden changes ==="
report "verification produced diagnostics for these; confirm that is intended:" \
    '*.viper.diag.txt'
report "conversion output changed; read the diff:" \
    '*.fir.diag.txt'
report "diagnostic markers changed; read the diff:" \
    'formver.compiler-plugin/testData/*.kt' \
    'formver.compiler-plugin/locality/testData/*.kt'
report "regenerated test registration; commit as-is:" \
    '*TestGenerated.java'

echo
"$SCRIPT_DIR/check-testdata.sh" || true

cat <<'EOF'

=== not visible in any diff ===
Does the test still fail when the implementation is broken?
Are the preconditions satisfiable? Contradictory ones verify anything.

Run ./scripts/run-test.sh <pattern> to confirm the result.
EOF
