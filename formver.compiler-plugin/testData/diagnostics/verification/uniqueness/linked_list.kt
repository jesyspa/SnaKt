// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class Link(var data: Int, @Unique val next: Link?)

@AlwaysVerify
fun <!VIPER_TEXT!>getVal<!>(@Unique l: Link): Int? {
    return l.next?.data
}


@AlwaysVerify
fun <!VIPER_TEXT!>test<!>() {
    val l = Link(5, Link(3, null))
    val expr1 = l.data == 5
    verify(expr1)
    val expr2 = l.next?.data == 3
    verify(expr2)
}
