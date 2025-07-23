// REPLACE_STDLIB_EXTENSIONS

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@NeverConvert
public inline fun <R> copiedRun(block: () -> R): R = block()

@NeverConvert
public inline fun <T, R> T.copiedRun(block: T.() -> R): R = block()

@NeverConvert
public inline fun intRun(block: () -> Int): Int = block()

@NeverConvert
public inline fun equalsThree(block: () -> Int): Boolean {
    val result = block()
    return result == 3
}

@NeverConvert
public inline fun equalsThreeParametrized(block: (Int) -> Int): Boolean {
    val result = block(1)
    return result == 3
}

@NeverConvert
public inline fun equalsThreeExtension(block: Int.() -> Int): Boolean {
    val result = 1.block()
    return result == 3
}

@NeverConvert
public inline fun doubleEqualsThree(block: Int.() -> Int): Boolean {
    val result = 1.block().block()
    return result == 3
}

@NeverConvert
public inline fun Int.doubleIntRun(block: Int.() -> Int): Int = block().block()

@OptIn(ExperimentalContracts::class)
public fun <!VIPER_TEXT!>useRun<!>() {
    val genericResult = copiedRun { 1 } + copiedRun { 2 } == copiedRun { 3 }
    val capturedResult = copiedRun { 1 } + copiedRun { 2 } == copiedRun { 3 }
    val intResult = intRun { 1 } + intRun { 2 } == intRun { 3 }
    val stdlibResult = run { 1 } + run { 2 } == run { 3 }
    val doubleIntRunResult = 1.doubleIntRun { plus(1) } == 3
    val genericReceiverResult = 1.copiedRun { plus(2) } == 3

    val cond1 = equalsThree { 1 + 2 }
    val cond2 = !equalsThree { 4 }
    val cond3 = equalsThreeParametrized { it + 2 }
    val cond4 = !equalsThreeParametrized { arg -> arg }
    val cond5 = equalsThreeExtension { this + 2 }
    val cond6 = equalsThreeExtension { plus(2) }
    val cond7 = doubleEqualsThree { plus(1) }

    verify(
        intResult,
        genericResult,
        stdlibResult,
        capturedResult,
        cond1,
        cond2,
        cond3,
        cond4,
        cond5,
        cond6,
        cond7,
        doubleIntRunResult,
        genericReceiverResult
    )
}

@NeverConvert
public inline fun <T> Boolean.ifTrue(block: () -> T?): T? = if (this) block() else null

@OptIn(ExperimentalContracts::class)
public fun <!VIPER_TEXT!>complexScenario<!>(arg: Boolean): Boolean {
    contract {
        returns(true) implies arg
        returns(false) implies !arg
    }

    return arg.ifTrue {
        equalsThreeParametrized {
            it.copiedRun {
                plus(1) // unused
                plus(1) // unused
                doubleIntRun { // receiver is `it` (== 1 in `equalsThreeParametrized`)
                    plus(1) // unused
                    plus(1)
                }
            }
        }
    } ?: copiedRun { // run with no receiver
        equalsThreeExtension {
            copiedRun { // run with receiver
                plus(1).copiedRun { // receiver is `this`
                    plus(1).copiedRun {
                        plus(1)
                    }
                }
            }
        }
    }
}

class CustomClass {
    val member: Int = 42

    @NeverConvert
    inline fun <T> memberRun(block: CustomClass.() -> T): T = block()
}

@NeverConvert
inline fun <T> CustomClass.extensionRun(block: CustomClass.() -> T): T = block()

@AlwaysVerify
fun <!VIPER_TEXT!>testCustomClass<!>() {
    val custom = CustomClass()
    val cond1 = custom.memberRun { member } == custom.extensionRun { member }
    verify(cond1)
}