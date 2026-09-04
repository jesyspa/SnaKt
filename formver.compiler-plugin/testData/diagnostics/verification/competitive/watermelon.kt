// FULL_JDK

// Codeforces 4A, "Watermelon".
// https://codeforces.com/problemset/problem/4/A
//
// A watermelon of weight w is to be divided into two parts, each of positive
// even weight. Decide whether that is possible.
//
// The one-line solution is `w % 2 == 0 && w > 2`; the specification is what
// makes the problem interesting. The two directions are stated differently on
// purpose:
//
//  - Soundness is an existential claim ("a split exists"), but our existential
//    quantifier needs a ground term for its trigger to match, and a bare
//    arithmetic `exists` has none -- see `existsPostcondWithoutTrigger` in
//    verification/user_invariants/exists.kt. So the witness is exhibited
//    constructively instead: the split is always (2, w - 2).
//  - Completeness is a universal claim ("no split exists"), which needs no
//    witness and is stated directly with `forAll`.

import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>watermelon<!>(w: Int): Boolean {
    preconditions {
        1 <= w && w <= 100
    }
    postconditions<Boolean> { res ->
        // When we answer yes, (2, w - 2) really is a valid split.
        res implies (w - 2 > 0 && (w - 2) % 2 == 0)
        // When we answer no, no split whatsoever exists.
        (!res) implies forAll<Int> { a ->
            !(a > 0 && a % 2 == 0 && w - a > 0 && (w - a) % 2 == 0)
        }
    }

    return w % 2 == 0 && w > 2
}
