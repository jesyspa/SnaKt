// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Verifies that we can transform exists from SnaKt to viper
@AlwaysVerify
fun <!VIPER_TEXT!>simpleExists<!>(): Int {
    preconditions {
        exists<Int> { it == 0 }
    }
    return 0
}

// `val c = s[0]` reads `s[0]` before the loop, making it a ground term. The loop
// invariant's `s[it]` trigger then lets the solver instantiate `it = 0` as the
// witness, connecting `c` back to `s[res]` in the postcondition.
@AlwaysVerify
fun <!VIPER_TEXT!>symbolExists<!>(s: String): Int {
    preconditions {
        s.length > 0
    }
    postconditions<Int> { res ->
        0 <= res && res < s.length &&
        exists<Int> { 0 <= it && it < s.length && s[it] == s[res] }
    }
    val c = s[0]
    var i = 0
    while (i < s.length) {
        loopInvariants {
            0 <= i && i <= s.length
            exists<Int> { 0 <= it && it < s.length && s[it] == c }
        }
        i += 1
    }
    return 0
}

// The postcondition supplies `res` as a finite ground candidate for `it`. Instantiating the
// body with that candidate reduces the existential to its three quantifier-free conjuncts.
@AlwaysVerify
fun <!VIPER_TEXT!>symbolExistsWithoutGroundTerm<!>(s: String): Int {
    preconditions {
        s.length > 0
    }
    postconditions<Int> { res ->
        0 <= res && res < s.length &&
        exists<Int> { 0 <= it && it < s.length && s[it] == s[res] }
    }
    return 0
}

// Literal candidates are instances too, so this proves the obvious witness without a trigger.
@AlwaysVerify
fun <!VIPER_TEXT!>existsPostcondWithoutTrigger<!>(): Int {
    postconditions<Int> {
        exists<Int> { it == 0 }
    }
    return 0
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsWithGroundLocal<!>(n: Int): Int {
    postconditions<Int> {
        exists<Int> { it == n }
    }
    return n
}

// Finite instantiation is only a proof aid: retaining the original existential is essential.
// A false ground instance must not make a false existential verify.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>falseExistsRemainsFalse<!>(): Int {
    postconditions<Int> {
        exists<Int> { it != it }
    }
    return 0
}<!>
