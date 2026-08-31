// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// `List.get` inside a quantifier body goes through a method-call (non-pure) embedding path,
// unlike `String`'s indexing operator. Quantifier bodies must be pure (they are lowered to
// Viper quantifier expressions which do not support side-effecting sub-expressions), so this
// is surfaced as a PURITY_VIOLATION.
<!PURITY_VIOLATION!>fun <!VERIFICATION_SKIPPED!>existsListGetCrash<!>(l: List<Int>, res: Int): Int {
    postconditions<Int> {
        exists<Int> { i -> 0 <= i && i < l.size && l[i] == l[res] }
    }
    return 0
}<!>
