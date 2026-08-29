// FULL_JDK
// WITH_STDLIB
// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class Node(var value: Int, var next: @Unique Node?)

fun consume(n: @Unique Node?) {}

fun borrow(n: @Borrowed Node?) {}

fun `push front`(head: @Unique Node?): @Unique Node {
    return Node(0, head)
}

fun `use old head after push front`(head: @Unique Node?) {
    val fresh: @Unique Node = Node(0, head)
    consume(fresh)
    consume(<!INVALID_MOVED_ACCESS!>head<!>)
}

fun `detach tail`(head: @Unique Node): @Unique Node? {
    return head.next
}

fun `return node after detaching tail`(head: @Unique Node): @Unique Node {
    val tail: @Unique Node? = head.next
    consume(tail)
    return <!ESCAPE_UNIQUENESS_INCONSISTENCY!>head<!>
}

// Advancing the cursor reads `next` out of the node it is leaving; the same assignment points the cursor at what it
// read, so nothing moved stays reachable through the cursor.
fun `sum values`(head: @Borrowed Node?): Int {
    var current: @Borrowed Node? = head
    var total = 0
    while (current != null) {
        total = total + current.value
        current = current.next
    }
    return total
}

fun `traverse then consume list`(head: @Unique Node) {
    val total = `sum values`(head)
    consume(head)
}

// The same walk by recursion: each call restores the node it borrowed at exit.
fun length(n: @Borrowed Node?): Int = if (n == null) 0 else 1 + length(n.next)

fun contains(n: @Borrowed Node?, v: Int): Boolean =
    if (n == null) false else if (n.value == v) true else contains(n.next, v)

fun `use list after recursive length`(head: @Unique Node) {
    val len = length(head)
    consume(head)
}

fun `use list after recursive search`(head: @Unique Node) {
    val found = contains(head, 3)
    consume(head)
}

// `next` is a `val` here, so only the payload is mutable.
class RoNode(var value: Int, val next: @Unique RoNode?)

fun consumeRo(n: @Unique RoNode?) {}

fun `sum a readonly spine in a loop`(head: @Borrowed RoNode?): Int {
    var current: @Borrowed RoNode? = head
    var total = 0
    while (current != null) {
        total = total + current.value
        current = current.next
    }
    return total
}

fun `sum a readonly spine recursively`(n: @Borrowed RoNode?): Int =
    if (n == null) 0 else n.value + `sum a readonly spine recursively`(n.next)

fun `bump a readonly spine recursively`(n: @Borrowed RoNode?) {
    if (n == null) return
    n.value = n.value + 1
    `bump a readonly spine recursively`(n.next)
}

fun `use readonly spine list after recursion`(head: @Unique RoNode) {
    val total = `sum a readonly spine recursively`(head)
    `bump a readonly spine recursively`(head)
    consumeRo(head)
}

// The field goes to `consume` before the assignment reads it, so the advance is a read of a reference already handed
// away.
fun `drop the tail in a loop`(head: @Unique Node?) {
    var current: @Unique Node? = head
    while (current != null) {
        consume(current.next)
        current = <!INVALID_MOVED_ACCESS!>current.next<!>
    }
}

// `prev` gains the spine of the node assigned to it and that spine is written into `current.next` next time round,
// which is what deepened the tracked paths until summarization bounded them.
fun `reverse in place`(head: @Unique Node?): @Unique Node? {
    var prev: @Unique Node? = null
    var current: @Unique Node? = head
    while (current != null) {
        val next: @Unique Node? = current.next
        current.next = prev
        prev = current
        current = next
    }
    return prev
}
