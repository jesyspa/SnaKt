// UNIQUE_CHECK_ONLY
// LANGUAGE: +ContextParameters

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class Box {
    var item: @Unique Any = Any()
    var other: @Unique Any = Any()
}

context(box: @Borrowed Box)
fun borrowContext() {}

context(box: @Unique Box)
fun consumeContext() {}

context(box1: @Unique Box)
fun consumeContextAndArg(box2: @Unique Box) {}

context(box1: @Borrowed Box)
fun borrowContextAndConsumeArg(box2: @Unique Box) {}

context(box1: @Unique Box)
fun (@Unique Box).consumeContextAndReceiver() {}

context(box1: @Unique Box)
fun (@Unique Any).consumeContextAndItemReceiver() {}

context(box: Box)
fun shareContext() {}

context(box: @Unique Box)
fun consumeContextItem() {
    consume(box.item)
}

context(box: @Unique Box)
val uniqueContextProperty: Any
    get() = Any()

context(box: @Borrowed Box)
val borrowedContextProperty: Any
    get() = Any()

context(box1: @Unique Box)
val @Unique Box.contextAndReceiverProperty: Any
    get() = Any()

context(box1: @Unique Box)
val @Unique Any.contextAndItemReceiverProperty: Any
    get() = Any()

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

// Context call arguments

fun `borrowed context through with moves unique value`(box: @Unique Box) {
    with(box) {
        borrowContext()
    }

    consume(<!INVALID_MOVED_ACCESS!>box<!>)
}

fun `unique context consumes unique value`(box: @Unique Box) {
    with(box) {
        consumeContext()
    }

    consume(<!INVALID_MOVED_ACCESS!>box<!>)
}

fun `shared context consumes unique value`(box: @Unique Box) {
    with(box) {
        shareContext()
    }

    consume(<!INVALID_MOVED_ACCESS!>box<!>)
}

fun `borrowed context rejects moved value`(box: @Unique Box) {
    consume(box)

    with(<!INVALID_MOVED_ACCESS!>box<!>) {
        borrowContext()
    }
}

// Context parameter bodies

context(box: @Unique Box)
fun `borrow context parameter then consume`() {
    borrow(box)
    consume(box)
}

context(box: @Unique Box)
fun `consume context parameter twice`() {
    consume(box)
    consume(<!INVALID_MOVED_ACCESS!>box<!>)
}

context(box: @Unique Box)
fun `consume context child then exit`() {
    consume(box.item)
}

context(box: @Unique Box)
fun `consume context child then restore`() {
    consume(box.item)
    box.item = Any()
}

// Context parameter collisions

context(box: @Unique Box)
fun `consume as context and argument`() {
    <!INVALID_DUPLICATE_UNIQUE_ARGUMENT!>consumeContextAndArg(<!INVALID_DUPLICATE_UNIQUE_ARGUMENT!>box<!>)<!>
}

context(box: @Unique Box)
fun `borrow as context and consume as argument`() {
    borrowContextAndConsumeArg(box)
}

context(box: @Unique Box)
fun `consume as context and receiver`() {
    <!INVALID_DUPLICATE_UNIQUE_ARGUMENT!><!INVALID_DUPLICATE_UNIQUE_ARGUMENT!>box<!>.consumeContextAndReceiver()<!>
}

context(box: @Unique Box)
fun `consume as context and item receiver`() {
    <!INVALID_OVERLAPPING_UNIQUE_ARGUMENTS!><!INVALID_OVERLAPPING_UNIQUE_ARGUMENTS!>box.item<!>.consumeContextAndItemReceiver()<!>
}

context(box: @Unique Box)
fun `consume as context property only`() {
    val x = uniqueContextProperty
}

context(box: Box)
fun `unique context property rejects shared context`() {
    val x = <!CONTEXT_UNIQUENESS_MISMATCH!>uniqueContextProperty<!>
}

context(box: @Unique Box)
fun `borrowed context property accepts unique context`() {
    val x = borrowedContextProperty
}

context(box: @Unique Box)
fun `consume as context and property receiver`() {
    val x = <!INVALID_DUPLICATE_UNIQUE_ARGUMENT!><!INVALID_DUPLICATE_UNIQUE_ARGUMENT!>box<!>.contextAndReceiverProperty<!>
}

context(box: @Unique Box)
fun `consume as context and item property receiver`() {
    val x = <!INVALID_OVERLAPPING_UNIQUE_ARGUMENTS!><!INVALID_OVERLAPPING_UNIQUE_ARGUMENTS!>box.item<!>.contextAndItemReceiverProperty<!>
}
