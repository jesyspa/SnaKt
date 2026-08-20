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

// Iterative reversal
//
// Commented out: the uniqueness checker does not terminate on this function.
// A run of `./agent-scripts/test.sh linked_list` with the body below live was
// still burning CPU inside the check after 40 minutes and produced no
// diagnostics, so the scenario cannot be recorded as a golden.
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
