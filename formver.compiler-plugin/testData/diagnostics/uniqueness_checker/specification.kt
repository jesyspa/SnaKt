// FULL_JDK
// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.*

class A {
    var n: Int = 0
    var x: @Unique Any = Any()
}

class B {
    var y: @Unique A = A()
}

fun consume(a: @Unique Any) {}

fun consumeA(a: @Unique A) {}

// Specification blocks do not move what they mention

fun `consume unique after verifying it`(a: @Unique A) {
    verify(a.n == 0)
    consumeA(a)
}

fun `consume unique subproperty after verifying the parent`(a: @Unique B) {
    verify(a.y.n == 0)
    consume(a.y)
}

fun `consume unique after requesting access to its field`(a: @Unique A) {
    verify(acc(a.x))
    consumeA(a)
}

fun `consume unique after requesting read access to its field`(a: @Unique A) {
    verify(acc(a.x, read()))
    consumeA(a)
}

fun `consume unique after taking its old value`(a: @Unique A) {
    verify(old(a) === a)
    consumeA(a)
}

fun `consume unique after taking the old value of its field`(a: @Unique A) {
    verify(old(a.n) == a.n)
    consumeA(a)
}

fun `consume unique after naming it in a uniqueness predicate`(a: @Unique A) {
    unfold(UniquePred(a))
    fold(UniquePred(a))
    consumeA(a)
}

fun `consume unique after naming it in a uniqueness predicate with permissions`(a: @Unique A) {
    unfold(UniquePred(a), write())
    fold(UniquePred(a), write())
    consumeA(a)
}

fun `consume unique after mentioning it in preconditions`(a: @Unique A) {
    preconditions {
        a.n == 0
    }
    consumeA(a)
}

fun `consume unique after mentioning it in postconditions`(a: @Unique A) {
    postconditions<Unit> {
        a.n == 0
    }
    consumeA(a)
}

fun `consume unique after mentioning it in loop invariants`(a: @Unique A) {
    var i = 0
    while (i < 10) {
        loopInvariants {
            a.n == 0
        }
        i = i + 1
    }
    consumeA(a)
}

fun `consume unique after mentioning it in a quantifier`(a: @Unique A) {
    verify(forAll<Int> { i ->
        triggers(i)
        a.n == i
    })
    consumeA(a)
}

// Specification blocks do not restore what was moved before them

fun `consume moved unique after an unrelated specification`(a: @Unique A, b: @Unique A) {
    consumeA(a)
    verify(b.n == 0)
    consumeA(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `consume moved unique after verifying it`(a: @Unique A, b: @Unique A) {
    consumeA(a)
    verify(<!INVALID_MOVED_ACCESS!>a<!> === b)
    consumeA(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `consume moved unique after taking its old value`(a: @Unique A, b: @Unique A) {
    consumeA(a)
    verify(old(<!INVALID_MOVED_ACCESS!>a<!>) === b)
    consumeA(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `consume moved unique after naming it in a uniqueness predicate`(a: @Unique A) {
    consumeA(a)
    unfold(UniquePred(<!INVALID_MOVED_ACCESS!>a<!>))
    consumeA(<!INVALID_MOVED_ACCESS!>a<!>)
}

fun `consume moved unique after mentioning it in preconditions`(a: @Unique A) {
    consumeA(a)
    preconditions {
        a.n == 0
    }
    consumeA(<!INVALID_MOVED_ACCESS!>a<!>)
}
