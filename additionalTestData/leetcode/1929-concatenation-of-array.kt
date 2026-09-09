import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.forAll
import org.jetbrains.kotlin.formver.plugin.implies
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.old
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

/** Pure-input solution for LeetCode 1929; see 1929-concatenation-of-array.md. */
@AlwaysVerify
fun getConcatenation(nums: IntArray): IntArray {
    preconditions {
        1 <= nums.size && nums.size <= 1000
        forAll<Int> { index ->
            (0 <= index && index < nums.size) implies
                (1 <= nums[index] && nums[index] <= 1000)
        }
    }
    postconditions<IntArray> { result ->
        result.size == 2 * nums.size
        forAll<Int> { index ->
            (0 <= index && index < nums.size) implies
                (result[index] == nums[index] && result[index + nums.size] == nums[index])
        }
        forAll<Int> { index ->
            (0 <= index && index < nums.size) implies
                (nums[index] == old(nums[index]))
        }
    }

    val result = IntArray(nums.size * 2)
    var index = 0
    while (index < nums.size) {
        loopInvariants {
            0 <= index && index <= nums.size
            forAll<Int> { completed ->
                (0 <= completed && completed < index) implies
                    (result[completed] == nums[completed] &&
                        result[completed + nums.size] == nums[completed])
            }
            forAll<Int> { originalIndex ->
                (0 <= originalIndex && originalIndex < nums.size) implies
                    (nums[originalIndex] == old(nums[originalIndex]))
            }
        }
        result[index] = nums[index]
        result[index + nums.size] = nums[index]
        index++
    }
    return result
}
