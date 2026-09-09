// FULL_JDK
// USE_STDLIB

import org.jetbrains.kotlin.formver.plugin.*

/*
 * Codeforces 231A, "Team": https://codeforces.com/problemset/problem/231/A
 *
 * Concise paraphrase: three participants independently mark each proposed
 * problem with 0 (unsure) or 1 (sure). Count the proposals for which at least
 * two participants are sure.
 *
 * Input representation: each participant's votes are a string of '0' and '1'
 * characters. Scan the three equally-sized vote strings together and increment
 * the answer exactly when the current three votes sum to at least two.
 * The loop invariants establish safe indexing and that the partial answer is
 * bounded by the number of proposals already inspected.
 */
@AlwaysVerify
fun teamProblemCount(first: String, second: String, third: String): Int {
    preconditions {
        first.length == second.length
        first.length == third.length
        forAll<Int> { i ->
            (0 <= i && i < first.length) implies
                    ((first[i] == '0' || first[i] == '1') &&
                            (second[i] == '0' || second[i] == '1') &&
                            (third[i] == '0' || third[i] == '1'))
        }
    }
    postconditions<Int> { result ->
        0 <= result && result <= first.length
    }

    val size = first.length
    var solved = 0
    var i = 0
    while (i < size) {
        loopInvariants {
            0 <= i && i <= size
            0 <= solved && solved <= i
        }
        var votes = 0
        if (first[i] == '1') votes += 1
        if (second[i] == '1') votes += 1
        if (third[i] == '1') votes += 1
        if (votes >= 2) {
            solved += 1
        }
        i += 1
    }
    return solved
}
