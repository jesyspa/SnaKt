// FULL_JDK

// LeetCode 28, "Find the Index of the First Occurrence in a String".
// https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/
//
// Return the index of the first occurrence of `needle` in `haystack`, or -1 if
// it does not occur. Naive O(n*m) search; `z_function.kt` in this directory is
// the linear-time machinery for the same family of problems.
//
// Two properties are wanted, and they are not equally hard:
//
//  - Soundness -- the index returned really is an occurrence -- is a universal
//    over the needle, and `indexOfNaive` proves it.
//  - First-occurrence -- nothing matches any earlier -- is a universal over
//    start positions whose body is *itself* existential ("some character
//    differs there"). `indexOfNaiveFirst` states it and does not verify; the
//    comment there records what was tried and where exactly it stops.

import org.jetbrains.kotlin.formver.plugin.*

// How much of `needle` matches `haystack` at `start`: `needle.length` on a full
// match, otherwise the index of the first character that differs. Same shape as
// `firstAtLeast` in verification/user_invariants/string_iterations.kt.
fun <!VIPER_TEXT!>matchLength<!>(haystack: String, needle: String, start: Int): Int {
    preconditions {
        0 <= start
        start + needle.length <= haystack.length
    }
    postconditions<Int> { res ->
        0 <= res && res <= needle.length
        forAll<Int> {
            (0 <= it && it < res) implies (haystack[start + it] == needle[it])
        }
        (res != needle.length) implies (haystack[start + res] != needle[res])
    }

    var j = 0
    while (j < needle.length) {
        loopInvariants {
            0 <= j && j <= needle.length
            forAll<Int> {
                (0 <= it && it < j) implies (haystack[start + it] == needle[it])
            }
        }
        if (haystack[start + j] != needle[j]) break
        ++j
    }
    return j
}

// Scans start positions left to right. The `break` leaves `i` at a full match
// and the loop condition still true, which is what the final `if`
// distinguishes: exiting normally means every start position was exhausted.
//
// An empty needle matches at 0, which is what LeetCode asks for.
@AlwaysVerify
fun <!VIPER_TEXT!>indexOfNaive<!>(haystack: String, needle: String): Int {
    postconditions<Int> { res ->
        -1 <= res
        (res >= 0) implies (res + needle.length <= haystack.length && forAll<Int> {
            (0 <= it && it < needle.length) implies (haystack[res + it] == needle[it])
        })
    }

    var i = 0
    while (i + needle.length <= haystack.length) {
        loopInvariants {
            0 <= i
        }
        if (matchLength(haystack, needle, i) == needle.length) break
        ++i
    }
    return if (i + needle.length <= haystack.length) i else -1
}

// The same search, now also claiming the occurrence is the *first* one. This
// does not verify: the invariant is a universal over start positions whose body
// is itself existential ("some character differs at k"), and it is reported as
// possibly not preserved.
//
// Note how narrow the failure is. The recorded diagnostic is a single one, for
// the invariant itself -- both first-occurrence postconditions *are* discharged
// from the invariant once it is assumed. So the specification is the right one
// and only its maintenance across the loop edge is missing.
//
// What makes this worth pinning is that neither half of the step is the
// problem, and the file demonstrates both halves:
//
//  - The witness is available. When `matchLength` stops early at `r`, its
//    postcondition hands over `haystack[i + r] != needle[r]`, which is exactly
//    the witness for `k == i`. The `verify` call in the loop body asserts
//    precisely that, and it goes through.
//  - The inductive step is within Z3's reach. Handed the step on its own --
//    the invariant for `i`, the fresh witness at `i`, and the invariant for
//    `i + 1` as the goal, over uninterpreted functions standing in for the two
//    strings -- Z3 4.8.7 closes it in under a second, with or without MBQI, and
//    with or without the `{ haystack[k + it] }` trigger pattern.
//
// So the gap is in putting the two together across a loop edge, not in the
// arithmetic and not in the trigger. Switching the existential's trigger from
// the inferred `needle[it]` -- which cannot discriminate between start
// positions, since every `k` shares it -- to the discriminating
// `haystack[k + it]` below does not change the outcome either.
//
// Every guard is closed under its own index bounds rather than relying on an
// earlier `ensures` clause, so each conjunct is well-defined on its own.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>indexOfNaiveFirst<!>(haystack: String, needle: String): Int {
    postconditions<Int> { res ->
        -1 <= res
        (res >= 0) implies (res + needle.length <= haystack.length && forAll<Int> {
            (0 <= it && it < needle.length) implies (haystack[res + it] == needle[it])
        })
        // No start position before the answer matches.
        (res >= 0) implies forAll<Int> { k ->
            (0 <= k && k < res && k + needle.length <= haystack.length) implies exists<Int> {
                triggers(haystack[k + it])
                0 <= it && it < needle.length && haystack[k + it] != needle[it]
            }
        }
        // And when there is no answer, no start position matches at all.
        (res == -1) implies forAll<Int> { k ->
            (0 <= k && k + needle.length <= haystack.length) implies exists<Int> {
                triggers(haystack[k + it])
                0 <= it && it < needle.length && haystack[k + it] != needle[it]
            }
        }
    }

    var i = 0
    while (i + needle.length <= haystack.length) {
        loopInvariants {
            0 <= i
            forAll<Int> { k ->
                (0 <= k && k < i && k + needle.length <= haystack.length) implies exists<Int> {
                    triggers(haystack[k + it])
                    0 <= it && it < needle.length && haystack[k + it] != needle[it]
                }
            }
        }
        val r = matchLength(haystack, needle, i)
        if (r == needle.length) break
        verify(0 <= r && r < needle.length, haystack[i + r] != needle[r])
        ++i
    }
    return if (i + needle.length <= haystack.length) i else -1
}<!>
