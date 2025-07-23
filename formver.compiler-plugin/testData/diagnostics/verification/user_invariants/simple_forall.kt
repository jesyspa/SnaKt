import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>anyIntegerSquaredAtLeastZero<!>(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            it * it >= 0
            it * it >= res
        }
    }
    return 0
}

@AlwaysVerify
fun <!VIPER_TEXT!>anyIntegerSquaredIsAtLeastOneExceptZero<!>(): Int {
    postconditions<Int> { res ->
        forAll<Int> {
            (it != 0) implies (it * it >= res)
        }
    }
    return 1
}

@AlwaysVerify
fun <!VIPER_TEXT!>anyIntegerSquaredIsAtLeastZeroStringVersion<!>(str: String): Int {
    var res = 0
    var i = 10
    while (i > 0) {
        loopInvariants {
            forAll<Int> {
                (0 <= it && it < str.length) implies ((str[it] - 'a') * (str[it] - 'a') >= res)
            }
        }
        i--
    }
    return res
}
