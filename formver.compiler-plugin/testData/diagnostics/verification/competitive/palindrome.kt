// FULL_JDK

// LeetCode 125, "Valid Palindrome" / LeetCode 344, "Reverse String".
// https://leetcode.com/problems/valid-palindrome/
//
// Decide whether a string reads the same forwards and backwards. LeetCode 125
// also asks for case folding and non-alphanumeric skipping, which needs
// `Char.isLetterOrDigit` and `lowercaseChar`; neither is modelled, so this is
// the core two-pointer check on the characters as given.
//
// The invariant is stated over *both* ends at once -- the processed prefix
// [0, lo) and the processed suffix (hi, length) -- rather than over the prefix
// alone. That is the whole trick: on exit the only index the invariant does not
// already cover is the middle one, where `it == length - 1 - it` holds by
// arithmetic. Stated over the prefix only, the solver would instead have to
// instantiate the invariant at the reflected index `length - 1 - it` to reach
// the second half, which is a much harder step for it to find.

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun String.<!VIPER_TEXT!>isPalindrome<!>(): Boolean {
    postconditions<Boolean> { res ->
        res implies forAll<Int> {
            (0 <= it && it < length) implies (get(it) == get(length - 1 - it))
        }
        (!res) implies exists<Int> {
            0 <= it && it < length && get(it) != get(length - 1 - it)
        }
    }

    var lo = 0
    var hi = length - 1
    while (lo < hi) {
        loopInvariants {
            // `lo + hi == length - 1` keeps the two pointers reflections of
            // each other, which is what makes the middle index collapse below.
            0 <= lo && -1 <= hi && hi < length && lo + hi == length - 1
            forAll<Int> {
                ((0 <= it && it < lo) || (hi < it && it < length)) implies
                        (get(it) == get(length - 1 - it))
            }
        }
        if (get(lo) != get(hi)) break
        ++lo
        --hi
    }
    return lo >= hi
}
