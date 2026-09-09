import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

/**
 * Returns whether reversing [num]'s decimal digits twice preserves the number.
 *
 * A positive trailing zero is discarded by the first reversal and cannot be
 * restored; zero itself and every number with a nonzero final digit survive.
 */
@Pure
@AlwaysVerify
fun isSameAfterReversals(num: Int): Boolean {
    preconditions {
        0 <= num && num <= 1_000_000
    }
    postconditions<Boolean> { result ->
        result == (num == 0 || num % 10 != 0)
    }

    return num == 0 || num % 10 != 0
}
