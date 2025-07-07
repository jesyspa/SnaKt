import org.jetbrains.kotlin.formver.plugin.*

// TODO: make verification of this procedure stable.
// Currently, it verifies only in rare cases, while during other runs it times out.
fun <!VIPER_TEXT!>zFuncHelper<!>(s: String, res: String, i: Int, checkedLeft: Int, checkedRight: Int): Int {
    preconditions {
        1 <= i && i < s.length
        res.length == i
        0 <= checkedLeft && checkedLeft <= checkedRight && checkedRight <= s.length
        checkedLeft < i

        forAll<Int> { idx ->
            (0 <= idx && idx < i) implies (
                '0' <= res[idx] && res[idx] <= '0' + s.length - idx && forAll<Int> {
                    (0 <= it && it < res[idx] - '0') implies (s[it] == s[idx + it])
                } && (res[idx] - '0' == s.length - idx || s[idx + (res[idx] - '0')] != s[res[idx] - '0'])
            )
        }

        forAll<Int> {
            (0 <= it && it < checkedRight - checkedLeft) implies (s[checkedLeft + it] == s[it])
        }
    }
    postconditions<Int> { j ->
        i <= j && j <= s.length
        forAll<Int> { idx ->
            (i <= idx && idx < j) implies (s[idx - i] == s[idx])
        }
    }
    return when {
        checkedLeft == 0 || checkedRight <= i -> i
        else -> {
            val bound = i + (res[i - checkedLeft] - '0')
            if (bound < checkedRight) bound else checkedRight
        }
    }
}

@AlwaysVerify
fun String.<!VIPER_TEXT!>zFunction<!>(): String {
    postconditions<String> { res ->
        res.length == length
        forAll<Int> { idx ->
            (0 <= idx && idx < length) implies (
                '0' <= res[idx] && res[idx] <= '0' + length - idx && forAll<Int> {
                    (0 <= it && it < res[idx] - '0') implies (get(it) == get(idx + it))
                } && (res[idx] - '0' == length - idx || get(idx + (res[idx] - '0')) != get(res[idx] - '0'))
            )
        }
    }
    if (length == 0) return this
    var i = 1
    var res = "" + ('0' + length)

    var checkedLeft = 0
    var checkedRight = 0

    while (i < length) {
        loopInvariants {
            res.length == i
            1 <= i && i <= length
            forAll<Int> { idx ->
                (0 <= idx && idx < i) implies (
                    '0' <= res[idx] && res[idx] <= '0' + length - idx && forAll<Int> {
                        (0 <= it && it < res[idx] - '0') implies (get(it) == get(idx + it))
                    } && (res[idx] - '0' == length - idx || get(idx + (res[idx] - '0')) != get(res[idx] - '0'))
                )
            }
            0 <= checkedLeft && checkedLeft <= checkedRight && checkedRight <= length
            checkedLeft < i
            forAll<Int> {
                (0 <= it && it < checkedRight - checkedLeft) implies (get(checkedLeft + it) == get(it))
            }
        }
        var j = zFuncHelper(this, res, i, checkedLeft, checkedRight)
        while (j < length && get(j - i) == get(j)) {
            loopInvariants {
                i <= j && j <= length
                forAll<Int> {
                    (checkedLeft <= it && it < checkedRight) implies (get(it - checkedLeft) == get(it))
                }
                forAll<Int> { idx ->
                    (i <= idx && idx < j) implies (get(idx - i) == get(idx))
                }
            }
            ++j
        }
        res += '0' + (j - i)
        if (j > checkedRight) {
            checkedLeft = i
            checkedRight = j
        }
        ++i
    }
    return res
}

@AlwaysVerify
fun String.<!VIPER_TEXT!>zFunctionNaive<!>(): String {
    postconditions<String> { res ->
        res.length == length
        forAll<Int> { idx ->
            (0 <= idx && idx < length) implies (
                '0' <= res[idx] && res[idx] <= '0' + length - idx && forAll<Int> {
                    (0 <= it && it < res[idx] - '0') implies (get(it) == get(idx + it))
                } && (res[idx] - '0' == length - idx || get(idx + (res[idx] - '0')) != get(res[idx] - '0'))
            )
        }
    }
    if (length == 0) return this
    var i = 1
    var res = "" + ('0' + length)

    while (i < length) {
        loopInvariants {
            res.length == i
            0 <= i && i <= length
            forAll<Int> { idx ->
                (0 <= idx && idx < i) implies (
                    '0' <= res[idx] && res[idx] <= '0' + length - idx && forAll<Int> {
                        (0 <= it && it < res[idx] - '0') implies (get(it) == get(idx + it))
                    } && (res[idx] - '0' == length - idx || get(idx + (res[idx] - '0')) != get(res[idx] - '0'))
                )
            }
        }
        var j = i
        while (j < length && get(j - i) == get(j)) {
            loopInvariants {
                i <= j && j <= length
                forAll<Int> { idx ->
                    (0 <= idx && idx < j - i) implies (get(i + idx) == get(idx))
                }
            }
            ++j
        }
        res += '0' + (j - i)
        ++i
    }
    return res
}