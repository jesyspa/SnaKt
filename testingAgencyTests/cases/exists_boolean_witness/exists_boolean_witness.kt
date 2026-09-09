// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>existsBooleanWitness<!>(): Boolean {
    postconditions<Boolean> {
        exists<Boolean> { it }
    }
    return true
}
