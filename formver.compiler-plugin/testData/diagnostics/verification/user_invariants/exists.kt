// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>simpleExists<!>(): Int {
    preconditions {
        exists<Int> { it == 0 }
    }
    return 0
}

// An unguarded integer index into a string inside `exists` should be flagged
// by Viper as a potentially-negative index well-formedness error.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>duplicateIndexExists<!>(s: String, res: Int): Int {
    postconditions<Int> {
        exists<Int> { i -> 0 <= i && i < s.length && s[i] == s[res] }
    }
    return 0
}<!>
