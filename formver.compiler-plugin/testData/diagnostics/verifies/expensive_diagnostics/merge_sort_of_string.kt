import org.jetbrains.kotlin.formver.plugin.*


@AlwaysVerify
fun String.<!VIPER_TEXT!>subs<!>(lo: Int, hi: Int) : String {
    preconditions {
        0 <= lo && lo <= hi && hi <= length
    }
    postconditions<String> { res ->
        res.length == hi - lo
        forAll<Int> {
            (0 <= it && it < res.length) implies (res[it] == this@subs[it + lo])
        }
    }

    var res = ""
    var i = lo
    while (i < hi) {
        loopInvariants {
            0 <= i && i <= hi
            res.length == i - lo
            forAll<Int> {
                (0 <= it && it < res.length) implies (res[it] == this@subs[it + lo])
            }
        }
        res += this[i++]
    }
    return res
}

@AlwaysVerify
fun <!VIPER_TEXT!>mergeStrings<!>(a: String, b: String): String {
    preconditions {
        forAll<Int> {
            (1 <= it && it < a.length) implies (a[it - 1] <= a[it])
            (1 <= it && it < b.length) implies (b[it - 1] <= b[it])
        }
    }
    postconditions<String> { res ->
        res.length == a.length + b.length
        forAll<Int> {
            (1 <= it && it < res.length) implies (res[it - 1] <= res[it])
        }
    }

    var pa = 0
    var pb = 0
    var res = ""

    val n = a.length + b.length

    while (pa + pb < n) {
        loopInvariants {
            0 <= pa && pa <= a.length
            0 <= pb && pb <= b.length
            res.length == pa + pb
            forAll<Int> {
                (1 <= it && it < res.length) implies (res[it - 1] <= res[it])
            }
            res.length == 0 || pa == a.length || res[res.length - 1] <= a[pa]
            res.length == 0 || pb == b.length || res[res.length - 1] <= b[pb]
        }
        res += when {
            pa == a.length -> b[pb++]
            pb == b.length -> a[pa++]
            a[pa] < b[pb] -> a[pa++]
            else -> b[pb++]
        }
    }
    return res
}

@AlwaysVerify
fun String.<!VIPER_TEXT!>mergeSorted<!>(): String {
    postconditions<String> { res ->
        res.length == length
        forAll<Int> {
            (1 <= it && it < res.length) implies (res[it - 1] <= res[it])
        }
    }
    return if (length <= 1) this
        else mergeStrings(subs(0, length / 2).mergeSorted(), subs(length / 2, length).mergeSorted())
}
