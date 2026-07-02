// FULL_JDK


import org.jetbrains.kotlin.formver.plugin.*

// A Char quantifier variable lowers to a Viper Int. The emitted [0, 65536)
// domain bound keeps it inside the Unicode code-point range instead of ranging
// over negative code points, so a claim true for every real Char verifies
// without a manual guard.

// The lower bound is load-bearing: the body lowers to 0 <= anon, which holds for
// every anon only because the emitted 0 <= anon && anon < 65536 bound excludes the
// negative code points a bare forall x: Int would range over.
@AlwaysVerify
fun <!VIPER_TEXT!>everyCharAtLeastNull<!>(): Int {
    postconditions<Int> {
        forAll<Char> { c -> '\u0000' <= c }
    }
    return 0
}

// exists<Char> likewise emits the [0, 65536) bound (visible in the Viper dump).
// The witness anon == 0 lies in range, but a bare exists without a trigger is not
// instantiated by Silicon, so this reports a verification error rather than
// verifying. That instantiation gap is a separate, currently-deferred limitation;
// this case pins the bound emission for exists<Char> and the known failure mode.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>someCharIsNull<!>(): Int {
    postconditions<Int> {
        exists<Char> { c -> c == '\u0000' }
    }
    return 0
}<!>
