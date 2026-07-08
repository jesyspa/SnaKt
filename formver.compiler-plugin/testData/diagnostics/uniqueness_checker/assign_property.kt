// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
    var w: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
    var x: @Unique A = A()
}

class C {
    var x: @Unique B = B()
}

class R(
    var um: @Unique R,
    val ui: @Unique R,
    var sm: R,
    val si: R,
)

fun borrow(x: @Unique @Borrowed A) {}

fun consume(a: @Unique Any) {}

// Sub-property assignments

fun `return local after assigning its property`(x: @Unique B) {
    val y = x.y
    val z = x

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>z<!>)
}

fun `assign shared to unique subproperty`(x: @Unique B, v: A): Unit {
    x.y = <!UNIQUENESS_MISMATCH!>v<!>
}

fun `assign borrowed to unique subproperty`(x: @Unique B, v: @Borrowed A): Unit {
    x.y = <!LOCALITY_MISMATCH, UNIQUENESS_MISMATCH!>v<!>
}

fun `assign unique to unique subproperty`(x: @Unique B, v: @Unique A): Unit {
    x.y = v

    consume(x)
}

fun `assign unique-borrowed to unique subproperty`(x: @Unique B, v: @Unique @Borrowed A): Unit {
    x.y = <!LOCALITY_MISMATCH!>v<!>

    consume(x)
}

// Property reads through if-expression initializers

fun `assign local to local in if`(a: @Unique A) {
    val x: @Unique Any = if (false) {
        val x: @Unique Any = a.x
        x
    } else {
        a.x
    }
}

fun `assign property to local in if`(c: @Unique C) {
    val x = (if (false) {
        val y: @Unique A = c.x.x
        y
    } else {
        c.x.x
    }).x
}

fun `consume nested unique after moving back`(a: @Unique R) {
    val b: @Unique R = a.um
    consume(b.um)
    a.um = b

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>a<!>)
}
