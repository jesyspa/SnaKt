// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique

class B

class A(
    @Unique val data: B,
)

fun takesA(@Unique a: A) {}
fun dropB(@Unique b: B) {}

<!UNIQUENESS_VIOLATION!>fun test(@Unique a: A) {
    dropB(a.data)
    takesA(a)
}<!>

