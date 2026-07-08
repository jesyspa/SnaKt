// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
}

class B {
    var y: @Unique A
    var z: @Unique A

    constructor(y: @Unique A, z: @Unique A) {
        this.y = y
        this.z = z
    }
}

class C(
    val first: @Unique A,
    val second: @Unique A
)

class D {
    var head: @Unique A = A()
    var tail: @Unique A = A()
}

class E {
    var first: @Unique A
    var second: @Unique A

    constructor(a: @Unique A) {
        this.first = a
        this.second = <!INVALID_MOVED_ACCESS!>a<!>
    }
}

fun consume(a: @Unique Any) {}

fun `construct B with unique args`(a: @Unique A, b: @Unique A) {
    val obj: @Unique B = B(a, b)
    consume(obj)
    consume(<!INVALID_MOVED_ACCESS!>a<!>)
    consume(<!INVALID_MOVED_ACCESS!>b<!>)
}

class F {
    var a: @Unique A
    var b: @Unique A

    constructor(a: @Unique A, b: @Unique A) {
        this.a = a
        this.b = b
    }

    constructor(only: @Unique A) : this(only, F.Companion.newUnique())

    companion object {
        fun newUnique(): @Unique A = A()
    }
}

fun `construct with chained constructor`(x: @Unique A) {
    val f: @Unique F = F(x)
    consume(f)
    consume(<!INVALID_MOVED_ACCESS!>x<!>)
}

class G(val item: @Unique A = A())

fun `construct with default argument`() {
    val g: @Unique G = G()
    consume(g)
}

fun `construct overriding default with unique`(a: @Unique A) {
    val g: @Unique G = G(a)
    consume(g)
    consume(<!INVALID_MOVED_ACCESS!>a<!>)
}
