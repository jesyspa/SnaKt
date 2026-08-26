// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Kotlin truncates `Char` arithmetic to the code range, so the results below stay in range
// and the boundary cases wrap rather than escaping it.

@AlwaysVerify
fun <!VIPER_TEXT!>maxPlusOneWrapsToMin<!>(): Char {
    postconditions<Char> { res ->
        res == '\u0000'
    }
    return '\uFFFF' + 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>minMinusOneWrapsToMax<!>(): Char {
    postconditions<Char> { res ->
        res == '\uFFFF'
    }
    return '\u0000' - 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>shiftByUnknownAmount<!>(c: Char, n: Int): Char = c + n

@AlwaysVerify
fun <!VIPER_TEXT!>shiftWithinRange<!>(c: Char): Char {
    preconditions {
        c < '\uFFFF'
    }
    postconditions<Char> { res ->
        res > c
    }
    return c + 1
}
