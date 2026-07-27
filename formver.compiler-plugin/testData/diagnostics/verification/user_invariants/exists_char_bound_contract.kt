// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Intended contract (CH-1): a Char-typed quantifier variable should stay within the
// Unicode code-point range [0, 65536), since every real Kotlin Char value does. This
// file states that contract directly in postconditions, which the verifier must
// actually discharge against the (trivial) body, rather than in preconditions, which
// are merely assumed and never proven for a callee-less method.

// Every real Char is at least Char.MIN_VALUE, i.e. the null character '\u0000': a
// universally-quantified statement of that always-true fact depends on the domain
// bound actually being enforced by the lowering, since without a bound the
// quantifier ranges over unbounded Viper Ints, not just valid code points, and
// "forall x: Int, x >= 0" is not provable.
@AlwaysVerify
fun <!VIPER_TEXT!>forallCharNonNegative<!>(): Boolean {
    postconditions<Boolean> {
        forAll<Char> { it >= '\u0000' }
    }
    return true
}

// Dually: no real Char is below Char.MIN_VALUE, so this existential has no witness
// among real Chars and should fail to verify. If the domain bound is missing, the
// quantifier ranges over all Viper Ints and the verifier may instead accept a
// negative "witness" that cannot correspond to any real Char, a false pass that
// would only be possible because the bound is absent.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>existsCharBelowMin<!>(): Boolean {
    postconditions<Boolean> {
        exists<Char> { it < '\u0000' }
    }
    return true
}<!>
