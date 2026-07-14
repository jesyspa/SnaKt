// RENDER_PREDICATES
// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class Node(var data: Int, val left: @Unique Node?, val right: @Unique Node?)

fun <!VIPER_TEXT!>get_left_val<!>(n: @Unique Node): Int? {
    return n.left?.data
}

// these expressions should all verify - they currently do not due to lack of uniqueness information
@AlwaysVerify
fun <!VIPER_TEXT!>test<!>() {
    val n = Node(5, Node(4, null, null), Node(3, Node(2, null, null), Node(1, null, null)))
    val expr1 = n.data == 5
    verify(<!VIPER_VERIFICATION_ERROR!>expr1<!>)
    val expr2 = n.left?.data == 4
    verify(expr2)
}
