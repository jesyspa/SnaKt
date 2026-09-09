import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

/** Pure solution for Codeforces 4A; see 4A-watermelon.md. */
@AlwaysVerify
fun canSplitEvenly(weight: Int): Boolean {
    preconditions {
        1 <= weight && weight <= 100
    }
    postconditions<Boolean> { result ->
        result == (weight > 2 && weight % 2 == 0)
    }

    return weight > 2 && weight % 2 == 0
}
