// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
}

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

fun share(a: Any) {}

// Extension functions with annotated receivers

fun @Unique A.consumeReceiver() {}

fun @Borrowed A.borrowReceiver() {}

fun A.shareReceiver() {}

val @Unique A.uniqueReceiverProperty: Any
    get() = Any()

val @Borrowed A.borrowedReceiverProperty: Any
    get() = Any()

// Calling extension consumes the receiver

fun `consume via extension function on unique`(a: @Unique A) {
    a.consumeReceiver()
    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

// Borrowed extension receivers are restored after the call.
fun `borrow via extension function on unique`(a: @Unique A) {
    a.borrowReceiver()
    consume(a)
}

fun `share via extension function on unique`(a: @Unique A) {
    a.shareReceiver()
    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `unique extension receiver rejects shared`(a: A) {
    <!UNIQUENESS_MISMATCH!>a<!>.consumeReceiver()
}

fun `unique extension property receiver rejects shared`(a: A) {
    val x = <!UNIQUENESS_MISMATCH!>a<!>.uniqueReceiverProperty
}

fun `borrowed extension property receiver accepts unique`(a: @Unique A) {
    val x = a.borrowedReceiverProperty
    consume(a)
}

// Using `this` in an extension body

fun (@Unique A).`consume this in receiver body`() {
    consume(this)
    consume(<!INVALID_MOVED_ACCESS!>this<!>)
}

fun (@Unique A).`borrow this in receiver body`() {
    borrow(this)
    consume(this)
}

// Implicit vs explicit `this`

fun (@Unique A).`borrow implicit then explicit this`() {
    borrow(this)
    borrow(this@`borrow implicit then explicit this`)
    consume(this)
    consume(<!INVALID_MOVED_ACCESS!>this<!>)
}

fun (@Unique A).`consume implicit then explicit this`() {
    consume(this)
    consume(<!INVALID_MOVED_ACCESS!>this@`consume implicit then explicit this`<!>)
    consume(<!INVALID_MOVED_ACCESS!>this<!>)
}

// Mixing receiver and argument in same call

fun (@Borrowed A).borrowSelfAndArg(other: @Borrowed A) {}

fun `borrow same value as receiver and arg`(a: @Unique A) {
    a.borrowSelfAndArg(a)
    consume(a)
}

fun (@Borrowed A).borrowSelfAndConsumeArg(other: @Unique A) {}

fun `borrow value as receiver and consume as arg`(a: @Unique A) {
    a.borrowSelfAndConsumeArg(a)
    consume(a)
}

fun (@Unique A).consumeSelfAndConsumeArg(other: @Unique A) {}

fun `consume value as receiver and consume as arg`(a: @Unique A) {
    <!INVALID_DUPLICATE_UNIQUE_ARGUMENT!>a<!>.consumeSelfAndConsumeArg(<!INVALID_DUPLICATE_UNIQUE_ARGUMENT!>a<!>)
    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun (@Unique A).`consume unique field of unique receiver`() {
    consume(x)
}

fun (@Borrowed @Unique A).`consume unique field of unique local receiver`() <!EXIT_UNIQUENESS_INCONSISTENCY!>{
    consume(x)
}<!>

fun (@Borrowed @Unique A).`consume unique field of unique local receiver and then reassign it`() {
    consume(x)
    x = Any()
}
