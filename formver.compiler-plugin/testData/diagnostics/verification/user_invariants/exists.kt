// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>simpleExists<!>(): Int {
    preconditions {
        exists<Int> { it == 0 }
    }
    return 0
}

// `s[res]` has no bounds guard on `res`; Viper rejects the unguarded index
// inside the exists body as a well-formedness violation. Tests that exists<T>
// in a postcondition surfaces a failure as VIPER_VERIFICATION_ERROR rather
// than crashing or silently passing.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>duplicateIndexExists<!>(s: String, res: Int): Int {
    postconditions<Int> {
        exists<Int> { i -> 0 <= i && i < s.length && s[i] == s[res] }
    }
    return 0
}<!>
