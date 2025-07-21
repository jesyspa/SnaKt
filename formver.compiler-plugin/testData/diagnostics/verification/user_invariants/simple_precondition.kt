import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify
import org.jetbrains.kotlin.formver.plugin.preconditions

@AlwaysVerify
fun <!VIPER_TEXT!>test<!>(idx: Int) {
    preconditions {
        0 <= idx
        idx < 3
    }
    verify(0 <= idx, idx < 3, idx != 100)
    verify("aaa"[idx] == 'a')
}


@AlwaysVerify
@Suppress("NOTHING_TO_INLINE")
inline fun <!VIPER_TEXT!>inlineWithSpecification<!>(bool: Boolean) {
    preconditions {
        true
        bool
    }
}

@AlwaysVerify
fun <!VIPER_TEXT!>good_call<!>() {
    test(2)

    // TODO: come up with a proper design for inlining functions with specifications
    // Currently the precondition is not checked
    inlineWithSpecification(true)
    inlineWithSpecification(false)
}
