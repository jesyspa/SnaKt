// RENDER_PREDICATES

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.verify

class Node(var data: Int, @Unique val left: Node?, @Unique val right: Node?)

fun <!VIPER_TEXT!>get_left_val<!>(@Unique n: Node): Int? {
    return n.left?.data
}

// these expressions should all verify
@AlwaysVerify
fun <!VIPER_TEXT!>test<!>() {
    val n = Node(5, Node(4, null, null), Node(3, Node(2, null, null), Node(1, null, null)))
    val expr1 = n.data == 5
    verify(expr1)
    val expr2 = n.left?.data == 4
    verify(expr2)
}