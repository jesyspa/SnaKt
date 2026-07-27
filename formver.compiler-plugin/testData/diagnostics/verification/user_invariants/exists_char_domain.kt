// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// `Char` lowers to a Viper `Int`. Under a correct [0, 65536) code-point bound on a
// `Char`-typed quantifier variable no character precedes the minimum code point, so this
// postcondition is unsatisfiable and must be rejected.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>charBelowMinExists<!>(): Int {
    postconditions<Int> {
        exists<Char> { c -> c < '\u0000' }
    }
    return 0
}<!>

// The same property at the upper end: no character exceeds the maximum code point.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>charAboveMaxExists<!>(): Int {
    postconditions<Int> {
        exists<Char> { c -> c > '\uFFFF' }
    }
    return 0
}<!>
