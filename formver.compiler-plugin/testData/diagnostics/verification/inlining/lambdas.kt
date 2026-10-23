// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@NeverConvert
inline fun invoke(f: (Int) -> Int): Int {
    return f(0)
}

@NeverConvert
fun increment(value: Int): Int = value + 1

@AlwaysVerify
fun functionReference() {
    invoke(<!UNSUPPORTED_FEATURE!>::increment<!>)
}

fun <!VIPER_TEXT!>explicitArg<!>(): Int {
    return invoke { x -> x + x }
}

fun <!VIPER_TEXT!>implicitArg<!>(): Int {
    return invoke { it * 2 }
}

fun <!VIPER_TEXT!>lambdaIf<!>(): Int {
    return invoke {
        if (it == 0) {
            it + 1
        } else {
            it + 2
        }
    }
}

fun <!VIPER_TEXT!>returnValueNotUsed<!>(): Unit {
    invoke { it }
}

fun <!VIPER_TEXT!>shadowing<!>(): Int {
    val x = 1
    val y = 1
    return invoke { x ->
        val y = 0
        x + y
    }
}

@NeverConvert
@Suppress("NOTHING_TO_INLINE")
inline fun nestedHelper(): Int {
    val x = 2
    return invoke { x -> x + 1 }
}

fun <!VIPER_TEXT!>nested<!>(): Int {
    val x = 2
    return nestedHelper()
}

@NeverConvert
inline fun passthroughHelper(f: (Int) -> Int) {
    invoke(f)
}

fun <!VIPER_TEXT!>lambdaPassthrough<!>() {
    passthroughHelper { it + 1 }
}

fun <!VIPER_TEXT!>captureArg<!>(g: (Int) -> Int): Int {
    return invoke { g(it) }
}

fun <!VIPER_TEXT!>captureVar<!>(): Int {
    val x = 1
    return invoke { it + x }
}

fun <!VIPER_TEXT!>captureAndShadow<!>(x: Int): Int {
    return invoke {
        val y = x
        val x = 1
        it + x + y
    }
}

@NeverConvert
inline fun invokeClash(f: (Int) -> Int): Int {
    val x = 1
    return f(0) + x
}

fun <!VIPER_TEXT!>captureVarClash<!>(x: Int): Int {
    return invokeClash { it * x }
}

fun <!VIPER_TEXT!>captureAndShadowClash<!>(x: Int): Int {
    return invokeClash {
        val y = x
        val x = 2
        x + y + it
    }
}

fun <!VIPER_TEXT!>nestedLambdaShadowing<!>(x: Int): Int {
    return invokeClash {
        invokeClash {
            val x = 3
            x + it
        }
        val y = x
        val x = 4
        x + y + it
    }
}

@NeverConvert
inline fun doubleInvoke(f: (Int) -> Int): Int {
    f(0)
    return f(1)
}

fun <!VIPER_TEXT!>callDoubleInvoke<!>(x: Int): Int {
    return doubleInvoke {
        val x = it
        x
    }
}
