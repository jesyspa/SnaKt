// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

sealed interface Expr
class Const(val value: Int) : Expr
class Neg(val operand: Const) : Expr

// An exhaustive `when` over a sealed interface with no `else` is total: the missing fallthrough is
// unreachable (`inhale false`), so the function verifies as always returning an Int.
@AlwaysVerify
fun <!VIPER_TEXT!>eval<!>(e: Expr): Int = when (e) {
    is Const -> e.value
    is Neg -> -e.operand.value
}

// Totality is trusted, but branch bodies are still checked. `r` may be negative (e.g. Const(-1)),
// so the assertion below fails to verify even though the `when` is total.
@AlwaysVerify
fun <!VIPER_TEXT!>evalNonNeg<!>(e: Expr): Int {
    val r = when (e) {
        is Const -> e.value
        is Neg -> -e.operand.value
    }
    verify(<!VIPER_VERIFICATION_ERROR!>r >= 0<!>)
    return r
}
