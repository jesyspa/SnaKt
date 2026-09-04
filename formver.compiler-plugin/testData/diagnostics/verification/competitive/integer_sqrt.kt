// FULL_JDK

// LeetCode 69, "Sqrt(x)".
// https://leetcode.com/problems/sqrtx/
//
// Return the square root of a non-negative x rounded down, without using any
// built-in exponent function. Equivalently: the largest integer whose square
// is at most x, which is exactly the postcondition shared by both functions
// below.
//
// Note what is *not* being proven. LeetCode's constraint is x <= 2^31 - 1, so
// the usual C++/Java solution compares `mid > x / mid` to keep `mid * mid`
// from overflowing a 32-bit int. Our `Int` is Viper's unbounded integer, so
// `mid * mid` is written directly here -- and, by the same token, this proof
// says nothing about overflow. It is a proof about the mathematical integers.

import org.jetbrains.kotlin.formver.plugin.*

// O(sqrt x): walk up while the next square still fits. The loop condition
// gives the upper bound on exit and the invariant carries the lower one.
@AlwaysVerify
fun <!VIPER_TEXT!>integerSqrtLinear<!>(x: Int): Int {
    preconditions {
        x >= 0
    }
    postconditions<Int> { res ->
        res >= 0
        res * res <= x
        x < (res + 1) * (res + 1)
    }

    var r = 0
    while ((r + 1) * (r + 1) <= x) {
        loopInvariants {
            r >= 0
            r * r <= x
        }
        ++r
    }
    return r
}

// O(log x): binary search for the last index whose square fits. The invariant
// is the postcondition split across the two bounds -- `lo` always under-shoots
// and `hi` always over-shoots -- so when they meet, `lo` is the answer.
//
// `mid` is biased upward, `lo + (hi - lo + 1) / 2`, which is what makes
// `lo := mid` progress rather than spin.
@AlwaysVerify
fun <!VIPER_TEXT!>integerSqrtBinarySearch<!>(x: Int): Int {
    preconditions {
        x >= 0
    }
    postconditions<Int> { res ->
        res >= 0
        res * res <= x
        x < (res + 1) * (res + 1)
    }

    var lo = 0
    var hi = x
    while (lo < hi) {
        loopInvariants {
            0 <= lo && lo <= hi && hi <= x
            lo * lo <= x
            x < (hi + 1) * (hi + 1)
        }
        val mid = lo + (hi - lo + 1) / 2
        if (mid * mid > x) {
            hi = mid - 1
        } else {
            lo = mid
        }
    }
    return lo
}
