import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun searchInsertPosition(values: String, target: Char): Int {
    preconditions {
        forAll<Int> { i ->
            (0 <= i && i < values.length) implies forAll<Int> { j ->
                (i < j && j < values.length) implies (values[i] < values[j])
            }
        }
    }
    postconditions<Int> { insertionIndex ->
        0 <= insertionIndex && insertionIndex <= values.length
        forAll<Int> { i ->
            (0 <= i && i < insertionIndex) implies (values[i] < target)
        }
        forAll<Int> { i ->
            (insertionIndex <= i && i < values.length) implies (target <= values[i])
        }
    }

    var low = 0
    var high = values.length
    while (low < high) {
        loopInvariants {
            0 <= low && low <= high && high <= values.length
            forAll<Int> { i ->
                (0 <= i && i < low) implies (values[i] < target)
                (high <= i && i < values.length) implies (target <= values[i])
            }
        }
        val middle = low + (high - low) / 2
        if (values[middle] < target) {
            low = middle + 1
        } else {
            high = middle
        }
    }
    return low
}
