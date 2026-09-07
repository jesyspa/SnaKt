import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun advancingCount(scores: String, k: Int): Int {
    preconditions {
        1 <= scores.length && scores.length <= 50
        1 <= k && k <= scores.length
        forAll<Int> { i ->
            (0 <= i && i < scores.length) implies ('\u0000' <= scores[i] && scores[i] <= 'd')
            (1 <= i && i < scores.length) implies (scores[i - 1] >= scores[i])
        }
    }
    postconditions<Int> { count ->
        0 <= count && count <= scores.length
        forAll<Int> { i ->
            (0 <= i && i < count) implies
                    (scores[i] > '\u0000' && scores[i] >= scores[k - 1])
            (count <= i && i < scores.length) implies
                    (scores[i] <= '\u0000' || scores[i] < scores[k - 1])
        }
    }

    val threshold = scores[k - 1]
    var count = 0
    while (count < scores.length && scores[count] > '\u0000' && scores[count] >= threshold) {
        loopInvariants {
            0 <= count && count <= scores.length
            forAll<Int> { i ->
                (0 <= i && i < count) implies
                        (scores[i] > '\u0000' && scores[i] >= threshold)
            }
        }
        count += 1
    }
    return count
}
