// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.exists
import org.jetbrains.kotlin.formver.plugin.preconditions

// Topic: an explicit string-access trigger inside an existential quantifier.
@AlwaysVerify
fun <!VIPER_TEXT!>existsWithExplicitTrigger<!>(value: String): Int {
    preconditions {
        exists<Int> { index ->
            triggers(value[index])
            0 <= index && index < value.length && value[index] == 'x'
        }
    }
    return value.length
}
