// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// Agency topic: existential quantification over Boolean values.
@AlwaysVerify
fun <!VIPER_TEXT!>existsBooleanWitness<!>(flag: Boolean): Boolean {
    preconditions {
        exists<Boolean> { witness -> witness == flag }
    }
    postconditions<Boolean> { result ->
        result == flag
    }
    return flag
}
