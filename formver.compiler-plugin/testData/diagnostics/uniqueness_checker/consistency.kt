// UNIQUE_CHECK_ONLY
// LANGUAGE: +ContextParameters

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

class A {
    var x: @Unique Any = Any()
    var w: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

class C {
    var b: @Unique B = B()
}

class D {
    var c: @Unique C = C()
}

class N(
    var b: @Unique N,
    var c: @Unique N,
)

fun consume(a: @Unique Any) {}

fun borrow(a: @Borrowed Any) {}

fun share(a: Any) {}

fun nondet(): Boolean = false

fun `consume grandchild leaks grandparent`(d: @Unique D) {
    consume(d.c.b.y)

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>d<!>)
}

fun `consume two siblings leaks parent`(b: @Unique B) {
    consume(b.y.x)
    consume(b.y.w)

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY, ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}

fun `consume then reassign sub-property does not leak`(b: @Unique B, fresh: @Unique A) {
    consume(b.y)
    b.y = fresh

    consume(b)
}

fun `consume one of two siblings then borrow parent`(b: @Unique B) {
    consume(b.y.x)
    borrow(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}

fun `return parent after consuming child`(b: @Unique B): @Unique B {
    consume(b.y.x)

    return <!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>
}

fun `consume in if then use parent`(b: @Unique B) {
    if (nondet()) {
        consume(b.y)
    }

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}

fun `borrow parent leaves children unique`(b: @Unique B) {
    borrow(b)
    consume(b.y)
}

fun `consume parent then access child`(b: @Unique B) {
    consume(b)
    consume(<!INVALID_MOVED_ACCESS!>b<!>.y)
}

fun `move child via assignment then access parent`(b: @Unique B) {
    val local: @Unique A = b.y
    consume(local)

    consume(<!ESCAPE_UNIQUENESS_INCONSISTENCY!>b<!>)
}

fun `move child via assignment then reassign`(b: @Unique B, fresh: @Unique A) {
    val local: @Unique A = b.y
    b.y = fresh
    consume(local)

    consume(b)
}

fun `consume property of if branching between root and child`(n: @Unique N) {
    consume((if (nondet()) n.b else n).c)
    consume(<!INVALID_MOVED_ACCESS!>n.c<!>)
    consume(<!INVALID_MOVED_ACCESS!>n.b.c<!>)
}

fun `consume property of when branching between root and child`(n: @Unique N) {
    consume((when { nondet() -> n.b; else -> n }).c)
    consume(<!INVALID_MOVED_ACCESS!>n.c<!>)
    consume(<!INVALID_MOVED_ACCESS!>n.b.c<!>)
}

fun `consume both subproperties via if branching between siblings`(n: @Unique N) {
    consume((if (nondet()) n.b else n.c).c)
    consume(<!INVALID_MOVED_ACCESS!>n.b.c<!>)
    consume(<!INVALID_MOVED_ACCESS!>n.c.c<!>)
}

fun `consume child of borrowed unique and then return`(b: @Borrowed @Unique B) {
    consume(b.y)

    <!EXIT_UNIQUENESS_INCONSISTENCY!>return<!>
}

fun `consume child of borrowed unique and then do nothing`(b: @Borrowed @Unique B)
<!EXIT_UNIQUENESS_INCONSISTENCY!>{
    consume(b.y)
}<!>

fun `consume child of borrowed unique and then throw`(b: @Borrowed @Unique B, t: Throwable) {
    consume(b.y)

    <!EXIT_UNIQUENESS_INCONSISTENCY!>throw t<!>
}

context(b: @Unique A)
fun consumeContext() {
}

context(b: @Unique A)
fun `pass inconsistent unique as context parameter`() {
    val local = b.x
    <!CONTEXT_ESCAPE_UNIQUENESS_INCONSISTENCY!>consumeContext()<!>
}

fun `assign unique local field to unique`(x: @Borrowed @Unique A, y: @Unique Any) <!EXIT_UNIQUENESS_INCONSISTENCY!>{
    var z = x.x;
}<!>

fun `assign unique local field to unique and then reassign it to unique`(x: @Borrowed @Unique A, y: @Unique Any) {
    var z = x.x;
    x.x = A()
}

fun `move borrowed root then escape original`(x: @Borrowed @Unique A) {
    var z: @Borrowed @Unique A = x
    consume(<!INVALID_MOVED_ACCESS, LOCALITY_MISMATCH!>x<!>)
}

fun `assign shared local field to shared`(x: @Borrowed A, y: @Unique Any) <!EXIT_UNIQUENESS_INCONSISTENCY!>{
    var z = x.x;
}<!>

fun `assign shared local field to shared and then reassign it to unique`(x: @Borrowed A, y: @Unique Any) {
    var z = x.x;
    x.x = A()
}
