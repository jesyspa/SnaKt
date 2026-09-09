// Adapted from Codeforces 41A, "Translation":
// https://codeforces.com/problemset/problem/41/A
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun isReverse(word: String, candidate: String): Boolean {
    preconditions {
        1 <= word.length && word.length <= 100
        1 <= candidate.length && candidate.length <= 100
        forAll<Int> { i ->
            (0 <= i && i < word.length) implies
                    ('a' <= word[i] && word[i] <= 'z')
        }
        forAll<Int> { i ->
            (0 <= i && i < candidate.length) implies
                    ('a' <= candidate[i] && candidate[i] <= 'z')
        }
    }
    postconditions<Boolean> { result ->
        result == (word.length == candidate.length && forAll<Int> { i ->
            (0 <= i && i < word.length) implies
                    (word[i] == candidate[word.length - 1 - i])
        })
    }

    if (word.length != candidate.length) return false

    var i = 0
    var matches = true
    while (i < word.length) {
        loopInvariants {
            0 <= i && i <= word.length
            word.length == candidate.length
            matches == forAll<Int> { j ->
                (0 <= j && j < i) implies
                        (word[j] == candidate[word.length - 1 - j])
            }
        }
        if (word[i] != candidate[word.length - 1 - i]) {
            matches = false
        }
        i += 1
    }
    return matches
}
