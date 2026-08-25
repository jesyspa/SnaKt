// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Green path: an `exists` in a *precondition* is assumed at method entry, so it
// verifies without needing the solver to construct a witness. This is the only
// position in which `exists` currently verifies — discharging an `exists` in a
// postcondition needs MBQI, which is not yet enabled (see the follow-up branch
// introducing `unsafeExists`).
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
