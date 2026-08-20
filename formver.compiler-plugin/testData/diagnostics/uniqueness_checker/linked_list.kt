// FULL_JDK
// WITH_STDLIB
// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class Node(var value: Int, var next: @Unique Node?)

fun consume(n: @Unique Node?) {}

fun borrow(n: @Borrowed Node?) {}

// Push front

fun `push front`(head: @Unique Node?): @Unique Node {
    return Node(0, head)
}

fun `use old head after push front`(head: @Unique Node?) {
    val fresh: @Unique Node = Node(0, head)
    consume(fresh)
    consume(<!INVALID_MOVED_ACCESS!>head<!>)
}

// Detach

fun `detach tail`(head: @Unique Node): @Unique Node? {
    return head.next
}

fun `return node after detaching tail`(head: @Unique Node): @Unique Node {
    val tail: @Unique Node? = head.next
    consume(tail)
    return <!ESCAPE_UNIQUENESS_INCONSISTENCY!>head<!>
}

// Borrowed traversal

// Advancing the cursor reads the unique `next` field out of the node the cursor
// points at. That read moves the field, but the same assignment points the
// cursor at what it read, so the moved field is no longer reachable through the
// cursor and the loop head has nothing to carry round.
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

// Traversing recursively through a borrowed parameter
//
// The same walk, written the other way. Each call borrows the node it is handed
// and restores it at exit, so nothing is left moved and the caller's list
// survives the traversal.

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

// A spine that cannot be reassigned
//
// `next` is a `val` here, so the chain is fixed once built and only the payload
// is mutable. Both the loop and the recursion walk it without disturbing it, and
// the recursive form may write through the spine as it goes.

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

// A cursor whose field is taken by someone else
//
// Overwriting the cursor only drops the moved mark because the assignment is
// what reads the field. Here the field goes to `consume` first, so the read that
// advances the cursor is a read of a reference already handed away.

fun `drop the tail in a loop`(head: @Unique Node?) {
    var current: @Unique Node? = head
    while (current != null) {
        consume(current.next)
        current = <!INVALID_MOVED_ACCESS!>current.next<!>
    }
}

// Iterative reversal
//
// Writing a unique local carried over from the previous iteration back into the
// field is what deepens the tracked paths: `prev` gains the spine of the node it
// was just assigned, and that spine is written into `current.next` next time
// round. Summarizing a path where a symbol comes round a second time bounds the
// depth, so the states stabilize and the reversal checks.

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
