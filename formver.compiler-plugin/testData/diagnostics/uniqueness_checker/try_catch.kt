// FULL_JDK
// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

fun mayThrow() {}

fun `consume in try only`(a: @Unique A) {
    try {
        consume(a)
    } catch (_: Throwable) {}

    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `consume in try and catch`(a: @Unique A) {
    try {
        consume(a)
    } catch (_: Throwable) {
        consume(<!INVALID_MOVED_ACCESS!>a<!>)
    }

    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `try only side-effects`(a: @Unique A) {
    try {
        mayThrow()
        borrow(a)
    } catch (_: Throwable) {}

    consume(a)
}

fun `consume in finally`(a: @Unique A) {
    try {
        mayThrow()
    } finally {
        consume(a)
    }

    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `consume in try then finally borrow`(a: @Unique A) {
    try {
        consume(a)
    } finally {
        borrow(<!INVALID_MOVED_ACCESS!>a<!>)
    }

    consume(a)
}

fun `try expression as initializer`(a: @Unique A): @Unique A {
    val r: @Unique A = try { a } catch (_: Throwable) { A() }
    consume(<!INVALID_MOVED_ACCESS!>a<!>)
    return r
}

fun `consume subproperty in try`(b: @Unique B) {
    try {
        consume(b.y)
    } catch (_: Throwable) {}

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}

fun `consume in alternative catches`(a: @Unique A) {
    try {
        mayThrow()
    } catch (_: IllegalStateException) {
        consume(a)
    } catch (_: Throwable) {
        consume(a)
    }

    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}
