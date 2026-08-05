#!/usr/bin/env bash
# update-goldens.sh — Regenerate golden files, then report what needs review.
#
# Regeneration records whatever the run produced. A function that fails
# verification has its failure written into the goldens and passes from then on,
# so the diff this prints is the only place that distinguishes the two.
#
# Usage:
#   ./scripts/update-goldens.sh                 # every test
#   ./scripts/update-goldens.sh Assign_local      # one test
#   ./scripts/update-goldens.sh assign_local      # same, as the file is named

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=scripts/lib.sh
source "$SCRIPT_DIR/lib.sh"
cd "$(cd "$SCRIPT_DIR/.." && pwd)"

PATTERN="${1:-}"


# --rerun: an UP-TO-DATE task writes no results, which the guard below
# cannot tell from a run that executed nothing.
args=(--rerun --no-daemon -q -Pkotlin.test.update.test.data=true)
if [[ -n "$PATTERN" ]]; then
    args+=(--tests "*$(gradle_filter "$PATTERN")*")
fi

MARKER="$(mktemp)"

# assertEqualsToFile writes the file and then fails, so these are expected to
# exit non-zero. A pattern naming a test in one module leaves the other with no
# matching tests, which fails the same way — and "No tests found" also exits
# non-zero, so gradle's exit status can't tell the two apart. report_ran_tests
# reads the XML instead.
./gradlew :formver.compiler-plugin:test "${args[@]}" 2>&1 || true
./gradlew :formver.compiler-plugin:locality:test "${args[@]}" 2>&1 || true

if ! report_ran_tests "$MARKER" "$PATTERN"; then
    rm -f "$MARKER"
    exit 1
fi
rm -f "$MARKER"

changed() {
    git status --porcelain -- "$@" | sed -E 's/^.{3}//'
}

# What the golden now says, not just that it changed: a path on its own leaves
# the reader to go and look, and that is the step that gets skipped. A new
# golden has no diff to show, so its whole body is the new content.
show() {
    local cap="$1" file="$2" body
    if git ls-files --error-unmatch "$file" >/dev/null 2>&1; then
        # Drop git's four header lines; the path is already printed above.
        body="$(git diff --no-prefix -- "$file" | tail -n +5)"
    else
        body="$(sed 's/^/+/' "$file")"
    fi
    if [[ "$(wc -l <<<"$body")" -gt "$cap" ]]; then
        head -"$cap" <<<"$body" | sed 's/^/    /'
        echo "    ... truncated, read $file"
    else
        sed 's/^/    /' <<<"$body"
    fi
}

# $2 caps how many lines of each file to show; 0 lists paths only.
report() {
    local header="$1" cap="$2"; shift 2
    local files file
    files="$(changed "$@")"
    if [[ -z "$files" ]]; then
        return 0
    fi
    echo
    echo "$header"
    while IFS= read -r file; do
        echo "  $file"
        if [[ "$cap" -gt 0 ]]; then
            show "$cap" "$file"
        fi
    done <<<"$files"
}

echo
echo "=== golden changes ==="
# Verification diagnostics are a few lines each, and are the thing most likely
# to be recorded by accident, so they are shown whole.
report "verification produced diagnostics for these; confirm that is intended:" 40 \
    '*.viper.diag.txt'
report "conversion output changed:" 40 \
    '*.fir.diag.txt'
report "diagnostic markers changed:" 40 \
    'formver.compiler-plugin/testData/*.kt' \
    'formver.compiler-plugin/locality/testData/*.kt'
report "regenerated test registration; commit as-is:" 0 \
    '*TestGenerated.java'

echo
echo "=== check-testdata.sh ==="
"$SCRIPT_DIR/check-testdata.sh" || true

# The regeneration itself cannot say whether anything was proven: a test whose
# verifier is switched off by a directive writes no golden, exactly like one
# that verified cleanly.
echo
echo "=== what was established ==="
"$SCRIPT_DIR/check-verified.sh" ${PATTERN:+"$PATTERN"} || true

cat <<'EOF'

Regeneration records whatever the run produced. What is above is what these
tests now assert: read it and confirm it is what you meant.
EOF
