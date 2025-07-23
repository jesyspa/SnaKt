import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun String.<!VIPER_TEXT!>firstAtLeast<!>(c: Char): Int {
    postconditions<Int> { res ->
        0 <= res && res <= length
        forAll<Int> {
            (0 <= it && it < res) implies (get(it) < c)
        }
        (res != length) implies (get(res) >= c)
    }

    var i = 0
    while (i < length) {
        loopInvariants {
            0 <= i && i <= length
            forAll<Int> {
                (0 <= it && it < i) implies (get(it) < c)
            }
        }
        if (get(i) >= c) break
        ++i
    }
    return i
}

@AlwaysVerify
fun String.<!VIPER_TEXT!>lastLess<!>(c: Char): Int {
    postconditions<Int> { res ->
        -1 <= res && res <= length - 1
        forAll<Int> {
            (res < it && it < length) implies (get(it) >= c)
        }
        (res != -1) implies (get(res) < c)
    }
    var i = length - 1
    while (i > -1) {
        loopInvariants {
            -1 <= i && i < length
            forAll<Int> {
                (i < it && it < length) implies (get(it) >= c)
            }
        }
        if (get(i) < c) break
        --i
    }
    return i
}