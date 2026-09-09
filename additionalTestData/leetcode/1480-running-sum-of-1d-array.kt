import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.forAll
import org.jetbrains.kotlin.formver.plugin.implies
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.old
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

/** Pure-input solution for LeetCode 1480; see 1480-running-sum-of-1d-array.md. */
@AlwaysVerify
fun runningSum(nums: IntArray): IntArray {
    preconditions {
        1 <= nums.size && nums.size <= 1000
        forAll<Int> { index ->
            (0 <= index && index < nums.size) implies
                (-1_000_000 <= nums[index] && nums[index] <= 1_000_000)
        }
    }
    postconditions<IntArray> { result ->
        result.size == nums.size
        result[0] == nums[0]
        forAll<Int> { index ->
            (1 <= index && index < result.size) implies
                (result[index] == result[index - 1] + nums[index])
        }
        forAll<Int> { index ->
            (0 <= index && index < nums.size) implies
                (nums[index] == old(nums[index]))
        }
    }

    val result = IntArray(nums.size)
    var index = 0
    var sum = 0
    while (index < nums.size) {
        loopInvariants {
            0 <= index && index <= nums.size
            (index == 0) || (sum == result[index - 1])
            forAll<Int> { completed ->
                (1 <= completed && completed < index) implies
                    (result[completed] == result[completed - 1] + nums[completed])
            }
            (index == 0) || (result[0] == nums[0])
            forAll<Int> { originalIndex ->
                (0 <= originalIndex && originalIndex < nums.size) implies
                    (nums[originalIndex] == old(nums[originalIndex]))
            }
        }
        sum += nums[index]
        result[index] = sum
        index++
    }
    return result
}
