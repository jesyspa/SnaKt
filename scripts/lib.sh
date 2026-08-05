# lib.sh — helpers shared by the scripts in this directory. Source, don't run.

# Turn a test name into the pattern Gradle's --tests expects.
#
# GenerateTestsKt capitalizes the first letter of a testData file's stem to form
# the JUnit method name (max_of_two.kt backs testMax_of_two), and the --tests
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
ran_tests_since() {
    local marker="$1"
    find formver.compiler-plugin/build/test-results \
         formver.compiler-plugin/locality/build/test-results \
         -name '*.xml' -newer "$marker" 2>/dev/null \
        | xargs -r grep -ho 'testcase name="[^"]*"' \
        | sed -E 's/^testcase name="(.*)"$/\1/; s/\(\)$//' \
        | sort -u
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
