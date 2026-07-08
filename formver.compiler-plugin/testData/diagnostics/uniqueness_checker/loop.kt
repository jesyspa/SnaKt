// FULL_JDK
// WITH_STDLIB
// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

fun nondet(): Boolean = false

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

fun `consume unique inside while`(a: @Unique A) {
    while (nondet()) {
        consume(<!INVALID_MOVED_ACCESS!>a<!>)
    }
}

fun `consume unique inside do-while`(a: @Unique A) {
    do {
        consume(<!INVALID_MOVED_ACCESS!>a<!>)
    } while (nondet())
}

fun `consume unique after while`(a: @Unique A) {
    while (nondet()) {
        borrow(a)
    }

    consume(a)
}

fun `consume unique after possibly consuming in loop`(a: @Unique A) {
    while (nondet()) {
        consume(<!INVALID_MOVED_ACCESS!>a<!>)
    }

    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `consume after reassigning unique in loop`() {
    var a: @Unique A = A()

    while (nondet()) {
        consume(a)
        a = A()
    }

    consume(a)
}

fun `borrow unique in while`(a: @Unique A) {
    while (nondet()) {
        borrow(a)
    }

    consume(a)
}

fun `consume captured unique in for`(a: @Unique A) {
    for (i in 0 until 4) {
        consume(<!INVALID_MOVED_ACCESS!>a<!>)
    }
}

fun `consume subproperty in while`(b: @Unique B) {
    while (nondet()) {
        consume(<!INVALID_MOVED_ACCESS!>b.y.x<!>)
    }

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}

fun `consume then break`(a: @Unique A) {
    while (nondet()) {
        consume(a)
        break
    }

    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `consume then continue`(a: @Unique A) {
    while (nondet()) {
        consume(<!INVALID_MOVED_ACCESS!>a<!>)
        continue
    }
}
