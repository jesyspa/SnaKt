// UNIQUE_CHECK_ONLY

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

// `when` with a subject. The desugaring of `when (a) { ... }` reads `a` once for the
// implicit subject and treats that read as a consuming access -- so subsequent uses
// inside any branch (and after the `when`) see `a` as moved. The borrow branch below
// only "should" leave `a` unique, but the subject-read move already happened.
//
// TODO: `when (subject)` should not consume the subject if it is only used to drive
// branch dispatch. Today every test below over-flags.

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

// `when` as an expression initializing a local

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

// `when` consuming different sub-properties in different branches. Joining the two
// paths produces a moved sub-state on both children -- the consistency check reports
// one violation per moved descendant.

fun `when consumes different subproperties`(b: @Unique B) {
    when {
        nondet() -> consume(b.y.x)
        else -> consume(b.y.w)
    }

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY, ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}

// `when` consuming the same subproperty in every branch

fun `when consumes same subproperty in all branches`(b: @Unique B) {
    when {
        nondet() -> consume(b.y.x)
        else -> consume(b.y.x)
    }

    // `b.y.x` is moved on every path; the parent leaks moved sub-state.
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}
