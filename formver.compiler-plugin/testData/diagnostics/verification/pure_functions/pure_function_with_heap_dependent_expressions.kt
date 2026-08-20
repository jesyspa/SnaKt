// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

class Node(val value: Int, val next: Node?)

@Pure
fun <!VIPER_TEXT!>getValue<!>(node: Node): Int {
    return node.value
}

@Pure
fun <!VIPER_TEXT!>getSafeNextValue<!>(node: Node): Int {
    return node.next?.value ?: -1
}

@Pure
fun <!VIPER_TEXT!>getSafeNextNextValue<!>(node: Node): Int {
    return node.next?.next?.value ?: 0
}

@Pure
fun <!VIPER_TEXT!>sumFirstTwoNodes<!>(node: Node): Int {
    val nextNode = node.next
    return if (nextNode != null) {
        node.value + nextNode.value
    } else {
        node.value
    }
}

@Pure
fun <!VIPER_TEXT!>isLastNode<!>(node: Node): Boolean {
    return node.next == null
}

@Pure
fun <!VIPER_TEXT!>length<!>(node: Node): Int {
    val nextNode = node.next
    return if (nextNode == null) {
        1
    } else {
        1 + length(nextNode)
    }
}

@Pure
fun <!VIPER_TEXT!>sumAllNodes<!>(node: Node): Int {
    val nextNode = node.next
    return if (nextNode == null) {
        node.value
    } else {
        node.value + sumAllNodes(nextNode)
    }
}

@Pure
fun <!VIPER_TEXT!>containsValue<!>(node: Node, target: Int): Boolean {
    if (node.value == target) return true
    val nextNode = node.next
    return if (nextNode != null) containsValue(nextNode, target) else false
}

@Pure
fun <!VIPER_TEXT!>aliasAndReassign<!>(node: Node): Int {
    val alias1 = node
    val alias2 = alias1.next
    var fallbackValue = -1

    if (alias2 != null) {
        fallbackValue = alias2.value
    }
    return fallbackValue
}

@Pure
fun <!VIPER_TEXT!>id<!>(node: Node?): Node? {
    return node
}

@Pure
fun <!VIPER_TEXT!>useIdentityFunction<!>(node: Node): Int {
    val sameNode = id(node)
    return sameNode?.value ?: 0
}

@Pure
fun <!VIPER_TEXT!>getNextValueUsingId<!>(node: Node): Int {
    val nextNode = id(node.next)
    return if (nextNode == null) 0 else nextNode.value
}

@Pure
fun <!VIPER_TEXT!>testBorrowed<!>(node: @Unique @Borrowed Node): Unit = Unit
