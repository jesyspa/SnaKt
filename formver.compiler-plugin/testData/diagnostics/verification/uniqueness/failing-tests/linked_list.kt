// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class Link(var data: Int, @Unique val next: Link?)

@NeverVerify
fun <!VIPER_TEXT!>getVal<!>(@Unique l: Link): Int? {
    return l.next?.data
}

// these expressions should also all verify, but currently do not due to lack of uniqueness information
@NeverVerify
fun <!VIPER_TEXT!>test<!>() {
    val l = Link(5, Link(3, null))
    val expr1 = l.data == 5
    verify(<!VIPER_VERIFICATION_ERROR!>expr1)
    val expr2 = l.next?.data == 3
    verify(<!VIPER_VERIFICATION_ERROR!>expr2)
}
