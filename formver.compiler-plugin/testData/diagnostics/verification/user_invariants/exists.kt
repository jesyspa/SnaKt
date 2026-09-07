// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>simpleExists<!>(): Int {
    preconditions {
        exists<Int> { it == 0 }
    }
    return 0
}

// Reading `s[0]` creates a ground term that lets the solver instantiate the invariant's
// existential witness.
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

// Without a ground string-index term, the solver cannot instantiate the existential witness.
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

// This existential is true, but its arithmetic-only body provides no trigger for finding
// a witness.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>existsPostcondWithoutTrigger<!>(): Int {
    postconditions<Int> {
        exists<Int> { it == 0 }
    }
    return 0
}<!>
