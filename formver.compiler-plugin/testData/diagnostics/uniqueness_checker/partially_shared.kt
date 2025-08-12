// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique

class B

class A(
    @Unique val data: B,
)

fun consumeA(@Unique a: A) {}
fun shareB(b: B) {}

<!UNIQUENESS_VIOLATION!>fun test(@Unique a: A) {
    shareB(a.data)
    consumeA(a)
}<!>

