// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// A quantifier variable is not introduced by a declaration, so the code range of a `Char`
// has to be restated as an antecedent for anything about it to be provable.

@AlwaysVerify
fun <!VIPER_TEXT!>everyCharIsNonNegative<!>(): Boolean {
    postconditions<Boolean> {
        forAll<Char> { it >= '\u0000' }
    }
    return true
}

@AlwaysVerify
fun <!VIPER_TEXT!>everyCharIsAtMostMax<!>(): Boolean {
    postconditions<Boolean> {
        forAll<Char> { it <= '\uFFFF' }
    }
    return true
}
