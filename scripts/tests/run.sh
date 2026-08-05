#!/usr/bin/env bash
# Exercises scripts/junit_ran_tests.py and scripts/junit_first_failure.py
# directly against the fixture XML in scripts/tests/fixtures, the same way
# lib.sh invokes them: python3 <script> <xml files...>. Not wired into any
# other script — run by hand.
set -euo pipefail

TESTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(cd "$TESTS_DIR/.." && pwd)"
FIXTURES="$TESTS_DIR/fixtures"

failures=0

# assert_eq NAME EXPECTED_STDOUT EXPECTED_EXIT -- CMD...
assert_eq() {
    local name="$1" expected="$2" expected_exit="$3"
    shift 3
    [[ "$1" == "--" ]] || { echo "assert_eq: missing --"; exit 2; }
    shift
    local actual actual_exit
    actual="$("$@" 2>&1)" && actual_exit=0 || actual_exit=$?
    if [[ "$actual" == "$expected" && "$actual_exit" == "$expected_exit" ]]; then
        echo "ok - $name"
        return 0
    fi
    echo "FAIL - $name"
    if [[ "$actual" != "$expected" ]]; then
        echo "  expected stdout:"
        sed 's/^/    /' <<<"$expected"
        echo "  actual stdout:"
        sed 's/^/    /' <<<"$actual"
    fi
    if [[ "$actual_exit" != "$expected_exit" ]]; then
        echo "  expected exit $expected_exit, got $actual_exit"
    fi
    failures=$((failures + 1))
}

# A passing run: one testcase, no failure/error. ran_tests lists it (and
# strips the trailing "()" JUnit adds to the method name); first_failure
# finds nothing and exits 1.
assert_eq "ran_tests: passing run" \
    "testAssign_local" 0 \
    -- python3 "$LIB_DIR/junit_ran_tests.py" "$FIXTURES/passing.xml"

assert_eq "first_failure: passing run reports nothing" \
    "" 1 \
    -- python3 "$LIB_DIR/junit_first_failure.py" "$FIXTURES/passing.xml"

# The same method name, testBasic, backed by two different classes across
# two files: ran_tests must qualify both as classname.method rather than
# collapsing them to one ambiguous "testBasic".
assert_eq "ran_tests: same method name in two classes gets qualified" \
    "$(printf '%s\n%s' "verification.BasicTest.testBasic" "verification.operators.BasicTest.testBasic")" 0 \
    -- python3 "$LIB_DIR/junit_ran_tests.py" "$FIXTURES/ambiguous_a.xml" "$FIXTURES/ambiguous_b.xml"

# A <failure>: first_failure reports type, "classname.name: message", then
# trace lines with the leading restated-message line dropped.
assert_eq "first_failure: <failure> is reported" \
    "$(printf '%s\n%s\n%s' \
        "org.opentest4j.AssertionFailedError" \
        "verification.BasicTest.testAssign_local: expected: <1> but was: <2>" \
        "    at verification.BasicTest.testAssign_local(BasicTest.java:10)")" 0 \
    -- python3 "$LIB_DIR/junit_first_failure.py" "$FIXTURES/failure.xml"

# An <error>: same reporting path as <failure>, via the error element instead.
assert_eq "first_failure: <error> is reported" \
    "$(printf '%s\n%s\n%s' \
        "java.lang.RuntimeException" \
        "verification.BasicTest.testNon_local_returns: boom" \
        "    at verification.BasicTest.testNon_local_returns(BasicTest.java:20)")" 0 \
    -- python3 "$LIB_DIR/junit_first_failure.py" "$FIXTURES/error.xml"

# A malformed XML file must be skipped, not raise: alongside a real failure,
# the failure is still found; alongside only a passing run, ran_tests still
# lists the real test.
assert_eq "first_failure: malformed XML is skipped, real failure still found" \
    "$(printf '%s\n%s\n%s' \
        "org.opentest4j.AssertionFailedError" \
        "verification.BasicTest.testAssign_local: expected: <1> but was: <2>" \
        "    at verification.BasicTest.testAssign_local(BasicTest.java:10)")" 0 \
    -- python3 "$LIB_DIR/junit_first_failure.py" "$FIXTURES/malformed.xml" "$FIXTURES/failure.xml"

assert_eq "ran_tests: malformed XML is skipped, real test still listed" \
    "testAssign_local" 0 \
    -- python3 "$LIB_DIR/junit_ran_tests.py" "$FIXTURES/malformed.xml" "$FIXTURES/passing.xml"

assert_eq "first_failure: only malformed XML reports nothing" \
    "" 1 \
    -- python3 "$LIB_DIR/junit_first_failure.py" "$FIXTURES/malformed.xml"

if [[ "$failures" -gt 0 ]]; then
    echo "$failures assertion(s) failed"
    exit 1
fi
echo "all assertions passed"
