// FULL_JDK
// DEFAULT_SELECTION

import org.jetbrains.kotlin.formver.plugin.*

// A specification block is the only thing marking these two functions as worth verifying.
// Callers may assume what they promise, so their bodies have to be checked against it.

<!VIPER_VERIFICATION_ERROR!>fun <!VIPER_TEXT!>alwaysPositive<!>(x: Int): Int {
    postconditions<Int> {
        it > 0
    }
    return x
}<!>

fun <!VIPER_TEXT!>atLeastTen<!>(x: Int): Int {
    preconditions {
        x >= 10
    }
    verify(<!VIPER_VERIFICATION_ERROR!>x > 100<!>)
    return x
}

@AlwaysVerify
fun <!VIPER_TEXT!>caller<!>(): Int {
    val positive = alwaysPositive(-1)
    verify(positive > 0)
    return atLeastTen(20)
}

// Neither specified nor annotated: not a target, and nothing is assumed of it.
fun unspecified(x: Int): Int = x + 1
