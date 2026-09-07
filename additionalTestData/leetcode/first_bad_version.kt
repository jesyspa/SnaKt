import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun firstBadVersion(versions: String): Int {
    preconditions {
        versions.length > 0
        versions[versions.length - 1] == '1'
        forAll<Int> { i ->
            (0 <= i && i < versions.length) implies
                    (versions[i] == '0' || versions[i] == '1')
            (0 <= i && i < versions.length) implies forAll<Int> { j ->
                (i < j && j < versions.length) implies (versions[i] <= versions[j])
            }
        }
    }
    postconditions<Int> { firstBad ->
        0 <= firstBad && firstBad < versions.length
        versions[firstBad] == '1'
        forAll<Int> { i ->
            (0 <= i && i < firstBad) implies (versions[i] == '0')
        }
    }

    var low = 0
    var high = versions.length - 1
    while (low < high) {
        loopInvariants {
            0 <= low && low <= high && high < versions.length
            versions[high] == '1'
            forAll<Int> { i ->
                (0 <= i && i < low) implies (versions[i] == '0')
            }
        }
        val middle = low + (high - low) / 2
        if (versions[middle] == '0') {
            low = middle + 1
        } else {
            high = middle
        }
    }
    return low
}
