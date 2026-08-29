// CHECKERS_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

fun consume(a: @Unique Any?) {}

fun borrow(a: @Borrowed Any) {}

fun newUnique(): @Unique A = A()

fun `safe call read of unique subproperty`(b: @Unique B?) {
    val z: @Unique A? = b?.y
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}

fun `consume via safe call`(a: @Unique A?) {
    consume(a?.x)
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>a<!>)
}

// Not-null assertion

fun `not-null assertion then consume`(a: @Unique A?) {
    consume(a!!.x)
    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>a<!>)
}

fun `consume after not-null assertion`(a: @Unique A?) {
    val nonNull: @Unique A = a!!
    consume(nonNull)
    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}

// Elvis operator

fun `elvis picks unique`(a: @Unique A?, b: @Unique A): @Unique A {
    return a ?: b
}

fun `consume after elvis`(a: @Unique A?, b: @Unique A) {
    val z: @Unique A = a ?: b
    consume(z)
    // Both branches of elvis consume their operand into `z`.
    consume(<!INVALID_MOVED_ACCESS!>a<!>)
    consume(<!INVALID_MOVED_ACCESS!>b<!>)
}

// Null literal as @Unique argument

fun `consume null`() {
    consume(null)
}

// `if (a != null)` smart-cast then consume

fun `smart cast then consume`(a: @Unique A?) {
    if (a != null) {
        consume(a)
        // After smart-cast consume, `a` is moved on the non-null path.
        consume(<!INVALID_MOVED_ACCESS!>a<!>)
    }
}

// Safe call as a borrowed argument

fun `safe call argument to borrow`(a: @Unique A?) {
    if (a != null) {
        borrow(a.x)
        // borrow does not consume the sub-property.
        consume(a.x)
    }
}
