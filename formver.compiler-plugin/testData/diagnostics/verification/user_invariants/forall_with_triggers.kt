import org.jetbrains.kotlin.formver.plugin.*

fun <!VIPER_TEXT!>forAllWithSimpleTrigger<!>(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            // Specify trigger expression to guide SMT solver
            triggers(it * it)
            it * it >= 0
            it * it >= res
        }
    }
    return 0
}

fun <!VIPER_TEXT!>forAllWithMultipleTriggers<!>(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            // Multiple trigger expressions can be provided
            triggers(it * it, it + 1)
            (it != 0) implies (it * it >= res)
        }
    }
    return 1
}

fun <!VIPER_TEXT!>forAllWithTriggersInLoop<!>(str: String): Int {
    var res = 0
    var i = 10
    while (i > 0) {
        loopInvariants {
            forAll<Int> {
                // Triggers can be used in loop invariants
                triggers(str[it])
                (0 <= it && it < str.length) implies ((str[it] - 'a') * (str[it] - 'a') >= res)
            }
        }
        i--
    }
    return res
}

fun <!VIPER_TEXT!>forAllWithoutTriggers<!>(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            // forAll without triggers still works (automatic trigger inference)
            it * it >= 0
        }
    }
    return 0
}
