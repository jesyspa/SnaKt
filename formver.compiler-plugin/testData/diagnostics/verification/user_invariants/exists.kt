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

// Without `val c = s[0]`, `s[0]` is never read before the loop so there is no ground
// term for the `s[it]` trigger to match — the solver cannot construct the witness.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>symbolExistsWithoutGroundTerm<!>(s: String): Int {
    preconditions {
        s.length > 0
    }
    postconditions<Int> { res ->
        0 <= res && res < s.length &&
        exists<Int> { 0 <= it && it < s.length && s[it] == s[res] }
    }
    return 0
}<!>

// Incompleteness path: this existential (`it == 0`) is plainly true, but its body is bare
// arithmetic with no trigger term for the solver to match on. Without model-based quantifier
// instantiation the witness cannot be constructed, so the postcondition is reported as a
// verification warning. This pins "witness not found" — not "existential is false" — and is the
// counterpart to max_character.kt, where a `s[i]` trigger plus a loop invariant let it verify.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>existsPostcondWithoutTrigger<!>(): Int {
    postconditions<Int> {
        exists<Int> { it == 0 }
    }
    return 0
}<!>
