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
