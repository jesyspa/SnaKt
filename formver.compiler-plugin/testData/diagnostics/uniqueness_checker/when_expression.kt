// CHECKERS_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
    var w: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

fun nondet(): Boolean = false

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

fun share(a: Any) {}

fun `when statement consumes in one branch`(a: @Unique A) {
    when {
        nondet() -> consume(a)
        else -> {}
    }

    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `when statement consumes in all branches`(a: @Unique A) {
    when {
        nondet() -> consume(a)
        nondet() -> consume(a)
        else -> consume(a)
    }

    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `when statement only borrows`(a: @Unique A) {
    when {
        nondet() -> borrow(a)
        else -> borrow(a)
    }

    consume(a)
}

fun `when subject borrows`(a: @Unique Any) {
    when (a) {
        is A -> borrow(a)
        else -> {}
    }

    consume(a)
}

fun `when subject consumes in branch`(a: @Unique Any) {
    when (a) {
        is A -> consume(a)
        else -> {}
    }

    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `when expression as initializer`(a: @Unique A, b: @Unique A): @Unique A {
    val z: @Unique A = when {
        nondet() -> a
        else -> b
    }

    // Both branches were consumed by the when result, so neither is reusable.
    consume(<!INVALID_MOVED_ACCESS!>a<!>)
    consume(<!INVALID_MOVED_ACCESS!>b<!>)
    return z
}

fun `when consumes different subproperties`(b: @Unique B) {
    when {
        nondet() -> consume(b.y.x)
        else -> consume(b.y.w)
    }

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY, ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}

fun `when consumes same subproperty in all branches`(b: @Unique B) {
    when {
        nondet() -> consume(b.y.x)
        else -> consume(b.y.x)
    }

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}
