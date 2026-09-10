// SOURCE: https://leetcode.com/problems/subtract-the-product-and-sum-of-digits-of-an-integer/
// LeetCode 1281, "Subtract the Product and Sum of Digits of an Integer".
//
// Given a positive decimal integer n, return the product of its digits minus
// the sum of its digits.
//
// Constraints used by the original problem:
//   1 <= n <= 100_000
//
// Examples:
//   n = 234 -> 2 * 3 * 4 - (2 + 3 + 4) = 15
//   n = 4421 -> 4 * 4 * 2 * 1 - (4 + 4 + 2 + 1) = 21
//
// Verification target: retain the input constraint as a precondition and
// justify digit extraction, accumulator bounds, and the loop's progress with
// explicit formal-verification annotations/invariants.

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.implies
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

@AlwaysVerify
fun subtractProductAndSum(n: Int): Int {
    preconditions {
        n >= 1
        n <= 100_000
    }
    postconditions<Int> { result ->
        result >= -54
        result <= 531_441
        (n == 234) implies (result == 15)
        (n == 4421) implies (result == 21)
    }

    var remaining = n
    var product = 1
    var sum = 0
    var processed = 0
    while (remaining > 0) {
        loopInvariants {
            remaining >= 0
            remaining <= n
            processed >= 0
            processed <= 6
            (processed == 0) implies (remaining <= 100_000)
            (processed == 1) implies (remaining <= 10_000)
            (processed == 2) implies (remaining <= 1_000)
            (processed == 3) implies (remaining <= 100)
            (processed == 4) implies (remaining <= 10)
            (processed == 5) implies (remaining <= 1)
            (processed == 6) implies (remaining == 0)
            product >= 0
            product <= 531_441
            sum >= 0
            sum <= 54
            sum <= processed * 9
            (processed == 0) implies (product <= 1)
            (processed == 1) implies (product <= 9)
            (processed == 2) implies (product <= 81)
            (processed == 3) implies (product <= 729)
            (processed == 4) implies (product <= 6_561)
            (processed == 5) implies (product <= 59_049)
        }
        val digit = remaining % 10
        product *= digit
        sum += digit
        remaining /= 10
        processed += 1
    }
    return product - sum
}
