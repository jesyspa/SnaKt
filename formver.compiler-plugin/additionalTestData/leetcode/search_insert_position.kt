// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.*

/*
 * Search Insert Position (LeetCode 35)
 * Source: https://leetcode.com/problems/search-insert-position/
 *
 * Given a nondecreasing string and a target character, return the first position whose
 * value is at least the target, or the list size if no such value exists.
 * Inserting the target at that position preserves the ordering.
 *
 * This character-based variant uses a linear scan: the prefix invariant records
 * that every skipped value is smaller than the target, making both safety and
 * the returned position's minimality explicit.
 */
@AlwaysVerify
fun searchInsertPosition(nums: String, target: Char): Int {
    preconditions {
        forAll<Int> { i ->
            (1 <= i && i < nums.length) implies (nums[i - 1] <= nums[i])
        }
    }
    postconditions<Int> { result ->
        0 <= result && result <= nums.length
        forAll<Int> { i ->
            (0 <= i && i < result) implies (nums[i] < target)
        }
        (result < nums.length) implies (target <= nums[result])
    }

    val size = nums.length
    var index = 0
    while (index < size && nums[index] < target) {
        loopInvariants {
            0 <= index && index <= size
            forAll<Int> { i ->
                (0 <= i && i < index) implies (nums[i] < target)
            }
        }
        index += 1
    }
    return index
}
