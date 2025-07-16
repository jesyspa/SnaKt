// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique

<!UNIQUENESS_VIOLATION!>fun f(@Unique x: Int) {
    val y = x

    val z = x
}<!>
