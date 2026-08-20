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
// points at, so the cursor's own `next` is moved on the way round the loop. The
// checker keeps that moved field in the state joined at the loop head, which
// makes the next read of `current.next` an access to a moved reference and
// leaves the borrowed cursor with a moved field at exit.
fun `sum values`(head: @Borrowed Node?): Int {
    var current: @Borrowed Node? = head
    var total = 0
    while (current != null) {
        total = total + current.value
        current = <!INVALID_MOVED_ACCESS!>current.next<!>
    }
    <!EXIT_UNIQUENESS_INCONSISTENCY!>return total<!>
}

fun `traverse then consume list`(head: @Unique Node) {
    val total = `sum values`(head)
    consume(head)
}

// Traversing recursively through a borrowed parameter
//
// The same walk the loop above cannot express. Each call borrows the node it is
// handed and restores it at exit, so nothing is left moved and the caller's list
// survives the traversal. What the loop loses is lost in the join at the loop
// head, not in the borrow.

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
// is mutable. That buys nothing for the cursor: the loop form is rejected the
// same way it is for a `var` spine, while the recursive form checks clean and
// may write through the spine it walks.

class RoNode(var value: Int, val next: @Unique RoNode?)

fun consumeRo(n: @Unique RoNode?) {}

fun `sum a readonly spine in a loop`(head: @Borrowed RoNode?): Int {
    var current: @Borrowed RoNode? = head
    var total = 0
    while (current != null) {
        total = total + current.value
        current = <!INVALID_MOVED_ACCESS!>current.next<!>
    }
    <!EXIT_UNIQUENESS_INCONSISTENCY!>return total<!>
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

// Iterative reversal
//
// Commented out: the uniqueness checker does not terminate on this function.
// A run with the body below live was still burning CPU inside the check after
// 40 minutes and produced no diagnostics, so there is no golden to record.
//
// The loop is not on its own what costs. Advancing a unique cursor with
// `current = current.next` and nothing else checks in seconds, and so does
// advancing while writing `null` into the field just read. What those two
// leave out, and what the body below adds, is writing a unique local carried
// over from the previous iteration back into the field.
//
// fun `reverse in place`(head: @Unique Node?): @Unique Node? {
//     var prev: @Unique Node? = null
//     var current: @Unique Node? = head
//     while (current != null) {
//         val next: @Unique Node? = current.next
//         current.next = prev
//         prev = current
//         current = next
//     }
//     return prev
// }
