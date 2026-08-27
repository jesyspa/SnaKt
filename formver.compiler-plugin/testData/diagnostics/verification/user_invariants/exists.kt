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

// Checks whether there is a symbol that can be verified
@AlwaysVerify
fun <!VIPER_TEXT!>symbolExists<!>(s: String): Int {
    preconditions {
        s.length > 0
    }
    postconditions<Int> { res ->
        exists<Int> { 0 <= it && it < s.length && s[it] == s[res] }
    }
    return 0
}

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
