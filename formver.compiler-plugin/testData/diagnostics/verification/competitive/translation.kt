// FULL_JDK

// Codeforces 41A, "Translation".
// https://codeforces.com/problemset/problem/41/A
//
// Given the word s in one language and t in the other, decide whether t is s
// read backwards.
//
// Both directions of the answer are specified. The positive direction is a
// universal over the whole word; the negative one is an existential, and the
// witness is what the helper below returns -- reading `s[res]` in its
// postcondition is what puts the ground term in scope for the existential's
// trigger to match, the pattern `symbolExists` pins in
// verification/user_invariants/exists.kt.

import org.jetbrains.kotlin.formver.plugin.*

// First index at which s disagrees with t read backwards, or s.length if there
// is none. Same shape as `firstAtLeast` in
// verification/user_invariants/string_iterations.kt: the invariant covers the
// prefix already checked, and the index returned after a break still carries
// the mismatch that caused it.
fun <!VIPER_TEXT!>firstReverseMismatch<!>(s: String, t: String): Int {
    preconditions {
        s.length == t.length
    }
    postconditions<Int> { res ->
        0 <= res && res <= s.length
        forAll<Int> {
            (0 <= it && it < res) implies (s[it] == t[t.length - 1 - it])
        }
        (res != s.length) implies (s[res] != t[t.length - 1 - res])
    }

    var i = 0
    while (i < s.length) {
        loopInvariants {
            0 <= i && i <= s.length
            forAll<Int> {
                (0 <= it && it < i) implies (s[it] == t[t.length - 1 - it])
            }
        }
        if (s[i] != t[t.length - 1 - i]) break
        ++i
    }
    return i
}

@AlwaysVerify
fun <!VIPER_TEXT!>isTranslation<!>(s: String, t: String): Boolean {
    postconditions<Boolean> { res ->
        // The length equality is the left conjunct on purpose: `&&` is
        // short-circuiting for well-definedness, so it is what makes the
        // indexing into `t` on the right legal.
        res implies (s.length == t.length && forAll<Int> {
            (0 <= it && it < s.length) implies (s[it] == t[t.length - 1 - it])
        })
        // Likewise `||` here: the right disjunct may assume the lengths match.
        (!res) implies (s.length != t.length || exists<Int> {
            0 <= it && it < s.length && s[it] != t[t.length - 1 - it]
        })
    }

    if (s.length != t.length) return false
    return firstReverseMismatch(s, t) == s.length
}
