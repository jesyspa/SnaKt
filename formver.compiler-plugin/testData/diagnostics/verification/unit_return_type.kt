import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

fun <T> <!VIPER_TEXT!>idFun<!>(t: T): T = t

fun <!VIPER_TEXT!>directReturns<!>(b: Boolean) {
    if (b) return
    else if (idFun(b)) return Unit
}

@NeverConvert
inline fun <T> T.unitRun(block: T.() -> Unit) {
    block()
}

@AlwaysVerify
fun <!VIPER_TEXT!>useInlineUnit<!>(b: Boolean) {
    val unitRes = b.unitRun {
        if (this) {
            val tmp = 42
            idFun(tmp)
        }
    }
    verify(unitRes == Unit)
}