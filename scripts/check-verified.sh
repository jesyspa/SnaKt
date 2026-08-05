#!/usr/bin/env bash
# check-verified.sh — Whether a test establishes that its subject verifies.
#
# A green suite means the goldens match, not that anything verified:
#
#   verifier ran, recorded nothing       it verifies
#   verifier ran, recorded diagnostics   the failure is now the expectation
#   a directive turned the verifier off  the question does not apply
#
# The second is what to watch for, since regenerating goldens records a failure
# as the expectation. The third is a test saying it is not a verification test,
# and is separated out only so this can answer rather than hedge: a directive
# leaves no golden either, which on its own reads the same as verifying.
#
# The directive covers the file. A conversion error can also stop an individual
# function from reaching the verifier, which the golden records against that
# function as VERIFICATION_SKIPPED.
#
# Usage:
#   ./scripts/check-verified.sh              # summarize every test
#   ./scripts/check-verified.sh Factorial    # one test
#
# Exit: 0 it verifies, 1 it records a failure, 2 no such test, 3 verifier off.

set -euo pipefail

cd "$(cd "$(dirname "$0")/.." && pwd)"

# Both generators model "diagnostics" under their testData root, so a .kt
# outside it backs no test and must not be reported on.
TEST_ROOTS=(
    formver.compiler-plugin/testData/diagnostics
    formver.compiler-plugin/locality/testData/diagnostics
)

PATTERN="${1:-}"

# testData names use dashes where the generated method name uses underscores,
# and the method carries a "test" prefix. Comparing in one spelling lets a
# pattern be given as the file is named or as the method is named.
normalize() {
    printf '%s' "${1//-/_}" | tr '[:upper:]' '[:lower:]'
}

NEEDLE=""
if [[ -n "$PATTERN" ]]; then
    NEEDLE="$(normalize "${PATTERN#test}")"
fi

# Directives are `// NAME` lines in the comment block at the top of the file.
# Reading only that block keeps a directive named in prose further down from
# being taken for a declaration.
leading_directives() {
    awk '
        /^\/\/ [A-Z_0-9]+$/ { print $2; next }
        /^[[:space:]]*$/    { next }
        /^\/\//             { next }
        { exit }
    ' "$1"
}

# Mirrors ExtensionRegistrarConfigurator: NEVER_VALIDATE forces verification off
# and beats ALWAYS_VALIDATE, which in turn overrides the two *_CHECK_ONLY
# directives. Absent all of them, verification is the default.
verifier_off_directive() {
    local directives
    directives="$(leading_directives "$1")"
    if grep -qx NEVER_VALIDATE <<<"$directives"; then
        echo NEVER_VALIDATE
    elif grep -qx ALWAYS_VALIDATE <<<"$directives"; then
        return 1
    elif grep -qx UNIQUE_CHECK_ONLY <<<"$directives"; then
        echo UNIQUE_CHECK_ONLY
    elif grep -qx LOCALITY_CHECK_ONLY <<<"$directives"; then
        echo LOCALITY_CHECK_ONLY
    else
        return 1
    fi
}

# Why the verifier does not run for this test, or 1 if it does.
off_because() {
    local file="$1" directive
    if [[ "$file" == formver.compiler-plugin/locality/* ]]; then
        echo "it is a locality test, and that module has no verification stage"
        return 0
    fi
    if directive="$(verifier_off_directive "$file")"; then
        echo "$directive turns the verifier off"
        return 0
    fi
    return 1
}

verifies=()
records=()
disabled=()

while read -r file; do
    stem="$(basename "$file" .kt)"
    if [[ -n "$NEEDLE" && "$(normalize "$stem")" != *"$NEEDLE"* ]]; then
        continue
    fi
    if reason="$(off_because "$file")"; then
        # Reason first, so the summary can group on it.
        disabled+=("$reason	$file")
        continue
    fi
    golden="${file%.kt}.viper.diag.txt"
    if [[ -s "$golden" ]]; then
        records+=("$golden")
    else
        verifies+=("$file")
    fi
done < <(find "${TEST_ROOTS[@]}" -name '*.kt' | sort)

print_records() {
    local golden
    for golden in "${records[@]}"; do
        echo "$golden does not verify:"
        sed 's/^/  /' "$golden"
    done
}

print_disabled() {
    printf '%s\n' "${disabled[@]}" | awk -F'\t' '{ print "  " $2 " — " $1 }'
}

if [[ -z "$PATTERN" ]]; then
    echo "${#verifies[@]} test(s) verify."
    echo
    echo "${#records[@]} test(s) record a verification failure."
    echo "Each one is only correct if it exists to pin down a known limitation."
    if [[ "${#records[@]}" -gt 0 ]]; then
        printf '  %s\n' "${records[@]}"
    fi
    echo
    echo "${#disabled[@]} test(s) do not run the verifier:"
    if [[ "${#disabled[@]}" -gt 0 ]]; then
        # Grouped by reason: naming all of them buries the counts, and a
        # pattern is how you ask about one.
        printf '%s\n' "${disabled[@]}" | cut -f1 | sort | uniq -c | sort -rn \
            | sed -E 's/^ *([0-9]+) /  \1 — /'
    fi
    exit 0
fi

if [[ "${#verifies[@]}" -eq 0 && "${#records[@]}" -eq 0 && "${#disabled[@]}" -eq 0 ]]; then
    echo "No test matches '$PATTERN'."
    exit 2
fi

# A pattern can match a mix. Report every category it matched — dropping the
# milder ones would hide most of the answer — and let the exit code take the
# most serious.
separator=""

if [[ "${#records[@]}" -gt 0 ]]; then
    print_records
    separator=$'\n'
fi

if [[ "${#disabled[@]}" -gt 0 ]]; then
    printf '%sThe verifier does not run for:\n' "$separator"
    print_disabled
    separator=$'\n'
fi

if [[ "${#verifies[@]}" -gt 0 ]]; then
    printf '%sVerified — the verifier ran and recorded no diagnostics for:\n' "$separator"
    printf '  %s\n' "${verifies[@]}"
fi

if [[ "${#records[@]}" -gt 0 ]]; then
    exit 1
fi
if [[ "${#disabled[@]}" -gt 0 ]]; then
    exit 3
fi
exit 0
