// FULL_JDK

// Codeforces 1328A, "Divisibility Problem".
// https://codeforces.com/problemset/problem/1328/A
//
// Given a and b, find the least number of times a has to be incremented by
// one to become divisible by b. The answer is (b - a % b) % b, and the outer
// `% b` handles the already-divisible case.
//
// This problem is one line of code and three quite different proof
// obligations, which is why it is here. The divisor is symbolic, and Z3 4.8.7
// is much weaker at `x % b` for symbolic `b` than the arithmetic looks:
//
//  - The range clause 0 <= res < b follows from the modulo range axiom alone
//    and goes through.
//  - Exact divisibility stated as `(a + res) % b == 0` does *not* go through:
//    relating two different modulo terms over the same symbolic divisor is
//    beyond what the solver will do. Stated instead as an equation against an
//    explicit quotient, `a + res == b * m`, it goes through immediately --
//    which is what `divisibilityQuotient` below is for.
//  - Minimality is out of reach entirely; see the comment on
//    `divisibilityProblemMinimal`.

import org.jetbrains.kotlin.formver.plugin.*

// The answer itself, specified by the range clause and by the case split that
// defines it. Note that exact divisibility is deliberately *not* claimed here
// -- `divisibilityQuotient` states it in the form the solver can discharge.
@AlwaysVerify
fun <!VIPER_TEXT!>divisibilityProblem<!>(a: Int, b: Int): Int {
    preconditions {
        a >= 1
        b >= 1
    }
    postconditions<Int> { res ->
        0 <= res && res < b
        (a % b == 0) implies (res == 0)
        (a % b != 0) implies (res == b - a % b)
    }

    return (b - a % b) % b
}

// The witness for divisibility: the quotient m with a + answer == b * m. This
// is the clause that makes the answer *correct* rather than merely in range,
// and phrasing it as multiplication rather than as `% b == 0` is what puts it
// inside Z3's reach.
@AlwaysVerify
fun <!VIPER_TEXT!>divisibilityQuotient<!>(a: Int, b: Int): Int {
    preconditions {
        a >= 1
        b >= 1
    }
    postconditions<Int> { m ->
        (a % b == 0) implies (a == b * m)
        (a % b != 0) implies (a + b - a % b == b * m)
    }

    return if (a % b == 0) a / b else a / b + 1
}

// Minimality: no smaller number of increments works. Mathematically this is
// immediate from the two clauses above -- if 0 <= k < res and b divides a + k,
// then res - k is a positive multiple of b smaller than b -- but the solver
// will not get there, and reports the quantified clause as unproven.
//
// The gap is quantifier instantiation, not arithmetic. Handed the instantiated
// core step directly -- `b * d == t` with `0 < t < b` and `b >= 1` is
// unsatisfiable -- Z3 closes it at once. What it will not do is reach that step
// on its own from `(a + k) % b` for a symbolic `b`, so the witness `d` never
// gets constructed. Pinned here rather than dropped, because the day modulo
// reasoning over symbolic divisors improves, this is the test that should
// start passing.
<!VIPER_VERIFICATION_ERROR!>@AlwaysVerify
fun <!VIPER_TEXT!>divisibilityProblemMinimal<!>(a: Int, b: Int): Int {
    preconditions {
        a >= 1
        b >= 1
    }
    postconditions<Int> { res ->
        0 <= res && res < b
        forAll<Int> { k ->
            (0 <= k && k < res) implies ((a + k) % b != 0)
        }
    }

    return (b - a % b) % b
}<!>
