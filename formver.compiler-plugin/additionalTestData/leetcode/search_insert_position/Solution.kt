// Adapted from LeetCode 35, "Search Insert Position":
// https://leetcode.com/problems/search-insert-position/
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun searchInsert(nums: String, target: Char): Int {
    preconditions {
        1 <= nums.length && nums.length <= 10_000
        forAll<Int> { i ->
            forAll<Int> { j ->
                (0 <= i && i < j && j < nums.length) implies (nums[i] < nums[j])
            }
        }
    }
    postconditions<Int> { result ->
        0 <= result && result <= nums.length
        forAll<Int> { i ->
            (0 <= i && i < result) implies (nums[i] < target)
        }
        forAll<Int> { i ->
            (result <= i && i < nums.length) implies (nums[i] >= target)
        }
    }

    var low = 0
    var high = nums.length
    while (low < high) {
        loopInvariants {
            0 <= low && low <= high && high <= nums.length
            forAll<Int> { i ->
                (0 <= i && i < low) implies (nums[i] < target)
            }
            forAll<Int> { i ->
                (high <= i && i < nums.length) implies (nums[i] >= target)
            }
        }
        val middle = low + (high - low) / 2
        if (nums[middle] < target) {
            low = middle + 1
        } else {
            high = middle
        }
    }
    return low
}
