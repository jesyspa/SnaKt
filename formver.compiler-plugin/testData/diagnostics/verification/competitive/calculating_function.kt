// FULL_JDK

// Codeforces 486A, "Calculating Function".
// https://codeforces.com/problemset/problem/486/A
//
// f(n) = -1 + 2 - 3 + 4 - ... +- n, i.e. the sum of (-1)^i * i for i in 1..n.
// The intended solution is the O(1) closed form; what is worth verifying is
// that the closed form agrees with the definition, which is what the loop
// below establishes.
//
// The real problem has n <= 10^15 and so needs `Long`, which we do not
// support; `n` is an `Int` here. That costs nothing in terms of what is
// proven, since our `Int` is Viper's unbounded integer either way.

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>calculatingFunctionClosedForm<!>(n: Int): Int {
    preconditions {
        n >= 1
    }
    postconditions<Int> { res ->
        (n % 2 == 0) implies (res == n / 2)
        (n % 2 != 0) implies (res == -((n + 1) / 2))
    }

    return if (n % 2 == 0) n / 2 else -((n + 1) / 2)
}

// Sums the series term by term. The invariant is the closed form applied to
// the number of terms consumed so far, `i - 1`, so on exit (`i == n + 1`) it
// is the postcondition.
@AlwaysVerify
fun <!VIPER_TEXT!>calculatingFunctionLoop<!>(n: Int): Int {
    preconditions {
        n >= 1
    }
    postconditions<Int> { res ->
        (n % 2 == 0) implies (res == n / 2)
        (n % 2 != 0) implies (res == -((n + 1) / 2))
    }

    var res = 0
    var i = 1
    while (i <= n) {
        loopInvariants {
            1 <= i && i <= n + 1
            ((i - 1) % 2 == 0) implies (res == (i - 1) / 2)
            ((i - 1) % 2 != 0) implies (res == -(i / 2))
        }
        if (i % 2 == 0) {
            res += i
        } else {
            res -= i
        }
        ++i
    }
    return res
}
