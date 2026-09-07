import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun canSplitEven(weight: Int): Boolean {
    preconditions {
        1 <= weight && weight <= 100
    }
    postconditions<Boolean> { canSplit ->
        canSplit == (weight > 2 && weight % 2 == 0)
    }

    return weight > 2 && weight % 2 == 0
}
