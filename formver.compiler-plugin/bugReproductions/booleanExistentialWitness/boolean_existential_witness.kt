// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Issue #297: existential quantifiers over Boolean do not find trivial witnesses.

@AlwaysVerify
fun <!VIPER_TEXT!>existsTruePredicate<!>(): Boolean {
    postconditions<Boolean> {
        exists<Boolean> { it }
    }
    return true
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsFalsePredicate<!>(): Boolean {
    postconditions<Boolean> {
        exists<Boolean> { !it }
    }
    return false
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsBooleanEqualTrue<!>(): Boolean {
    postconditions<Boolean> {
        exists<Boolean> { it == true }
    }
    return true
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsBooleanEqualFalse<!>(): Boolean {
    postconditions<Boolean> {
        exists<Boolean> { it == false }
    }
    return false
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsBooleanMatchesResult<!>(): Boolean {
    postconditions<Boolean> { result ->
        exists<Boolean> { it == result }
    }
    return true
}

@AlwaysVerify
fun <!VIPER_TEXT!>existsBooleanMatchesParameter<!>(value: Boolean): Boolean {
    postconditions<Boolean> {
        exists<Boolean> { it == value }
    }
    return value
}

@AlwaysVerify
fun <!VIPER_TEXT!>nestedBooleanExistential<!>(): Boolean {
    postconditions<Boolean> {
        exists<Boolean> { outer -> exists<Boolean> { inner -> outer || inner } }
    }
    return true
}

// Control: an existential assumption is accepted; the failure is in proving one.
@AlwaysVerify
fun <!VIPER_TEXT!>booleanExistentialPrecondition<!>(): Boolean {
    preconditions {
        exists<Boolean> { it }
    }
    return true
}

// Control: an unsatisfiable existential must continue to fail verification.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>impossibleBooleanExistential<!>(): Boolean {
    postconditions<Boolean> {
        exists<Boolean> { it && !it }
    }
    return false
}<!>

// Control for issue scope: witness discovery also fails for a trivial Int equality.
@AlwaysVerify
fun <!VIPER_TEXT!>existsIntegerWitnessControl<!>(): Int {
    postconditions<Int> {
        exists<Int> { it == 1 }
    }
    return 1
}
