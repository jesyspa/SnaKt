// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Intended contract (CH-1): a Char-typed forAll quantifier variable should stay within
// the Unicode code-point range [0, 65536), since every real Kotlin Char value does.
// Without the domain bound the quantifier ranges over unbounded Viper Ints, so
// "forall x: Int, x >= 0" is not provable from the empty body.
@AlwaysVerify
fun <!VIPER_TEXT!>forallCharNonNegative<!>(): Boolean {
    postconditions<Boolean> {
        forAll<Char> { it >= '\u0000' }
    }
    return true
}
