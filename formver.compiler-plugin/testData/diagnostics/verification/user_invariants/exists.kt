// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Green path: an `exists` in a *precondition* is assumed at method entry, so it
// verifies without the solver having to construct a witness. (An `exists` in a
// postcondition must instead be proven; that verifies when a witness is reachable
// — see max_character.kt in the expensive_verification tests.)
@AlwaysVerify
fun <!VIPER_TEXT!>simpleExists<!>(): Int {
    preconditions {
        exists<Int> { it == 0 }
    }
    return 0
}

// Well-formedness path: `s[res]` inside the `exists` body has no bounds guard on
// `res`, so Viper rejects the body as not well-formed ("Index ... might be
// negative"). This pins that a well-formedness failure inside an `exists` body
// surfaces as a VIPER_VERIFICATION_ERROR diagnostic rather than crashing the
// compiler or silently passing. (It is not a failure to prove the existential.)
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>duplicateIndexExists<!>(s: String, res: Int): Int {
    postconditions<Int> {
        exists<Int> { i -> 0 <= i && i < s.length && s[i] == s[res] }
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
