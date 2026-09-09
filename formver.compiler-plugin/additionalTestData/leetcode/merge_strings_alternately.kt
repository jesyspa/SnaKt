// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

/*
 * LeetCode 1768: Merge Strings Alternately
 * Source: https://leetcode.com/problems/merge-strings-alternately/
 *
 * Problem brief (paraphrased): Build a string by alternating characters from
 * word1 and word2, beginning with word1.  Once either input is exhausted,
 * append the unconsumed suffix of the other input.
 *
 * Contract intent:
 * - There is no precondition; empty strings and unequal lengths are allowed.
 * - The result contains exactly all input characters, so its length is the sum
 *   of the two input lengths.
 * - Up to min(word1.length, word2.length), input character i occurs at result
 *   positions 2*i and 2*i+1 respectively.
 * - Every character after that paired prefix is the corresponding character
 *   of the longer input, in its original order.
 *
 */
@AlwaysVerify
fun mergeStringsAlternately(word1: String, word2: String): String {
    postconditions<String> { res ->
        res.length == word1.length + word2.length
        forAll<Int> {
            (0 <= it && it < word1.length && it < word2.length) implies
                (res[2 * it] == word1[it] && res[2 * it + 1] == word2[it])
        }
        forAll<Int> {
            (word2.length <= it && it < word1.length) implies
                (res[2 * word2.length + it - word2.length] == word1[it])
        }
        forAll<Int> {
            (word1.length <= it && it < word2.length) implies
                (res[2 * word1.length + it - word1.length] == word2[it])
        }
    }

    var res = ""
    var i = 0

    while (i < word1.length && i < word2.length) {
        loopInvariants {
            0 <= i && i <= word1.length && i <= word2.length
            res.length == 2 * i
            forAll<Int> {
                (0 <= it && it < i) implies
                    (res[2 * it] == word1[it] && res[2 * it + 1] == word2[it])
            }
        }
        res += word1[i]
        res += word2[i]
        i += 1
    }

    if (i < word1.length) {
        var j = i
        while (j < word1.length) {
            loopInvariants {
                0 <= i && i < word1.length && i == word2.length
                i <= j && j <= word1.length
                res.length == 2 * i + j - i
                forAll<Int> {
                    (0 <= it && it < i) implies
                        (res[2 * it] == word1[it] && res[2 * it + 1] == word2[it])
                }
                forAll<Int> {
                    (i <= it && it < j) implies
                        (res[2 * i + it - i] == word1[it])
                }
            }
            res += word1[j]
            j += 1
        }
    } else {
        var j = i
        while (j < word2.length) {
            loopInvariants {
                0 <= i && i == word1.length && i <= word2.length
                i <= j && j <= word2.length
                res.length == 2 * i + j - i
                forAll<Int> {
                    (0 <= it && it < i) implies
                        (res[2 * it] == word1[it] && res[2 * it + 1] == word2[it])
                }
                forAll<Int> {
                    (i <= it && it < j) implies
                        (res[2 * i + it - i] == word2[it])
                }
            }
            res += word2[j]
            j += 1
        }
    }

    return res
}
