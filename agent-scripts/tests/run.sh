#!/usr/bin/env bash
# Exercises junit_first_failure.py against the fixture XML in fixtures/, the way
# lib.sh invokes it: python3 <script> <xml files...>. Needs no build, so
# pre-commit runs it as a hook; also runnable by hand.
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
    # stderr kept separate: folded into stdout, a traceback reads as wrong
    # output rather than as a crash.
    local actual actual_exit err_file
    err_file="$(mktemp)"
    actual="$("$@" 2>"$err_file")" && actual_exit=0 || actual_exit=$?
    local errors
    errors="$(cat "$err_file")"
    rm -f "$err_file"
    if [[ "$actual" == "$expected" && "$actual_exit" == "$expected_exit" && -z "$errors" ]]; then
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
    if [[ -n "$errors" ]]; then
        echo "  unexpected stderr:"
        sed 's/^/    /' <<<"$errors"
    fi
    failures=$((failures + 1))
}

assert_eq "first_failure: passing run reports nothing" \
    "" 1 \
    -- python3 "$LIB_DIR/junit_first_failure.py" "$FIXTURES/passing.xml"

assert_eq "first_failure: <failure> is reported" \
    "$(printf '%s\n%s\n%s' \
        "org.opentest4j.AssertionFailedError" \
        "verification.BasicTest.testAssign_local: expected: <1> but was: <2>" \
        "    at verification.BasicTest.testAssign_local(BasicTest.java:10)")" 0 \
    -- python3 "$LIB_DIR/junit_first_failure.py" "$FIXTURES/failure.xml"

assert_eq "first_failure: <error> is reported" \
    "$(printf '%s\n%s\n%s' \
        "java.lang.RuntimeException" \
        "verification.BasicTest.testNon_local_returns: boom" \
        "    at verification.BasicTest.testNon_local_returns(BasicTest.java:20)")" 0 \
    -- python3 "$LIB_DIR/junit_first_failure.py" "$FIXTURES/error.xml"

assert_eq "first_failure: malformed XML is skipped, real failure still found" \
    "$(printf '%s\n%s\n%s' \
        "org.opentest4j.AssertionFailedError" \
        "verification.BasicTest.testAssign_local: expected: <1> but was: <2>" \
        "    at verification.BasicTest.testAssign_local(BasicTest.java:10)")" 0 \
    -- python3 "$LIB_DIR/junit_first_failure.py" "$FIXTURES/malformed.xml" "$FIXTURES/failure.xml"

assert_eq "first_failure: only malformed XML reports nothing" \
    "" 1 \
    -- python3 "$LIB_DIR/junit_first_failure.py" "$FIXTURES/malformed.xml"

# counts are "total assertion_failed other_failed skipped unreadable".
assert_eq "counts: a passing run" \
    "1 0 0 0 0" 0 \
    -- python3 "$LIB_DIR/junit_counts.py" "$FIXTURES/passing.xml"

assert_eq "counts: a golden mismatch is counted apart from a thrown exception" \
    "2 1 1 0 0" 0 \
    -- python3 "$LIB_DIR/junit_counts.py" "$FIXTURES/failure.xml" "$FIXTURES/error.xml"

assert_eq "counts: a skipped test is neither passed nor failed" \
    "2 0 0 1 0" 0 \
    -- python3 "$LIB_DIR/junit_counts.py" "$FIXTURES/skipped.xml"

assert_eq "counts: malformed XML is reported, not silently dropped" \
    "1 0 0 0 1" 0 \
    -- python3 "$LIB_DIR/junit_counts.py" "$FIXTURES/malformed.xml" "$FIXTURES/passing.xml"

if [[ "$failures" -gt 0 ]]; then
    echo "$failures assertion(s) failed"
    exit 1
fi
echo "all assertions passed"
