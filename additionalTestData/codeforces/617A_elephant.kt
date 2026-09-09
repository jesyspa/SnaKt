// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>minimumElephantSteps<!>(x: Int): Int {
    preconditions {
        1 <= x && x <= 1_000_000
    }
    postconditions<Int> { result ->
        result > 0 && (result - 1) * 5 < x && x <= result * 5
    }

    return (x + 4) / 5
}
