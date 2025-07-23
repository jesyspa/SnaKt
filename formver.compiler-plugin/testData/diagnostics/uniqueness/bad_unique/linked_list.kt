// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique

class Link(@Unique val next: Link?, @Unique var data: Int)

fun <!VIPER_TEXT!>getVal<!>(@Unique l: Link): Int {
    return l.next?.data
}
