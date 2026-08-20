// FULL_JDK
// RENDER_PREDICATES
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

open class WithInheritedVar {
    var inherited: Int = 0
}

class DerivedWithVar(var own: Int) : WithInheritedVar()

interface Apex {
    val tag: Int
        get() = 0
}

interface LeftBranch : Apex

abstract class RightBranch : Apex

class Diamond : LeftBranch, RightBranch()

@AlwaysVerify
fun <!VIPER_TEXT!>buildDerived<!>(): WithInheritedVar = DerivedWithVar(7)

@AlwaysVerify
fun <!VIPER_TEXT!>buildDiamond<!>(): Apex = Diamond()
