# lib.sh — helpers shared by the scripts in this directory. Source, don't run.

# Turn a test name into the pattern Gradle's --tests expects.
#
# GenerateTestsKt capitalizes the first letter of a testData file's stem to form
# the JUnit method name (assign_local.kt backs testAssign_local), and the --tests
# filter is case-sensitive, so a pattern taken verbatim from the file name would
# match nothing. A name already in method form is passed through.
gradle_filter() {
    local pattern="$1"
    if [[ "$pattern" == test* ]]; then
        printf '%s' "$pattern"
    else
        printf '%s%s' "$(printf '%s' "${pattern:0:1}" | tr '[:lower:]' '[:upper:]')" "${pattern:1}"
    fi
}

# List the JUnit test methods that actually ran, based on the test-results XML
# gradle writes under formver.compiler-plugin/build/test-results/<task>/ and
# formver.compiler-plugin/locality/build/test-results/test/. Only files newer
# than $1 count: gradle can report a test task SUCCESS (e.g. UP-TO-DATE)
# without re-executing it, leaving stale XML from an earlier run that would
# otherwise be mistaken for this run's output.
#
# Reporting nothing is a normal outcome, so this never fails: a task that ran
# only one module leaves the other module's directory absent, and the caller
# runs under `set -e`.
ran_tests_since() {
    local marker="$1" dirs=() dir
    for dir in formver.compiler-plugin/build/test-results \
               formver.compiler-plugin/locality/build/test-results; do
        if [[ -d "$dir" ]]; then
            dirs+=("$dir")
        fi
    done
    if [[ "${#dirs[@]}" -eq 0 ]]; then
        return 0
    fi
    find "${dirs[@]}" -name '*.xml' -newer "$marker" \
        | xargs -r grep -ho 'testcase name="[^"]*"' \
        | sed -E 's/^testcase name="(.*)"$/\1/; s/\(\)$//' \
        | sort -u \
        || true
}

# Report the tests behind a gradle success, or fail: a SUCCESS with no test
# method behind it is not a pass. $2, if given, is the --tests pattern that
# produced this run, used only to decide how much of the list to show.
report_ran_tests() {
    local marker="$1" pattern="${2:-}"
    local tests count
    tests="$(ran_tests_since "$marker")"
    if [[ -z "$tests" ]]; then
        echo "Gradle reported success, but no test actually ran."
        return 1
    fi
    count="$(wc -l <<<"$tests")"

    if [[ -z "$pattern" ]]; then
        echo "Ran $count tests."
    elif [[ "$count" -eq 1 ]]; then
        echo "Ran: $tests"
    elif [[ "$count" -le 5 ]]; then
        echo "Ran $count tests — the pattern matched all of them:"
        sed 's/^/  /' <<<"$tests"
    else
        local sample
        sample="$(head -5 <<<"$tests" | paste -sd, - | sed 's/,/, /g')"
        echo "Ran $count tests — the pattern matched all of them, including: $sample, ..."
    fi
    return 0
}

# True if a JUnit <failure>/<error> "type" attribute names an exception
# DumpAssertionDiffExtension can pull expected/actual out of: opentest4j's
# AssertionFailedError (assertEqualsToFile) or a *ComparisonFailure (covers
# both org.junit.ComparisonFailure and com.intellij's FileComparisonFailure).
# Anything else is a thrown exception, not a golden-file mismatch, and
# re-running through dump-test-diff.sh would recover nothing.
is_assertion_failure_type() {
    case "$1" in
        org.opentest4j.AssertionFailedError|*ComparisonFailure) return 0 ;;
        *) return 1 ;;
    esac
}

# Print the first failing <testcase> from JUnit XML newer than $1, across the
# directories ran_tests_since checks: its failure "type" on the first line,
# then "classname.name: message", then a few lines of stack trace.
#
# Reused so callers can decide what a failure actually was before acting on
# it, instead of assuming a golden-file mismatch and escalating to
# dump-test-diff.sh only to find nothing there. Returns 1 with nothing
# printed if there is no fresh XML at all (the run died before any test
# executed) or no failing testcase in it.
report_first_xml_failure() {
    local marker="$1" dirs=() dir
    for dir in formver.compiler-plugin/build/test-results \
               formver.compiler-plugin/locality/build/test-results; do
        if [[ -d "$dir" ]]; then
            dirs+=("$dir")
        fi
    done
    if [[ "${#dirs[@]}" -eq 0 ]]; then
        return 1
    fi
    local files=()
    while IFS= read -r f; do
        files+=("$f")
    done < <(find "${dirs[@]}" -name '*.xml' -newer "$marker")
    if [[ "${#files[@]}" -eq 0 ]]; then
        return 1
    fi
    python3 - "${files[@]}" <<'PY'
import sys
import xml.etree.ElementTree as ET

for path in sys.argv[1:]:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    for testcase in root.findall("testcase"):
        node = testcase.find("failure")
        if node is None:
            node = testcase.find("error")
        if node is None:
            continue
        classname = testcase.get("classname", root.get("name", "?"))
        name = testcase.get("name", "?")
        message = node.get("message") or "(no message)"
        print(node.get("type", ""))
        print(f"{classname}.{name}: {message}")
        for line in (node.text or "").strip().splitlines()[:8]:
            print(f"    {line}")
        sys.exit(0)

sys.exit(1)
PY
}
