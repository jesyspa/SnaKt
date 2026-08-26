// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>charLiteralIsInCodeRange<!>(): Char = 'a'

// The parameter's code range comes from its type, so the callee's precondition is met
// without the caller saying anything about the argument.
@AlwaysVerify
fun <!VIPER_TEXT!>requiresNonNegativeChar<!>(c: Char): Char {
    preconditions {
        c >= '\u0000'
    }
    return c
}

@AlwaysVerify
fun <!VIPER_TEXT!>passesCharParameterOn<!>(c: Char): Char = requiresNonNegativeChar(c)

@AlwaysVerify
fun <!VIPER_TEXT!>nullableCharRoundTrip<!>(c: Char?): Char? = c

// A string is embedded as a sequence of unconstrained `Int`s, so an element read out of one
// only lands in the code range because `String.get` reduces it into the range.
@AlwaysVerify
fun <!VIPER_TEXT!>stringElementIsInCodeRange<!>(s: String): Char {
    preconditions {
        s.length > 0
    }
    return requiresNonNegativeChar(s[0])
}

// `c` is assigned in the loop body, so it is havocked at the loop head: the code range is
// only available afterwards if the loop carries the invariant.
@AlwaysVerify
fun <!VIPER_TEXT!>advanceCharInLoop<!>(n: Int): Char {
    preconditions {
        n >= 0
    }
    var c = 'a'
    var i = 0
    while (i < n) {
        loopInvariants {
            i <= n
        }
        c += 1
        i += 1
    }
    return requiresNonNegativeChar(c)
}
