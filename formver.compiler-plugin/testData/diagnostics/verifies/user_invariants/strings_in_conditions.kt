import org.jetbrains.kotlin.formver.plugin.*

fun <!VIPER_TEXT!>firstNotSortedIndex<!>(s: String): Int {
    postconditions<Int> { res ->
        0 <= res && res <= s.length
        forAll<Int> {
            (0 <= it && it + 1 < res) implies (s[it] <= s[it + 1])
        }
    }

    if (s.length == 0) {
        return 0
    } else {
        var i = 1
        while (i < s.length && s[i - 1] <= s[i]) {
            loopInvariants {
                forAll<Int> {
                    (0 <= it && it + 1 < i) implies (s[it] <= s[it + 1])
                }
            }
            ++i
        }
        return i
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>returnNewString<!>(): String {
    return "42"
}

fun <!VIPER_TEXT!>addCharacterTimes<!>(s: String, c: Char, n: Int): String {
    preconditions {
        n >= 0
    }
    postconditions<String> { res ->
        res.length == s.length + n
        forAll<Int> {
            (s.length <= it && it < res.length) implies (res[it] == c)
        }
    }

    var i = 0
    var res = s
    while (i < n) {
        loopInvariants {
            0 <= i && i <= n
            res.length == s.length + i
            forAll<Int> {
                (s.length <= it && it < res.length) implies (res[it] == c)
            }
        }
        res += c
    }

    return res
}