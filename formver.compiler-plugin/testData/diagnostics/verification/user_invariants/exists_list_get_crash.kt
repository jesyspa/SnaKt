// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Unlike String indexing, `List.get` uses the impure method-call embedding path inside a
// quantifier, so this case must produce a controlled purity diagnostic rather than crash.
<!PURITY_VIOLATION!>fun <!VERIFICATION_SKIPPED!>existsListGetCrash<!>(l: List<Int>, res: Int): Int {
    postconditions<Int> {
        exists<Int> { i -> 0 <= i && i < l.size && l[i] == l[res] }
    }
    return 0
}<!>
