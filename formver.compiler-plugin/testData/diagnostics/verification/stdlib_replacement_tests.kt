// REPLACE_STDLIB_EXTENSIONS
// ALWAYS_VALIDATE

import org.jetbrains.kotlin.formver.plugin.verify

fun <!VIPER_TEXT!>useChecks<!>(): Unit {
    check(true)
    check(true) { "Lazy message" }
}

// TODO: add test for `repeat` (we actually have a bug there because we require unsatisfied precondition in loops)

fun <!VIPER_TEXT!>useRuns<!>(x: Int): Unit {
    val cond1 = run { x + 1 } == 1 + x
    val cond2 = x.run { plus(1) } == 1 + x
    verify(
        cond1,
        cond2,
    )
}

fun <!VIPER_TEXT!>useAlso<!>(x: Int): Unit {
    (x + 1).also { verify(it == 1 + x) }
}

fun <!VIPER_TEXT!>useLet<!>(x: Int): Unit {
    val cond1 = x.let { it + 1 } == 1 + x
    verify(cond1)
}

fun <!VIPER_TEXT!>useWith<!>(x: Int): Unit {
    with(x + 1) { verify(this == 1 + x) }
}

fun <!VIPER_TEXT!>useApply<!>(x: Int): Unit {
    (x + 1).apply { verify(this == 1 + x) }
}

